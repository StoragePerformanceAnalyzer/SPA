package edu.kit.sdq.storagebenchmarkharness.exceptions;

/**
 * 
 * @author Axel Busch
 * 
 */
public class ParsingException extends Exception
{
	private static final long serialVersionUID = -5629638910119899745L;

	public ParsingException()
	{
		super();
	}

	public ParsingException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public ParsingException(String message)
	{
		super(message);
	}

	public ParsingException(Throwable cause)
	{
		super(cause);
	}
}
