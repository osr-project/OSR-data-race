package engine.racedetectionengine.spPerVar;

import engine.racedetectionengine.RaceDetectionEngine;
import parse.ParserType;

import java.util.HashSet;


public class SyncPreservingRaceOfflineEngine extends RaceDetectionEngine<SyncPreservingRaceState, SyncPreservingRaceEvent> {

	public SyncPreservingRaceOfflineEngine(ParserType pType, String trace_folder, long startTime) {
		super(pType);
		this.threadSet = new HashSet<> ();
		initializeReader(trace_folder);
		this.state = new SyncPreservingRaceState(this.threadSet, trace_folder, startTime);
		handlerEvent = new SyncPreservingRaceEvent();
	}


	@Override
	protected boolean skipEvent(SyncPreservingRaceEvent handlerEvent){
		return false;
	}

	@Override
	protected void postHandleEvent(SyncPreservingRaceEvent handlerEvent) {

	}

}
