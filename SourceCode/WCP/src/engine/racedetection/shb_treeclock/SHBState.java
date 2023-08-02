package engine.racedetection.shb_treeclock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.treeclock.iterative.TreeClock;
import util.vectorclock.VectorClock;

public class SHBState extends State {

	// Internal data
	public HashMap<Thread, Short> threadToIndex;
	public HashSet<Short> tids;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private short numThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<TreeClock> clockThread;
	public ArrayList<TreeClock> lastReleaseLock;
	public ArrayList<TreeClock> lastWriteVariable;

	public ArrayList<VectorClock> readVariable;
	public ArrayList<VectorClock> writeVariable;

	// Book-keeping the last-write's location
	public ArrayList<Integer> lastWriteVariableLocId;

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

		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	public void initData(HashSet<Thread> tSet) {

		// initialize clockThread
		this.clockThread = new ArrayList<TreeClock>();
		for (short i = 0; i < this.numThreads; i++) {
			this.clockThread.add(new TreeClock(i, 1));
		}

		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<TreeClock>();

		// initialize lastWriteVariable
		this.lastWriteVariable = new ArrayList<TreeClock>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<VectorClock>();

		// initialize readVariable
		this.readVariable = new ArrayList<VectorClock>();

		// initialize locationIds
		this.lastWriteVariableLocId = new ArrayList<Integer>();
	}

	public void addLock(Lock l) {
		if (!lockToIndex.containsKey(l)) {
			lockToIndex.put(l, this.numLocks);
			this.numLocks++;
			lastReleaseLock.add(new TreeClock());
		}
	}

	public void addVariable(Variable v) {
		if (!variableToIndex.containsKey(v)) {
			variableToIndex.put(v, this.numVariables);
			this.numVariables++;
			lastWriteVariable.add(new TreeClock());
			readVariable.add(new VectorClock(this.numThreads));
			writeVariable.add(new VectorClock(this.numThreads));
			lastWriteVariableLocId.add(-1); // Initialize loc id's to be -1
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

	public int getLWLocId(Variable v) {
		int vIndex = variableToIndex.get(v);
		return this.lastWriteVariableLocId.get(vIndex);
	}

	public void setLWLocId(Variable v, int loc) {
		int vIndex = variableToIndex.get(v);
		this.lastWriteVariableLocId.set(vIndex, loc);
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