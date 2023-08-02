package engine.racedetection.fasthb_treeclock;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class FastHBEngine extends RaceDetectionEngine<FastHBState, FastHBEvent> {

	public FastHBEngine(ParserType pType, String trace_folder, boolean forceOrdering,
			boolean tickClockOnAccess, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread>();
		initializeReader(trace_folder);
		this.state = new FastHBState(this.threadSet, forceOrdering, tickClockOnAccess,
				verbosity);
		handlerEvent = new FastHBEvent();
	}

	@Override
	protected boolean skipEvent(FastHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(FastHBEvent handlerEvent) {
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
