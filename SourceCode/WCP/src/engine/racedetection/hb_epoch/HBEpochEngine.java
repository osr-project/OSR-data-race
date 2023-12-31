package engine.racedetection.hb_epoch;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class HBEpochEngine extends RaceDetectionEngine<HBEpochState, HBEpochEvent>{

	public HBEpochEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new HBEpochState(this.threadSet, verbosity);
		handlerEvent = new HBEpochEvent();
	}

	@Override
	protected boolean skipEvent(HBEpochEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(HBEpochEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
