import engine.racedetectionengine.spPerVar.SyncPreservingRaceOfflineEngine;
import parse.ParserType;

public class SPPerVar {
    public static void main(String[] args) {
//		CmdOptions options = new GetOptions(args).parse();
		String trace_folder = "C:\\Research\\traces\\SP-NoEmptyThread-Filter\\moldyn.std";
//        String trace_folder = "C:\\Research\\traces\\sync-preserving-logs\\bubblesort.std";
//        String trace_folder = "D:\\Research\\traces\\sync-preserving-logs\\lang.std";

//		String trace_folder = args[0];
        analysis(trace_folder);
    }

    public static void analysis(String trace_folder){
        long startTimeAnalysis = System.currentTimeMillis();

//        OrderedVarsEngine orderedVarsEngine = new OrderedVarsEngine(ParserType.STD, trace_folder, 0);
//        orderedVarsEngine.analyzeTrace(true, 0);
//
//        HashMap<String, HashSet<String>> lockToThreadSet = orderedVarsEngine.getLockToThreadSet();
//        HashMap<String, HashSet<String>> stringVariableToThreadSet = orderedVarsEngine.getVariableToThreadSet();
//        HashSet<String> orderedVariables = orderedVarsEngine.getOrdredVars();

//        Thread.threadCountTracker = 0;
//        Lock.lockCountTracker = 0;
//        Variable.variableCountTracker = 0;
//        Event.eventCountTracker = 0L;

        SyncPreservingRaceOfflineEngine engine = new SyncPreservingRaceOfflineEngine(ParserType.STD, trace_folder, startTimeAnalysis);
        engine.analyzeTrace(true, 1);
        long endTime = System.currentTimeMillis();
        double timeInMins = (endTime - startTimeAnalysis) * 1.0 / 60000;
        System.out.println("Finished: " + trace_folder + "  " + timeInMins + " mins,  racy vars: " + engine.state.racyVars.size());
//        System.out.println("Read Time : " + (engine.state.readTime / 1000));
//        System.out.println("Write Time : " + (engine.state.writeTime / 1000));
//        printAfterAnalysis(trace_folder, startTimeAnalysis, engine);
    }

    public static void printAfterAnalysis(String trace_folder, long startTime, SyncPreservingRaceOfflineEngine engine){
        long stopTimeAnalysis = System.currentTimeMillis();
        long timeAnalysis = stopTimeAnalysis - startTime;
        double timeInMins = timeAnalysis * 1.0 / 60000;
        double timeInSeconds = timeAnalysis * 1.0 / 1000;
        System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
        System.out.println("Time for analysis in seconds = " + String.format("%.3f", timeInSeconds));
        System.out.println("Time for analysis in mins = " + String.format("%.1f", timeInMins));

        System.out.println("Number of racy vars are " + engine.state.racyVars.size());
        System.out.println(trace_folder + "          " + "SyncP Per Var");
        System.out.println("--------------------------------------------------");
    }
}
