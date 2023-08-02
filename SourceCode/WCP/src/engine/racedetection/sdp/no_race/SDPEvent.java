package engine.racedetection.sdp.no_race;

import java.util.HashSet;
import java.util.Stack;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

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

		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread,
				this.getThread());
		VectorClock H_l = state.getVectorClock(state.HBPredecessorLock, this.getLock());
		H_t.updateWithMax(H_t, H_l);

		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, getLock());
		P_t.updateWithMax(P_t, P_l);

		if (!reEntrant) {
			state.updateViewAsWriterAtAcquire(getLock(), getThread());
		}
		// System.out.println("Acquire :");
		// System.out.println(state.view.toString());
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

		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, getThread());

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
								VectorClock L_r_l_x_tprime = state.getVectorClock(
										state.lastReleaseLockReadVariableThread,
										this.getLock(), r, tPrime);
								L_r_l_x_tprime.updateWithMax(L_r_l_x_tprime, H_t);
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
								VectorClock L_w_l_x_tprime = state.getVectorClock(
										state.lastReleaseLockWriteVariableThread,
										this.getLock(), w, tPrime);
								L_w_l_x_tprime.updateWithMax(L_w_l_x_tprime, H_t);
							}
						}
					}
				}
			}
		}

		VectorClock H_l = state.getVectorClock(state.HBPredecessorLock, this.getLock());
		H_l.copyFrom(H_t);

		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, this.getLock());
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
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

		VectorClock B_t_v = state.getVectorClock(state.branchClockVariableThread,
				this.getVariable(), this.getThread());
		P_t.updateMax2(B_t_v);

		for (Lock l : this.getLockSet()) {
			// VectorClock writeClock =
			// state.getVectorClock(state.lastReleaseLockWriteVariable, l,
			// this.getVariable());
			// P_t.updateWithMax(P_t, writeClock);
			if (state.existsLockWriteVariableThreads.containsKey(l.getName())) {
				if (state.existsLockWriteVariableThreads.get(l.getName())
						.containsKey(this.getVariable().getName())) {
					if (state.existsLockWriteVariableThreads.get(l.getName())
							.get(this.getVariable().getName())
							.contains(this.getThread().getName())) {
						VectorClock writeClock = state.getVectorClock(
								state.lastReleaseLockWriteVariableThread, l,
								this.getVariable(), this.getThread());
						P_t.updateWithMax(P_t, writeClock);
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
						VectorClock readClock = state.getVectorClock(
								state.lastReleaseLockReadVariableThread, l,
								this.getVariable(), this.getThread());
						P_t.updateWithMax(P_t, readClock);
					}

				}
			}
		}
		/***** P_t updated **************/

		this.printRaceInfo(state);

		/***** Update branchClocks **************/
		VectorClock B_t_v = state.getVectorClock(state.branchClockVariableThread,
				this.getVariable(), this.getThread());
		B_t_v.setToZero();

		for (Lock l : this.getLockSet()) {
			if (state.existsLockWriteVariableThreads.containsKey(l.getName())) {
				if (state.existsLockWriteVariableThreads.get(l.getName())
						.containsKey(this.getVariable().getName())) {
					if (state.existsLockWriteVariableThreads.get(l.getName())
							.get(this.getVariable().getName())
							.contains(this.getThread().getName())) {
						VectorClock writeClock = state.getVectorClock(
								state.lastReleaseLockWriteVariableThread, l,
								this.getVariable(), this.getThread());
						B_t_v.updateMax2(writeClock);
					}

				}
			}
		}

		/***** branchClocks updated **************/

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

			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread,
					this.getThread());

			VectorClock H_tc = state.getVectorClock(state.HBPredecessorThread,
					this.getTarget());
			// System.out.println("Fork : Setting HB of target");
			H_tc.updateWithMax(H_tc, H_t);

			VectorClock P_tc = state.getVectorClock(state.WCPPredecessorThread,
					this.getTarget());
			// System.out.println("Fork : Setting WCP of target");
			P_tc.updateWithMax(P_tc, H_t);

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

			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread,
					this.getThread());
			VectorClock H_tc = state.getVectorClock(state.HBPredecessorThread,
					this.getTarget());
			VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread,
					this.getThread());

			H_t.updateWithMax(H_t, H_tc);
			P_t.updateWithMax(P_t, H_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
