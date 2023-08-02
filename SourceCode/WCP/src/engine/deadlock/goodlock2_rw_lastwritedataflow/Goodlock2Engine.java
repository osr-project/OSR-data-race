package engine.deadlock.goodlock2_rw_lastwritedataflow;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.RefinedAccessTimesEngine;
import engine.dataflow.lastwrite.LastWriteEngine;
import engine.deadlock.DeadlockEngine;
import engine.deadlock.goodlock2_rw.Goodlock2AnnotationEngine;
import engine.deadlock.goodlock2_rw.Goodlock2Event;
import engine.deadlock.goodlock2_rw.Goodlock2State;
import event.EventType;
import event.Thread;
import event.Variable;
import parse.ParserType;
import util.vectorclock.VectorClock;

public class Goodlock2Engine extends DeadlockEngine<Goodlock2State, Goodlock2Event>{
	
	private HashMap<String, HashSet<String>> variableToThreadSet;
	private HashMap<String, HashSet<String>> lockToThreadSet;
	
	private VectorClock branchClock;
	private HashSet<String> usefulVariables;
	
	private HashMap<Thread, Integer> thread_to_vc_index;
	private HashMap<Thread, VectorClock> threadClock;
	private HashMap<Variable, VectorClock> lastWriteClock;

	public Goodlock2Engine(ParserType pType, String trace_folder, int verbosity){
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		handlerEvent = new Goodlock2Event();
		
		RefinedAccessTimesEngine accessTimesEngine = new RefinedAccessTimesEngine(pType, trace_folder);
		accessTimesEngine.computeLastAccessTimes();
		this.variableToThreadSet = accessTimesEngine.variableToThreadSet;
		this.lockToThreadSet = accessTimesEngine.lockToThreadSet;
		
		LastWriteEngine dataFlowEngine = new LastWriteEngine(pType, trace_folder, 0);
		dataFlowEngine.analyzeTrace(true);
		this.branchClock = dataFlowEngine.getBranchClock();
		this.usefulVariables = dataFlowEngine.getUsefulVariables();
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
		
		Goodlock2AnnotationEngine annotationEngine = new Goodlock2AnnotationEngine(pType, trace_folder);
		annotationEngine.analyzeTrace();
		
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
	protected void postHandleEvent(Goodlock2Event handlerEvent){
	}
	
}
