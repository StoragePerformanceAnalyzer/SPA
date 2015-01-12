package edu.kit.sdq.storagebenchmarkharness.benchmarks.filebench;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import edu.kit.sdq.storagebenchmarkharness.BenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.RemoteProcess;
import edu.kit.sdq.storagebenchmarkharness.SSHRemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueComposite;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Fileset;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFilebench;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Metric;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Thread;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Type;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Configuration.SystemUnderTest;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.Operation;
import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.exceptions.CheckedBenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.exceptions.ParsingException;
import edu.kit.sdq.storagebenchmarkharness.exceptions.RemoteConnectionException;

/**
 * Provides an interface to the Filebench Benchmark.
 * 
 * As sidenotes, the units of measurement for the
 * IndependentVariablesOfFilebench are as follows:
 * <ul>
 * <li>memsize: b for bytes (can be k for kb, m for mb as well)
 * <li>meanDirWidth: b for bytes (can be k for kb, m for mb as well)
 * <li>meanFileSize: b for bytes (can be k for kb, m for mb as well)
 * <li>prealloc: in %
 * </ul>
 * 
 * @author Axel Busch
 * 
 */
public final class FilebenchBenchmarkDriver extends BenchmarkDriver<IndependentVariablesOfFilebench, DependentVariables>
{

	private static final Logger LOGGER = Logger.getLogger(FilebenchBenchmarkDriver.class);

	private final String targetDir;// On Remote Machine!
	
	private int runTime;
	
	private int timeoutPrepare = 0; // timeput for praparation phase

	public String getTargetDir()
	{
		return targetDir;
	}

	private String confFileWarmup;
	private String confFileBenchmark;

	public FilebenchBenchmarkDriver(RemoteConnection con, String rawFileSaveDir, String targetDir)
	{
		super(con, rawFileSaveDir);
		this.targetDir = targetDir;
		LOGGER.debug("TargetDir is %s", targetDir);
	}

	public FilebenchBenchmarkDriver(RemoteConnection con, String rawFileSaveDir)
	{
		this(con, rawFileSaveDir, getEnvDefault("filebenchtargetdir", "/tmp/filebenchtarget/"));
	}
	
	private boolean observeBenchmark(RemoteProcess benchProcess, RemoteConnection conn, String command, int timeout, int expectedTime) throws InterruptedException
	{
		int watchDogCount = 0;
		
		long startTime = System.currentTimeMillis(); 
		boolean timeErrorOccurred = false;
		
		while (!benchProcess.isClosed())
		{
			java.lang.Thread.sleep(1000);
			List<Integer> watchDogPids;
			if ((watchDogPids = filebenchWatchdog(conn)) != null)
			{
				if (watchDogPids.contains(benchProcess.getPid()))
				{
					if (watchDogCount > 2)
					{
						LOGGER.debug("Restarting filebench...");
						benchProcess.stopProcess();
						benchProcess.finish();
						benchProcess = connection.execCmd(command, true);
						watchDogCount = 0;
						
						// Filebench restarted, reset start time
						startTime = System.currentTimeMillis(); 
					}
					else
						++watchDogCount;
				}
			}
			
			if((System.currentTimeMillis() - startTime) / 1000 > timeout) {
				benchProcess.stopProcess();
				benchProcess.finish(); // The connection is expected to close 
				LOGGER.error("Benchmark timeout.");
				timeErrorOccurred = true;
			}
		}
		
		if((System.currentTimeMillis() - startTime) / 1000 < expectedTime) {
			benchProcess.stopProcess();
			benchProcess.finish(); // The connection is expected to close
			LOGGER.error("Benchmark too short.");
			timeErrorOccurred = true;
		}
		
		return timeErrorOccurred;
	}

