package engine.racedetection.wcp.treeclock.no_race_HM;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.TripletHardCodeWordTricks;
import util.treeclock.word_tricks_map.ClockEpochPair;
import util.treeclock.word_tricks_map.TreeClock;
import util.vectorclock.Epoch;

//Manages the clocks and other data structures used by the WCP algorithm
public class WCPState extends State {

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashSet<Lock> lockSet;
	private HashSet<Variable> variableSet;

	private int numThreads;

	// Data used for algorithm
	private ArrayList<Integer> clockThread;
	public ArrayList<TreeClock> HBPredecessorThread;
	public HashMap<Lock, TreeClock> HBPredecessorLock;
	public ArrayList<TreeClock> WCPPredecessorThread;
	public HashMap<Lock, TreeClock> WCPPredecessorLock;
	public HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, TreeClock>>>> lastReleaseLockReadVariableThread;
	public HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, TreeClock>>>> lastReleaseLockWriteVariableThread;
	public WCPView view;

	// Data used for online tracking of locks and variables
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<Lock>> mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();

	// Data for offline space saving
	public HashMap<String, HashMap<String, HashSet<String>>> existsLockReadVariableThreads;
	public HashMap<String, HashMap<String, HashSet<String>>> existsLockWriteVariableThreads;

	public HashMap<String, Long> variableToReadEquivalenceClass;
	public HashMap<String, Long> variableToWriteEquivalenceClass;

	// parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	public WCPState(HashSet<Thread> tSet, boolean forceOrder, boolean tickClockOnAccess,
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
		// Initialize clockThread
		this.clockThread = new ArrayList<Integer>();
		for (int i = 0; i < this.numThreads; i++) {
			this.clockThread.add((Integer) 1);
		}

		// initialize HBPredecessorThread
		this.HBPredecessorThread = new ArrayList<TreeClock>();
		for (short i = 0; i < this.numThreads; i++) {
			this.HBPredecessorThread.add(new TreeClock(i, 1, this.numThreads));
		}

		// initialize HBPredecessorLock
		this.HBPredecessorLock = new HashMap<Lock, TreeClock>();

		// initialize WCPPredecessorThread
		this.WCPPredecessorThread = new ArrayList<TreeClock>();
		for (short i = 0; i < this.numThreads; i++) {
			this.WCPPredecessorThread.add(new TreeClock(i, this.numThreads));
		}

		// initialize WCPPredecessorLock
		this.WCPPredecessorLock = new HashMap<Lock, TreeClock>();

		// initialize lastReleaseLockReadVariable
		this.lastReleaseLockReadVariableThread = new HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, TreeClock>>>>();

		// initialize lastReleaseLockWriteVariable
		this.lastReleaseLockWriteVariableThread = new HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, TreeClock>>>>();

