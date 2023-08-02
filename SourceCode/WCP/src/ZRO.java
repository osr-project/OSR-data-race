import cmd.CmdOptions;
import cmd.GetOptions;
import debug.CacheStatistics;
import debug.ZROEventStatistics;
import engine.racedetection.zeroreversal.distance.ZROfflineEngine;

public class ZRO {

	public ZRO() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		ZROfflineEngine engine = new ZROfflineEngine(options.parserType, options.path, options.verbosity);
		engine.analyzeTrace(options.multipleRace);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}
		
		if(ZROEventStatistics.isEnabled()) {
			ZROEventStatistics.print();
			CacheStatistics.print();
		}
	}
}