	/*
	 * Generates the two configfiles for warmup and benchmarking. These
	 * config-files are saved on the SUT and a copies is saved in the {@code
	 * filesavedir} if it is not {@code null}. In this prepare stage, the
	 * fileset is created and a initial warmup benchmark is executed.
	 */
	@Override
	protected void prepareExperiment(IndependentVariablesOfSut sutVars, IndependentVariablesOfFilebench benchVars) throws CheckedBenchmarkException
	{
		// Generated and save the configurations
		prepareFilebenchConfigurations(sutVars, benchVars);

		// Set Scheduler
		setScheduler(sutVars.getScheduler(), targetDir);

		// Check the Filesystem
		checkFileSystem(sutVars.getFileSystem(), targetDir);

		runTime = benchVars.getRunTime();
		// Warmup-Execution
		LOGGER.debug("Filebench Warmup");

		SystemUnderTest sut = connection.getHost();

		String command = "filebench" + " -f " + confFileWarmup;
		RemoteProcess filebenchWarmup = connection.execCmd(command, true);
		String stdOut;

		try
		{
			RemoteConnection conn = new SSHRemoteConnection(sut);
			conn.open(); // remember to close before returning / proceeding
			// Filebench workaround
			boolean timeErrorOccured = observeBenchmark(filebenchWarmup, conn, command, benchVars.getWarmupTime()*2 + timeoutPrepare, benchVars.getWarmupTime());
			conn.close();
			
			stdOut = CharStreams.toString(new InputStreamReader(filebenchWarmup.getInputStream()));
			String stdErr = CharStreams.toString(new InputStreamReader(filebenchWarmup.getErrorStream()));
			
			// Save Warmup log locally
			saveFile("warmup.log", stdOut);
			
			if (timeErrorOccured)
			{
				LOGGER.debug("Warm-up duration unexpected...");
				throw new CheckedBenchmarkException("Warm-up duration unexpected...");
			}

			if (filebenchWarmup.getExitStatus() != 0)
			{
				LOGGER.error("Warmup Failed: %s / %s", stdOut, stdErr);
				filebenchWarmup.finish();
				throw new BenchmarkException("Filebench warmup did not exit with status 0");
			}
		} catch (IOException e)
		{
			filebenchWarmup.finish();
			LOGGER.error("IO Exception on warmup", e);
			throw new BenchmarkException("Benchmark warmup failed", e);
		} catch (InterruptedException e)
		{
			filebenchWarmup.finish();
			LOGGER.error("IO Exception on warmup", e);
			throw new BenchmarkException("Benchmark warmup failed", e);
		}

		filebenchWarmup.finish();
	}

	/**
	 * This method is used as workaround due to several filebench issues
	 * 
	 * @param conn
	 * @return
	 */
	private List<Integer> filebenchWatchdog(RemoteConnection conn)
	{
		String cmd = "ps ax | grep '<defunct>' | grep -v grep | wc -l";
		RemoteProcess fbWatchdog = conn.execCmd(cmd, false, false);
		BufferedReader br = null;
		ArrayList<Integer> result = new ArrayList<Integer>();
		try
		{
			// Check if defunct process happened
			br = new BufferedReader(new InputStreamReader(fbWatchdog.getInputStream()));
			if (Integer.parseInt(br.readLine()) > 0)
			{
				cmd = "ps -o pid,ppid,command ax | grep '<defunct>' | grep -v 'grep'";
				RemoteProcess fbWatchdog2 = conn.execCmd(cmd, false, false);
				BufferedReader br2 = new BufferedReader(new InputStreamReader(fbWatchdog2.getInputStream()));
				String s;
				while ((s = br2.readLine()) != null)
					result.add(Integer.parseInt(s.replaceAll("\\s+", " ").trim().split(" ")[1]));
				br2.close();
				fbWatchdog2.waitFor();
				fbWatchdog2.finish();
			}
			else
			{	
				// check if no worker is generated by the process
				cmd = "ps -o pid,ppid,command ax | grep 'process1' | grep -v 'grep'";
				RemoteProcess fbWatchdog2 = conn.execCmd(cmd, false, false);
				BufferedReader br2 = new BufferedReader(new InputStreamReader(fbWatchdog2.getInputStream()));
				String s;
				while ((s = br2.readLine()) != null && s != "")
				{
					if (Integer.parseInt(s.replaceAll("\\s+", " ").trim().split(" ")[1]) == 1)
					{
						cmd = "sudo kill " + s.replaceAll("\\s+", " ").trim().split(" ")[0];
						RemoteProcess fbWatchdog3 = conn.execCmd(cmd, false, false);
						fbWatchdog3.waitFor();
						fbWatchdog3.finish();
					}
				}
				br2.close();
				fbWatchdog2.waitFor();
				fbWatchdog2.finish();
				return null;
			}
		} catch (IOException e)
		{
			try
			{
				if (br != null) {
					br.close();
				}
			} catch (IOException e1)
			{
				LOGGER.debug("IOException");
			}
			fbWatchdog.finish();
		}
		LOGGER.debug("Filebench bad");
		fbWatchdog.waitFor();
		fbWatchdog.finish();
		return result;
	}
	
