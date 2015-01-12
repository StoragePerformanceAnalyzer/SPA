package edu.kit.sdq.storagebenchmarkharness.exceptions;

/**
 * 
 * @author Dominik Bruhn
 *
 */
public class DataStoreException extends BenchmarkException
{
	private static final long serialVersionUID = -7611700336640955827L;

	public DataStoreException()
	{
		super();
	}

	public DataStoreException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public DataStoreException(String message)
	{
		super(message);
	}

	public DataStoreException(Throwable cause)
	{
		super(cause);
	}
}