		// initialize view
		this.view = new WCPView(tSet);
	}

	public void initOnlineData() {
		mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	}

	// Access methods
	private TreeClock getTreeClockFrom1DArray(ArrayList<TreeClock> arr, int index) {
		return arr.get(index);
	}

	private void checkAndAddLock(Lock l) {
		if (!lockSet.contains(l)) {
			lockSet.add(l);

			HBPredecessorLock.put(l, new TreeClock(this.numThreads));
			WCPPredecessorLock.put(l, new TreeClock(this.numThreads));

			String lName = l.getName();
			if (existsLockReadVariableThreads.containsKey(lName)) {
				this.lastReleaseLockReadVariableThread.put(l,
						new HashMap<Long, HashMap<Long, HashMap<Thread, TreeClock>>>());
				for (Variable v : variableSet) {
					String vName = v.getName();
					Long vEquivRead = this.variableToReadEquivalenceClass.get(vName);
					Long vEquivWrite = this.variableToWriteEquivalenceClass.get(vName);
					if (existsLockReadVariableThreads.get(lName).containsKey(vName)) {
						if (!lastReleaseLockReadVariableThread.get(l)
								.containsKey(vEquivRead)) {
							this.lastReleaseLockReadVariableThread.get(l).put(vEquivRead,
									new HashMap<Long, HashMap<Thread, TreeClock>>());
						}
						if (!lastReleaseLockReadVariableThread.get(l).get(vEquivRead)
								.containsKey(vEquivWrite)) {
							this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead)
									.put(vEquivWrite, new HashMap<Thread, TreeClock>());
						}
						for (Thread t : this.threadToIndex.keySet()) {
							if (existsLockReadVariableThreads.get(lName).get(vName)
									.contains(t.getName())) {
								this.lastReleaseLockReadVariableThread.get(l)
										.get(vEquivRead).get(vEquivWrite)
										.put(t, new TreeClock(this.numThreads));
							}
						}

					}

				}
			}

			if (existsLockWriteVariableThreads.containsKey(lName)) {
				this.lastReleaseLockWriteVariableThread.put(l,
						new HashMap<Long, HashMap<Long, HashMap<Thread, TreeClock>>>());
				for (Variable v : variableSet) {
					String vName = v.getName();
					Long vEquivRead = this.variableToReadEquivalenceClass.get(vName);
					Long vEquivWrite = this.variableToWriteEquivalenceClass.get(vName);
					if (existsLockWriteVariableThreads.get(lName).containsKey(vName)) {
						if (!lastReleaseLockWriteVariableThread.get(l)
								.containsKey(vEquivRead)) {
							this.lastReleaseLockWriteVariableThread.get(l).put(vEquivRead,
									new HashMap<Long, HashMap<Thread, TreeClock>>());
						}
						if (!lastReleaseLockWriteVariableThread.get(l).get(vEquivRead)
								.containsKey(vEquivWrite)) {
							this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead)
									.put(vEquivWrite, new HashMap<Thread, TreeClock>());
						}
						for (Thread t : this.threadToIndex.keySet()) {
							if (existsLockWriteVariableThreads.get(lName).get(vName)
									.contains(t.getName())) {
								this.lastReleaseLockWriteVariableThread.get(l)
										.get(vEquivRead).get(vEquivWrite)
										.put(t, new TreeClock(this.numThreads));
							}
						}
					}
				}
			}

			view.checkAndAddLock(l);
		}
	}

	private void checkAndAddVariable(Variable v) {
		if (!variableSet.contains(v)) {
			variableSet.add(v);

			Long vEquivRead = this.variableToReadEquivalenceClass.get(v.getName());
			Long vEquivWrite = this.variableToWriteEquivalenceClass.get(v.getName());

			for (Lock l : lockSet) {
				if (existsLockReadVariableThreads.containsKey(l.getName())) {
					if (existsLockReadVariableThreads.get(l.getName())
							.containsKey(v.getName())) {
						if (!this.lastReleaseLockReadVariableThread.get(l)
								.containsKey(vEquivRead)) {
							this.lastReleaseLockReadVariableThread.get(l).put(vEquivRead,
									new HashMap<Long, HashMap<Thread, TreeClock>>());
						}
						if (!this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead)
								.containsKey(vEquivWrite)) {
							this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead)
									.put(vEquivWrite, new HashMap<Thread, TreeClock>());
						}
						HashSet<String> threads = existsLockReadVariableThreads
								.get(l.getName()).get(v.getName());
						for (Thread t : this.threadToIndex.keySet()) {
							if (threads.contains(t.getName())) {
								if (!lastReleaseLockReadVariableThread.get(l)
										.get(vEquivRead).get(vEquivWrite)
										.containsKey(t)) {
									this.lastReleaseLockReadVariableThread.get(l)
											.get(vEquivRead).get(vEquivWrite)
											.put(t, new TreeClock(this.numThreads));
								}
							}
						}
					}
				}

				if (existsLockWriteVariableThreads.containsKey(l.getName())) {
					if (existsLockWriteVariableThreads.get(l.getName())
							.containsKey(v.getName())) {
						if (!this.lastReleaseLockWriteVariableThread.get(l)
								.containsKey(vEquivRead)) {
							this.lastReleaseLockWriteVariableThread.get(l).put(vEquivRead,
									new HashMap<Long, HashMap<Thread, TreeClock>>());
						}
						if (!this.lastReleaseLockWriteVariableThread.get(l)
								.get(vEquivRead).containsKey(vEquivWrite)) {
							this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead)
									.put(vEquivWrite, new HashMap<Thread, TreeClock>());
						}
						HashSet<String> threads = existsLockWriteVariableThreads
								.get(l.getName()).get(v.getName());
						for (Thread t : this.threadToIndex.keySet()) {
							if (threads.contains(t.getName())) {
								if (!lastReleaseLockWriteVariableThread.get(l)
										.get(vEquivRead).get(vEquivWrite)
										.containsKey(t)) {
									this.lastReleaseLockWriteVariableThread.get(l)
											.get(vEquivRead).get(vEquivWrite)
											.put(t, new TreeClock(this.numThreads));
								}
							}
						}
					}
				}
			}
		}
	}

	public int getClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		return clockThread.get(tIndex);
	}

	public TreeClock generateVectorClockFromClockThread(Thread t) {
		TreeClock pred = getTreeClock(WCPPredecessorThread, t);
		TreeClock wcpClock = new TreeClock(pred);
		long root_data = wcpClock.root.getData();
		int tValue = getClockThread(t);
		long new_data = TripletHardCodeWordTricks.setClock(tValue, root_data);
		wcpClock.root.setData(new_data);
		return wcpClock;
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		int origVal = clockThread.get(tIndex);
		clockThread.set(tIndex, (Integer) (origVal + 1));
		TreeClock H_t = getTreeClock(this.HBPredecessorThread, t);
		H_t.incrementBy(1);

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

	public TreeClock getTreeClock(
			HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, TreeClock>>>> arr,
			Lock l, Variable v, Thread t) {
		checkAndAddLock(l);
		checkAndAddVariable(v);

		Long vEquivRead = this.variableToReadEquivalenceClass.get(v.getName());
		Long vEquivWrite = this.variableToWriteEquivalenceClass.get(v.getName());
		if (!arr.containsKey(l))
			throw new IllegalArgumentException("No l found");
		if (!arr.get(l).containsKey(vEquivRead))
			throw new IllegalArgumentException("No vEquivRead");
		if (!arr.get(l).get(vEquivRead).containsKey(vEquivWrite))
			throw new IllegalArgumentException("No vEquivRead");
		if (!arr.get(l).get(vEquivRead).get(vEquivWrite).containsKey(t))
			throw new IllegalArgumentException("No t found");

		return arr.get(l).get(vEquivRead).get(vEquivWrite).get(t);
	}

	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
		checkAndAddLock(l);
		if (!this.view.lockThreadLastInteraction.get(l.getName())
				.containsKey(t.getName())) {
			throw new IllegalArgumentException(
					"Invalid operation : No critical section on lock " + l.getName()
							+ " in thread " + t.getName());
		}
		int tIndex = this.threadToIndex.get(t);
		int clk = this.clockThread.get(tIndex);
		Epoch e = new Epoch(clk, tIndex);
		view.pushClockPair(l, new ClockEpochPair(e));
	}

	public void readViewOfWriters(Lock l, Thread t) {
		checkAndAddLock(l);
		if (!this.view.lockThreadLastInteraction.get(l.getName())
				.containsKey(t.getName())) {
			throw new IllegalArgumentException(
					"Invalid operation : No critical section on lock " + l.getName()
							+ " in thread " + t.getName());
		}
		TreeClock P_t = getTreeClock(WCPPredecessorThread, t);
		TreeClock C_t = generateVectorClockFromClockThread(t);
		TreeClock vc = view.getMaxLowerBound(t, l, C_t);
		if (vc != null) {
			P_t.join(vc);
		}
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		checkAndAddLock(l);
		if (!this.view.lockThreadLastInteraction.get(l.getName())
				.containsKey(t.getName())) {
			throw new IllegalArgumentException(
					"Invalid operation : No critical section on lock " + l.getName()
							+ " in thread " + t.getName());
		}
		TreeClock H_t = getTreeClock(HBPredecessorThread, t);
		view.updateTopRelease(l, H_t);
	}

	public void printThreadClock() {
		ArrayList<TreeClock> printVC = new ArrayList<TreeClock>();
		for (Thread thread : threadToIndex.keySet()) {
			TreeClock C_t = generateVectorClockFromClockThread(thread);
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
		if (!this.view.lockThreadLastInteraction.get(l.getName())
				.containsKey(t.getName())) {
			throw new IllegalArgumentException(
					"Invalid operation : No critical section on lock " + l.getName()
							+ " in thread " + t.getName());
		}
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

	public void destroyLock(Lock l) {
		if (!lockSet.contains(l)) {
			throw new IllegalArgumentException(
					"Cannot delete non-existent lock " + l.getName());
		} else {
			lockSet.remove(l);

			HBPredecessorLock.remove(l);
			WCPPredecessorLock.remove(l);

			lastReleaseLockReadVariableThread.remove(l);
			lastReleaseLockWriteVariableThread.remove(l);

			view.destroyLock(l);
		}
	}

	public void destroyLockThreadStack(Lock l, Thread t) {
		if (!lockSet.contains(l)) {
			throw new IllegalArgumentException(
					"Cannot delete stacks for non-existent lock " + l.getName());
		} else if (!threadToIndex.containsKey(t)) {
			throw new IllegalArgumentException(
					"Cannot delete stacks for non-existent thread " + t.getName());
		} else if (!this.view.lockThreadLastInteraction.get(l.getName())
				.containsKey(t.getName())) {
			throw new IllegalArgumentException(
					"Invalid operation : No critical section on lock " + l.getName()
							+ " in thread " + t.getName());
		} else {
			view.destroyLockThreadStack(l, t);
		}
	}
}