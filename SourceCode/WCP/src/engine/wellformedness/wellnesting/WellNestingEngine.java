package engine.wellformedness.wellnesting;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.RefinedAccessTimesEngine;
import engine.wellformedness.WellFormednessEngine;
import event.Thread;
import parse.ParserType;

public class WellNestingEngine extends WellFormednessEngine<WellNestingState, WellNestingEvent>{
		
	private HashMap<String, HashSet<String>> variableToThreadSet;
	private HashMap<String, HashSet<String>> lockToThreadSet;

	public WellNestingEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new WellNestingState(this.threadSet, verbosity);
		handlerEvent = new WellNestingEvent();
		
		boolean time_reporting = false;
		
		long startTimeAnalysis = 0;
		if(time_reporting){
			startTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
		}
				
		RefinedAccessTimesEngine accessTimesEngine = new RefinedAccessTimesEngine(pType, trace_folder);
		accessTimesEngine.computeLastAccessTimes();
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for Phase-1 = " + timeAnalysis + " milliseconds");	
		}
		
		this.state = new WellNestingState(this.threadSet, verbosity);
		this.variableToThreadSet = accessTimesEngine.variableToThreadSet;
		this.lockToThreadSet = accessTimesEngine.lockToThreadSet;	
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
	protected boolean skipEvent(WellNestingEvent handlerEvent){
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
	protected void postHandleEvent(WellNestingEvent handlerEvent){
	}
	
}
