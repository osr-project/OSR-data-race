package engine.racedetection.hb.norace_optjoin;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class HBEngine extends RaceDetectionEngine<HBState, HBEvent>{

	public HBEngine(ParserType pType, String trace_folder, boolean forceOrdering, boolean tickClockOnAccess, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new HBState(this.threadSet, forceOrdering, tickClockOnAccess, verbosity);
		handlerEvent = new HBEvent();
	}

	@Override
	protected boolean skipEvent(HBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(HBEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
