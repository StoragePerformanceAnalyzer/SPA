package edu.kit.sdq.storagebenchmarkharness.monitors;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.MonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.RemoteProcess;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValue;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueComposite;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueSingle;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBlktrace;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Metric;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Type;
import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.util.AccessVisualizer;
import edu.kit.sdq.storagebenchmarkharness.util.Parsing;
import edu.kit.sdq.storagebenchmarkharness.util.PatternRecognizer;

/**
 * Provides an interface to the Blktrace monitoring tool.
 * 
 * As sidenotes, the range of selected IVs of Blktrace's are as follows:
 * <ul>
 * <li>bufferSize: [0,9999]
 * </ul>
 * 
 * @author Axel Busch
 * 
 */
public class BlktraceMonitorDriver extends MonitorDriver<IndependentVariablesOfBlktrace, DependentVariables>
{

	private static final Logger LOGGER = Logger.getLogger(BlktraceMonitorDriver.class);

	private final String targetDir;// On Remote Machine!
	private File workDir;

	private RemoteProcess blktraceMonitor;

	private InputStreamReader is;

	public BlktraceMonitorDriver(RemoteConnection con, String logFileSaveDir, String targetDir)
	{
		super(con, logFileSaveDir);
		this.targetDir = targetDir;
		LOGGER.debug("TargetDir is %s", targetDir);
	}

	public BlktraceMonitorDriver(RemoteConnection con, String logFileSaveDir)
	{
		this(con, logFileSaveDir, getEnvDefault("blktracetargetdir", "/tmp/blktracetarget/"));
	}

	/**
	 * Executes Blktrace or actual monitoring tool and saves log file.
	 * 
	 * @param sutVars
	 * @param monitorVars
	 **/
	@Override
	public void startMonitoring(IndependentVariablesOfSut sutVars, IndependentVariablesOfBlktrace monitorVars)
	{
		LOGGER.debug("Executing Blktrace...");
		String command = null;
		File destination = new File(targetDir);
		
		command = "ls "+ destination.getAbsolutePath();
		RemoteProcess testTargetDir = connection.execCmd(command, false, true);
		testTargetDir.waitFor();
		if(testTargetDir.getExitStatus() != 0) {
			testTargetDir.finish();
			LOGGER.error("Target monitoring directory test failed. Make sure " + destination.getAbsolutePath() + " exists");
			throw new BenchmarkException("Monitor failed");
		}
		testTargetDir.finish();
		
		int bufferSize = monitorVars.getBufferSize();
		int bufferNum = monitorVars.getNumberOfBuffers();
		String device = monitorVars.getTargetDevice();
		command = "sudo blktrace -d " + device + " -D " + destination.getAbsoluteFile() + "/" + monitorVars.getLogFilePrefix() + " -b " + bufferSize
				+ " -n " + bufferNum;
		command = command.replaceAll("//", "/");
		this.blktraceMonitor = connection.execCmd(command, true);

		try
		{
			is = new InputStreamReader(blktraceMonitor.getInputStream());
		} catch (IOException e)
		{
			blktraceMonitor.finish();
			LOGGER.error("Exception while stopping monitor", e);
			throw new BenchmarkException("Monitor failed", e);
		}
	}

