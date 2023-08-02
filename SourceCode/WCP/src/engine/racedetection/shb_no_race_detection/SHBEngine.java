package engine.racedetection.shb_no_race_detection;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SHBEngine extends RaceDetectionEngine<SHBState, SHBEvent>{

	public SHBEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SHBState(this.threadSet, verbosity);
		handlerEvent = new SHBEvent();
	}

	@Override
	protected boolean skipEvent(SHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SHBEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
