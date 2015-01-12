package edu.kit.sdq.storagebenchmarkharness;

import static edu.kit.sdq.storagebenchmarkharness.TestUtils.assertClassEquals;
import static edu.kit.sdq.storagebenchmarkharness.TestUtils.assertEObjectEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.List;

import org.junit.Test;

import edu.kit.sdq.storagebenchmarkharness.ExperimentSeriesHelper.DriverAndIndependentVars;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.FileSystem;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBenchmark;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFFSB;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFilebench;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Scheduler;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.ConfigurationFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.ExperimentSeries;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.Fileset;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.Thread;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.Operations.Read;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariableSpaceOfFFSB;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariableSpaceOfFilebench;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariableSpaceOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.OperationsFactory;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.ffsb.FFSBenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.filebench.FilebenchBenchmarkDriver;

public class ExperimentSeriesHelperTest
{
	private static final Logger LOGGER = Logger.getLogger(ExperimentSeriesHelperTest.class);

	private static ExperimentSeries getSeries1(boolean leaveTimeOut, boolean multiValue)
	{
		IndependentVariableSpaceOfFFSB ivsBench = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfFFSB();
		ivsBench.getReadPercentage().add(100);
		ivsBench.getReadBlockSize().add(32);
		ivsBench.getWriteBlockSize().add(64);
		if (multiValue)
		{
			ivsBench.getWriteBlockSize().add(128);
		}
		ivsBench.getFilesetSize().add(100);
		ivsBench.getSequentialRead().add(true);
		ivsBench.getSequentialWrite().add(true);
		if (multiValue)
		{
			ivsBench.getSequentialWrite().add(false);
		}
		ivsBench.getThreadCount().add(100);
		ivsBench.getWriteFsync().add(false);
		ivsBench.getFileSize().add(4 * 1024);
		ivsBench.getOpsPerFile().add(256);
		if (!leaveTimeOut)
		{
			ivsBench.getRunTime().add(60);
			ivsBench.getWarmUpTime().add(60);
		}
		ivsBench.getDirectIO().add(true);

		IndependentVariableSpaceOfSut ivsHost = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfSut();
		ivsHost.getFileSystem().add(FileSystem.EXT4);
		ivsHost.getScheduler().add(Scheduler.NOOP);

		ExperimentSeries series = ConfigurationFactory.eINSTANCE.createExperimentSeries();
		series.setIndependentVariableSpaceOfBenchmark(ivsBench);
		series.setIndependentVariableSpaceOfSut(ivsHost);

		return series;
	}

	private static ExperimentSeries getFilebenchSeries1()
	{
		IndependentVariableSpaceOfFilebench ivsBench = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfFilebench();
		ivsBench.getRunTime().add(10);
		ivsBench.setName("Testbench");
		Fileset fileset1 = ConfigurationFactory.eINSTANCE.createFileset();
		fileset1.setFilesetName("bigfile1");
		fileset1.getMeanFileSize().add("1g");
		fileset1.getMeanDirWidth().add(1);
		fileset1.getFiles().add(1);
		fileset1.getDirectory().add(new File("/tmp"));
		fileset1.getPrealloc().add(100);
		ivsBench.getFilesets().add(fileset1);

		Thread thread = ConfigurationFactory.eINSTANCE.createThread();
		thread.getInstances().add(1);
		thread.getMemsize().add("10m");
		thread.getMemsize().add("100m");
		thread.setThreadName("filereaderthread");

		Read readOper = edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.Operations.OperationsFactory.eINSTANCE.createRead();
		readOper.setOperationName("write-file");
		readOper.setFlowOpName("read");
		readOper.setFilesetname(fileset1.getFilesetName());
		readOper.getRandom().add(1);
		readOper.getDirectio().add(1);
		readOper.getIosize().add("2k");
		readOper.getIosize().add("4k");
		readOper.getIters().add("20");
		readOper.getIters().add("30");
		Read readOper2 = edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.Operations.OperationsFactory.eINSTANCE.createRead();
		readOper2.setOperationName("write-file");
		readOper2.setFlowOpName("read");
		readOper2.setFilesetname(fileset1.getFilesetName());
		readOper2.getRandom().add(1);
		readOper2.getDirectio().add(1);
		readOper2.getIosize().add("2k");
		readOper2.getIters().add("1");
		readOper2.getIters().add("10");

		thread.getOperations().add(readOper);
		thread.getOperations().add(readOper2);
		ivsBench.getThreads().add(thread);

		IndependentVariableSpaceOfSut ivsHost = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfSut();
		ivsHost.getFileSystem().add(FileSystem.EXT4);
		ivsHost.getScheduler().add(Scheduler.NOOP);

		ExperimentSeries series = ConfigurationFactory.eINSTANCE.createExperimentSeries();
		series.setIndependentVariableSpaceOfBenchmark(ivsBench);
		series.setIndependentVariableSpaceOfSut(ivsHost);

		return series;
	}