	/**
	 * Stops Blktrace or actual monitoring tool and returns the recorded values.
	 **/
	@Override
	public DependentVariables stopMonitoring(IndependentVariablesOfBlktrace monitorVars, int repeatNr, String benchmarkPrefix)
	{
		
		try
		{
			// stop monitor process
			this.blktraceMonitor.stopProcess();
			File logFile = getFile("blktrace." + repeatNr + ".log");
			BufferedReader reader = new BufferedReader(is);
			String line;
			try
			{
				OutputStream out = null;
				if (logFile != null)
				{
					out = new BufferedOutputStream(new FileOutputStream(logFile));
				}

				while ((line = reader.readLine()) != null)
				{
					if (out != null)
					{
						out.write(line.getBytes(Charset.defaultCharset()));
						out.write('\n');
					}
				}
				if (out != null)
				{
					out.flush();
					out.close();
				}
			} catch (IOException e)
			{
				throw new BenchmarkException(e);
			}
		} catch (Exception e)
		{
			this.blktraceMonitor.finish();
			LOGGER.error("Exception while stopping monitor", e);
			throw new BenchmarkException("Monitor failed", e);
		}

		this.blktraceMonitor.finish();

		// parsing results
		parsingResults(monitorVars);

		// block files
		File resultFileReads = getReadBlocksFile();
		File resultFileWrites = getWriteBlocksFile();
		File resultFileQ2CReads = getReadQ2CFile();
		File resultFileQ2CWrites = getWriteQ2CFile();
		File resultFileAQDReads = getReadAQDFile();
		File resultFileAQDWrites = getWriteAQDFile();

		DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();

		result.setBenchmarkPrefix(benchmarkPrefix);
		result.getValues().clear();

		int readNums = 0;
		if (resultFileReads != null)
		{
			List<DependentVariablesValueSingle> blockValuesReads = getBlockValues(resultFileReads, repeatNr, "read");

			readNums = blockValuesReads.size();
			result.getValues().addAll(blockValuesReads);

			if (blockValuesReads.size() > 0)
			{
				// Calculate avg. request size reads
				if (monitorVars.isAvgRequestSize())
				{
					double avgRequSizeRead = calcAvgReqSize(blockValuesReads);

					DependentVariablesValueComposite avgReqSizeComposite = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
					avgReqSizeComposite.setOperation("read");
					avgReqSizeComposite.setType(Type.MEAN);
					avgReqSizeComposite.setValue(avgRequSizeRead);
					avgReqSizeComposite.setSource("blktrace");
					avgReqSizeComposite.setOperationMetric(Metric.REQUEST_SIZE);
					result.getValues().add(avgReqSizeComposite);
				}

				// Calculate request access pattern
				if (monitorVars.isAccessPattern())
				{
					List<DependentVariablesValue> tmp = calculateAccessPattern(Parsing.divideLineArray(fileToArray(resultFileReads), " "), "read",
							monitorVars.getPatternWindowSize());
					if (tmp != null)
						result.getValues().addAll(tmp);
				}

				// Visualize access pattern
				if (monitorVars.isVisualizeAccessPattern())
					AccessVisualizer.visualize(Parsing.divideLineArray(fileToArray(resultFileReads), " "), getFile("bench.reads." + repeatNr
							+ ".accessPattern.log"));

				// Calculate avg ops per file
				if (monitorVars.isOpsPerFile())
				{
					DependentVariablesValueComposite avgOpsPerFileRead = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
					avgOpsPerFileRead.setOperation("read");
					avgOpsPerFileRead.setType(Type.MEAN);
					avgOpsPerFileRead.setValue(calculateAvgOpsPerFile(Parsing.divideLineArray(fileToArray(resultFileReads), " "),
							monitorVars.getOpsWindowSize()));
					avgOpsPerFileRead.setSource("blktrace");
					avgOpsPerFileRead.setOperationMetric(Metric.OPS_PER_FILE);
					if (avgOpsPerFileRead.getValue() >= 0)
						result.getValues().add(avgOpsPerFileRead);
				}
			}

		}

		if (monitorVars.isRecordQ2c() && resultFileQ2CReads != null)
		{
			List<DependentVariablesValueSingle> readQ2c = getQ2CTimes(resultFileQ2CReads, repeatNr, "read");
			result.getValues().addAll(readQ2c);

			DependentVariablesValueComposite avgQ2C = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
			avgQ2C.setOperation("read");
			avgQ2C.setType(Type.MEAN);
			avgQ2C.setValue(calculateAvg(readQ2c));
			avgQ2C.setSource("blktrace");
			avgQ2C.setOperationMetric(Metric.RESPONSE_TIME);
			result.getValues().add(avgQ2C);
		}
		
		if (monitorVars.isActiveQueueDepth() && resultFileAQDReads != null)
		{
			List<DependentVariablesValueSingle> readAQD = getAQDVals(resultFileAQDReads, repeatNr, "read");
			result.getValues().addAll(readAQD);
			
			DependentVariablesValueComposite avgAQD = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
			avgAQD.setOperation("read");
			avgAQD.setType(Type.MEAN);
			avgAQD.setValue(calculateAvg(readAQD));
			avgAQD.setSource("blktrace");
			avgAQD.setOperationMetric(Metric.QUEUE_DEPTH);
			result.getValues().add(avgAQD);
		}

		int writeNums = 0;
		if (resultFileWrites != null)
		{
			List<DependentVariablesValueSingle> blockValuesWrites = getBlockValues(resultFileWrites, repeatNr, "write");

			result.getValues().addAll(blockValuesWrites);

			writeNums = blockValuesWrites.size();
			if (blockValuesWrites.size() > 0)
			{
				// Calculate avg. request size writes
				if (monitorVars.isAvgRequestSize())
				{
					double avgRequSizeWrite = calcAvgReqSize(blockValuesWrites);

					DependentVariablesValueComposite avgReqSizeComposite = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
					avgReqSizeComposite.setOperation("write");
					avgReqSizeComposite.setType(Type.MEAN);
					avgReqSizeComposite.setSource("blktrace");
					avgReqSizeComposite.setOperationMetric(Metric.REQUEST_SIZE);
					avgReqSizeComposite.setValue(avgRequSizeWrite);
					result.getValues().add(avgReqSizeComposite);
				}

				// Calculate request access pattern
				if (monitorVars.isAccessPattern())
				{
					result.getValues().addAll(
							calculateAccessPattern(Parsing.divideLineArray(fileToArray(resultFileWrites), " "), "write",
									monitorVars.getPatternWindowSize()));
				}

				// Visualize access pattern
				if (monitorVars.isVisualizeAccessPattern())
					AccessVisualizer.visualize(Parsing.divideLineArray(fileToArray(resultFileWrites), " "), new File("bench.writes." + repeatNr
							+ ".accessPattern.log"));

				// Calculate avg ops per file
				if (monitorVars.isOpsPerFile())
				{
					DependentVariablesValueComposite avgOpsPerFileWrite = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
					avgOpsPerFileWrite.setOperation("write");
					avgOpsPerFileWrite.setType(Type.MEAN);
					avgOpsPerFileWrite.setValue(calculateAvgOpsPerFile(Parsing.divideLineArray(fileToArray(resultFileWrites), " "),
							monitorVars.getOpsWindowSize()));
					avgOpsPerFileWrite.setSource("blktrace");
					avgOpsPerFileWrite.setOperationMetric(Metric.OPS_PER_FILE);
					result.getValues().add(avgOpsPerFileWrite);
				}
			}
		}

		if (monitorVars.isRecordQ2c() && resultFileQ2CWrites != null)
		{
			List<DependentVariablesValueSingle> writeQ2C = getQ2CTimes(resultFileQ2CWrites, repeatNr, "write");
			result.getValues().addAll(writeQ2C);

			DependentVariablesValueComposite avgQ2C = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
			avgQ2C.setOperation("write");
			avgQ2C.setType(Type.MEAN);
			avgQ2C.setValue(calculateAvg(writeQ2C));
			avgQ2C.setSource("blktrace");
			avgQ2C.setOperationMetric(Metric.RESPONSE_TIME);
			result.getValues().add(avgQ2C);
		}
		
		if (monitorVars.isActiveQueueDepth() && resultFileAQDWrites != null)
		{
			List<DependentVariablesValueSingle> writeAQD = getAQDVals(resultFileAQDWrites, repeatNr, "write");
			result.getValues().addAll(writeAQD);
			
			DependentVariablesValueComposite avgAQD = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
			avgAQD.setOperation("write");
			avgAQD.setType(Type.MEAN);
			avgAQD.setValue(calculateAvg(writeAQD));
			avgAQD.setSource("blktrace");
			avgAQD.setOperationMetric(Metric.QUEUE_DEPTH);
			result.getValues().add(avgAQD);
		}

		// Add request mix
		if (monitorVars.isRequestMix())
		{
			DependentVariablesValueComposite requMix = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
			requMix.setOperation("read/(read+write)");
			requMix.setType(Type.PERCENTAGE);
			requMix.setSource("blktrace");
			requMix.setOperationMetric(Metric.REQUEST_MIX);
			if ((readNums + writeNums) > 0)
				requMix.setValue((readNums + 0.0) / (readNums + writeNums));
			result.getValues().add(requMix);
		}

		return result;
	}

