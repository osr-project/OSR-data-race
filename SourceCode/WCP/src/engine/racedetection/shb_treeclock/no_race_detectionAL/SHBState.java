package engine.racedetection.shb_treeclock.no_race_detectionAL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;
import util.treeclock.word_tricks_list.TreeClock;

public class SHBState extends State {

	// Internal data
	public HashMap<Thread, Short> threadToIndex;
	public HashSet<Short> tids;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	public short numThreads;
	private int numLocks;
	private int numVariables;

	public int numTimesCopyWasDeep, numTimesCopyWasMonotone;
	// Data used for algorithm
	public ArrayList<TreeClock> clockThread;
	public ArrayList<TreeClock> lastReleaseLock;
	public ArrayList<TreeClock> lastWriteVariable;

	// parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	public SHBState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Short>();
		this.tids = new HashSet<Short>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			// System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (short) this.numThreads);
			this.tids.add(this.numThreads);
			this.numThreads++;
		}

		// System.out.println("Initialized " + this.numThreads + " threads");

		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	public void initData(HashSet<Thread> tSet) {

		// initialize clockThread
		this.clockThread = new ArrayList<TreeClock>();
		for (short i = 0; i < this.numThreads; i++) {
			this.clockThread.add(new TreeClock(i, 1, this.numThreads));
		}

		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<TreeClock>();

		// initialize lastWriteVariable
		this.lastWriteVariable = new ArrayList<TreeClock>();

	}

	public void addLock(Lock l) {
		if (!lockToIndex.containsKey(l)) {
			lockToIndex.put(l, this.numLocks);
			this.numLocks++;
			lastReleaseLock.add(new TreeClock(this.numThreads));
		}
	}

	public void addVariable(Variable v) {
		if (!variableToIndex.containsKey(v)) {
			variableToIndex.put(v, this.numVariables);
			this.numVariables++;
			lastWriteVariable.add(new TreeClock(this.numThreads));
		}
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		TreeClock tc_t = clockThread.get(tIndex);
		tc_t.incrementBy(1);
	}

	public TreeClock getTreeClock(ArrayList<TreeClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return arr.get(tIndex);
	}

	public TreeClock getTreeClock(ArrayList<TreeClock> arr, Lock l) {
		int lIndex = lockToIndex.get(l);
		return arr.get(lIndex);
	}

	public TreeClock getTreeClock(ArrayList<TreeClock> arr, Variable v) {
		int vIndex = variableToIndex.get(v);
		return arr.get(vIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Variable v) {
		int vIndex = variableToIndex.get(v);
		return arr.get(vIndex);
	}

	public void setIndex(VectorClock vc, Thread t, int val) {
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}

	public int getIndex(VectorClock vc, Thread t) {
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}

	public void printThreadClock() {
		ArrayList<TreeClock> printVC = new ArrayList<TreeClock>();
		for (Thread thread : threadToIndex.keySet()) {
			TreeClock C_t = getTreeClock(clockThread, thread);
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