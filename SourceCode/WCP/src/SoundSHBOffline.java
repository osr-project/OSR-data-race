import cmd.CmdOptions;
import cmd.GetOptions;
import engine.racedetection.soundshb.SoundSHBOfflineEngine;

public class SoundSHBOffline {

	public SoundSHBOffline() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		SoundSHBOfflineEngine engine = new SoundSHBOfflineEngine(options.parserType, options.path, options.verbosity);
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
