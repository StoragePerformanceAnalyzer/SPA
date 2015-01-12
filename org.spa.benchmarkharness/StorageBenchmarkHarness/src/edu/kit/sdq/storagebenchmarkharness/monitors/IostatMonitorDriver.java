package edu.kit.sdq.storagebenchmarkharness.monitors;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import com.google.common.collect.Lists;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.MonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.RemoteProcess;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValue;
//import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueComposite;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueSingle;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfIostatMonitor;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Metric;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
//import edu.kit.sdq.storagebenchmarkharness.SBHModel.Type;
import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;

/**
 * Provides an interface to the iostat monitoring tool. 
 * 
 * @author Kiana Rostami
 * 
 */
public class IostatMonitorDriver extends MonitorDriver<IndependentVariablesOfIostatMonitor, DependentVariables>
{
	
	private static final Logger LOGGER = Logger.getLogger(IostatMonitorDriver.class);
	private final String targetDir;
	private RemoteProcess monitor;
	private InputStreamReader is;
	private File logTemp;
	
	
	public IostatMonitorDriver(RemoteConnection con, String logFileSaveDir, String targetDir)
	{
		super(con, logFileSaveDir);
		this.targetDir=targetDir;
		LOGGER.debug("TargetDir is %s", targetDir);
		logTemp = new File("iostat.log");
	}
	
	public IostatMonitorDriver(RemoteConnection con, String logFileSaveDir)
	{
		this(con, logFileSaveDir,getEnvDefault("iostattargetdir", "/tmp/iostattarget/"));	
	}


	@Override
	public void startMonitoring(IndependentVariablesOfSut sutVars, IndependentVariablesOfIostatMonitor monitorVars)
	{
		LOGGER.debug("Executing iostat Monitoring...");
		File destination = new File(targetDir);
		String command = null;
		String device = monitorVars.getTargetDevice();

		command = "iostat -xtkcd " + monitorVars.getInterval() + " " + device +" > "+ destination.getAbsoluteFile() + "/"+ logTemp;

		this.monitor = connection.execCmd(command, true);

	}

	@Override
	/**
	 *  Stops monitoring iostat and returns the recorded values.
	 **/
	public DependentVariables stopMonitoring(IndependentVariablesOfIostatMonitor monitorVars, int repeatNr, String benchmarkPrefix)
	{	
		DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();
		result.setBenchmarkPrefix(benchmarkPrefix);
				
		try
		{
			// stop monitor process

			this.monitor.stopProcess();
			String command = null;
			LOGGER.debug("Reading monitoring results...");
			File destination = new File(targetDir);
			
			command = "cat "+ destination.getAbsoluteFile() + "/"+ logTemp;
			LOGGER.debug("Command is: %s", command);
			RemoteProcess postProcessing = connection.execCmd(command, false);
			
			try
			{
				is = new InputStreamReader(postProcessing.getInputStream());
				File logFile = getFile("iostatMonitor." + repeatNr + ".log");
				
				List<DependentVariablesValueSingle> values = parseMonitorResultsLogAndSave(is, logFile);
				result.getValues().addAll(values);
				
				postProcessing.waitFor();
				postProcessing.finish();
				
				command = "rm -R " + destination.getAbsoluteFile() + "/"+ logTemp;
				LOGGER.debug("Command is: %s", command);
				postProcessing = connection.execCmd(command, false);
				postProcessing.waitFor();
				postProcessing.finish();
				
			} catch (IOException e)
			{
				postProcessing.finish();
				LOGGER.error("Exception while postprocessing", e);
				throw new BenchmarkException("Monitor failed", e);
			}

		} catch (Exception e)
		{
			this.monitor.finish();
			LOGGER.error("Exception while stopping monitor", e);
			throw new BenchmarkException("Monitor failed", e);
		}
		


		this.monitor.finish();

		return result;
	}

