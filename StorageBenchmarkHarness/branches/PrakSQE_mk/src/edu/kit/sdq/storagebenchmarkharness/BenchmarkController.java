package edu.kit.sdq.storagebenchmarkharness;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.DiagnosticChain;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.EValidator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.emf.ecore.util.EcoreValidator;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.ocl.ecore.delegate.OCLDelegateDomain;
import org.eclipse.ocl.ecore.delegate.OCLInvocationDelegateFactory;
import org.eclipse.ocl.ecore.delegate.OCLSettingDelegateFactory;
import org.eclipse.ocl.ecore.delegate.OCLValidationDelegateFactory;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.validators.PositiveInteger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import edu.kit.sdq.storagebenchmarkharness.ExperimentSeriesHelper.DriverAndIndependentVars;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelPackage;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.Configuration;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.ExperimentSeries;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.ExperimentSetup;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.SystemUnderTest;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.CheckedBenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.filebench.ConnectionCollector;
import edu.kit.sdq.storagebenchmarkharness.benchmarkschedulerstrategies.IBenchmarkSchedulingStrategy;
import edu.kit.sdq.storagebenchmarkharness.benchmarkschedulerstrategies.SimpleStrat;
import edu.kit.sdq.storagebenchmarkharness.datastore.DataStore;
import edu.kit.sdq.storagebenchmarkharness.datastore.sqlite.SQLiteDataStore;
import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.exceptions.DataStoreException;

/**
 * The BenchmarkController controlls the the benchmarks by using the
 * {@code BenchmarkDrivers}. It uses a {@code DataStore} to save the results of
 * the benchmark runs.
 * 
 * @author Dominik Bruhn, Axel Busch (axel.busch@student.kit.edu)
 * 
 */
public class BenchmarkController
{
	private static final Logger LOGGER = Logger.getLogger(BenchmarkController.class);

	/**
	 * Simple class which holds the parameters which can be provided on the
	 * command line. Used by {@code JCommander} for the parameter parsing.
	 * 
	 * @author dominik, Axel Busch (axel.busch@student.kit.edu)
	 * 
	 */
	private static final class BenchmarkControllerParameters
	{
		@Parameter(names =
		{ "--quiet", "-q" }, description = "Quiet mode, do not display debug messages")
		private boolean quiet = false;

		@Parameter(names =
		{ "--database", "-d" }, description = "Database path. Required for actual benchmarking. Not required for verifying.")
		private String dbpath;

		@Parameter(names =
		{ "--conf", "-c" }, description = "Configuration File", required = true)
		private String confpath;

		@Parameter(names =
		{ "--rawfilesavedir", "-r" }, description = "Raw File Save Dir. A directory where the raw outputs of the benchmarks/monitors should be saved.")
		private String rawFileSaveDir;

		@Parameter(names =
		{ "--verify", "-v" }, description = "Verify only, do not benchmark. Creates a verification file containing all experiments.")
		private boolean verify;

		@Parameter(names =
		{ "--output", "-o" }, description = "Path where the verification file should be written in verify only mode")
		private String verifyOutput;

		@Parameter(names =
		{ "--startWithExperiment", "-s" }, description = "Experiment Number to start with", validateWith = PositiveInteger.class)
		private int startFromExperiment = 0;
	}

