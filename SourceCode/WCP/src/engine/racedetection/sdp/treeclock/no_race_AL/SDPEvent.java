package engine.racedetection.sdp.treeclock.no_race_AL;

import java.util.HashSet;
import java.util.Stack;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.treeclock.word_tricks_list.TreeClock;
import util.vectorclock.VectorClock;
import util.treeclock.word_tricks_list.ForestClock;

public class SDPEvent extends RaceDetectionEvent<SDPState> {

	public SDPEvent() {
		super();
	}

	public boolean Handle(SDPState state) {
		// Do some things first
		if (!(state.mapThreadLockStack.containsKey(this.thread))) {
			state.mapThreadLockStack.put(this.thread, new Stack<Lock>());
			state.mapThreadReadVarSetStack.put(this.thread,
					new Stack<HashSet<Variable>>());
			state.mapThreadWriteVarSetStack.put(this.thread,
					new Stack<HashSet<Variable>>());
		}
		// Now call child's function
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
	public void printRaceInfoLockType(SDPState state) {
		if (this.getType().isLockType()) {
			if (state.verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClock C_t = state
						.generateVectorClockFromClockThread(this.getThread());
				str += C_t.toString();
				// str += C_t.timesToString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(SDPState state) {
		if (this.getType().isAccessType()) {
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getVariable().getName();
			str += "|";
			VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
			str += C_t.toString();
			// str += C_t.timesToString();
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
	public void printRaceInfoExtremeType(SDPState state) {
		if (this.getType().isExtremeType()) {
			if (state.verbosity == 2) {
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClock C_t = state
						.generateVectorClockFromClockThread(this.getThread());
				str += C_t.toString();
				// str += C_t.timesToString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public boolean HandleSubAcquire(SDPState state) {
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

		TreeClock H_t = state.getTreeClock(state.HBPredecessorThread, this.getThread());
		TreeClock H_l = state.getTreeClock(state.HBPredecessorLock, this.getLock());
		H_t.join(H_l);

		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, getLock());

		// P_t.PPJoin(P_l);
		P_t.updateWithMax(P_t, P_l);

		if (!reEntrant) {
			state.updateViewAsWriterAtAcquire(getLock(), getThread());
		}
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(SDPState state) {
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

		TreeClock H_t = state.getTreeClock(state.HBPredecessorThread, getThread());

		for (Thread tPrime : state.threadToIndex.keySet()) {
			if (tPrime.getId() != this.getThread().getId()) {

				for (Variable r : this.readVarSet) {
					if (state.existsLockReadVariableThreads
							.containsKey(this.getLock().getName())) {
						if (state.existsLockReadVariableThreads
								.get(this.getLock().getName()).containsKey(r.getName())) {
							if (state.existsLockReadVariableThreads
									.get(this.getLock().getName()).get(r.getName())
									.contains(tPrime.getName())) {
								ForestClock L_r_l_x_tprime = state.getForestClock(
										state.lastReleaseLockReadVariableThread,
										this.getLock(), r, tPrime);
								// L_r_l_x_tprime.monotoneCopy(H_t);
								L_r_l_x_tprime.join(H_t);
							}
						}
					}
				}

				for (Variable w : this.writeVarSet) {
					if (state.existsLockWriteVariableThreads
							.containsKey(this.getLock().getName())) {
						if (state.existsLockWriteVariableThreads
								.get(this.getLock().getName()).containsKey(w.getName())) {
							if (state.existsLockWriteVariableThreads
									.get(this.getLock().getName()).get(w.getName())
									.contains(tPrime.getName())) {
								TreeClock L_w_l_x_tprime = state.getTreeClock(
										state.lastReleaseLockWriteVariableThread,
										this.getLock(), w, tPrime);
								L_w_l_x_tprime.monotoneCopy(H_t);
							}
						}
					}
				}
			}
		}

		TreeClock H_l = state.getTreeClock(state.HBPredecessorLock, this.getLock());
		H_l.monotoneCopy(H_t);

		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, this.getLock());
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		// P_l.monotoneCopy(P_t);
		P_l.copyFrom(P_t);

		// System.out.println(state.view.toString());

		state.updateViewAsWriterAtRelease(getLock(), getThread());
		state.incClockThread(getThread());

		// System.out.println("Release :");
		this.printRaceInfo(state);

		return false;
	}

	@Override
	public boolean HandleSubRead(SDPState state) {
		/****** Annotation phase starts **********/
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadReadVarSetStack.get(getThread()).peek().add(getVariable());
			this.setLockSet(state.getSetFromStack(getThread()));
		}
		/****** Annotation phase ends **********/

		/***** Update P_t **************/
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread,
				this.getThread());

		ForestClock B_t_v = state.getForestClock(state.branchClockVariableThread,
				this.getVariable(), this.getThread());

		state.joinVCWithFC(P_t, B_t_v);

		for (Lock l : this.getLockSet()) {
			if (state.existsLockWriteVariableThreads.containsKey(l.getName())) {
				if (state.existsLockWriteVariableThreads.get(l.getName())
						.containsKey(this.getVariable().getName())) {
					if (state.existsLockWriteVariableThreads.get(l.getName())
							.get(this.getVariable().getName())
							.contains(this.getThread().getName())) {
						TreeClock writeClock = state.getTreeClock(
								state.lastReleaseLockWriteVariableThread, l,
								this.getVariable(), this.getThread());
						// P_t.PJoin(writeClock);
						state.joinVCWithTC(P_t, writeClock);
					}
				}
			}
		}
		/***** P_t updated **************/

		this.printRaceInfo(state);

		if (state.tickClockOnAccess) {
			state.incClockThread(getThread());
		}

		return false;
	}

	@Override
	public boolean HandleSubWrite(SDPState state) {

		/****** Annotation phase starts **********/
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadWriteVarSetStack.get(getThread()).peek().add(getVariable());
			this.setLockSet(state.getSetFromStack(getThread()));
		}
		/****** Annotation phase ends **********/

		/***** Update P_t **************/
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread,
				this.getThread());
		for (Lock l : this.getLockSet()) {
			if (state.existsLockReadVariableThreads.containsKey(l.getName())) {
				if (state.existsLockReadVariableThreads.get(l.getName())
						.containsKey(this.getVariable().getName())) {
					if (state.existsLockReadVariableThreads.get(l.getName())
							.get(this.getVariable().getName())
							.contains(this.getThread().getName())) {
						ForestClock readClock = state.getForestClock(
								state.lastReleaseLockReadVariableThread, l,
								this.getVariable(), this.getThread());
						// P_t.PJoin(readClock);
						state.joinVCWithFC(P_t, readClock);
					}

				}
			}
		}
		/***** P_t updated **************/

		/**** Update branchClocks ****/

		ForestClock B_t_v = state.getForestClock(state.branchClockVariableThread,
				this.getVariable(), this.getThread());
		for (Lock l : this.getLockSet()) {
			if (state.existsLockWriteVariableThreads.containsKey(l.getName())) {
				if (state.existsLockWriteVariableThreads.get(l.getName())
						.containsKey(this.getVariable().getName())) {
					if (state.existsLockWriteVariableThreads.get(l.getName())
							.get(this.getVariable().getName())
							.contains(this.getThread().getName())) {
						TreeClock writeClock = state.getTreeClock(
								state.lastReleaseLockWriteVariableThread, l,
								this.getVariable(), this.getThread());
						B_t_v.join(writeClock);
					}

				}
			}
		}

		/*
		 * int tIndex = state.threadToIndex.get(this.getThread()); TreeClock new_clk =
		 * new TreeClock((short) tIndex, 0, P_t.getDim()); for (Lock l :
		 * this.getLockSet()) { if
		 * (state.existsLockWriteVariableThreads.containsKey(l.getName())) { if
		 * (state.existsLockWriteVariableThreads.get(l.getName())
		 * .containsKey(this.getVariable().getName())) { if
		 * (state.existsLockWriteVariableThreads.get(l.getName())
		 * .get(this.getVariable().getName()) .contains(this.getThread().getName())) {
		 * TreeClock writeClock = state.getTreeClock(
		 * state.lastReleaseLockWriteVariableThread, l, this.getVariable(),
		 * this.getThread()); new_clk.join(writeClock); }
		 * 
		 * } } } ForestClock B_t_v =
		 * state.getForestClock(state.branchClockVariableThread, this.getVariable(),
		 * this.getThread()); B_t_v.deepCopy(new_clk);
		 */
		/**** branchClocks updated ****/

		this.printRaceInfo(state);

		if (state.tickClockOnAccess) {
			state.incClockThread(getThread());
		}

		return false;
	}

	@Override
	public boolean HandleSubFork(SDPState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			/****** Annotation phase starts **********/
			// No annotation required
			/****** Annotation phase ends **********/

			TreeClock H_t = state.getTreeClock(state.HBPredecessorThread,
					this.getThread());

			TreeClock H_tc = state.getTreeClock(state.HBPredecessorThread,
					this.getTarget());
			// System.out.println("Fork : Setting HB of target");
			H_tc.join(H_t);

			VectorClock P_tc = state.getVectorClock(state.WCPPredecessorThread,
					this.getTarget());
			// System.out.println("Fork : Setting WCP of target");
			// P_tc.join(H_t);
			state.joinVCWithTC(P_tc, H_t);

			state.incClockThread(this.getThread());
			this.printRaceInfo(state);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(SDPState state) {
		if (state.isThreadRelevant(this.getTarget())) {

			/****** Annotation phase starts **********/
			// No annotation required
			/****** Annotation phase ends **********/

			TreeClock H_t = state.getTreeClock(state.HBPredecessorThread,
					this.getThread());
			TreeClock H_tc = state.getTreeClock(state.HBPredecessorThread,
					this.getTarget());
			VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread,
					this.getThread());

			H_t.join(H_tc);
			// P_t.PJoin(H_tc);
			state.joinVCWithTC(P_t, H_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
