import engine.racedetectionengine.shbPerVar.SHBEngine;
import parse.ParserType;

import java.util.ArrayList;
import java.util.List;

public class SHBPerVar {
    public static void main(String[] args) {
//		CmdOptions options = new GetOptions(args).parse();

        String trace_folder = "C:\\Research\\traces\\sync-preserving-logs\\lang.std";
        analysis(trace_folder);
//		runScript("C:\\Research\\traces\\sync-preserving-logs\\");
    }

    public static void analysis(String trace_folder){
        long startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();

        SHBEngine engine = new SHBEngine(ParserType.STD, trace_folder, startTimeAnalysis);
        engine.analyzeTrace(true, 0);
        long endTime = System.currentTimeMillis();
        double timeInMins = (endTime - startTimeAnalysis) * 1.0 / 60000;
        System.out.println("Finished: " + trace_folder + "  " + timeInMins + " mins,  racy vars: " + engine.state.racyVars.size());
//        printAfterAnalysis(trace_folder, startTimeAnalysis, engine);
    }

    public static void printAfterAnalysis(String trace_folder, long startTime, SHBEngine engine){
        long stopTimeAnalysis = System.currentTimeMillis();
        long timeAnalysis = stopTimeAnalysis - startTime;
        double timeInMins = timeAnalysis * 1.0 / 60000;
        double timeInSeconds = timeAnalysis * 1.0 / 1000;
        System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
        System.out.println("Time for analysis in seconds = " + String.format("%.3f", timeInSeconds));
        System.out.println("Time for analysis in mins = " + String.format("%.1f", timeInMins));

        System.out.println(trace_folder + "          " + "SHB Per Var");
        System.out.println("Number of racy vars: " + engine.state.racyVars.size());
        System.out.println("--------------------------------------------------");
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
        files.add("moldyn.std");
        files.add("linkedlist.std");
        files.add("derby.std");
        files.add("jigsaw.std");
        files.add("sunflow.std");
        files.add("cryptorsa.std");
        files.add("xalan.std");
        files.add("lufact.std");
        files.add("batik.std");
        files.add("lusearch.std");
        files.add("tsp.std");
        files.add("luindex.std");
        files.add("sor.std");


        for(String file : files){
            analysis(trace_folder + file);
        }
    }
}
