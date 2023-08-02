package engine.racedetection.zeroreversal.v4;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class ZREngine extends RaceDetectionEngine<ZRState, ZREvent> {

	public ZREngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread>();
		initializeReader(trace_folder);
		this.state = new ZRState(this.threadSet, verbosity);
		handlerEvent = new ZREvent();
	}

	@Override
	protected boolean skipEvent(ZREvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(ZREvent handlerEvent) {
		// if(handlerEvent.getType().isAccessType()){
		// if(state.verbosity == 1 || state.verbosity == 2){
		// System.out.println();
		// }
		// }
	}

}
