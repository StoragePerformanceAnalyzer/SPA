package edu.kit.sdq.storagebenchmarkharness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.impl.EReferenceImpl;

import com.google.common.collect.Lists;

import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBenchmark;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfMonitor;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelPackage;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.ExperimentSeries;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariableSpaceOfFFSB;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariableSpaceOfFilebench;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariableSpaceOfPostmark;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.OperationsPackage;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.ffsb.FFSBenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.filebench.FilebenchBenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.benchmarks.postmark.PostmarkBenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.monitors.BlktraceMonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.monitors.FilesetMonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.monitors.FilesizeMonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.monitors.IostatMonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.monitors.ThreadsMonitorDriver;

/**
 * This helper class is used by the {@code BenchmarkController}. It expands a
 * {@code ExperimentSeries} (which is a description of many Experiments) into a
 * list of {@code DriverAndIndependentVars}. This class contains every
 * information which is necessary to run a single benchmark run.
 * 
 * @author Axel Busch
 * @author Dominik Bruhn
 * 
 */
public final class ExperimentSeriesHelper
{
	private static final Logger LOGGER = Logger.getLogger(ExperimentSeriesHelper.class);

	private static ExplorationStrategy explo;
	static
	{
		String v = System.getenv("explorationStrategy");
		if (v == null)
		{
			explo = new FullFactorialExploration();
		} else if (v.equals("randomChoosing"))
		{
			int n = Integer.valueOf(System.getenv("explorationCount"));
			explo = new RandomChoosingExploration(n);
		} else if (v.equals("randomChoosingFullFactorial"))
		{
			int n = Integer.valueOf(System.getenv("explorationCount"));
			explo = new RandomChoosingFullFactorialExploration(n);
		}
	}

	/**
	 * Expands a ExperimentSeries into a list of benchmark experiments. The
	 * correct BenchmarkDriver is assigned with the experiment. The expansion is
	 * done using the cartesian product. It establishes a mapping between a
	 * subclass of IndependentVariableSpaceOfBenchmark and the matching subclass
	 * of IndependentVariablesOfBenchmark. It transforms all attributes of the
	 * first class into the attributes of the second class.
	 * 
	 * All parameters except the ExperimentSeries are passed into the
	 * constructor of the appropriate BenchmarkDriver.
	 * 
	 * 
	 * @param series
	 *            The series which should be expanded.
	 * @param benchmarkCon
	 *            A RemoteConnection for running the benchmark which will be
	 *            provided to the BenchmarkDriver which will be constructed.
	 * @param monitorConns
	 *            A set of RemoteConnections for running the monitors which will
	 *            be provided to the BenchmarkDriver which will be constructed.
	 * @param rawFileSaveDir
	 *            The directory where the benchmark/monitor driver should save
	 *            its raw files. If null no saving should take place.
	 * @return A list of descriptions for each individual experiment.
	 */
	public static List<DriverAndIndependentVars> getExperiments(ExperimentSeries series, RemoteConnection benchmarkCon,
			List<RemoteConnection> monitorConns, String rawFileSaveDir)
	{

		List<MonitorDriver<?, ?>> monitorDrivers = new ArrayList<MonitorDriver<?, ?>>();
		List<EClass> monitorVarsClass = new ArrayList<EClass>();

		if (series.getIndependentVariableSpaceOfBenchmark() instanceof IndependentVariableSpaceOfFFSB)
		{
			FFSBenchmarkDriver bd = new FFSBenchmarkDriver(benchmarkCon, rawFileSaveDir);
			initialMonitorDrivers(monitorConns, series, rawFileSaveDir, monitorDrivers, monitorVarsClass, bd.getTargetDir());
			List<DriverAndIndependentVars> daivs = expandExperimentSeries(series, SBHModelPackage.eINSTANCE.getIndependentVariablesOfFFSB(),
					monitorVarsClass, bd, monitorDrivers);

			return daivs;
		} else if (series.getIndependentVariableSpaceOfBenchmark() instanceof IndependentVariableSpaceOfPostmark)
		{
			PostmarkBenchmarkDriver bd = new PostmarkBenchmarkDriver(benchmarkCon, rawFileSaveDir);
			initialMonitorDrivers(monitorConns, series, rawFileSaveDir, monitorDrivers, monitorVarsClass, bd.getTargetDir());
			List<DriverAndIndependentVars> daivs = expandExperimentSeries(series, SBHModelPackage.eINSTANCE.getIndependentVariablesOfPostmark(),
					monitorVarsClass, bd, monitorDrivers);

			return daivs;
		} else if (series.getIndependentVariableSpaceOfBenchmark() instanceof IndependentVariableSpaceOfFilebench)
		{
			FilebenchBenchmarkDriver bd = new FilebenchBenchmarkDriver(benchmarkCon, rawFileSaveDir);
			initialMonitorDrivers(monitorConns, series, rawFileSaveDir, monitorDrivers, monitorVarsClass, bd.getTargetDir());
			List<DriverAndIndependentVars> daivs = expandExperimentSeries(series, SBHModelPackage.eINSTANCE.getIndependentVariablesOfFilebench(),
					monitorVarsClass, bd, monitorDrivers);

			return daivs;
		} else
		{
			throw new IllegalArgumentException("Unkown ExperimentSeries " + series.getClass());
		}
	}

