import cmd.CmdOptions;
import cmd.GetOptions;
import debug.EventStatistics;
import debug.TreeClockStatistics;
import engine.racedetection.hb_treeclock.monitor.HBEngine;

public class HBTreeClockCounting {

	public HBTreeClockCounting() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		EventStatistics.enable();
		TreeClockStatistics.enable();
		
		HBEngine engine = new HBEngine(options.parserType, options.path, options.forceOrdering, options.tickClockOnAccess, options.verbosity);
		engine.analyzeTrace(options.multipleRace);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}
		
		TreeClockStatistics.print();
	}
}
