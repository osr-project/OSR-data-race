package engine.racedetection.fasthb_treeclock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Pair;
import util.treeclock.iterative.TreeClock;

public class FastHBState extends State {

	// Internal data
	public HashMap<Thread, Short> threadToIndex;
	private HashSet<Short> tids;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private short numThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<TreeClock> clockThread;
	public ArrayList<TreeClock> lastReleaseLock;

	public ArrayList<Pair<Short, Integer>> epochWriteVariable; // t@c
	public ArrayList<HashMap<Short, Integer>> epochReadVariable; // t->c

	// parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	public FastHBState(HashSet<Thread> tSet, boolean forceOrder,
			boolean tickClockOnAccess, int verbosity) {
		this.forceOrder = forceOrder;
		this.tickClockOnAccess = tickClockOnAccess;
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
			this.threadToIndex.put(thread, this.numThreads);
			this.tids.add(this.numThreads);
			this.numThreads++;
		}

		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	public void initData(HashSet<Thread> tSet) {
		// Initialize clockThread
		this.clockThread = new ArrayList<TreeClock>();
		for (short i = 0; i < this.numThreads; i++) {
			this.clockThread.add(new TreeClock(this.tids, i, (short) 1));
		}

		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<TreeClock>();

		// initialize epochWriteVariable
		this.epochWriteVariable = new ArrayList<Pair<Short, Integer>>();

		// initialize epochReadVariable
		this.epochReadVariable = new ArrayList<HashMap<Short, Integer>>();
	}

	/*
	 * // Access methods private TreeClock
	 * getTreeClockFrom1DArray(ArrayList<TreeClock> arr, int index) { if (index < 0
	 * || index >= arr.size()) { throw new
	 * IllegalArgumentException("Illegal Out of Bound access"); } return
	 * arr.get(index); }
	 * 
	 * private TreeClock getTreeClockFrom1DArray(ArrayList<TreeClock> arr, short
	 * index) { if (index < 0 || index >= arr.size()) { throw new
	 * IllegalArgumentException("Illegal Out of Bound access"); } return
	 * arr.get(index); }
	 */

	public int checkAndAddLock(Lock l, Thread t) {
		if (!lockToIndex.containsKey(l)) {
			lockToIndex.put(l, this.numLocks);
			this.numLocks++;
			short tid = this.threadToIndex.get(t);
			lastReleaseLock.add(new TreeClock(this.tids, tid, (short) 0));
		}
		return lockToIndex.get(l);
	}

	public void addLock(Lock l, Thread t) {
		if (!lockToIndex.containsKey(l)) {
			lockToIndex.put(l, this.numLocks);
			this.numLocks++;
			short tid = this.threadToIndex.get(t);
			lastReleaseLock.add(new TreeClock(this.tids, tid, (short) 0));
		}
	}

	public int checkAndAddVariable(Variable v) {
		if (!variableToIndex.containsKey(v)) {
			variableToIndex.put(v, this.numVariables);
			this.numVariables++;
			epochWriteVariable.add(null);
			epochReadVariable.add(new HashMap<Short, Integer>());
		}
		return variableToIndex.get(v);
	}

	public void addVariable(Variable v) {
		if (!variableToIndex.containsKey(v)) {
			variableToIndex.put(v, this.numVariables);
			this.numVariables++;
			epochWriteVariable.add(null);
			epochReadVariable.add(new HashMap<Short, Integer>());
		}
	}

	/*
	 * public int getClockThread(Thread t) { int tIndex = threadToIndex.get(t);
	 * return clockThread.getLocalClock(tIndex); }
	 * 
	 * public VectorClock generateVectorClockFromClockThread(Thread t) { int tIndex
	 * = threadToIndex.get(t);
	 * 
	 * int tValue = getClockThread(t); VectorClock pred =
	 * getVectorClock(HBPredecessorThread, t); VectorClock hbClock = new
	 * VectorClock(pred);
	 * 
	 * hbClock.setClockIndex(tIndex, tValue); return hbClock; }
	 */

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		TreeClock tc_t = clockThread.get(tIndex);
		tc_t.incrementBy(1);
	}

	/*
	 * public TreeClock getTreeClock(ArrayList<TreeClock> arr, Thread t) { short
	 * tIndex = threadToIndex.get(t); return getTreeClockFrom1DArray(arr, tIndex); }
	 * 
	 * public TreeClock getTreeClock(ArrayList<TreeClock> arr, Lock l) { int lIndex
	 * = lockToIndex.get(l); return getTreeClockFrom1DArray(arr, lIndex); }
	 */

	public <E> E getClock(ArrayList<E> arr, Thread t) {
		short tIndex = threadToIndex.get(t);
		return arr.get(tIndex);
	}

	public <E> E getClock(ArrayList<E> arr, Lock l) {
		int lIndex = lockToIndex.get(l);
		return arr.get(lIndex);
	}

	public <E> E getEpochs(ArrayList<E> arr, Variable v) {
		int vIndex = variableToIndex.get(v);
		return arr.get(vIndex);
	}

	public void setWriteEpoch(Variable v, Pair<Short, Integer> epoch) {
		int vIndex = variableToIndex.get(v);
		this.epochWriteVariable.set(vIndex, epoch);
	}

	/*
	 * public void printThreadClock(){ ArrayList<VectorClock> printVC = new
	 * ArrayList<VectorClock>(); for(Thread thread : threadToIndex.keySet()){
	 * VectorClock C_t = generateVectorClockFromClockThread(thread);
	 * printVC.add(C_t); } System.out.println(printVC); System.out.println();
	 * System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%"); }
	 */

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