	@Test
	// No values at all
	public void testGetExperimentsEmpty()
	{
		IndependentVariableSpaceOfFFSB ivsBench = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfFFSB();
		IndependentVariableSpaceOfSut ivsHost = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfSut();

		ExperimentSeries series = ConfigurationFactory.eINSTANCE.createExperimentSeries();
		series.setIndependentVariableSpaceOfBenchmark(ivsBench);
		series.setIndependentVariableSpaceOfSut(ivsHost);

		List<DriverAndIndependentVars> exps = ExperimentSeriesHelper.getExperiments(series, null, null, null);

		assertEquals(1, exps.size());

		IndependentVariableSpaceOfFilebench ivsBench2 = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfFilebench();

		ExperimentSeries series2 = ConfigurationFactory.eINSTANCE.createExperimentSeries();
		series2.setIndependentVariableSpaceOfBenchmark(ivsBench2);
		series2.setIndependentVariableSpaceOfSut(ivsHost);

		exps = ExperimentSeriesHelper.getExperiments(series2, null, null, null);

		assertEquals(1, exps.size());
	}

	@Test
	// Some values, some empty
	public void testGetExperimentsSomeEmpty()
	{
		IndependentVariableSpaceOfFFSB ivsBench = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfFFSB();
		ivsBench.getReadBlockSize().add(64 * 1024);
		ivsBench.getWriteBlockSize().add(64 * 1024);
		ivsBench.getWriteBlockSize().add(128 * 1024);

		IndependentVariableSpaceOfSut ivsHost = ConfigurationFactory.eINSTANCE.createIndependentVariableSpaceOfSut();

		ExperimentSeries series = ConfigurationFactory.eINSTANCE.createExperimentSeries();
		series.setIndependentVariableSpaceOfBenchmark(ivsBench);
		series.setIndependentVariableSpaceOfSut(ivsHost);

		List<DriverAndIndependentVars> exps = ExperimentSeriesHelper.getExperiments(series, null, null, null);

		assertEquals(2, exps.size());

		IndependentVariablesOfFFSB ffsbVars = (IndependentVariablesOfFFSB) exps.get(0).getBenchVars();
		assertEquals(null, ffsbVars.getFilesetSize());
		assertEquals(null, ffsbVars.getSequentialRead());

		// assertEquals(null, exps.get(0).getSutVars().getFileSystem());
	}

