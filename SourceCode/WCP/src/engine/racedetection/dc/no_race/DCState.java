package engine.racedetection.dc.no_race;

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
	public HashMap<Lock, HashMap<Long, HashMap<Long, VectorClock>>> lastReleaseLockReadVariable;
	public HashMap<Lock, HashMap<Long, HashMap<Long, VectorClock>>> lastReleaseLockWriteVariable;
	public DCView view;

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

	// public long timerReleaseJoins = 0;
	// public long timerReleaseTotal = 0;

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
		this.lastReleaseLockReadVariable = new HashMap<Lock, HashMap<Long, HashMap<Long, VectorClock>>>();

		// initialize lastReleaseLockWriteVariable
		this.lastReleaseLockWriteVariable = new HashMap<Lock, HashMap<Long, HashMap<Long, VectorClock>>>();

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

			String lName = l.getName();
			if (existsLockReadVariableThreads.containsKey(lName)) {
				this.lastReleaseLockReadVariable.put(l,
						new HashMap<Long, HashMap<Long, VectorClock>>());
				for (Variable v : variableSet) {
					String vName = v.getName();
					Long vEquivRead = this.variableToReadEquivalenceClass.get(vName);
					Long vEquivWrite = this.variableToWriteEquivalenceClass.get(vName);
					if (existsLockReadVariableThreads.get(lName).containsKey(vName)) {
						if (!lastReleaseLockReadVariable.get(l).containsKey(vEquivRead)) {
							this.lastReleaseLockReadVariable.get(l).put(vEquivRead,
									new HashMap<Long, VectorClock>());
						}
						if (!lastReleaseLockReadVariable.get(l).get(vEquivRead)
								.containsKey(vEquivWrite)) {
							this.lastReleaseLockReadVariable.get(l).get(vEquivRead)
									.put(vEquivWrite, new VectorClock(this.numThreads));
						}
					}

				}
			}

			if (existsLockWriteVariableThreads.containsKey(lName)) {
				this.lastReleaseLockWriteVariable.put(l,
						new HashMap<Long, HashMap<Long, VectorClock>>());
				for (Variable v : variableSet) {
					String vName = v.getName();
					Long vEquivRead = this.variableToReadEquivalenceClass.get(vName);
					Long vEquivWrite = this.variableToWriteEquivalenceClass.get(vName);
					if (existsLockWriteVariableThreads.get(lName).containsKey(vName)) {
						if (!lastReleaseLockWriteVariable.get(l)
								.containsKey(vEquivRead)) {
							this.lastReleaseLockWriteVariable.get(l).put(vEquivRead,
									new HashMap<Long, VectorClock>());
						}
						if (!lastReleaseLockWriteVariable.get(l).get(vEquivRead)
								.containsKey(vEquivWrite)) {
							this.lastReleaseLockWriteVariable.get(l).get(vEquivRead)
									.put(vEquivWrite, new VectorClock(this.numThreads));
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
						if (!this.lastReleaseLockReadVariable.get(l)
								.containsKey(vEquivRead)) {
							this.lastReleaseLockReadVariable.get(l).put(vEquivRead,
									new HashMap<Long, VectorClock>());
						}
						if (!this.lastReleaseLockReadVariable.get(l).get(vEquivRead)
								.containsKey(vEquivWrite)) {
							this.lastReleaseLockReadVariable.get(l).get(vEquivRead)
									.put(vEquivWrite, new VectorClock(this.numThreads));
						}
					}
				}

				if (existsLockWriteVariableThreads.containsKey(l.getName())) {
					if (existsLockWriteVariableThreads.get(l.getName())
							.containsKey(v.getName())) {
						if (!this.lastReleaseLockWriteVariable.get(l)
								.containsKey(vEquivRead)) {
							this.lastReleaseLockWriteVariable.get(l).put(vEquivRead,
									new HashMap<Long, VectorClock>());
						}
						if (!this.lastReleaseLockWriteVariable.get(l).get(vEquivRead)
								.containsKey(vEquivWrite)) {
							this.lastReleaseLockWriteVariable.get(l).get(vEquivRead)
									.put(vEquivWrite, new VectorClock(this.numThreads));
						}
					}
				}
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

	public VectorClock getVectorClock(
			HashMap<Lock, HashMap<Long, HashMap<Long, VectorClock>>> arr, Lock l,
			Variable v) {
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

		return arr.get(l).get(vEquivRead).get(vEquivWrite);
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
		VectorClock C_t = getVectorClock(clockThread, t);
		Epoch e = new Epoch(C_t.getClockIndex(tIndex), tIndex);
		view.pushClockPair(l, t, new ClockEpochPair(e, new VectorClock(this.numThreads)));
	}

	public void readViewOfWriters(Lock l, Thread t) {
		checkAndAddLock(l);
		if (!this.view.lockThreadLastInteraction.get(l.getName())
				.containsKey(t.getName())) {
			throw new IllegalArgumentException(
					"Invalid operation : No critical section on lock " + l.getName()
							+ " in thread " + t.getName());
		}
		VectorClock C_t = getVectorClock(clockThread, t);
		for (Thread tprime : this.threadToIndex.keySet()) {
			if (tprime.equals(t))
				continue;
			if (!this.view.lockThreadLastInteraction.get(l.getName())
					.containsKey(tprime.getName())) {
				continue;
			}
			VectorClock vc = view.getMaxLowerBound(t, l, tprime, C_t);
			C_t.updateWithMax(C_t, vc);
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

			lastReleaseLockReadVariable.remove(l);
			lastReleaseLockWriteVariable.remove(l);

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