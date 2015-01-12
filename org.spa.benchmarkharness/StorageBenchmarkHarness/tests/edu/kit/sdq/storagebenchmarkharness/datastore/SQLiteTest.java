package edu.kit.sdq.storagebenchmarkharness.datastore;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.google.common.collect.Lists;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueComposite;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.FileSystem;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Fileset;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFFSB;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFilebench;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Metric;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Scheduler;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Thread;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Type;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.OperationsFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.Read;
import edu.kit.sdq.storagebenchmarkharness.datastore.sqlite.SQLiteDataStore;

public class SQLiteTest
{
	private String tempFile;
	private static final Logger LOGGER = Logger.getLogger(SQLiteTest.class);

	@Before
	public void setup()
	{
		try
		{
			tempFile = File.createTempFile("TestDataBase", ".sqlite").getAbsolutePath();
			LOGGER.debug("TempFile is %s", tempFile);
		} catch (IOException e)
		{
			LOGGER.error("Could not create tempfile", e);
		}

		((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("edu.kit.sdq.storagebenchmarkharness")).setLevel(Level.TRACE);
	}

	@Test
	public void testSetup()
	{
		SQLiteDataStore ds = new SQLiteDataStore(tempFile);
		ds.setupDataStore();
		ds.closeDataStore();
	}

	@Test
	public void testConfRun()
	{
		SQLiteDataStore ds = new SQLiteDataStore(tempFile);
		ds.setupDataStore();
		ds.storeConfigurationRun("My First Identifier");
		ds.finishConfigurationRun();
		ds.closeDataStore();
	}

	@Test
	public void testEmptyResultSave() throws SQLiteException
	{
		SQLiteDataStore ds = new SQLiteDataStore(tempFile);
		ds.setupDataStore();
		ds.storeConfigurationRun("My First Identifier");

		IndependentVariablesOfFFSB expBenchVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		expBenchVars.setReadPercentage(100);
		expBenchVars.setReadBlockSize(32);
		expBenchVars.setWriteBlockSize(64);
		expBenchVars.setFilesetSize(100);

		IndependentVariablesOfSut expSutVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		expSutVars.setFileSystem(FileSystem.EXT4);
		expSutVars.setScheduler(Scheduler.NOOP);

		String hostId = String.format("hostId_%d", (int) (Math.random() * 1000));

		DependentVariables depVars = SBHModelFactory.eINSTANCE.createDependentVariables();
		depVars.setBenchmarkPrefix("ffsb");
		List<DependentVariables> resultList = Lists.newArrayList();
		resultList.add(depVars);

		ds.storeExperimentResults(0, hostId, "FFSBBenchmark", 1, "testID", expSutVars, expBenchVars, resultList);

		ds.finishConfigurationRun();
		ds.closeDataStore();

		// Check database contains row
		SQLiteConnection db = new SQLiteConnection(new File(tempFile));
		db.open(false);

		// Run-Table
		SQLiteStatement stmt = db.prepare("SELECT count(runId) FROM runs WHERE hostId=?;");
		stmt.bind(1, hostId);
		stmt.step();
		Assert.assertEquals(1, stmt.columnInt(0));
		stmt.dispose();

		// IndependentVars
		SQLiteStatement stmt2 = db.prepare("SELECT count(runId) FROM ffsbIndependentVars;");
		stmt2.step();
		Assert.assertEquals(1, stmt2.columnInt(0));
		stmt2.dispose();

		db.dispose();
	}

	@Test
	public void testResultSave()
	{
		SQLiteDataStore ds = new SQLiteDataStore(tempFile);
		ds.setupDataStore();
		ds.storeConfigurationRun("My First Identifier");

		IndependentVariablesOfFFSB expBenchVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		expBenchVars.setReadPercentage(100);
		expBenchVars.setReadBlockSize(32);
		expBenchVars.setWriteBlockSize(64);
		expBenchVars.setFilesetSize(100);

		IndependentVariablesOfSut expSutVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		expSutVars.setFileSystem(FileSystem.EXT4);
		expSutVars.setScheduler(Scheduler.NOOP);

		DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();
		result.getValues().clear();
		result.setBenchmarkPrefix("ffsb");
		DependentVariablesValueComposite value = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
		value.setOperation("write");
		value.setType(Type.ABSOLUTE);
		value.setOperationMetric(Metric.RESPONSE_TIME);
		value.setValue(1.23);
		result.getValues().add(value);
		List<DependentVariables> resultList = Lists.newArrayList();
		resultList.add(result);

		ds.storeExperimentResults(0, "hostId", "FFSBBenchmark", 1, "testId", expSutVars, expBenchVars, resultList);

		ds.finishConfigurationRun();
		ds.closeDataStore();
	}

	@Test
	public void testResultSaveFast()
	{
		SQLiteDataStore ds = new SQLiteDataStore(tempFile);
		ds.setupDataStore();
		ds.storeConfigurationRun("My First Identifier");

		IndependentVariablesOfFFSB expBenchVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		expBenchVars.setReadPercentage(100);
		expBenchVars.setReadBlockSize(32);
		expBenchVars.setWriteBlockSize(64);
		expBenchVars.setFilesetSize(100);

		IndependentVariablesOfSut expSutVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		expSutVars.setFileSystem(FileSystem.EXT4);
		expSutVars.setScheduler(Scheduler.NOOP);

		DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();
		result.getValues().clear();
		result.setBenchmarkPrefix("ffsb");
		DependentVariablesValueComposite value = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
		value.setOperation("write");
		value.setOperationMetric(Metric.RESPONSE_TIME);
		value.setType(Type.ABSOLUTE);
		value.setValue(1.23);
		result.getValues().add(value);

		List<DependentVariables> resultList = Lists.newArrayList();
		resultList.add(result);

		for (int i = 0; i <= 10; i++)
		{
			ds.storeExperimentResults(0, "hostId", "FFSBBenchmark", i, "testId", expSutVars, expBenchVars, resultList);
		}

		ds.finishConfigurationRun();
		ds.closeDataStore();
	}

	@Test
	public void testResultSaveFilebench()
	{
		SQLiteDataStore ds = new SQLiteDataStore(tempFile);
		ds.setupDataStore();
		ds.storeConfigurationRun("Filebench Test");

		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.CFQ);

		IndependentVariablesOfFilebench filebenchVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFilebench();
		filebenchVar.setName("filemicro_rread");
		filebenchVar.setRunTime(10);
		Fileset fileset = SBHModelFactory.eINSTANCE.createFileset();
		fileset.setFilesetName("bigfile1");
		fileset.setDirectory(new File("/tmp"));
		fileset.setMeanFileSize("1g");
		fileset.setMeanDirWidth(1);
		fileset.setPrealloc(100);
		fileset.setFiles(1);
		filebenchVar.getFilesets().add(fileset);

		Thread thread = SBHModelFactory.eINSTANCE.createThread();
		thread.setInstances(1);
		thread.setMemsize("10m");
		thread.setThreadName("filereaderthread");

		Read readOper = OperationsFactory.eINSTANCE.createRead();
		readOper.setOperationName("write-file");
		readOper.setFlowOpName("read");
		readOper.setFilesetname(fileset.getFilesetName());
		readOper.setRandom(1);
		readOper.setDirectio(1);
		readOper.setIosize("2k");
		readOper.setIters("1");

		thread.getOperations().add(readOper);
		filebenchVar.getThreads().add(thread);

		DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();
		result.getValues().clear();
		result.setBenchmarkPrefix("filebench");
		DependentVariablesValueComposite value = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
		value.setOperation("write");
		value.setOperationMetric(Metric.RESPONSE_TIME);
		value.setType(Type.ABSOLUTE);
		value.setValue(1.23);
		result.getValues().add(value);
		List<DependentVariables> resultList = Lists.newArrayList();
		resultList.add(result);

		for (int i = 0; i <= 10; i++)
		{
			ds.storeExperimentResults(0, "hostId", "FilebenchBenchmark", i, "testId", sutVar, filebenchVar, resultList);
		}

		ds.finishConfigurationRun();
		ds.closeDataStore();
	}
}
