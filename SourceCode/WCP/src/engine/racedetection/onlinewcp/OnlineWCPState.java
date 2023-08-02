package engine.racedetection.onlinewcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.ClockPair;
import util.vectorclock.VectorClock;

public class OnlineWCPState extends State {

	// Internal data
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	//private int totThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	private ArrayList<Integer> clockThread;
	private ArrayList<VectorClock> WCPThread;
	public ArrayList<VectorClock> HBPredecessorThread;
	public ArrayList<VectorClock> HBPredecessorLock;
	public ArrayList<VectorClock> lastReleaseLock;
	public ArrayList<VectorClock> WCPPredecessorThread;
	public ArrayList<VectorClock> WCPPredecessorLock;
	public ArrayList<ArrayList<VectorClock>> lastReleaseLockReadVariable;
	public ArrayList<ArrayList<VectorClock>> lastReleaseLockWriteVariable;
	public ArrayList<VectorClock> readVariable;
	public ArrayList<VectorClock> writeVariable;
	/***The next two clocks are relevant for and only for the "forceOrder" feature ***/
	public ArrayList<VectorClock> HBPredecessorReadVariable;
	public ArrayList<VectorClock> HBPredecessorWriteVariable;
	/*********************************************************************************/
	public OnlineWCPView view;
	
	//Data used for online tracking of locks and variables
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<Lock>> mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	
	//Space saving
	private VectorClock local_old_P_t;
	private VectorClock local_vc_imax;
	
	//parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	public OnlineWCPState(HashSet<Thread> tSet, boolean forceOrder, boolean tickClockOnAccess, int verbosity) {
		this.forceOrder = forceOrder;
		this.tickClockOnAccess = tickClockOnAccess;
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
		initOnlineData();	
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
		
