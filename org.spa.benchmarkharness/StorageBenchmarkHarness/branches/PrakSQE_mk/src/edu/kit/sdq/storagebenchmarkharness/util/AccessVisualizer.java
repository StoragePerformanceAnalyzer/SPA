package edu.kit.sdq.storagebenchmarkharness.util;

import java.io.File;
import java.util.List;

/**
 * Creates an output file that can be used as input to the r-statistic tool to
 * visualize the access pattern
 * 
 * @author Axel Busch (axel.busch@student.kit.edu)
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
}
