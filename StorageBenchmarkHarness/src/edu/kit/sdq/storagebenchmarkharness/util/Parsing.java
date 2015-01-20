package edu.kit.sdq.storagebenchmarkharness.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Axel Busch
 *
 */
public class Parsing
{
	public static List<String[]> divideLineArray(List<String> al, String delimiter)
	{
		ArrayList<String[]> result = new ArrayList<String[]>();
		for (int i = 0; i < al.size(); ++i)
		{
			result.add(divideLine(al.get(i), delimiter));
		}
		return result;
	}

	public static String[] divideLine(String s, String delimiter)
	{
		s = s.trim();
		return s.split(delimiter);
	}
}
