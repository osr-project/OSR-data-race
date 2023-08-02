package engine.racedetection.shb_treeclock.no_race_detectionHM;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.treeclock.word_tricks_map.TreeClock;

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
				// str += C_t.toString();
				str += C_t.timesToString();
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
				// str += C_t.toString();
				str += C_t.timesToString();
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
				// str += C_t.toString();
				str += C_t.timesToString();
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
		this.printRaceInfo(state);
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
		this.printRaceInfo(state);
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
		TreeClock LW_v = state.getTreeClock(state.lastWriteVariable, v);

		this.printRaceInfo(state);

		C_t.join(LW_v);

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(SHBState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.addVariable(v);

		boolean raceDetected = false;
		TreeClock C_t = state.getTreeClock(state.clockThread, t);

		// this.printRaceInfo(state);

		TreeClock LW_v = state.getTreeClock(state.lastWriteVariable, v);
		if (LW_v.isLessThanOrEqual(C_t)) {
			LW_v.monotoneCopy(C_t);

			/*
			 * if(LW_v.monotoneCopy(C_t)) { state.numTimesCopyWasMonotone += 1; } else {
			 * state.numTimesCopyWasDeep += 1; }
			 */

		} else {
			LW_v.deepCopy(C_t);
			// state.numTimesCopyWasDeep += 1;
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
			this.printRaceInfo(state);
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
			this.printRaceInfo(state);
		}
		return false;
	}

}
