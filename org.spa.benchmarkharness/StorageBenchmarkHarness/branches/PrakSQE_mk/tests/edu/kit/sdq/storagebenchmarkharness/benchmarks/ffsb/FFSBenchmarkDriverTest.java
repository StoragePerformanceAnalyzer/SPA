package edu.kit.sdq.storagebenchmarkharness.benchmarks.ffsb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Maps;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.TestUtils;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValue;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.FileSystem;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFFSB;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Scheduler;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.CheckedBenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.ffsb.FFSBenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.exceptions.RemoteConnectionException;

public class FFSBenchmarkDriverTest
{
	private static final Logger LOGGER = Logger.getLogger(FFSBenchmarkDriverTest.class);

	@Test
	public void testParsing()
	{
		String log = TestUtils.getResource("ffsbParsingTest1");

		List<DependentVariablesValue> results = FFSBenchmarkDriver.parseFFSBLogAndSave(new BufferedReader(new StringReader(log)), false, null);

		assertEquals(183232, results.size());

		// Count different types
		Map<String, Integer> counts = Maps.newHashMap();
		for (DependentVariablesValue r : results)
		{
			int c = counts.containsKey(r.getOperation()) ? counts.get(r.getOperation()) : 0;
			counts.put(r.getOperation(), c + 1);
		}

		LOGGER.debug("Count-Data is %s", counts);
		assertEquals(1349, (int) counts.get("open"));
		assertEquals(24133, (int) counts.get("read"));
		assertEquals(156043, (int) counts.get("write"));
		assertEquals(171, (int) counts.get("unlink"));
		assertEquals(1349, (int) counts.get("close"));
		assertEquals(187, (int) counts.get("stat"));

		assertEquals(6, counts.keySet().size());
	}

