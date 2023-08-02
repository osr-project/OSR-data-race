package engine.atomicity.incorrect_conflictserializability.vc;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.AccessTimesEngine;
import engine.atomicity.AtomicityEngine;
import event.Thread;
import parse.ParserType;

public class VCConflictSerializabilityOfflineEngine extends AtomicityEngine<VCConflictSerializabilityState, VCConflictSerializabilityEvent> {

	private HashMap<String, HashSet<String>> variableToThreadSet;
	private HashMap<String, HashSet<String>> lockToThreadSet;

	public VCConflictSerializabilityOfflineEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new VCConflictSerializabilityState(this.threadSet, verbosity);
		handlerEvent = new VCConflictSerializabilityEvent();

		boolean time_reporting = true;
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}	
		AccessTimesEngine accessTimesEngine = new AccessTimesEngine(pType, trace_folder);
		accessTimesEngine.computeLastAccessTimes();
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for Phase-1 = " + timeAnalysis + " milliseconds");	
		}

		this.variableToThreadSet = accessTimesEngine.variableToThreadSet;
		this.lockToThreadSet = accessTimesEngine.lockToThreadSet;
	}

	@Override
	protected boolean skipEvent(VCConflictSerializabilityEvent handlerEvent){
		boolean skip = false;
		if(handlerEvent.getType().isAccessType()){
			if(variableToThreadSet.get(handlerEvent.getVariable().getName()).size() <= 1 ){
				skip = true;
			}
		}
		if(handlerEvent.getType().isLockType()){
			if(lockToThreadSet.get(handlerEvent.getLock().getName()).size() <= 1 ){
				skip = true;
			}
		}
		return skip;
	}

	@Override
	protected void postHandleEvent(VCConflictSerializabilityEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
