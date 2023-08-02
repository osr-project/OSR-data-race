package engine.deadlock.goodlock2;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.RefinedAccessTimesEngine;
import engine.deadlock.DeadlockEngine;
import engine.deadlock.goodlock2_rw.Goodlock2AnnotationEngine;
import engine.deadlock.goodlock2_rw.Goodlock2State;
import event.Thread;
import parse.ParserType;

public class Goodlock2Engine extends DeadlockEngine<Goodlock2State, Goodlock2Event>{
	
	private HashMap<String, HashSet<String>> variableToThreadSet;
	private HashMap<String, HashSet<String>> lockToThreadSet;

	public Goodlock2Engine(ParserType pType, String trace_folder, int verbosity){
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		handlerEvent = new Goodlock2Event();
		
		RefinedAccessTimesEngine accessTimesEngine = new RefinedAccessTimesEngine(pType, trace_folder);
		accessTimesEngine.computeLastAccessTimes();
		this.variableToThreadSet = accessTimesEngine.variableToThreadSet;
		this.lockToThreadSet = accessTimesEngine.lockToThreadSet;
		
		Goodlock2AnnotationEngine annotationEngine = new Goodlock2AnnotationEngine(pType, trace_folder);
		annotationEngine.analyzeTrace();
//		for(String str: annotationEngine.specialWrites.keySet()){
//			System.out.println(str + " ==> " + annotationEngine.specialWrites.get(str));
//		}
		
		this.state = new Goodlock2State(this.threadSet, annotationEngine.specialWrites, verbosity);
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
		System.out.println("Analysis complete");
		System.out.println("Number of deadlock patterns (of size 2) found = " + Long.toString(deadlockCount));
	}
	
	@Override
	protected boolean skipEvent(Goodlock2Event handlerEvent){
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
	protected void postHandleEvent(Goodlock2Event handlerEvent){
	}
	
}
