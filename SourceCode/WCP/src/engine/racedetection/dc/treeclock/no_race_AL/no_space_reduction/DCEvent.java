package engine.racedetection.dc.treeclock.no_race_AL.no_space_reduction;

import java.util.HashSet;
import java.util.Stack;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Lock;
import event.Variable;
import util.treeclock.word_tricks_list.TreeClock;
import util.treeclock.word_tricks_list.ForestClock;

public class DCEvent extends RaceDetectionEvent<DCState> {

	public DCEvent() {
		super();
	}

	@Override
	public boolean Handle(DCState state) {
		// Do some things first
		if (!(state.mapThreadLockStack.containsKey(this.thread))) {
			state.mapThreadLockStack.put(this.thread, new Stack<Lock>());
			state.mapThreadReadVarSetStack.put(this.thread,
					new Stack<HashSet<Variable>>());
			state.mapThreadWriteVarSetStack.put(this.thread,
					new Stack<HashSet<Variable>>());
		}

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
	public void printRaceInfoLockType(DCState state) {
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
	public void printRaceInfoAccessType(DCState state) {
		if (this.getType().isAccessType()) {
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
			str += "|" + Long.toString(this.getAuxId());
			if (state.verbosity == 1 || state.verbosity == 2) {
				System.out.println(str);
			}
			if (state.verbosity == 3) {
				System.out.print(str);
			}
		}
	}

	@Override
	public void printRaceInfoExtremeType(DCState state) {
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
	public boolean HandleSubAcquire(DCState state) {
		// System.out.println("Acquire Start: " + this.getThread().toString() + " || " +
		// this.getLock().toString());

		/*** Extra Pre-processing for reEntrants *****/
		boolean reEntrant = state.isLockAcquired(this.getThread(), this.getLock());
		/*** Extra Pre-processing for reEntrants done *****/

		/****** Annotation phase starts **********/
		state.mapThreadLockStack.get(getThread()).push(getLock());
		state.mapThreadReadVarSetStack.get(getThread()).push(new HashSet<Variable>());
		state.mapThreadWriteVarSetStack.get(getThread()).push(new HashSet<Variable>());
		/****** Annotation phase ends **********/

		if (!reEntrant) {
			state.updateViewAsWriterAtAcquire(getLock(), getThread());
		}

		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(DCState state) {
		// System.out.println("Release Start: " + this.getThread().toString() + " || " +
		// this.getLock().toString());

		/****** Annotation phase starts **********/
		HashSet<Variable> readVarSet = state.mapThreadReadVarSetStack.get(getThread())
				.pop();
		HashSet<Variable> writeVarSet = state.mapThreadWriteVarSetStack.get(getThread())
				.pop();

		this.setReadVarSet(readVarSet);
		this.setWriteVarSet(writeVarSet);

		state.mapThreadLockStack.get(getThread()).pop();

		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadReadVarSetStack.get(getThread()).peek().addAll(readVarSet);
			state.mapThreadWriteVarSetStack.get(getThread()).peek().addAll(writeVarSet);
		}
		/****** Annotation phase ends **********/

		state.readViewOfWriters(getLock(), getThread());

		TreeClock C_t = state.getTreeClock(state.clockThread, getThread());

		for (Variable r : this.readVarSet) {
			ForestClock L_r_l_x = state.getTreeClock(state.lastReleaseLockReadVariable,
					this.getLock(), r);
			// L_r_l_x.monotoneCopy(C_t);
			L_r_l_x.join(C_t);
		}

		for (Variable w : this.writeVarSet) {
			TreeClock L_w_l_x = state.getTreeClock(state.lastReleaseLockWriteVariable,
					this.getLock(), w);
			L_w_l_x.monotoneCopy(C_t);
		}

		state.updateViewAsWriterAtRelease(getLock(), getThread());
		state.incClockThread(getThread());

		// System.out.println("Release :");
		this.printRaceInfo(state);

		return false;
	}

	@Override
	public boolean HandleSubRead(DCState state) {
		/****** Annotation phase starts **********/
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadReadVarSetStack.get(getThread()).peek().add(getVariable());
			this.setLockSet(state.getSetFromStack(getThread()));
		}
		/****** Annotation phase ends **********/

		/***** Update P_t **************/
		TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
		for (Lock l : this.getLockSet()) {
			TreeClock writeClock = state.getTreeClock(state.lastReleaseLockWriteVariable,
					l, this.getVariable());
			C_t.join(writeClock);
		}
		/***** P_t updated **************/

		this.printRaceInfo(state);

		if (state.tickClockOnAccess) {
			state.incClockThread(getThread());
		}

		return false;
	}

	@Override
	public boolean HandleSubWrite(DCState state) {

		/****** Annotation phase starts **********/
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadWriteVarSetStack.get(getThread()).peek().add(getVariable());
			this.setLockSet(state.getSetFromStack(getThread()));
		}
		/****** Annotation phase ends **********/

		/***** Update P_t **************/
		TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
		for (Lock l : this.getLockSet()) {
			TreeClock writeClock = state.getTreeClock(state.lastReleaseLockWriteVariable,
					l, this.getVariable());
			C_t.join(writeClock);

			ForestClock readClock = state.getTreeClock(state.lastReleaseLockReadVariable,
					l, this.getVariable());
			C_t.join(readClock);
		}
		/***** P_t updated **************/

		this.printRaceInfo(state);

		if (state.tickClockOnAccess) {
			state.incClockThread(getThread());
		}

		return false;
	}

	@Override
	public boolean HandleSubFork(DCState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			/****** Annotation phase starts **********/
			// No annotation required
			/****** Annotation phase ends **********/

			TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());

			TreeClock C_tc = state.getTreeClock(state.clockThread, this.getTarget());
			C_tc.join(C_t);

			state.incClockThread(this.getThread());
			this.printRaceInfo(state);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(DCState state) {
		if (state.isThreadRelevant(this.getTarget())) {

			/****** Annotation phase starts **********/
			// No annotation required
			/****** Annotation phase ends **********/

			TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
			TreeClock C_tc = state.getTreeClock(state.clockThread, this.getTarget());

			C_t.join(C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
