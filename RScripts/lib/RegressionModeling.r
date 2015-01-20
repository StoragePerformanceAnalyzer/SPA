# vim:tw=80:ts=2:sw=2:colorcolumn=81:nosmartindent
#
# Model Fitting functions
# --------------
# Author: Qais Noorshams <noorshams@kit.edu>, Dominik Bruhn <dominik@dbruhn.de>
#
# This file contains functions that can be used to generate regression models 

library(data.table)
library(caret)
library(forecast)

customSummary <- function (data, lev = NULL, model = NULL) {
    #Custom Summary Function for Cross-Validation

    #Calulate rSquare
    ssTot <- sum((data$obs-mean(data$obs))^2)
    ssErr <- sum((data$obs-data$pred)^2)
    rSquare <- 1-(ssErr/ssTot)

    #Calculate MSE
    mse <- mean((data$pred - data$obs)^2)

    #Aggregate
    out <- c(sqrt(mse), 1-(ssErr/ssTot))
    names(out) <- c("RMSE", "Rsquared")

    #Original Values from the original caret implementation
    #Do NOT return the same values because of the different implementation
    #of R^2, see the caret documentation.
    #old = postResample(data[, "pred"], data[, "obs"])
    #names(old) <- c("RMSE'", "Rsquared'")

  
    # Additionally, calculate MAE and MAPE
    stats <- accuracy(data[, "pred"], data[, "obs"])
    out <- c(out, stats[,'MAE'], stats[,'MAPE'])
    names(out) <- c("RMSE", "Rsquared", "MAE", "MAPE")
  
    out
}

