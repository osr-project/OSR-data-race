package engine.racedetection.dc.no_race.no_space_reduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.ClockEpochPair;
import util.vectorclock.Epoch;
import util.vectorclock.VectorClock;

//Manages the clocks and other data structures used by the WCP algorithm
public class DCState extends State {

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashSet<Lock> lockSet;
	private HashSet<Variable> variableSet;

	private int numThreads;

	// Data used for algorithm
	public ArrayList<VectorClock> clockThread;
	public HashMap<Lock, HashMap<Variable, VectorClock>> lastReleaseLockReadVariable;
	public HashMap<Lock, HashMap<Variable, VectorClock>> lastReleaseLockWriteVariable;
	public DCView view;

	// Data used for online tracking of locks and variables
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<Lock>> mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();

	// parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	public DCState(HashSet<Thread> tSet, boolean forceOrder, boolean tickClockOnAccess,
			int verbosity) {
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
			this.threadToIndex.put(thread, (Integer) this.numThreads);
			this.numThreads++;
		}

		this.lockSet = new HashSet<Lock>();
		this.variableSet = new HashSet<Variable>();
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr,
			int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {
		// initialize WCPThread
		this.clockThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);
		for (int i = 0; i < this.numThreads; i++) {
			VectorClock C_t = this.clockThread.get(i);
			C_t.setClockIndex(i, 1);
		}

		// initialize lastReleaseLockReadVariable
		this.lastReleaseLockReadVariable = new HashMap<Lock, HashMap<Variable, VectorClock>>();

		// initialize lastReleaseLockWriteVariable
		this.lastReleaseLockWriteVariable = new HashMap<Lock, HashMap<Variable, VectorClock>>();

		// initialize view
		this.view = new DCView(tSet);
	}

	public void initOnlineData() {
		mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	}

	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	private void checkAndAddLock(Lock l) {
		if (!lockSet.contains(l)) {
			lockSet.add(l);

			this.lastReleaseLockReadVariable.put(l, new HashMap<Variable, VectorClock>());
			this.lastReleaseLockWriteVariable.put(l,
					new HashMap<Variable, VectorClock>());
			for (Variable v : this.variableSet) {
				this.lastReleaseLockReadVariable.get(l).put(v,
						new VectorClock(this.numThreads));
				this.lastReleaseLockWriteVariable.get(l).put(v,
						new VectorClock(this.numThreads));
			}
			view.checkAndAddLock(l);
		}
	}

	private void checkAndAddVariable(Variable v) {
		if (!variableSet.contains(v)) {
			variableSet.add(v);
			for (Lock l : lockSet) {
				this.lastReleaseLockReadVariable.get(l).put(v,
						new VectorClock(this.numThreads));
				this.lastReleaseLockWriteVariable.get(l).put(v,
						new VectorClock(this.numThreads));
			}
		}
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = getVectorClock(this.clockThread, t);
		int origVal = C_t.getClockIndex(tIndex);
		C_t.setClockIndex(tIndex, origVal + 1);

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

	public VectorClock getVectorClock(HashMap<Lock, HashMap<Variable, VectorClock>> arr,
			Lock l, Variable v) {
		checkAndAddLock(l);
		checkAndAddVariable(v);
		return arr.get(l).get(v);
	}

	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
		checkAndAddLock(l);
		int tIndex = this.threadToIndex.get(t);
		VectorClock C_t = getVectorClock(clockThread, t);
		Epoch e = new Epoch(C_t.getClockIndex(tIndex), tIndex);
		view.pushClockPair(l, t, new ClockEpochPair(e, new VectorClock(this.numThreads)));
	}

	public void readViewOfWriters(Lock l, Thread t) {
		checkAndAddLock(l);
		VectorClock C_t = getVectorClock(clockThread, t);
		for (Thread tprime : this.threadToIndex.keySet()) {
			if (tprime.equals(t))
				continue;
			VectorClock vc = view.getMaxLowerBound(t, l, tprime, C_t);
			C_t.updateWithMax(C_t, vc);
		}
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		checkAndAddLock(l);
		VectorClock C_t = getVectorClock(clockThread, t);
		view.updateTopRelease(l, t, C_t);
	}

	public void printThreadClock() {
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for (Thread thread : threadToIndex.keySet()) {
			VectorClock C_t = getVectorClock(clockThread, thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}

	public <T> HashSet<T> stackToSet(Stack<T> stack) {
		HashSet<T> set = new HashSet<T>();
		for (T t : stack) {
			set.add(t);
		}
		return set;
	}

	public HashSet<Lock> getSetFromStack(Thread t) {
		return stackToSet(this.mapThreadLockStack.get(t));
	}

	public boolean isLockAcquired(Thread t, Lock l) {
		checkAndAddLock(l);
		return this.getSetFromStack(t).contains(l);
	}

	public boolean isThreadRelevant(Thread t) {
		return this.threadToIndex.containsKey(t);
	}

	public void printViewSize() {
		this.view.printSize();
	}

	public void printMemory() {
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.lockSet.size()));
		System.err.println(
				"Number of variables = " + Integer.toString(this.variableSet.size()));
		this.view.printSize();
	}
}