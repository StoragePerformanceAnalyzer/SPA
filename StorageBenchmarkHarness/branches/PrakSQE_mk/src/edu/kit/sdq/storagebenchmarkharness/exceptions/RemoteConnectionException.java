package edu.kit.sdq.storagebenchmarkharness.exceptions;

public class RemoteConnectionException extends BenchmarkException
{
	private static final long serialVersionUID = -3972637180315363098L;

	public RemoteConnectionException(Throwable cause)
	{
		super(cause);
	}

	public RemoteConnectionException()
	{
		super();
	}

	public RemoteConnectionException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public RemoteConnectionException(String message)
	{
		super(message);
	}
}
