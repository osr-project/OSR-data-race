package engine.atomicity.incorrect_conflictserializability.vc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.atomicity.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Pair;
import util.vectorclock.VectorClock;

public class VCConflictSerializabilityState extends State {

	// Internal data
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<VectorClock> clockThread;
	public ArrayList<VectorClock> lastReleaseLock;
	public ArrayList<VectorClock> readVariable;
	public ArrayList<VectorClock> writeVariable;
	public ArrayList<Pair<Integer, Integer>> blameInterval;
	public ArrayList<VectorClock> checkLastReleaseLock;
	public ArrayList<VectorClock> checkReadVariable;
	public ArrayList<VectorClock> checkWriteVariable;
	
	//parameter flags
	public int verbosity;

	public VCConflictSerializabilityState(HashSet<Thread> tSet, int verbosity) {
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
		
		// initialize clockThread
		this.clockThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);		
		for (int i = 0; i < this.numThreads; i++) {
			VectorClock C_t = this.clockThread.get(i);
			C_t.setClockIndex(i, 1);
		}
		
		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<VectorClock>();

		// initialize readVariable
		this.readVariable = new ArrayList<VectorClock>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<VectorClock>();
		
		// initialize lastWriteVariable
		this.blameInterval = new ArrayList<Pair<Integer, Integer>>();
		for (int i = 0; i < this.numThreads; i++) {
			blameInterval.add(new Pair<Integer, Integer> (-1, -1));
		}
		
		// initialize lastReleaseLock
		this.checkLastReleaseLock = new ArrayList<VectorClock>();

		// initialize readVariable
		this.checkReadVariable = new ArrayList<VectorClock>();

		// initialize writeVariable
		this.checkWriteVariable = new ArrayList<VectorClock>();
	}
	
	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}
	
	private int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			//System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			lastReleaseLock.add(new VectorClock(this.numThreads));
			checkLastReleaseLock.add(new VectorClock(this.numThreads));
		}
		return lockToIndex.get(l);
	}
	
	private int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			readVariable			.add(new VectorClock(this.numThreads));
			writeVariable			.add(new VectorClock(this.numThreads));
			checkReadVariable		.add(new VectorClock(this.numThreads));
			checkWriteVariable		.add(new VectorClock(this.numThreads));
		}
		return variableToIndex.get(v);
	}
	
	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = getVectorClock(clockThread, t);
		int origVal = C_t.getClockIndex(tIndex);
		C_t.setClockIndex(tIndex, origVal + 1);
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

	public void setIndex(VectorClock vc, Thread t, int val){
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}
	
	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}
	
	public boolean isEmptyBlame(Thread t){
		int tIndex = threadToIndex.get(t);
		Pair<Integer, Integer> interval = this.blameInterval.get(tIndex);
		if(interval.second == -1){
			return true;
		}
		else return false;
	}
	
	public boolean containsBlame(Thread t, int val){
		int tIndex = threadToIndex.get(t);
		Pair<Integer, Integer> interval = this.blameInterval.get(tIndex);
		if(interval.second == -1){
//			throw new IllegalArgumentException("Blame Interval is empty. Cannot check containment");
			return false;
		}
		else{
			if(interval.first <= val && interval.second >= val){
				return true;
			}
			else{
				return false;
			}
		}
	}
	
	public void pushBlame(Thread t, int clock_val){
		int tIndex = threadToIndex.get(t);
		Pair<Integer, Integer> interval = this.blameInterval.get(tIndex);
		if(interval.second == -1){
			interval.first = clock_val;
		}
		interval.second = clock_val;
	}
	
	public void popBlame(Thread t){
		int tIndex = threadToIndex.get(t);
		Pair<Integer, Integer> interval = this.blameInterval.get(tIndex);
		if(interval.second == -1){
			throw new IllegalArgumentException("Blame Interval is empty. Cannot pop");
		}
		else if(interval.first == interval.second){
			interval.first = -1;
			interval.second = -1;
		}
		else{
			interval.second = interval.second-1;
		}
	}
	
	//Updates vc1 as -> vc1 := \lambda t' . if t' == t then vc1[t] else max(vc1[t'], vc2[t'])
	public void updateCheckClock(VectorClock vc1, VectorClock vc2, Thread t){
		int tIndex = threadToIndex.get(t);
		vc1.updateMax2WithoutLocal(vc2, tIndex);
	}
	
	public void printThreadClock(){
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for(Thread thread : threadToIndex.keySet()){
			VectorClock C_t = getVectorClock(clockThread, thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
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