	@Override
	public void endMonitoring()
	{
		// cleanup
		String command = "rm -R " + workDir.getAbsolutePath();
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();
		postProcessing.finish();
	}

	private void parsingResults(IndependentVariablesOfBlktrace monitorVars)
	{
		LOGGER.debug("Merging results...");
		File destination = new File(targetDir);

		workDir = new File(targetDir, monitorVars.getLogFilePrefix());
		File resultFile = new File(workDir + "/" + monitorVars.getTargetDevice().split("/")[2]);
		// resultFile.mkdir();
		File mergedFile = new File(workDir + "/" + monitorVars.getTargetDevice().split("/")[2] + ".merged");
		// mergedFile.mkdir();

		String command = "sudo changeOwner.sh " + resultFile.getParent();
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();
		postProcessing.finish();

		// processing results
		LOGGER.debug("Running blkparse...");
		command = "blkparse -i " + resultFile.getAbsolutePath() + " -d " + mergedFile.getAbsolutePath() + " > /dev/null";
		LOGGER.debug("Command is: %s", command);
		postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();
		postProcessing.finish();

		LOGGER.debug("Creating btt read results...");

		// btt blocks
		command = "btt -i " + mergedFile.getAbsoluteFile() + " -B " + destination.getAbsolutePath() + "/" + monitorVars.getLogFilePrefix()
				+ "/blocks > /dev/null";
		LOGGER.debug("Command is: %s", command);
		postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();
		postProcessing.finish();
		
		
		if (monitorVars.isActiveQueueDepth() || monitorVars.isRecordQ2c())
		{
			
			// generate read Q2C response times
			command = "blkparse -i " + resultFile.getAbsolutePath() + " -d " + mergedFile.getAbsolutePath() + " -a read > /dev/null";
			LOGGER.debug("Command is: %s", command);
			postProcessing = connection.execCmd(command, false);
			postProcessing.waitFor();
			postProcessing.finish();
			
			if (monitorVars.isActiveQueueDepth())
			{
				LOGGER.debug("Creating btt read active queue depth values...");
				command = "btt -i " + mergedFile.getAbsoluteFile() + " -Q " + destination.getAbsolutePath() + "/" + monitorVars.getLogFilePrefix()
						+ "/queue_depth_read > /dev/null";
				LOGGER.debug("Command is: %s", command);
				postProcessing = connection.execCmd(command, false);
				postProcessing.waitFor();
				postProcessing.finish();
			}
			
			if (monitorVars.isRecordQ2c())
			{
				LOGGER.debug("Creating btt Q2C read response times...");
				command = "btt -i " + mergedFile.getAbsoluteFile() + " -q " + destination.getAbsolutePath() + "/" + monitorVars.getLogFilePrefix()
						+ "/q2c_read > /dev/null";
				LOGGER.debug("Command is: %s", command);
				postProcessing = connection.execCmd(command, false);
				postProcessing.waitFor();
				postProcessing.finish();
			}
			
			// generate write Q2C response times
			command = "blkparse -i " + resultFile.getAbsolutePath() + " -d " + mergedFile.getAbsolutePath() + " -a write > /dev/null";
			LOGGER.debug("Command is: %s", command);
			postProcessing = connection.execCmd(command, false);
			postProcessing.waitFor();
			postProcessing.finish();
			
			if (monitorVars.isActiveQueueDepth())
			{
				LOGGER.debug("Creating btt write active queue depth values...");
				command = "btt -i " + mergedFile.getAbsoluteFile() + " -Q " + destination.getAbsolutePath() + "/" + monitorVars.getLogFilePrefix()
						+ "/queue_depth_write > /dev/null";
				LOGGER.debug("Command is: %s", command);
				postProcessing = connection.execCmd(command, false);
				postProcessing.waitFor();
				postProcessing.finish();
			}
			
			if (monitorVars.isRecordQ2c())
			{
				LOGGER.debug("Creating btt Q2C write response times...");
				command = "btt -i " + mergedFile.getAbsoluteFile() + " -q " + destination.getAbsolutePath() + "/" + monitorVars.getLogFilePrefix()
						+ "/q2c_write > /dev/null";
				LOGGER.debug("Command is: %s", command);
				postProcessing = connection.execCmd(command, false);
				postProcessing.waitFor();
				postProcessing.finish();
			}
		}

		try
		{
			String stdErr = CharStreams.toString(new InputStreamReader(postProcessing.getErrorStream()));
			if (postProcessing.getExitStatus() != 0)
			{
				postProcessing.finish();
				LOGGER.error("Monitor postprocessing: %s", stdErr);
				throw new BenchmarkException("Monitor postprocessing failed");
			}

			postProcessing.finish();
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("IO Exception on Benchmark parsing", e);
			throw new BenchmarkException("Benchmark parsing failed due to IO interrupt", e);
		}
	}

