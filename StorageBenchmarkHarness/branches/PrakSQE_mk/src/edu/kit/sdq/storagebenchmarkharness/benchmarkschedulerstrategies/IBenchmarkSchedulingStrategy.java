package edu.kit.sdq.storagebenchmarkharness.benchmarkschedulerstrategies;

import java.util.List;
import java.util.Map;

import edu.kit.sdq.storagebenchmarkharness.ExperimentSeriesHelper.DriverAndIndependentVars;

public interface IBenchmarkSchedulingStrategy
{
	/**
	 * Adds all experiments of one SuT to the {@code SchedulingStrategy} at once.
	 * 
	 * @param experimentsForSut is a Map<String, List<DriverAndIndependentVars>> in which is the key-value 
	 * the SuT-ID. The List contains all experiments of the SuT.
	 */
	public void addExperiment(Map<String, List<DriverAndIndependentVars>> experimentsForSut);
	/**
	 * Adds a single experiment of a Sut.
	 *  
	 * @param sutId ID of SuT. 
	 * @param l Experiment which should be added.
	 */
	public void addExperiment(String sutId, DriverAndIndependentVars l);
	
	/**
	 * Resets the Scheduling Strategy to the initial state.
	 */
	public void reset();
	
	/**
	 * Gets the next experiment for the specified SuT. 
	 * @param sutId ID of SuT for which an experiment should be returned.
	 * @return The next experiment for the SuT. 
	 */
	public DriverAndIndependentVars getNextExperiment(String sutId);
	
	/**
	 * Signals that all {@code BenchmarkRunner}s have finished without 
	 * problem and the Strategy can go to the next experiments.
	 */
	public void signalSuccess();
	
	/**
	 * Is asked to determine if there are more experiments to execute.
	 * 
	 * @return Returns {@code true} when there are more experiments, {@code false} otherwise.
	 */
	public boolean hasNextExperiment();
}
