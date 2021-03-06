package org.hobbit.benchmark.gerbil;

public class StressTestPhaseResult {

    public final long durationSum;
    public final double f1ScoreSum;
    public final double beta;
    public final int errors;
    public final double avgMillisPerDoc;
    
    public StressTestPhaseResult(long duration, double f1ScoreSum, double beta, int errors, double avgMillisPerDoc) {
        this.durationSum = duration;
        this.f1ScoreSum = f1ScoreSum;
        this.beta = beta;
        this.errors = errors;
        this.avgMillisPerDoc = avgMillisPerDoc;
    }
}
