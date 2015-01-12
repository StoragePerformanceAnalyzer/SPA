package edu.kit.sdq.storagebenchmarkharness;

import org.junit.Assert;
import org.junit.Test;

public class NullTest
{
	private static final Logger LOGGER = Logger.getLogger(NullTest.class);

	private Boolean a;
	private Integer i;
	private Integer j;

	@Test(expected = NullPointerException.class)
	public void booleanTest1()
	{
		if (a)
		{
			Assert.fail();
		}
	}

	@Test(expected = NullPointerException.class)
	public void booleanTest2()
	{
		if (!a)
		{
			Assert.fail();
		}
	}

	@Test(expected = NullPointerException.class)
	public void booleanTest3()
	{
		if (a == true)
		{
			Assert.fail();
		}
	}

	@Test(expected = NullPointerException.class)
	public void booleanTest4()
	{
		if (a == false)
		{
			Assert.fail();
		}
	}

	@Test(expected = NullPointerException.class)
	public void booleanTest5()
	{
		String s = a ? "yes" : "no";
		LOGGER.debug("String is %s", s);
		Assert.fail();
	}

	@Test(expected = NullPointerException.class)
	public void intTest1()
	{
		String s = "" + (100 - i);
		LOGGER.debug("String is %s", s);
		Assert.fail();
	}

	@Test()
	public void intTest2()
	{
		String s = "abc" + i;
		LOGGER.debug("String is %s", s);
		Assert.assertEquals("abcnull", s);
	}

	@Test(expected = NullPointerException.class)
	public void intTest3()
	{
		String s = "abc" + i.toString();
		LOGGER.debug("String is %s", s);
		Assert.fail();
	}

	@Test(expected = NullPointerException.class)
	public void intTest4()
	{
		int result = i * j;
		LOGGER.debug("Result is %d", result);
		Assert.fail();
	}

	@Test(expected = NullPointerException.class)
	public void intTest5()
	{
		int v = 100;
		int result = i * v;
		LOGGER.debug("Result is %d", result);
		Assert.fail();
	}
}
