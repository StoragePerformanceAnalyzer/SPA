# vim:tw=80:ts=2:sw=2:colorcolumn=81:nosmartindent
#
# Qais Magic Optimization Algorithm
# --------------
# Author: Dominik Bruhn <dominik@dbruhn.de>
#
# Algorithm described in: 
# Qais Noorshams, Dominik Bruhn, Samuel Kounev, and Ralf Reussner.
# Predictive Performance Modeling of Virtualized Storage Systems using 
# Optimized Statistical Regression Techniques. In Proceedings of the 
# ACM/SPEC International Conference on Performance Engineering, Prague, 
# Czech Republic, ICPE 13, pages 283-294, New York, NY, USA. ACM. 2013.

#Here-Script which finds out where this file resides
frame_files <- lapply(sys.frames(), function(x) x$ofile)
frame_files <- Filter(Negate(is.null), frame_files)
PATH_SPALIB <- dirname(frame_files[[length(frame_files)]])
rm(frame_files)

# Uses custom summary defined in RegressionModeling.r
source(paste(PATH_SPALIB, "RegressionModeling.r", sep="/"))


# Default values for techniques
defaultRangesRPart = list(
  cp=c(0, 1),
  minsplit=c(2, 100)
)
defaultRangesEarth = list(
  thresh=c(0, 1),
  nprune=c(2,200),
  degree=c(1,2),
  nk=c(3, 200)
)
defaultRangesM5 = list(
  rules=c(2, 100)
)
defaultRangesCubist = list(
  rules=c(2, 100),
  neighbors=c(0, 9),
  committees=c(1, 100)
)


optimizeTechniques<-function(method, formula, data, ranges=NA, nSplits, nExplorations,
                   trace=0, fold=NA, nIterations=15) {
  # This method optimizes the parameters 
  

  d<-function(x) {
    if(trace>1) {
      print(x)
    }
  }

  #Parameter Handling
  if(length(ranges)==1 && is.na(ranges)) {
    d("Loading Default Ranges")
    if(method=='rpart') {
      ranges = defaultRangesRPart
    } else if(method=='earth') {
      ranges = defaultRangesEarth
    } else if (method=='m5') {
      ranges = defaultRangesM5
    } else if (method=='cubist') {
      ranges = defaultRangesCubist
    }
  }

  stopifnot(nSplits>0)
  stopifnot(nExplorations>0)
  stopifnot(nIterations>0)
  
  #Create fold
  if(length(fold)==1 && is.na(fold)) {
    fold = createFolds(seq(1, nrow(data)),10,returnTrain = TRUE)
  }
  trControl = trainControl(method='LGOCV', index=fold, summary=customSummary)

  d("Fold is")
  d(str(fold))

  #Add one sample to E
  E1_Params = data.table(expand.grid(ranges))[1,]
  d("E1_Params is")
  d(E1_Params)

  E1_Value = evaluate(method, formula, data, trControl, E1_Params)
  d("E1_Value is")
  d(E1_Value)

  E = cbind(E1_Params, quality=E1_Value, iterationNo=0)
  setkeyv(E, setdiff(names(E), c("quality", "iterationNo")))
  M = E

  d("Before first iteration:")
  d("E")
  d(E)
  d("M")
  d(M)
  
  iterNo = 1
  repeat {
    Ej = E[0] #Ej is empty 

    #Iterate over M
    by(M, 1:nrow(M), function(Mrow) {
      d("Inspecting row")
      d(Mrow)

      x = Mrow[,1:(length(Mrow)-2), with=FALSE] #all except last 2 column 2]
      d("x is")
      d(x)
      #y = Mrow[length(Mrow)] #last column]

      #For each parameter find lower and upper limits
      S = yapply(x, function(xv) {
        d(paste("Inspecting parameter",NAMES))
        range = ranges[[NAMES]]
        d("Ranges:")
        d(range)
        extValues = E[[NAMES]]
        d("Extisting Values:")
        d(extValues)
        d(paste("Current Value:", xv))
        
        d(paste("lower check=",extValues[extValues<xv]))
        lowerBorder = max(extValues[extValues<xv], range[1])
        upperBorder = min(extValues[extValues>xv], range[2])
        d(paste("lower=",lowerBorder,"upper=",upperBorder))



        return(makeValueValid(method, NAMES,
                    unique(seq(lowerBorder, upperBorder, length=nSplits+2))
        ))
      })
      d("Calculated new values")
      d(S)

      #Expand new Values
      Sexp = data.table(expand.grid(S))
      d("Calculated expanded values")
      d(Sexp)

      #Evaluate these values
      by(Sexp, 1:nrow(Sexp), function(SexpRow) {
        d("Evaluating")
        d(SexpRow)

        #Check if already exists in E
        if(is.na(E[SexpRow]$quality) &
           is.na(Ej[SexpRow]$quality)) {
          d("Row is not in E")
          
          res = evaluate(method, formula, data, trControl, SexpRow)
          d(paste("Results is ", res))
          
          Ej <<- rbind(Ej, cbind(SexpRow, quality=res, iterationNo=iterNo))

          #Need to update the key because rbind destroys it
          setkeyv(Ej, setdiff(names(Ej), c("quality", "iterationNo")))
        }
      })
   })

    d("Ej is now")
    d(Ej)

    d("After each M was evaluated:")
    E = rbind(E, Ej)
    setkeyv(E, setdiff(names(E), c("quality", "iterationNo")))
    d(E)

    #Get top quality parameters
    thisNExplorations = nExplorations
    M = E[order(quality),][1:min(thisNExplorations,nrow(E)) ]
    d("M is now")
    d(M)
 
    #Stop Condition
    if(trace>=1) {
      cat(paste("Iteration ",iterNo,"/",nIterations,
                ": #E=",nrow(E),", #Ej=",nrow(Ej)," Best:\n",
                  sep=""))
      print(M[1,])
    }

    if(iterNo>=nIterations) {
      break
    }
    iterNo = iterNo+1
  }

  return(E[order(quality), ])
}

