package edu.kit.sdq.storagebenchmarkharness.exceptions;

public class BenchmarkException extends RuntimeException
{
	private static final long serialVersionUID = -5629638910119899745L;

	public BenchmarkException()
	{
		super();
	}

	public BenchmarkException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public BenchmarkException(String message)
	{
		super(message);
	}

	public BenchmarkException(Throwable cause)
	{
		super(cause);
	}
}
