import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetection.hb_epoch.HBEpochOfflineEngine;

public class HBEpochOffline {

	public HBEpochOffline() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		if(options.multipleRace){
			throw new IllegalArgumentException("The HB Epoch engine is supposed to run only until the first race is discovered.");
		}
		HBEpochOfflineEngine engine = new HBEpochOfflineEngine(options.parserType, options.path, options.verbosity);
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
