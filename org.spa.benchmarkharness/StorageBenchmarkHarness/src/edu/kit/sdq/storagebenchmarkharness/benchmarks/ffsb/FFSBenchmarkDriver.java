package edu.kit.sdq.storagebenchmarkharness.benchmarks.ffsb;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;

import edu.kit.sdq.storagebenchmarkharness.BenchmarkDriver;
import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.RemoteProcess;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValue;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueComposite;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueSingle;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfFFSB;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Metric;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Type;
import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;
import edu.kit.sdq.storagebenchmarkharness.exceptions.RemoteConnectionException;

/**
 * Provides a interface to the FFSB Benchmark. The original FFSB-Binary does not
 * work with this driver. See the modified source at
 * https://github.com/theomega/ffsb.
 * 
 * As sidenotes, the units of measurement for the IndependentVariablesOfFFSB are
 * as follows:
 * <ul>
 * <li>readBlockSize: bytes
 * <li>writeBlockSize: bytes
 * <li>fileSetSize: MB
 * <li>fileSize: kb
 * </ul>
 * 
 * @author Dominik Bruhn 
 * @author Axel Busch
 * 
 */
public final class FFSBenchmarkDriver extends BenchmarkDriver<IndependentVariablesOfFFSB, DependentVariables>
{
	private static final Logger LOGGER = Logger.getLogger(FFSBenchmarkDriver.class);

	private final String template;
	private final String targetDir;// On Remote Machine!
	
	private final boolean saveSingleResults;

	public String getTargetDir()
	{
		return targetDir;
	}

	private String confFileWarmup;
	private String confFileBenchmark;

	public FFSBenchmarkDriver(RemoteConnection con, String rawFileSaveDir, String targetDir, boolean saveSingleResults)
	{
		super(con, rawFileSaveDir);
		template = getResource("ffsb_template");
		
		// Generate random UUID to save data in
		String tmpFolder = UUID.randomUUID().toString();
		if(targetDir.endsWith("/")) {
			this.targetDir = targetDir + tmpFolder;
		} else {
			this.targetDir = targetDir + "/" + tmpFolder;
		}
		LOGGER.debug("TargetDir is %s", targetDir);
		
		this.saveSingleResults = saveSingleResults;
		if (this.saveSingleResults) {
			LOGGER.debug("FFSB Single Results will be saved.");
		} else {
			LOGGER.debug("FFSB Single Results will NOT be saved.");
		}
	}
	
	public FFSBenchmarkDriver(RemoteConnection con, String rawFileSaveDir, String targetDir)
	{
		this(con,rawFileSaveDir, targetDir, getEnvDefault("ffsbsingleresults", "TRUE").equalsIgnoreCase("TRUE"));
	}

	public FFSBenchmarkDriver(RemoteConnection con, String rawFileSaveDir)
	{
		this(con, rawFileSaveDir, getEnvDefault("ffsbtargetdir", "/tmp/ffsbtarget/"));
	}

	/*
	 * Generates the two configfiles for warmup and benchmarking. These
	 * config-files are saved on the SUT and a copies is saved in the {@code
	 * filesavedir} if it is not {@code null}. In this prepare stage, the
	 * fileset is created and a initial warmup benchmark is executed.
	 */
	@Override
	public void prepareExperiment(IndependentVariablesOfSut sutVars, IndependentVariablesOfFFSB benchVars)
	{
		// Create tmpFolder for benchmarking
		String command = "mkdir " + targetDir; 
		RemoteProcess setup = connection.execCmd(command, false);
		setup.waitFor();
		setup.finish();
		
		// Generated and save the configurations
		prepareFFSBConfigurations(sutVars, benchVars);

		// Set Scheduler
		setScheduler(sutVars.getScheduler(), targetDir);

		// Check the Filesystem
		checkFileSystem(sutVars.getFileSystem(), targetDir);

		// Warmup-Execution
		LOGGER.debug("FFSB Warmup");
		RemoteProcess ffsbWarmup = connection.execCmd("ffsb" + " " + confFileWarmup, false);
		String stdOut;
		try
		{
			stdOut = CharStreams.toString(new InputStreamReader(ffsbWarmup.getInputStream()));
			String stdErr = CharStreams.toString(new InputStreamReader(ffsbWarmup.getErrorStream()));

			ffsbWarmup.waitFor();

			if (ffsbWarmup.getExitStatus() != 0)
			{
				LOGGER.error("Warmup Failed: %s / %s", stdOut, stdErr);
				ffsbWarmup.finish();
				throw new BenchmarkException("FFSB Warmup did not exit with status 0");
			}
		} catch (IOException e)
		{
			ffsbWarmup.finish();
			LOGGER.error("IO Exception on Warmup", e);
			throw new BenchmarkException("Benchmark warmup failed", e);
		}

		ffsbWarmup.finish();

		// Save Warmup log locally
		saveFile("warmup.log", stdOut);
	}

