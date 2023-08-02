import cmd.CmdOptions;
import cmd.GetOptions;
import debug.CacheStatistics;
import debug.EventStatistics;
import engine.racedetection.wcp.no_race.WCPEngine;

public class WCPNoRace {

	public WCPNoRace() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		EventStatistics.enable();
		
		WCPEngine engine = new WCPEngine(options.parserType, options.path, options.forceOrdering, options.tickClockOnAccess, options.verbosity );
		engine.analyzeTrace(options.multipleRace);

		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}	
		
		if (EventStatistics.isEnabled()) {
			EventStatistics.print();
			CacheStatistics.print();
		}

	}
}