	private File getReadBlocksFile()
	{
		// catch read blocks
		String command = "ls " + workDir + " | grep _r";
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();

		InputStreamReader is;
		File resultFileReads = null;
		try
		{
			is = new InputStreamReader(postProcessing.getInputStream());
			BufferedReader br = new BufferedReader(is);
			String s;
			if ((s = br.readLine()) != null)
				resultFileReads = new File(workDir, s);
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}
		postProcessing.finish();
		return resultFileReads;
	}

	private File getReadQ2CFile()
	{
		// catch read Q2Cs file
		String command = "ls " + workDir + " | grep q2c_read";
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();

		InputStreamReader is;
		File resultFileReads = null;
		try
		{
			is = new InputStreamReader(postProcessing.getInputStream());
			BufferedReader br = new BufferedReader(is);
			String s;
			if ((s = br.readLine()) != null)
				resultFileReads = new File(workDir, s);
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}
		postProcessing.finish();
		return resultFileReads;
	}
	
	private File getReadAQDFile()
	{
		// catch read Q2Cs file
		String command = "ls " + workDir + " | grep queue_depth_read";
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();

		InputStreamReader is;
		File resultFileReads = null;
		try
		{
			is = new InputStreamReader(postProcessing.getInputStream());
			BufferedReader br = new BufferedReader(is);
			String s;
			if ((s = br.readLine()) != null)
				resultFileReads = new File(workDir, s);
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}
		postProcessing.finish();
		return resultFileReads;
	}

