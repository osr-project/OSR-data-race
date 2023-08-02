package engine.racedetection.dc.treeclock.no_race_AL.no_space_reduction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.Epoch;
import util.treeclock.word_tricks_list.ClockEpochPair;
import util.treeclock.word_tricks_list.TreeClock;
import util.treeclock.word_tricks_list.ForestClock;

//Manages the clocks and other data structures used by the WCP algorithm
public class DCState extends State {

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashSet<Lock> lockSet;
	private HashSet<Variable> variableSet;

	private int numThreads;

	// Data used for algorithm
	public ArrayList<TreeClock> clockThread;
	public HashMap<Lock, HashMap<Variable, ForestClock>> lastReleaseLockReadVariable;
	public HashMap<Lock, HashMap<Variable, TreeClock>> lastReleaseLockWriteVariable;
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

	public void initData(HashSet<Thread> tSet) {
		// initialize WCPThread
		this.clockThread = new ArrayList<TreeClock>();
		for (short i = 0; i < this.numThreads; i++) {
			this.clockThread.add(new TreeClock(i, 1, this.numThreads));
		}

		// initialize lastReleaseLockReadVariable
		this.lastReleaseLockReadVariable = new HashMap<Lock, HashMap<Variable, ForestClock>>();

		// initialize lastReleaseLockWriteVariable
		this.lastReleaseLockWriteVariable = new HashMap<Lock, HashMap<Variable, TreeClock>>();

		// initialize view
		this.view = new DCView(tSet);
	}

	public void initOnlineData() {
		mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	}

	// Access methods
	private TreeClock getTreeClockFrom1DArray(ArrayList<TreeClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	private void checkAndAddLock(Lock l) {
		if (!lockSet.contains(l)) {
			lockSet.add(l);

			this.lastReleaseLockReadVariable.put(l, new HashMap<Variable, ForestClock>());
			this.lastReleaseLockWriteVariable.put(l, new HashMap<Variable, TreeClock>());
			for (Variable v : variableSet) {
				this.lastReleaseLockReadVariable.get(l).put(v,
						new ForestClock(this.numThreads));
				this.lastReleaseLockWriteVariable.get(l).put(v,
						new TreeClock(this.numThreads));
			}
			view.checkAndAddLock(l);
		}
	}

	private void checkAndAddVariable(Variable v) {
		if (!variableSet.contains(v)) {
			variableSet.add(v);

			for (Lock l : lockSet) {
				this.lastReleaseLockReadVariable.get(l).put(v,
						new ForestClock(this.numThreads));
				this.lastReleaseLockWriteVariable.get(l).put(v,
						new TreeClock(this.numThreads));
			}
		}
	}

	public void incClockThread(Thread t) {
		TreeClock C_t = getTreeClock(this.clockThread, t);
		C_t.incrementBy(1);

	}

	public TreeClock getTreeClock(ArrayList<TreeClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getTreeClockFrom1DArray(arr, tIndex);
	}

	public TreeClock getTreeClock(HashMap<Lock, TreeClock> arr, Lock l) {
		checkAndAddLock(l);
		return arr.get(l);
	}

	public TreeClock getTreeClock(HashMap<Variable, TreeClock> arr, Variable v) {
		checkAndAddVariable(v);
		return arr.get(v);
	}

	public <E> E getTreeClock(HashMap<Lock, HashMap<Variable, E>> arr, Lock l,
			Variable v) {
		checkAndAddLock(l);
		checkAndAddVariable(v);

		return arr.get(l).get(v);
	}

	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
		checkAndAddLock(l);
		int tIndex = this.threadToIndex.get(t);
		TreeClock C_t = getTreeClock(clockThread, t);
		long root_data = C_t.root.getData();
		int clk = util.TripletHardCodeWordTricks.getClock(root_data);
		Epoch e = new Epoch(clk, tIndex);
		view.pushClockPair(l, t, new ClockEpochPair(e));
	}

	public void readViewOfWriters(Lock l, Thread t) {
		checkAndAddLock(l);
		TreeClock C_t = getTreeClock(clockThread, t);
		for (Thread tprime : this.threadToIndex.keySet()) {
			if (tprime.equals(t))
				continue;
			TreeClock vc = view.getMaxLowerBound(t, l, tprime, C_t);
			if (vc != null) {
				C_t.join(vc);
			}
		}
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		checkAndAddLock(l);
		TreeClock C_t = getTreeClock(clockThread, t);
		view.updateTopRelease(l, t, C_t);
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