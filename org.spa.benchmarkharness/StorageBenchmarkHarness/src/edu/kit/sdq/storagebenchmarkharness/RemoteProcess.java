package edu.kit.sdq.storagebenchmarkharness;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Defines a remotely controllable process.
 * 
 * @author Dominik Bruhn
 *
 */
public interface RemoteProcess
{
	public abstract int getExitStatus();

	public abstract void waitFor();

	public boolean isClosed();

	public abstract boolean stopProcess();
	
	public abstract int getPid();

	public abstract InputStream getErrorStream() throws IOException;

	public abstract OutputStream getOutputStream() throws IOException;

	public abstract InputStream getInputStream() throws IOException;

	public abstract void finish();
}
