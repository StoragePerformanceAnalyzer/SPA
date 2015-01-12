package edu.kit.sdq.storagebenchmarkharness.benchmarks.filebench;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

import org.junit.Test;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.RemoteProcess;
import edu.kit.sdq.storagebenchmarkharness.TestUtils;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.FileSystem;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Fileset;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBlktrace;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFilebench;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Scheduler;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Thread;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.SystemUnderTest;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.OperationsFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.Read;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.ffsb.MockupRemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.exceptions.CheckedBenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.exceptions.RemoteConnectionException;
import edu.kit.sdq.storagebenchmarkharness.monitors.BlktraceMonitorDriver;

/**
 * JUnit class for testing FilebenchBenchmarkDriver
 * 
 * @author Axel Busch (axel.busch@student.kit.edu)
 * 
 */
public class FilebenchBenchmarkDriverTest
{
	private static final Logger LOGGER = Logger.getLogger(FilebenchBenchmarkDriverTest.class);

	@Test
	public void testConfigGeneration() throws IOException, InterruptedException
	{
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

		Thread thread = SBHModelFactory.eINSTANCE.createThread();
		thread.setInstances(1);
		thread.setMemsize("10m");
		thread.setThreadName("filereaderthread");

		Read readOper = OperationsFactory.eINSTANCE.createRead();
		readOper.setOperationName("write-file");
		readOper.setFilesetname(fileset.getFilesetName());
		readOper.setRandom(1);
		readOper.setDirectio(1);
		readOper.setIosize("2k");
		readOper.setIters("1");

		thread.getOperations().add(readOper);
		filebenchVar.getFilesets().add(fileset);

		RemoteConnection testRemoteConnection = new MockupRemoteConnection()
		{
			@Override
			public void saveStringToFile(String content, String filename, boolean useSudo) throws RemoteConnectionException
			{
				LOGGER.debug("File %s is %s", filename, content);
				assertNotNull(filename);
				assertTrue(filename.length() > 1);

				assertNotNull(content);
				assertTrue(content.length() > 1);
			}

			@Override
			public RemoteProcess execCmd(String cmd, boolean pid)
			{
				return null;
			}

			@Override
			public SystemUnderTest getHost()
			{
				// TODO Auto-generated method stub
				return null;
			}
		};

		new File("/tmp/localResults").mkdir();

		FilebenchBenchmarkDriver filebench = new FilebenchBenchmarkDriver(testRemoteConnection, null, "/tmp/target");
		filebench.prepareFilebenchConfigurations(sutVar, filebenchVar);
	}

	/**
	 * ATTENTION: This test fails if the system is not configured properly. The
	 * connection data from {@code TestUtils.getLocalhostConnection} must be
	 * valid!
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testBenchmarkingFilebench() throws InterruptedException, IOException
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.CFQ);

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
		

		DependentVariables results = null;
		retry = true;
		while(retry) {
			try
			{
				results = driver.startExperiment(1);
				retry = false;
			} catch (CheckedBenchmarkException e)
			{
				// Do retry
			}
		}
		
		assertNotNull(results);

		assertTrue(new File("/tmp/localResultsFilebench/", uuid + "/warmup.log").exists());
		assertTrue(new File("/tmp/localResultsFilebench/", uuid + "/filebenchBench.1.log").exists());
	}

	/**
	 * ATTENTION: This test fails if the system is not configured properly. The
	 * connection data from {@code TestUtils.getLocalhostConnection} must be
	 * valid!
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	@Test
	public void testBenchmarkingFilebenchMonitored() throws InterruptedException, IOException
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.CFQ);

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

		IndependentVariablesOfBlktrace blktrace = SBHModelFactory.eINSTANCE.createIndependentVariablesOfBlktrace();
		blktrace.setBufferSize(999);
		blktrace.setNumberOfBuffers(30);
		blktrace.setTargetDevice("/dev/sdb1");
		blktrace.setLogFilePrefix("test");

		RemoteConnection conBlktrace = TestUtils.getLocalhostConnection();
		conBlktrace.open();
		File localResults = new File("/tmp/localResultsFilebench");
		localResults.mkdir();

		RemoteConnection conFilebench = TestUtils.getLocalhostConnection();
		conFilebench.open();

		new File("/tmp/localResultsFilebench").mkdir();

		String uuid = UUID.randomUUID().toString();

		FilebenchBenchmarkDriver driver = new FilebenchBenchmarkDriver(conFilebench, "/tmp/localResultsFilebench/");
		// driver.prepareExperiment(uuid, sutVar, filebenchVar);

		BlktraceMonitorDriver blktraceDriver = new BlktraceMonitorDriver(conBlktrace, localResults.getAbsolutePath());
		blktraceDriver.startMonitoring(uuid, sutVar, blktrace);

		DependentVariables resultsFilebench = null;
		boolean retry = true;
		while(retry) {
			try
			{
				resultsFilebench = driver.startExperiment(1);
				retry = false;
			} catch (CheckedBenchmarkException e)
			{
				// Do retry
			}
		}
		DependentVariables resultsBlktrace = blktraceDriver.stopMonitoring(blktrace, 1, resultsFilebench.getBenchmarkPrefix());
		assertNotNull(resultsFilebench);
		assertNotNull(resultsBlktrace);

		assertTrue(new File("/tmp/localResultsFilebench/", uuid + "/warmup.log").exists());
		assertTrue(new File("/tmp/localResultsFilebench/", uuid + "/filebenchBench.1.log").exists());
	}
}
