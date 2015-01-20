package edu.kit.sdq.storagebenchmarkharness;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import com.google.common.io.CharStreams;

import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.FileSystem;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBenchmark;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Scheduler;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.CheckedBenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;

/**
 * Provides a interface between a remote benchmark and the benchmark controller.
 * Every benchmark has some independent variables (or input variables) and some
 * dependent variables (or output variables). These two variables are captured
 * by the generic variables IV and DV.
 * 
 * The sequence of execution is as follows:
 * 
 * <pre>
 * 1. Creation of the BenchmarkDriver
 * 2. For each Experiment:
 *    2.1 prepareExperiment(Experiment)
 *    2.2 For each repetition:
 *        2.2.1 startExperiment()
 *    2.3 endExperiment()
 * </pre>
 * 
 * The {@code startExperiment} and {@code prepareExperiment} method needs to be
 * implemented. The {@code endExperiment} method can be left unimplemented if
 * not functionality is needed.
 * 
 * @author Dominik Bruhn, Axel Busch (axel.busch@student.kit.edu)
 * 
 * @param <IV>
 *            The independent variables for this benchmark (aka input variables)
 * @param <DV>
 *            The dependent variables for this benchmark (aka output variables)
 */
public abstract class BenchmarkDriver<IV extends IndependentVariablesOfBenchmark, DV extends DependentVariables> extends Driver
{
	private static final Logger LOGGER = Logger.getLogger(BenchmarkDriver.class);

	protected final RemoteConnection connection;
	
	protected RemoteProcess currentProcess;

	public BenchmarkDriver(RemoteConnection con, String rawFileSaveDir)
	{
		super(rawFileSaveDir);
		connection = con;

		LOGGER.debug("RawFileSaveDir is %s", rawFileSaveDir);
	}

	/**
	 * Provides the independent variables to the benchmark driver. These contain
	 * every information which is necessary to execute the benchmark. It is
	 * executed once before the calls to startExperiment().
	 * 
	 * @param sutVars
	 *            The independent variables which are not specific for this
	 *            benchmark but instead are needed for every benchmark.
	 * @param benchVars
	 *            The independent variables which are specific for this
	 *            benchmark.
	 */
	protected abstract void prepareExperiment(IndependentVariablesOfSut sutVars, IV benchVars) throws BenchmarkException, CheckedBenchmarkException;

	/**
	 * This method is actually called from the outside. It does some generic
	 * work and then forward to the {@code prepareExperimentImpl} method. All
	 * parameters except the {@code expUid} are forwarded to this method.
	 * 
	 * This method contains a workaround for some shortcomings of the java
	 * generic system. In some parts of the benchmark controller, the generic
	 * type of the benchVars is known but can not be told to the java type
	 * system. This method does the cast to the appropriate type. It then calls
	 * {@code prepareExperiment} using the same parameters as provided to this
	 * method.
	 * 
	 * @param expUId
	 *            A unique identifier for this experiment. This identifier is
	 *            used for saving the raw files.
	 * @param sutVars
	 * @param benchVars
	 */
	@SuppressWarnings("unchecked")
	public void prepareExperiment(String expUId, IndependentVariablesOfSut sutVars, IndependentVariablesOfBenchmark benchVars) throws BenchmarkException, CheckedBenchmarkException
	{
		setExpUid(expUId);
		prepareExperiment(sutVars, (IV) benchVars);
	}

	/**
	 * Starts the experiment. This method is called several times, depending how
	 * often each experiment should be repeated. The method should return zero
	 * or more results from this run.
	 * 
	 * @param repeatNo
	 * @return The results of the experiment run, may not be null, but may be of
	 *         length zero
	 */
	public abstract DV startExperiment(int repeatNo) throws BenchmarkException, CheckedBenchmarkException;

	/**
	 * Called after all repetitions of the experiments have been completed. Can
	 * be used to clean up resources and files.
	 */
	public void endExperiment()
	{
		// Override if necessary
	}

	/**
	 * Retrieves a java resource from the source tree. Can be used by the
	 * benchmark drivers to retrieve templates for their configuration files.
	 * 
	 * @param name
	 *            The filename of the java resource.
	 * @return The contents of the resource as string or null if the resource
	 *         does not exist or could not be read.
	 */
	protected static String getResource(String name)
	{
		try
		{
			return CharStreams.toString(new InputStreamReader(BenchmarkDriver.class.getResourceAsStream("/res/" + name)));
		} catch (IOException e)
		{
			LOGGER.error("Could not read resource", e);
			return null;
		}
	}