	private File getWriteQ2CFile()
	{
		// catch write Q2Cs file
		String command = "ls " + workDir + " | grep q2c_write";
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();

		InputStreamReader is;
		File resultFileReads = null;
		try
		{
			is = new InputStreamReader(postProcessing.getInputStream());
			BufferedReader br = new BufferedReader(is);
			String s;
			if ((s = br.readLine()) != null)
				resultFileReads = new File(workDir, s);
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}
		postProcessing.finish();
		return resultFileReads;
	}
	
	private File getWriteAQDFile()
	{
		// catch read Q2Cs file
		String command = "ls " + workDir + " | grep queue_depth_write";
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();

		InputStreamReader is;
		File resultFileReads = null;
		try
		{
			is = new InputStreamReader(postProcessing.getInputStream());
			BufferedReader br = new BufferedReader(is);
			String s;
			if ((s = br.readLine()) != null)
				resultFileReads = new File(workDir, s);
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}
		postProcessing.finish();
		return resultFileReads;
	}

	private File getWriteBlocksFile()
	{
		// catch write blocks
		String command = "ls " + workDir + " | grep _w";
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		postProcessing.waitFor();

		File resultFileWrites = null;
		try
		{
			is = new InputStreamReader(postProcessing.getInputStream());
			BufferedReader br = new BufferedReader(is);
			String s;
			if ((s = br.readLine()) != null)
				resultFileWrites = new File(workDir, s);
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}
		postProcessing.finish();
		return resultFileWrites;
	}