fitModels<-function(formula, data, methods=list()) {
  # Fits several models using the formula and the data provided. The function
  # returns a named list containing multiple aspects of the models generated.
  #
  # This function can be used as a drop in replacement for simple 'lm' calls.
  #
  # In detail is does the following:
  #   - Partition the data into 10 training and test subsets. This is done by
  #   randomly removing the test samples from the input samples.
  #
  #   - Run some predefined regression techniques on the input data. These are
  #   Linear Regressions (One, Two, Three, Four and Five Dimensional), CART, MARS 
  #   and MARS with a dimensions up to five. During this model creation and new
  #   model is generated for each of the 10 subsets. For each of the subsets the
  #   left out test data is used to test the model and caluculate model metrics.
  #
  #   - If needed (if the methods parameter is of length >0) additional modeling
  #     techniques are run.
  #
  #   - The results of the tests of the modeling techniques are gather in the
  #     so-called resample dataset. This contains all the data which is later
  #     needed to compare the models.
  #
  ## -Currently broken after R updates-
  ##   - For each of the input data samples and each of the modeling techniques a
  ##     prediction is done. This helps to later compare the predicted and the
  ##     actual results.
  #
  # Args:
  #   formula: A formula describing what should be modeled.
  #   data: A 'data.table' containing the underlying data.
  #   methods: A list which contains additional modeling techniques which should
  #            be used.  See the 'caret' documentation for methods supported.
  #
  # Returns:
  #   A named list containing the following values:
  #     - LM: The caret-training results for training a linear model up to
  #           degree 5.
  #     - CART: The caret-training results for training a CART/rpart model.
  #     - MARS: The caret-training results for training a MARS/earth model of
  #             degree 1.
  #     - MARS_MULTI: The caret-training results for training a MARS/earth model
  #                   of a higher degree.
  #     - compare: A structure which can be used for comparing the training
  #                metrics of the models using either simply 'summary' for a
  #                textual representation of the comparison or using plot
  #                functions.
  #
  ## -Currently broken after R updates-
  ##     - predictions: A structure containing the predicted values for all
  ##                    observed values in all models. This structure can be used
  ##                    to plot observed and predicted values for example using
  ##                    the 'plotObsVsPred' function.

  #Calculate custom Formula containing degree 3 interactions
  deg2Formula=as.formula(paste(formula[2]," ~ (",formula[3],")^2", sep=""))
  deg3Formula=as.formula(paste(formula[2]," ~ (",formula[3],")^3", sep=""))
  deg4Formula=as.formula(paste(formula[2]," ~ (",formula[3],")^4", sep=""))
  deg5Formula=as.formula(paste(formula[2]," ~ (",formula[3],")^5", sep=""))

  cat("Creating Fold\n")
  fold=createFolds(seq(1, nrow(data)),10,returnTrain = TRUE)
  cat("Fold is:\n")
  cat(str(fold))

  result = list()

  #Choose sampling method and sumamry function
  cat("Creating Control\n")
  myControl <- trainControl(method='LGOCV', index=fold, summary=customSummary)

  #Train LM
  #Caret has a bug for linear models: the final models are invalid if inception
  #terms or factor are involved. As a workaround, the final model gets
  #calculated manually after carets terminiation.
  cat("Training LM")
  result$LM <- train(formula, data=data, method='lm', trControl=myControl)
  result$LM$finalModel = lm(formula, data=data)
  cat(".\n")

  cat("Training LM (interactions btw. 2 parameters)")
  result$LM_2PARAM_INTER <- train(deg2Formula, data=data, method='lm', trControl=myControl)
  result$LM_2PARAM_INTER$finalModel = lm(deg2Formula, data=data)
  cat(".\n")

  cat("Training LM (interactions btw. 3 parameters)")
  result$LM_3PARAM_INTER <- train(deg3Formula, data=data, method='lm', trControl=myControl)
  result$LM_3PARAM_INTER$finalModel = lm(deg3Formula, data=data)
  cat(".\n")

  cat("Training LM (interactions btw. 4 parameters)")
  result$LM_4PARAM_INTER <- train(deg4Formula, data=data, method='lm', trControl=myControl)
  result$LM_4PARAM_INTER$finalModel = lm(deg4Formula, data=data)
  cat(".\n")

  cat("Training LM (interactions btw. 5 parameters)")
  result$LM_5PARAM_INTER <- train(deg5Formula, data=data, method='lm', trControl=myControl)
  result$LM_5PARAM_INTER$finalModel = lm(deg5Formula, data=data)
  cat(".\n")


  cat("Training CART (cp= 0.01, minsplit=20)")
  result$CART <- train(formula, data=data, method='rpart', trControl=myControl,
                       tuneGrid=expand.grid(.cp=0.01),control=rpart.control(minsplit=20))
  cat(".\n")

  cat("Training CART (cp= 0.01, minsplit= 5)")
  result$CART_VAR1 <- train(formula, data=data, method='rpart', trControl=myControl,
                       tuneGrid=expand.grid(.cp=0.01),
                       control=rpart.control(minsplit=5))
  cat(".\n")

  cat("Training CART (cp=0.001, minsplit=20)")
  result$CART_VAR2 <- train(formula, data=data, method='rpart', trControl=myControl,
                       tuneGrid=expand.grid(.cp=0.001),
                       control=rpart.control(minsplit=20))
  cat(".\n")

  cat("Training CART (cp=0.001, minsplit= 5)")
  result$CART_VAR3 <- train(formula, data=data, method='rpart', trControl=myControl,
                       tuneGrid=expand.grid(.cp=0.001),
                       control=rpart.control(minsplit=5))
  cat(".\n")


  #Train MARS
  cat("Training MARS (nk=20, nprune=20, thresh= 0.001, degree= 1)")
  result$MARS <- train(formula, data=data, method='earth', trControl=myControl,
                       tuneGrid=expand.grid(.nprune=20, .degree=1),
                       nk=20, thresh=0.001)
  cat(".\n")

  cat("Training MARS (nk=20, nprune=20, thresh= 0.001, degree=<5)")
  result$MARS_MULTI <- train(formula, data=data, method='earth',
                             trControl=myControl,
                             tuneGrid = expand.grid(.nprune=20, .degree=5),
                             nk=20, thresh=0.001)
  cat(".\n")

  cat("Training MARS (nk=40, nprune=40, thresh= 0.001, degree=<5)")
  result$MARS_VAR1 <- train(formula, data=data, method='earth',
                             trControl=myControl,
                             tuneGrid = expand.grid(.nprune=40, .degree=5),
                             nk=40, thresh=0.001)
  cat(".\n")

  cat("Training MARS (nk=20, nprune=20, thresh=0.0001, degree=<5)")
  result$MARS_VAR2 <- train(formula, data=data, method='earth',
                             trControl=myControl,
                             tuneGrid = expand.grid(.nprune=20, .degree=5),
                             nk=20, thresh=0.0001)
  cat(".\n")

  cat("Training MARS (nk=40, nprune=40, thresh=0.0001, degree=<5)")
  result$MARS_VAR3 <- train(formula, data=data, method='earth',
                             trControl=myControl,
                             tuneGrid = expand.grid(.nprune=40, .degree=5),
                             nk=40, thresh=0.0001)
  cat(".\n")



  cat("Training M5")
  result$M5 <- train(formula, data=data, method='cubist',
                        trControl=myControl,
                        tuneGrid = data.frame(.committees=1, .neighbors=0))
  cat(".\n")
 
  result$models <- list(lm=result$LM,
                        lm_2param_inter=result$LM_2PARAM_INTER,
                        lm_3param_inter=result$LM_3PARAM_INTER,
                        lm_4param_inter=result$LM_4PARAM_INTER,
                        lm_5param_inter=result$LM_5PARAM_INTER,
                        
                        cart=result$CART, cart_var1=result$CART_VAR1,
                        cart_var2=result$CART_VAR2, cart_var3=result$CART_VAR3,

                        mars=result$MARS, mars_multi=result$MARS_MULTI,
                        mars_var1=result$MARS_VAR1, mars_var2=result$MARS_VAR2,
                        mars_var3=result$MARS_VAR3,
                        
                        m5 = result$M5) 


  if(length(methods)>0) {
    cat("Training additional methods:\n")
    lapply(methods, function(method) {
      cat(paste("  ", method))

      if(method=="cubist") {
        cubistGrid = expand.grid(.committees = c( 1, 2, 3, 4, 5, 10, 15, 20),
                                   .neighbors = c(0,1,2,3,4,5))
        result$CUBIST <<- train(formula, data=data, method='cubist',
                                 trControl=myControl, tuneGrid=cubistGrid)
        result$models[['cubist']] <<- result$CUBIST
     } else {
        result[[method]] <<- train(formula, data=data, method=method,
                              trControl=myControl)
        result$models[[method]] <<- result[[method]]
      }
      cat(".\n")
    })
  }


  cat("Calculating resamples")
  result$compare <- resamples(result$models)
  cat(".\n")

  #cat("Calculating Predictions")
  ##Another CARET Bug: The trainingData set is calculated incorrectly if R
  ##factors are present. To fix this we set the trainingData ourself.

  #nl=lapply(result$models, function(m) {
  #  m$trainingData = data
  #  m
  #})
  #result$models = nl
  #
  ##result$predictions = extractPrediction(result$models)
  ##names(result$predictions) <- c("obs", "pred", "modelType", "dataType", "model")
  #cat(".\n")

  return(result)
}