	/**
	 * Checks if the target directory has the correct filesystem. Throws a
	 * exception if the wrong filesystem is applied on the target directory.
	 * 
	 * This method does NOT change the filesystem, this must currently done by
	 * hand in order to prevent data loss.
	 * 
	 * @param fileSystem
	 *            The filesystem which should be applied on the filesystem.
	 * @param directory
	 *            The directory where the data will be stored and for which the
	 *            scheduler should be set
	 */
	protected void checkFileSystem(FileSystem fileSystem, String targetDir)
	{
		String expFsName;
		switch (fileSystem)
		{
		case EXT4:
			expFsName = "ext4";
			break;
		case EXT3:
			expFsName = "ext3";
			break;
		default:
			throw new BenchmarkException("Unsupported Filesystem " + fileSystem);
		}

		RemoteProcess pfs = connection.execCmd("df -T -P " + targetDir + " | awk 'NR>1 {printf $2}'", false);
		try
		{
			String actualFsName = CharStreams.toString(new InputStreamReader(pfs.getInputStream()));
			pfs.waitFor();

			if (pfs.getExitStatus() != 0)
			{
				throw new BenchmarkException("Could not retrieve filesystem of " + targetDir);
			}

			if (actualFsName == null || actualFsName.length() == 0)
			{
				throw new BenchmarkException("Could not retrieve filesystem of " + targetDir);
			}

			LOGGER.debug("Current Filesystem %s, expected %s", actualFsName, expFsName);

			if (!actualFsName.equals(expFsName))
			{
				throw new BenchmarkException("Filesystem of " + targetDir + " is " + actualFsName + ", " + expFsName + " expected");
			}
		} catch (IOException e)
		{
			throw new BenchmarkException("Could not retrieve filesystem of " + targetDir, e);
		} finally
		{
			pfs.finish();
		}
	}

	/**
	 * Sets the scheduler for the right block device. This method also checks if
	 * the scheduler is available on the system and throws a exception if not.
	 * 
	 * @param scheduler
	 *            The scheduler which should be made active on the blockdevice.
	 * @param directory
	 *            The directory where the data will be stored and for which the
	 *            scheduler should be set
	 */
	protected void setScheduler(Scheduler scheduler, String targetDir)
	{
		LOGGER.debug("Setting scheduler to %s", scheduler);

		// Get Block-Device where the targetdir resides
		String targetDev;
		RemoteProcess pfs = connection.execCmd("readlink -f `df -T -P " + targetDir + " | awk 'NR>1 {printf $1}'`", false);
		try
		{
			targetDev = CharStreams.toString(new InputStreamReader(pfs.getInputStream()));
			pfs.waitFor();
			if (pfs.getExitStatus() != 0)
			{
				throw new BenchmarkException("Could not get Block-Device of " + targetDir);
			}
		} catch (IOException e)
		{
			throw new BenchmarkException("Could not get Block-Device of " + targetDir, e);
		} finally
		{
			pfs.finish();
		}

		if (targetDev == null || targetDev.trim().length() == 0)
		{
			throw new BenchmarkException("Could not get Block-Device of " + targetDir);
		}

		if (!targetDev.startsWith("/dev/"))
		{
			throw new BenchmarkException("Unspported Blockdevice-Group in " + targetDev);
		}

		// Cut away '/dev/'
		targetDev = targetDev.trim().substring("/dev/".length());

		if (targetDev.startsWith("dm-"))
		{
			LOGGER.error("ATTENTION: Target-Dir %s is stored on device %s which is handled by a " + "device mapper. Unable to set scheduler",
					targetDir, targetDev);
			return;
		}

		// Get Available Schedulers
		String availableSchedulers;

		String schedulerPath;
		if (Character.isDigit(targetDev.charAt(targetDev.length() - 1)))
		{
			schedulerPath = "/sys/dev/block/`cat /sys/class/block/" + targetDev + "/dev`/../queue/scheduler";
		} else
		{
			schedulerPath = "/sys/class/block/" + targetDev + "/queue/scheduler";
		}

		RemoteProcess pcat = connection.execCmd("cat " + schedulerPath, false);
		try
		{
			availableSchedulers = CharStreams.toString(new InputStreamReader(pcat.getInputStream()));
			pcat.waitFor();
			if (pcat.getExitStatus() != 0)
			{
				throw new BenchmarkException("Could not get avaiable schedulers on " + targetDev);
			}
		} catch (IOException e)
		{
			throw new BenchmarkException("Could not get avaiable schedulers on " + targetDev, e);
		} finally
		{
			pfs.finish();
		}

		if (availableSchedulers == null || availableSchedulers.length() == 0)
		{
			throw new BenchmarkException("Could not get avaiable schedulers on " + targetDev);
		}

		List<String> schedulers = Arrays.asList(availableSchedulers.split(" "));
		LOGGER.debug("Available schedulers are %s", schedulers);

		// Construct expected schedulers
		String expScheduler;
		switch (scheduler)
		{
		case CFQ:
			expScheduler = "cfq";
			break;
		case DEADLINE:
			expScheduler = "deadline";
			break;
		case NOOP:
			expScheduler = "noop";
			break;
		default:
			throw new BenchmarkException("Unspported Scheduler by SBH " + scheduler);
		}

		if (schedulers.contains("[" + expScheduler + "]"))
		{
			// Scheduler already active
			LOGGER.debug("Scheduler " + expScheduler + " is already active, doing nothing");
		} else if (schedulers.contains(expScheduler))
		{
			// Scheduler available, but not active => activate
			LOGGER.debug("Activating scheduler " + expScheduler);
			connection.saveStringToFile(expScheduler, schedulerPath, true);
		} else
		{
			// Scheduler is not available
			throw new BenchmarkException("The host does not support the scheduler " + scheduler + " on device " + targetDev);
		}
	}
	
	public void stopCurrentProcess() {
		if(currentProcess != null && !currentProcess.isClosed())
			currentProcess.stopProcess();
	}
}