	/**
	 * Generates the FFSB Configurations for the warmup and the actual
	 * benchmarking. It saves the configs remotely and also locally if the
	 * {@code filesavedir} is set.
	 * 
	 * @param expUid
	 * @param sutVars
	 * @param benchVars
	 */
	protected void prepareFFSBConfigurations(IndependentVariablesOfSut sutVars, IndependentVariablesOfFFSB benchVars)
	{
		String confId = UUID.randomUUID().toString();
		confFileWarmup = "/tmp/" + confId + ".warmup.ffsb";
		confFileBenchmark = "/tmp/" + confId + ".bench.ffsb";
		LOGGER.debug("FFSB Configfile is %s and %s", confFileWarmup, confFileBenchmark);

		String conf = template;

		conf = conf.replace("{{READ_WEIGHT}}", benchVars.getReadPercentage().toString());
		if (benchVars.getWriteFsync() == true)
		{
			conf = conf.replace("{{WRITE_FSYNC_WEIGHT}}", "" + (100 - benchVars.getReadPercentage()));
			conf = conf.replace("{{WRITE_WEIGHT}}", "0");
		} else
		{
			conf = conf.replace("{{WRITE_WEIGHT}}", "" + (100 - benchVars.getReadPercentage()));
			conf = conf.replace("{{WRITE_FSYNC_WEIGHT}}", "0");
		}

		// Block-Sizes & read/write Sizes
		if (benchVars.getReadBlockSize() != null && benchVars.getWriteBlockSize() != null && benchVars.getBlockSize() == null)
		{
			conf = conf.replace("{{WRITE_BLOCKSIZE}}", benchVars.getWriteBlockSize().toString());
			conf = conf.replace("{{READ_BLOCKSIZE}}", benchVars.getReadBlockSize().toString());

			int readSize, writeSize;
			if (benchVars.getOpsPerFile() == null && benchVars.getOpsPerFileRead() != null && benchVars.getOpsPerFileWrite() != null)
			{
				readSize = benchVars.getReadBlockSize() * benchVars.getOpsPerFileRead();
				writeSize = benchVars.getWriteBlockSize() * benchVars.getOpsPerFileWrite();
			}
			else
			{
				readSize = benchVars.getReadBlockSize() * benchVars.getOpsPerFile();
				writeSize = benchVars.getWriteBlockSize() * benchVars.getOpsPerFile();
			}

			conf = conf.replace("{{READ_SIZE}}", "" + readSize);
			conf = conf.replace("{{WRITE_SIZE}}", "" + writeSize);
		} else if (benchVars.getBlockSize() != null && benchVars.getReadBlockSize() == null && benchVars.getWriteBlockSize() == null
				&& benchVars.getOpsPerFile() != null)
		{
			int blockSize = benchVars.getBlockSize();
			// Use the closest multiple of 512 less than or equal to blockSize
			// no Math.round needed as long as the divisor and dividend are both int
			blockSize = ((blockSize / 512) * 512);

			if (blockSize != benchVars.getBlockSize())
			{
				LOGGER.error("BlockSize %d is unsupported, rounding to %d", benchVars.getBlockSize(), blockSize);
			}

			conf = conf.replace("{{WRITE_BLOCKSIZE}}", "" + blockSize);
			conf = conf.replace("{{READ_BLOCKSIZE}}", "" + blockSize);

			int opSize = blockSize * benchVars.getOpsPerFile();

			conf = conf.replace("{{WRITE_SIZE}}", "" + opSize);
			conf = conf.replace("{{READ_SIZE}}", "" + opSize);
		} else
		{
			throw new IllegalArgumentException("Could not find valid blocksize in variables");
		}

		// Sequential Access
		if (benchVars.getSequentialWrite() != null && benchVars.getSequentialRead() != null && benchVars.getSequentialAccess() == null)
		{
			conf = conf.replace("{{WRITE_RANDOM}}", benchVars.getSequentialWrite() ? "0" : "1");
			conf = conf.replace("{{READ_RANDOM}}", benchVars.getSequentialRead() ? "0" : "1");
		} else if (benchVars.getSequentialAccess() != null && benchVars.getSequentialWrite() == null && benchVars.getSequentialRead() == null)
		{
			conf = conf.replace("{{WRITE_RANDOM}}", benchVars.getSequentialAccess() ? "0" : "1");
			conf = conf.replace("{{READ_RANDOM}}", benchVars.getSequentialAccess() ? "0" : "1");
		} else
		{
			throw new IllegalArgumentException("No valid values for sequential access found");
		}

		conf = conf.replace("{{DIRECTIO}}", benchVars.getDirectIO() ? "1" : "0");

		// Fileset:
		int numfiles = (int) Math.ceil(((double) benchVars.getFilesetSize() * 1024 / (double) benchVars.getFileSize()));
		conf = conf.replace("{{MINFILESIZE}}", benchVars.getFileSize() + "k");
		conf = conf.replace("{{MAXFILESIZE}}", benchVars.getFileSize() + "k");
		conf = conf.replace("{{NUMFILES}}", "" + numfiles);

		// Threads
		conf = conf.replace("{{NUM_THREADS}}", benchVars.getThreadCount().toString());

		// Target
		conf = conf.replace("{{TARGET}}", targetDir);
		
		// Delay
		
		conf = conf.replace("{{OP_DELAY}}", benchVars.getOpDelay().toString());

		// First Config: Warmup
		String confWarmup = conf;
		confWarmup = confWarmup.replace("{{REUSE}}", "0");
		confWarmup = confWarmup.replace("{{TIME}}", benchVars.getWarmUpTime().toString());
		confWarmup = confWarmup.replace("{{ENABLESTATS}}", "0");
		connection.saveStringToFile(confWarmup, confFileWarmup, false);

		// Second Config: Benchmarking
		String confBench = conf;
		confBench = confBench.replace("{{REUSE}}", "1");
		confBench = confBench.replace("{{TIME}}", benchVars.getRunTime().toString());
		confBench = confBench.replace("{{ENABLESTATS}}", "1");
		connection.saveStringToFile(confBench, confFileBenchmark, false);

		// Save a copy locally on the Measurement Machine for later debugging.
		saveFile("warmup.ffsb", confWarmup);
		saveFile("bench.ffsb", confBench);
	}

