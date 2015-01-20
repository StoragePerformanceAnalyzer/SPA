package edu.kit.sdq.storagebenchmarkharness;

import org.junit.Assert;
import org.junit.Test;

public class JavaHomeTest
{
	/**
	 * ATTENTION: For abvious reasons this test fails on every system which are
	 * not Unix/Linux and the logged in user is not 'dominik'.
	 */
	@Test
	public void testJavaHome()
	{
		Assert.assertEquals("/home/dominik", System.getProperty("user.home"));
	}
}