fitCertainModel<-function(formula, data, method, parameter = NULL, fold = NULL) {
  # Fits a certain model using the formula and the data provided using the 
  # method with the parameters specified or with default parameters if not 
  # specified. This method is a facade for the specific regression techniques 
  # and is able to directly process the return value of 
  # RegressionOptimization::optimizeTechniques()
  #
  # Args:
  #   formula: A formula describing what should be modelled.
  #   data: A 'data.table' containing the underlying data.
  #   method: The regression technique that should be used. Currently "lm", 
  #           "earth", "rpart", "m5", or "cubist"
  #   parameter: A 'data.table' for the parameter of the regression technique. 
  #              Currently supported parameters are (see help of the technique 
  #              for details, e.g., '?earth'):
  #              earth - nk, nprune, thresh, degree
  #              rpart - cp, minsplit
  #              m5 - rules
  #              cubist - rules, committees, neighbors
  #
  # Returns:
  #   A trained object for the method. The model is in result$finalModel.
  #
  if(is.null(fold)) {
    fold=createFolds(seq(1, nrow(data)),10,returnTrain = TRUE)
  }
  myControl <- trainControl(method='LGOCV', index=fold, summary=customSummary)
  
  if (method=='lm') {
    result <- train(formula, data=data, method=method)
    
  } else if (method=='rpart') {
    library(rpart)
    
    if(is.null(parameter)){
      parameter = data.table(cp=0.01,minsplit=20); 
    }
    
    tuneGrid=expand.grid(.cp=parameter[1,cp]);
    control=rpart.control(minsplit=parameter[1,minsplit]);
    result = train(formula, data=data, method=method, 
                   trControl=myControl,
                   tuneGrid=tuneGrid,
                   control=control
                   );
  } else if (method=='earth') {
    if(is.null(parameter)){
      parameter = data.table(nprune=21,nk=21,degree=1,thresh=0.001); 
    }
    
    tuneGrid=expand.grid(.nprune=parameter[1,nprune], .degree=parameter[1,degree]);
    result = train(formula, data=data, method=method, 
          trControl=myControl,
          tuneGrid=tuneGrid,
          nk=parameter[1,nk], thresh=parameter[1,thresh]
          );
  } else if (method=='m5') {
    library(Cubist)
    if(is.null(parameter)){
      parameter = data.table(rules=100); 
    }
    
    tuneGrid = data.frame(.committees=1, 
                          .neighbors=0);
    control=cubistControl(rules=parameter[1,rules]);
    result = train(formula, data=data, method='cubist', 
                   trControl=myControl,
                   tuneGrid = tuneGrid,
                   control=control
                   );
  } else if (method=='cubist') {
    library(Cubist)
    if(is.null(parameter)){
      parameter = data.table(committees=1,neighbors=0,rules=100); 
    }
    
    tuneGrid = data.frame(.committees=parameter[1,committees], 
                          .neighbors=parameter[1,neighbors]);
    control=cubistControl(rules=parameter[1,rules]);
    result = train(formula, data=data, method=method, 
          trControl=myControl,
          tuneGrid = tuneGrid,
          control=control
          );
  } else {
    stop("Supported methods: rpart, earth, m5, cubist.\n");
  }
  
  return(result)
  
}

plotCART<-function(fit) {
  # Prints a CART-Model which was generated using the 'rpart' command.
  #
  # Args:
  #   fit: The rpart-model which should be plotted
  #
  # Returns:
  #     - nothing

  library(party)
  library(partykit)
  library(rpart)
  plot(as.party(fit))
}
