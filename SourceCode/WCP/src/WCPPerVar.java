import engine.accesstimes.orderedvars.OrderedVarsEngine;
import engine.racedetection.wcpPerVar.WCPEngine;
import event.Lock;
import event.Thread;
import parse.ParserType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class WCPPerVar {
    public static void main(String[] args) {
//		runScript("D:\\Research\\traces\\sync-preserving-logs\\");
        analysis("C:\\Research\\traces\\SP-NoEmptyThread-Filter\\ftpserver.std");
    }

    public static void  analysis(String trace_folder){
        long startTimeAnalysis = System.currentTimeMillis();

        WCPEngine engine = new WCPEngine(ParserType.STD, trace_folder, true, false, 0, startTimeAnalysis);

        engine.analyzeTrace(true);

        long endTime = System.currentTimeMillis();
        double timeInMins = (endTime - startTimeAnalysis) * 1.0 / 60000;
        System.out.println("Finished: " + trace_folder + "  " + timeInMins + "  mins");
//        printAfterAnalysis(startTimeAnalysis, trace_folder, engine);

    }

    public static void printAfterAnalysis(long startTime, String trace_folder, WCPEngine engine){
        long stopTimeAnalysis = System.currentTimeMillis();
        long timeAnalysis = stopTimeAnalysis - startTime;
        double timeInMins = timeAnalysis * 1.0 / 60000;
        System.out.println("Time for full analysis = " + timeInMins + " mins");
        System.out.println("Number of racy vars: " + engine.state.racyVars.size());
        System.out.println(trace_folder + "              WCP Per Var");
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
