package edu.kit.sdq.storagebenchmarkharness.datastore;

import java.util.List;

import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBenchmark;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.exceptions.DataStoreException;

public interface DataStore
{
	public void setupDataStore() throws DataStoreException;

	public void storeConfigurationRun(String identifier);

	public void storeExperimentResults(int expNo, String hostIdentifier, String benchmarkId, int repeatNo, String expUid,
			IndependentVariablesOfSut sutVars, IndependentVariablesOfBenchmark benchVars, List<DependentVariables> dependentVars);

	public void finishConfigurationRun();

	public void closeDataStore();
}