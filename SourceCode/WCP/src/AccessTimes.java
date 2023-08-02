import java.util.HashMap;

import cmd.CmdOptions;
import cmd.GetOptions;
import engine.accesstimes.RefinedAccessTimesEngine;

public class AccessTimes {

	public AccessTimes() {

	}
	
	public static void main(String[] args) {		
		CmdOptions options = new GetOptions(args).parse();
		
		boolean time_reporting = false;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
		
		RefinedAccessTimesEngine engine = new RefinedAccessTimesEngine(options.parserType, options.path);
		engine.computeLastAccessTimes();
		HashMap<String, Long> lTimes = engine.lockLast;
		HashMap<String, Long> vTimes = engine.variableLast;

//		System.out.println("Locks :-");
//		for(String lname : lTimes.keySet()){
//			System.out.println(lname + " : " +  Long.toString(lTimes.get(lname)));
//		}
		
//		System.out.println("Variables :-");
//		for(String vname : vTimes.keySet()){
//			System.out.println(vname + " : " +  Long.toString(vTimes.get(vname)));
//		}
		
		int sharedLocks = 0;
		for(String lname : lTimes.keySet()){
			if(engine.lockToThreadSet.get(lname).size() > 1 ){
				sharedLocks = sharedLocks + 1;
			}
		}
		System.out.println("Number of shared locks = " + Integer.toString(sharedLocks));
		System.out.println("Number of locks = " + Integer.toString(lTimes.keySet().size()));
		
		int sharedVariables = 0;
		for(String vname : vTimes.keySet()){
			if(engine.variableToThreadSet.get(vname).size() > 1 ){
				sharedVariables = sharedVariables + 1;
			}
		}
		System.out.println("Number of shared variables = " + Integer.toString(sharedVariables));
		System.out.println("Number of variables = " + Integer.toString(vTimes.keySet().size()));

		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
	}
}
