# vim:tw=80:ts=2:sw=2:colorcolumn=81:nosmartindent
#
# Datastore Interface
# --------------
# Author: Qais Noorshams <noorshams@kit.edu>, Dominik Bruhn <dominik@dbruhn.de>
#
# This file provides some basic functions for interfacing with the
# SPA datastore.  Most of the functions need a installed sqlite3
# binary in the path of the user. Pay attention to the version, sqlite versions
# below 3 do not work.

library(data.table)

#Here-Script which finds out where this file resides
frame_files <- lapply(sys.frames(), function(x) x$ofile)
frame_files <- Filter(Negate(is.null), frame_files)
PATH_SPALIB <- dirname(frame_files[[length(frame_files)]])
rm(frame_files)

source(paste(PATH_SPALIB, "Util.r", sep="/"))

# Constants used to define information and tables in the database
SPADATASTORECONSTANTS = list(filebenchprefix = "filebench", ffsbprefix = "ffsb", 
  configurationrunstable = "configurationRuns", runstable = "runs", 
  ffsbbenchmarkid = "FFSBenchmarkDriver", 
  ffsbindependentvarstable = "ffsbIndependentVars", 
  filebenchbenchmarkid = "FilebenchBenchmarkDriver", 
  filebenchindependentvarstable = "filebenchIndependentVars",
  filebenchindependentvarsfilesettable = "filebenchIndependentVarsFileset",
  filebenchindependentvarsthreadtable = "filebenchIndependentVarsThread",
  filebenchindependentvarsthreadoperationtable = "filebenchIndependentVarsThreadOperation",
  dependentvarstable = "DependentVars", dependentvarvaluestable = "DependentVarsValues");

# Constants used in the results
SPAMETRICCONSTANTS = list("responseTime" = 0, "throughput" = 1, "operations" = 2, 
  "requestSize" = 3, "access" = 4, "requestMix" = 5, "filesetSize" = 6, "filesize" = 7, 
  "threads" = 8,  "opsPerFile" = 9, "queueDepth" = 10, "mergesPerSec" = 11, "serviceTime" = 12, "cpuUtilUser" = 13, "cpuUtilNice" = 14, "cpuUtilSystem" = 15 , "cpuIowait" = 16, "cpuSteal" = 17, "cpuIdle" = 18);

# Constants used in the results
SPATYPECONSTANTS = list("mean" = 0, "stdDev" = 1, "absolute" = 2, "pattern" = 3, 
  "relation" = 4, "percentage" = 5, "singleValue" = 6);

execQuery<-function(db, query) {    
  # Executes a provided query against the provided database. The result of the
  # query is converted to a 'data.table'. Only 'SELECT' queries make sense. If
  # the database does not exist, an error is thrown.
  #
  # Args:
  #   db: A path to the sqlite database file where the query should be executed.
  #   query: The query which should be executed
  #
  # Returns:
  #   A 'data.table' containing the results of the query. The columns of the
  #   table are named after the columns in the database table.

  if (!file.exists(db)) {
    stop(paste("Database",db,"does not exist"));
  }

  useSQLiteCommandLineInterface = TRUE;
  
  if(useSQLiteCommandLineInterface) {
    
    query = paste("'", query, "'", sep="");
    cmd = paste("sqlite3 -batch -csv -header", db, query);
    result = data.table(read.csv(pipe(cmd)));
  } else {
              
    library(RSQLite)
    driver = dbDriver("SQLite");
    connection = dbConnect(driver, dbname=db);
    result = data.table(dbGetQuery(connection, query));
    dbDisconnect(connection);
  }
    
  return(result);
}

getDependentVars <- function(db, runId, benchmarkprefix, metric=NULL, type=NULL) {
# Loads the Dependent Variables of a single benchmark run from a SPA Database.
# Args:
#   db: The sqlite database which contains the run.
#   runId: The runid for which the dependent variables should be returned.
#   benchmarkprefix: The benchmark identifier
#   metric: Specific result metric (optional)
#   type: The result type (optional)
#
# Returns:
#   A 'data.table' containing all requests for this run.

  query = paste("SELECT * FROM ", benchmarkprefix, SPADATASTORECONSTANTS$dependentvarstable, 
                " LEFT JOIN ", benchmarkprefix, SPADATASTORECONSTANTS$dependentvarvaluestable, 
                " USING (","dvId",")",
                " WHERE ",
                "runId=",runId,
                sep="");
  
  if(!is.null(metric)) {
    query = paste(query, " AND ", "opMetric=", metric, sep="");
  }
  
  if(!is.null(type)) {
    query = paste(query, " AND ", "opType=", type, sep="");
  }
  
  query = paste(query,";",sep="");

  result = execQuery(db, query);

# Insert speaking name of metric
  metricValues = result$opMetric;
  result = result[, opMetric:=as.character(opMetric)];
  result$opMetric = names(SPAMETRICCONSTANTS)[metricValues+1];
  
# Insert speaking name of type
  typeValues = result$opType;
  result = result[, opType:=as.character(opType)];
  result$opType = names(SPATYPECONSTANTS)[typeValues+1];
  
  return(result);
}

