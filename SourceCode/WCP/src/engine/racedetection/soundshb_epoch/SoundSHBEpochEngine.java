package engine.racedetection.soundshb_epoch;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SoundSHBEpochEngine extends RaceDetectionEngine<SoundSHBEpochState, SoundSHBEpochEvent>{

	public SoundSHBEpochEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SoundSHBEpochState(this.threadSet, verbosity);
		handlerEvent = new SoundSHBEpochEvent();
	}

	@Override
	protected boolean skipEvent(SoundSHBEpochEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SoundSHBEpochEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
