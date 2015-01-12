package edu.kit.sdq.storagebenchmarkharness.benchmarkschedulerstrategies;

import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;

import edu.kit.sdq.storagebenchmarkharness.ExperimentSeriesHelper.DriverAndIndependentVars;

/**
 * Scheduling strategy in which the sequence of the experiments depends 1. on
 * the lexicographical order of the attributes 2. on the input order of the
 * attribute values.
 * 
 * e.g. The FFSB benchmark has the attributes and values:
 * "File Size: {512, 1024, 128}" and "Runtime: {30, 20}"
 * Then the sequence of the experiments would be (File Size, Runtime):
 * (512,30),(1024,30),(128,30),(512,20)(1024,20),(128,20)
 * 
 * @author Michael Kaufmann
 */
public class SimpleStrat extends ASchedulingStrategy
{

	private int startIndex, curIdx;

	@Override
	public void reset()
	{
		// experiments = Maps.newHashMap();
		curIdx = startIndex;
	}

	@Override
	public DriverAndIndependentVars getNextExperiment(String sutId)
	{
		List<DriverAndIndependentVars> tmp = experiments.get(sutId);
		return tmp.get(curIdx);
	}

	@Override
	public boolean hasNextExperiment()
	{
		return curIdx < expCount;
	}

	@Override
	public void signalSuccess()
	{
		curIdx++;
	}

}
