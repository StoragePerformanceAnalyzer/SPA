package edu.kit.sdq.storagebenchmarkharness.datastore.sqlite;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.emf.common.util.Enumerator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;

import edu.kit.sdq.storagebenchmarkharness.Logger;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueComposite;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariablesValueSingle;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfBenchmark;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.IndependentVariablesOfSut;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelFactory;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.SBHModelPackage;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Type;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.Operations.OperationsPackage;
import edu.kit.sdq.storagebenchmarkharness.exceptions.InconsistentTableException;

/**
 * This class maps between a SQLite Table and the EMF-Objects which are used in
 * the Benchmark Harness. It does two things:
 * 
 * 1. It creates the tables which are needed for storing the independent and
 * dependent variables. It ensures that all columns which are needed exist and
 * have the right type. If necessary it adds new columns.
 * 
 * 2. It provides a simple interface for saving a independent or dependent
 * variable object. To accomplish this, a SQL statement is constructed and the
 * values are filled in.
 * 
 * In short this is a poor mans ORM for use with EMF-Objects.
 * 
 * @author Dominik Bruhn 
 * @author Axel Busch
 * 
 */
public final class SQLiteHelper
{
	private static final Logger LOGGER = Logger.getLogger(SQLiteHelper.class);

	private final Map<String, SQLiteStatement> ivStmnts;
	private final Map<String, SQLiteStatement> dvStmnts;

	/**
	 * Creates a new SQLiteHelper
	 */
	public SQLiteHelper()
	{
		this.ivStmnts = Maps.newHashMap();
		this.dvStmnts = Maps.newHashMap();
	}

	/**
	 * Does the two jobs (see above): Tables for the dependent and independent
	 * variables are created for each BenchmarkDriver which exists. The function
	 * works as follows: If the tables do not exist, a new table with only the
	 * basic columns is created. In a next step, for each attribute of the EMF
	 * Object a new column is added to the table if it is missing. If the column
	 * already exists, its type is checked.
	 * 
	 * In a second step, the SQL-Statements for inserting in the tables are
	 * created and prepared on the connection. The ensures a fast execution.
	 * 
	 * This method should be executed once before starting to insert results.
	 * 
	 * @param db
	 *            The SQLite Database connection
	 * @throws SQLiteException
	 */
	public void setupTables(SQLiteConnection db) throws SQLiteException
	{

		DependentVariables dep = SBHModelFactory.eINSTANCE.createDependentVariables();
		// For FFSB
		createTables(db, getPrefixForVariables(SBHModelPackage.eINSTANCE.getIndependentVariablesOfFFSB()));
		setupIndependentTables(db, SBHModelPackage.eINSTANCE.getIndependentVariablesOfFFSB());
		dep.setBenchmarkPrefix("ffsb");
		setupDependentTables(db, dep);

		// For Postmark
		createTables(db, getPrefixForVariables(SBHModelPackage.eINSTANCE.getIndependentVariablesOfPostmark()));
		setupIndependentTables(db, SBHModelPackage.eINSTANCE.getIndependentVariablesOfPostmark());
		dep.setBenchmarkPrefix("postmark");
		setupDependentTables(db, dep);

		// For Filebench
		createTables(db, getPrefixForVariables(SBHModelPackage.eINSTANCE.getIndependentVariablesOfFilebench()));
		setupIndependentTables(db, SBHModelPackage.eINSTANCE.getIndependentVariablesOfFilebench());
		dep.setBenchmarkPrefix("filebench");
		setupDependentTables(db, dep);

		// Create Filebench operations
		List<EClass> filebenchOps = getAllFilebenchOps();
		setupFilebenchOperations(db, filebenchOps);
	}

	/**
	 * Creates actual tables. Method takes table template and inserts it into
	 * the database with the corresponding benchmark prefix.
	 * 
	 * @param db
	 *            The SQLite Database connection
	 * @param prefix
	 *            Benchmark prefix
	 * @throws SQLiteException
	 */
	private void createTables(SQLiteConnection db, String prefix) throws SQLiteException
	{
		// Create the two tables, one for independent and one for dependent vars
		String schema = SQLiteDataStore.getSchema("table").replace("{{TEMPLATE}}", prefix);
		db.exec(schema);
	}

