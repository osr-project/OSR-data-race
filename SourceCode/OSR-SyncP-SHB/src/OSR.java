import engine.racedetectionengine.OSR.POBuild.POBuildReverse;
import engine.racedetectionengine.OSR.OSREngine;
import event.Event;
import event.Lock;
import event.Thread;
import event.Variable;
import parse.ParserType;
import util.Triplet;
import util.vectorclock.VectorClock;

import java.io.IOException;
import java.util.*;

public class OSR {
    public static void main(String[] args) throws IOException {
        runScript("C:\\Research\\traces\\SP-NoEmptyThread-Filter\\");
//        runScript("D:\\Research\\traces\\SP-NoEmptyThread-Filter\\");
    }

    public static void test(String traceDir) throws IOException {
        POBuildReverse pb = new POBuildReverse(traceDir);
        pb.readTraceAndUpdateDS();
    }

    public static void runScript(String trace_folder) throws IOException {

        List<String> files = new ArrayList<>();
//        files.add("test.std");
//        files.add("array.std");
//        files.add("critical.std");
//        files.add("account.std");
//        files.add("airlinetickets.std");
//        files.add("pingpong.std");
//        files.add("twostage.std");
//        files.add("wronglock.std");
//        files.add("boundedbuffer.std");
//        files.add("producerconsumer.std");
//        files.add("clean.std");
        files.add("mergesort.std");
        files.add("bubblesort.std");
        files.add("lang.std");
//        files.add("readerswriters.std");
//        files.add("raytracer.std");
//        files.add("bufwriter.std");
//        files.add("ftpserver.std");
//        files.add("moldyn.std");
//        files.add("linkedlist.std");
//        files.add("derby.std");
//        files.add("jigsaw.std");
//        files.add("sunflow.std");
//        files.add("cryptorsa.std");
//        files.add("xalan.std");
//        files.add("lufact.std");
//        files.add("batik.std");
//        files.add("lusearch.std");
//        files.add("tsp.std");
//        files.add("luindex.std");
//        files.add("sor.std");
//        files.add("graphchi_filter.std");

//        files.add("test3.std");

        for(String file : files){
            analysis(trace_folder + file);
        }
//        saveToCSV(saveLocation);
    }

    public static void analysis(String trace_dir) throws IOException {
        boolean time_reporting = true;
        long startTimeAnalysis = 0;
        if (time_reporting) {
            startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
        }

//        OrderedVarsEngine orderedVarsEngine = new OrderedVarsEngine(ParserType.STD, trace_dir, 0);
//        orderedVarsEngine.analyzeTrace(true, 0);
//        HashSet<String> ordredVars = orderedVarsEngine.getOrdredVars();
//        HashMap<String, HashSet<String>> lockToThreadSet = orderedVarsEngine.getLockToThreadSet();
//        HashSet<Thread> threads = orderedVarsEngine.getThreadSet();
//        orderedVarsEngine = null;
//
//        Thread.threadCountTracker = 0;
//        Lock.lockCountTracker = 0;
//        Variable.variableCountTracker = 0;
//        Event.eventCountTracker = 0L;

        POBuildReverse po = new POBuildReverse(trace_dir);
        po.readTraceAndUpdateDS();
        int numThreads = po.numThreads;
//        po.printArray();

        Map<Integer, Map<Integer, ArrayList<Integer>>> succToNode = po.succToNode;
        Map<Integer, Map<Integer, ArrayList<Integer>>> succFromNode = po.succFromNode;

        OSREngine engine = new OSREngine(ParserType.STD, trace_dir, numThreads, succFromNode, succToNode);
        succToNode = null;
        succFromNode = null;
        engine.analyzeTrace(true, 0);

        Thread.threadCountTracker = 0;
        Lock.lockCountTracker = 0;
        Variable.variableCountTracker = 0;
        Event.eventCountTracker = 0L;

//        testShortestPath(engine);

        long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
        long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
        double timeInSeconds = timeAnalysis * 1.0 / 1000;
        double timeInMin = timeAnalysis * 1.0 / 60000;
        System.out.println("======================================================");
//        System.out.println("Time for analysis = " + timeAnalysis + " milliseconds");
        System.out.println("Time for analysis in seconds = " + String.format("%.3f", timeInSeconds));
        System.out.println("Time for analysis in mins = " + String.format("%.1f", timeInMin));
        System.out.println("Number of racy events: " + engine.state.racyEvents.size());
        System.out.println("Racy events: " + engine.state.racyEvents);
        System.out.println("Number of racy locations: " + engine.state.racyLocations.size());
        System.out.println("Racy locations: " + engine.state.racyLocations);
        System.out.println(trace_dir + "                              OSR");
    }

    private static void testShortestPath(OSREngine engine) {
        VectorClock ssp = new VectorClock(engine.state.numThreads);
        ssp.setClockIndex(0, 2);
        ssp.setClockIndex(1, 2);
        ssp.setClockIndex(2, 2);
        ssp.setClockIndex(3, 2);
        ssp.setClockIndex(4, 4);

        Triplet<Integer, Integer, Long> event = new Triplet<>(0, 0, 0L);

        System.out.println("query : " + engine.state.partialOrder.successors.get(0).get(1).getMinWithRange(0, 1));
        System.out.println("earlies : " + engine.state.partialOrder.queryForEvent(event, ssp));
    }
}
