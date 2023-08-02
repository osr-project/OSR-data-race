package engine.racedetectionengine.SyncPNoOrderVars;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

import java.util.HashMap;
import java.util.HashSet;


public class SyncPreservingRaceOfflineEngine extends RaceDetectionEngine<SyncPreservingRaceState, SyncPreservingRaceEvent> {

	private HashMap<String, HashSet<String>> lockToThreadSet;
	private HashSet<String> orderedVariables;
	
	public SyncPreservingRaceOfflineEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SyncPreservingRaceState(this.threadSet);
		handlerEvent = new SyncPreservingRaceEvent();

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}

		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
//			System.out.println("Time for Phase-1 = " + timeAnalysis + " milliseconds");
		}

	}

	@Override
	protected boolean skipEvent(SyncPreservingRaceEvent handlerEvent){
		return false;
	}

	@Override
	protected void postHandleEvent(SyncPreservingRaceEvent handlerEvent) {

	}

}
