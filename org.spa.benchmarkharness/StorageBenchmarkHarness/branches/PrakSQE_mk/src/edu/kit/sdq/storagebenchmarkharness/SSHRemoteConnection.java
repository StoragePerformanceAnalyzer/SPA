package edu.kit.sdq.storagebenchmarkharness;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.io.CharStreams;
import com.google.common.io.OutputSupplier;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.SystemUnderTest;
import edu.kit.sdq.storagebenchmarkharness.exceptions.RemoteConnectionException;

/**
 * Provides a remote connection to a system under test using the Java Secure
 * Channel (JSch) library. This library is poorly documented and has some odd
 * conventions. To hide this complexity from the {@code BenchmarkDrivers} this
 * class provides the most used functions with a simple interface.
 * 
 * Before the execution of any commands, the RemoteConnection first has to be
 * connected using the {@code open} function.
 * 
 * @author dominik, Axel Busch (axel.busch@student.kit.edu), Qais Noorshams
 *         (noorshams@kit.edu)
 * 
 */
public class SSHRemoteConnection implements RemoteConnection
{

	// How often should it retried to open a execChannel.
	private final static int OPEN_NUMBER_OF_RETRIES = 4;

	// The timeout for opening a session in ms.
	private final static int SESSION_CONNECT_TIMEOUT = 100000;

	private static final Logger LOGGER = Logger.getLogger(SSHRemoteConnection.class);
	private final SystemUnderTest host;

	private JSch jsch;
	private Session session;
	private int pid = -1;

	/**
	 * This set contains all <i>open</i> connections. It is a
	 * "ConcurrentHashSet" in order to be thread safe, this also avoids
	 * {@code ConcurrentModificationException}s.
	 */
	private static Set<SSHRemoteConnection> connectionRegistry = Collections.newSetFromMap(new ConcurrentHashMap<SSHRemoteConnection, Boolean>());

	/**
	 * Use this method only to close all connections in case of an exception.<br>
	 * Do <i>NOT</i> use this method to open connections arbitrarily and close
	 * orphaned connections in between the running process.
	 */
	public static void closeOrphanedConnections()
	{

		if (connectionRegistry.size() > 0)
		{
			LOGGER.debug("Closing %d connections", connectionRegistry.size());
			LOGGER.debug("If no exception occurred, check the code for orphaned connections!");
		}
		for (Iterator<SSHRemoteConnection> iterator = connectionRegistry.iterator(); iterator.hasNext();)
		{
			SSHRemoteConnection conn = iterator.next();
			conn.close();
		}
	}

	/**
	 * Creates a new RemoteConnection but does not open it.
	 * 
	 * @param host
	 *            The host to which the connection should be established later.
	 */
	public SSHRemoteConnection(SystemUnderTest host)
	{
		this.host = host;

		LOGGER.debug("Creating remote connection to %s", host);
	}

	@Override
	public void open() throws RemoteConnectionException
	{
		try
		{
			jsch = new JSch();
			LOGGER.debug("Adding publickey %s", host.getKeyFile());
			jsch.addIdentity(host.getKeyFile());

			openSession();

			connectionRegistry.add(this);
		} catch (JSchException e)
		{
			throw new RemoteConnectionException(e);
		}
	}

	/**
	 * This method actually opens the SSH session. It is executed from the
	 * {@code open} method. Additionally it is tried to reestablish a new
	 * connection every time the connection fails (due to network outages).
	 * 
	 * @throws JSchException
	 */
	private void openSession() throws JSchException
	{
		if (session != null)
		{
			try
			{
				session.disconnect();
			} catch (Exception e)
			{
				LOGGER.error("Closing previous connection which failed", e);
			}
		}

		LOGGER.debug("Connecting to %s:%d", host.getIp(), host.getPort());
		session = jsch.getSession(host.getUser(), host.getIp(), host.getPort());
		session.setConfig("StrictHostKeyChecking", "no");
		session.setTimeout(SESSION_CONNECT_TIMEOUT);

		session.connect();
	}

	@Override
	public void close()
	{
		if (session != null)
		{
			session.disconnect();

			connectionRegistry.remove(this);
		}
	}

	/**
	 * Opens a new Exec-Channel in the ssh-session. If the session is broken, a
	 * new session is established. This can happen due to network failures or
	 * due to bugs in the jsch library.
	 * 
	 * @return A SSH-Channel which can be used to execute commands remotely.
	 * @throws JSchException
	 */
	private ChannelExec openExecChannel() throws JSchException
	{
		int retries = OPEN_NUMBER_OF_RETRIES;

		while (retries > 0)
		{
			try
			{
				ChannelExec r = (ChannelExec) session.openChannel("exec");
				return r;
			} catch (JSchException ex)
			{
				String message = ex.getMessage();
				if ("channel is not opened.".equals(message) || "session is down".equals(message))
				{
					LOGGER.info("JSch exception opening channel. Waiting, reconnecting and retrying", ex);
					// Looks like in this case an attempt to
					// just re-open a channel will fail - so wait and create a
					// new session

					try
					{
						Thread.sleep(1000);
					} catch (InterruptedException e)
					{
						throw new RemoteConnectionException(e);
					}

					openSession();
				} else
				{
					throw ex;
				}
			}

			retries--;
		}

		LOGGER.error("JSch failed %d times, aborting", OPEN_NUMBER_OF_RETRIES);
		throw new RemoteConnectionException("JSch failed " + OPEN_NUMBER_OF_RETRIES + " times.");
	}