	private List<DependentVariablesValueSingle> getQ2CTimes(File resultFile, int repeatNr, String operation)
	{
		// catch Q2C vals
		String command = "cat " + resultFile.getAbsolutePath();
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		List<DependentVariablesValueSingle> q2cValues;
		try
		{
			InputStream inStream = postProcessing.getInputStream();
			InputStreamReader inStreamReader = new InputStreamReader(inStream);
			BufferedReader br = new BufferedReader(inStreamReader);

			List<String> inputFile = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null)
			{
				inputFile.add(line);
			}
			q2cValues = parseBlktraceQ2CLogAndSave(inputFile, getFile("bench.q2cs." + operation + "." + repeatNr + ".log"), operation);
			postProcessing.waitFor();
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}

		return q2cValues;
	}
	
	private List<DependentVariablesValueSingle> getAQDVals(File resultFile, int repeatNr, String operation)
	{
		// catch AQD vals
		String command = "cat " + resultFile.getAbsolutePath();
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		List<DependentVariablesValueSingle> aqdValues;
		try
		{
			InputStream inStream = postProcessing.getInputStream();
			InputStreamReader inStreamReader = new InputStreamReader(inStream);
			BufferedReader br = new BufferedReader(inStreamReader);

			List<String> inputFile = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null)
			{
				inputFile.add(line);
			}
			aqdValues = parseBlktraceAQDLogAndSave(inputFile, getFile("bench.aqds." + operation + "." + repeatNr + ".log"), operation);
			postProcessing.waitFor();
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}

		return aqdValues;
	}

	private List<DependentVariablesValueSingle> getBlockValues(File resultFile, int repeatNr, String operation)
	{
		// catch block vals
		String command = "cat " + resultFile.getAbsolutePath();
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		List<DependentVariablesValueSingle> blockValues;
		try
		{
			InputStream inStream = postProcessing.getInputStream();
			InputStreamReader inStreamReader = new InputStreamReader(inStream);
			BufferedReader br = new BufferedReader(inStreamReader);

			List<String> inputFile = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null)
			{
				inputFile.add(line);
			}
			blockValues = parseBlktraceBlocksLogAndSave(inputFile, getFile("bench." + operation + "." + repeatNr + ".log"), operation);
			postProcessing.waitFor();
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}

		return blockValues;
	}

	private double calcAvgReqSize(List<DependentVariablesValueSingle> blockValues)
	{
		double avgRequSize = 0.;
		for (int i = 0; i < blockValues.size(); ++i)
		{
			avgRequSize += blockValues.get(i).getValue();
		}
		avgRequSize /= blockValues.size();

		return avgRequSize;
	}

	private double calculateAvg(List<DependentVariablesValueSingle> values)
	{
		double avg = 0.;
		for (int i = 0; i < values.size(); ++i)
		{
			avg += values.get(i).getValue();
		}
		avg /= values.size();

		return avg;
	}

	private List<DependentVariablesValue> calculateAccessPattern(List<String[]> values, String operation, int windowSize)
	{
		ArrayList<DependentVariablesValue> result = Lists.newArrayList();
		ArrayList<String[]> tmpArray = null;

		if (windowSize < 1)
		{
			DependentVariablesValueSingle depVar = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
			depVar.setOperation(operation);
			depVar.setSource("blktrace");
			depVar.setOperationMetric(Metric.ACCESS);
			depVar.setValue(PatternRecognizer.recognize(values,false));
			if (depVar.getValue() >= 0)
				result.add(depVar);
			return result;
		}

		int i = 0;
		for (int j = 1; j <= Double.parseDouble(values.get(values.size() - 1)[0]) / windowSize; ++j)
		{
			tmpArray = Lists.newArrayList();

			while (Double.parseDouble(values.get(i)[0]) / windowSize < j)
			{
				tmpArray.add(values.get(i++));
			}

			DependentVariablesValueSingle depVar = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
			depVar.setOperation(operation);
			depVar.setSource("blktrace");
			depVar.setOperationMetric(Metric.ACCESS);
			depVar.setValue(PatternRecognizer.recognize(tmpArray,false));
			if (depVar.getValue() >= 0)
				result.add(depVar);
		}

		if (result.size() > 1)
		{
			double avg = 0.;
			for (int j = 0; j < result.size(); ++j)
			{
				avg += result.get(j).getValue();
			}

			DependentVariablesValueComposite depVar = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
			depVar.setOperation(operation);
			depVar.setSource("blktrace");
			depVar.setOperationMetric(Metric.ACCESS);
			depVar.setType(Type.MEAN);
			depVar.setValue(avg / result.size());
			result.add(depVar);
		}

		return result;
	}

	public double calculateAvgOpsPerFile(List<String[]> values, int windowSize)
	{
		if (windowSize < 1)
			return PatternRecognizer.getAvgOpsPerFile(values);

		ArrayList<String[]> tmpArray = null;
		double tmpResult = 0.0;
		int iters = 0;
		int i = 0;
		for (int j = 1; j <= Double.parseDouble(values.get(values.size() - 1)[0]) / windowSize; ++j)
		{
			tmpArray = Lists.newArrayList();

			while (Double.parseDouble(values.get(i)[0]) / windowSize < j)
			{
				tmpArray.add(values.get(i++));
			}

			double res = PatternRecognizer.getAvgOpsPerFile(tmpArray);
			if (res >= 0)
			{
				tmpResult += res;
				++iters;
			}
		}

		if (iters < 1)
			iters = 1;
		return tmpResult / iters;
	}

	public List<String> fileToArray(File inputFile)
	{
		String command = "cat " + inputFile.getAbsolutePath();
		LOGGER.debug("Command is: %s", command);
		RemoteProcess postProcessing = connection.execCmd(command, false);
		List<String> resultList;
		try
		{
			InputStream inStream = postProcessing.getInputStream();
			InputStreamReader inStreamReader = new InputStreamReader(inStream);
			BufferedReader br = new BufferedReader(inStreamReader);

			resultList = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null)
			{
				resultList.add(line);
			}
			postProcessing.waitFor();
		} catch (IOException e)
		{
			postProcessing.finish();
			LOGGER.error("Exception while postprocessing", e);
			throw new BenchmarkException("Monitor failed", e);
		}
		return resultList;
	}

	/**
	 * Parses the output for stopMonitoring(..). Results will be saved by the
	 * BenchmarkController.
	 * 
	 * @param inputFile
	 * @param outputFile
	 *            Outputs original log file
	 * @param operation
	 *            Operation name
	 **/
	public static List<DependentVariablesValueSingle> parseBlktraceBlocksLogAndSave(List<String> inputFile, File outputFile, String operation)
	{
		String line = null;
		List<DependentVariablesValueSingle> results = Lists.newArrayList();
		try
		{
			OutputStream out = null;
			if (outputFile != null)
			{
				out = new BufferedOutputStream(new FileOutputStream(outputFile));
			}

			for (int i = 0; i < inputFile.size(); ++i)
			{
				line = inputFile.get(i);
				if (out != null)
				{
					out.write(line.getBytes(Charset.defaultCharset()));
					out.write('\n');
				}
				String[] vals = line.trim().split(" ");
				if (vals.length >= 3)
				{
					try{
						DependentVariablesValueSingle var = createBlockResult(operation, vals[0], Integer.parseInt(vals[2]) - Integer.parseInt(vals[1]));
						results.add(var);
					}catch(NumberFormatException e)
					{}
				}
			}
			if (out != null)
			{
				out.flush();
				out.close();
			}
			
			return results;
		} catch (IOException e)
		{
			throw new BenchmarkException(e);
		}
	}

	/**
	 * Parses the output for stopMonitoring(..). Results will be saved by the
	 * BenchmarkController.
	 * 
	 * @param inputFile
	 * @param outputFile
	 *            Outputs original log file
	 * @param operation
	 *            Operation name
	 **/
	public static List<DependentVariablesValueSingle> parseBlktraceQ2CLogAndSave(List<String> inputFile, File outputFile, String operation)
	{
		String line = null;
		List<DependentVariablesValueSingle> results = Lists.newArrayList();
		try
		{
			OutputStream out = null;
			if (outputFile != null)
			{
				out = new BufferedOutputStream(new FileOutputStream(outputFile));
			}

			for (int i = 0; i < inputFile.size(); ++i)
			{
				line = inputFile.get(i);
				if (out != null)
				{
					out.write(line.getBytes(Charset.defaultCharset()));
					out.write('\n');
				}
				String[] vals = line.trim().split(" ");
				if (vals.length > 1)
				{
					DependentVariablesValueSingle var = createQ2CResult(operation, vals[0], Double.parseDouble(vals[1]) * 1000);
					results.add(var);
				}
			}
			if (out != null)
			{
				out.flush();
				out.close();
			}
			return results;
		} catch (IOException e)
		{
			throw new BenchmarkException(e);
		}
	}
	
	/**
	 * Parses the output for stopMonitoring(..). Results will be saved by the
	 * BenchmarkController.
	 * 
	 * @param inputFile
	 * @param outputFile
	 *            Outputs original log file
	 * @param operation
	 *            Operation name
	 **/
	public static List<DependentVariablesValueSingle> parseBlktraceAQDLogAndSave(List<String> inputFile, File outputFile, String operation)
	{
		String line = null;
		List<DependentVariablesValueSingle> results = Lists.newArrayList();
		try
		{
			OutputStream out = null;
			if (outputFile != null)
			{
				out = new BufferedOutputStream(new FileOutputStream(outputFile));
			}

			for (int i = 0; i < inputFile.size(); ++i)
			{
				line = inputFile.get(i);
				if (out != null)
				{
					out.write(line.getBytes(Charset.defaultCharset()));
					out.write('\n');
				}
				String[] vals = line.trim().split(" ");
				if (vals.length > 1)
				{
					DependentVariablesValueSingle var = createAQDResult(operation, vals[0], Integer.parseInt(vals[1]));
					results.add(var);
				}
			}
			if (out != null)
			{
				out.flush();
				out.close();
			}
			return results;
		} catch (IOException e)
		{
			throw new BenchmarkException(e);
		}
	}

	/**
	 * Shortcut method for simpler creation of {@code DependentVariablesValue}
	 * instances.
	 * 
	 * @param operation
	 *            Operation name
	 * @param blocks
	 *            read blocks
	 * @return
	 */
	private static DependentVariablesValueSingle createBlockResult(String operation, String timestamp, int blocks)
	{
		DependentVariablesValueSingle depVarsValueSingle = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
		depVarsValueSingle.setOperation(operation);
		depVarsValueSingle.setTimestamp(timestamp);
		depVarsValueSingle.setValue(blocks);
		depVarsValueSingle.setSource("blktrace");
		depVarsValueSingle.setOperationMetric(Metric.REQUEST_SIZE);

		return depVarsValueSingle;
	}

	private static DependentVariablesValueSingle createQ2CResult(String operation, String timestamp, double latency)
	{
		DependentVariablesValueSingle depVarsValueSingle = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
		depVarsValueSingle.setOperation(operation);
		depVarsValueSingle.setTimestamp(timestamp);
		depVarsValueSingle.setValue(latency);
		depVarsValueSingle.setSource("blktrace");
		depVarsValueSingle.setOperationMetric(Metric.RESPONSE_TIME);

		return depVarsValueSingle;
	}
	
	private static DependentVariablesValueSingle createAQDResult(String operation, String timestamp, int aqd)
	{
		DependentVariablesValueSingle depVarsValueSingle = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
		depVarsValueSingle.setOperation(operation);
		depVarsValueSingle.setTimestamp(timestamp);
		depVarsValueSingle.setValue(aqd);
		depVarsValueSingle.setSource("blktrace");
		depVarsValueSingle.setOperationMetric(Metric.QUEUE_DEPTH);

		return depVarsValueSingle;
	}
}
