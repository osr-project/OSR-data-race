package engine.racedetection.shb_treeclock;

import java.util.HashSet;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.treeclock.iterative.TreeClock;
import util.vectorclock.VectorClock;

public class SHBEvent extends RaceDetectionEvent<SHBState> {

	@Override
	public boolean Handle(SHBState state) {
		if (EventStatistics.isEnabled()) {
			boolean toReturn;

			long startTime = System.currentTimeMillis();
			toReturn = this.HandleSub(state);
			long stopTime = System.currentTimeMillis();

			EventStatistics.updateTime(this.getType(), (stopTime - startTime));
			return toReturn;
		} else {
			// TODO: This should not call sub directly
			return this.HandleSub(state);
		}
	}

	@Override
	public void printRaceInfoLockType(SHBState state) {
		if (this.getType().isLockType()) {
			if (state.verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(SHBState state) {
		if (this.getType().isAccessType()) {
			if (state.verbosity == 1 || state.verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getVariable().getName();
				str += "|";
				TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				str += "|";
				str += this.getAuxId();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoExtremeType(SHBState state) {
		if (this.getType().isExtremeType()) {
			if (state.verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public boolean HandleSubAcquire(SHBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.addLock(l);
		TreeClock C_t = state.getTreeClock(state.clockThread, t);
		TreeClock L_l = state.getTreeClock(state.lastReleaseLock, l);
		C_t.join(L_l);
//		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(SHBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.addLock(l);
		TreeClock C_t = state.getTreeClock(state.clockThread, t);
		TreeClock L_l = state.getTreeClock(state.lastReleaseLock, l);
		L_l.monotoneCopy(C_t);
		state.incClockThread(t);
		return false;
	}

	@Override
	public boolean HandleSubRead(SHBState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.addVariable(v);

		boolean raceDetected = false;
		TreeClock C_t = state.getTreeClock(state.clockThread, t);
		VectorClock W_v = state.getVectorClock(state.writeVariable, v);
		HashSet<Short> tids = state.tids;
		for (short tprime : tids) {
			if (C_t.getLocalClock(tprime) < W_v.getClockIndex(tprime)) {
				raceDetected = true;
				break;
			}
		}

		TreeClock LW_v = state.getTreeClock(state.lastWriteVariable, v);
		C_t.join(LW_v);

		this.printRaceInfo(state);

		VectorClock R_v = state.getVectorClock(state.readVariable, v);
		short tid = state.threadToIndex.get(t);
		int c_t_t = C_t.getLocalClock(tid);
		state.setIndex(R_v, t, c_t_t);

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(SHBState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.addVariable(v);

		boolean raceDetected = false;
		TreeClock C_t = state.getTreeClock(state.clockThread, t);
		VectorClock R_v = state.getVectorClock(state.readVariable, v);
		VectorClock W_v = state.getVectorClock(state.writeVariable, v);
		HashSet<Short> tids = state.tids;
		for (short tprime : tids) {
			int c_t_tprime = C_t.getLocalClock(tprime);
			if (c_t_tprime < R_v.getClockIndex(tprime)) {
				raceDetected = true;
				break;
			}
			if (c_t_tprime < W_v.getClockIndex(tprime)) {
				raceDetected = true;
				break;
			}
		}

		this.printRaceInfo(state);

		short tid = state.threadToIndex.get(t);
		int c_t_t = C_t.getLocalClock(tid);
		state.setIndex(W_v, t, c_t_t);

		TreeClock LW_v = state.getTreeClock(state.lastWriteVariable, v);
		if (LW_v.isLessThanOrEqual(C_t)) {
			LW_v.monotoneCopy(C_t);
		} else {
			LW_v.deepCopy(C_t);
		}

		state.incClockThread(t);
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(SHBState state) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			TreeClock C_t = state.getTreeClock(state.clockThread, t);
			TreeClock C_tc = state.getTreeClock(state.clockThread, tc);
			C_tc.join(C_t);
			state.incClockThread(t);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(SHBState state) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			TreeClock C_t = state.getTreeClock(state.clockThread, t);
			TreeClock C_tc = state.getTreeClock(state.clockThread, tc);
			C_t.join(C_tc);
		}
		return false;
	}

}
