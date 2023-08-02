package engine.racedetection.soundshb;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SoundSHBEngine extends RaceDetectionEngine<SoundSHBState, SooundSHBEvent>{

	public SoundSHBEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SoundSHBState(this.threadSet, verbosity);
		handlerEvent = new SooundSHBEvent();
	}

	@Override
	protected boolean skipEvent(SooundSHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SooundSHBEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