	/**
	 * Does the job explained above for one BenchmarkDriver (for the independent
	 * variables).
	 * 
	 * @param db
	 *            SQLite database which stores the tables
	 * @param benchVars
	 *            A EMF-Eclass describing the independent variables for the
	 *            specific benchmark driver
	 * @throws SQLiteException
	 */
	private void setupIndependentTables(SQLiteConnection db, EClass benchVars) throws SQLiteException
	{
		String prefix = getPrefixForVariables(benchVars);

		// Update the table with their corresponding columns
		// In the IndependentVariableTable, both the benchmark
		// indepedentvariables and the sut independent variables are stored.
		updateTableFromEClass(db, prefix + "IndependentVars", SBHModelPackage.eINSTANCE.getIndependentVariablesOfSut());
		updateTableFromEClass(db, prefix + "IndependentVars", benchVars);

		// Construct queries for insertion of independent variables.
		StringBuilder ivSql = new StringBuilder("INSERT INTO " + prefix + "IndependentVars (runId, ");
		int varNo = 1;
		for (EAttribute ea : SBHModelPackage.eINSTANCE.getIndependentVariablesOfSut().getEAllAttributes())
		{
			varNo++;
			ivSql.append(ea.getName() + ", ");
		}
		for (EAttribute ea : benchVars.getEAllAttributes())
		{
			varNo++;
			ivSql.append(ea.getName() + ", ");
		}
		ivSql.delete(ivSql.length() - 2, ivSql.length());

		ivSql.append(") VALUES (");
		ivSql.append(Strings.repeat("?,", varNo));
		ivSql.delete(ivSql.length() - 1, ivSql.length());
		ivSql.append(");");
		LOGGER.debug("SQL for prefix %s=%s", prefix, ivSql.toString());

		// Rembemeber the statements for later use when values should be
		// inserted
		ivStmnts.put(prefix, db.prepare(ivSql.toString()));
	}

	/**
	 * Does the job explained above for one BenchmarkDriver (for the dependent
	 * variables).
	 * 
	 * @param db
	 *            SQLite database which stores the tables
	 * @param benchVars
	 *            A EMF-Eclass describing the independent variables for the
	 *            specific benchmark driver specific benchmark driver
	 * @throws SQLiteException
	 */
	private void setupDependentTables(SQLiteConnection db, DependentVariables benchVars) throws SQLiteException
	{
		String prefix = benchVars.getBenchmarkPrefix();

		// Construct queries for insertion of dependent variables.
		StringBuilder dvSql = new StringBuilder("INSERT INTO " + prefix + "DependentVars (runId, ");
		dvSql.append("benchPrefix");
		dvSql.append(") VALUES (?, ?");
		dvSql.append(");");

		// Rembemeber the statements for later use when values should be
		// inserted
		dvStmnts.put(prefix + "_first", db.prepare(dvSql.toString()));

		LOGGER.debug("SQL for prefix %s=%s", prefix + "_first", dvSql.toString());

		dvSql = new StringBuilder("INSERT INTO " + prefix + "DependentVarsValues (dvId, ");
		dvSql.append("operation, opMetric, opValue, source, opTimestamp, opType");
		dvSql.append(") VALUES (?, ?, ?, ?, ?, ?, ?");
		//dvSql.append("operation, opMetric, opValue, opTimestamp, opType");
		//dvSql.append(") VALUES (?, ?, ?, ?, ?, ?");
		dvSql.append(");");

		dvStmnts.put(prefix + "_second", db.prepare(dvSql.toString()));

		LOGGER.debug("SQL for prefix %s=%s", prefix + "_second", dvSql.toString());
	}

	public void setupFilebenchOperations(SQLiteConnection db, List<EClass> filebenchOps) throws SQLiteException
	{
		String prefix = getPrefixForVariables(SBHModelPackage.eINSTANCE.getIndependentVariablesOfFilebench());
		String parentTable = "IndependentVars" + SBHModelPackage.eINSTANCE.getThread().getName()
				+ OperationsPackage.eINSTANCE.getOperation().getName();

		for (int i = 0; i < filebenchOps.size(); ++i)
		{
			updateTableFromEClass(db, prefix + parentTable, filebenchOps.get(i));
		}
	}

