import java.util.HashMap;
import java.util.LinkedList;

import cmd.CmdOptions;
import cmd.GetOptions;
import engine.annotation.write_block_boundary.WriteBlockBoundaryEngine;

public class WriteBlockBoundary {

	public WriteBlockBoundary() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = false;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		WriteBlockBoundaryEngine engine = new WriteBlockBoundaryEngine(options.parserType, options.path);
		engine.analyzeTrace(true);
		
		HashMap<String, LinkedList<Long>> boundaries = engine.getBoundaries();
		HashMap<String, LinkedList<Boolean>> threadLocality = engine.getThreadLocality();

		System.out.println("Boundaries: " + boundaries.toString());
		System.out.println("ThreadLocalities: " + threadLocality.toString());
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
