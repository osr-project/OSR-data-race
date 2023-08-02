package debug;
/**
 * Created by umang on 9/1/20.
 */
public class TreeClockStatistics {
    protected static boolean enabled = false;

    private static long numJoins = 0;
    private static long numJoinsSkipped = 0;
    private static long opsInJoins = 0;
    private static long numMonotoneCopy = 0;
    private static long opsInMonotoneCopy = 0;

    public static long getNumJoins() {
        return numJoins;
    }
    
    public static void incrementNumJoinsBy(int k) {
    	numJoins = numJoins + k;
    }
    
    public static long getNumJoinsSkipped() {
        return numJoinsSkipped;
    }
    
    public static void incrementNumJoinsSkippedBy(int k) {
    	numJoinsSkipped = numJoinsSkipped + k;
    }
    
    public static long getOpsInJoins() {
        return opsInJoins;
    }
    
    public static void incrementOpsInJoinsBy(int k) {
    	opsInJoins = opsInJoins + k;
    }
    
    public static long getNumMonotoneCopy() {
        return numMonotoneCopy;
    }
    
    public static void incrementNumMonotoneCopyBy(int k) {
    	numMonotoneCopy = numMonotoneCopy + k;
    }
    
    public static long getOpsInMonotoneCopy() {
        return opsInMonotoneCopy;
    }
    
    public static void incrementOpsInMonotoneCopyBy(int k) {
    	opsInMonotoneCopy = opsInMonotoneCopy + k;
    }
    
   
    public static boolean isEnabled() {
        return TreeClockStatistics.enabled;
    }

    public static void enable() {
        TreeClockStatistics.enabled = true;
    }
    
    public static void print() {
    	EventStatistics.print();
    	
        System.out.println("---------------Tree-Clock Statistics---------------");
        
        System.out.println("NumJoins = " + numJoins);
        System.out.println("NumJoinsSkipped = " + numJoinsSkipped);
        System.out.println("OpsInJoins = " + opsInJoins);
        System.out.println("NumMonotoneCopy = " + numMonotoneCopy);
        System.out.println("OpsInMonotoneCopy = " + opsInMonotoneCopy);
        
        System.out.println("---------------------------------------------------");
    }
}
