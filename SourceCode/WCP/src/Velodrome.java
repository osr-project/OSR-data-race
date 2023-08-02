import cmd.CmdOptions;
import cmd.GetOptions;
import debug.CacheStatistics;
import debug.EventStatistics;
import engine.atomicity.conflictserializability.velodome.VelodromeEngine;

public class Velodrome {

	public Velodrome() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		VelodromeEngine engine = new VelodromeEngine(options.parserType, options.path, options.verbosity);

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		engine.analyzeTrace(false);
		
		System.out.println("Number of transactions remaining = " + engine.numTransactionsActive());
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
		
		if(EventStatistics.isEnabled()) {
			EventStatistics.print();
			CacheStatistics.print();
		}
	}
}
