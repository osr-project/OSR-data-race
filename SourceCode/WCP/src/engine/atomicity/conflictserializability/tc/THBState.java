package engine.atomicity.conflictserializability.tc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.atomicity.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Triplet;
import util.dll.DoublyLinkedList;
import util.tree.EfficientTreeNode;
import util.treeclock.iterative.TreeClock;
import util.vectorclock.VectorClockOpt;

public class THBState extends State {

	// Internal data
	public HashMap<Thread, Short> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private short numThreads;
	private int numLocks;
	private int numVariables;

	public ArrayList<TreeClock> clockThread; // mathcal{C}
	public ArrayList<TreeClock> clockThreadBegin; // mathcal{C^BEGIN}
	public HashMap<Lock, TreeClock> clockLock; // mathcal{L}
	public HashMap<Variable, TreeClock> clockWriteVariable; // mathcal{W}
	public HashMap<Variable, TreeClock> clockReadVariable; // mathcal{R}
	public HashMap<Variable, VectorClockOpt> clockReadVariableCheck; // mathcal{chR}

	public HashMap<Lock, Thread> lastThreadToRelease;
	public HashMap<Variable, Thread> lastThreadToWrite;

	public HashMap<Thread, Integer> threadToNestingDepth;

	// parameter flags
	public int verbosity;

