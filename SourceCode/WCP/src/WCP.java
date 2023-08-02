import engine.racedetection.wcp.WCPEngine;
import parse.ParserType;

import java.util.ArrayList;
import java.util.List;

public class WCP {

	public WCP() {

	}
	
	public static void main(String[] args) {
//		runScript("D:\\Research\\traces\\sync-preserving-logs\\");
		analysis("C:\\Research\\traces\\SP-NoEmptyThread-Filter\\ftpserver.std");
	}

	public static void analysis(String trace_folder){

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}

//		WCPEngine engine = new WCPEngine(options.parserType, options.path, options.forceOrdering, options.tickClockOnAccess, options.verbosity );

		WCPEngine engine = new WCPEngine(ParserType.STD, trace_folder, true, false, 0);

//		engine.analyzeTrace(options.multipleRace);
		engine.analyzeTrace(true);

		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for full analysis = " + timeAnalysis + " milliseconds");
		}
		System.out.println(trace_folder + "              WCP");
		System.out.println("-----------------------------------------------------------------");
	}

	public static void runScript(String trace_folder){

		List<String> files = new ArrayList<>();
        files.add("array.std");
        files.add("critical.std");
        files.add("account.std");
        files.add("airlinetickets.std");
        files.add("pingpong.std");
        files.add("twostage.std");
        files.add("wronglock.std");
        files.add("boundedbuffer.std");
        files.add("producerconsumer.std");
        files.add("clean.std");
        files.add("mergesort.std");
        files.add("bubblesort.std");
        files.add("lang.std");
        files.add("readerswriters.std");
        files.add("raytracer.std");
        files.add("bufwriter.std");
        files.add("ftpserver.std");
//        files.add("moldyn.std");
//        files.add("linkedlist.std");
//        files.add("derby.std");
//        files.add("jigsaw.std");
//        files.add("sunflow.std");
//		files.add("cryptorsa.std");
//        files.add("xalan.std");
//		files.add("lufact.std");
//        files.add("batik.std");
//        files.add("lusearch.std");
//		files.add("tsp.std");
//        files.add("luindex.std");
//		files.add("sor.std");

		for(String file : files){
			analysis(trace_folder + file);
		}
	}

}
