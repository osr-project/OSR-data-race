import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetection.hb.HBOfflineEngine;

public class HBOffline {

	public HBOffline() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		HBOfflineEngine engine = new HBOfflineEngine(options.parserType, options.path, options.forceOrdering, options.tickClockOnAccess, options.verbosity);
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		engine.analyzeTrace(options.multipleRace);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}	
	}
}
