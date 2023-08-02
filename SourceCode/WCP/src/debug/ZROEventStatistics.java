package debug;

import event.EventType;
/**
 * Created by matt on 4/17/18.
 */
public class ZROEventStatistics extends EventStatistics {
    private static long readFPTime = 0;
    private static long writeFPTime = 0;
    
    private static long numReadFPIters = 0;
    private static long numWriteFPIters = 0;
    
    private static int numReadFP = 0;
    private static int numWriteFP = 0;

    public static void updateReadFPTime(long beginTime, long numIters) {
        ZROEventStatistics.numReadFP++;
        ZROEventStatistics.readFPTime = ZROEventStatistics.readFPTime + beginTime;
        ZROEventStatistics.numReadFPIters = ZROEventStatistics.numReadFPIters + numIters;
    }
    
    public static void updateWriteFPTime(long beginTime, long numIters) {
        ZROEventStatistics.numWriteFP++;
        ZROEventStatistics.writeFPTime = ZROEventStatistics.writeFPTime + beginTime;
        ZROEventStatistics.numWriteFPIters = ZROEventStatistics.numWriteFPIters + numIters;
    }
    
    public static void updateFPTime(EventType type, long time, long numIters) {

        switch (type) {
            case READ:
                updateReadFPTime(time, numIters);
                break;
            case WRITE:
            	updateWriteFPTime(time, numIters);
                break;
            default:
        }
    }
    
    public static void print() {
    	EventStatistics.print();
    	
        System.out.println("---------------FP Statistics---------------");
        
        System.out.println("Read FP Time: " + readFPTime + ", Avg = "
                + (readFPTime != 0 ? formatter.format((readFPTime / (numReadFP * 1.0))) : 0)
                + ", Num = " + numReadFP);
        
        System.out.println("Read FP Iters: " + numReadFPIters + ", Avg = "
                + (numReadFPIters != 0 ? formatter.format((numReadFPIters / (numReadFP * 1.0))) : 0)
                + ", Num = " + numReadFP);
        
        System.out.println("Write FP Time: " + writeFPTime + ", Avg = "
                + (writeFPTime != 0 ? formatter.format((writeFPTime / (numWriteFP * 1.0))) : 0)
                + ", Num = " + numWriteFP);
        
        System.out.println("Write FP Iters: " + numWriteFPIters + ", Avg = "
                + (numWriteFPIters != 0 ? formatter.format((numWriteFPIters / (numWriteFP * 1.0))) : 0)
                + ", Num = " + numWriteFP);
    }
}
