package engine.racedetection.hb_treeclock.monitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.treeclock.recursive.monitor.TreeClockRecursiveMonitor;

public class HBState extends State {

	// Internal data
	private HashMap<Thread, Short> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private short numThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<TreeClockRecursiveMonitor> clockThread;
	public ArrayList<TreeClockRecursiveMonitor> lastReleaseLock;
	public ArrayList<TreeClockRecursiveMonitor> readVariable;
	public ArrayList<TreeClockRecursiveMonitor> writeVariable;
	
	//parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	public HBState(HashSet<Thread> tSet, boolean forceOrder, boolean tickClockOnAccess, int verbosity) {
		this.forceOrder = forceOrder;
		this.tickClockOnAccess = tickClockOnAccess;
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Short>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			//System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, this.numThreads);
			this.numThreads ++;
		}
		
		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	public void initData(HashSet<Thread> tSet) {
		// Initialize clockThread
		this.clockThread = new ArrayList<TreeClockRecursiveMonitor>();
		for (short i = 0; i < this.numThreads; i++) {
			this.clockThread.add(new TreeClockRecursiveMonitor(i, (short)0));
		}
	
		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<TreeClockRecursiveMonitor>();

		// initialize readVariable
		this.readVariable = new ArrayList<TreeClockRecursiveMonitor>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<TreeClockRecursiveMonitor>();
	}
	
	// Access methods
	private TreeClockRecursiveMonitor getTreeClockFrom1DArray(ArrayList<TreeClockRecursiveMonitor> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}
	
	private TreeClockRecursiveMonitor getTreeClockFrom1DArray(ArrayList<TreeClockRecursiveMonitor> arr, short index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}
	
	public int checkAndAddLock(Lock l, Thread t){
		if(!lockToIndex.containsKey(l)){
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			short tid = this.threadToIndex.get(t);
			lastReleaseLock.add(new TreeClockRecursiveMonitor(tid));
		}
		return lockToIndex.get(l);
	}
	
	public void addLock(Lock l, Thread t){
		if(!lockToIndex.containsKey(l)){
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			short tid = this.threadToIndex.get(t);
			lastReleaseLock.add(new TreeClockRecursiveMonitor(tid));
		}
	}
	
	public int checkAndAddVariable(Variable v, Thread t){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			short tid = this.threadToIndex.get(t);
			readVariable.add(new TreeClockRecursiveMonitor(tid));
			writeVariable.add(new TreeClockRecursiveMonitor(tid));
		}
		return variableToIndex.get(v);
	}
	
	public void addVariable(Variable v, Thread t){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			short tid = this.threadToIndex.get(t);
			readVariable.add(new TreeClockRecursiveMonitor(tid));
			writeVariable.add(new TreeClockRecursiveMonitor(tid));
		}
	}

	/*
	public int getClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		return clockThread.getLocalClock(tIndex);
	}
	
	public VectorClock generateVectorClockFromClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);

		int tValue = getClockThread(t);
		VectorClock pred = getVectorClock(HBPredecessorThread, t);
		VectorClock hbClock = new VectorClock(pred);
		
		hbClock.setClockIndex(tIndex, tValue);
		return hbClock;
	}
	*/

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		TreeClockRecursiveMonitor tc_t = clockThread.get(tIndex);
		tc_t.incrementBy(1);
	}

	public TreeClockRecursiveMonitor getTreeClock(ArrayList<TreeClockRecursiveMonitor> arr, Thread t) {
		short tIndex = threadToIndex.get(t);
		return getTreeClockFrom1DArray(arr, tIndex);
	}

	public TreeClockRecursiveMonitor getTreeClock(ArrayList<TreeClockRecursiveMonitor> arr, Lock l) {
		int lIndex = lockToIndex.get(l);
		return getTreeClockFrom1DArray(arr, lIndex);
	}

	public TreeClockRecursiveMonitor getTreeClock(ArrayList<TreeClockRecursiveMonitor> arr, Variable v) {
		int vIndex = variableToIndex.get(v);
		return getTreeClockFrom1DArray(arr, vIndex);
	}
	
	/*
	public void printThreadClock(){
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for(Thread thread : threadToIndex.keySet()){
			VectorClock C_t = generateVectorClockFromClockThread(thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}
	*/

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}
	
	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}