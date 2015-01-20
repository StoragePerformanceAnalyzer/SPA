package edu.kit.sdq.storagebenchmarkharness.datastore.sqlite;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteJob;
import com.almworks.sqlite4java.SQLiteStatement;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBenchmark;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.datastore.DataStore;
import edu.kit.sdq.storagebenchmarkharness.exceptions.DataStoreException;

/**
 * This class provides a DataStore which is backed by a SQLite Database. The
 * interface to SQLite is done using the sqlite4java library. As SQLite is
 * single-threaded, only one thread can use the connection. For performance
 * reasons, the database connection held in its own thread and a queuing
 * interface is used to queue jobs which will be executed by the database
 * thread. This functionality is provided by the sqlite4java class
 * {@code SQLiteQueue}.
 * 
 * To map between the EMF-Classes and the database columns, the
 * {@code SQLiteHelper} is used.
 * 
 * @author Dominik Bruhn, Axel Busch (axel.busch@student.kit.edu)
 * 
 */
public class SQLiteDataStore implements DataStore
{
	private static final Logger LOGGER = Logger.getLogger(SQLiteDataStore.class);

	// The amount of time the connection waits to acquire a database lock. If
	// this time is exceeded, the query fails. In ms.
	private static final int LOCK_WAIT_TIMEOUT = 10000;

	private final SBHSQLiteQueue queue;
	private final SQLiteHelper helper;

	private long crId;
	private boolean wasSetup = false;

	/**
	 * Creates all structures needed for the database and opens the connection.
	 * If the database does not exist, it is created. Some configuration
	 * parameters are set.
	 * 
	 * @param database
	 */
	public SQLiteDataStore(String database)
	{
		java.util.logging.Logger.getLogger("com.almworks.sqlite4java").setLevel(Level.WARNING);

		LOGGER.debug("Opening Database '%s'", database);

		// Create Queue
		queue = new SBHSQLiteQueue(new File(database));
		queue.start();

		try
		{
			queue.execute(new SQLiteJob<Void>()
			{
				protected Void job(SQLiteConnection db) throws SQLiteException
				{
					// Wait 10 seconds for a lock, then fail
					db.setBusyTimeout(LOCK_WAIT_TIMEOUT);
					// Do not use fsync to write the wal-logs of sqlite to the
					// disk
					db.exec("PRAGMA synchronous = off;");

					return null;
				}
			}).get();
		} catch (InterruptedException e)
		{
			throw new DataStoreException(e);
		} catch (ExecutionException e)
		{
			throw new DataStoreException(e);
		}

		// Create Helpers
		helper = new SQLiteHelper();
	}

	@Override
	public void setupDataStore()
	{
		// Create Database and Base Schema synchronously, thus waiting for the
		// job to finish.
		LOGGER.debug("Setting up Database");

		try
		{
			queue.execute(new SQLiteJob<Void>()
			{
				protected Void job(SQLiteConnection db) throws SQLiteException
				{
					// Create the base-schema with the basic tables
					db.exec(getSchema("base"));

					// Initialize all benchmark-related tables and their
					// appropriate columns
					helper.setupTables(db);

					return null;
				}
			}).get();
		} catch (InterruptedException e)
		{
			throw new DataStoreException(e);
		} catch (ExecutionException e)
		{
			throw new DataStoreException(e);
		}

		wasSetup = true;
	}

	@Override
	public void closeDataStore()
	{
		Preconditions.checkArgument(wasSetup, "Need to call 'setupDataStore' first");

		// Stop the queue (does not wait for the queue to finish)
		queue.stop(true);
		try
		{
			// Wait for the queue to finish all jobs
			queue.join();
		} catch (InterruptedException e)
		{
			LOGGER.error("Queue.join was interrupted", e);
		}
	}

	@Override
	public void storeConfigurationRun(final String identifier)
	{
		Preconditions.checkArgument(wasSetup, "Need to call 'setupDataStore' first");

		// Save the configuration run and remember its database id for later
		// reference. This is done synchronously thus waiting for the
		// termination of the job.
		try
		{
			crId = queue.execute(new SQLiteJob<Long>()
			{
				protected Long job(SQLiteConnection db) throws SQLiteException
				{
					SQLiteStatement stmnt = db.prepare("INSERT INTO configurationRuns (crIdentifier, crTime, crFinished)  "
							+ "VALUES (?, DATETIME(), 0);");
					stmnt.bind(1, identifier);
					stmnt.step();
					stmnt.dispose();

					return db.getLastInsertId();
				}
			}).get();
		} catch (InterruptedException e)
		{
			throw new DataStoreException(e);
		} catch (ExecutionException e)
		{
			throw new DataStoreException(e);
		}
	}