	/**
	 * Initializes the monitoring drivers with its own connection and attributes
	 * needed during runtime
	 * 
	 * monitorDrivers and monitorVarsClass has to be initialized by the caller
	 * method!
	 * 
	 * @param monitorConns
	 *            A set of RemoteConnections for running the monitors which will
	 *            be provided to the BenchmarkDriver which will be constructed.
	 * @param series
	 *            The experiment series.
	 * @param rawFileSaveDir
	 *            The directory where the benchmark/monitor driver should save
	 *            its raw files. If null no saving should take place.
	 * @param monitorDrivers
	 *            List of monitor drivers. This list will be filled by this
	 *            method.
	 * @param monitorVarsClass
	 *            List of classes of concrete monitor drivers. This list will be
	 *            filled by this method.
	 * @param targetDir
	 *            Represents the target directory for monitoring
	 */
	private static void initialMonitorDrivers(List<RemoteConnection> monitorConns, ExperimentSeries series, String rawFileSaveDir,
			List<MonitorDriver<?, ?>> monitorDrivers, List<EClass> monitorVarsClass, String targetDir)
	{
		if (monitorConns != null && monitorConns.size() > 0)
		{
			for (int i = 0; i < series.getIndependentVariablesOfMonitor().size(); ++i)
			{
				if (series.getIndependentVariablesOfMonitor().get(i) instanceof edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariablesOfBlktrace)
				{
					monitorDrivers.add(new BlktraceMonitorDriver(monitorConns.get(i), rawFileSaveDir));
					monitorVarsClass.add(SBHModelPackage.eINSTANCE.getIndependentVariablesOfBlktrace());
				}

				if (series.getIndependentVariablesOfMonitor().get(i) instanceof edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariablesOfFilesetMonitor)
				{
					monitorDrivers.add(new FilesetMonitorDriver(monitorConns.get(i), rawFileSaveDir, targetDir));
					monitorVarsClass.add(SBHModelPackage.eINSTANCE.getIndependentVariablesOfFilesetMonitor());
				}

				if (series.getIndependentVariablesOfMonitor().get(i) instanceof edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariablesOfFilesizeMonitor)
				{
					monitorDrivers.add(new FilesizeMonitorDriver(monitorConns.get(i), rawFileSaveDir, targetDir));
					monitorVarsClass.add(SBHModelPackage.eINSTANCE.getIndependentVariablesOfFilesizeMonitor());
				}

				if (series.getIndependentVariablesOfMonitor().get(i) instanceof edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependentVariablesOfThreadsMonitor)
				{
					monitorDrivers.add(new ThreadsMonitorDriver(monitorConns.get(i), rawFileSaveDir));
					monitorVarsClass.add(SBHModelPackage.eINSTANCE.getIndependentVariablesOfThreadsMonitor());
				}
				
				if (series.getIndependentVariablesOfMonitor().get(i) instanceof edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.IndependetVariablesOfIostatMonitor)
				{
					monitorDrivers.add(new IostatMonitorDriver(monitorConns.get(i), rawFileSaveDir));
					monitorVarsClass.add(SBHModelPackage.eINSTANCE.getIndependentVariablesOfIostatMonitor());
				}
			}
		}
	}

