package edu.kit.sdq.storagebenchmarkharness.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.monitors.ThreadsMonitorDriver;

public class WriteOut
{
	private static final Logger LOGGER = Logger.getLogger(ThreadsMonitorDriver.class);
	
	File file = null;
	BufferedWriter out = null;

	public WriteOut(String outFile)
	{
		this(new File(outFile));
	}

	public WriteOut(File outFile)
	{
		this.file = outFile;
		try
		{
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
		} catch (FileNotFoundException e)
		{
			LOGGER.debug("WriteOut failed", e);
		}
	}

	public void writeLine(String line)
	{
		try
		{
			out.write(line);
		} catch (IOException e)
		{
			LOGGER.debug("WriteLine failed", e);
		}
	}

	public static <T> void WriteFile(String filename, ArrayList<T> lines)
	{
		File file = new File(filename);
		BufferedWriter out = null;
		try
		{
			out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));

			for (int i = 0; i < lines.size(); ++i)
			{
				out.write(lines.get(i).toString());
			}
		} catch (Exception ex)
		{
			LOGGER.debug("BufferedWriter closed", ex);
		} finally
		{
			if (out != null)
				try
				{
					out.close();
				} catch (IOException e)
				{
					LOGGER.debug("Out closed", e);
				}
		}
	}
}