	public static void main(String[] args)
	{
		// Parse Parameters
		BenchmarkControllerParameters bcp = new BenchmarkControllerParameters();
		JCommander jcomm = new JCommander(bcp);
		try
		{
			jcomm.parse(args);
		} catch (ParameterException e)
		{
			jcomm.usage();
			System.exit(1);
		}

		// Disable Logging if 'quiet' is set.
		if (bcp.quiet)
		{
			((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("edu.kit.sdq.storagebenchmarkharness")).setLevel(Level.WARN);
		}

		if (!bcp.verify)
		{
			// Real execution of the benchmarks
			if (bcp.dbpath == null)
			{
				jcomm.usage();
				System.err.println("You must provide a database path.");
				System.exit(1);
			}

			DataStore dataStore = new SQLiteDataStore(bcp.dbpath);
			BenchmarkController controller = new BenchmarkController(bcp.confpath, dataStore, bcp.rawFileSaveDir);

			controller.run(bcp.startFromExperiment);
		} else
		{
			// Only verify and output verify html
			if (bcp.verifyOutput == null)
			{
				jcomm.usage();
				System.err.println("You must provide an output file for the verification.");
				System.exit(1);
			}

			BenchmarkController runner = new BenchmarkController(bcp.confpath);
			runner.verify(bcp.verifyOutput);
		}
	}

	private final DataStore datastore;

	// Maps from SUT-Identifier to a List of Benchmark and its variables.
	private final Map<String, List<DriverAndIndependentVars>> experimentsForSut;
	// Maps from SUT-Identifier to a RemoteConnection which connects to this
	// host
	private final Map<String, RemoteConnection> connectionsForSut;

	private Map<String, List<RemoteConnection>> connectionsForMonitor;
	// Number of SUTs involved in this config
	private final int sutCount;

	private final int repeatCount;

	private final boolean repeatWarmup;

	// Identifier of this configuration
	private final String mIdentifier;

	// ----------------------------------------------------------------------------------
	// Scheduling Strategy
	private IBenchmarkSchedulingStrategy strat;
	// Map to assign the threads to the BenchmarkRunners.
	private Map<BenchmarkRunner, Thread> threads;
	// Just a counter to keep track of restarts.
	private int restartCounter = 0;
	// Counter to keep track of experiments.
	private int experimentCounter = 0;

	/**
	 * Interruptflag, is true when one or more BenchmarkRunner experienced a
	 * benchmark malfunction.
	 */
	private volatile boolean interruptBenchmarks;

	// ----------------------------------------------------------------------------------

	/**
	 * Constructs a new BenchmarkController which can be used for benchmarking.
	 * 
	 * @param confFile
	 *            Configuration file in the XMI Format.
	 * @param datastore
	 *            A datastore instance which is used to save the results, may
	 *            not be {@code null}.
	 * @throws BenchmarkException
	 *             If the configuration is invalid
	 * @throws DataStoreException
	 *             If something regarding the {@code DataStore} fails.
	 */
	public BenchmarkController(String confFile, DataStore datastore, String rawFileSaveDir)
	{
		this.datastore = datastore;

		LOGGER.debug("Reading Configuration from %s", confFile);
		ExperimentSetup expSetup = loadConfigurationFromXMI(confFile);
		LOGGER.debug("Setup is %s", expSetup);

		mIdentifier = expSetup.getIdentifier();
		repeatCount = expSetup.getRepeatCount();
		repeatWarmup = expSetup.isRepeatWarmup();

		experimentsForSut = Maps.newHashMap();
		connectionsForSut = Maps.newHashMap();
		connectionsForMonitor = Maps.newHashMap();

		for (ExperimentSeries series : expSetup.getExperimentSeries())
		{
			LOGGER.debug("Found series %s", series);

			SystemUnderTest sut = series.getSystemUnderTest();
			String sutId = sut.getIdentifier();

			// Check if a connection to this SUT already exists, if not create
			// one and store
			RemoteConnection connection;
			if (!connectionsForSut.containsKey(sutId))
			{
				LOGGER.debug("No connection found for SUT %s, creating one", sutId);
				connection = new SSHRemoteConnection(sut);
				connectionsForSut.put(sutId, connection);
			} else
			{
				connection = connectionsForSut.get(sutId);
			}

			// Check if a connection set for the monitors for a particular SUT
			// already exists, if not create
			// one and store
			List<RemoteConnection> connectionSet = new ArrayList<RemoteConnection>();
			if (!connectionsForMonitor.containsKey(sutId))
			{
				LOGGER.debug("No connection set found for monitoring the SUT %s, creating one", sutId);
				for (int i = 0; i < series.getIndependentVariablesOfMonitor().size(); ++i)
				{
					connectionSet.add(new SSHRemoteConnection(sut));
				}
				connectionsForMonitor.put(sutId, connectionSet);
			} else
				connectionSet = connectionsForMonitor.get(sutId);

			// Check if a list in the experimentsForSut Map must be created
			List<DriverAndIndependentVars> experiments;
			if (!experimentsForSut.containsKey(sutId))
			{
				experiments = Lists.newArrayList();
				experimentsForSut.put(sutId, experiments);
			} else
			{
				experiments = experimentsForSut.get(sutId);
			}

			// Create Benchmark
			LOGGER.debug("Adding Experiments");
			List<DriverAndIndependentVars> expsForSeries = ExperimentSeriesHelper.getExperiments(series, connection, connectionSet, rawFileSaveDir);

			experiments.addAll(expsForSeries);
			LOGGER.debug("Found %d Experiments in this series", expsForSeries.size());

		}

		sutCount = connectionsForSut.size();

		// Check the experiment count
		// Get the experiment count of the first SUT and compare all other
		// SUTs experiments
		int length = experimentsForSut.values().iterator().next().size();
		for (List<DriverAndIndependentVars> experiments : experimentsForSut.values())
		{
			if (experiments.size() != length)
			{
				throw new BenchmarkException("There must be the same number of experiments for every SUT");
			}
		}

		if (datastore != null)
		{
			datastore.setupDataStore();
		}

		threads = Maps.newHashMap();
		interruptBenchmarks = false;
		strat = new SimpleStrat(); // TODO als Parameter, config...
		strat.addExperiment(experimentsForSut);
	}

	/**
	 * Constructs a new BenchmarkController which can only be used for
	 * verification due to the missing DataStore.
	 * 
	 * @param confFile
	 *            Configuration file in the XMI Format.
	 * @throws BenchmarkException
	 *             If the configuration is invalid
	 */
	public BenchmarkController(String confFile)
	{
		this(confFile, null, null);
	}

	/**
	 * Runs all experiments specified in the configuration starting from the
	 * first.
	 */
	public void run()
	{
		run(0);
	}

	/**
	 * Runs all experiments starting from a specific experiment. Can be used to
	 * resume benchmarking after a interruption.
	 * 
	 * @param startingFromExperiment
	 *            The Number of the first experiment which should be benchmarked
	 */
	public void run(int startingFromExperiment)
	{
		if (datastore == null)
		{
			throw new IllegalArgumentException("datastore is null, can not run benchmarks without datastore.");
		}

		try
		{
			// Open the RemoteConnections to all SUTs.
			LOGGER.debug("Connecting to all SUTs:");
			for (RemoteConnection con : connectionsForSut.values())
			{
				con.open();
			}

			datastore.storeConfigurationRun(mIdentifier);

			threadSynchronization = new CyclicBarrier(sutCount);
			threadFinished = new CountDownLatch(sutCount);

			// Connections for Monitoring
			LOGGER.debug("Create connecting for monitoring");
			for (List<RemoteConnection> conList : connectionsForMonitor.values())
				for (int i = 0; i < conList.size(); ++i)
					conList.get(i).open();

			// Start BenchmarkRunners for Experiment
			// repeat until no more experiments available
			while (strat.hasNextExperiment())
			{
				LOGGER.debug("Creating and Starting Threads");

				DriverAndIndependentVars tmp;
				for (String sutId : experimentsForSut.keySet())
				{
					// Get next experiment from the BenchmarkSchedulerStrategy
					// for the specific SuT.
					// Create a new BenchmarkRunner for only one experiment
					// and start a new thread.
					tmp = strat.getNextExperiment(sutId);
					BenchmarkRunner br = new BenchmarkRunner(sutId, tmp);
					Thread t = new Thread(br,  experimentCounter + "H-" + sutId);
					registerBenchmarkRunnerAndThread(br, t);
					t.start();
				}

				LOGGER.debug("Waiting for Threads to finish.");
				try
				{
					do
					{
						setInterruptFlag(false);
						threadFinished.await();
						if (getInterruptFlag())
						{
							LOGGER.debug("Experiment %d restarted %d times." , experimentCounter, restartCounter);
							restartRoutine();
							threadFinished = new CountDownLatch(sutCount);
						}
					} while (getInterruptFlag());
					setInterruptFlag(false);

					threadFinished = new CountDownLatch(sutCount);
					// Signal the successful finish of all BenchmarkRunners to
					// the BenchmarkController, so he can hand out the new
					// experiments.
					signalSuccess();
				} catch (InterruptedException e)
				{
					LOGGER.error("Interrupted the waiting for finish", e);
					throw new BenchmarkException(e);
				}
			}

			LOGGER.debug("All Threads finished");

			LOGGER.debug("Finishing Cofiguration Run");
			datastore.finishConfigurationRun();
		} finally
		{
			// Disconnect from all Suts
			LOGGER.debug("Closing all connections");
			for (RemoteConnection con : connectionsForSut.values())
			{
				con.close();
			}
			
			ConnectionCollector.closeAllAdditionalConnections();
			
			for (List<RemoteConnection> conList : connectionsForMonitor.values())
				for (int i = 0; i < conList.size(); ++i)
					conList.get(i).close();

			// Close orphaned connections in case of an exception
			LOGGER.debug("Closing all remaining connections");
			SSHRemoteConnection.closeOrphanedConnections();

			// Close the Datastore
			LOGGER.debug("Closing the datastore");
			datastore.closeDataStore();
		}
	}

	/**
	 * Registers a thread and the corresponding BenchmarkRunner.
	 * 
	 * @param br
	 *            The {@code BenchmarkRunner} which should be registered.
	 * @param t
	 *            Corresponding thread to {@code br}.
	 */
	private void registerBenchmarkRunnerAndThread(BenchmarkRunner br, Thread t)
	{
		threads.put(br, t);
	}

	/**
	 * Removes the {@code BenchmarkRunner} and corresponding thread.
	 * 
	 * @param br
	 *            The {@code BenchmarkRunner} which should be removed.
	 */
	private void removeBenchmarkRunnerAndThread(BenchmarkRunner br)
	{
		threads.remove(br);
	}

	/**
	 * This method have to be called after all BenchmarkRunners finished without
	 * problems.
	 */
	private void signalSuccess()
	{
		restartCounter = 0;
		experimentCounter++;
		threads.clear();
		strat.signalSuccess();
	}

	/**
	 * Restarts all {@code BenchmarkRunner}s.
	 */
	private void restartRoutine()
	{
		// sync = new CyclicBarrier(sutCount);
		restartCounter++;
		Set<BenchmarkRunner> brs;
		brs = threads.keySet();
		ExecutorService exec = Executors.newFixedThreadPool(brs.size());

		try
		{
			for (final BenchmarkRunner br : brs)
			{
				exec.submit(new Runnable()
				{
					@Override
					public void run()
					{
						Thread tmp = threads.get(br);
						br.stopExperiment();

						try
						{
							LOGGER.debug("Waiting for all restart threads");
							threadSynchronization.await();
						} catch (InterruptedException e)
						{
						} catch (BrokenBarrierException e)
						{
						}

						tmp = new Thread(br, experimentCounter + "H" + restartCounter + "-" + br.sutId);
						threads.put(br, tmp);
						tmp.start();
					}
				});
			}
		} finally
		{
			exec.shutdown();
			threadSynchronization.reset();
			// interruptBenchmarks = false;
		}
	}

	/**
	 * Sets the value of the interrupt flag. If the interrupt flag is true, the
	 * {@code BenchmarkRunner}s skip their following work at the first
	 * possibility.
	 * 
	 * @param b
	 *            {@code true} when a benchmark malfunction appeared and the
	 *            {@code BenchmarkRunner}s should skip their following work. Set
	 *            {@code false} otherwise or to reset the flag.
	 */
	public synchronized void setInterruptFlag(boolean b)
	{
		interruptBenchmarks = b;
	}

	/**
	 * Returns the interrupt flag.
	 * 
	 * @return {@code true} when the {@code BenchmarkRunner}s should skip their
	 *         following work, {@code false} otherwise.
	 */
	public synchronized boolean getInterruptFlag()
	{
		return interruptBenchmarks;
	}

	/**
	 * Output a verification HTML file which can be used to quickly check which
	 * experiments are defined in this configuration.
	 * 
	 * @param outputFile
	 *            The output file where the HTML file should be saved to, will
	 *            be overridden if exists.
	 */
	public void verify(String outputFile)
	{
		try
		{
			TablePrinter.printAsTable(experimentsForSut, outputFile);
		} catch (IOException e)
		{
			LOGGER.error("IOException", e);
		}
	}

	/**
	 * Loads a configuration from the XMI-File specified as parameter
	 * {@code confFile}. The configuration is validated using OCL constraints
	 * specified in the ECore Model. If the validation fails, an exception is
	 * thrown. Otherwise the parsed configuration is returned.
	 * 
	 * @param confFile
	 *            The config-file which should be loaded.
	 * @return A {@code ExperimentSetup} instance containing every information
	 *         needed to run the benchmarks.
	 * @throws BenchmarkException
	 *             if the configuration is invalid.
	 */
	public static ExperimentSetup loadConfigurationFromXMI(String confFile)
	{
		ResourceSet load_resourceSet = new ResourceSetImpl();
		load_resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put("*", new XMIResourceFactoryImpl());
		load_resourceSet.getPackageRegistry().put(SBHModelPackage.eNS_URI, SBHModelPackage.eINSTANCE);
		Resource load_resource = load_resourceSet.getResource(URI.createURI(confFile), true);

		// Add OCL-Validators to EMF
		String oclDelegateURI = OCLDelegateDomain.OCL_DELEGATE_URI;
		EOperation.Internal.InvocationDelegate.Factory.Registry.INSTANCE.put(oclDelegateURI, new OCLInvocationDelegateFactory.Global());
		EStructuralFeature.Internal.SettingDelegate.Factory.Registry.INSTANCE.put(oclDelegateURI, new OCLSettingDelegateFactory.Global());
		EValidator.ValidationDelegate.Registry.INSTANCE.put(oclDelegateURI, new OCLValidationDelegateFactory.Global());

		EValidator.Registry.INSTANCE.put(SBHModelPackage.eINSTANCE, new EcoreValidator());
		BasicDiagnostic diagnostics = new BasicDiagnostic();

		LOGGER.debug("Validating Objects:");
		for (EObject eObj : ImmutableList.copyOf(load_resource.getAllContents()))
		{

			validate(eObj, diagnostics);
		}

		// Output Error-Messages if errors occured
		if (diagnostics.getSeverity() != Diagnostic.OK)
		{
			LOGGER.error("Validation of %s failed:", confFile);
			for (Diagnostic d : diagnostics.getChildren())
			{
				LOGGER.error("%s", d.getMessage());
			}
			throw new BenchmarkException("Configuration not valid");
		}

		if (load_resource.getContents().size() != 1)
		{
			LOGGER.error("Configuration invalid: There must be one root element, not %d", load_resource.getContents().size());
			throw new BenchmarkException("Configuration not valid");
		}

		EObject o = load_resource.getContents().get(0);

		if (!(o instanceof Configuration))
		{
			LOGGER.error("Configuration invalid: Root Element is %s not 'Configuration'", o);
			throw new BenchmarkException("Rootelement is not 'Configuration'");
		}

		// Start to evaluate Configuration
		Configuration conf = (Configuration) o;

		if (conf.getExperimentSetup() == null)
		{
			LOGGER.error("Configuration invalid: Configuration does not contain a ExperimentSetups");
			throw new BenchmarkException("Configuration does not contain a ExperimentSetups");
		}

		return conf.getExperimentSetup();
	}

	/**
	 * Recursively validates a EObject by following all references.
	 * 
	 * @param obj
	 *            The EObject which should be validated
	 * @param diagChain
	 *            Validating DiagnosticChain
	 */
	private static void validate(EObject obj, DiagnosticChain diagChain)
	{
		LOGGER.debug("Validating %s", obj);
		Map<Object, Object> context = Maps.newHashMap();
		Diagnostician.INSTANCE.validate(obj, diagChain, context);

		Iterator<EObject> eoIter = obj.eCrossReferences().iterator();

		while (eoIter.hasNext())
		{
			EObject eoSub = eoIter.next();
			validate(eoSub, diagChain);
		}
	}

	private CyclicBarrier threadSynchronization;
	private CountDownLatch threadFinished;

	/**
	 * This thread does the actual work: One thread is created per host where
	 * experiments should run. Multiple threads synchronize using the
	 * 'threadSynchronization' barrier. The 'threadFinished' latch is used by
	 * the main thread to wait for the finish of all threads.
	 * 
	 * @author dominik, Axel Busch (axel.busch@student.kit.edu)
	 * 
	 */
	private final class BenchmarkRunner implements Runnable
	{
		/**
		 * A thread which executed a single experiment on a host. The results of
		 * the run are saved in the member variables. This thread is used for
		 * synchronized execution of the experiments.
		 * 
		 * @author dominik, Axel Busch (axel.busch@student.kit.edu)
		 * 
		 */
		private final String sutId;
		private final DriverAndIndependentVars exp;

		/**
		 * Construct a new thread for synchronized execution.
		 * 
		 * @param sutId
		 *            A identifier for the SUT this Thread runs on
		 * @param experiment
		 *            Experiment which sould be executed.
		 */
		public BenchmarkRunner(String sutId, DriverAndIndependentVars experiment)
		{
			this.sutId = sutId;
			this.exp = experiment;
		}

		@Override
		public void run()
		{
			try
			{
				LOGGER.debug("Running experiment on SuT %s.", sutId);
				LOGGER.debug("Configuration: %s/%s.", exp.getSutVars(), exp.getBenchVars());

				// Wait for prepare
				LOGGER.debug("Waiting for barrier for preparation");
				for (int repeatNo = 1; repeatNo <= repeatCount; repeatNo++)
				{
					LOGGER.debug("Repeat %d/%d", repeatNo, repeatCount);

					boolean initPrepareExp = false; // indicates if initial
													// experiment
													// preparation
													// was done
					threadSynchronization.await();
					if (!getInterruptFlag())
					{
						if (!initPrepareExp || repeatWarmup)
						{ // Prepare experiment at least in the first iteration
							LOGGER.debug("Preparing experiment");

							threadSynchronization.await();
							try
							{
								exp.getBenchmarkDriver().prepareExperiment(exp.getExpUid(), exp.getSutVars(), exp.getBenchVars());
							} catch (CheckedBenchmarkException e)
							{
								setInterruptFlag(true);
							}
						}

						initPrepareExp = true;
					}

					threadSynchronization.await();
					if (!getInterruptFlag())
					{
						// Waiting for start monitoring
						if (exp.getMonitorDriver() != null)
						{
							LOGGER.debug("Waiting for start monitoring");
							for (int i = 0; i < exp.getMonitorDriver().size(); ++i)
								exp.getMonitorDriver().get(i).startMonitoring(exp.getExpUid(), exp.getSutVars(), exp.getMonitorVars().get(i));
						}
					}

					DependentVariables benchmarkResults = null;
					threadSynchronization.await();
					if (!getInterruptFlag())
					{
						LOGGER.debug("Waiting for finishing of starting monitors");
						LOGGER.debug("Starting Benchmarking");
						threadSynchronization.await();
						try
						{
							benchmarkResults = exp.getBenchmarkDriver().startExperiment(repeatNo);
						} catch (CheckedBenchmarkException e)
						{
							setInterruptFlag(true);
						}
					}
					
					List<DependentVariables> results = Lists.newArrayList();
					threadSynchronization.await();
					if (!getInterruptFlag())
					{
						if (benchmarkResults != null)
						{
							results.add(benchmarkResults);
							LOGGER.debug("Finished Benchmarking");
						} else
						{
							LOGGER.debug("Finished Benchmarking. No Results.");
						}
					}
					
					threadSynchronization.await();
					if (!getInterruptFlag())
					{
						LOGGER.debug("Finished Monitoring");
						LOGGER.debug("Finishing Experiment");

						// Saving results
						LOGGER.debug("Result-Saving-Phase: %d Results", results.size());

						if (exp.getMonitorDriver() != null)
						{
							for (int i = 0; i < exp.getMonitorDriver().size(); ++i)
							{
								String prefix = "";

								if (benchmarkResults != null)
									prefix = benchmarkResults.getBenchmarkPrefix();
								results.add(exp.getMonitorDriver().get(i)
										.stopMonitoring(exp.getExpUid(), exp.getMonitorVars().get(i), repeatNo, prefix));
								// exp.getMonitorDriver().get(i).endMonitoring();
							}
						}
					}
					
					threadSynchronization.await();
					if (!getInterruptFlag())
					{
						if (benchmarkResults != null)
						{
							LOGGER.debug("Result-Saving-Phase: %d Results", results.size());
							datastore.storeExperimentResults(experimentCounter, sutId, exp.getBenchmarkDriver().getClass().getSimpleName(), repeatNo,
									exp.getExpUid(), exp.getSutVars(), exp.getBenchVars(), results);
							LOGGER.debug("Saved results");
						} else
						{
							--repeatNo;
							LOGGER.debug("Rescheduling Benchmark run");
						}
					}
				}

				// Finishing
				LOGGER.debug("Waiting for finish");
				LOGGER.debug("Finishing Experiment");

				cleanup();

			} catch (Exception e)
			{
				// This is a pokemon exception (catch all) because all other
				// threads need to be stopped in case of a error.
				LOGGER.error("Exception in Thread", e);

				Thread.currentThread().interrupt();
				try
				{
					threadSynchronization.await();
				} catch (Exception ie)
				{
				}
			} finally
			{
				threadFinished.countDown();
			}
		}

		/**
		 * Ends the monitors and finishs the experiment.
		 */
		private void cleanup()
		{
			System.out.println();
			if (exp.getMonitorDriver() != null)
				for (int i = 0; i < exp.getMonitorDriver().size(); ++i)
					exp.getMonitorDriver().get(i).endMonitoring();
			exp.getBenchmarkDriver().endExperiment();
		}

		/**
		 * Stops the current experiment of this BenchmarkRunner.
		 */
		public void stopExperiment()
		{
			exp.getBenchmarkDriver().stopCurrentProcess();
		}
	}
}
