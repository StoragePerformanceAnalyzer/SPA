package edu.kit.sdq.storagebenchmarkharness.datastore.sqlite;

import java.io.File;
import java.util.concurrent.ThreadFactory;

import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteQueue;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.exceptions.DataStoreException;

/**
 * Extends the {@code SQLiteQueue} provided by sqlite4java in some aspects:
 * 
 * - ability to check how long the queue is.
 * 
 * - ability to check if a job is currently running in the queue
 * 
 * - Throw exceptions in all threads if the worker-thread throws a exception.
 * 
 * @author Dominik Bruhn
 * 
 */
public class SBHSQLiteQueue extends com.almworks.sqlite4java.SQLiteQueue
{
	private static final Logger LOGGER = Logger.getLogger(SBHSQLiteQueue.class);

	private boolean jobRuning = false;

	private boolean jobFailed = false;
	private Throwable jobsFailureReason;

	public SBHSQLiteQueue()
	{
		super();
	}

	public SBHSQLiteQueue(File databaseFile, ThreadFactory threadFactory)
	{
		super(databaseFile, threadFactory);
	}

	public SBHSQLiteQueue(File databaseFile)
	{
		super(databaseFile);
	}

	public int getQueueLength()
	{
		return myJobs.size();
	}

	@Override
	protected void executeJob(SQLiteJob job) throws Throwable
	{
		LOGGER.debug("Starting executing Job %s", job);
		jobRuning = true;
		super.executeJob(job);
		jobRuning = false;
		LOGGER.debug("Finished executing Job %s", job);
	}

	@Override
	protected void handleJobException(SQLiteJob job, Throwable e) throws Throwable
	{
		super.handleJobException(job, e);
		LOGGER.error("Job %s throw error: %s", job, e);
		jobFailed = true;
		jobsFailureReason = e;
	}

	public boolean isJobRunning()
	{
		return jobRuning;
	}

	@Override
	public <T, J extends SQLiteJob<T>> J execute(J job)
	{
		if (jobFailed)
		{
			throw new DataStoreException("A previous job in the queue failed", jobsFailureReason);
		}
		return super.execute(job);
	}

	@Override
	public SQLiteQueue stop(boolean gracefully)
	{
		if (jobFailed)
		{
			throw new DataStoreException("A previous job in the queue failed", jobsFailureReason);
		}
		return super.stop(gracefully);
	}

	@Override
	public SQLiteQueue join() throws InterruptedException
	{
		if (jobFailed)
		{
			throw new DataStoreException("A previous job in the queue failed", jobsFailureReason);
		}
		return super.join();
	}

}
