package engine.racedetection.shb_no_race.optjoin;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Thread;
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
				VectorClock C_t = state.getVectorClock(state.clockThread,
						this.getThread());
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
				VectorClock C_t = state.getVectorClock(state.clockThread,
						this.getThread());
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
				VectorClock C_t = state.getVectorClock(state.clockThread,
						this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public boolean HandleSubAcquire(SHBState state) {
		if (state.lastThreadToRelease.containsKey(this.getLock())) {
			Thread last_thread = state.lastThreadToRelease.get(this.getLock());
			if (last_thread.getName().equals(this.getThread().getName())) {
				return false;
			}
		}
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(SHBState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		this.printRaceInfo(state);
		state.incClockThread(getThread());
		state.lastThreadToRelease.put(this.getLock(), this.getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(SHBState state) {
		if (state.lastThreadToWrite.containsKey(this.getVariable())) {
			Thread last_thread = state.lastThreadToWrite.get(this.getVariable());
			if (last_thread.getName().equals(this.getThread().getName())) {
				return false;
			}
		}

		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());

		this.printRaceInfo(state);

		C_t.updateWithMax(C_t, LW_v);

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(SHBState state) {
		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());

		this.printRaceInfo(state);

		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		LW_v.copyFrom(C_t);
		state.incClockThread(getThread());
		state.lastThreadToWrite.put(this.getVariable(), this.getThread());
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(SHBState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_tc.copyFrom(C_t);
			state.setIndex(C_tc, this.getTarget(), 1);
			this.printRaceInfo(state);
			state.incClockThread(getThread());
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(SHBState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
