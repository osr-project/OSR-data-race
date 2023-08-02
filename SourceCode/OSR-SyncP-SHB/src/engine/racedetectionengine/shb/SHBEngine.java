package engine.racedetectionengine.shb;

import engine.racedetectionengine.RaceDetectionEngine;
import parse.ParserType;

import java.util.HashSet;

public class SHBEngine extends RaceDetectionEngine<SHBState, SHBEvent>{

	public SHBEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<> ();
		initializeReader(trace_folder);
		this.state = new SHBState(this.threadSet);
		handlerEvent = new SHBEvent();
	}

	@Override
	protected boolean skipEvent(SHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SHBEvent handlerEvent) {	
	}

}
