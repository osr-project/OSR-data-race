import cmd.CmdOptions;
import cmd.GetOptions;
import engine.atomicity.incorrect_conflictserializability.vc.VCConflictSerializabilityEngine;

public class ConflictSerializabilityChecker {

	public ConflictSerializabilityChecker() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		VCConflictSerializabilityEngine engine = new VCConflictSerializabilityEngine(options.parserType, options.path, options.verbosity);
		engine.analyzeTrace(options.multipleRace);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}	
	}
}
