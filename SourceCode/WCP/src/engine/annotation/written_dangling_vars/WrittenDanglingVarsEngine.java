package engine.annotation.written_dangling_vars;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import engine.annotation.write_block_boundary.WriteBlockBoundaryEngine;
import engine.racedetection.RaceDetectionEngine;
import parse.ParserType;

public class WrittenDanglingVarsEngine extends RaceDetectionEngine<WrittenDanglingVarsState, WrittenDanglingVarsEvent> {

	public WrittenDanglingVarsEngine(ParserType pType, String trace_folder) {
		super(pType);

		WriteBlockBoundaryEngine wbbengine = new WriteBlockBoundaryEngine(pType, trace_folder);
		wbbengine.enablePrintStatus = false;
		wbbengine.analyzeTrace(true);
		HashMap<String, LinkedList<Long>> boundaries = wbbengine.getBoundaries();
		HashMap<String, LinkedList<Boolean>> threadLocality = wbbengine.getThreadLocality();

		initializeReader(trace_folder);
		this.state = new WrittenDanglingVarsState(boundaries, threadLocality);
		handlerEvent = new WrittenDanglingVarsEvent();
	}

	public HashMap<String, LinkedList<Long>> getBoundaries() {
		return this.state.variableToListOfBoundaryIndices;
	}

	public HashMap<String, LinkedList<HashSet<String>>> getWrittenVars() {
		return this.state.lockToListOfWrittenVars;
	}

	public HashMap<String, LinkedList<HashSet<String>>> getDanglingVars() {
		return this.state.lockToListOfDanglingVars;
	}

	@Override
	protected boolean skipEvent(WrittenDanglingVarsEvent handlerEvent) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void postHandleEvent(WrittenDanglingVarsEvent handlerEvent) {
		// TODO Auto-generated method stub
	}
}
