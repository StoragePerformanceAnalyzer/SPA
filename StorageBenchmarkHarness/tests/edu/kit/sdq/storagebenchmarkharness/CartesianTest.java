package edu.kit.sdq.storagebenchmarkharness;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;

import com.google.common.collect.Lists;

public class CartesianTest
{
	private static FullFactorialExploration cartesian = new FullFactorialExploration();

	@Before
	public void setup()
	{
		((ch.qos.logback.classic.Logger) LoggerFactory.getLogger("edu.kit.sdq.storagebenchmarkharness")).setLevel(Level.TRACE);
	}

	@Test
	// Parameters is null
	public void testNull()
	{
		List<List<Object>> result = cartesian.explore(null);
		Assert.assertEquals(null, result);
	}

	@Test
	// Empty List as input
	public void testEmpty()
	{
		List<List<?>> input = Lists.newArrayList();
		List<List<Object>> expected = Lists.newArrayList();

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(0, result.size());
		Assert.assertEquals(expected, result);
	}

	@Test
	// Only one empty list as input
	public void testEmpty2()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(new ArrayList<Object>());

		List<List<Object>> expected = Lists.newArrayList();

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(0, result.size());
		Assert.assertEquals(expected, result);
	}

	@Test
	// A empty and a non-empty list as input
	public void testEmpty3()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1));
		input.add(new ArrayList<Object>());

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, null));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(expected, result);
	}

	@Test
	// Multiple empty lists as input
	public void testEmpty4()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1));
		input.add(new ArrayList<Object>());
		input.add(Lists.newArrayList(2));
		input.add(new ArrayList<Object>());

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, null, 2, null));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(1, result.size());
		Assert.assertEquals(expected, result);
	}

	@Test
	// Multiple empty lists and multiple items as input
	public void testEmpty5()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1, 2, 3));
		input.add(new ArrayList<Object>());
		input.add(Lists.newArrayList(100, 200));
		input.add(new ArrayList<Object>());

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, null, 100, null));
		expected.add(Lists.newArrayList(1, null, 200, null));
		expected.add(Lists.newArrayList(2, null, 100, null));
		expected.add(Lists.newArrayList(2, null, 200, null));
		expected.add(Lists.newArrayList(3, null, 100, null));
		expected.add(Lists.newArrayList(3, null, 200, null));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(6, result.size());
		Assert.assertEquals(expected, result);
	}

	@Test
	public void testSimple1()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1));

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(expected, result);
	}

	@Test
	public void testSimple2()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1));
		input.add(Lists.newArrayList(2));

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, 2));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(expected, result);
	}

	@Test
	public void testSimple3()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1));
		input.add(Lists.newArrayList(2));
		input.add(Lists.newArrayList(3));

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, 2, 3));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(expected, result);
	}

	@Test
	public void testSimple3Types()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1));
		input.add(Lists.newArrayList("String"));
		input.add(Lists.newArrayList(new Double(1.2)));

		List<List<Object>> expected = Lists.newArrayList();
		List<Object> expectedl = new ArrayList<Object>();
		expectedl.add(1);
		expectedl.add("String");
		expectedl.add(new Double(1.2));
		expected.add(expectedl);

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(expected, result);
	}

	@Test
	public void test2()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1, 2, 3));
		input.add(Lists.newArrayList(10, 20));

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, 10));
		expected.add(Lists.newArrayList(1, 20));
		expected.add(Lists.newArrayList(2, 10));
		expected.add(Lists.newArrayList(2, 20));
		expected.add(Lists.newArrayList(3, 10));
		expected.add(Lists.newArrayList(3, 20));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(expected, result);
	}

	@Test
	public void test3()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1, 2, 3));
		input.add(Lists.newArrayList(10, 20));
		input.add(Lists.newArrayList(100, 200));

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, 10, 100));
		expected.add(Lists.newArrayList(1, 10, 200));
		expected.add(Lists.newArrayList(1, 20, 100));
		expected.add(Lists.newArrayList(1, 20, 200));
		expected.add(Lists.newArrayList(2, 10, 100));
		expected.add(Lists.newArrayList(2, 10, 200));
		expected.add(Lists.newArrayList(2, 20, 100));
		expected.add(Lists.newArrayList(2, 20, 200));
		expected.add(Lists.newArrayList(3, 10, 100));
		expected.add(Lists.newArrayList(3, 10, 200));
		expected.add(Lists.newArrayList(3, 20, 100));
		expected.add(Lists.newArrayList(3, 20, 200));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(expected, result);
	}

	@Test
	public void testStrange()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096));
		input.add(Lists.newArrayList(0));
		input.add(Lists.newArrayList(100));
		input.add(Lists.newArrayList(50000));

		List<List<?>> expected2 = Lists.newArrayList();
		expected2.add(Lists.newArrayList(4, 0, 100, 50000));
		expected2.add(Lists.newArrayList(8, 0, 100, 50000));
		expected2.add(Lists.newArrayList(16, 0, 100, 50000));
		expected2.add(Lists.newArrayList(32, 0, 100, 50000));
		expected2.add(Lists.newArrayList(64, 0, 100, 50000));
		expected2.add(Lists.newArrayList(128, 0, 100, 50000));
		expected2.add(Lists.newArrayList(256, 0, 100, 50000));
		expected2.add(Lists.newArrayList(512, 0, 100, 50000));
		expected2.add(Lists.newArrayList(1024, 0, 100, 50000));
		expected2.add(Lists.newArrayList(2048, 0, 100, 50000));
		expected2.add(Lists.newArrayList(4096, 0, 100, 50000));

		List<List<Object>> result = cartesian.explore(input);

		Assert.assertEquals(expected2, result);
	}

	@Test
	public void testStrange2()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1, 2, 3));
		input.add(Lists.newArrayList(10));
		input.add(Lists.newArrayList(20));

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, 10, 20));
		expected.add(Lists.newArrayList(2, 10, 20));
		expected.add(Lists.newArrayList(3, 10, 20));

		List<List<Object>> result = cartesian.explore(input);
		Assert.assertEquals(expected, result);
	}

	@Test
	public void testStrange3()
	{
		List<List<?>> input = Lists.newArrayList();
		input.add(Lists.newArrayList(1, 2, 3));
		input.add(Lists.newArrayList(10));
		input.add(Lists.newArrayList(20));
		input.add(Lists.newArrayList(100, 200, 300));

		List<List<Integer>> expected = Lists.newArrayList();
		expected.add(Lists.newArrayList(1, 10, 20, 100));
		expected.add(Lists.newArrayList(1, 10, 20, 200));
		expected.add(Lists.newArrayList(1, 10, 20, 300));
		expected.add(Lists.newArrayList(2, 10, 20, 100));
		expected.add(Lists.newArrayList(2, 10, 20, 200));
		expected.add(Lists.newArrayList(2, 10, 20, 300));
		expected.add(Lists.newArrayList(3, 10, 20, 100));
		expected.add(Lists.newArrayList(3, 10, 20, 200));
		expected.add(Lists.newArrayList(3, 10, 20, 300));

		List<List<Object>> result = cartesian.explore(input);
		Assert.assertEquals(expected, result);
	}
}