	@Test
	public void testConfigGeneration() throws IOException, InterruptedException
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.NOOP);

		final int writeBlockSize = 2048;
		final int readBlockSize = 4096;
		final int opsPerFile = 256;

		IndependentVariablesOfFFSB ffsbVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		ffsbVar.setFilesetSize(10240);
		ffsbVar.setFileSize(4096);
		ffsbVar.setOpsPerFile(opsPerFile);
		ffsbVar.setReadBlockSize(readBlockSize);
		ffsbVar.setWriteBlockSize(writeBlockSize);
		ffsbVar.setReadPercentage(50);
		ffsbVar.setRunTime(30);
		ffsbVar.setSequentialRead(false);
		ffsbVar.setSequentialWrite(false);
		ffsbVar.setThreadCount(15);
		ffsbVar.setWarmUpTime(30);
		ffsbVar.setWriteFsync(false);
		ffsbVar.setDirectIO(true);

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

				assertFalse("Content of file " + filename + " contains {{", content.contains("{{"));
				assertFalse("Content of file " + filename + " contains }}", content.contains("}}"));
				assertFalse("Content of file " + filename + " contains }", content.contains("}"));
				assertFalse("Content of file " + filename + " contains {", content.contains("{"));

				assertTrue("Write Size wrongly calculated", content.contains("write_size = " + (writeBlockSize * opsPerFile)));
				assertTrue("Read Size wrongly calculated", content.contains("read_size = " + (readBlockSize * opsPerFile)));

				assertTrue("read_random wrong set", content.contains("read_random = 1"));
				assertTrue("write_random wrong set", content.contains("write_random = 1"));
			}
		};

		new File("/tmp/localResults").mkdir();

		FFSBenchmarkDriver ffsb = new FFSBenchmarkDriver(testRemoteConnection, null, "/tmp/target");
		ffsb.prepareFFSBConfigurations(sutVar, ffsbVar);
	}

	@Test(expected = NullPointerException.class)
	public void testConfigGenerationNull()
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.NOOP);

		IndependentVariablesOfFFSB ffsbVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		ffsbVar.setFilesetSize(10240);
		ffsbVar.setFileSize(4096);
		ffsbVar.setOpsPerFile(256);
		ffsbVar.setReadBlockSize(4096);
		ffsbVar.setWriteBlockSize(2048);
		ffsbVar.setReadPercentage(50);
		ffsbVar.setRunTime(null);
		ffsbVar.setSequentialRead(false);
		ffsbVar.setSequentialWrite(false);
		ffsbVar.setThreadCount(15);
		ffsbVar.setWarmUpTime(null);
		ffsbVar.setWriteFsync(false);
		ffsbVar.setDirectIO(true);

		RemoteConnection testRemoteConnection = new MockupRemoteConnection()
		{
			@Override
			public void saveStringToFile(String content, String filename, boolean useSudo) throws RemoteConnectionException
			{
				Assert.fail();
			}
		};

		new File("/tmp/localResults").mkdir();

		FFSBenchmarkDriver ffsb = new FFSBenchmarkDriver(testRemoteConnection, null, "/tmp/target");
		ffsb.prepareExperiment(sutVar, ffsbVar);

		Assert.fail();
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
	public void testBenchmarkingFFSB() throws InterruptedException, IOException
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.CFQ);

		IndependentVariablesOfFFSB ffsbVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		ffsbVar.setFilesetSize(10);
		ffsbVar.setFileSize(4096);
		ffsbVar.setOpsPerFile(256);
		ffsbVar.setReadBlockSize(4096);
		ffsbVar.setWriteBlockSize(2048);
		ffsbVar.setReadPercentage(50);
		ffsbVar.setRunTime(5);
		ffsbVar.setSequentialRead(false);
		ffsbVar.setSequentialWrite(false);
		ffsbVar.setThreadCount(15);
		ffsbVar.setWarmUpTime(5);
		ffsbVar.setWriteFsync(false);
		ffsbVar.setDirectIO(true);

		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();

		new File("/tmp/localResultsFFSB").mkdir();
		// new File("/tmp/target").mkdir();
		// Runtime.getRuntime().exec("chmod 777 /tmp/target").waitFor();

		String uuid = UUID.randomUUID().toString();

		FFSBenchmarkDriver driver = new FFSBenchmarkDriver(con, "/tmp/localResultsFFSB");

		boolean cont = true;
		while(cont) {
			try
			{
				driver.prepareExperiment(uuid, sutVar, ffsbVar);
				cont = false;
			} catch (BenchmarkException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CheckedBenchmarkException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		
		DependentVariables results = driver.startExperiment(1);

		assertNotNull(results);
		// assertTrue(results.length > 0);

		assertTrue(new File("/tmp/localResultsFFSB/", uuid + "/warmup.log").exists());
		assertTrue(new File("/tmp/localResultsFFSB/", uuid + "/bench.1.log").exists());
	}

	@Test(expected = BenchmarkException.class)
	public void testBenchmarkingWrongFilesystem() throws InterruptedException, IOException
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.NOOP);

		IndependentVariablesOfFFSB ffsbVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		ffsbVar.setFilesetSize(10);
		ffsbVar.setFileSize(4096);
		ffsbVar.setOpsPerFile(256);
		ffsbVar.setReadBlockSize(4096);
		ffsbVar.setWriteBlockSize(2048);
		ffsbVar.setReadPercentage(50);
		ffsbVar.setRunTime(5);
		ffsbVar.setSequentialRead(false);
		ffsbVar.setSequentialWrite(false);
		ffsbVar.setThreadCount(15);
		ffsbVar.setWarmUpTime(5);
		ffsbVar.setWriteFsync(false);
		ffsbVar.setDirectIO(true);

		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();

		new File("/tmp/localResults").mkdir();
		// new File("/tmp/target").mkdir();
		// Runtime.getRuntime().exec("chmod 777 /tmp/target").waitFor();

		String uuid = UUID.randomUUID().toString();

		FFSBenchmarkDriver driver = new FFSBenchmarkDriver(con, "/tmp/localResults", "/tmp/");

		
		boolean cont = true;
		while(cont) {
			try
			{
				driver.prepareExperiment(uuid, sutVar, ffsbVar);
				cont = false;
			} catch (BenchmarkException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CheckedBenchmarkException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}
		DependentVariables results = driver.startExperiment(1);

		assertNotNull(results);
		// assertTrue(results.length > 0);

		assertTrue(new File("/tmp/localResults/", uuid + "/warmup.log").exists());
		assertTrue(new File("/tmp/localResults/", uuid + "/bench.1.log").exists());
	}

	@Test
	public void testConfigGenerationSingleBlockSize()
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.NOOP);

		final int fileSize = 4 * 1024; // kB
		final int fileSetSize = 1 * 1024; // MB
		final int blocksize = 4 * 1024; // bytes

		IndependentVariablesOfFFSB ffsbVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		ffsbVar.setFilesetSize(fileSetSize);
		ffsbVar.setFileSize(fileSize);
		ffsbVar.setOpsPerFile(256);
		ffsbVar.setBlockSize(blocksize);
		ffsbVar.setReadPercentage(50);
		ffsbVar.setRunTime(30);
		ffsbVar.setSequentialRead(false);
		ffsbVar.setSequentialWrite(false);
		ffsbVar.setThreadCount(15);
		ffsbVar.setWarmUpTime(30);
		ffsbVar.setWriteFsync(false);
		ffsbVar.setDirectIO(true);

		RemoteConnection testRemoteConnection = new MockupRemoteConnection()
		{
			@Override
			public void saveStringToFile(String content, String filename, boolean useSudo) throws RemoteConnectionException
			{
				LOGGER.debug("%s: %s", filename, content);
				assertTrue(content.contains("write_blocksize = " + blocksize));
				assertTrue(content.contains("read_blocksize = " + blocksize));
				assertTrue(content.contains("max_filesize = " + fileSize + "k"));
				assertTrue(content.contains("min_filesize = " + fileSize + "k"));

				assertTrue(content.contains("num_files = 256"));
			}

		};

		new File("/tmp/localResults").mkdir();

		FFSBenchmarkDriver ffsb = new FFSBenchmarkDriver(testRemoteConnection, null, "/tmp/target");
		ffsb.prepareFFSBConfigurations(sutVar, ffsbVar);
	}

	@Test
	public void testConfigGenerationStrangeFilesize()
	{
		IndependentVariablesOfSut sutVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();
		sutVar.setFileSystem(FileSystem.EXT4);
		sutVar.setScheduler(Scheduler.NOOP);

		final int fileSize = 4 * 1024; // kB
		final int fileSetSize = 1 * 1024 + 2; // MB
		final int blocksize = 4 * 1024; // bytes

		IndependentVariablesOfFFSB ffsbVar = SBHModelFactory.eINSTANCE.createIndependentVariablesOfFFSB();
		ffsbVar.setFilesetSize(fileSetSize);
		ffsbVar.setFileSize(fileSize);
		ffsbVar.setOpsPerFile(256);
		ffsbVar.setBlockSize(blocksize);
		ffsbVar.setReadPercentage(50);
		ffsbVar.setRunTime(30);
		ffsbVar.setSequentialRead(false);
		ffsbVar.setSequentialWrite(false);
		ffsbVar.setThreadCount(15);
		ffsbVar.setWarmUpTime(30);
		ffsbVar.setWriteFsync(false);
		ffsbVar.setDirectIO(true);

		RemoteConnection testRemoteConnection = new MockupRemoteConnection()
		{
			@Override
			public void saveStringToFile(String content, String filename, boolean useSudo) throws RemoteConnectionException
			{
				LOGGER.debug("%s: %s", filename, content);
				assertTrue(content.contains("write_blocksize = " + blocksize));
				assertTrue(content.contains("read_blocksize = " + blocksize));
				assertTrue(content.contains("max_filesize = " + fileSize + "k"));
				assertTrue(content.contains("min_filesize = " + fileSize + "k"));

				assertTrue(content.contains("num_files = 257"));
			}

		};

		new File("/tmp/localResults").mkdir();

		FFSBenchmarkDriver ffsb = new FFSBenchmarkDriver(testRemoteConnection, null, "/tmp/target");
		ffsb.prepareFFSBConfigurations(sutVar, ffsbVar);
	}

}
