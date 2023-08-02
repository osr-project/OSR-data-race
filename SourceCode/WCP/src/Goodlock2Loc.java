import cmd.CmdOptions;
import cmd.GetOptions;
import engine.deadlock.goodlock2_locations.Goodlock2Engine;

public class Goodlock2Loc {

	public Goodlock2Loc() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		Goodlock2Engine engine = new Goodlock2Engine(options.parserType, options.path, options.verbosity);
		engine.analyzeTrace(options.multipleRace);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