	/**
	 * Does the job explained above for one BenchmarkDriver.
	 * 
	 * @param db
	 *            SQLite database which stores the tables
	 * @param benchVars
	 *            A EMF-Eclass describing the independent variables for the
	 *            specific benchmark driver
	 * @param resultVars
	 *            A EMF-Eclass describing the dependent variables for the
	 *            specific benchmark driver
	 * @throws SQLiteException
	 */
	@Deprecated
	private void setupTable(SQLiteConnection db, EClass benchVars, EClass resultVars) throws SQLiteException
	{
		String prefix = getPrefixForVariables(benchVars);

		// Create the two tables, one for independent and one for dependent vars
		String schema = SQLiteDataStore.getSchema("table").replace("{{TEMPLATE}}", prefix);
		db.exec(schema);

		// Update the two tables with their corresponding columns
		// In the IndependentVariableTable, both the benchmark
		// indepedentvariables and the sut independent variables are stored.
		updateTableFromEClass(db, prefix + "IndependentVars", SBHModelPackage.eINSTANCE.getIndependentVariablesOfSut());
		updateTableFromEClass(db, prefix + "IndependentVars", benchVars);
		updateTableFromEClass(db, prefix + "DependentVars", resultVars);

		// Construct queries for insertion of indepenendet variables.
		StringBuilder ivSql = new StringBuilder("INSERT INTO " + prefix + "IndependentVars (runId, ");
		int varNo = 1;
		for (EAttribute ea : SBHModelPackage.eINSTANCE.getIndependentVariablesOfSut().getEAllAttributes())
		{
			varNo++;
			ivSql.append(ea.getName() + ", ");
		}
		for (EAttribute ea : benchVars.getEAllAttributes())
		{
			varNo++;
			ivSql.append(ea.getName() + ", ");
		}
		ivSql.delete(ivSql.length() - 2, ivSql.length());

		ivSql.append(") VALUES (");
		ivSql.append(Strings.repeat("?,", varNo));
		ivSql.delete(ivSql.length() - 1, ivSql.length());
		ivSql.append(");");
		LOGGER.debug("SQL for prefix %s=%s", prefix, ivSql.toString());

		// Construct queries for insertion of dependent variables.
		StringBuilder dvSql = new StringBuilder("INSERT INTO " + prefix + "DependentVars (runId, ");
		varNo = 1;
		for (EAttribute ea : resultVars.getEAllAttributes())
		{
			varNo++;
			dvSql.append(ea.getName() + ", ");
		}
		dvSql.delete(dvSql.length() - 2, dvSql.length());

		dvSql.append(") VALUES (");
		dvSql.append(Strings.repeat("?,", varNo));
		dvSql.delete(dvSql.length() - 1, dvSql.length());
		dvSql.append(");");
		LOGGER.debug("SQL for prefix %s=%s", prefix, dvSql.toString());

		// Rembemeber the statements for later use when values should be
		// inserted
		ivStmnts.put(prefix, db.prepare(ivSql.toString()));
		dvStmnts.put(prefix, db.prepare(dvSql.toString()));
	}

	/**
	 * Makes sure that all attributes in this eClass have corresponding columns
	 * in the table
	 * 
	 * @param db
	 *            The sqlite database connection
	 * @param tablename
	 *            The sql table which should be checked
	 * @param eclass
	 *            A EClass describing the columns which are needed.
	 * @throws SQLiteException
	 */
	private static void updateTableFromEClass(SQLiteConnection db, String tablename, EClass eclass) throws SQLiteException
	{
		Map<String, String> columnsInTable = getColumnsInTable(db, tablename);

		for (EAttribute ea : eclass.getEAllAttributes())
		{
			String columnname = ea.getName();
			String type = ecoreToSQLType(ea.getEAttributeType());

			checkCreateColumn(db, columnsInTable, tablename, columnname, type);
		}

		for (EReference er : eclass.getEAllReferences())
		{
			EClass contClass = er.getEReferenceType();
			String contClassTableName = tablename + contClass.getName();
			// Create operation specific table + auto increment
			LOGGER.debug("Create SQL %s", "CREATE TABLE IF NOT EXISTS " + contClassTableName
					+ "(runId INTEGER NOT NULL, FOREIGN KEY(runId) REFERENCES " + tablename + "(runId));");
			//LOGGER.debug("Create SQL %s", "CREATE INDEX IF NOT EXISTS " + contClassTableName + "RunIdIdx ON " + contClassTableName + "(id);");
			db.exec("CREATE TABLE IF NOT EXISTS " + contClassTableName
					+ "(runId INTEGER NOT NULL, FOREIGN KEY(runId) REFERENCES " + tablename + "(runId));");
			//db.exec("CREATE INDEX IF NOT EXISTS " + contClassTableName + "RunIdIdx ON " + contClassTableName + "(id);");
			updateTableFromEClass(db, contClassTableName, contClass);
		}
	}