# The following function are a convenient shortcuts for the above function
# They simply set certain constant values and call the more general function
# The function are also hierarchical meaning that they are reused to only set one constant per function
getFFSBDependentVars <- function(db, runId, metric=NULL, type=NULL) {
  return(getDependentVars(db, runId, SPADATASTORECONSTANTS$ffsbprefix, metric=metric, type=type))
}
getFilebenchDependentVars <- function(db, runId, metric=NULL, type=NULL) { 
  return(getDependentVars(db, runId, SPADATASTORECONSTANTS$filebenchprefix, metric=metric, type=type))
}
getFFSBMeanDependentVars <- function(db, runId) {
  return(getFFSBDependentVars(db, runId, type=SPATYPECONSTANTS$mean))
}
getFFSBSingleDependentVars <- function(db, runId) { 
  return(getFFSBDependentVars(db, runId, type=SPATYPECONSTANTS$singleValue))
}
getFilebenchMeanDependentVars <- function(db, runId) { 
  return(getFilebenchDependentVars(db, runId, type=SPATYPECONSTANTS$mean))
}

getFFSBIndependentVars <- function(db, runId) {
# Loads the Independent Variables of a single FFSB run from a SPA Database.
# This query is only valid for FFSB runs and can not be used for other
# benchmarks.
#
# Args:
#   db: The sqlite database which contains the run.
#   runId: The runid for which the independent variables should be returned.
#
# Returns:
#   A 'data.table' containing a single row which contains the independent
#   variables for the run.
  
  query = paste("SELECT crIdentifier, crTime,",
                " runId, repeatNo, expNo, hostId, expUid, ",
                SPADATASTORECONSTANTS$ffsbindependentvarstable,".* ",
                " FROM ", SPADATASTORECONSTANTS$configurationrunstable, 
                " JOIN ", SPADATASTORECONSTANTS$runstable, " USING (","crId",")",
                " JOIN ", SPADATASTORECONSTANTS$ffsbindependentvarstable, " USING (","runId",")",
                " WHERE runId=",
                runId,";", sep="")

  return(execQuery(db, query))
}

getFilebenchIndependentVarsComplete <- function(db, runId) {
# Loads the complete Independent Variables including request level information 
# of a single Filebench run from a SPA Database. This query is only valid for Filebench 
# runs and can not be used for other benchmarks.
#
# Args:
#   db: The sqlite database which contains the run.
#   runId: The runid for which the independent variables should be returned.
#
# Returns:
#   A 'data.table' containing a single row which contains the independent
#   variables for the run.
  
  query = paste("SELECT crIdentifier, crTime, ",
                " repeatNo, expNo, hostId, expUid, ",
                SPADATASTORECONSTANTS$filebenchindependentvarstable, ".* ",
                ",", SPADATASTORECONSTANTS$filebenchindependentvarsfilesettable, ".* ",
                ",", SPADATASTORECONSTANTS$filebenchindependentvarsthreadtable, ".* ",
                ",", SPADATASTORECONSTANTS$filebenchindependentvarsthreadoperationtable, ".* ",
                " FROM ", SPADATASTORECONSTANTS$configurationrunstable, 
                " JOIN ", SPADATASTORECONSTANTS$runstable, " USING (","crId",")",
                " JOIN ", SPADATASTORECONSTANTS$filebenchindependentvarstable, " ON (",SPADATASTORECONSTANTS$runstable,".runId=",SPADATASTORECONSTANTS$filebenchindependentvarstable,".runId",")",
                " JOIN ", SPADATASTORECONSTANTS$filebenchindependentvarsfilesettable, " ON (",SPADATASTORECONSTANTS$filebenchindependentvarstable,".runId=",SPADATASTORECONSTANTS$filebenchindependentvarsfilesettable,".runId",")",
                " JOIN ", SPADATASTORECONSTANTS$filebenchindependentvarsthreadtable, " ON (",SPADATASTORECONSTANTS$filebenchindependentvarstable,".runId=",SPADATASTORECONSTANTS$filebenchindependentvarsthreadtable,".runId",")",
                " JOIN ", SPADATASTORECONSTANTS$filebenchindependentvarsthreadoperationtable, " ON (",SPADATASTORECONSTANTS$filebenchindependentvarsthreadtable,".runId=",SPADATASTORECONSTANTS$filebenchindependentvarsthreadoperationtable,".runId",")",
                " WHERE ", SPADATASTORECONSTANTS$filebenchindependentvarstable, ".runId=",
                runId,";", sep="")
  return(execQuery(db, query))
}


