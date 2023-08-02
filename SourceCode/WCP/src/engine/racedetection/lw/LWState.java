package engine.racedetection.lw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClockBitMap;

public class LWState extends State {

	// ==== Internal data ====
	private HashMap<Thread, Integer> threadToIndex;
	// private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;

	// ==== Data used for algorithm ====

	// Data for maintaining the partial order
	public ArrayList<VectorClockBitMap> clockThread;
	public ArrayList<VectorClockBitMap> lastWriteVariable;

	// Data for checking races
	public ArrayList<VectorClockBitMap> readVariable;
	public ArrayList<VectorClockBitMap> writeVariable;
	public HashMap<Thread, HashMap<Lock, Integer>> threadToLockNestingDepth;
	public HashMap<Thread, Integer> threadToOpenLocks; // Number of different locks that
														// have nesting depth > 0.

	// ==== Book-keeping the last-write's location ====
	public ArrayList<Integer> lastWriteVariableLocId;

	// ==== parameter flags ====
	public boolean tickClockOnAccess;
	public int verbosity;

	public LWState(HashSet<Thread> tSet, int verbosity) {
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
			// System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (Integer) this.numThreads);
			this.numThreads++;
		}

		// this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	private void initialize1DArrayOfVectorClocksWithBottom(
			ArrayList<VectorClockBitMap> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClockBitMap(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {

		// initialize clockThread
		this.clockThread = new ArrayList<VectorClockBitMap>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);
		for (int i = 0; i < this.numThreads; i++) {
			VectorClockBitMap C_t = this.clockThread.get(i);
			C_t.setClockIndex(i, 1);
		}

		// initialize lastWriteVariable
		this.lastWriteVariable = new ArrayList<VectorClockBitMap>();

		// initialize readVariable
		this.readVariable = new ArrayList<VectorClockBitMap>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<VectorClockBitMap>();

		// initialize locationIds
		this.lastWriteVariableLocId = new ArrayList<Integer>();

		// initialize threadToLockNestingDepth
		this.threadToLockNestingDepth = new HashMap<Thread, HashMap<Lock, Integer>>();
		for (Thread t : tSet) {
			this.threadToLockNestingDepth.put(t, new HashMap<>());
		}

		// initialize threadToOpenLocks
		this.threadToOpenLocks = new HashMap<Thread, Integer>();
		for (Thread t : tSet) {
			this.threadToOpenLocks.put(t, 0);
		}
	}

	// Access methods
	private VectorClockBitMap getVectorClockFrom1DArray(ArrayList<VectorClockBitMap> arr,
			int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	/*
	private int checkAndAddLock(Lock l) {
		if (!lockToIndex.containsKey(l)) {
			// System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks++;
		}
		return lockToIndex.get(l);
	}
	*/

	private int checkAndAddVariable(Variable v) {
		if (!variableToIndex.containsKey(v)) {
			variableToIndex.put(v, this.numVariables);
			this.numVariables++;
			readVariable.add(new VectorClockBitMap(this.numThreads));
			writeVariable.add(new VectorClockBitMap(this.numThreads));
			lastWriteVariable.add(new VectorClockBitMap(this.numThreads));
			lastWriteVariableLocId.add(-1); // Initialize loc id's to be -1
		}
		return variableToIndex.get(v);
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClockBitMap C_t = getVectorClock(clockThread, t);
		int origVal = C_t.getClockIndex(tIndex);
		C_t.setClockIndex(tIndex, origVal + 1);
	}

	public VectorClockBitMap getVectorClock(ArrayList<VectorClockBitMap> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClockBitMap getVectorClock(ArrayList<VectorClockBitMap> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr, vIndex);
	}

	public int getLWLocId(Variable v) {
		int vIndex = checkAndAddVariable(v);
		return this.lastWriteVariableLocId.get(vIndex);
	}

	public void setLWLocId(Variable v, int loc) {
		int vIndex = checkAndAddVariable(v);
		this.lastWriteVariableLocId.set(vIndex, loc);
	}

	public void setClockIndex(VectorClockBitMap vc, Thread t, int val) {
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}

	public int getClockIndex(VectorClockBitMap vc, Thread t) {
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}
	
	public void setBitIndex(VectorClockBitMap vc, Thread t, Boolean val) {
		int tIndex = threadToIndex.get(t);
		vc.setBitIndex(tIndex, val);
	}

	public Boolean getBitIndex(VectorClockBitMap vc, Thread t) {
		int tIndex = threadToIndex.get(t);
		return vc.getBitIndex(tIndex);
	}

	public void printThreadClock() {
		ArrayList<VectorClockBitMap> printVC = new ArrayList<VectorClockBitMap>();
		for (Thread thread : threadToIndex.keySet()) {
			VectorClockBitMap C_t = getVectorClock(clockThread, thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}

	public boolean isThreadRelevant(Thread t) {
		return this.threadToIndex.containsKey(t);
	}

	public void printMemory() {
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err
				.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}