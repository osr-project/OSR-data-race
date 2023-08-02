package engine.racedetection.fasthb_treeclock;

import java.util.HashMap;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Pair;
import util.treeclock.iterative.TreeClock;

public class FastHBEvent extends RaceDetectionEvent<FastHBState> {

	@Override
	public boolean Handle(FastHBState state) {
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
	public void printRaceInfoLockType(FastHBState state) {
		if (this.getType().isLockType()) {
			if (state.verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				str += this.getThread().getName();
				str += "|";
				TreeClock C_t = state.getClock(state.clockThread, this.getThread());
				str += C_t.toString();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(FastHBState state) {
		if (this.getType().isAccessType()) {
			if (state.verbosity == 1 || state.verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getVariable().getName();
				str += "|";
				str += this.getThread().getName();
				str += "|";
				str += this.getAuxId();
				str += "|";
				TreeClock C_t = state.getClock(state.clockThread, this.getThread());
				str += C_t.toString();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoExtremeType(FastHBState state) {
		if (this.getType().isExtremeType()) {
			if (state.verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				str += this.getThread().getName();
				str += "|";
				TreeClock C_t = state.getClock(state.clockThread, this.getThread());
				str += C_t.toString();
				System.out.println(str);
			}
		}
	}

	@Override
	public boolean HandleSubAcquire(FastHBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.addLock(l, t);

		TreeClock C_t = state.getClock(state.clockThread, t);
		TreeClock L_l = state.getClock(state.lastReleaseLock, l);
		C_t.join(L_l);
		return false;
	}

	@Override
	public boolean HandleSubRelease(FastHBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.addLock(l, t);

		TreeClock C_t = state.getClock(state.clockThread, t);
		TreeClock L_l = state.getClock(state.lastReleaseLock, l);
		L_l.monotoneCopy(C_t);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(FastHBState state) {

		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.addVariable(v);

		TreeClock C_t = state.getClock(state.clockThread, t);
		
		boolean raceDetected = false;
		Pair<Short, Integer> epochWrite_v = state.getEpochs(state.epochWriteVariable, v);
		if (epochWrite_v != null) {
			short wthread = epochWrite_v.first;
			int wclk = epochWrite_v.second;
			if (C_t.getLocalClock(wthread) < wclk) {
				raceDetected = true;
			}
		}

		short tid = state.threadToIndex.get(t);
		int clk = C_t.getLocalClock(tid);
		state.getEpochs(state.epochReadVariable, v).put(tid, clk);

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(FastHBState state) {

		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.addVariable(v);

		TreeClock C_t = state.getClock(state.clockThread, t);
		boolean raceDetected = false;

		Pair<Short, Integer> epochWrite_v = state.getEpochs(state.epochWriteVariable, v);
		if (epochWrite_v != null) {
			short wthread = epochWrite_v.first;
			int wclk = epochWrite_v.second;
			if (C_t.getLocalClock(wthread) < wclk) {
				raceDetected = true;
			}
		}
		if (!raceDetected) {
			HashMap<Short, Integer> epochRead_v = state.getEpochs(state.epochReadVariable,
					v);
			for (short tprime : epochRead_v.keySet()) {
				if (C_t.getLocalClock(tprime) < epochRead_v.get(tprime)) {
					raceDetected = true;
				}
			}
		}

		short tid = state.threadToIndex.get(t);
		int clk = C_t.getLocalClock(tid);
		state.setWriteEpoch(v, new Pair<Short, Integer>(tid, clk));
		state.getEpochs(state.epochReadVariable, v).clear();

		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(FastHBState state) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			TreeClock C_t = state.getClock(state.clockThread, t);
			TreeClock C_tc = state.getClock(state.clockThread, tc);
			C_tc.join(C_t);
			state.incClockThread(t);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(FastHBState state) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			TreeClock C_t = state.getClock(state.clockThread, t);
			TreeClock C_tc = state.getClock(state.clockThread, tc);
			C_t.join(C_tc);
		}
		return false;
	}

}
