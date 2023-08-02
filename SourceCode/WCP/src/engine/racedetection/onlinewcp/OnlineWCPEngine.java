package engine.racedetection.onlinewcp;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;


public class OnlineWCPEngine extends RaceDetectionEngine<OnlineWCPState, OnlineWCPEvent> {
		
	public OnlineWCPEngine(ParserType pType, String trace_folder, boolean forceOrdering, boolean tickClockOnAccess, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new OnlineWCPState(this.threadSet, forceOrdering, tickClockOnAccess, verbosity);
		handlerEvent = new OnlineWCPEvent();		
	}


	@Override
	protected boolean skipEvent(OnlineWCPEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(OnlineWCPEvent handlerEvent) {
	}
	
}
