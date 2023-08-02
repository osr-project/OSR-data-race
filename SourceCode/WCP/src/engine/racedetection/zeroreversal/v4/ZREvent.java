package engine.racedetection.zeroreversal.v4;

import java.util.HashMap;
import java.util.HashSet;

import debug.ZROEventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Thread;
import event.Variable;
import event.EventType;
import event.Lock;
import util.Pair;
import util.Quintet;
import util.Triplet;
import util.ll.EfficientLLView;
import util.ll.EfficientNode;
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
		if(tp.isWrite()) {
			if(state.lastType != null) {
				if(state.lastType.isWrite()) {
					if(state.lastDecor == this.getVariable().getId()) {
						if(state.lastThread == this.getThread().getId()) {
							return state.lastAnswer;
						}
					}
				}
			}
		}

		if(tp.isAccessType()) {
			state.checkAndAddVariable(this.getVariable());
		}
		if(tp.isLockType()) {
			state.checkAndAddLock(this.getLock());
		}

		boolean toReturn;
		if(ZROEventStatistics.isEnabled()) {
			long startTime = System.currentTimeMillis();

			toReturn = this.HandleSub(state);

			long stopTime = System.currentTimeMillis();
			ZROEventStatistics.updateTime(this.getType(), (stopTime - startTime));
		} else {
			toReturn = this.HandleSub(state);
		}

		state.lastDecor = -1;
		state.lastThread = -1;
		state.lastType = null;
		state.lastAnswer = toReturn;
		if(this.getType().isWrite()) {
			state.lastDecor = this.getVariable().getId();
			state.lastThread = this.getThread().getId();
			state.lastType = tp;
		}
		
		flush_event_ctr = flush_event_ctr + 1;
		if(flush_event_ctr == flushEventDuration) {
			state.flushAcquireViews();
			flush_event_ctr = 0;
		}
		
		/*
		Thread t = this.getThread();
		HashSet<Lock> locksHeld = state.threadToLocksHeld.get(t);
		if(!locksHeld.isEmpty()){
			int local_clock = state.getIndex(state.getVectorClock(state.clockThread, t), t);
			Pair<Integer, HashSet<Lock>> pair_to_store = new Pair<Integer, HashSet<Lock>> (local_clock, new HashSet<Lock> (locksHeld));
			state.openLockInfo.get(t).pushTop(pair_to_store);
		}
		*/
		
		return toReturn;
		//		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(ZRState state) {
		if(this.getType().isLockType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
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
		if(this.getType().isAccessType()){
			if(state.verbosity == 1 || state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getVariable().getName();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
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
		if(this.getType().isExtremeType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
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

		if(!state.threadsAccessingLocks.containsKey(l)) {
			state.threadsAccessingLocks.put(l, new HashSet<Thread> ());
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
		state.incClockThread(getThread());
		state.updateViewAsWriterAtRelease(this.getLock(), this.getThread());
		state.removeLockHeld(this.getThread(), this.getLock());
		this.printRaceInfo(state);
		return false;
	}

	// Modifies I_old
	private VectorClock fixPointIdeal(ZRState state, Quintet<Thread, EventType, Thread, EventType, Variable> acquireInfoKey, VectorClock I_old) {
		VectorClock I = new VectorClock(I_old);
		state.numIters = 0;
		while(true) {
			state.numIters = state.numIters + 1;
			HashSet<Thread> threads_in_I = new HashSet<Thread> ();
			HashMap<Thread, Triplet<Integer, Long, VectorClock>> base_triplets = new HashMap<Thread, Triplet<Integer, Long, VectorClock>> ();
			for(Thread v: state.threads) {
				int I_v = state.getIndex(I, v);
				if(I_v > 0) {
					threads_in_I.add(v);
					Triplet<Integer, Long, VectorClock> triplet_I_v = new Triplet<Integer, Long, VectorClock> (I_v, 0L, null);
					base_triplets.put(v, triplet_I_v);
				}
			}
//			HashSet<Lock> openLocks = state.openLocksInIdeal(I, acquireInfoKey);
//			for(Lock l: openLocks) {
			for(Lock l: state.threadsAccessingLocks.keySet()) {
				long LA_l = -1;
				VectorClock maxVC_match_l = null;
				Thread max_thread = null;
				Pair<EfficientNode<Triplet<Integer, Long, VectorClock>>, Integer> max_nextNode = null;
				HashSet<Thread> threads_accessing_l_and_in_I = new HashSet<Thread> (state.threadsAccessingLocks.get(l));
				threads_accessing_l_and_in_I.retainAll(threads_in_I);
				for(Thread v: threads_accessing_l_and_in_I) {
					EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>> store = state.acquireInfo.get(v).get(l);
					// if store is empty then skip
					if (store.isEmpty()) {
						state.threadsAccessingLocks.get(l).remove(v);
						if(state.threadsAccessingLocks.get(l).isEmpty()) {
							state.threadsAccessingLocks.remove(l);
						}
						continue;
					}

					if(store.isEmpty(acquireInfoKey)) {
						continue;
					}
					Triplet<Integer, Long, VectorClock> bottomPointer = store.bottom(acquireInfoKey);
					if(bottomPointer.first > state.getIndex(I, v)) {
						continue;
					}

					// change the following to With Update
					Triplet<Boolean, Triplet<Integer, Long, VectorClock>, Pair<EfficientNode<Triplet<Integer, Long, VectorClock>>, Integer>> found_lockTriplet_nextNodeIter = store.getMaxLowerBoundPenultimate(acquireInfoKey, base_triplets.get(v), state.firstComparatorAcquireInfo);
					if(found_lockTriplet_nextNodeIter.first) {
						Triplet<Integer, Long, VectorClock> lockTriplet = found_lockTriplet_nextNodeIter.second;
						long GI_v_l = lockTriplet.second;
						VectorClock C_match_v_l = lockTriplet.third;
						if(LA_l == -1) {
							LA_l = GI_v_l;
							maxVC_match_l = C_match_v_l;
							max_thread = v;
							max_nextNode = found_lockTriplet_nextNodeIter.third;
						}
						else {
							if(GI_v_l > LA_l) {
								I.updateWithMax(I, maxVC_match_l);
								state.acquireInfo.get(max_thread).get(l).setBottom(acquireInfoKey, max_nextNode);

								LA_l = GI_v_l;
								maxVC_match_l = C_match_v_l;
								max_thread = v;
								max_nextNode = found_lockTriplet_nextNodeIter.third;
							}
							else {								
								I.updateWithMax(I, C_match_v_l);
								state.acquireInfo.get(v).get(l).setBottom(acquireInfoKey, found_lockTriplet_nextNodeIter.third);
							}
						}
					}
					// TODO: If you are not clearing the store, it cannot become empty at this point.
//					if (store.isEmpty()) {
//						state.threadsAccessingLocks.get(l).remove(v);
//						if(state.threadsAccessingLocks.get(l).isEmpty()) {
//							state.threadsAccessingLocks.remove(l);
//						}
//					}
				}
			}
			if(I.isEqual(I_old)) {
				break;
			}
			I_old.copyFrom(I);
		}
		return I;
	}

	
	// First remove all elements (_, C, _) of all stores
	// AccessInfo(u, a(x))_t for all a \in {r, w} and x \in Vars
	// such that C <= I
	// Also record the minimum over all such clocks C
	// Next, for every other thread u' \not\in {t, u},
	// remove the earliest entries (prefix) (_, C', _) of AccessInfo(v, b(y))
	// such that C <= C' <= I
	private void clearViews(ZRState state, Thread t, EventType a, Variable x, Thread u, int lb_local_clock, VectorClock ub_clock) {
		VectorClock lb_clock = new VectorClock(state.threads.size());
		state.setIndex(lb_clock, u, lb_local_clock);
		Pair<VectorClock, Integer> lb = new Pair<VectorClock, Integer> (lb_clock, -1);
		HashSet<Thread> threadSet_x = state.variableToThreadSet.get(x);
		for(Thread v: threadSet_x) {
			if(v.equals(t)) {
				continue;
			}
			int ub_local_clock = state.getIndex(ub_clock, v);
			Pair<VectorClock, Integer> ub = new Pair<VectorClock, Integer> (null, ub_local_clock);
			for(EventType aprime: ZRState.accessTypes) {
				if(EventType.conflicting(a, aprime)) {
					state.accessInfo.get(v).get(aprime).get(x).removePrefixWithinReturnMin(t, lb, state.firstComparatorAccessInfo, ub, state.secondComparatorAccessInfo);
				}
			}
		}
	}
	
	private void flushConflictingEventsEagerly(EfficientLLView<Thread, Pair<VectorClock, Integer>> store, ZRState state, Thread t, EventType a, Variable x, Thread u, int lb_local_clock, VectorClock ub_clock) {
		store.advanceKeyByOne(t);
		clearViews(state, t, a, x, u, lb_local_clock, ub_clock);
	}

	private EventType getEarlierConflictingEvent(ZRState state, Thread t, Variable x, EventType a, Thread u) {
		Pair<VectorClock, Integer> writeTriplet = null;
		int writeClock = 0;
		EfficientLLView<Thread, Pair<VectorClock, Integer>> store_write = state.accessInfo.get(u).get(EventType.WRITE).get(x);
		if(!store_write.isEmpty(t)) {
			writeTriplet = store_write.bottom(t);
			writeClock = writeTriplet.second;
		}
		Pair<VectorClock, Integer> readTriplet = null;
		int readClock = 0;
		if(a.equals(EventType.WRITE)) {
			EfficientLLView<Thread, Pair<VectorClock, Integer>> store_read = state.accessInfo.get(u).get(EventType.READ).get(x);
			if(!store_read.isEmpty(t)) {
				readTriplet = store_read.bottom(t);
				readClock = readTriplet.second;
			}
		}

		if(writeClock > 0 && readClock > 0) {
			return readClock < writeClock ? EventType.READ : EventType.WRITE ;
		}
		else if(writeClock > 0) {
			return EventType.WRITE;
		}
		else if(readClock > 0) {
			return EventType.READ;
		}
		else return null;
	}

	private boolean checkRaces(ZRState state, Thread t, Variable x, EventType a, VectorClock C_pred_t) {
		HashSet<Thread> threadSet_x = state.variableToThreadSet.get(x);
		for(Thread u: threadSet_x) {
			if(!u.equals(t)) {

				while(true) {
					EventType aprime = getEarlierConflictingEvent(state, t, x, a, u);
					if(!(aprime == null)) {
						EfficientLLView<Thread, Pair<VectorClock, Integer>> store = state.accessInfo.get(u).get(aprime).get(x);
						if(!store.isEmpty(t)) {
							Pair<VectorClock, Integer> conflictingTriplet = store.bottom(t);
							VectorClock C_pred_u = conflictingTriplet.first;
							int C_u_u = conflictingTriplet.second;

							// Cheap check
							if(C_u_u <= state.getIndex(C_pred_t, u)) {
								flushConflictingEventsEagerly(store, state, t, a, x, u, C_u_u, C_pred_t);
								continue;
							}

							Quintet<Thread, EventType, Thread, EventType, Variable> acquireInfoKey = new Quintet<Thread, EventType, Thread, EventType, Variable>(u, aprime, t, a, x);

							VectorClock I = new VectorClock(C_pred_t);
							I.updateWithMax(I, C_pred_u);
							VectorClock lastIeal = state.lastIdeal.get(acquireInfoKey);
							I.updateWithMax(I, lastIeal);

							if(ZROEventStatistics.isEnabled()) {
								long startTime = System.currentTimeMillis();
								I = this.fixPointIdeal(state, acquireInfoKey, I);
								long stopTime = System.currentTimeMillis();

								ZROEventStatistics.updateFPTime(this.getType(), (stopTime - startTime), state.numIters);
							} else {
								I = this.fixPointIdeal(state, acquireInfoKey, I);
							}

							state.lastIdeal.put(acquireInfoKey, I);

							if(!(C_u_u <= state.getIndex(I, u))) {
								return true;
							}
							else {
								flushConflictingEventsEagerly(store, state, t, a, x, u, C_u_u, I);
							}
						}
					}
					else {
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

		VectorClock C_t  = state.getVectorClock(state.clockThread, t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, v);

		VectorClock C_pred_t = new VectorClock(C_t);

		state.incClockThread(t);
		C_t.updateWithMax(C_t, LW_v);

		// TODO instead of storing C_t, store C_t(t) in the second component.
		Pair<VectorClock, Integer> infoToStore = new Pair<VectorClock, Integer> (C_pred_t, state.getIndex(C_t, t));
		state.accessInfo.get(t).get(EventType.READ).get(v).pushTop(infoToStore);

		boolean emptyLS = state.updateLocksetAtAccess(t, v, tp);
		boolean raceDetected = false;
		if(emptyLS) {
			raceDetected = checkRaces(state, t, v, tp, C_pred_t);
		}

		this.printRaceInfo(state);
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(ZRState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		EventType tp = this.getType();
		VectorClock C_t  = state.getVectorClock(state.clockThread, t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, v);
		VectorClock C_pred_t = new VectorClock(C_t);

		state.incClockThread(t);

		LW_v.copyFrom(C_t);

		Pair<VectorClock, Integer> infoToStore = new Pair<VectorClock, Integer> (C_pred_t, state.getIndex(C_t, t));
		state.accessInfo.get(t).get(EventType.WRITE).get(v).pushTop(infoToStore);

		boolean emptyLS = state.updateLocksetAtAccess(t, v, tp);
		boolean raceDetected = false;
		if(emptyLS) {
			raceDetected = checkRaces(state, t, v, tp, C_pred_t);
		}

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
		}
		return false;
	}

}
