package edu.kit.sdq.storagebenchmarkharness.benchmarks.filebench;

import java.util.Map;

import com.beust.jcommander.internal.Maps;

import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.SSHRemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.SystemUnderTest;

/**
 * This class collects the additional {@code RemoteConnection}s which are needed for the Filebench.
 * @author Michael Kaufmann
 *
 */
public class ConnectionCollector
{
	private static Map<SystemUnderTest, RemoteConnection> connections = Maps.newHashMap();

	/**
	 * Returns a connected Connection to the specified SuT.
	 * 
	 * @param sut {@code SystemUnderTest} to which the connection should be build.
	 * @return {@code RemoteConnection} to the specified SuT.
	 */
	public static RemoteConnection getAdditionalConnection(SystemUnderTest sut)
	{
		RemoteConnection conn = connections.get(sut);

		if (conn == null)
		{
			conn = new SSHRemoteConnection(sut);
			conn.open();
			connections.put(sut, conn);
		}
		
		if(!conn.isConnected())
			conn.open();

		return conn;
	}

	/**
	 * Closes all additional connections to the SuTs.
	 */
	public static void closeAllAdditionalConnections() {
		for(RemoteConnection conn : connections.values()) {
			conn.close();
		}
	}
}
