# vim:tw=80:ts=2:sw=2:colorcolumn=81:nosmartindent
#
# Main Loader
# --------------
# Authors: Qais Noorshams <noorshams@kit.edu>, Dominik Bruhn <dominik@dbruhn.de>
#
# This file loads available data sets into the current R environment. It
# also loads the libraries which can be used for analyzing these results.

#Here-Script which finds out where this file resides
frame_files <- lapply(sys.frames(), function(x) x$ofile)
frame_files <- Filter(Negate(is.null), frame_files)
PATH_SPA <- dirname(frame_files[[length(frame_files)]])
rm(frame_files)

#Load Libraries
source(paste(PATH_SPA, "lib/Util.r", sep="/"))
source(paste(PATH_SPA, "lib/DataStoreInterface.r", sep="/"))
source(paste(PATH_SPA, "lib/RegressionModeling.r", sep="/"))
source(paste(PATH_SPA, "lib/RegressionOptimization.r", sep="/"))

if(Sys.info()['sysname'] != 'Windows') {
#Register parallel backend on non Windows systems.
#This is not supported on Windows, the computation will not be
#parallized there.
  
  library(doMC)
  cat("Registering doMC parallel backend\n")
  registerDoMC()
}


#Load Examples

filebenchexample = getAllFilebenchVars(paste(PATH_SPA,"data/test/sqlite/filebenchtest.sqlite", sep="/"));
ffsbexample = getAllFFSBVars(paste(PATH_SPA,"data/test/sqlite/ffsbtest.sqlite", sep="/"), type=SPATYPECONSTANTS$mean);
