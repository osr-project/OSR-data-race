package engine.atomicity.conflictserializability.vc_unopt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.atomicity.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;


public class THBState extends State {

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;

	public ArrayList<VectorClock> clockThread; // mathcal{C}
	public ArrayList<VectorClock> clockThreadBegin; // mathcal{C^BEGIN}
	public HashMap<Lock, VectorClock> clockLock; // mathcal{L}
	public HashMap<Variable, VectorClock> clockWriteVariable; // mathcal{W}
	public HashMap<Variable, HashMap<Thread, VectorClock>> clockReadVariableThread; // mathcal{R}

	public HashMap<Lock, Thread> lastThreadToRelease;
	public HashMap<Variable, Thread> lastThreadToWrite;

	public HashMap<Thread, Integer> threadToNestingDepth;

	//parameter flags
	public int verbosity;

	public THBState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Integer>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			//System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (Integer)this.numThreads);
			this.numThreads ++;
		}

		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {
		this.clockThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);
		for(int tIndex = 0; tIndex < this.numThreads; tIndex ++){
			this.clockThread.get(tIndex).setClockIndex(tIndex, 1);
		}

		this.clockThreadBegin = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThreadBegin, this.numThreads);

		this.clockLock = new HashMap<Lock, VectorClock> ();

		this.clockWriteVariable = new HashMap<Variable, VectorClock> ();

		this.clockReadVariableThread = new HashMap<Variable, HashMap<Thread, VectorClock>> ();
		
		this.lastThreadToRelease = new HashMap<Lock, Thread> ();
		
		this.lastThreadToWrite = new HashMap<Variable, Thread> ();

		this.threadToNestingDepth = new HashMap<Thread, Integer> ();
		for(Thread t: tSet){
			this.threadToNestingDepth.put(t, 0);
		}
	}

	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		int origVal = this.clockThread.get(tIndex).getClockIndex(tIndex);
		this.clockThread.get(tIndex).setClockIndex(tIndex, (Integer)(origVal + 1));
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(HashMap<Lock, VectorClock> arr, Lock l) {
		checkAndAddLock(l);
		return arr.get(l);
	}

	public VectorClock getVectorClock(HashMap<Variable, VectorClock> arr, Variable v) {
		checkAndAddVariable(v);
		return arr.get(v);
	}

	public VectorClock getVectorClock(HashMap<Variable, HashMap<Thread, VectorClock>> arr, Variable v, Thread t) {
		checkAndAddVariable(v);
		return arr.get(v).get(t);
	}


	public int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			//System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			this.clockLock.put(l, new VectorClock(this.numThreads));
		}
		return lockToIndex.get(l);
	}

	public int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			this.clockReadVariableThread.put(v, new HashMap<Thread, VectorClock> ());
			for(Thread t: this.threadToIndex.keySet()){
				this.clockReadVariableThread.get(v).put(t,  new VectorClock(this.numThreads));
			}
			this.clockWriteVariable.put(v, new VectorClock(this.numThreads));
		}
		return variableToIndex.get(v);
	}

	public boolean checkAndGetClock(VectorClock fromClock, Thread target) {
		boolean violationDetected = false;
		VectorClock C_target_begin = getVectorClock(clockThreadBegin, target);		
		if(C_target_begin.isLessThanOrEqual(fromClock) && threadToNestingDepth.get(target) > 0) {
			violationDetected = true;
		}
		VectorClock C_target = getVectorClock(clockThread, target);
		C_target.updateWithMax(C_target, fromClock);
		return violationDetected;
	}

	public boolean handshakeAtEndEvent(Thread t) {
		boolean violationDetected = false;
		VectorClock C_t_begin = getVectorClock(clockThreadBegin, t);
		VectorClock C_t = getVectorClock(clockThread, t);

		for(Thread u: this.threadToIndex.keySet()) {
			if(!u.equals(t)) {
				VectorClock C_u = getVectorClock(clockThread, u);
				if(C_t_begin.isLessThanOrEqual(C_u)) {
					violationDetected |= checkAndGetClock(C_t, u);
					if(violationDetected) break;
				}
			}
		}
		if(violationDetected) return violationDetected;

		for(Lock l: this.lockToIndex.keySet()) {
			VectorClock L_l = getVectorClock(clockLock, l);
			if(C_t_begin.isLessThanOrEqual(L_l)) {
				L_l.updateWithMax(L_l, C_t);
			}
		}

		for(Variable v: this.variableToIndex.keySet()) {
			VectorClock W_v = getVectorClock(clockWriteVariable, v);
			if(C_t_begin.isLessThanOrEqual(W_v)) {
				W_v.updateWithMax(W_v, C_t);
			}

			for(Thread u: this.threadToIndex.keySet()) {
				VectorClock R_v_u = getVectorClock(clockReadVariableThread, v, u);
				if(C_t_begin.isLessThanOrEqual(R_v_u)) {
					R_v_u.updateWithMax(R_v_u, C_t);
				}
			}
		}

		return violationDetected;
	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}

	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}