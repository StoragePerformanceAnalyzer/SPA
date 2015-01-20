package edu.kit.sdq.storagebenchmarkharness.benchmarks.postmark;

import edu.kit.sdq.storagebenchmarkharness.BenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueComposite;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfPostmark;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;

/**
 * This is currently only a demonstrator for a second benchmark. It contains no
 * actual implementation!!
 * 
 * @author Dominik Bruhn, Axel Busch (axel.busch@student.kit.edu)
 * 
 */
public final class PostmarkBenchmarkDriver extends BenchmarkDriver<IndependentVariablesOfPostmark, DependentVariables>
{
	private static final Logger LOGGER = Logger.getLogger(PostmarkBenchmarkDriver.class);

	private IndependentVariablesOfPostmark independentVariables;

	private String targetDir;

	public String getTargetDir()
	{
		return targetDir;
	}

	public PostmarkBenchmarkDriver(RemoteConnection con, String rawFileSaveDir)
	{
		super(con, rawFileSaveDir);
	}

	@Override
	public void prepareExperiment(IndependentVariablesOfSut sutVars, IndependentVariablesOfPostmark benchVars)
	{
		this.independentVariables = benchVars;
	}

	@Override
	public DependentVariables startExperiment(int repeatNo)
	{
		LOGGER.debug("Postmark was started with parameters %s (#%d)", independentVariables.getApplicationName(), repeatNo);
		DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();
		result.getValues().clear();
		result.getValues().add(createResult(Math.random() * 1000d));

		return result;
	}

	private static DependentVariablesValueComposite createResult(double runtime)
	{
		DependentVariablesValueComposite r = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
		r.setValue(runtime);
		return r;
	}
}