	/**
	 * Generates the Filebench Configurations for the warmup and the actual
	 * benchmarking. It saves the configs remotely and also locally if the
	 * {@code filesavedir} is set.
	 * 
	 * @param expUid
	 * @param sutVars
	 * @param benchVars
	 */
	protected void prepareFilebenchConfigurations(IndependentVariablesOfSut sutVars, IndependentVariablesOfFilebench benchVars)
	{
		String confId = UUID.randomUUID().toString();
		confFileWarmup = "/tmp/" + confId + ".warmup.filebench";
		confFileBenchmark = "/tmp/" + confId + ".bench.filebench";
		LOGGER.debug("Filebench config file is %s and %s", confFileWarmup, confFileBenchmark);

		StringBuffer conf = new StringBuffer();
		String statement;

		int totalNoOfFiles = 0; // use this information to calculate timeout for preparation phase
		
		// Create filesets
		for (int i = 0; i < benchVars.getFilesets().size(); ++i)
		{
			Fileset fs = benchVars.getFilesets().get(i);
			String targetDir = this.targetDir + fs.getDirectory().getAbsolutePath();
			targetDir = targetDir.replaceAll("//", "/");
			
			totalNoOfFiles += fs.getFiles();

			statement = "define fileset name=" + fs.getFilesetName() + ",path=" + targetDir + ",size=" + fs.getMeanFileSize() + ",entries="
					+ fs.getFiles() + ",dirwidth=" + fs.getMeanDirWidth() + ",prealloc=" + fs.getPrealloc() + ",reuse=0" + "\n";
			conf.append(statement);
		}
		
		// set timeout for preparation phase
		timeoutPrepare = totalNoOfFiles / 1000; 
		
		// Create process
		statement = "define process name=process1,instances=1\n{\n";
		conf.append(statement);

		// Create threads
		for (int i = 0; i < benchVars.getThreads().size(); ++i)
		{
			Thread th = benchVars.getThreads().get(i);
			statement = "thread name=" + th.getThreadName() + ",memsize=" + th.getMemsize() + ",instances=" + th.getInstances() + "\n{\n";
			conf.append(statement);

			// Create operations
			for (int j = 0; j < th.getOperations().size(); ++j)
			{
				Operation op = th.getOperations().get(j);

				EClass opClass = ((EObject) op).eClass();
				List<EAttribute> eAttributes = opClass.getEAllAttributes();

				if (!op.isWarmupOperation())
					conf.append("#");
				conf.append("flowop " + op.getFlowOpName() + " name=" + op.getOperationName());
				for (int k = 3; k < eAttributes.size(); ++k)
				{
					EAttribute attribute = eAttributes.get(k);
					Object attributeValue = ((EObject) op).eGet(opClass.getEStructuralFeature(attribute.getFeatureID()));

					if (attributeValue != null && !attributeValue.equals(""))
						conf.append("," + attribute.getName() + "=" + attributeValue);
				}
				conf.append("\n");
			}
			conf.append("}\n");
		}
		conf.append("}\n");
		String warmupConf = conf.toString() + "run " + benchVars.getWarmupTime();
		String benchConf = conf.toString() + "run " + benchVars.getRunTime();

		connection.saveStringToFile(warmupConf, confFileWarmup, false);
		connection.saveStringToFile(benchConf.replaceFirst("reuse=0", "reuse=1").replaceAll("#", ""), confFileBenchmark, false);

		// Save a copy locally on the Measurement Machine for later debugging.
		saveFile("bench.filebench", conf.toString());
	}