getFilebenchIndependentVars <- function(db, runId) {
# Loads the Independent Variables of a single Filebench run from a SPA Database.
# This query is only valid for Filebench runs and can not be used for other
# benchmarks.
#
# Args:
#   db: The sqlite database which contains the run.
#   runId: The runid for which the independent variables should be returned.
#
# Returns:
#   A 'data.table' containing a single row which contains the independent
#   variables for the run.
  
  query = paste("SELECT crIdentifier, crTime, ",
                " repeatNo, expNo, hostId, expUid, ",
                SPADATASTORECONSTANTS$filebenchindependentvarstable, ".* ",
                ",", SPADATASTORECONSTANTS$filebenchindependentvarsfilesettable, ".* ",
                ",", SPADATASTORECONSTANTS$filebenchindependentvarsthreadtable, ".* ",
#                ",", SPADATASTORECONSTANTS$filebenchindependentvarsthreadoperationtable, ".* ",
                " FROM ", SPADATASTORECONSTANTS$configurationrunstable, 
                " JOIN ", SPADATASTORECONSTANTS$runstable, " USING (","crId",")",
                " JOIN ", SPADATASTORECONSTANTS$filebenchindependentvarstable, " ON (",SPADATASTORECONSTANTS$runstable,".runId=",SPADATASTORECONSTANTS$filebenchindependentvarstable,".runId",")",
                " JOIN ", SPADATASTORECONSTANTS$filebenchindependentvarsfilesettable, " ON (",SPADATASTORECONSTANTS$filebenchindependentvarstable,".runId=",SPADATASTORECONSTANTS$filebenchindependentvarsfilesettable,".runId",")",
                " JOIN ", SPADATASTORECONSTANTS$filebenchindependentvarsthreadtable, " ON (",SPADATASTORECONSTANTS$filebenchindependentvarstable,".runId=",SPADATASTORECONSTANTS$filebenchindependentvarsthreadtable,".runId",")",
#                " JOIN ", SPADATASTORECONSTANTS$filebenchindependentvarsthreadoperationtable, " ON (",SPADATASTORECONSTANTS$filebenchindependentvarsthreadtable,".runId=",SPADATASTORECONSTANTS$filebenchindependentvarsthreadoperationtable,".runId",")",
                " WHERE ", SPADATASTORECONSTANTS$filebenchindependentvarstable, ".runId=",
                runId,";", sep="")
  return(execQuery(db, query))
}

getFFSBVars <- function(db, runId, metric=NULL, type=NULL) {
# Loads both the Independent Variables and the Dependent Variables 
# of a single FFSB run from a SPA Database.
#
# Args:
#   db: The sqlite database which contains the run.
#   runId: The runid for which the independent variables should be returned.
#   metric: Specific result metric (optional)
#   type: The result type (optional)
#
# Returns:
#   A 'data.table' containing a single row which contains the independent
#   variables for the run.
  
  DV = getFFSBDependentVars(db, runId, metric=metric, type=type);
  IV = getFFSBIndependentVars(db, runId);
  setkey(IV, runId);
  
  # Join tables on runId
  return(merge(IV,DV))
}

getFilebenchVars <- function(db, runId, metric=NULL, type=NULL) {
# Loads both the Independent Variables and the Dependent Variables 
# of a single Filebench run from a SPA Database.
#
# Args:
#   db: The sqlite database which contains the run.
#   runId: The runid for which the independent variables should be returned.
#   metric: Specific result metric (optional)
#   type: The result type (optional)
#
# Returns:
#   A 'data.table' containing a single row which contains the independent
#   variables for the run.
  
  DV = getFilebenchDependentVars(db, runId, metric=metric, type=type);
  IV = getFilebenchIndependentVars(db, runId);
  setkey(IV, runId);
  
# Join tables on runId
  return(merge(IV,DV))
}

