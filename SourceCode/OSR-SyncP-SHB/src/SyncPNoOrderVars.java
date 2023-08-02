import engine.racedetectionengine.SyncPNoOrderVars.SyncPreservingRaceOfflineEngine;
import parse.ParserType;

public class SyncPNoOrderVars {
    public static void main(String[] args) {
//		CmdOptions options = new GetOptions(args).parse();
        String trace_folder = "C:\\Research\\traces\\SP-NoEmptyThread-Filter\\twostage.std";
//		String trace_folder = "D:\\Research\\traces\\sync-preserving-logs\\ftpserver.std";

//		String trace_folder = args[0];
        analysis(trace_folder);
    }

    public static void analysis(String trace_folder){
        boolean time_reporting = true;
        long startTimeAnalysis = 0;
        if (time_reporting) {
            startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
        }

        SyncPreservingRaceOfflineEngine engine = new SyncPreservingRaceOfflineEngine(
                ParserType.STD, trace_folder);
        engine.analyzeTrace(true, 1);

        if (time_reporting) {
            long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
            long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
            double timeInSeconds = timeAnalysis * 1.0 / 1000;
            double timeInMin = timeAnalysis * 1.0 / 60000;
            System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
            System.out.println("Time for analysis in seconds = " + String.format("%.3f", timeInSeconds));
            System.out.println("Time for analysis in mins = " + String.format("%.1f", timeInMin));
            System.out.println("Number of racy events = " + engine.state.racyEvents.size());
            System.out.println("Number of racy locations = " + engine.state.racyLocations.size());
        }
        System.out.println(trace_folder + "          " + "SyncP");
        System.out.println("--------------------------------------------------");
    }
}
