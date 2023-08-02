

import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetection.onlinewcp.OnlineWCPEngine;

public class WCPOnline {

	public WCPOnline() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		OnlineWCPEngine engine = new OnlineWCPEngine(options.parserType, options.path, options.forceOrdering, options.tickClockOnAccess, options.verbosity);
		engine.analyzeTrace(options.multipleRace);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