getFFSBRunIds<-function(db) {
  # Returns the runIds of all FFSB runs which are saved in the database. The
  # function only returns those calls which have at least one dependent variable
  # and which have their independent variable stored in the database. Although
  # the later condition should be true for every run, some databases might be
  # damanged and not contain the independent variables.
  # 
  # Args: 
  #   db: The sqlite database for which all runs should be returned
  #
  # Returns:
  #   A 'data.table' containing all FFSB runs which met the conditions specified
  #   above.
  
#  query = paste("SELECT runId from runs WHERE",
#                "(SELECT runId FROM ffsbIndependentVars WHERE",
#                "runId=runs.runId LIMIT 1) IS NOT NULL AND",
#                "(SELECT dvId FROM ffsbDependentVars WHERE",
#                "runId=runs.runId LIMIT 1) IS NOT NULL AND",
#                "(benchmarkId=\"FFSBenchmark\" OR ",
#                "benchmarkId=\"FFSBenchmarkDriver\");")
  
  query = paste("SELECT runId FROM ", SPADATASTORECONSTANTS$runstable, " WHERE",
                " (SELECT runId FROM ", SPADATASTORECONSTANTS$ffsbindependentvarstable, 
                " WHERE runId=", SPADATASTORECONSTANTS$runstable,".runId LIMIT 1) IS NOT NULL AND",
                " (SELECT dvId FROM ", SPADATASTORECONSTANTS$ffsbprefix, 
                  SPADATASTORECONSTANTS$dependentvarstable, " WHERE",
                " runId=", SPADATASTORECONSTANTS$runstable, ".runId LIMIT 1) IS NOT NULL AND",
                " (benchmarkId=\"",SPADATASTORECONSTANTS$ffsbbenchmarkid,"\");", sep ="")
  
  return(execQuery(db, query)[,runId])
}

getFilebenchRunIds<-function(db) {
# Returns the runIds of all Filebench runs which are saved in the database. The
# function only returns those calls which have at least one dependent variable
# and which have their independent variable stored in the database. Although
# the later condition should be true for every run, some databases might be
# damanged and not contain the independent variables.
# 
# Args: 
#   db: The sqlite database for which all runs should be returned
#
# Returns:
#   A 'data.table' containing all Filebench runs which met the conditions specified
#   above.
  
  query = paste("SELECT runId FROM ", SPADATASTORECONSTANTS$runstable, " WHERE",
                " (SELECT runId FROM ", SPADATASTORECONSTANTS$filebenchindependentvarstable, 
                " WHERE runId=", SPADATASTORECONSTANTS$runstable,".runId LIMIT 1) IS NOT NULL AND",
                " (SELECT dvId FROM ", SPADATASTORECONSTANTS$filebenchprefix, 
                SPADATASTORECONSTANTS$dependentvarstable, " WHERE",
                " runId=", SPADATASTORECONSTANTS$runstable, ".runId LIMIT 1) IS NOT NULL AND",
                " (benchmarkId=\"",SPADATASTORECONSTANTS$filebenchbenchmarkid,"\");", sep ="")
  
  return(execQuery(db, query)[,runId])
}

####################
# Top-level method #
####################
getAllFFSBVars<-function(db, metric=NULL, type=NULL) {
# Returns all IndependentVariables combined with the DependentVariables of 
# FFSB benchmarks for all runs found in the database. Results can be adjusted 
#  using the parameters. Removes columns without content before returning.
#
# Args: 
#   db: The sqlite database for which all information should be returned
#   metric: Specific result metric (optional)
#   type: The result type (optional)
#
# Returns:
#   A 'data.table' containing all FFSB runs.
  
  runIds = getFFSBRunIds(db);
  
  result = data.table()
  
  if(length(runIds) > 0) {
    for(i in 1:length(runIds)) {
      result = rbind(result, getFFSBVars(db, runIds[i], metric=metric, type=type))
    }
  }
  
  # Remove unnecessary columns before returning
  return(removeNAColumns(result[, runId.1 := NULL]))
}

####################
# Top-level method #
####################
getAllFilebenchVars<-function(db, metric=NULL, type=NULL) {
# Returns all IndependentVariables combined with the DependentVariables of 
# Filebench benchmarks for all runs found in the database. Results can be adjusted 
#  using the parameters. Removes columns without content before returning.
#
# Args: 
#   db: The sqlite database for which all information should be returned
#   metric: Specific result metric (optional)
#   type: The result type (optional)
#
# Returns:
#   A 'data.table' containing all Filebench runs.
  
  runIds = getFilebenchRunIds(db);
  
  result = data.table()
  
  if(length(runIds) > 0) {
    for(i in 1:length(runIds)) {
      result = rbind(result, getFilebenchVars(db, runIds[i], metric=metric, type=type))
    }
  }
  
  # Remove unnecessary columns before returning
  return(removeNAColumns(result[, runId.1 := NULL][, runId.2 := NULL]))
}