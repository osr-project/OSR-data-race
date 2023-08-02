package engine.racedetectionengine.shbPerVar;

import engine.racedetectionengine.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

import java.util.HashMap;
import java.util.HashSet;

public class SHBEngine extends RaceDetectionEngine<SHBState, SHBEvent>{

	public HashSet<String> orderedVariables;
	public HashMap<String, HashSet<String>> lockToThreadSet;

	public SHBEngine(ParserType pType, String trace_folder) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SHBState(this.threadSet);
		handlerEvent = new SHBEvent();
	}

	public SHBEngine(ParserType pType, String trace_folder, long startTime) {
		super(pType);
		this.threadSet = new HashSet<> ();
		initializeReader(trace_folder);
		this.state = new SHBState(this.threadSet, trace_folder, startTime);
		handlerEvent = new SHBEvent();
	}

	public SHBEngine(ParserType pType, String trace_folder, long startTime, HashSet<String> orderedVariables,
					 HashMap<String, HashSet<String>> lockToThreadSet) {
		super(pType);
		this.orderedVariables = orderedVariables;
		this.lockToThreadSet = lockToThreadSet;
		this.threadSet = new HashSet<> ();
		initializeReader(trace_folder);
		this.state = new SHBState(this.threadSet, trace_folder, startTime);
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
