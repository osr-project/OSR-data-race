package engine.racedetection.zeroreversal.distance;

import java.util.HashSet;

//import debug.ZROEventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Thread;
import event.Variable;
import event.EventType;
import event.Lock;
import util.Quintet;
import util.Triplet;
import util.ll.EfficientLLView;
import util.vectorclock.VectorClock;

public class ZREvent extends RaceDetectionEvent<ZRState> {

	static final int flushEventDuration = 100000;
	static int flush_event_ctr = 0;

	@Override
	public boolean Handle(ZRState state) {

		EventType tp = this.getType();

		// Check if this is a write and the last event was a write
		// on the same variable by the same thread
		// If so, skip everything
		if (tp.isWrite()) {
			if (state.lastType != null) {
				if (state.lastType.isWrite()) {
					if (state.lastDecor == this.getVariable().getId()) {
						if (state.lastThread == this.getThread().getId()) {
							return state.lastAnswer;
						}
					}
				}
			}
		}

		if (tp.isAccessType()) {
			state.checkAndAddVariable(this.getVariable());
		}
		if (tp.isLockType()) {
			state.checkAndAddLock(this.getLock());
		}

		boolean toReturn;
		// if(ZROEventStatistics.isEnabled()) {
		// long startTime = System.currentTimeMillis();
		//
		// toReturn = this.HandleSub(state);
		//
		// long stopTime = System.currentTimeMillis();
		// ZROEventStatistics.updateTime(this.getType(), (stopTime - startTime));
		// } else {
		toReturn = this.HandleSub(state);
		// }

		state.lastDecor = -1;
		state.lastThread = -1;
		state.lastType = null;
		state.lastAnswer = toReturn;
		if (this.getType().isWrite()) {
			state.lastDecor = this.getVariable().getId();
			state.lastThread = this.getThread().getId();
			state.lastType = tp;
		}

		flush_event_ctr = flush_event_ctr + 1;
		if (flush_event_ctr == flushEventDuration) {
			state.flushAcquireViews();
			flush_event_ctr = 0;
		}

		/*
		 * Thread t = this.getThread(); HashSet<Lock> locksHeld =
		 * state.threadToLocksHeld.get(t); if(!locksHeld.isEmpty()){ int local_clock =
		 * state.getIndex(state.getVectorClock(state.clockThread, t), t); Pair<Integer,
		 * HashSet<Lock>> pair_to_store = new Pair<Integer, HashSet<Lock>> (local_clock,
		 * new HashSet<Lock> (locksHeld));
		 * state.openLockInfo.get(t).pushTop(pair_to_store); }
		 */

		return toReturn;
		// return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(ZRState state) {
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
				str += "C_t = ";
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(ZRState state) {
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
	public void printRaceInfoExtremeType(ZRState state) {
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
	public boolean HandleSubAcquire(ZRState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();

		if (!state.threadsAccessingLocks.containsKey(l)) {
			state.threadsAccessingLocks.put(l, new HashSet<Thread>());
		}
		state.threadsAccessingLocks.get(l).add(t);

		state.numAcquires = state.numAcquires + 1;
		state.incClockThread(getThread());
		state.updateViewAsWriterAtAcquire(l, t);
		this.printRaceInfo(state);

		state.addLockHeld(t, l);

		return false;
	}

	@Override
	public boolean HandleSubRelease(ZRState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.incClockThread(t);
		state.updateViewAsWriterAtRelease(l, t);
		state.removeLockHeld(t, l);
		this.printRaceInfo(state);

		return false;
	}

	private EventType getEarlierConflictingEvent(ZRState state, Thread t, Variable x,
			EventType a, Thread u) {
		Triplet<VectorClock, Integer, Long> writeTriplet = null;
		int writeClock = 0;
		EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>> store_write = state.accessInfo
				.get(u).get(EventType.WRITE).get(x);
		if (!store_write.isEmpty(t)) {
			writeTriplet = store_write.bottom(t);
			writeClock = writeTriplet.second;
		}
		Triplet<VectorClock, Integer, Long> readTriplet = null;
		int readClock = 0;
		if (a.equals(EventType.WRITE)) {
			EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>> store_read = state.accessInfo
					.get(u).get(EventType.READ).get(x);
			if (!store_read.isEmpty(t)) {
				readTriplet = store_read.bottom(t);
				readClock = readTriplet.second;
			}
		}

		if (writeClock > 0 && readClock > 0) {
			return readClock < writeClock ? EventType.READ : EventType.WRITE;
		} else if (writeClock > 0) {
			return EventType.WRITE;
		} else if (readClock > 0) {
			return EventType.READ;
		} else
			return null;
	}

	private boolean checkRaces(ZRState state, Thread t, Variable x, EventType a,
			VectorClock C_pred_t, Long auxId) {
		HashSet<Thread> threadSet_x = state.variableToThreadSet.get(x);
		for (Thread u : threadSet_x) {
			if (!u.equals(t)) {

				while (true) {
					EventType aprime = getEarlierConflictingEvent(state, t, x, a, u);
					if (!(aprime == null)) {
						EfficientLLView<Thread, Triplet<VectorClock, Integer, Long>> store = state.accessInfo
								.get(u).get(aprime).get(x);
						if (!store.isEmpty(t)) {
							Triplet<VectorClock, Integer, Long> conflictingTriplet = store
									.bottom(t);
							VectorClock C_pred_u = conflictingTriplet.first;
							int C_u_u = conflictingTriplet.second;

							// Cheap check
							if (C_u_u <= state.getIndex(C_pred_t, u)) {
								state.flushConflictingEventsEagerly(store, t, a, x, u,
										C_u_u, C_pred_t);
								continue;
							}

							Quintet<Thread, EventType, Thread, EventType, Variable> acquireInfoKey = new Quintet<Thread, EventType, Thread, EventType, Variable>(
									u, aprime, t, a, x);

							VectorClock I = new VectorClock(C_pred_t);
							I.updateWithMax(I, C_pred_u);
							VectorClock lastIeal = state.lastIdeal.get(acquireInfoKey);
							I.updateWithMax(I, lastIeal);

							// if(ZROEventStatistics.isEnabled()) {
							// long startTime = System.currentTimeMillis();
							// I.copyFrom(state.fixPointIdeal(acquireInfoKey, I));
							// long stopTime = System.currentTimeMillis();
							//
							// ZROEventStatistics.updateFPTime(this.getType(), (stopTime -
							// startTime), state.numIters);
							// } else {
							I.copyFrom(state.fixPointIdeal(acquireInfoKey, I, t));
							// }

							state.lastIdeal.put(acquireInfoKey, new VectorClock(I));

							if (!(C_u_u <= state.getIndex(I, u))) {
								long d = auxId - conflictingTriplet.third;
								if (state.maxDistance < d) {
									state.maxDistance = d;
								}
								state.sumDistance = state.sumDistance + d;
								state.numRaces = state.numRaces + 1;
								return true;
							} else {
								state.flushConflictingEventsEagerly(store, t, a, x, u,
										C_u_u, I);
							}
						}
					} else {
						break;
					}
				}
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubRead(ZRState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		EventType tp = this.getType();
		VectorClock C_t = state.getVectorClock(state.clockThread, t);

		// Check race
		VectorClock C_pred_t = new VectorClock(C_t);
		boolean emptyLS = state.updateLocksetAtAccess(t, v, tp);
		boolean raceDetected = false;
		if (emptyLS) {
			raceDetected = checkRaces(state, t, v, tp, C_pred_t, this.getAuxId());
		}

		// Update and send clocks
		state.incClockThread(t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, v);
		C_t.updateWithMax(C_t, LW_v);
		// Eager computation of closure. Do it after checking for races.
		C_t.copyFrom(state.updatePointersAtAccessAndGetFixPoint(t, C_t));

		Triplet<VectorClock, Integer, Long> infoToStore = new Triplet<VectorClock, Integer, Long>(
				C_pred_t, state.getIndex(C_t, t), this.getAuxId());
		state.accessInfo.get(t).get(EventType.READ).get(v).pushTop(infoToStore);

		this.printRaceInfo(state);
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(ZRState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		EventType tp = this.getType();
		VectorClock C_t = state.getVectorClock(state.clockThread, t);

		// Check race
		VectorClock C_pred_t = new VectorClock(C_t);
		boolean emptyLS = state.updateLocksetAtAccess(t, v, tp);
		boolean raceDetected = false;
		if (emptyLS) {
			raceDetected = checkRaces(state, t, v, tp, C_pred_t, this.getAuxId());
		}

		// Do send stuff
		state.incClockThread(t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, v);
		LW_v.copyFrom(C_t);
		Triplet<VectorClock, Integer, Long> infoToStore = new Triplet<VectorClock, Integer, Long>(
				C_pred_t, state.getIndex(C_t, t), this.getAuxId());
		state.accessInfo.get(t).get(EventType.WRITE).get(v).pushTop(infoToStore);

		this.printRaceInfo(state);
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(ZRState state) {
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
	public boolean HandleSubJoin(ZRState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state);
			state.incClockThread(getThread());
			// Eager computation of closure.
			C_t.copyFrom(
					state.updatePointersAtAccessAndGetFixPoint(this.getThread(), C_t));
		}
		return false;
	}

}
