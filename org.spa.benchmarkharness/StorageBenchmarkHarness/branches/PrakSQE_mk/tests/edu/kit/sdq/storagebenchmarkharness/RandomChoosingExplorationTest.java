package edu.kit.sdq.storagebenchmarkharness;

import static edu.kit.sdq.storagebenchmarkharness.TestUtils.assertClassEquals;
import static edu.kit.sdq.storagebenchmarkharness.TestUtils.assertEObjectEquals;
import static org.junit.Assert.*;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.google.common.collect.Lists;

import edu.kit.sdq.storagebenchmarkharness.ExperimentSeriesHelper.DriverAndIndependentVars;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.FileSystem;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFFSB;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Scheduler;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.ConfigurationFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.ExperimentSeries;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariableSpaceOfFFSB;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariableSpaceOfSut;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.ffsb.FFSBenchmarkDriver;

public class RandomChoosingExplorationTest
{
	@Test
	public void testSimple2()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1, 10));
		input.add(Lists.newArrayList(100, 200));

		ExplorationStrategy expStra = new RandomChoosingExploration(100);

		List<List<Object>> result = expStra.explore(input);

		assertEquals(100, result.size());

		for (List<Object> l : result)
		{
			assertEquals(2, l.size());

			assertTrue(((Integer) l.get(0)) >= 1);
			assertTrue(((Integer) l.get(0)) <= 100);
			assertTrue(((Integer) l.get(1)) >= 100);
			assertTrue(((Integer) l.get(1)) <= 200);
		}
	}

	@Test
	public void testSimple3()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1, 10));
		input.add(Lists.newArrayList(123));
		input.add(Lists.newArrayList(100, 200));

		ExplorationStrategy expStra = new RandomChoosingExploration(100);

		List<List<Object>> result = expStra.explore(input);

		assertEquals(100, result.size());

		for (List<Object> l : result)
		{
			assertEquals(3, l.size());

			assertTrue(((Integer) l.get(0)) >= 1);
			assertTrue(((Integer) l.get(0)) <= 100);
			assertTrue(((Integer) l.get(1)) == 123);
			assertTrue(((Integer) l.get(2)) >= 100);
			assertTrue(((Integer) l.get(2)) <= 200);
		}
	}

	@Test
	public void testSimple4()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1, 10));
		input.add(Lists.newArrayList(123));
		input.add(Lists.newArrayList(100, 200));
		input.add(Lists.newArrayList());

		ExplorationStrategy expStra = new RandomChoosingExploration(100);

		List<List<Object>> result = expStra.explore(input);

		assertEquals(100, result.size());

		for (List<Object> l : result)
		{
			assertEquals(4, l.size());

			assertTrue(((Integer) l.get(0)) >= 1);
			assertTrue(((Integer) l.get(0)) <= 100);
			assertTrue(((Integer) l.get(1)) == 123);
			assertTrue(((Integer) l.get(2)) >= 100);
			assertTrue(((Integer) l.get(2)) <= 200);
			assertNull(l.get(3));
		}
	}

}