	/*
	 * Executes FFSB or actual benchmarking. The results of the run is saved for
	 * debugging purposes. The results the benchmark run is parsed and returned.
	 */
	@Override
	public DependentVariables startExperiment(int repeatNo) throws CheckedBenchmarkException
	{
		LOGGER.debug("Executing Filebench for #%d", repeatNo);

		SystemUnderTest sut = connection.getHost();
		// Benchmark-Execution
		String command = "filebench" + " -f " + confFileBenchmark;
		RemoteProcess filebenchBench = connection.execCmd(command, true);

		File logFile = getFile("filebenchBench." + repeatNo + ".log");

		RemoteConnection conn = new SSHRemoteConnection(sut);
		try
		{
			conn.open(); // remember to close before returning / proceeding
			boolean timeErrorOccured = observeBenchmark(filebenchBench, conn, command, runTime * 2, runTime);
			conn.close();
			
			if (timeErrorOccured)
			{
				LOGGER.error("Benchmark duration unexpected...");
				filebenchBench.finish();
				throw new CheckedBenchmarkException("Benchmark duration unexpected...");
			}
			
			List<DependentVariablesValueComposite> values = parseFilebenchLogAndSave(
					new BufferedReader(new InputStreamReader(filebenchBench.getInputStream())), logFile);
			if (values == null) {
				LOGGER.error("No results obtained from filebench log");
				filebenchBench.finish();
				throw new CheckedBenchmarkException("No results obtained from filebench log");
			}
			String stdErr = CharStreams.toString(new InputStreamReader(filebenchBench.getErrorStream()));

			if (filebenchBench.getExitStatus() != 0)
			{
				LOGGER.error("Benchmarking failed: %s", stdErr);
				throw new BenchmarkException("Filebench Benchmarking failed");
			}

			filebenchBench.finish();

			DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();
			result.getValues().clear();
			result.getValues().addAll(values);
			result.setBenchmarkPrefix("filebench");
			return result;
		} catch (IOException e)
		{
			LOGGER.error("IO Exception on Benchmark parsing", e);
			throw new BenchmarkException("Benchmark parsing failed due to IO interrupt", e);
		} catch (ParsingException e)
		{
			LOGGER.error("Parsing exception on Benchmark parsing", e);
			throw new BenchmarkException("Benchmark parsing failed due to invalid log file", e);
		} catch (InterruptedException e)
		{
			LOGGER.error("Parsing exception on Benchmark parsing", e);
			throw new BenchmarkException("Benchmark parsing failed due to invalid log file", e);
		} finally {
			// Always disconnect
			filebenchBench.finish();
		}
	}

	/*
	 * Deletes temporary files and the two configfiles from the SUT.
	 */
	@Override
	public void endExperiment() throws RemoteConnectionException
	{
		connection.deleteFile(confFileBenchmark);
		connection.deleteFile(confFileWarmup);
		
		String command = "rm /tmp/filebench-*";
		RemoteProcess cleanup = connection.execCmd(command, false);
		cleanup.waitFor();
		
		command = "rm /tmp/*.filebench";
		cleanup = connection.execCmd(command, false);
		cleanup.waitFor();
		
		// Delete all file sets in target directory
		if(targetDir.endsWith("/")) {
			command = "rm -r " + targetDir + "*";
		} else {
			command = "rm -r " + targetDir + "/*";
		}
		cleanup = connection.execCmd(command, false);
		cleanup.waitFor();
	}