		local_old_P_t = new VectorClock(this.numThreads);
		local_vc_imax = new VectorClock(this.numThreads);
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len) {
		// arr = new ArrayList<VectorClock>();
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	/*
	private void initialize2DArrayOfVectorClocksWithBottom(ArrayList<ArrayList<VectorClock>> arr, int len1, int len2) {
		// arr = new ArrayList<ArrayList<VectorClock>>();
		for (int i = 0; i < len1; i++) {
			ArrayList<VectorClock> arr_i = new ArrayList<VectorClock>();
			for (int j = 0; j < len2; j++) {
				arr_i.add(new VectorClock(this.totThreads));
			}
			arr.add(arr_i);
		}
	}
	*/

	public void initData(HashSet<Thread> tSet) {
		// Initialize clockThread
		this.clockThread = new ArrayList<Integer>();
		for (int i = 0; i < this.numThreads; i++) {
			this.clockThread.add((Integer)1);
		}
		
		// initialize WCPThread
		this.WCPThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.WCPThread, this.numThreads);

		// initialize HBPredecessorThread
		this.HBPredecessorThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.HBPredecessorThread, this.numThreads);

		// initialize HBPredecessorLock
		this.HBPredecessorLock = new ArrayList<VectorClock>();

		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<VectorClock>();

		// initialize WCPPredecessorThread
		this.WCPPredecessorThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.WCPPredecessorThread, this.numThreads);

		// initialize WCPPredecessorLock
		this.WCPPredecessorLock = new ArrayList<VectorClock>();

		// initialize lastReleaseLockReadVariable
		this.lastReleaseLockReadVariable = new ArrayList<ArrayList<VectorClock>>();

		// initialize lastReleaseLockWriteVariable
		this.lastReleaseLockWriteVariable = new ArrayList<ArrayList<VectorClock>>();

		// initialize readVariable
		this.readVariable = new ArrayList<VectorClock>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<VectorClock>();
		
		// initialize HBPredecessorReadVariable
		this.HBPredecessorReadVariable = new ArrayList<VectorClock>();

		// initialize HBPredecessorWriteVariable
		this.HBPredecessorWriteVariable = new ArrayList<VectorClock>();

		// initialize view
		this.view = new OnlineWCPView(tSet);
	}
	
	public void initOnlineData(){
		mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	}

	/*
	public void initLazyData(){
		lazyHB = new HashMap<Thread, VectorClock> ();
		lazyWCP = new HashMap<Thread, VectorClock> ();
	}
	*/
	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	private VectorClock getVectorClockFrom2DArray(ArrayList<ArrayList<VectorClock>> arr, int index1, int index2) {
		if (index1 < 0 || index1 >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		if (index2 < 0 || index2 >= arr.get(index1).size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index1).get(index2);
	}

	/*
	private int checkAndAddThread(Thread t){
		if(!threadToIndex.containsKey(t)){
			//System.out.println("Map : " + threadToIndex.toString());
			//System.out.println("Thread : " + t.toString());
			threadToIndex.put(t, this.numThreads);
			this.numThreads ++;
			
			if(this.numThreads > this.totThreads){
				System.out.println("Number of threads exceed the max. limit");
			}
			
			clockThread.add((Integer)1);
			HBPredecessorThread	.add(new VectorClock(this.totThreads));
			WCPPredecessorThread.add(new VectorClock(this.totThreads));
			view.checkAndAddThread(t);
		}
		return threadToIndex.get(t);
	}
	*/
	
	private int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			//System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			
			HBPredecessorLock	.add(new VectorClock(this.numThreads));
			lastReleaseLock		.add(new VectorClock(this.numThreads));
			WCPPredecessorLock	.add(new VectorClock(this.numThreads));
			
			lastReleaseLockReadVariable.add(new ArrayList<VectorClock> ());
			lastReleaseLockWriteVariable.add(new ArrayList<VectorClock> ());
			
			for (int i = 0; i < this.numVariables; i++) {
				lastReleaseLockReadVariable	.get(this.numLocks-1).add(new VectorClock(this.numThreads));
				lastReleaseLockWriteVariable.get(this.numLocks-1).add(new VectorClock(this.numThreads));
			}
			view.checkAndAddLock(l);
		}
		return lockToIndex.get(l);
	}
	
	private int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			
			for (int i = 0; i < this.numLocks; i++) {
				lastReleaseLockReadVariable	.get(i).add(new VectorClock(this.numThreads));
				lastReleaseLockWriteVariable.get(i).add(new VectorClock(this.numThreads));
			}
			
			readVariable	.add(new VectorClock(this.numThreads));
			writeVariable	.add(new VectorClock(this.numThreads));
			
			HBPredecessorReadVariable	.add(new VectorClock(this.numThreads));
			HBPredecessorWriteVariable	.add(new VectorClock(this.numThreads));
		}
		return variableToIndex.get(v);
	}

	public int getClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		return clockThread.get(tIndex);
	}
	
	public VectorClock generateVectorClockFromClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		//System.out.println("numThreads = " + this.numThreads);
		VectorClock wcpClock = getVectorClock(WCPThread, t);
		VectorClock pred = getVectorClock(WCPPredecessorThread, t);
		//System.out.println("numThreads = " + this.numThreads);
		int tValue = getClockThread(t);
		
		wcpClock.copyFrom(pred);
		wcpClock.setClockIndex(tIndex, tValue);
		return wcpClock;
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		int origVal = clockThread.get(tIndex);
		clockThread.set(tIndex, (Integer)(origVal + 1));
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Lock l) {
		int lIndex = checkAndAddLock(l);
		return getVectorClockFrom1DArray(arr, lIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr, vIndex);
	}

	public VectorClock getVectorClock(ArrayList<ArrayList<VectorClock>> arr, Lock l, Variable v) {
		int lIndex = checkAndAddLock(l);
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom2DArray(arr, lIndex, vIndex);
	}
	
	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
		//System.out.println("updateViewAsWriterAtAcquire : (t, l) = (" + tIndex + ", " + lIndex + ")" );
		VectorClock C_t = generateVectorClockFromClockThread(t);
		view.pushClockPair(t, l, new ClockPair(C_t));
	}

	public void readViewOfWriters(Lock l, Thread t) {
		//int lIndex = checkAndAddLock(l);
		//int tIndex = threadToIndex.get(t);
		//System.out.println("readViewOfWriters : (t, l) = (" + tIndex + ", " + lIndex + ")" );

		local_old_P_t.setToZero();
		local_vc_imax.setToZero();
		while (true) {
			VectorClock P_t = getVectorClock(WCPPredecessorThread, t);
			VectorClock C_t = generateVectorClockFromClockThread(t);
			local_old_P_t.copyFrom(P_t);

			for (Thread tPrime: threadToIndex.keySet()) {
				view.getMaxLowerBound(tPrime, t, l, C_t, local_vc_imax);
				P_t.updateWithMax(P_t, local_vc_imax);	
			}
			
			if (local_old_P_t.isEqual(P_t)) {
				break;
			}
		}
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		//int lIndex = checkAndAddLock(l);
		//int tIndex = threadToIndex.get(t);
		//System.out.println("updateViewAsWriterAtRelease : (t, l) = (" + tIndex + ", " + lIndex + ")" );
		VectorClock C_t = generateVectorClockFromClockThread(t);
		VectorClock H_t = getVectorClock(HBPredecessorThread, t);
		view.updateTopRelease(t, l, C_t, H_t);
	}
	
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
	
	public <T> HashSet<T> stackToSet(Stack<T> stack){
		HashSet<T> set = new HashSet<T>();
		for(T t : stack){
			set.add(t);
		}
		return set;
	}
	
	public HashSet<Lock> getSetFromStack(Thread t){
		return stackToSet(this.mapThreadLockStack.get(t));
	}
	
	public boolean isLockAcquired(Thread t, Lock l){
		return this.getSetFromStack(t).contains(l);
	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}
	
	public void printViewSize(){
		this.view.printSize();
	}
	
	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err.println("Number of variables = " + Integer.toString(this.numVariables));
		this.view.printSize();
	}
}