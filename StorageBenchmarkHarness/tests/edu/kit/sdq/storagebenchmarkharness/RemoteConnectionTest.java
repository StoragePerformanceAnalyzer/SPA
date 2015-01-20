package edu.kit.sdq.storagebenchmarkharness;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.CharStreams;
import com.google.common.io.Files;

import edu.kit.sdq.storagebenchmarkharness.exceptions.RemoteConnectionException;

/**
 * ATTENTION: All these test fail if the system is not configured properly. The
 * connection data from {@code TestUtils.getLocalhostConnection} must be valid!
 * 
 * @author dominik
 * 
 */
public class RemoteConnectionTest
{
	private static final Logger LOGGER = Logger.getLogger(RemoteConnectionTest.class);

	String tempFile;

	@Before
	public void setup()
	{
		tempFile = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID().toString() + ".bin";
		LOGGER.debug("TempFile is %s", tempFile);
	}

	@After
	public void after()
	{
		File t = new File(tempFile);
		if (t.exists())
		{
			t.delete();
		}
	}

	@Test
	public void testOpen()
	{
		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();
		con.close();
	}

	@Test
	public void testTee() throws IOException
	{
		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();

		String testString = UUID.randomUUID().toString();

		con.saveStringToFile(testString, tempFile, false);

		con.close();

		File t = new File(tempFile);
		assertTrue(t.exists());
		String actually = Files.toString(t, Charset.defaultCharset());
		assertEquals(testString, actually);
	}

	@Test
	public void testTeeAndDelete() throws IOException
	{
		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();

		String testString = UUID.randomUUID().toString();

		con.saveStringToFile(testString, tempFile, false);

		File t = new File(tempFile);
		assertTrue(t.exists());
		String actually = Files.toString(t, Charset.defaultCharset());
		assertEquals(testString, actually);

		con.deleteFile(tempFile);
		assertFalse(t.exists());

		con.close();
	}

	@Test
	public void testSudoTee() throws IOException
	{
		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();

		String testString = UUID.randomUUID().toString();

		con.saveStringToFile(testString, tempFile, true);

		File t = new File(tempFile);
		assertTrue(t.exists());
		String actually = Files.toString(t, Charset.defaultCharset());
		assertEquals(testString, actually);

		try
		{
			con.deleteFile(tempFile);
			fail("Should have thrown exception");
		} catch (RemoteConnectionException e)
		{

		}

		con.close();
	}

	@Test
	public void testSudoProc() throws IOException
	{
		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();

		con.saveStringToFile("noop", "/sys/block/sda/queue/scheduler", true);

		con.close();
	}

	@Test
	public void testExec() throws IOException
	{
		RemoteConnection con = TestUtils.getLocalhostConnection();
		con.open();

		RemoteProcess id = con.execCmd("id", false);
		String out = CharStreams.toString(new InputStreamReader(id.getInputStream()));
		id.waitFor();

		assertEquals(id.getExitStatus(), 0);

		assertTrue("Output " + out + " does not contain ffsbTest", out.contains("ffsbTest"));
	}

}