	/**
	 * Expands the experiment series to single experiments.
	 * 
	 * @param expSeries
	 *            The experiment series.
	 * @param benchmarkDriver
	 *            List of benchmark drivers.
	 * @param benchVarsClass
	 * @param monitorDriver
	 *            List of the monitor drivers.
	 * @param monitorVarsClass
	 *            List of classes of concrete monitor drivers. This list will be
	 *            filled by this method.
	 */
	private static List<DriverAndIndependentVars> expandExperimentSeries(ExperimentSeries expSeries, EClass benchVarsClass,
			List<EClass> monitorVarsClass, BenchmarkDriver<?, ?> benchmarkDriver, List<MonitorDriver<?, ?>> monitorDriver)
	{
		LOGGER.debug("Expanding series %s", expSeries);

		List<IndependentVariablesOfSut> sutVariables = Lists.newArrayList();
		List<IndependentVariablesOfBenchmark> benchVariables = Lists.newArrayList();
		List<IndependentVariablesOfMonitor> monitorVariables = Lists.newArrayList();

		LOGGER.debug("Creating SUTVariables");
		int sutAmount = getInstanceAmount(expSeries.getIndependentVariableSpaceOfSut());
		ExperimentSeriesHelper.usedAmount = 1;
		List<EObjectImpl> sut = instanceObject(expSeries.getIndependentVariableSpaceOfSut(),
				SBHModelPackage.eINSTANCE.getIndependentVariablesOfSut(), sutAmount, sutAmount);
		for (EObjectImpl object : sut)
			sutVariables.add((IndependentVariablesOfSut) object);

		LOGGER.debug("Creating BenchmarkVariables");
		int benchAmount = getInstanceAmount(expSeries.getIndependentVariableSpaceOfBenchmark());
		ExperimentSeriesHelper.usedAmount = 1;
		List<EObjectImpl> bench = instanceObject(expSeries.getIndependentVariableSpaceOfBenchmark(), benchVarsClass, benchAmount, benchAmount);
		for (EObjectImpl object : bench)
			benchVariables.add((IndependentVariablesOfBenchmark) object);

		if (expSeries.getIndependentVariablesOfMonitor() != null && expSeries.getIndependentVariablesOfMonitor().size() > 0)
		{
			LOGGER.debug("Creating MonitorVariables");
			for (int i = 0; i < expSeries.getIndependentVariablesOfMonitor().size(); ++i)
			{
				int monitorAmount = getInstanceAmount(expSeries.getIndependentVariablesOfMonitor().get(i));
				ExperimentSeriesHelper.usedAmount = 1;
				List<EObjectImpl> monitor = instanceObject(expSeries.getIndependentVariablesOfMonitor().get(i), monitorVarsClass.get(i),
						monitorAmount, monitorAmount);
				monitorVariables.add((IndependentVariablesOfMonitor) (monitor.get(0)));
			}
		}

		List<DriverAndIndependentVars> result = Lists.newArrayList();
		for (int i = 0; i < sutAmount; ++i)
		{
			for (int j = 0; j < benchAmount; ++j)
			{
				String uuid = UUID.randomUUID().toString();
				result.add(new DriverAndIndependentVars(benchmarkDriver, monitorDriver, uuid, benchVariables.get(j), sutVariables.get(i),
						monitorVariables));
			}
		}
		return result;
	}

	private static int getInstanceAmount(EObjectImpl object)
	{
		int result = 1;
		for (EAttribute ea : object.eClass().getEAllAttributes())
		{
			if (object.eGet(ea.getFeatureID(), false, false) instanceof List<?>)
			{
				int count = ((List<?>) object.eGet(ea.getFeatureID(), false, false)).size();
				if (count > 0)
					result *= count;
			}
		}

		for (EReference er : object.eClass().getEReferences())
		{
			List<?> resolve = (List<?>) object.eGet(er.getFeatureID(), false, false);
			if (resolve != null && resolve.size() != 0)
			{
				for (int i = 0; i < resolve.size(); ++i)
				{
					EObjectImpl resolveObject = (EObjectImpl) resolve.get(i);
					int count = getInstanceAmount((EObjectImpl) resolveObject);
					if (count > 0)
						result *= count;
				}
			}
		}

		return result;
	}

