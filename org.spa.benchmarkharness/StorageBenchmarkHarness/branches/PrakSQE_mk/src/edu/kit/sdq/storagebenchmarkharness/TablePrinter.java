package edu.kit.sdq.storagebenchmarkharness;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.beust.jcommander.internal.Lists;
import com.beust.jcommander.internal.Maps;

import edu.kit.sdq.storagebenchmarkharness.ExperimentSeriesHelper.DriverAndIndependentVars;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelPackage;

/**
 * A Helper class which contains a single method for generating a html page
 * which contains a summary for the experiments which will be issued. This class
 * is only used byt the {@code BenchmarkController} if it is called using the
 * {@code verify} switch.
 * 
 * @author dominik
 * 
 */
public final class TablePrinter
{
	private static final Logger LOGGER = Logger.getLogger(TablePrinter.class);

	/**
	 * Generates a HTML document with all the experiments and saves it to a file
	 * on the hardisk.
	 * 
	 * @param experimentsForSut
	 *            The description of the experiments as they are generated in
	 *            the {@code BenchmarkController}
	 * @param filename
	 *            The filename where the table should be saved to
	 * @throws IOException
	 */
	public static void printAsTable(Map<String, List<DriverAndIndependentVars>> experimentsForSut, String filename) throws IOException
	{
		Map<String, List<EClass>> hostColumns = Maps.newHashMap();

		for (Entry<String, List<DriverAndIndependentVars>> e : experimentsForSut.entrySet())
		{
			List<EClass> columns = Lists.newArrayList();

			for (DriverAndIndependentVars b : e.getValue())
			{
				if (!columns.contains(b.getBenchVars().eClass()))
				{
					columns.add(b.getBenchVars().eClass());
				}
			}

			hostColumns.put(e.getKey(), columns);
		}

		int expCount = experimentsForSut.values().iterator().next().size();
		EClass sutClass = SBHModelPackage.eINSTANCE.getIndependentVariablesOfSut();
		int sutFieldCount = sutClass.getFeatureCount();
		List<String> hostNames = new ArrayList<String>(experimentsForSut.keySet());

		BufferedWriter fw = new BufferedWriter(new FileWriter(new File(filename)));
		fw.write("<html>");
		fw.write("<head>");
		fw.write("<style TYPE=\"text/css\">");
		fw.write("table { border: 1px black solid; border-collapse: collapse;}");
		fw.write("td {border: 1px black solid;}");
		fw.write("th {border: 1px black solid; background-color: #dddddd;}");
		fw.write("tr:nth-child(even) {background-color: #fefefe;}");
		fw.write("tr:nth-child(odd) {background-color: #efefef;}");
		fw.write("</style>");
		fw.write("</head><body>");
		fw.write("<table>");

		// Host-Headers
		fw.write("<tr><th rowspan=\"3\">ExpNo.</th>");
		for (String hostName : hostNames)
		{
			int span = sutFieldCount;
			for (EClass e : hostColumns.get(hostName))
			{
				span += e.getFeatureCount();
			}

			fw.write(String.format("<th colspan=\"%d\">%s</th>", span, hostName));
//			fw.write("<th> </th>");
		}
		fw.write("</tr>");

		// Benchmark-Headers
		fw.write("<tr>");
		for (String hostName : hostNames)
		{
			fw.write(String.format("<th colspan=\"%d\">%s</th>", sutFieldCount, "SUT"));

			for (EClass e : hostColumns.get(hostName))
			{
				fw.write(String.format("<th colspan=\"%d\">%s</th>", e.getEAllAttributes().size(), e.getName()));
			}
//			fw.write("<th> </th>");
		}
		fw.write("</tr>");

		// Properties-Header
		fw.write("<tr>");
		for (String hostName : hostNames)
		{
			for (EAttribute ea : sutClass.getEAllAttributes())
			{
				fw.write(String.format("<th>%s</th>", ea.getName()));
			}
			for (EClass e : hostColumns.get(hostName))
			{
				for (EAttribute ea : e.getEAllAttributes())
				{
					fw.write(String.format("<th>%s</th>", ea.getName()));
				}
//				for (EReference er: e.getEAllReferences())
//					fw.write(String.format("<th>%s</th>", er.getName()));
			}

//			fw.write("<th> </th>");
		}
		fw.write("</tr>");

		for (int expNo = 0; expNo < expCount; expNo++)
		{
			fw.write(String.format("<tr><td>%d</td>", expNo));

			for (String hostName : hostNames)
			{
				DriverAndIndependentVars b = experimentsForSut.get(hostName).get(expNo);

				for (EAttribute ea : sutClass.getEAllAttributes())
				{
					fw.write(String.format("<td>%s</td>", b.getSutVars().eGet(ea.getFeatureID(), false, false).toString()));
				}

				for (EClass ec : hostColumns.get(hostName))
				{
					if (ec.equals(b.getBenchVars().eClass()))
					{
						for (EAttribute ea : b.getBenchVars().eClass().getEAllAttributes())
						{
							Object o = b.getBenchVars().eGet(ea.getFeatureID(), false, false);
							fw.write(String.format("<td>%s</td>", o == null ? "&#9216;" : o.toString()));
						}
						for (EReference er : b.getBenchVars().eClass().getEAllReferences())
						{
							Object o = b.getBenchVars().eGet(er.getFeatureID(), false, false);
							fw.write(String.format("<td>%s</td>", o == null ? "&#9216;" : o.toString().substring(o.toString().indexOf("(") + 1, o.toString().indexOf(")"))));
							List<?> objectList = (List<?>)o;
							for (Object workObject: objectList)
							{
								System.out.println(workObject.toString());
								for (EReference er2: ((EObjectImpl)workObject).eClass().getEAllReferences())
								{
									Object o2 = ((EObjectImpl)workObject).eGet(er2.getFeatureID(), false, false);
									System.out.println(o2.toString());
									String[] inner = o2.toString().split("\\(");
									for (int i = 1; i < inner.length; ++i)
									{
										fw.write(String.format("<td>%s</td>", inner[i] == null ? "&#9216;" : inner[i].substring(inner[i].indexOf("(") + 1, inner[i].indexOf(")"))));
									}
								}
							}
						}
					} else
					{
						fw.write(String.format("<td colspan=\"%d\"> </td>", ec.getEAllAttributes().size()));
					}
				}

				fw.write("<td> </td>");
			}

			fw.write("</tr>");
		}

		fw.write("</table></body></html>");

		fw.flush();
		fw.close();

		LOGGER.info("HTML was saved to %s", filename);
	}
}
