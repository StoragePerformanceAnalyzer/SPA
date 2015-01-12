package edu.kit.sdq.storagebenchmarkharness;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import edu.kit.sdq.storagebenchmarkharness.SBHModel.FileSystem;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelPackage;

public class IndependentVarTest
{
	private static final Logger LOGGER = Logger.getLogger(IndependentVarTest.class);

	@Test
	public void testNull()
	{
		IndependentVariablesOfSut sutVars = SBHModelFactory.eINSTANCE.createIndependentVariablesOfSut();

		FileSystem x = FileSystem.BTRFS;

		x = null;

		LOGGER.debug("Filesystem is %s %s", sutVars.getFileSystem(), x);

		assertFalse(sutVars.eIsSet(SBHModelPackage.INDEPENDENT_VARIABLES_OF_SUT__FILE_SYSTEM));
		sutVars.setFileSystem(FileSystem.EXT4);
		assertTrue(sutVars.eIsSet(SBHModelPackage.INDEPENDENT_VARIABLES_OF_SUT__FILE_SYSTEM));
	}
}
