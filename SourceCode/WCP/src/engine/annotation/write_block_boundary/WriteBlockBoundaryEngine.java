package engine.annotation.write_block_boundary;

import java.util.HashMap;
import java.util.LinkedList;

import engine.racedetection.RaceDetectionEngine;
import parse.ParserType;

public class WriteBlockBoundaryEngine extends RaceDetectionEngine<WriteBlockBoundaryState, WriteBlockBoundaryEvent>{
	
	public WriteBlockBoundaryEngine(ParserType pType, String trace_folder) {
		super(pType);
		initializeReader(trace_folder);
		this.state = new WriteBlockBoundaryState();
		handlerEvent = new WriteBlockBoundaryEvent();
	}
	
	public HashMap<String, LinkedList<Long>> getBoundaries(){
		return this.state.variableToListOfBoundaryIndices;
	}
	
	public HashMap<String, LinkedList<Boolean>> getThreadLocality(){
		return this.state.variableToListOfThreadLocality;
	}

	@Override
	protected boolean skipEvent(WriteBlockBoundaryEvent handlerEvent) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void postHandleEvent(WriteBlockBoundaryEvent handlerEvent) {
		// TODO Auto-generated method stub
	}	
}