	/**
	 * Traverses and initializes the objects.
	 * 
	 * @param input
	 * @param resClass
	 * @param localAmount
	 * @param maxAmount
	 * @return Fully initialized object
	 */
	private static List<EObjectImpl> instanceObject(EObjectImpl input, EClass resClass, int localAmount, int maxAmount)
	{
		List<EObjectImpl> result = Lists.newArrayList();
		for (int i = 0; i < maxAmount; ++i)
		{
			Object classContainer = resClass.eContainer();
			if (classContainer instanceof OperationsPackage)
			{
				EClass o = input.eClass();
				result.add((EObjectImpl) edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.OperationsFactory.eINSTANCE.create(o));
			} else
				result.add((EObjectImpl) SBHModelFactory.eINSTANCE.create(resClass));
		}

		for (EAttribute ea : input.eClass().getEAllAttributes())
		{
			if (input.eGet(ea.getFeatureID(), false, false) instanceof List<?>)
			{
				List<?> l = (List<?>) input.eGet(ea.getFeatureID(), false, false);
				if (l.size() > 0)
					ExperimentSeriesHelper.usedAmount *= l.size();
				int localCount = 0;
				for (int i = 0; i < result.size(); ++i)
				{
					EClass resultClass = (result.get(i)).eClass();
					String featureName = input.eClass().getEStructuralFeature(ea.getFeatureID()).getName();
					EStructuralFeature ft = resultClass.getEStructuralFeature(featureName);
					if (l.size() > 0)
					{
						int max = maxAmount / ExperimentSeriesHelper.usedAmount;
						if (max >= localAmount)
							max /= localAmount;
						result.get(i).eSet(ft, l.get((localCount / max) % l.size()));
						++localCount;
					}
				}
			} else if (input.eGet(ea.getFeatureID(), false, false) instanceof Object)
			{
				for (int i = 0; i < result.size(); ++i)
				{
					EClass resultClass = (result.get(i)).eClass();
					String featureName = input.eClass().getEStructuralFeature(ea.getFeatureID()).getName();
					EStructuralFeature ft = resultClass.getEStructuralFeature(featureName);
					if (ft != null)
						result.get(i).eSet(ft, input.eGet(ea.getFeatureID(), false, false));
				}
			}
		}

		for (EReference er : input.eClass().getEAllReferences())
		{
			List<?> resolveList = (List<?>) input.eGet(er.getFeatureID(), false, false);
			if (resolveList != null && resolveList.size() != 0)
			{
				for (int i = 0; i < resolveList.size(); ++i)
				{
					int count = getInstanceAmount((EObjectImpl) resolveList.get(i));
					String featureName = input.eClass().getEStructuralFeature(er.getFeatureID()).getName();
					EReferenceImpl ftref = (EReferenceImpl) resClass.getEStructuralFeature(featureName);
					EClass ftclass = ftref.getEReferenceType();
					List<EObjectImpl> erRes = instanceObject((EObjectImpl) resolveList.get(i), ftclass, count, maxAmount);
					//int localCount = 0;
					for (int j = 0; j < result.size(); ++j)
					{
						EClass resultClass = (result.get(j)).eClass();
						featureName = input.eClass().getEStructuralFeature(er.getFeatureID()).getName();
						EStructuralFeature ft = resultClass.getEStructuralFeature(featureName);
						if (erRes.size() > 0)
						{
							//int max = maxAmount / ExperimentSeriesHelper.usedAmount;
							if (result.get(j).eGet(ft) == null)
								result.get(j).eSet(ft, new ArrayList<EObjectImpl>());
							//((List<EObjectImpl>) result.get(j).eGet(ft)).add(erRes.get((localCount / max) % erRes.size()));
							((List<EObjectImpl>) result.get(j).eGet(ft)).add(erRes.get(j));
							//++localCount;
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * A simple tuple which contains all information needed for a execution of a
	 * single benchmark run:
	 * <ul>
	 * <li>A BenchmarkDriver which will be used to execute the benchark
	 * <li>A unique identifier for this benchmark run, can be used for later
	 * reference of the results
	 * <li>The IndependentVars of the SUT, thus those variables which are
	 * independent of the benchmark
	 * <li>The IndependentVar for the Benchmark
	 * <li>A runNo which can be used to track how often this particular
	 * experiment has been executed.
	 * </ul>
	 * 
	 * @author Dominik Bruhn 
	 * @author Axel Busch
	 * 
	 */
	public final static class DriverAndIndependentVars
	{
		private final BenchmarkDriver<?, ?> benchmarkDriver;
		private final List<MonitorDriver<?, ?>> monitorDriver;
		private final IndependentVariablesOfBenchmark benchVars;
		private final List<IndependentVariablesOfMonitor> monitorVars;
		private final IndependentVariablesOfSut sutVars;
		private final String expUid;

		public DriverAndIndependentVars(BenchmarkDriver<?, ?> benchmarkDriver, List<MonitorDriver<?, ?>> monitorDriver, String expUid,
				IndependentVariablesOfBenchmark benchVars, IndependentVariablesOfSut sutVars, List<IndependentVariablesOfMonitor> monitorVars)
		{
			this.expUid = expUid;
			this.benchmarkDriver = benchmarkDriver;
			this.benchVars = benchVars;
			this.sutVars = sutVars;
			this.monitorVars = monitorVars;
			this.monitorDriver = monitorDriver;
		}

		public BenchmarkDriver<?, ?> getBenchmarkDriver()
		{
			return benchmarkDriver;
		}

		public List<MonitorDriver<?, ?>> getMonitorDriver()
		{
			return monitorDriver;
		}

		public IndependentVariablesOfBenchmark getBenchVars()
		{
			return benchVars;
		}

		public List<IndependentVariablesOfMonitor> getMonitorVars()
		{
			return monitorVars;
		}

		public IndependentVariablesOfSut getSutVars()
		{
			return sutVars;
		}

		public String getExpUid()
		{
			return expUid;
		}
	}

	private static int usedAmount = 0;
}
