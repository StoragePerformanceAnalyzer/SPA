# vim:tw=80:ts=2:sw=2:colorcolumn=81:nosmartindent
#
# Util functions
# --------------
# Author: Dominik Bruhn <dominik@dbruhn.de>
#
# This file contains some widely used helper functions which are not available in R per
# default.

stdErr <- function(x) {
  # Calculates the standart error of x.
  #
  # Args:
  #   x: The data structure for which the standart error should be calculated.
  #      Can be a vector or a list.
  #
  # Returns:
  #   The standart error
  sd(x)/sqrt(length(x))
}

withinQuantile<-function(data, q) {
  # This function returns a vector which contains a boolean for every row in
  # 'data'. This boolean is true if the value if within the q-quantile range.
  # 
  # Args:
  #   data: The vector for which the calcuation should be done
  #   q: The quantile range
  #
  # Returns:
  #   A boolean vector which contains TRUE iff the value of data is within the
  #   q-quantile range.

  r=quantile(data, c(q, 1-q))
  data>=r[1] & data<=r[2]
}

getWithinQuantile<-function(data, q) {
  # This function filters a vector returning only those values who are within a 
  # q-quantile range. This function uses the 'withinQuantile' function.
  # 
  # Args:
  #   data: The vector for which the calcuation should be done
  #   q: The quantile range
  #
  # Returns:
  #   All values in 'data' except for those not within the q-quantile range.

  data[withinQuantile(data, q)]
}

wrapper <- function(x, ...) {
  # Word wraps a string using linebreaks.
  #
  # Args:
  #   x: The string which should be wrapped
  #   ...: Further arguments passed to 'strwrap'
  #
  # Returns:
  #   The string wordwrapped.

  return(paste(strwrap(x, ...), collapse = "\n"))
}


allButCols<-function(data, removeCols) {
  # Returns a subset (not a copy) of the provided 'data.table' removing the
  # supplied columns. The column must be provided as a list of strings.
  #
  # Args:
  #   data: A 'data.table' containing some data
  #   removeCols: A list of columns to remove
  #
  # Returns:
  #   A 'data.table' which includes all columns from 'data' except those
  #   specified by 'removeCols'. The returned object is not a copy but contains
  #   references for the columns.

  keepCols=setdiff(names(data),removeCols)
  data[, match(keepCols, names(data)), with=FALSE]
}


yapply <- function(X,FUN, ...) { 
  # Drop in replacement for 'lapply'. See its documentation for details on use.
  # This function adds two new parameters for the applied function which contain
  # the names of the values in case of named lists.
  # For further documentation see 'lapply'.

  index <- seq(length.out=length(X)) 
  namesX <- names(X) 
  if(is.null(namesX)) 
    namesX <- rep(NA,length(X))

  FUN <- match.fun(FUN) 
  fnames <- names(formals(FUN)) 
  if( ! "INDEX" %in% fnames ){ 
    formals(FUN) <- append( formals(FUN), alist(INDEX=) )   
  } 
  if( ! "NAMES" %in% fnames ){ 
    formals(FUN) <- append( formals(FUN), alist(NAMES=) )   
  } 
  mapply(FUN,X,INDEX=index, NAMES=namesX,MoreArgs=list(...), SIMPLIFY = FALSE)
}

summary.aov.ext <- function(a, xtable=TRUE) {
  # Extends the summary function of aov to include the percentage of interaction
  # which can be explained by dividing the Sum Sq byt the total Sum sq.
  # The output is automatically transformed to a xtable-tex-table. 
  #
  # Params:
  #   - a: aov class object
  #   - xtable: if false, the underlying data is returned
  #
  # Returns:
  #   If xtable is TRUE a tex containing the table is returned. Otherwise the
  #   raw data.

  if(xtable){
    library(xtable)
  }

  t=data.table(summary(a)[[1]])

  tsum = sum(t[,'Sum Sq', with=FALSE])
  percol = t[,'Sum Sq', with=FALSE]/tsum*100

  t = cbind(t[, 'Df', with=FALSE], t[,'Sum Sq', with=FALSE], data.table('rSum Sq'=percol),
            t[,'Mean Sq', with=FALSE], t[,'F value', with=FALSE],
            t[,'Pr(>F)', with=FALSE])
  rownames(t)=c(attr(a$terms, "term.labels"), "Residuals")
  setnames(t, 3, "rSum Sq")


  if(xtable) {
    suggested.digits <- c(0,rep(2,ncol(t)))
    suggested.digits[grep("Pr\\(>",names(t))+1] <- 4
    suggested.digits[grep("P\\(>",names(t))+1] <- 4
    suggested.digits[grep("Df",names(t))+1] <- 0

    xtable(t, digits=suggested.digits, align="|l|rrrrrr|")
  } else {
    t
  }
}

removeNAColumns<-function(dt) {
# removes columns in a data.table that contain only NAs
  return(dt[, which(dt[,colSums(is.na(dt))<nrow(dt)]), with = FALSE])
}

rSquare<-function(pred, obs) {
    #Calulate rSquare
    ssTot <- sum((obs-mean(obs))^2)
    ssErr <- sum((obs-pred)^2)
    rSquare <- 1-(ssErr/ssTot)

    return(rSquare)
}


