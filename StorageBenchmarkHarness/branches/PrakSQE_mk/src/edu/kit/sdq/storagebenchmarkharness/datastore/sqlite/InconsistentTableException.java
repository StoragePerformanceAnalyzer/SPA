package edu.kit.sdq.storagebenchmarkharness.datastore.sqlite;

import edu.kit.sdq.storagebenchmarkharness.exceptions.DataStoreException;

public final class InconsistentTableException extends DataStoreException
{
	private static final long serialVersionUID = -5807999776871024360L;

	public InconsistentTableException()
	{
		super();
	}

	public InconsistentTableException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public InconsistentTableException(String message)
	{
		super(message);
	}

	public InconsistentTableException(Throwable cause)
	{
		super(cause);
	}

}
