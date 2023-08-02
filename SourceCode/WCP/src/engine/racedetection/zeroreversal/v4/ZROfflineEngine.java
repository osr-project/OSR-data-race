package engine.racedetection.zeroreversal.v4;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.orderedvars.OrderedVarsEngine;
import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;


public class ZROfflineEngine extends RaceDetectionEngine<ZRState, ZREvent> {

	private HashMap<String, HashSet<String>> lockToThreadSet;
	private HashSet<String> orderedVariables;
	
	public ZROfflineEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new ZRState(this.threadSet, verbosity);
		handlerEvent = new ZREvent();

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}	
		OrderedVarsEngine orderedVarsEngine = new OrderedVarsEngine(pType, trace_folder, 0);
		orderedVarsEngine.analyzeTrace(true);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for Phase-1 = " + timeAnalysis + " milliseconds");	
		}
		
		this.lockToThreadSet = orderedVarsEngine.getLockToThreadSet();
		this.state.stringVariableToThreadSet = orderedVarsEngine.getVariableToThreadSet();
		this.orderedVariables = orderedVarsEngine.getOrdredVars();
	}

	@Override
	protected boolean skipEvent(ZREvent handlerEvent){
		if(handlerEvent.getType().isAccessType()){
			String var_name = handlerEvent.getVariable().getName();
			if(this.orderedVariables.contains(var_name)) {
				return true;
			}
		}
		else if(handlerEvent.getType().isLockType()){
			if(lockToThreadSet.get(handlerEvent.getLock().getName()).size() <= 1 ){
				return true;
			}
		}
		return false;
	}

	@Override
	protected void postHandleEvent(ZREvent handlerEvent) {	
	}

}