	/*
	 * Deletes the temporary files and the two configfiles from the SUT.
	 */
	@Override
	public void endExperiment() throws RemoteConnectionException
	{
		connection.deleteFile(confFileBenchmark);
		connection.deleteFile(confFileWarmup);
		
		// Delete file set in target directory including tmpFolder
		
		String command = "rm -r " + targetDir; 
		RemoteProcess cleanup = connection.execCmd(command, false);
		cleanup.waitFor();
		cleanup.finish();
	}

	/*
	 * Executes FFSB or actual benchmarking. The results of the run is saved for
	 * debugging purposes. The results the benchmark run is parsed and returned.
	 */
	@Override
	public DependentVariables startExperiment(int repeatNo)
	{
		LOGGER.debug("Executing FFSB for #%d", repeatNo);

		// Benchmark-Execution
		RemoteProcess ffsbBench = connection.execCmd("ffsb" + " " + confFileBenchmark, false);

		File logFile = getFile("bench." + repeatNo + ".log");

		try
		{
			List<DependentVariablesValue> values = parseFFSBLogAndSave(new BufferedReader(new InputStreamReader(ffsbBench.getInputStream())),
					true, logFile, this.saveSingleResults);
			String stdErr = CharStreams.toString(new InputStreamReader(ffsbBench.getErrorStream()));
			ffsbBench.waitFor();

			if (ffsbBench.getExitStatus() != 0 || values == null)
			{
				ffsbBench.finish();
				LOGGER.error("Benchmarking failed: %s", stdErr);
				throw new BenchmarkException("FFSB Benchmarking failed");
			}

			ffsbBench.finish();
			DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();
			result.setBenchmarkPrefix("ffsb");
			result.getValues().clear();
			result.getValues().addAll(values);
			return result;
		} catch (IOException e)
		{
			ffsbBench.finish();
			LOGGER.error("IO Exception on Benchmark parsing", e);
			throw new BenchmarkException("Benchmark parsing failed", e);
		}
	}