	@Override
	// Saves a string remotely using tee.
	public void saveStringToFile(String content, String filename, boolean useSudo) throws RemoteConnectionException
	{
		try
		{
			String cmd;
			if (!useSudo)
			{
				cmd = "tee " + filename;
			} else
			{
				cmd = "sudo tee " + filename;
			}

			final RemoteProcess teeProcess = execCmd(cmd, false);

			try
			{
				CharStreams.write(content, new OutputSupplier<OutputStreamWriter>()
				{
					@Override
					public OutputStreamWriter getOutput() throws IOException
					{
						return new OutputStreamWriter(teeProcess.getOutputStream());
					}

				});

				teeProcess.waitFor();

				String str = CharStreams.toString(new InputStreamReader(teeProcess.getInputStream()));
				String errStr = CharStreams.toString(new InputStreamReader(teeProcess.getErrorStream()));

				// Check Exit-Status
				if (teeProcess.getExitStatus() != 0)
				{
					LOGGER.error("Could not write to file %s: %s", filename, errStr);
					throw new RemoteConnectionException("Could not write to file " + filename);
				}

				// Check everything correctly written
				if (!content.equals(str))
				{
					LOGGER.error("Write failed: Expected %s but got %s", content, str);
					throw new RemoteConnectionException("Could not write to file " + filename);
				}
			} finally
			{
				if (teeProcess != null)
				{
					teeProcess.finish();
				}
			}
		} catch (IOException e)
		{
			LOGGER.error("saveStringToFile failed", e);
			throw new RemoteConnectionException(e);
		}
	}

	@Override
	public void deleteFile(String filename)
	{
		RemoteProcess rmProcess = execCmd("rm " + filename, false);
		try
		{
			rmProcess.waitFor();

			if (rmProcess.getExitStatus() != 0)
			{
				LOGGER.error("Could not delete file %s", filename);
				throw new RemoteConnectionException("Could not delete file " + filename);
			}
		} finally
		{
			if (rmProcess != null)
			{
				rmProcess.finish();
			}
		}
	}

	@Override
	public RemoteProcess execCmd(String cmd, boolean savePid) throws RemoteConnectionException
	{
		try
		{
			return execCmd(cmd, savePid, true);

		} catch (RemoteConnectionException e)
		{
			LOGGER.error("execCmd failed", e);
			throw new RemoteConnectionException(e);
		}
	}

	public RemoteProcess execCmd(String cmd, boolean savePid, boolean log) throws RemoteConnectionException
	{
		try
		{
			final ChannelExec channel = openExecChannel();

			// Do not execute directly but instead use bash. This evaluates the
			// users .profile file before executing and thus expands the PATH
			// variable.
			if (savePid)
				cmd += " &  sudo echo PID $!";
			cmd = "bash -l -c '" + cmd.replace("'", "'\\''") + "'";

			if (log)
				LOGGER.debug("Command is %s", cmd);

			channel.setCommand(cmd);
			final InputStream in = channel.getInputStream();
			final OutputStream out = channel.getOutputStream();
			final InputStream err = channel.getErrStream();
			channel.connect();
			if (savePid)
			{
				BufferedReader br = new BufferedReader(new InputStreamReader(in));
				String s;
				while ((s = br.readLine()) != null && !s.contains("PID"))
				{
					// Empty body to skip lines
				}
				if (s != null)
				{
					this.pid = Integer.parseInt(s.split(" ")[1]);
				} else
				{
					throw new RemoteConnectionException("Could not obtain PID. Did you forget to setup sudoers file?");
				}
			}
			return new RemoteProcess()
			{
				@Override
				public void waitFor()
				{
					while (!channel.isClosed())
					{
						try
						{
							Thread.sleep(200);
						} catch (InterruptedException e)
						{
							LOGGER.error("Channel closed", e);
						}
					}
				}

				public boolean isClosed()
				{
					return channel.isClosed();
				}

				@Override
				public OutputStream getOutputStream() throws IOException
				{
					return out;
				}

				@Override
				public InputStream getInputStream() throws IOException
				{
					return in;
				}

				@Override
				public int getExitStatus()
				{
					return channel.getExitStatus();
				}

				@Override
				public InputStream getErrorStream() throws IOException
				{
					return err;
				}

				@Override
				public void finish()
				{
					channel.disconnect();
				}

				@Override
				public boolean stopProcess()
				{
					if (pid < 0)
					{
						LOGGER.debug("Cannot stop process, PID was not saved");
						return false;
					} else
					{
						RemoteConnection localConn = new SSHRemoteConnection(host);
						localConn.open();
						String cmd = "sudo kill " + pid;
						localConn.execCmd(cmd, false);
						localConn.close();
						return true;
					}
				}

				@Override
				public int getPid()
				{
					return pid;
				}
			};

		} catch (JSchException e)
		{
			LOGGER.error("execCmd failed", e);
			throw new RemoteConnectionException(e);
		} catch (IOException e)
		{
			LOGGER.error("execCmd failed", e);
			throw new RemoteConnectionException(e);
		}
	}

	@Override
	public String toString()
	{
		return "RemoteConnection [host=" + host + "]";
	}

	@Override
	public SystemUnderTest getHost()
	{
		return host;
	}

	@Override
	public boolean isConnected()
	{
		boolean ret = false;
		if (session != null)
			ret = session.isConnected();
		return ret;
	}
}
