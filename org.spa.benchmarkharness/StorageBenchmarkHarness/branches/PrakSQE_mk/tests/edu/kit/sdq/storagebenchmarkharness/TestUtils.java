package edu.kit.sdq.storagebenchmarkharness;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;

import com.google.common.io.CharStreams;

import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.ConfigurationFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.SystemUnderTest;

public final class TestUtils
{
	public final static void assertEObjectEquals(EObject objExp, EObject objAct)
	{
		assertTrue("EObjects to not match. Expected " + objExp + ", got " + objAct, EcoreUtil.equals(objExp, objAct));
	}

	public final static void assertClassEquals(Class<?> c, Object o)
	{
		assertTrue("Object is of class " + o.getClass() + ", expected " + c, o.getClass().equals(c));
	}

	public static RemoteConnection getLocalhostConnection()
	{
		SystemUnderTest host = ConfigurationFactory.eINSTANCE.createSystemUnderTest();
		host.setIp("localhost");
		host.setPort(10022);
		host.setUser("abusch");
		host.setKeyFile("/Users/axelbusch/.ssh/id_rsa");

		return new SSHRemoteConnection(host);
	}

	public static String getResource(String name)
	{
		try
		{
			return CharStreams.toString(new InputStreamReader(TestUtils.class.getResourceAsStream("/res/" + name)));
		} catch (IOException e)
		{
			return null;
		}
	}
}
