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

import com.google.common.collect.Lists;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.MonitorDriver;
import edu.kit.sdq.storagebenchmarkharness.RemoteConnection;
import edu.kit.sdq.storagebenchmarkharness.RemoteProcess;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueComposite;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueSingle;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfThreadsMonitor;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Metric;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Type;
import edu.kit.sdq.storagebenchmarkharness.exceptions.BenchmarkException;

/**
 * Monitors the number of running threads using bash commands. Results are
 * discrete values and averaged mean values
 * 
 * @author Axel Busch
 * 
 */
public class ThreadsMonitorDriver extends MonitorDriver<IndependentVariablesOfThreadsMonitor, DependentVariables>
{
	private static final Logger LOGGER = Logger.getLogger(ThreadsMonitorDriver.class);

	private RemoteProcess monitor;

	private InputStreamReader is;

	public ThreadsMonitorDriver(RemoteConnection con, String rawFileSaveDir)
	{
		super(con, rawFileSaveDir);
	}

	@Override
	public void startMonitoring(IndependentVariablesOfSut sutVars, IndependentVariablesOfThreadsMonitor monitorVars)
	{		
		LOGGER.debug("Executing Threads Monitoring...");
		String command = null;
		command = "while true; do echo \"$(date '+%Y-%m-%d-%H:%M:%S') $(ps aux -L | grep " + monitorVars.getBenchmarkPrefix()
				+ " | grep process | grep -v grep | wc -l)\"; sleep " + monitorVars.getInterval() + "; done";
		this.monitor = connection.execCmd(command, true);

		try
		{
			is = new InputStreamReader(monitor.getInputStream());
		} catch (IOException e)
		{
			monitor.finish();
			LOGGER.error("Exception while stopping monitor", e);
			throw new BenchmarkException("Monitor failed", e);
		}
	}

	@Override
	/**
	 *  Stops monitoring the number of running threads and returns the recorded values.
	 **/
	public DependentVariables stopMonitoring(IndependentVariablesOfThreadsMonitor monitorVars, int repeatNr, String benchmarkPrefix)
	{
		DependentVariables result = SBHModelFactory.eINSTANCE.createDependentVariables();
		result.setBenchmarkPrefix(benchmarkPrefix);
		try
		{
			// stop monitor process
			this.monitor.stopProcess();
			File logFile = getFile("threadsMonitor." + repeatNr + ".log");
			List<DependentVariablesValueSingle> values = parseMonitorResultsLogAndSave(is, logFile);
			result.getValues().addAll(values);

			// calculate avg thread num

			result.getValues().add(calculateAvgThreadNum(values));
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
		SimpleDateFormat ft = new SimpleDateFormat("yyyy-mm-dd-hh:mm:ss");
		BufferedReader reader = new BufferedReader(is);
		List<DependentVariablesValueSingle> results = Lists.newArrayList();
		try
		{
			OutputStream out = null;
			if (outputFile != null)
			{
				out = new BufferedOutputStream(new FileOutputStream(outputFile));
			}

			String lastTimestamp = null;
			while1: while ((line = reader.readLine()) != null)
			{
				if (out != null)
				{
					out.write(line.getBytes(Charset.defaultCharset()));
					out.write('\n');
				}

				String[] lineSplit = line.trim().split(" ");
				DependentVariablesValueSingle val = SBHModelFactory.eINSTANCE.createDependentVariablesValueSingle();
				val.setOperation("");
				val.setSource("threadsMonitor");
				val.setOperationMetric(Metric.THREADS);
				if (results.size() < 1)
				{
					val.setTimestamp("0");
					lastTimestamp = lineSplit[0];
				} else
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
					val.setTimestamp("" + ((c2.getTimeInMillis() - c1.getTimeInMillis()) / 1000));
				}

				if (lineSplit.length > 1)
				{
					double singleVal = Double.parseDouble(lineSplit[1]);
					if (singleVal > 0)
						--singleVal;
					val.setValue(singleVal);
					results.add(val);
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

	public static DependentVariablesValueComposite calculateAvgThreadNum(List<DependentVariablesValueSingle> values)
	{
		double resultVal = 0;
		int count = 0;
		for (DependentVariablesValueSingle elem : values)
		{
			resultVal += elem.getValue();
			if (elem.getValue() > 0)
				++count;
		}
		DependentVariablesValueComposite result = SBHModelFactory.eINSTANCE.createDependentVariablesValueComposite();
		result.setOperation("");
		result.setSource("threadsMonitor");
		result.setOperationMetric(Metric.THREADS);
		result.setType(Type.MEAN);
		result.setValue(resultVal / count);
		return result;
	}
}