	/**
	 * Returns all columns in the table and their type
	 * 
	 * @param db
	 * @param tablename
	 * @return
	 * @throws SQLiteException
	 */
	private static Map<String, String> getColumnsInTable(SQLiteConnection db, String tablename) throws SQLiteException
	{
		Map<String, String> columnsInTable = Maps.newHashMap();
		SQLiteStatement sColumns = db.prepare("pragma table_info(" + tablename + ");");
		while (sColumns.step())
		{
			columnsInTable.put(sColumns.columnString(1).toLowerCase(), sColumns.columnString(2).toUpperCase());
		}
		sColumns.dispose();

		return columnsInTable;
	}

	/**
	 * Check if the column exists in the table, if yes, check the type, if no
	 * create it with the appropriate type.
	 * 
	 * @param db
	 * @param columnsInTable
	 *            A Map containing all columns which exists in the table and
	 *            their type.
	 * @param tablename
	 *            The table which should be checked
	 * @param columnname
	 *            The column in this table which should be checked
	 * @param type
	 *            The desired type of the column
	 * @throws SQLiteException
	 * @throws InconsistentTableException
	 *             If the column already exists in the table but the type is
	 *             wrong.
	 */
	private static void checkCreateColumn(SQLiteConnection db, Map<String, String> columnsInTable, String tablename, String columnname, String type)
			throws SQLiteException, InconsistentTableException
	{
		String columnnameLower = columnname.toLowerCase();
		String typeUpper = type.toUpperCase();

		if (columnsInTable.containsKey(columnnameLower))
		{
			String actualType = columnsInTable.get(columnnameLower);
			if (!actualType.equals(typeUpper))
			{
				throw new InconsistentTableException("Column " + tablename + "." + columnname + " should be " + typeUpper + " but is " + actualType);
			}
		} else
		{
			LOGGER.debug("Create SQL %s", "ALTER TABLE " + tablename + " ADD COLUMN " + columnname + " " + typeUpper + " DEFAULT NULL;");
			db.exec("ALTER TABLE " + tablename + " ADD COLUMN " + columnname + " " + typeUpper + " DEFAULT NULL;");
		}
	}

	/**
	 * Saves the independent variables of a run in the appropriate table in the
	 * database. The independent variables are split into those variables which
	 * are independent of the benchmark (the sutVars) and those which are
	 * special for this benchmark.
	 * 
	 * @param runId
	 * @param sutVars
	 * @param benchVars
	 * @throws SQLiteException
	 */
	public void saveIndependentVars(long runId, IndependentVariablesOfSut sutVars, IndependentVariablesOfBenchmark benchVars,  SQLiteConnection db) throws SQLiteException
	{
		LOGGER.trace("Saving independent Vars %s %s", sutVars, benchVars);
		String prefix = getPrefixForVariables(benchVars.eClass());
		LOGGER.trace("Saving to prefix %s", prefix);

		SQLiteStatement stmt = ivStmnts.get(prefix);

		stmt.bind(1, runId);
		int paramNo = 2;
		for (EAttribute ea : sutVars.eClass().getEAllAttributes())
		{
			LOGGER.trace("%s=%s", ea.getName(), sutVars.eGet(ea.getFeatureID(), false, false));
			bind(stmt, paramNo, sutVars.eGet(ea.getFeatureID(), false, false));
			paramNo++;
		}

		for (EAttribute ea : benchVars.eClass().getEAllAttributes())
		{
			LOGGER.trace("%s=%s", ea.getName(), benchVars.eGet(ea.getFeatureID(), false, false));
			bind(stmt, paramNo, benchVars.eGet(ea.getFeatureID(), false, false));
			paramNo++;
		}
		
		for (EReference er : benchVars.eClass().getEAllReferences())
		{
			List<?> l = (List<?>) benchVars.eGet(er.getFeatureID(), false, false);
			saveIndependentReferenceVars((EObjectImpl)l.get(0), prefix + "IndependentVars" + er.getEReferenceType().getName(), runId, db);
		}
		
		stmt.step();
		stmt.reset();
		LOGGER.trace("Finished Saving independent Vars %s %s", sutVars, benchVars);
	}
	
