package edu.kit.sdq.storagebenchmarkharness;

import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfMonitor;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;

/**
 * Provides an interface between a remote monitoring cool and the benchmark
 * controller. Every monitor has several independent variables (or input
 * variables) and several dependent variables (or output variables). These two
 * variables are captured by the generic variables IV and DV.
 * 
 * The sequence of execution is as follows:
 * 
 * <pre>
 * 1. Creation of the MonitorDriver
 * 2. For each Experiment:
 *    2.1 prepareMonitor(Experiment)
 *    2.2 For each repetition:
 *        2.2.1 startMonitor()
 *    2.3 finishMonitor()
 * </pre>
 * 
 * The {@code startMonitoring}, {@code prepareMonitoring} and
 * {@code stopMonitoring} methods needs to be implemented. The
 * {@code endMonitoring} method can be left unimplemented if not functionality
 * is needed.
 * 
 * @author Axel Busch (axel.busch@student.kit.edu)
 * 
 * @param <IV>
 *            The independent variables for this monitor (aka input variables)
 * @param <DV>
 *            The dependent variables for this monitor (aka output variables)
 */
public abstract class MonitorDriver<IV extends IndependentVariablesOfMonitor, DV extends DependentVariables> extends Driver
{
	private static final Logger LOGGER = Logger.getLogger(MonitorDriver.class);
	protected final RemoteConnection connection;

	public MonitorDriver(RemoteConnection con, String logFileSaveDir)
	{
		super(logFileSaveDir);
		this.connection = con;
		LOGGER.debug("LogFileSaveDir is %s", logFileSaveDir);
	}

	/**
	 * Provides the independent variables to the monitor driver. These contain
	 * every information which is necessary to execute the monitor. It is
	 * executed once before startMonitor().
	 * 
	 * @param sutVars
	 *            The independent variables which are not specific for this
	 *            monitor but instead are needed for every monitor.
	 * @param monitorVars
	 *            The independent variables which are specific for this monitor.
	 */
	protected void prepareMonitor()
	{
		// Override if necessary
	}

	/**
	 * This method starts the monitor.
	 * 
	 * @param sutVars
	 *            The independent variables which are not specific for this
	 *            monitor but instead are needed for every monitor.
	 * @param monitorVars
	 *            The independent variables which are specific for this monitor.
	 */
	public abstract void startMonitoring(IndependentVariablesOfSut sutVars, IV monitorVars);

	/**
	 * This method is actually called from the outside. It does some generic
	 * work and then forward to the {@code startMonitoringImpl} method. All
	 * parameters except the {@code expUid} are forwarded to this method.
	 * 
	 * This method contains a workaround for some shortcomings of the java
	 * generic system. In some parts of the benchmark controller, the generic
	 * type of the monitorVars is known but can not be told to the java type
	 * system. This method does the cast to the appropriate type. It then calls
	 * {@code startMonitoring} using the same parameters as provided to this
	 * method.
	 * 
	 * @param expUId
	 *            A unique identifier for this experiment. This identifier is
	 *            used for saving the raw files.
	 * @param sutVars
	 * @param monitorVars
	 */
	@SuppressWarnings("unchecked")
	public void startMonitoring(String expUId, IndependentVariablesOfSut sutVars, IndependentVariablesOfMonitor monitorVars)
	{
		setExpUid(expUId);
		startMonitoring(sutVars, (IV) monitorVars);
	}

	/**
	 * Called after the monitoring process should be completed. Stops recording
	 * and responses the monitoring results. It should return zero or more
	 * results.
	 */
	public abstract DV stopMonitoring(IV monitorVars, int repeatNr, String benchmarkPrefix);

	/**
	 * Called after the monitoring process has been completed. Can be used to
	 * clean up resources and files.
	 */
	public void endMonitoring()
	{
		// Override if necessary
	}

	@SuppressWarnings("unchecked")
	public DV stopMonitoring(String expUID, IndependentVariablesOfMonitor monitorVars, int repeatNr, String benchmarkPrefix)
	{
		return stopMonitoring((IV) monitorVars, repeatNr, benchmarkPrefix);
	}
}