	@Test
	public void testGetExperiments1()
	{
		List<DriverAndIndependentVars> exps = ExperimentSeriesHelper.getExperiments(getSeries1(false, false), null, null, null);

		IndependentVariablesOfFFSB expBenchVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		expBenchVars.setReadPercentage(100);
		expBenchVars.setReadBlockSize(32);
		expBenchVars.setWriteBlockSize(64);
		expBenchVars.setFilesetSize(100);
		expBenchVars.setSequentialRead(true);
		expBenchVars.setSequentialWrite(true);
		expBenchVars.setThreadCount(100);
		expBenchVars.setWriteFsync(false);
		expBenchVars.setFileSize(4 * 1024);
		expBenchVars.setOpsPerFile(256);
		expBenchVars.setRunTime(60);
		expBenchVars.setWarmUpTime(60);
		expBenchVars.setDirectIO(true);

		IndependentVariablesOfSut expSutVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		expSutVars.setFileSystem(FileSystem.EXT4);
		expSutVars.setScheduler(Scheduler.NOOP);

		assertEquals(1, exps.size());
		assertEObjectEquals(expSutVars, exps.get(0).getSutVars());
		assertEObjectEquals(expBenchVars, exps.get(0).getBenchVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(0).getBenchmarkDriver());
	}

	@Test
	public void testGetExperiments2()
	{
		ExperimentSeries series = getSeries1(false, true);

		List<DriverAndIndependentVars> exps = ExperimentSeriesHelper.getExperiments(series, null, null, null);

		assertEquals(4, exps.size());

		IndependentVariablesOfFFSB expBenchVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		expBenchVars.setReadPercentage(100);
		expBenchVars.setReadBlockSize(32);
		expBenchVars.setWriteBlockSize(64);
		expBenchVars.setFilesetSize(100);
		expBenchVars.setSequentialRead(true);
		expBenchVars.setSequentialWrite(true);
		expBenchVars.setThreadCount(100);
		expBenchVars.setWriteFsync(false);
		expBenchVars.setFileSize(4 * 1024);
		expBenchVars.setOpsPerFile(256);
		expBenchVars.setRunTime(60);
		expBenchVars.setWarmUpTime(60);
		expBenchVars.setDirectIO(true);

		IndependentVariablesOfSut expSutVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		expSutVars.setFileSystem(FileSystem.EXT4);
		expSutVars.setScheduler(Scheduler.NOOP);

		assertEObjectEquals(expSutVars, exps.get(0).getSutVars());
		assertEObjectEquals(expBenchVars, exps.get(0).getBenchVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(0).getBenchmarkDriver());

		expBenchVars.setWriteBlockSize(64);
		expBenchVars.setSequentialWrite(false);
		for (int i = 0; i < exps.size(); ++i)
			System.out.println(exps.get(i).getBenchVars());
		assertEObjectEquals(expSutVars, exps.get(1).getSutVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(1).getBenchmarkDriver());

		expBenchVars.setWriteBlockSize(128);
		expBenchVars.setSequentialWrite(true);
		assertEObjectEquals(expSutVars, exps.get(2).getSutVars());
		assertEObjectEquals(expBenchVars, exps.get(2).getBenchVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(2).getBenchmarkDriver());

		expBenchVars.setWriteBlockSize(128);
		expBenchVars.setSequentialWrite(false);
		assertEObjectEquals(expSutVars, exps.get(3).getSutVars());
		assertEObjectEquals(expBenchVars, exps.get(3).getBenchVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(3).getBenchmarkDriver());
	}

	@Test
	public void testGetExperimentsFilebench()
	{
		ExperimentSeries series = getFilebenchSeries1();

		List<DriverAndIndependentVars> exps = ExperimentSeriesHelper.getExperiments(series, null, null, null);

		IndependentVariablesOfFilebench ivsBench = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFilebench();
		ivsBench.setRunTime(10);
		ivsBench.setName("Testbench");

		edu.kit.sdq.storagebenchmarkharness.SBHModel.Fileset fileset1 = SBHModelFactory.eINSTANCE.createFileset();
		fileset1.setFilesetName("bigfile1");
		fileset1.setMeanFileSize("1g");
		fileset1.setMeanDirWidth(1);
		fileset1.setFiles(1);
		fileset1.setDirectory(new File("/tmp"));
		fileset1.setPrealloc(100);
		ivsBench.getFilesets().add(fileset1);

		edu.kit.sdq.storagebenchmarkharness.SBHModel.Thread thread = SBHModelFactory.eINSTANCE.createThread();
		thread.setInstances(1);
		thread.setMemsize("10m");
		thread.setThreadName("filereaderthread");

		edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.Read readOper = edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.OperationsFactory.eINSTANCE
				.createRead();
		readOper.setOperationName("write-file");
		readOper.setFlowOpName("read");
		readOper.setFilesetname(fileset1.getFilesetName());
		readOper.setRandom(1);
		readOper.setDirectio(1);
		readOper.setIosize("2k");
		readOper.setIters("1");

		thread.getOperations().add(readOper);
		ivsBench.getThreads().add(thread);

		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.NOOP);