	private void saveIndependentReferenceVars(EObjectImpl input, String statement, long runId, SQLiteConnection db) throws SQLiteException
	{
		StringBuilder ivSql = new StringBuilder("INSERT INTO " + statement + " (runId");
		List<EAttribute> attributes = input.eClass().getEAllAttributes();
		for (int i = 0; i < attributes.size(); ++i)
		{
			EAttribute ea = attributes.get(i);
			LOGGER.trace("%s=%s", ea.getName(), input.eClass().getEStructuralFeature((ea.eClass().getEStructuralFeature(ea.getFeatureID())).getFeatureID()));
			
			ivSql.append(", " + ea.getName());
			//System.out.println(input.eGet(ea.getFeatureID(), false, false));
			//System.out.println(ea.eClass().getName());
			//paramNo++;
		}
		ivSql.append(") VALUES (?");
		for (int i = 0; i < attributes.size(); ++i)
			ivSql.append(", ?");
		ivSql.append(");");
		
		SQLiteStatement stmt = db.prepare(ivSql.toString());
		bind(stmt, 1, runId);
		
		for (int i = 0; i < attributes.size(); ++i)
		{
			EAttribute ea = attributes.get(i);
			bind(stmt, i+2, input.eGet(ea.getFeatureID(), false, false));
		}
		
		for (EReference er : input.eClass().getEAllReferences())
		{
			EClass contClass = er.getEReferenceType();
			String contClassTableName = statement + contClass.getName();
			List<EObjectImpl> l = (List<EObjectImpl>)input.eGet(er.getFeatureID(), false, false);
			for (int i = 0; i < l.size(); ++i)
				saveIndependentReferenceVars(l.get(i), contClassTableName, runId, db);
		}
		stmt.step();
		stmt.reset();
	}

	/**
	 * Save the dependent variables of a run in the appropriate table. There can
	 * be zero or more results which should be saved.
	 * 
	 * @param runId
	 * @param depVars
	 * @throws SQLiteException
	 */
	public void saveDependentVars(long runId, List<DependentVariables> depVarsList, SQLiteConnection db) throws SQLiteException
	{
		String prefix = depVarsList.get(0).getBenchmarkPrefix();

		SQLiteStatement stmt = dvStmnts.get(prefix + "_first");
		int paramNo = 0;
		stmt.bind(++paramNo, runId);
		stmt.bind(++paramNo, depVarsList.get(0).getBenchmarkPrefix());

		stmt.step();
		stmt.reset();

		SQLiteStatement sIndex = db.prepare("SELECT last_insert_rowid() FROM " + prefix + "DependentVars");
		sIndex.step();
		String dvId = sIndex.columnString(0);
		sIndex.dispose();

		for (int j = 0; j < depVarsList.size(); ++j)
		{
			DependentVariables depVars = depVarsList.get(j);
			for (int i = 0; i < depVars.getValues().size(); ++i)
			{
				stmt = dvStmnts.get(prefix + "_second");
				paramNo = 0;

				stmt.bind(++paramNo, dvId);
				stmt.bind(++paramNo, depVars.getValues().get(i).getOperation());
				stmt.bind(++paramNo, depVars.getValues().get(i).getOperationMetric().getValue());
				stmt.bind(++paramNo, depVars.getValues().get(i).getValue());
				stmt.bind(++paramNo, depVars.getValues().get(i).getSource());
				if (depVars.getValues().get(i) instanceof DependentVariablesValueSingle)
				{
					//System.out.println(depVars.getValues().get(i));
					stmt.bind(++paramNo, ((DependentVariablesValueSingle) depVars.getValues().get(i)).getTimestamp());
					stmt.bind(++paramNo, Type.SINGLE_VALUE_VALUE);
				} else if (depVars.getValues().get(i) instanceof DependentVariablesValueComposite)
				{
					//System.out.println(depVars.getValues().get(i));
					stmt.bind(++paramNo, "");
					stmt.bind(++paramNo, ((DependentVariablesValueComposite) depVars.getValues().get(i)).getType().getValue());
				}
				stmt.step();
				stmt.reset();
			}
			LOGGER.trace("Finished Saving Dependent Vars %s", depVars);
		}

	}

