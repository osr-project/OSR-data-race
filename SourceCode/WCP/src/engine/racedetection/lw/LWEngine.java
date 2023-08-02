package engine.racedetection.lw;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class LWEngine extends RaceDetectionEngine<LWState, LWEvent>{

	public LWEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new LWState(this.threadSet, verbosity);
		handlerEvent = new LWEvent();
	}

	@Override
	protected boolean skipEvent(LWEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(LWEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
