package engine.deadlock.rcpto_lastwritedataflow_loc;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.RefinedAccessTimesEngine;
import engine.dataflow.lastwrite.LastWriteEngine;
import engine.deadlock.DeadlockEngine;
import event.EventType;
import event.Thread;
import event.Variable;
import parse.ParserType;
import util.vectorclock.VectorClock;
import engine.deadlock.rcp_to_locations.RCPState;
import engine.deadlock.rcp_to_locations.RCPEvent;

public class RCPEngine extends DeadlockEngine<RCPState, RCPEvent>{
	
	private HashMap<String, Long> lockEndTimes;
	private HashMap<String, HashSet<String>> variableToThreadSet;
	private HashMap<String, HashSet<String>> lockToThreadSet;
	private VectorClock branchClock;
	private HashSet<String> usefulVariables;
	
	private int maxStackSize;
	
	private HashMap<Thread, Integer> thread_to_vc_index;
	private HashMap<Thread, VectorClock> threadClock;
	private HashMap<Variable, VectorClock> lastWriteClock;

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
		
		LastWriteEngine dataFlowEngine = new LastWriteEngine(pType, trace_folder, 0);
		dataFlowEngine.analyzeTrace(true);
		
		if(time_reporting){
			long stopTimeAnalysis = System.currentTimeMillis(); //System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for Phase-1 = " + timeAnalysis + " milliseconds");	
		}
		
		lockEndTimes = accessTimesEngine.lockLast;
		this.state = new RCPState(this.threadSet, verbosity);
		this.state.view.lockThreadLastInteraction = accessTimesEngine.lockThreadLast;
		this.state.existsLockReadVariableThreads = accessTimesEngine.existsLockReadVariableThreads;
		this.state.existsLockWriteVariableThreads = accessTimesEngine.existsLockWriteVariableThreads;
		this.state.variableToReadEquivalenceClass = accessTimesEngine.variableToReadEquivalenceClass;
		this.state.variableToWriteEquivalenceClass = accessTimesEngine.variableToWriteEquivalenceClass;
		
		this.variableToThreadSet = accessTimesEngine.variableToThreadSet;
		this.lockToThreadSet = accessTimesEngine.lockToThreadSet;
		
		this.branchClock = dataFlowEngine.getBranchClock();
		this.usefulVariables = dataFlowEngine.getUsefulVariables();
		
//		System.out.println("usefulvars = " + this.usefulVariables);
		
		// Maintaining DF clocks
		int vc_index = 0;
		this.thread_to_vc_index = new HashMap<Thread, Integer> ();
		this.threadClock = new HashMap<Thread, VectorClock> ();
		for(Thread t: this.threadSet){
			int t_ind = vc_index;
			thread_to_vc_index.put(t, t_ind);
			vc_index = vc_index + 1;
			VectorClock C_t = new VectorClock(this.threadSet.size());
			C_t.setClockIndex(t_ind, 1);
			threadClock.put(t, C_t);
		}
		this.lastWriteClock = new HashMap<Variable, VectorClock> ();
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
		EventType tp = handlerEvent.getType();
		Thread t = handlerEvent.getThread();
		VectorClock C_t  = this.threadClock.get(t);
		
		/********* Update clocks **************/
		if(tp.isAccessType()){
			Variable v = handlerEvent.getVariable();
			if(!this.lastWriteClock.containsKey(v)){
				lastWriteClock.put(v,  new VectorClock(this.threadSet.size()));
			}
			VectorClock LW_v = this.lastWriteClock.get(v);
			if(tp.isRead()){			
				C_t.updateWithMax(C_t, LW_v);
			}
			else{
				LW_v.copyFrom(C_t);
			}
		}
		/**********Update clocks *************/
		
		boolean skip = false;
		if(tp.isAccessType()){
			String v_name = handlerEvent.getVariable().getName();
			if(variableToThreadSet.get(v_name).size() <= 1 ){
				skip = true;
			}
			if(!skip){
				if(!this.usefulVariables.contains(v_name)){
					skip = true;
				}
				else{
					if(tp.isRead() && !(C_t.isLessThanOrEqual(branchClock))){
						skip = true;
					}
				}
			}
			if(tp.isWrite()){
				int t_ind = this.thread_to_vc_index.get(t);
				C_t.setClockIndex(t_ind, C_t.getClockIndex(t_ind) + 1);
			}
		}
		if(tp.isLockType()){
			if(lockToThreadSet.get(handlerEvent.getLock().getName()).size() <= 1 ){
				skip = true;
			}
		}
		if(tp.isBranch() || tp.isTransactionType()){
			skip = true;
		}
		if(skip){
//			System.out.println("Skipping " + handlerEvent.toCompactString() + ", C_t = " + C_t+ ", branch = " + branchClock);
		}
		return skip;
	}

	@Override
	protected void postHandleEvent(RCPEvent handlerEvent){
//		System.out.println(handlerEvent.toCompactString() + " => (C_t =" + state.generateVectorClockFromClockThread(handlerEvent.getThread()) + ", P_t = " + state.getVectorClock(state.StrictRCPThread, handlerEvent.getThread()) + ")");
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