	/**
	 * Returns the table prefix which should be used for the provided EClass.
	 * This is dependent on the BenchmarkDriver.
	 * 
	 * @param eclass
	 * @return
	 */
	private static String getPrefixForVariables(EClass eclass)
	{
		if (eclass.equals(SBHModelPackage.eINSTANCE.getIndependentVariablesOfFFSB()))
		{
			return "ffsb";
		} else if (eclass.equals(SBHModelPackage.eINSTANCE.getIndependentVariablesOfPostmark()))
		{
			return "postmark";
		} else if (eclass.equals(SBHModelPackage.eINSTANCE.getIndependentVariablesOfFilebench()))
		{
			return "filebench";
		} else
		{
			throw new IllegalArgumentException("Unkown class " + eclass);
		}
	}

	/**
	 * Binds a Object to a SQLiteStatement. The right {@code bind} method of the
	 * {@code SQLiteStatement} has to be used because otherwise the type is not
	 * preserved.
	 * 
	 * @param stmt
	 * @param idx
	 * @param o
	 * @throws SQLiteException
	 */
	private static void bind(SQLiteStatement stmt, int idx, Object o) throws SQLiteException
	{
		if (o == null)
		{
			stmt.bindNull(idx);
		} else if (o instanceof Double)
		{
			stmt.bind(idx, (Double) o);
		} else if (o instanceof String)
		{
			stmt.bind(idx, (String) o);
		} else if (o instanceof Integer)
		{
			stmt.bind(idx, (Integer) o);
		} else if (o instanceof Long)
		{
			stmt.bind(idx, (Long) o);
		} else if (o instanceof Boolean)
		{
			stmt.bind(idx, (Boolean) o == Boolean.TRUE ? 1 : 0);
		} else if (o instanceof Enumerator)
		{
			// ECore EEnum
			stmt.bind(idx, ((Enumerator) o).getLiteral());
		} else if (o instanceof Timestamp)
		{
			stmt.bind(idx, ((Timestamp) o).getTime());
		} else if (o instanceof File)
		{
			stmt.bind(idx, ((File) o).getAbsolutePath());
		} else if (o instanceof Integer)
		{
			stmt.bind(idx, (Integer) o);
		} else
		{
			throw new IllegalArgumentException("Could not bind Object " + o);
		}
	}

	/**
	 * Gets the SQL type which corresponds to a Java type.
	 * 
	 * @param javaType
	 * @return
	 */
	private static String ecoreToSQLType(EDataType type)
	{
		String clazz = type.getInstanceClass().getCanonicalName();
		if (type instanceof EEnum)
		{
			// Enums will be converted to their literal String
			return "VARCHAR";
		} else if (clazz.equals("double"))
		{
			return "REAL";
		} else if (clazz.equals("java.lang.String"))
		{
			return "VARCHAR";
		} else if (clazz.equals("int") || clazz.equals("long") || clazz.equals("java.lang.Integer"))
		{
			// Integers in SQLite can be of multiple lengths up to 64bit.
			return "INTEGER";
		} else if (clazz.equals("boolean") || clazz.equals("java.lang.Boolean"))
		{
			// SQLite does not know booleans, map them to ints
			return "INTEGER";
		} else if (clazz.equals("boolean") || clazz.equals("java.io.File"))
		{
			return "VARCHAR";
		}
		throw new IllegalArgumentException("Can not convert ECore type '" + clazz + "' to SQLITE");
	}

	public List<EClass> getAllFilebenchOps()
	{
		ArrayList<EClass> result = new ArrayList<EClass>();
		result.add(OperationsPackage.eINSTANCE.getAppendFileRand());
		result.add(OperationsPackage.eINSTANCE.getCloseFile());
		result.add(OperationsPackage.eINSTANCE.getCreateFile());
		result.add(OperationsPackage.eINSTANCE.getDelay());
		result.add(OperationsPackage.eINSTANCE.getDeleteFile());
		result.add(OperationsPackage.eINSTANCE.getFsync());
		result.add(OperationsPackage.eINSTANCE.getOpenFile());
		result.add(OperationsPackage.eINSTANCE.getRead());
		result.add(OperationsPackage.eINSTANCE.getReadWholeFile());
		result.add(OperationsPackage.eINSTANCE.getStatFile());
		result.add(OperationsPackage.eINSTANCE.getWrite());
		result.add(OperationsPackage.eINSTANCE.getWriteHoleFile());
		return result;
	}
}
