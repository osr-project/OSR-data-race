package engine.dataflow.lastwrite;

import java.util.HashSet;

import engine.dataflow.DataFlowEngine;
import event.Thread;
import parse.ParserType;
import util.vectorclock.VectorClock;

public class LastWriteEngine extends DataFlowEngine<LastWriteState, LastWriteEvent>{

	public LastWriteEngine(ParserType pType, String trace_folder, int verbosity){
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		handlerEvent = new LastWriteEvent();
		this.state = new LastWriteState(this.threadSet, verbosity);
	}
	
	@Override
	public void analyzeTrace(boolean multipleRace){
		eventCount = (long) 0;
		deadlockCount = (long) 0;
		totalSkippedEvents = (long) 0;
		
		if(this.parserType.isRV()){
			analyzeTraceRV(multipleRace);		
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV(multipleRace);
		}
		else if(this.parserType.isRR()){
			analyzeTraceRR(multipleRace);
		}
		else if(this.parserType.isSTD()){
			analyzeTraceSTD(multipleRace);
		}
		else{
			throw new IllegalArgumentException("Unkown parser type " + this.parserType);
		}
	}
	
	@Override
	protected boolean skipEvent(LastWriteEvent handlerEvent){
		return false;
	}

	@Override
	protected void postHandleEvent(LastWriteEvent handlerEvent){
	}
	
	public VectorClock getBranchClock(){
		return this.state.branchClock;
	}
	
	public HashSet<String> getUsefulVariables(){
		return this.state.getUsefulVariables();
	}
	
}
