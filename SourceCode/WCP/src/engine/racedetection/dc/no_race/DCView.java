package engine.racedetection.dc.no_race;

import java.util.HashMap;
import java.util.HashSet;
import java.util.function.Function;

import event.Lock;
import event.Thread;
import util.Pair;
import util.ll.EfficientLLView;
import util.vectorclock.ClockEpochPair;
import util.vectorclock.VectorClock;

public class DCView {

	public static Function<VectorClock, Function<ClockEpochPair, Boolean>> compareEpochVC = vc -> clep -> clep
			.getAcquire().isLessThanOrEqual(vc);

	private HashMap<Lock, HashMap<Thread, EfficientLLView<Thread, ClockEpochPair>>> view;
	private HashSet<Thread> threadSet;
	private HashSet<Lock> lockSet;

	public HashMap<String, HashMap<String, Long>> lockThreadLastInteraction;

	// private VectorClock tempMin;

	DCView(HashSet<Thread> tSet) {
		this.threadSet = new HashSet<Thread>(tSet);
		this.lockSet = new HashSet<Lock>();
		this.view = new HashMap<Lock, HashMap<Thread, EfficientLLView<Thread, ClockEpochPair>>>();
	}

	public void checkAndAddLock(Lock l) {
		if (!lockSet.contains(l)) {
			lockSet.add(l);
			HashSet<Thread> tSet = new HashSet<Thread>();
			for (Thread tReader : this.threadSet) {
				if (this.lockThreadLastInteraction.get(l.getName())
						.containsKey(tReader.getName())) {
					tSet.add(tReader);
				}
			}
			HashMap<Thread, EfficientLLView<Thread, ClockEpochPair>> vw_l = new HashMap<Thread, EfficientLLView<Thread, ClockEpochPair>>();
			for (Thread t : tSet) {
				EfficientLLView<Thread, ClockEpochPair> vw_l_t = new EfficientLLView<Thread, ClockEpochPair>(
						tSet);
				vw_l.put(t, vw_l_t);
			}
			this.view.put(l, vw_l);
		}
	}

	public void pushClockPair(Lock l, Thread t, ClockEpochPair clockPair) {
		checkAndAddLock(l);
		this.view.get(l).get(t).pushTop(clockPair);
	}

	public VectorClock getMaxLowerBound(Thread tReader, Lock l, Thread tWriter,
			VectorClock ct) {
		checkAndAddLock(l);
		Pair<Boolean, ClockEpochPair> mlb = view.get(l).get(tWriter)
				.getMaxLowerBound(tReader, compareEpochVC.apply(ct));
		if (mlb.first) {
			return mlb.second.getRelease();
		} else {
			return new VectorClock(ct.getDim());
		}
	}

	public void updateTopRelease(Lock l, Thread t, VectorClock ht) {
		checkAndAddLock(l);
		ClockEpochPair clockPair = this.view.get(l).get(t).top();
		clockPair.getRelease().copyFrom(ht);
	}

	public String toString() {
		String str = "";
		for (Lock l : lockSet) {
			str += "[" + l.getName() + "]";
			str += " : " + view.get(l).toString() + "\n";
		}
		str += "\n";
		return str;
	}

	public int getSize() {
		int sz = 0;
		for (Lock l : lockSet) {
			for (Thread t : this.threadSet) {
				if (this.lockThreadLastInteraction.get(l.getName())
						.containsKey(t.getName())) {
					sz += view.get(l).get(t).getLength();
				}
			}
		}
		return sz;
	}

	public void printSize() {
		System.err.println("Stack size = " + Integer.toString(this.getSize()));
	}

	public void destroyLock(Lock l) {
		if (!lockSet.contains(l)) {
			throw new IllegalArgumentException(
					"Cannot delete non-existent lock " + l.getName());
		} else {
			lockSet.remove(l);
			view.remove(l);
		}
	}

	public void destroyLockThreadStack(Lock l, Thread t) {
		if (!lockSet.contains(l)) {
			throw new IllegalArgumentException(
					"Cannot delete stack for non-existent lock " + l.getName());
		} else if (!threadSet.contains(t)) {
			throw new IllegalArgumentException(
					"Cannot delete stack for non-existent thread " + t.getName());
		} else {
			for (Thread tprime : view.get(l).keySet()) {
				this.view.get(l).get(tprime).destroyKey(t);
			}
			this.view.get(l).remove(t);
			this.lockThreadLastInteraction.get(l.getName()).remove(t.getName());
		}
	}

}