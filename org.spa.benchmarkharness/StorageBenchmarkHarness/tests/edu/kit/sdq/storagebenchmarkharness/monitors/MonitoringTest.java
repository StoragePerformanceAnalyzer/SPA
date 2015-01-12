package edu.kit.sdq.storagebenchmarkharness.monitors;

import static org.junit.Assert.*;

import java.awt.List;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.TestUtils;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValue;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.FileSystem;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Fileset;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBlktrace;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFilebench;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFilesetMonitor;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFilesizeMonitor;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfThreadsMonitor;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Scheduler;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Thread;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.OperationsFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.Read;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.filebench.FilebenchBenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.exceptions.CheckedBenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.monitors.FilesetMonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.monitors.FilesizeMonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.monitors.ThreadsMonitorDriver;

public class MonitoringTest
{
	// @Test
	// public void testStartBlktraceMonitoring()
	// {
	// IndependentVariablesOfSut sutVar =
	// SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
	// sutVar.setFileSystem(FileSystem.EXT4);
	// sutVar.setScheduler(Scheduler.CFQ);
	//
	// IndependentVariablesOfBlktrace blktrace =
	// SBHModelFactory.eINSTANCE.createIndependentVariablesOfBlktrace();
	// blktrace.setBufferSize(999);
	// blktrace.setNumberOfBuffers(30);
	// blktrace.setTargetDevice("/dev/sdb1");
	// blktrace.setLogFilePrefix("test");
	//
	// RemoteConnection con = TestUtils.getLocalhostConnection();
	// con.open();
	// File localResults = new File("/tmp/localResultsBlktrace/");
	// localResults.mkdir();
	//
	// String uuid = UUID.randomUUID().toString();
	//
	// FilebenchBenchmarkDriver filebenchDriver =
	// getFilebenchBenchmarkDriver(sutVar);
	//
	// BlktraceMonitorDriver blktraceDriver = new BlktraceMonitorDriver(con,
	// localResults.getAbsolutePath());
	// blktraceDriver.startMonitoring(uuid, sutVar, blktrace);
	// filebenchDriver.startExperiment(0);
	//
	// DependentVariables result = blktraceDriver.stopMonitoring(blktrace, 0,
	// "filebench");
	// assertNotNull(result.getValues());
	// }