# Evaluate for a given thechnique the model with the given parameters
evaluate<-function(method, formula, data, trainControl, params) {
  #Actual train model
  if(method=='rpart') {
    trainRPart(formula, data, trainControl, 
               params$minsplit, params$cp)
  } else if(method=='earth') {
    trainEarth(formula, data, trainControl, 
               params$nprune, params$nk, params$thresh, params$degree)
  } else if(method=='m5') {
    trainM5(formula, data, trainControl, params$rules)
  } else if(method=='cubist') {
    trainCubist(formula, data, trainControl, params$rules, params$committees, params$neighbors)
  } else {
    stop("Technique unknown: ", method)
  }
}

# Create CV of Method and return mean RMSE
trainRPart<-function(formula, data, trainControl, minsplit, cp) {
  stopifnot(!is.null(minsplit))
  stopifnot(!is.null(cp))
  stopifnot(!is.na(minsplit))
  stopifnot(!is.na(cp))

  m=train(formula, data, method='rpart', tuneGrid=data.frame(.cp=cp),
        control=rpart.control(minsplit=minsplit), 
        trControl=trainControl)

  #Return mean RMSE
  return(m$result$RMSE)
}

trainEarth<-function(formula, data, trainControl, nprune, nk, thresh, degree) {
  stopifnot(!is.null(nprune))
  stopifnot(!is.null(nk))
  stopifnot(!is.null(thresh))
  stopifnot(!is.null(degree))
  stopifnot(!is.na(nprune))
  stopifnot(!is.na(nk))
  stopifnot(!is.na(thresh))
  stopifnot(!is.na(degree))  
  
  m=train(formula, data, method='earth', tuneGrid=data.frame(.nprune=nprune,.degree=degree),
          nk=nk, thresh=thresh,
          trControl=trainControl)
  
  return(m$result$RMSE)
}

trainM5<-function(formula, data, trainControl, rules) {
  return(trainCubist(formula, data, trainControl, rules, 1, 0))
}

trainCubist<-function(formula, data, trainControl, rules, committees, neighbors) {
  stopifnot(!is.null(rules))
  stopifnot(!is.na(rules))
  
  m=train(formula, data, method='cubist',
	  tuneGrid=data.frame(.committees=committees, .neighbors=neighbors),
          control=cubistControl(rules=rules), trControl=trainControl)
  
  return(m$result$RMSE)
}

# Enforce for each parameter of a method that the value is valid
makeValueValid<-function(method, param, value) {
  if(method=='rpart') {
    if(param=='minsplit') {
      return(pmax(2,round(value)))
    } else if (param=='cp') {
      return(pmax(0,pmin(0.99999,value)))
    }
  } else if(method=='earth') {
    if(param=='nk') {
      return(pmax(2,round(value)))
    } else if(param=='nprune') {
      return(pmax(2,round(value)))
    } else if(param=='thresh') {
      return(pmax(0,pmin(0.99999,value)))
    } else if(param=='degree') {
      return(pmax(1,round(value)))
    }
  } else if(method=='m5') {
    if(param=='rules') {
      return(pmax(2,round(value)))
    }
  } else if(method=='cubist') {
    if(param=='rules') {
      return(pmax(2,round(value)))
    } else if(param=='committees') {
      return(pmax(1,round(value)))
    } else if(param=='neighbors') {
      return(pmax(0,pmin(9,round(value))))
    }
  }
  return(value)
}