	public static List<DependentVariablesValueSingle> parseMonitorResultsLogAndSave(InputStreamReader is, File outputFile)
	{
		

		String line = null;
		SimpleDateFormat ft = new SimpleDateFormat("mm/dd/yyyy hh:mm:ss");

		BufferedReader reader = new BufferedReader(is);
		
		
		List<DependentVariablesValueSingle> results = Lists.newArrayList();
		
		// set timestamp
		
		String timestamp = null;
		try
		{
			OutputStream out = null;
			if (outputFile != null)
			{
				out = new BufferedOutputStream(new FileOutputStream(outputFile));
			}

			int lineCount = 0;
			
			String lastTimestamp = null;

			
			while1: while ((line = reader.readLine()) != null)
			{
				
				if (out != null)
				{
					out.write(line.getBytes(Charset.defaultCharset()));
					out.write('\n');
				}

				String[] lineSplit = line.replaceAll("\\s+", " ").trim().split(" ");
				
				if(lineSplit.length == 2)
				{
					lineCount++;
					if (lineCount < 7)
						continue while1;
					
					String tmp = "";
					tmp = lineSplit[0]+" "+lineSplit[1];
					lineSplit[0] = tmp;
					lineSplit[1] = null;
					
					if (results.size() < 1)
					{
						timestamp = "0";
						lastTimestamp = lineSplit[0];

						
					} else if (results.size() > 0)
					{
						String time1 = lastTimestamp;
						String time2 = lineSplit[0];
						Calendar c1 = Calendar.getInstance();
						Calendar c2 = Calendar.getInstance();
						try
						{
							c1.setTime(ft.parse(time1));
							c2.setTime(ft.parse(time2));
						} catch (ParseException e)
						{
							continue while1;
						}
						timestamp=("" + ((c2.getTimeInMillis() - c1.getTimeInMillis()) / 1000));
					}
					
				}
				else if (lineSplit.length > 11 && lineSplit[0].equals("Device:"))
				{
					lineCount++;
					continue while1;
				}
				else if (lineSplit.length < 8 && lineSplit[0].equals("avg-cpu:"))
				{
					lineCount++;
					continue while1;
				}
				else if (lineSplit.length > 11)
				{
					lineCount++;
					if (lineCount < 10)
					{
						continue while1;
					}
					
					//  Compute the number of read requests merged per second
					
					DependentVariablesValueSingle NumOfMergedReadReqs = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double NumOfMergedReadPerSec = 0;
					NumOfMergedReadPerSec = Double.parseDouble(lineSplit[1]);
					NumOfMergedReadReqs.setOperation("read");
					NumOfMergedReadReqs.setTimestamp(timestamp);
					NumOfMergedReadReqs.setValue(NumOfMergedReadPerSec);
					NumOfMergedReadReqs.setSource("iostat");
					NumOfMergedReadReqs.setOperationMetric(Metric.MERGES_PER_SEC);					
					results.add(NumOfMergedReadReqs);
					
					//  Compute number of write requests merged per second
					 
					DependentVariablesValueSingle NumOfMergedWriteReqs = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double NumOfMergedWritePerSec = 0;
					NumOfMergedWritePerSec = Double.parseDouble(lineSplit[2]);
					NumOfMergedWriteReqs.setOperation("write");
					NumOfMergedWriteReqs.setTimestamp(timestamp);
					NumOfMergedWriteReqs.setValue(NumOfMergedWritePerSec);
					NumOfMergedWriteReqs.setSource("iostat");
					NumOfMergedWriteReqs.setOperationMetric(Metric.MERGES_PER_SEC);					
					results.add(NumOfMergedWriteReqs);
					
					//  Compute the proportion of read requests per second
					 
					DependentVariablesValueSingle NumOfReadReqs2DevProportion = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double NumOfReadReqs2DevPerSec = 0;
					double NumOfWriteReqs2DevPerSec = 0;			
					NumOfReadReqs2DevPerSec = Double.parseDouble(lineSplit[3]);
					NumOfWriteReqs2DevPerSec = Double.parseDouble(lineSplit[4]);					
					NumOfReadReqs2DevProportion.setTimestamp(timestamp);
					NumOfReadReqs2DevProportion.setOperation("read");
					if(NumOfReadReqs2DevPerSec > 0 || NumOfWriteReqs2DevPerSec > 0)
					{
						NumOfReadReqs2DevProportion.setValue(NumOfReadReqs2DevPerSec/(NumOfReadReqs2DevPerSec+NumOfWriteReqs2DevPerSec));
					}
					else
					{
						NumOfReadReqs2DevProportion.setValue(0.0);
					}
					NumOfReadReqs2DevProportion.setSource("iostat");
					NumOfReadReqs2DevProportion.setOperationMetric(Metric.REQUEST_MIX);					
					results.add(NumOfReadReqs2DevProportion);
					
					
					//  Compute the proportion of write requests per second
					 
					DependentVariablesValueSingle NumOfWriteReqs2DevProportion = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();					
					NumOfWriteReqs2DevProportion.setOperation("write");
					NumOfWriteReqs2DevProportion.setTimestamp(timestamp);
					if(NumOfReadReqs2DevPerSec > 0 || NumOfWriteReqs2DevPerSec > 0)
					{
						NumOfWriteReqs2DevProportion.setValue(NumOfWriteReqs2DevPerSec/(NumOfReadReqs2DevPerSec+NumOfWriteReqs2DevPerSec));
					}
					else
					{
						NumOfWriteReqs2DevProportion.setValue(0.0);
					}
					NumOfWriteReqs2DevProportion.setSource("iostat");
					NumOfWriteReqs2DevProportion.setOperationMetric(Metric.REQUEST_MIX);					
					results.add(NumOfWriteReqs2DevProportion);
					
					
					//  Compute the number of read requests per second
					 
					DependentVariablesValueSingle NumOfReadReqs2Dev = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					
					NumOfReadReqs2Dev.setOperation("read");
					NumOfReadReqs2Dev.setTimestamp(timestamp);
					NumOfReadReqs2Dev.setValue(NumOfReadReqs2DevPerSec);
					NumOfReadReqs2Dev.setSource("iostat");
					NumOfReadReqs2Dev.setOperationMetric(Metric.OPERATIONS);
					
					results.add(NumOfReadReqs2Dev);
					
					
					//  Compute the number of write requests per second
					 
					DependentVariablesValueSingle NumOfWriteReqs2Dev = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					
					NumOfWriteReqs2Dev.setOperation("write");
					NumOfWriteReqs2Dev.setTimestamp(timestamp);
					NumOfWriteReqs2Dev.setValue(NumOfWriteReqs2DevPerSec);
					NumOfWriteReqs2Dev.setSource("iostat");
					NumOfWriteReqs2Dev.setOperationMetric(Metric.OPERATIONS);
					
					results.add(NumOfWriteReqs2Dev);
					
					
					//  Compute the average read request size in kilobyteper per second over an interval of time
					
					DependentVariablesValueSingle AvgSizeOfReadReqsInKB2Dev = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double NumOfReadReqsInKBPerSecond = 0;				
					NumOfReadReqsInKBPerSecond = Double.parseDouble(lineSplit[5]);
					AvgSizeOfReadReqsInKB2Dev.setOperation("read");
					AvgSizeOfReadReqsInKB2Dev.setTimestamp(timestamp);
					if(NumOfReadReqs2DevPerSec > 0)
					{
						AvgSizeOfReadReqsInKB2Dev.setValue(NumOfReadReqsInKBPerSecond/NumOfReadReqs2DevPerSec);
					}
					else
					{
						AvgSizeOfReadReqsInKB2Dev.setValue(0.0);
					}
					AvgSizeOfReadReqsInKB2Dev.setSource("iostat");
					AvgSizeOfReadReqsInKB2Dev.setOperationMetric(Metric.REQUEST_SIZE);
					
					if (NumOfReadReqs2DevPerSec != 0.0)
					{
						results.add(AvgSizeOfReadReqsInKB2Dev);
					}
					
					// Compute the average write request size in kilobyteper per second over an interval of time
					 
					DependentVariablesValueSingle AvgSizeOfWriteReqsInKB2Dev = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double NumOfWriteReqsInKBPerSecond = 0;
					NumOfWriteReqsInKBPerSecond  = Double.parseDouble(lineSplit[6]);
					AvgSizeOfWriteReqsInKB2Dev.setOperation("write");
					AvgSizeOfWriteReqsInKB2Dev.setTimestamp(timestamp);
					if(NumOfWriteReqs2DevPerSec > 0)
					{
						AvgSizeOfWriteReqsInKB2Dev.setValue(NumOfWriteReqsInKBPerSecond/NumOfWriteReqs2DevPerSec);
					}
					else
					{
						AvgSizeOfWriteReqsInKB2Dev.setValue(0.0);
					}
					AvgSizeOfWriteReqsInKB2Dev.setSource("iostat");
					AvgSizeOfWriteReqsInKB2Dev.setOperationMetric(Metric.REQUEST_SIZE);
					
					if (NumOfWriteReqs2DevPerSec != 0.0)
					{
						results.add(AvgSizeOfWriteReqsInKB2Dev);
					}
					
					//  Compute the average request size in kilobyte per second over an interval of time
					 
					DependentVariablesValueSingle AvgSizeOfReqsInKB2Dev = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double AvgReqSize = 0;
					AvgReqSize = Double.parseDouble(lineSplit[7]);
					AvgSizeOfReqsInKB2Dev.setOperation("read+write");
					AvgSizeOfReqsInKB2Dev.setTimestamp(timestamp);
					if(NumOfReadReqs2DevPerSec > 0 || NumOfWriteReqs2DevPerSec > 0)
					{
						AvgSizeOfReqsInKB2Dev.setValue(AvgReqSize/(NumOfReadReqs2DevPerSec+NumOfWriteReqs2DevPerSec));
					}
					else
					{
						AvgSizeOfReqsInKB2Dev.setValue(0.0);
					}
					AvgSizeOfReqsInKB2Dev.setSource("iostat");
					AvgSizeOfReqsInKB2Dev.setOperationMetric(Metric.REQUEST_SIZE);					
					results.add(AvgSizeOfReqsInKB2Dev);
										
					//  Compute the average queue length of the requests
					 
					DependentVariablesValueSingle AvgQLengthOfReqs = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double AvgQueueLength = 0;
					AvgQueueLength  = Double.parseDouble(lineSplit[8]);
					AvgQLengthOfReqs.setOperation("read+write");
					AvgQLengthOfReqs.setTimestamp(timestamp);
					AvgQLengthOfReqs.setValue(AvgQueueLength);
					AvgQLengthOfReqs.setSource("iostat");
					AvgQLengthOfReqs.setOperationMetric(Metric.QUEUE_DEPTH);					
					results.add(AvgQLengthOfReqs);
					
					
					
					//  Compute the  average  response  time  (in  milliseconds)
					 
					DependentVariablesValueSingle AvgResponseTime = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double AvgRespTime = 0;
					AvgRespTime  = Double.parseDouble(lineSplit[9]);					
					AvgResponseTime.setOperation("read+write");
					AvgResponseTime.setTimestamp(timestamp);
					AvgResponseTime.setValue(AvgRespTime);
					AvgResponseTime.setSource("iostat");
					AvgResponseTime.setOperationMetric(Metric.RESPONSE_TIME);
					results.add(AvgResponseTime);
					
					
					
					//  Compute the  average  service  time  (in  milliseconds) 
					 
					DependentVariablesValueSingle AvgServiceTime = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double AvgServTime = 0;	
					AvgServTime = Double.parseDouble(lineSplit[10]);					
					AvgServiceTime.setOperation("read+write");
					AvgServiceTime.setTimestamp(timestamp);
					AvgServiceTime.setValue(AvgServTime);
					AvgServiceTime.setSource("iostat");
					AvgServiceTime.setOperationMetric(Metric.SERVICE_TIME);
					
					results.add(AvgServiceTime);
					
				}
				else if (lineSplit.length>4 && lineSplit.length < 8)
				{
					lineCount++;
					if (lineCount < 10)
					{
						continue while1;
					}
					
					DependentVariablesValueSingle cpuUtilizationUserSpace = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double cpuUtilizationUser = 0;	
					cpuUtilizationUser = Double.parseDouble(lineSplit[0]);					
					cpuUtilizationUserSpace.setOperation("read+write");
					cpuUtilizationUserSpace.setTimestamp(timestamp);
					cpuUtilizationUserSpace.setValue(cpuUtilizationUser);
					cpuUtilizationUserSpace.setSource("iostat");
					cpuUtilizationUserSpace.setOperationMetric(Metric.CPU_UTIL_USER);
					results.add(cpuUtilizationUserSpace);
					
					DependentVariablesValueSingle cpuUtilizationNiceUser = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double cpuUtilizationNice = 0;	
					cpuUtilizationNice = Double.parseDouble(lineSplit[1]);					
					cpuUtilizationNiceUser.setOperation("read+write");
					cpuUtilizationNiceUser.setTimestamp(timestamp);
					cpuUtilizationNiceUser.setValue(cpuUtilizationNice);
					cpuUtilizationNiceUser.setSource("iostat");
					cpuUtilizationNiceUser.setOperationMetric(Metric.CPU_UTIL_NICE);
					results.add(cpuUtilizationNiceUser);
					
					DependentVariablesValueSingle cpuUtilizationSystemSpace = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double cpuUtilizationSystem = 0;	
					cpuUtilizationSystem = Double.parseDouble(lineSplit[2]);					
					cpuUtilizationSystemSpace.setOperation("read+write");
					cpuUtilizationSystemSpace.setTimestamp(timestamp);
					cpuUtilizationSystemSpace.setValue(cpuUtilizationSystem);
					cpuUtilizationSystemSpace.setSource("iostat");
					cpuUtilizationSystemSpace.setOperationMetric(Metric.CPU_UTIL_SYSTEM);
					results.add(cpuUtilizationSystemSpace);
					
					DependentVariablesValueSingle cpuIowaitTime = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double cpuIowait = 0;	
					cpuIowait = Double.parseDouble(lineSplit[3]);					
					cpuIowaitTime.setOperation("read+write");
					cpuIowaitTime.setTimestamp(timestamp);
					cpuIowaitTime.setValue(cpuIowait);
					cpuIowaitTime.setSource("iostat");
					cpuIowaitTime.setOperationMetric(Metric.CPU_IOWAIT);
					results.add(cpuIowaitTime);
					
					DependentVariablesValueSingle cpuStealTime = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double cpuSteal = 0;	
					cpuSteal = Double.parseDouble(lineSplit[4]);					
					cpuStealTime.setOperation("read+write");
					cpuStealTime.setTimestamp(timestamp);
					cpuStealTime.setValue(cpuSteal);
					cpuStealTime.setSource("iostat");
					cpuStealTime.setOperationMetric(Metric.CPU_STEAL);
					results.add(cpuStealTime);
					
					DependentVariablesValueSingle cpuIdleTime = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
					double cpuIdle = 0;	
					cpuIdle = Double.parseDouble(lineSplit[5]);					
					cpuIdleTime.setOperation("read+write");
					cpuIdleTime.setTimestamp(timestamp);
					cpuIdleTime.setValue(cpuIdle);
					cpuIdleTime.setSource("iostat");
					cpuIdleTime.setOperationMetric(Metric.CPU_IDLE);
					results.add(cpuIdleTime);
					
				}
				else
				{
					lineCount++;
					continue while1;
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
	
}