	public THBState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
	}

	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Short>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			// System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, this.numThreads);
			this.numThreads++;
		}

		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	public void initData(HashSet<Thread> tSet) {
		this.clockThread = new ArrayList<TreeClock>();
		for (short tIndex = 0; tIndex < this.numThreads; tIndex++) {
			this.clockThread.add(new TreeClock(tIndex, 1));
		}

		this.clockThreadBegin = new ArrayList<TreeClock>();
		for (short tIndex = 0; tIndex < this.numThreads; tIndex++) {
			this.clockThreadBegin.add(new TreeClock(tIndex, 0));
		}

		this.clockLock = new HashMap<Lock, TreeClock>();

		this.clockWriteVariable = new HashMap<Variable, TreeClock>();

		this.clockReadVariable = new HashMap<Variable, TreeClock>();

		this.clockReadVariableCheck = new HashMap<Variable, VectorClockOpt>();

		this.lastThreadToRelease = new HashMap<Lock, Thread>();

		this.lastThreadToWrite = new HashMap<Variable, Thread>();

		this.threadToNestingDepth = new HashMap<Thread, Integer>();
		for (Thread t : tSet) {
			this.threadToNestingDepth.put(t, 0);
		}
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		TreeClock tc_t = clockThread.get(tIndex);
		tc_t.incrementBy(1);
	}

	public TreeClock getClock(ArrayList<TreeClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return arr.get(tIndex);
	}

	public TreeClock getClock(HashMap<Lock, TreeClock> arr, Lock l) {
		return arr.get(l);
	}

	public <E> E getClock(HashMap<Variable, E> arr, Variable v) {
		return arr.get(v);
	}

	public int checkAndAddLock(Lock l) {
		if (!lockToIndex.containsKey(l)) {
			lockToIndex.put(l, this.numLocks);
			this.numLocks++;
			this.clockLock.put(l, new TreeClock());
		}
		return lockToIndex.get(l);
	}

	public int checkAndAddVariable(Variable v) {
		if (!variableToIndex.containsKey(v)) {
			variableToIndex.put(v, this.numVariables);
			this.numVariables++;
			this.clockReadVariable.put(v, new TreeClock());
			this.clockReadVariableCheck.put(v, new VectorClockOpt(this.numThreads));
			this.clockWriteVariable.put(v, new TreeClock());
		}
		return variableToIndex.get(v);
	}

	public boolean checkAndGetClock(VectorClockOpt checkClock, TreeClock fromClock,
			Thread target) {
		short tIndex = this.threadToIndex.get(target);
		boolean violationDetected = false;
		TreeClock C_target_begin = getClock(clockThreadBegin, target);
		int checkClock_tIndex_value = checkClock.getClock().get(tIndex).intValue();
		int C_target_begin_tIndex_value = C_target_begin.getLocalClock(tIndex);
		if ((C_target_begin_tIndex_value <= checkClock_tIndex_value)
				&& threadToNestingDepth.get(target) > 0) {
			violationDetected = true;
		}
		TreeClock C_target = getClock(clockThread, target);
		C_target.join(fromClock);
		return violationDetected;
	}

	public boolean checkAndGetClock(TreeClock checkClock, TreeClock fromClock,
			Thread target) {
		boolean violationDetected = false;
		TreeClock C_target_begin = getClock(clockThreadBegin, target);
		if (C_target_begin.isLessThanOrEqual(checkClock)
				&& threadToNestingDepth.get(target) > 0) {
			violationDetected = true;
		}
		TreeClock C_target = getClock(clockThread, target);
		C_target.join(fromClock);
		return violationDetected;
	}

	public boolean handshakeAtEndEvent(Thread t) {
		boolean violationDetected = false;
		int tIndex = this.threadToIndex.get(t);
		TreeClock C_t_begin = this.clockThreadBegin.get(tIndex);
		TreeClock C_t = this.clockThread.get(tIndex);

		for (Thread u : this.threadToIndex.keySet()) {
			if (!u.equals(t)) {
				TreeClock C_u = getClock(clockThread, u);
				if (C_t_begin.isLessThanOrEqual(C_u)) {
					violationDetected |= checkAndGetClock(C_t, C_t, u);
					if (violationDetected)
						break;
				}
			}
		}
		if (violationDetected)
			return violationDetected;

		for (Lock l : this.lockToIndex.keySet()) {
			TreeClock L_l = getClock(clockLock, l);
			if (C_t_begin.isLessThanOrEqual(L_l)) {
				L_l.join(C_t);
			}
		}

		for (Variable v : this.variableToIndex.keySet()) {
			TreeClock W_v = getClock(clockWriteVariable, v);
			if (C_t_begin.isLessThanOrEqual(W_v)) {
				W_v.join(C_t);
			}
			TreeClock R_v = getClock(clockReadVariable, v);
			VectorClockOpt chR_v = getClock(clockReadVariableCheck, v);
			if (C_t_begin.isLessThanOrEqual(R_v)) {
				R_v.join(C_t);
				updateCheckClock(chR_v, C_t);
			}
		}

		return violationDetected;
	}

	// in-place join of vc with tc
	// That is, vc := vc join tc
	private void joinVCWithTC(VectorClockOpt vc, TreeClock tc) {
		if (tc.root == null)
			return;

		EfficientTreeNode<Triplet<Short, Integer, Integer>> zprime = tc.root;
		short zprime_tid = zprime.getData().first;
		int zprime_local_clock = vc.getClockIndex(zprime_tid);
		int zprime_clk = zprime.getData().second;
		if (zprime_clk <= zprime_local_clock)
			return;

		DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>>();

		// Handle the root first
		EfficientTreeNode<Triplet<Short, Integer, Integer>> vprime = zprime
				.getHeadChild();
		while (vprime != null) {
			short vprime_tid = vprime.getData().first;
			int vprime_local_clock = vc.getClockIndex(vprime_tid);
			if (vprime_local_clock < vprime.getData().second) {
				S.pushLatest(vprime);
			} else {
				if (vprime.getData().third <= zprime_local_clock) {
					break;
				}
			}
			vprime = vprime.getNext();
		}

		vc.setClockIndex(zprime_tid, zprime_clk);

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNode<Triplet<Short, Integer, Integer>> uprime = S.popLatest();

			short uprime_tid = uprime.getData().first;
			int vc_uprime_tid_clk = vc.getClockIndex(uprime_tid);

			// Expand to children of u'
			vprime = uprime.getHeadChild();
			while (vprime != null) {
				short vprime_tid = vprime.getData().first;
				int vprime_local_clock = vc.getClockIndex(vprime_tid);
				if (vprime_local_clock < vprime.getData().second) {
					S.pushLatest(vprime);
				} else {
					if (vprime.getData().third <= vc_uprime_tid_clk) {
						break;
					}
				}
				vprime = vprime.getNext();
			}

			vc.setClockIndex(uprime_tid, vc_uprime_tid_clk);
		}
	}

	private void updateMax2WithoutLocal(VectorClockOpt vc, TreeClock tc) {
		EfficientTreeNode<Triplet<Short, Integer, Integer>> zprime = tc.root;
		short zprime_tid = zprime.getData().first;
		int zprime_local_clock = vc.getClockIndex(zprime_tid);
		joinVCWithTC(vc, tc);
		vc.setClockIndex(zprime_tid, zprime_local_clock);
	}

	public void updateCheckClock(VectorClockOpt vc, TreeClock tc) {
		updateMax2WithoutLocal(vc, tc);
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