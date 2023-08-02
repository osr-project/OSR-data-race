package engine.racedetection.shb_epoch_old;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SHBEpochEngine extends RaceDetectionEngine<SHBEpochState, SHBEpochEvent>{

	public SHBEpochEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SHBEpochState(this.threadSet, verbosity);
		handlerEvent = new SHBEpochEvent();
	}

	@Override
	protected boolean skipEvent(SHBEpochEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SHBEpochEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
