package engine.racedetection.soundfhb;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SoundFHBEngine extends RaceDetectionEngine<SoundFHBState, SoundFHBEvent>{

	public SoundFHBEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SoundFHBState(this.threadSet, verbosity);
		handlerEvent = new SoundFHBEvent();
	}

	@Override
	public void postAnalysis(){
		System.out.println("Number of race pc pairs = " + Integer.toString(state.getLocPairs().size()));
	}
	
	@Override
	protected boolean skipEvent(SoundFHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SoundFHBEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
