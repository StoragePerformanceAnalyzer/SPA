package edu.kit.sdq.storagebenchmarkharness.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.monitors.ThreadsMonitorDriver;

/**
 * Creates an output file that can be used as input to the r-statistic tool to
 * visualize the access pattern
 * 
 * @author Axel Busch
 * 
 */
public class AccessVisualizer
{
	
	public static void visualize(List<String[]> val, File fileOut)
	{

		WriteOut out = new WriteOut(fileOut);
		long tmp;
		for (int i = 0; i < val.size(); ++i)
		{
			long start = Long.parseLong(val.get(i)[1]);
			long end = Long.parseLong(val.get(i)[2]);
			for (int j = 0; j < (end - start); ++j)
			{
				tmp = start + j;
				out.writeLine(String.valueOf(tmp) + "\n");
			}
		}
	}
	
	private static final class WriteOut
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
	
}