	/**
	 * ATTENTION: This test fails if the system is not configured properly. The
	 * connection data from {@code TestUtils.getLocalhostConnection} must be
	 * valid!
	 * 
	 */
	@Test
	public void testFilesetMonitoring()
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.CFQ);

		IndependentVariablesOfFilesetMonitor ivFSet = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFilesetMonitor();

		// interval in seconds
		ivFSet.setInterval(1);

		RemoteConnection monitorCon = TestUtils.getLocalhostConnection();
		monitorCon.open();

		File localResults = new File("/tmp/localResultsFileset");
		localResults.mkdir();

		String uuid = UUID.randomUUID().toString();

		FilebenchBenchmarkDriver filebenchDriver = getFilebenchBenchmarkDriver(sutVar);
		FilesetMonitorDriver filesetDriver = new FilesetMonitorDriver(monitorCon, localResults.getAbsolutePath(), "/tmp/filebenchtarget/tmp/");
		filesetDriver.startMonitoring(uuid, sutVar, ivFSet);
		
		boolean retry = true;
		while(retry) {
			try
			{
				filebenchDriver.startExperiment(0);
				retry = false;
			} catch (CheckedBenchmarkException e)
			{
				// Do retry
			}
		}

		DependentVariables result = filesetDriver.stopMonitoring(ivFSet, 0, "filebench");
		assertTrue(result.getValues().size() > 0);
		// print output
		java.util.List<DependentVariablesValue> resultList = result.getValues();
		for (DependentVariablesValue elem : resultList)
			System.out.println(elem);
		monitorCon.close();
	}

	/**
	 * ATTENTION: This test fails if the system is not configured properly. The
	 * connection data from {@code TestUtils.getLocalhostConnection} must be
	 * valid!
	 * 
	 */
	@Test
	public void testFilesizeMonitoring()
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.CFQ);

		IndependentVariablesOfFilesizeMonitor ivFSize = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFilesizeMonitor();
		// interval in seconds
		ivFSize.setInterval(1);

		RemoteConnection monitorCon = TestUtils.getLocalhostConnection();
		monitorCon.open();

		File localResults = new File("/tmp/localResultsFileset");
		localResults.mkdir();

		String uuid = UUID.randomUUID().toString();

		FilebenchBenchmarkDriver filebenchDriver = getFilebenchBenchmarkDriver(sutVar);
		FilesizeMonitorDriver filesizeDriver = new FilesizeMonitorDriver(monitorCon, localResults.getAbsolutePath(), "/tmp/filebenchtarget/tmp/");
		filesizeDriver.startMonitoring(uuid, sutVar, ivFSize);
		
		boolean retry = true;
		while(retry) {
			try
			{
				filebenchDriver.startExperiment(0);
				retry = false;
			} catch (CheckedBenchmarkException e)
			{
				// Do retry
			}
		}

		DependentVariables result = filesizeDriver.stopMonitoring(ivFSize, 0, "filebench");

		assertTrue(result.getValues().size() > 0);
		// print output
		java.util.List<DependentVariablesValue> resultList = result.getValues();
		for (DependentVariablesValue elem : resultList)
			System.out.println(elem);
		monitorCon.close();
	}

	/**
	 * ATTENTION: This test fails if the system is not configured properly. The
	 * connection data from {@code TestUtils.getLocalhostConnection} must be
	 * valid!
	 * 
	 */
	@Test
	public void testThreadsMonitoring()
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.CFQ);

		IndependentVariablesOfThreadsMonitor ivThreads = SBHModelFactory.eINSTANCE.createIndependentVariablesOfThreadsMonitor();
		ivThreads.setBenchmarkPrefix("filebench");

		RemoteConnection monitorCon = TestUtils.getLocalhostConnection();
		monitorCon.open();

		File localResults = new File("/tmp/localResultsFileset");
		localResults.mkdir();

		String uuid = UUID.randomUUID().toString();

		FilebenchBenchmarkDriver filebenchDriver = getFilebenchBenchmarkDriver(sutVar);
		ThreadsMonitorDriver threadsDriver = new ThreadsMonitorDriver(monitorCon, localResults.getAbsolutePath());
		threadsDriver.startMonitoring(uuid, sutVar, ivThreads);
		
		boolean retry = true;
		while(retry) {
			try
			{
				filebenchDriver.startExperiment(0);
				retry = false;
			} catch (CheckedBenchmarkException e)
			{
				// Do retry
			}
		}

		DependentVariables result = threadsDriver.stopMonitoring(ivThreads, 0, "filebench");

		assertTrue(result.getValues().size() > 0);

		// print output
		java.util.List<DependentVariablesValue> resultList = result.getValues();
		for (DependentVariablesValue elem : resultList)
			System.out.println(elem);
		monitorCon.close();
	}

	public static FilebenchBenchmarkDriver getFilebenchBenchmarkDriver(IndependentVariablesOfSut sutVar)
	{
		IndependentVariablesOfFilebench filebenchVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFilebench();
		filebenchVar.setName("filemicro_rread");
		filebenchVar.setRunTime(1);
		Fileset fileset = SBHModelFactory.eINSTANCE.createFileset();
		fileset.setFilesetName("bigfile1");
		fileset.setDirectory(new File("/tmp"));
		fileset.setMeanFileSize("100k");
		fileset.setMeanDirWidth(1);
		fileset.setPrealloc(100);
		fileset.setFiles(1);
		filebenchVar.getFilesets().add(fileset);

		Thread thread = SBHModelFactory.eINSTANCE.createThread();
		thread.setInstances(1);
		thread.setMemsize("10m");
		thread.setThreadName("filereaderthread");

		Read readOper = OperationsFactory.eINSTANCE.createRead();
		readOper.setFlowOpName("read");
		readOper.setOperationName("read1");
		readOper.setFilesetname(fileset.getFilesetName());
		readOper.setRandom(0);
		readOper.setDirectio(1);
		readOper.setIosize("4k");
		readOper.setIters("1");

		thread.getOperations().add(readOper);
		filebenchVar.getThreads().add(thread);

		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();

		new File("/tmp/localResultsFilebench").mkdir();

		String uuid = UUID.randomUUID().toString();

		FilebenchBenchmarkDriver driver = new FilebenchBenchmarkDriver(con, "/tmp/localResultsFilebench/");
		
		boolean retry = true;
		while(retry) {
			try
			{
				driver.prepareExperiment(uuid, sutVar, filebenchVar);
				retry = false;
			} catch (CheckedBenchmarkException e)
			{
				// Do retry
			}
		}

		return driver;
	}
}
