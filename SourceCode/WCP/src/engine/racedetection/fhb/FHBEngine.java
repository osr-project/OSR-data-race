package engine.racedetection.fhb;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class FHBEngine extends RaceDetectionEngine<FHBState, FHBEvent>{

	public FHBEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new FHBState(this.threadSet, verbosity);
		handlerEvent = new FHBEvent();
	}

	@Override
	protected boolean skipEvent(FHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(FHBEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
