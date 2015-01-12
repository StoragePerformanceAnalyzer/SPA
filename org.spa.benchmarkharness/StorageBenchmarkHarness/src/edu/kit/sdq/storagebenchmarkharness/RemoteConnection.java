package edu.kit.sdq.storagebenchmarkharness;

import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.SystemUnderTest;

/**
 * Generic interface for a connection to a remote host. Used for communication
 * between benchmark drivers and benchmark targets.
 * 
 * @author Dominik Bruhn 
 * @author Axel Busch 
 * @author Qais Noorshams
 * 
 */
public interface RemoteConnection
{
	/**
	 * Opens the connection. Will be called before any other call is made.
	 */
	public abstract void open();

	/**
	 * Closes the connection. Will be the last call to the connection.
	 */
	public abstract void close();

	/**
	 * Saves a text/string to a file on the remote host.
	 * 
	 * @param content
	 *            The contents which should be saved to the host
	 * @param filename
	 *            The absolute path to the file where the content should be
	 *            written.
	 * @param useSudo
	 *            Defines whether additional privileges should be obtained by
	 *            using sudo. This allows to save to files which are only
	 *            writable by the superuser.
	 */
	public abstract void saveStringToFile(String content, String filename, boolean useSudo);

	/**
	 * Deletes a file from the remote host.
	 * 
	 * @param filename
	 *            The absolute filename to the file which should be deleted.
	 */
	public abstract void deleteFile(String filename);

	/**
	 * Executes a remote program.
	 * 
	 * @param cmd
	 *            The command which should be executed. Can be either a absolute
	 *            path or a single command. In the second case the command is
	 *            searched in the PATH variable which was set by the system
	 *            and/or by the user.
	 * @param savePid
	 *            Save the process ID for future access
	 * @return A RemoteProcess instance which can be used to interact with the
	 *         process and retrieve its output and results.
	 */
	public abstract RemoteProcess execCmd(String cmd, boolean savePid);

	/**
	 * Executes a remote program.
	 * 
	 * @param cmd
	 *            The command which should be executed. Can be either a absolute
	 *            path or a single command. In the second case the command is
	 *            searched in the PATH variable which was set by the system
	 *            and/or by the user.
	 * @param savePid
	 *            Save the process ID for future access
	 * @param log
	 *            Print the commands in the log output
	 * @return A RemoteProcess instance which can be used to interact with the
	 *         process and retrieve its output and results.
	 */
	public abstract RemoteProcess execCmd(String cmd, boolean savePid, boolean log);

	/**
	 * @return Return the host to which one is connected to.
	 */
	public abstract SystemUnderTest getHost();
}