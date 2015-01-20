package edu.kit.sdq.storagebenchmarkharness.exceptions;

/**
 * In contrast to {@code BenchmarkException}, this exception is used to specify
 * an exception that must be caught, i.e., it is a checked exception. It allows 
 * to signal an exception that can be handled by the BenchmarkController by re-
 * scheduling the benchmark run. 
 * 
 * @author Qais Noorshams
 *
 */
public class CheckedBenchmarkException extends Exception {

	private static final long serialVersionUID = -6845756487597386176L;

	public CheckedBenchmarkException()
	{
		super();
	}

	public CheckedBenchmarkException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public CheckedBenchmarkException(String message)
	{
		super(message);
	}

	public CheckedBenchmarkException(Throwable cause)
	{
		super(cause);
	}
}
