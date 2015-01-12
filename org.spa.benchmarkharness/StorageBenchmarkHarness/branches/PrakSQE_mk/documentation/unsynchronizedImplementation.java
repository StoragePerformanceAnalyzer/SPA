import java.util.List;
import java.util.concurrent.CountDownLatch;

import com.google.common.collect.Lists;

import edu.kit.sdq.storagebenchmarkharness.ExperimentSeriesHelper.BenchmarkDriverAndIndependentVars;
import edu.kit.sdq.storagebenchmarkharness.SBHModel.DependentVariables;

LOGGER.debug("Starting unsynchronized mode");
        datastore.storeConfigurationRun(identifier, false);

        // Create Threads
        List<UnsynchronizedBenchmarkRunner> threads = Lists.newArrayList();
        final CountDownLatch latch = new CountDownLatch(sutCount);

        for (Entry<String, List<BenchmarkDriverAndIndependentVars>> experiments: experimentsForSut.entrySet()) {
            UnsynchronizedBenchmarkRunner thread = new UnsynchronizedBenchmarkRunner(experiments.getValue(),
                    experiments.getKey(), latch);
            thread.setName("ExperimentsOn" + experiments.getKey());
            threads.add(thread);
        }

        // Start Threads
        LOGGER.debug("Running Thread:");
        LOGGER.newBlock();
        for (Thread t: threads) {
            LOGGER.debug("%s", t.getName());
            t.start();
        }
        LOGGER.leaveBlock();

        // Wait for all Threads finish
        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted the waiting for finish", e);
            throw new BenchmarkException(e);
        }

        LOGGER.debug("All Threads Finished");

        // Check if all threads successsfull
        for (UnsynchronizedBenchmarkRunner t: threads) {
            if (!t.success) {
                LOGGER.error("A Runnerthread throwed a exception");
                throw new BenchmarkException("A BenchmarkThread failed");
            }
        }

        // Save Results
        LOGGER.debug("Saving Results:");
        LOGGER.newBlock();
        for (UnsynchronizedBenchmarkRunner t: threads) {
            LOGGER.debug("SUT %s:", t.sutId);
            LOGGER.newBlock();
            for (int expNo = 0; expNo < t.dependentVars.size(); expNo++) {
                BenchmarkDriverAndIndependentVars exp = t.benchAndExps.get(expNo);
                DependentVariables[] depVarsForSut = t.dependentVars.get(expNo);

                datastore.storeExperimentResults(expNo, t.sutId, exp.getBenchmarkDriver().getClass().getSimpleName(),
                        exp.getRunNo(), exp.getExpUid(), exp.getSutVars(), exp.getBenchVars(), depVarsForSut);
                LOGGER.debug("    Exp#%d: %s", expNo, depVarsForSut.length);
            }
            LOGGER.leaveBlock();
        }
        LOGGER.leaveBlock();

        datastore.finishConfigurationRun();
        
        

        /**
         * A Thread that runs all experiments of one host one after another. This
         * thread is used for the unsychronized execution of the benchmarks.
         * 
         * @author dominik
         * 
         */
        private final static class UnsynchronizedBenchmarkRunner extends Thread {
            private final List<BenchmarkDriverAndIndependentVars> benchAndExps;
            private final CountDownLatch latch;
            private final String sutId;
            private final List<DependentVariables[]> dependentVars;

            private boolean success = false;

            /**
             * Construct a new thread for unsynchronized execution.
             * 
             * @param benchAndExps
             *            A list of all experiments for one host
             * @param sutId
             *            A identifier for the host where the experiments should be
             *            run
             * @param latch
             *            A countdown latch which is decremented when all experiment
             *            are finished and uppon failure.
             */
            public UnsynchronizedBenchmarkRunner(List<BenchmarkDriverAndIndependentVars> benchAndExps, String sutId,
                    CountDownLatch latch) {
                super();
                this.benchAndExps = benchAndExps;
                this.sutId = sutId;
                this.latch = latch;
                dependentVars = Lists.newArrayList();
            }

            @Override
            public void run() {
                try {
                    for (BenchmarkDriverAndIndependentVars exp: benchAndExps) {
                        exp.getBenchmarkDriver().prepareExperimentSuper(exp.getExpUid(), exp.getSutVars(),
                                exp.getBenchVars());
                        dependentVars.add(exp.getBenchmarkDriver().startExperiment());
                        exp.getBenchmarkDriver().endExperiment();
                    }

                    success = true;
                } catch (Exception e) {
                    LOGGER.error("Caught Benchmark-Exception", e);
                    success = false;
                } finally {
                    latch.countDown();
                }
            }
        }
