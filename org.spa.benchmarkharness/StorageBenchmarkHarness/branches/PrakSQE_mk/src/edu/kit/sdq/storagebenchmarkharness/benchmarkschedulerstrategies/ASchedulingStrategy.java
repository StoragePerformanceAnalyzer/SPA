package edu.kit.sdq.storagebenchmarkharness.benchmarkschedulerstrategies;

import java.util.List;
import java.util.Map;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;

import edu.kit.sdq.storagebenchmarkharness.ExperimentSeriesHelper.DriverAndIndependentVars;

/**
 * An abstract class if a scheduling strategy, only the addExperiment(...) methods are implemented. 
 * 
 * @author Michael Kaufmann
 */
public abstract class ASchedulingStrategy implements IBenchmarkSchedulingStrategy
{

	protected Map<String, List<DriverAndIndependentVars>> experiments;

	protected int expCount;

	public ASchedulingStrategy()
	{
		experiments = Maps.newHashMap();
	}

	@Override
	public void addExperiment(Map<String, List<DriverAndIndependentVars>> experimentsForSut)
	{
		experiments.putAll(experimentsForSut);
		for (String sutId : experiments.keySet())
		{
			expCount = experiments.get(sutId).size(); // number of experiments if equal
			break;
		}
	}

	@Override
	public void addExperiment(String sutId, DriverAndIndependentVars l)
	{
		List<DriverAndIndependentVars> t = experiments.get(sutId);
		if (t == null)
			t = Lists.newLinkedList();
		t.add(l);
		experiments.put(sutId, t);
	}
}
