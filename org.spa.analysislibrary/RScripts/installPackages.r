# vim:tw=80:ts=2:sw=2:colorcolumn=81:nosmartindent
#
# Package Installation
# --------------
# Author: Dominik Bruhn <dominik@dbruhn.de>
#
# This file install all packages required by the Analysis Libraries to function.
# You must pay attention that:
#    - You have a recent R version installed 
#    - You have access to the internet and the CRAN mirror
#    - You have a compiler (gcc) installed if packages must be compiled from
#      source.
#    - The package list is updated once before running this.
#
# Depending on the operating system and the R configuration the packages are 
# either downloaded as binaries or compiled on the system. The later approach
# heavily extends the time period needed for the package installation.


instPackage <- function(x) { 
  x <- as.character(substitute(x)) 
  if(!isTRUE(x %in% .packages(all.available=TRUE))) { 
    eval(parse(text=paste("install.packages('", x, "')", sep=""))) 
  } 
} 

#Set CRAN mirror so that the user doesn't get asked for the mirror
old.repos <- getOption("repos") 

#Reset CRAN URL when function exists
on.exit(options(repos = old.repos))

new.repos <- old.repos 

new.repos["CRAN"] <- "http://ftp5.gwdg.de/pub/misc/cran/"
options(repos = new.repos) 

#Update the local packages

update.packages()

#Actual load the packages
instPackage("data.table")
instPackage("caret")
instPackage("forecast")
instPackage("party")
instPackage("partykit")
instPackage("Cubist")
instPackage("earth")
instPackage("ggplot2")
instPackage("xtable")
## Only Needed for Latex Output of Graphs
##install.packages("tikzDevice", repos="http://R-Forge.R-project.org")
#print("Building packages from sources might require RTools on Windows (http://cran.at.r-project.org/bin/windows/Rtools/)")
#install.packages("tikzDevice", repos="http://R-Forge.R-project.org", type="source")
instPackage("filehash")

if(Sys.info()['sysname'] != 'Windows') { 
  #doMC is not available on Windows
  instPackage("doMC")
}

instPackage("RSQLite")