	/**
	 * Parses the Filebench Output into DependentVariables which will be used
	 * and saved by the benchmark controller.
	 * 
	 * @param reader
	 *            A BufferedReader which provides the output of a filebench-run
	 * @param outputFile
	 *            The file where a copy of the raw output of ffsb should be
	 *            saved to. Can be null if no saving is required.
	 * @return A list of DependentVariables containing the number of operations
	 *         for each operation FFSB executed. If the logfile is invalid, null
	 *         is returned.
	 */
	public static List<DependentVariablesValueComposite> parseFilebenchLogAndSave(BufferedReader reader, File outputFile) throws ParsingException
	{
		String line = null;
		List<DependentVariablesValueComposite> results = Lists.newArrayList();

		OutputStream out = null;
		try
		{
			if (outputFile != null)
			{
				out = new BufferedOutputStream(new FileOutputStream(outputFile));
			}

			while ((line = reader.readLine()) != null)
			{
				if (out != null)
				{
					out.write(line.getBytes(Charset.defaultCharset()));
					out.write('\n');
				}

				line = line.replaceAll("\\s+", " ").trim();
				if (line.contains("ops") && line.contains("us/op-cpu"))
				{
					String[] lineElems = line.split(" ");
					results.add(createOpsResult(lineElems[0].trim(), Integer.parseInt(lineElems[1].trim().replaceFirst("ops", ""))));
					results.add(createThroughputResult(lineElems[0].trim(), Double.parseDouble(lineElems[3].trim().replaceFirst("mb/s", ""))));
					results.add(createLatencyResult(lineElems[0].trim(), Double.parseDouble(lineElems[4].trim().replaceFirst("ms/op", ""))));
				} else if (line.contains("Summary:"))
				{
					String[] lineElems = line.split(" ");
					results.add(createOpsResult(lineElems[3].trim(), Integer.parseInt(lineElems[4])));
					results.add(createThroughputResult(lineElems[3].trim(), Double.parseDouble(lineElems[10].trim().replaceFirst("mb/s,", ""))));
					results.add(createLatencyResult(lineElems[3].trim(), Double.parseDouble(lineElems[13].replaceFirst("ms", ""))));
					if (out != null)
					{
						out.flush();
						out.close();
					}

					return results;
				}
			}
			LOGGER.error("Log did not contain the last line, perhaps invalid?");
			return null;
		} catch (IOException e)
		{
			LOGGER.error("IO Exception on Benchmark parsing", e);
			throw new BenchmarkException("Benchmark parsing failed due to IO interrupt", e);
		}
	}

	/**
	 * Shortcut method for simpler creation of {@code DependentVariablesValue}
	 * instances.
	 * 
	 * @param operation
	 *            Operation name
	 * @param ops
	 *            number of operations
	 * @return
	 */
	private static DependentVariablesValueComposite createOpsResult(String operation, int ops)
	{
		DependentVariablesValueComposite depVarsValueComp = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
		depVarsValueComp.setOperation(operation);
		depVarsValueComp.setValue(ops);
		depVarsValueComp.setType(Type.ABSOLUTE);
		depVarsValueComp.setSource("filebench");
		depVarsValueComp.setOperationMetric(Metric.OPERATIONS);

		return depVarsValueComp;
	}
	
	private static DependentVariablesValueComposite createLatencyResult(String operation, double latency)
	{
		DependentVariablesValueComposite depVarsValueComp = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
		depVarsValueComp.setOperation(operation);
		depVarsValueComp.setValue(latency);
		depVarsValueComp.setType(Type.MEAN);
		depVarsValueComp.setSource("filebench");
		depVarsValueComp.setOperationMetric(Metric.RESPONSE_TIME);

		return depVarsValueComp;
	}
	
	private static DependentVariablesValueComposite createThroughputResult(String operation, double throughput)
	{
		DependentVariablesValueComposite depVarsValueComp = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
		depVarsValueComp.setOperation(operation);
		depVarsValueComp.setValue(throughput);
		depVarsValueComp.setType(Type.MEAN);
		depVarsValueComp.setSource("filebench");
		depVarsValueComp.setOperationMetric(Metric.THROUGHPUT);

		return depVarsValueComp;
	}
}
