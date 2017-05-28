package org.hobbit.benchmark.gerbil;

public class StressTestDocumentResult implements Comparable<StressTestDocumentResult> {

    public final long startTime;
    public final long endTime;
    public final long duration;
    public final boolean error;
    public final double f1;
    
    public StressTestDocumentResult(long startTime, long endTime, boolean error, double f1) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.duration = endTime - startTime;
        this.error = error;
        this.f1 = f1;
    }

    @Override
    public int compareTo(StressTestDocumentResult o) {
        return Long.compare(startTime, o.startTime);
    }

}