	/**
	 * Parses the FFSB Output into DependentVariables which will be used and
	 * saved by the benchmark controller.
	 * 
	 * @param reader
	 *            A BufferedReader which provides the output of a ffsb-run
	 * @param onlyReadWrite
	 *            Should only the read and write operations of the ffsb-run be
	 *            transformed into DependentVariables? This heavily reduces the
	 *            memory footprint of this application.
	 * @param outputFile
	 *            The file where a copy of the raw output of ffsb should be
	 *            saved to. Can be null if no saving is required.
	 * @return A list of DependentVariables containing the responseTime for each
	 *         operation FFSB executed. If the logfile is invalid, null is
	 *         returned.
	 */
	public static List<DependentVariablesValue> parseFFSBLogAndSave(BufferedReader reader, boolean onlyReadWrite, File outputFile, boolean saveSingleResults)
	{
		boolean foundHeading = false;
		boolean foundLastLine = false;// For security purposes check if the last
										// line occured
		String currentOp = null;

		String line;

		List<DependentVariablesValue> results = Lists.newArrayList();

		try
		{
			OutputStream out = null;
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

				if (line.length() == 0)
				{
					continue;
				}
				
				if (line.contains("Read Throughput") || line.contains("Write Throughput"))
				{
					line = line.replaceAll("\\s+", " ").trim();
					String[] lineElems = line.split(" ");
					
					DependentVariablesValueComposite throughput = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
					throughput.setOperation(lineElems[0].toLowerCase());
					throughput.setSource("ffsb");
					throughput.setOperationMetric(Metric.THROUGHPUT);
					throughput.setType(Type.MEAN);
					throughput.setValue(Double.parseDouble(lineElems[2].replaceAll("MB/sec|KB/sec","")));
					
					results.add(throughput);
				}

				if (line.equals("Discrete overall System Call Latency statistics in millisecs"))
				{
					foundHeading = true;
					LOGGER.debug("Found Main Heading");
					continue;
				}

				if (!foundHeading)
				{
					if (line.length() > 40 && line.contains("[") && line.contains("]"))
					{
						// Store number of operations and mean response times
						line = line.replaceAll("\\s+", " ").trim();
						String[] lineElems = line.split(" ");

						if (onlyReadWrite && !lineElems[1].contains("read") && !lineElems[1].contains("write"))
							continue;
						else
						{
							
							DependentVariablesValueComposite avgResponseTime = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
							avgResponseTime.setOperation(lineElems[1].replace("]",""));
							avgResponseTime.setSource("ffsb");
							avgResponseTime.setOperationMetric(Metric.RESPONSE_TIME);
							avgResponseTime.setType(Type.MEAN);
							avgResponseTime.setValue(Double.parseDouble(lineElems[3]));
							
							results.add(avgResponseTime);
							
							DependentVariablesValueComposite numberOps = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
							numberOps.setOperation(lineElems[1].replace("]",""));
							numberOps.setSource("ffsb");
							numberOps.setOperationMetric(Metric.OPERATIONS);
							numberOps.setType(Type.ABSOLUTE);
							numberOps.setValue(Double.parseDouble(lineElems[5]));
							
							results.add(numberOps);
						}
					}
					else
						continue;
				}

				// Throw away last three lines
				if (line.endsWith("User   Time") || line.endsWith("System Time"))
				{
					continue;
				}
				if (line.endsWith("CPU Utilization"))
				{
					foundLastLine = true;
					continue;
				}

				// Check for heading
				if (line.length() > 30 && line.contains("Total calls: ") && line.contains("[") && line.contains("]"))
				{
					currentOp = line.substring(1, 8).trim();
					LOGGER.debug("Found Heading %s", currentOp);
					
					continue;
				}

				// Check for results
				if (currentOp != null && !line.equals("===="))
				{
					double time = Double.parseDouble(line);
					if (time > 10000)
					{
						LOGGER.error("Converted line %s to strange time %f", line, time);
					}
					if (onlyReadWrite && !currentOp.equals("read") && !currentOp.equals("write"))
					{
						continue;
					}
					if (saveSingleResults) {
						results.add(createResult(currentOp, time));
					}
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

		if (!foundLastLine)
		{
			LOGGER.error("Log did not contain the last line, perhaps invalid?");
			return null;
		}

		return results;
	}

	/**
	 * Shortcut method for simpler creation of {@code DependentVariablesValue}
	 * instances.
	 * 
	 * @param operation
	 * @param responseTime
	 * @return
	 */
	private static DependentVariablesValueSingle createResult(String operation, double responseTime)
	{
		DependentVariablesValueSingle r = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
		r.setOperation(operation);
		r.setValue(responseTime);
		r.setTimestamp("" + System.currentTimeMillis());
		r.setOperationMetric(Metric.RESPONSE_TIME);
		r.setSource("ffsb");
		return r;
	}
}
