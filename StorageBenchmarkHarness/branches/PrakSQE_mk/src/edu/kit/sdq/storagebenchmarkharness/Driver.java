package edu.kit.sdq.storagebenchmarkharness;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.io.Files;

import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;

/**
 * Captures routines used for both, BenchmarkDrivers and MonitorDrivers
 * 
 * @author Axel Busch (axel.busch@student.kit.edu)
 * 
 */
public abstract class Driver
{
	private final String fileSaveDir;
	private String expUid;

	public Driver(String fileSaveDir)
	{
		this.fileSaveDir = fileSaveDir;
	}

	/**
	 * Returns the full path where the output should be saved. If no output
	 * should be saved, the method returns null.
	 * 
	 * @param fileName
	 *            A filename (not a path) for the file
	 * @return The full absolute path for the file.
	 */
	protected File getFile(String fileName)
	{
		if (fileSaveDir == null)
		{
			return null;
		}

		if (expUid == null)
			expUid = "0";
		File dir = new File(fileSaveDir, expUid);
		if (!dir.exists())
		{
			if (!dir.mkdirs())
			{
				throw new BenchmarkException("FileSaveDir could not be created");
			}
		}

		File f = new File(dir, fileName);
		return f;
	}

	/**
	 * Saves the results of the benchmark driver to a file. If the benchmark or
	 * monitor driver was configured not to save the files, this method simply
	 * does nothing.
	 * 
	 * @param fileName
	 *            A filename (not a path) for the raw file
	 * @param content
	 *            The contents of the file
	 */
	protected void saveFile(String fileName, String content)
	{
		if (fileSaveDir == null)
		{
			return;
		}

		try
		{
			Files.write(content, getFile(fileName), Charset.defaultCharset());
		} catch (IOException e)
		{
			throw new BenchmarkException("FileSaveDir could not be saved", e);
		}
	}

	/**
	 * Gets an environment variable. If the environment variable is not set, a
	 * default value will be returned.
	 * 
	 * @param envName
	 * @param defaultValue
	 * @return
	 */
	protected static String getEnvDefault(String envName, String defaultValue)
	{
		String v = System.getenv(envName);
		if (v == null)
		{
			return defaultValue;
		} else
		{
			return v;
		}
	}

	protected String getFileSaveDir()
	{
		return fileSaveDir;
	}

	protected String getExpUid()
	{
		return expUid;
	}

	protected void setExpUid(String expUid)
	{
		this.expUid = expUid;
	}
}
