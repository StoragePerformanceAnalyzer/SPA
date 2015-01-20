package edu.kit.sdq.storagebenchmarkharness.benchmarks;

public class CheckedBenchmarkException extends Exception
{
	public CheckedBenchmarkException() {
		super();
	}
	
	public CheckedBenchmarkException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public CheckedBenchmarkException(String message) {
		super(message);
	}
	
	public CheckedBenchmarkException(Throwable cause) {
		super(cause);
	}
}
