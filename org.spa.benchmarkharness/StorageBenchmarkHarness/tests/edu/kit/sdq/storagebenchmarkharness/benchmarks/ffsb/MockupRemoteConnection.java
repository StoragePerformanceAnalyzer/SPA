package edu.kit.sdq.storagebenchmarkharness.benchmarks.ffsb;

import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.RemoteProcess;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.SystemUnderTest;
import edu.kit.sdq.storagebenchmarkharness.exceptions.RemoteConnectionException;

/**
 * A class implementing the {@code RemoteConnection} interface but leaving all
 * methods empty. Only usefull in tests.
 * 
 * @author dominik
 * 
 */
public abstract class MockupRemoteConnection implements RemoteConnection
{
	@Override
	public void open() throws RemoteConnectionException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void close()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void saveStringToFile(String content, String filename, boolean useSudo) throws RemoteConnectionException
	{
		// TODO Auto-generated method stub
	}

	@Override
	public void deleteFile(String filename)
	{
		// TODO Auto-generated method stub
	}

	@Override
	public RemoteProcess execCmd(String cmdl, boolean savePid) throws RemoteConnectionException
	{
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public RemoteProcess execCmd(String cmdl, boolean savePid, boolean log) throws RemoteConnectionException
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public SystemUnderTest getHost()
	{
		// TODO Auto-generated method stub
		return null;
	}
}