		for (int i = 0; i < exps.size(); ++i)
		{
			IndependentVariablesOfFilebench bv = (IndependentVariablesOfFilebench) exps.get(i).getBenchVars();
			System.out.println(bv);
			for (edu.kit.sdq.storagebenchmarkharness.SBHModel.Thread t : bv.getThreads())
			{
				System.out.println(t);
				for (edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.Operation o : t.getOperations())
				{
					System.out.println(o);
				}
			}
		}

		assertEquals(16, exps.size());
		assertEObjectEquals(sutVar, exps.get(0).getSutVars());
		// assertEObjectEquals(ivsBench, exps.get(0).getBenchVars());
		assertClassEquals(FilebenchBenchmarkDriver.class, exps.get(0).getBenchmarkDriver());
	}

	@Test
	public void testGetExperimentsLeaveOut()
	{
		ExperimentSeries series = getSeries1(true, true);

		List<DriverAndIndependentVars> exps = ExperimentSeriesHelper.getExperiments(series, null, null, null);

		assertEquals(4, exps.size());

		IndependentVariablesOfFFSB expBenchVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		expBenchVars.setReadPercentage(100);
		expBenchVars.setReadBlockSize(32);
		expBenchVars.setWriteBlockSize(64);
		expBenchVars.setFilesetSize(100);
		expBenchVars.setSequentialRead(true);
		expBenchVars.setSequentialWrite(true);
		expBenchVars.setThreadCount(100);
		expBenchVars.setWriteFsync(false);
		expBenchVars.setFileSize(4 * 1024);
		expBenchVars.setOpsPerFile(256);
		expBenchVars.setRunTime(null);
		expBenchVars.setWarmUpTime(null);
		expBenchVars.setDirectIO(true);

		IndependentVariablesOfSut expSutVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		expSutVars.setFileSystem(FileSystem.EXT4);
		expSutVars.setScheduler(Scheduler.NOOP);

		assertEObjectEquals(expSutVars, exps.get(0).getSutVars());
		assertEObjectEquals(expBenchVars, exps.get(0).getBenchVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(0).getBenchmarkDriver());

		expBenchVars.setWriteBlockSize(64);
		expBenchVars.setSequentialWrite(false);
		assertEObjectEquals(expSutVars, exps.get(1).getSutVars());
		assertEObjectEquals(expBenchVars, exps.get(1).getBenchVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(1).getBenchmarkDriver());

		expBenchVars.setWriteBlockSize(128);
		expBenchVars.setSequentialWrite(true);
		assertEObjectEquals(expSutVars, exps.get(2).getSutVars());
		assertEObjectEquals(expBenchVars, exps.get(2).getBenchVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(2).getBenchmarkDriver());

		expBenchVars.setWriteBlockSize(128);
		expBenchVars.setSequentialWrite(false);
		assertEObjectEquals(expSutVars, exps.get(3).getSutVars());
		assertEObjectEquals(expBenchVars, exps.get(3).getBenchVars());
		assertClassEquals(FFSBenchmarkDriver.class, exps.get(3).getBenchmarkDriver());

	}

}
