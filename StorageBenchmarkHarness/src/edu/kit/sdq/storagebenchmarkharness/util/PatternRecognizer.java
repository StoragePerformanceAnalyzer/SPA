package edu.kit.sdq.storagebenchmarkharness.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


/**
 * Recognizes the access pattern of a preparsed blktrace output.
 * 
 * Result is a probability for sequential access
 * 
 * @author Axel Busch
 * 
 */
public class PatternRecognizer
{
	public static double recognizeWeighted(List<String[]> val)
	{
		ArrayList<double[]> lines = new ArrayList<double[]>();
		double[] tmp;
		for (int i = 0; i < val.size(); ++i)
		{
			tmp = new double[2];
			tmp[0] = Double.parseDouble(val.get(i)[1]);
			tmp[1] = Double.parseDouble(val.get(i)[2]);
			lines.add(tmp);
		}
		int num_Ops = lines.size();
		int num_Ops_seq = 0;
		int i = 0, j = 0;
		double alpha = 0;
		double beta = 0;
		double blocks = 0;
		
		while1: while (i < lines.size())
		{
			j = i + 1;
			alpha = lines.get(i)[1];

			while (j < lines.size())
			{
				beta = lines.get(j)[0];
				if (alpha == beta)
				{
					blocks = ((alpha - lines.get(i)[0]) / 8) + ((lines.get(j)[1] - beta) / 8);
					num_Ops_seq += blocks;
					num_Ops += blocks - 2;
					lines.remove(j);
					lines.remove(i);
					continue while1;
				}
				else
					++j;
			}
			if (alpha - lines.get(i)[0] > 8)
			{
				blocks = (alpha - lines.get(i)[0]) / 8;
				num_Ops_seq += blocks;
				num_Ops += blocks - 1;
				lines.remove(i);
			}
			else
				++i;
		}
		if (num_Ops < 1)
			return -1;
		else
			return (double) num_Ops_seq / (double) num_Ops;
	}
	
	public static double recognize(List<String[]> val, boolean weightedRequests)
	{
		if (weightedRequests)
			return recognizeWeighted(val);
		ArrayList<double[]> lines = new ArrayList<double[]>();
		double[] tmp;
		for (int i = 0; i < val.size(); ++i)
		{
			tmp = new double[2];
			tmp[0] = Double.parseDouble(val.get(i)[1]);
			tmp[1] = Double.parseDouble(val.get(i)[2]);
			lines.add(tmp);
		}
		int num_Ops = lines.size();
		int num_Ops_seq = 0;
		int i = 0, j = 0;
		double alpha = 0;

		while1: while (i < lines.size())
		{
			j = i + 1;
			alpha = lines.get(i)[1];

			while (j < lines.size())
			{
				if (alpha == lines.get(j)[0])
				{
					lines.remove(j);
					lines.remove(i);
					num_Ops_seq += 2;
					continue while1;
				} else
					++j;
			}
			++i;
		}
		if (num_Ops < 1)
			return -1;
		else
			return (double) num_Ops_seq / (double) num_Ops;
	}
	
	public static double getAvgOpsPerFile(List<String[]> val)
	{
		ArrayList<double[]> lines = new ArrayList<double[]>();
		double[] tmp;
		int iters = 0;
		for (int i = 0; i < val.size(); ++i)
		{
			tmp = new double[2];
			if (val.get(i).length > 1)
			{
				tmp[0] = Double.parseDouble(val.get(i)[1]);
				tmp[1] = Double.parseDouble(val.get(i)[2]);
				lines.add(tmp);
			}
		}
		
		
		double[][] linesArray = new double[lines.size()][2];
		for (int i = 0; i < lines.size(); ++i)
			linesArray[i] = lines.get(i);
		
		
		Arrays.sort(linesArray, new Comparator<double[]>()
		{
			@Override
			public int compare(double[] arg0, double[] arg1)
			{
				return Double.compare(arg0[0], arg1[0]);
			}
			
		});
		
		long avgBlockDistance = 0;
		for (int i = 0; i < linesArray.length - 1; ++i)
			avgBlockDistance += linesArray[i+1][0] - linesArray[i][1];
		
		if (linesArray.length > 0)
			avgBlockDistance /= linesArray.length;
		else
			avgBlockDistance = 2;
			
		for1:for (int i = 0; i < linesArray.length - 1; ++i)
		{
			if (linesArray[i+1][0] - linesArray[i][1] < avgBlockDistance)
			{
				continue for1;
			}

			++iters;
		}
		if (iters < 1)
			return -1;
		else
			return (lines.size() + 0.0) / (iters);
	}
}