	@Override
	public void storeExperimentResults(final int expNo, final String hostId, final String benchmarkId, final int repeatNo, final String expUid,
			final IndependentVariablesOfSut sutVars, final IndependentVariablesOfBenchmark benchVars, final List<DependentVariables> dependentVars)
	{
		Preconditions.checkArgument(wasSetup, "Need to call 'setupDataStore' first");

		// Saves the independent (input) and dependent (output) variables for a
		// single experiment. Because of the huge amount of data involved, this
		// is done asynchronously. This means that this method returns
		// immediately and the work is done in background by the database
		// thread. Because of the huge amount of data involved, it is not
		// possible to move more than one job to the background.

		// To prevent a out of memory exception, if there is there are currently
		// jobs in the queue, wait until all of them finish before continuing.
		// if (queue.getQueueLength() > 0 || queue.isJobRunning()) {
		if (queue.getQueueLength() > 1)
		{
			LOGGER.debug("Queue contains %d job and a job is Running %b waiting until they finish", queue.getQueueLength(), queue.isJobRunning());
			// while (queue.getQueueLength() > 0 || queue.isJobRunning()) {
			while (queue.getQueueLength() > 1)
			{
				try
				{
					Thread.sleep(1000);
				} catch (InterruptedException e)
				{
					LOGGER.error("Waiting for job finish failed", e);
				}
			}
			LOGGER.debug("Queue is now empty and job finished, continuing execution");
		}

		// Actually append the new job
		queue.execute(new SQLiteJob<Void>()
		{
			protected Void job(SQLiteConnection db) throws SQLiteException
			{
				for (int i = 0; i < dependentVars.size(); ++i)
					LOGGER.debug("Starting saving %d results for host %s, expNo %d, repeatNo %d", dependentVars.get(i).getValues().size(), hostId,
							expNo, repeatNo);
				db.exec("BEGIN;");

				LOGGER.trace("Saving in runs-table");
				SQLiteStatement stmnt = db.prepare("INSERT INTO runs (crId, expNo, repeatNo, hostId, benchmarkId, expUid) "
						+ "VALUES (?, ?, ?, ?, ?, ?);");
				stmnt.bind(1, crId);
				stmnt.bind(2, expNo);
				stmnt.bind(3, repeatNo);
				stmnt.bind(4, hostId);
				stmnt.bind(5, benchmarkId);
				stmnt.bind(6, expUid);
				stmnt.step();
				stmnt.dispose();

				long runId = db.getLastInsertId();

				LOGGER.trace("Saving independent vars");
				helper.saveIndependentVars(runId, sutVars, benchVars, db);
				LOGGER.trace("Saving dependent vars");
				helper.saveDependentVars(runId, dependentVars, db);

				LOGGER.trace("Commiting");
				db.exec("COMMIT;");

				for (int i = 0; i < dependentVars.size(); ++i)
					LOGGER.debug("Finished saving %d results for host %s, expNo %d, repeatNo %d", dependentVars.get(i).getValues().size(), hostId,
							expNo, repeatNo);

				return null;
			}
		});
	}

	@Override
	// Marks the configuration run finished in a synchronous way.
	public void finishConfigurationRun()
	{
		Preconditions.checkArgument(wasSetup, "Need to call 'setupDataStore' first");

		try
		{
			queue.execute(new SQLiteJob<Void>()
			{
				protected Void job(SQLiteConnection db) throws SQLiteException
				{
					SQLiteStatement stmnt = db.prepare("UPDATE configurationRuns SET crFinished=1 WHERE crId=?;");
					stmnt.bind(1, crId);
					stmnt.step();
					stmnt.dispose();
					return null;
				}
			}).get();
		} catch (InterruptedException e)
		{
			throw new DataStoreException(e);
		} catch (ExecutionException e)
		{
			throw new DataStoreException(e);
		}
	}

	/**
	 * Reads the SQL-Statements used to construct a table from a resource. The
	 * default platform encoding is used.
	 * 
	 * @param name
	 *            the schema-file to read
	 * @return the schema
	 */
	public static String getSchema(String name)
	{
		try
		{
			InputStream is = SQLiteDataStore.class.getResourceAsStream("/res/" + name + ".sqlite.sql");
			return CharStreams.toString(new InputStreamReader(is));
		} catch (IOException e)
		{
			throw new IllegalArgumentException("Schema read failed", e);
		}
	}
}
