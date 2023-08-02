package engine.deadlock.rcp_to;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.RefinedAccessTimesEngine;
import engine.deadlock.DeadlockEngine;
import event.Thread;
import parse.ParserType;

public class RCPEngine extends DeadlockEngine<RCPState, RCPEvent>{
	
	private HashMap<String, Long> lockEndTimes;
	//private HashMap<String, Long> variableEndTimes;
	//private HashMap<String, HashMap<String, Long>> lockThreadLastInteraction;
	
	private HashMap<String, HashSet<String>> variableToThreadSet;
	private HashMap<String, HashSet<String>> lockToThreadSet;
	
	private int maxStackSize;


	public RCPEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new RCPState(this.threadSet, verbosity);
		handlerEvent = new RCPEvent();
		
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
		
		lockEndTimes = accessTimesEngine.lockLast;
		//variableEndTimes = accessTimesEngine.variableLast;
		this.state = new RCPState(this.threadSet, verbosity);
		this.state.view.lockThreadLastInteraction = accessTimesEngine.lockThreadLast;
		this.state.existsLockReadVariableThreads = accessTimesEngine.existsLockReadVariableThreads;
		this.state.existsLockWriteVariableThreads = accessTimesEngine.existsLockWriteVariableThreads;
		this.state.variableToReadEquivalenceClass = accessTimesEngine.variableToReadEquivalenceClass;
		this.state.variableToWriteEquivalenceClass = accessTimesEngine.variableToWriteEquivalenceClass;
		//this.state.variableToAccessedLocks = accessTimesEngine.variableToAccessdLocks;
		
		this.variableToThreadSet = accessTimesEngine.variableToThreadSet;
		this.lockToThreadSet = accessTimesEngine.lockToThreadSet;	
	}
	
	@Override
	public void analyzeTrace(boolean multipleRace){
		eventCount = (long) 0;
		deadlockCount = (long) 0;
		totalSkippedEvents = (long) 0;
		maxStackSize = 0;
		
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
		System.out.println("Number of deadlocks found = " + Long.toString(deadlockCount));
		System.out.println("Maximum stack size = " + Integer.toString(maxStackSize));
	}
	
	@Override
	protected boolean skipEvent(RCPEvent handlerEvent){
//		System.out.println(handlerEvent.toCompactString());
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
	protected void postHandleEvent(RCPEvent handlerEvent){
		int stackSize = state.view.getSize();
		if(maxStackSize < stackSize){
			maxStackSize = stackSize;
		}	
		if(handlerEvent.getType().isLockType()){
			long currEventIndex = handlerEvent.getAuxId();
			long lockThreadEndIndex = this.state.view.lockThreadLastInteraction.get(handlerEvent.getLock().getName()).get(handlerEvent.getThread().getName());
			if(currEventIndex >= lockThreadEndIndex){
				state.destroyLockThreadStack(handlerEvent.getLock(), handlerEvent.getThread());
			}
			//If the lock has to be deleted, it should be done at the end
			long lockEndIndex = lockEndTimes.get(handlerEvent.getLock().getName());
			if(currEventIndex >= lockEndIndex){
				state.destroyLock(handlerEvent.getLock());
			}
		}
	}
	
}
