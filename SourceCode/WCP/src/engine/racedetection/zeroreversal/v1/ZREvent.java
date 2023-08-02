package engine.racedetection.zeroreversal.v1;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEvent;
import event.Thread;
import event.Variable;
import event.EventType;
import event.Lock;
import util.Pair;
import util.Quintet;
import util.Triplet;
import util.ll.EfficientLLView;
import util.vectorclock.VectorClock;

public class ZREvent extends RaceDetectionEvent<ZRState> {

	@Override
	public boolean Handle(ZRState state) {
		
		EventType tp = this.getType();
		if(tp.isAccessType()) {
			state.checkAndAddVariable(this.getVariable());
		}
		if(tp.isLockType()) {
			state.checkAndAddLock(this.getLock());
		}
			
		return this.HandleSub(state);
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
				LockVC LA_t = state.getVectorClock(state.lastAcquireThread, this.getThread());
				str += "LA_t = ";
				str += LA_t.toString();
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
		
		state.locksAccessed.get(t).add(l);
		
		if(!state.isReEntrant(t, l)) {
			state.numAcquires = state.numAcquires + 1;
			LockVC LA_t = state.getVectorClock(state.lastAcquireThread, t);
			state.setIndex(LA_t, l, state.numAcquires);
			state.incClockThread(getThread());
			state.updateViewAsWriterAtAcquire(l, t);
			this.printRaceInfo(state);
		}
		
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
	private VectorClock fixPointIdeal(ZRState state, Quintet<Thread, EventType, Thread, EventType, Variable> acquireInfoKey, VectorClock I_old, LockVC LA_old) {
		VectorClock I = new VectorClock(I_old);
		LockVC LA = new LockVC(LA_old);
		//		int i = 0;
		while(true) {
			//			if(acquireInfoKey.fifth.getName().equals("x")) {
			//				System.out.println("\n\n@@@@@@@@@ Round  " + i++);
			//			}
			for(Thread v: state.threads) {
				int I_v = state.getIndex(I, v);
				if(I_v <= 0) {
					continue;
				}
				Triplet<Integer, Long, VectorClock> triplet_I_v = new Triplet<Integer, Long, VectorClock> (I_v, 0L, null);
				HashSet<Lock> locks_v = state.locksAccessed.get(v);
				for(Lock l: locks_v) {
					EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>> store = state.acquireInfo.get(v).get(l);

					//					System.out.println("%% VARNAME = " + acquireInfoKey.fifth.getName());
					//					if(acquireInfoKey.fifth.getName().equals("e")) {
					//						System.out.println("%% Looking up M_{" + v + "," + l + "} for key " + triplet_I_v);
					//						System.out.println(store);
					//					}

					Pair<Boolean, Triplet<Integer, Long, VectorClock>> found_lockTriplet = store.getMaxLowerBoundWithoutUpdate(acquireInfoKey, triplet_I_v, state.firstComparatorAcquireInfo);
					if(found_lockTriplet.first) {
						Triplet<Integer, Long, VectorClock> lockTriplet = found_lockTriplet.second;

						//						if(acquireInfoKey.fifth.getName().equals("x")) {
						//							System.out.println("\n%% Looking up M_{" + v + "," + l + "} for key " + triplet_I_v);
						//							System.out.println("%% Found node " + lockTriplet);
						//							System.out.println(store.toStoreString());
						//						}

						long GI_v_l = lockTriplet.second;
						VectorClock C_match_v_l = lockTriplet.third;
						long LA_l = state.getIndex(LA, l);

						//						if(acquireInfoKey.fifth.getName().equals("x")) {
						//							System.out.println("$$$ GI_v_l = " + GI_v_l);
						//							System.out.println("$$$ LA_l = " + LA_l);
						//						}

						if(GI_v_l > LA_l) {
							state.setIndex(LA, l, GI_v_l);
							//							if(acquireInfoKey.fifth.getName().equals("x")) {
							//								System.out.println("%% IF CONDITION");
							//								System.out.println("%% LA = " + LA);
							//							}
						}
						else if(GI_v_l < LA_l) {
//							System.out.println("Thread = " + v + ", Lock = " + l + ", key = " + acquireInfoKey);
//							System.out.println("C_match_v_l = " + C_match_v_l);
							if(C_match_v_l == null) {
								System.out.println("NULL at " + this.getAuxId());
							}
							I.updateWithMax(I, C_match_v_l);
							state.acquireInfo.get(v).get(l).getMaxLowerBound(acquireInfoKey, triplet_I_v, state.firstComparatorAcquireInfo);
							//							if(acquireInfoKey.fifth.getName().equals("x")) {
							//								System.out.println("%% ELSE-IF CONDITION");
							//								System.out.println("%% I = " + I);
							//							}
						}
						//						else {
						//							if(acquireInfoKey.fifth.getName().equals("x")) {
						//								System.out.println("%% ELSE CONDITION");
						//							}
						//						}
					}
					
					if (store.isEmpty()) {
						locks_v.remove(l);
					}
				}
			}

			//			if(acquireInfoKey.fifth.getName().equals("x")) {
			//				System.out.println("\n@@@@@ I_old = " + I_old + ", I = " + I);
			//				System.out.println("@@@@@ LA = " + LA);
			//			}

			if(I.isEqual(I_old) && LA.isEqual(LA_old)) {
				break;
			}
			I_old.copyFrom(I);
			LA_old.copyFrom(LA);
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
	private void clearViews(ZRState state, Thread t, EventType a, Variable x, VectorClock lb_clock, VectorClock ub_clock) {
		VectorClock bottomVC = new VectorClock(state.threads.size());
		LockVC bottomLockVC = new LockVC(0);
		Triplet<VectorClock, VectorClock, LockVC> lb = new Triplet<VectorClock, VectorClock, LockVC> (bottomVC, lb_clock, bottomLockVC);
		Triplet<VectorClock, VectorClock, LockVC> ub = new Triplet<VectorClock, VectorClock, LockVC> (bottomVC, ub_clock, bottomLockVC);
		HashSet<Thread> threadSet_x = state.variableToThreadSet.get(x);
		for(Thread v: threadSet_x) {
			if(v.equals(t)) {
				continue;
			}
			for(EventType aprime: ZRState.accessTypes) {
				if(EventType.conflicting(a, aprime)) {
					state.accessInfo.get(v).get(aprime).get(x).removePrefixWithinReturnMin(t, lb, ub, state.secondComparatorAccessInfo);
				}
			}

		}

	}

	private EventType getEarlierConflictingEvent(ZRState state, Thread t, Variable x, EventType a, Thread u) {
		Triplet<VectorClock, VectorClock, LockVC> writeTriplet = null;
		VectorClock writeClock = null;
		EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>> store_write = state.accessInfo.get(u).get(EventType.WRITE).get(x);
		if(!store_write.isEmpty(t)) {
			writeTriplet = store_write.bottom(t);
			writeClock = writeTriplet.second;
			if(writeClock.isZero()) {
				writeClock = null;
			}
		}
		Triplet<VectorClock, VectorClock, LockVC> readTriplet = null;
		VectorClock readClock = null;
		if(a.equals(EventType.WRITE)) {
			EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>> store_read = state.accessInfo.get(u).get(EventType.READ).get(x);
			if(!store_read.isEmpty(t)) {
				readTriplet = store_read.bottom(t);
				readClock = readTriplet.second;
				if(readClock.isZero()) {
					readClock = null;
				}
			}
		}

		if(!(writeClock == null || readClock == null)) {
			return readClock.isLessThanOrEqual(writeClock) ? EventType.READ : EventType.WRITE ;
		}
		else if(!(writeClock == null)) {
			return EventType.WRITE;
		}
		else if(!(readClock == null)) {
			return EventType.READ;
		}
		else return null;
	}

	private boolean checkRaces(ZRState state, Thread t, Variable x, EventType a, VectorClock C_pred_t, LockVC LA_t) {
		HashSet<Thread> threadSet_x = state.variableToThreadSet.get(x);
		for(Thread u: threadSet_x) {
			if(!u.equals(t)) {

				while(true) {
					EventType aprime = getEarlierConflictingEvent(state, t, x, a, u);
					if(!(aprime == null)) {
						EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>> store = state.accessInfo.get(u).get(aprime).get(x);
						if(!store.isEmpty(t)) {
							Triplet<VectorClock, VectorClock, LockVC> conflictingTriplet = store.bottom(t);
							VectorClock C_pred_u = conflictingTriplet.first;
							VectorClock C_u = conflictingTriplet.second;
							LockVC LA_u = conflictingTriplet.third;

							// Cheap check
							if(C_u.isLessThanOrEqual(C_pred_t)) {
								clearViews(state, t, a, x, C_u, C_pred_t);
								continue;
							}
							
							Quintet<Thread, EventType, Thread, EventType, Variable> acquireInfoKey = new Quintet<Thread, EventType, Thread, EventType, Variable>(u, aprime, t, a, x);

							VectorClock I = new VectorClock(C_pred_t);
							I.updateWithMax(I, C_pred_u);
							VectorClock lastIeal = state.lastIdeal.get(acquireInfoKey);
							I.updateWithMax(I, lastIeal);
							
							LockVC LA = new LockVC(LA_t);							
							LA.updateWithMax(LA, LA_u);


							//							System.out.println("%% VARNAME = " + acquireInfoKey.fifth.getName());
							//							if(acquireInfoKey.fifth.getName().equals("x")) {
							//								System.out.println("#### acquireInfoKey = " + acquireInfoKey);
							//								System.out.println("#### C_pred_t = " + C_pred_t);
							//								System.out.println("#### C_pred_u = " + C_pred_u);
							//								System.out.println("#### LA_old = " + LA);
							//								System.out.println("#### I_old = " + I);
							//							}
							I = this.fixPointIdeal(state, acquireInfoKey, I, LA);
							//							if(acquireInfoKey.fifth.getName().equals("x")) {
							//								System.out.println("#### I_new = " + I);
							//							}
							
							state.lastIdeal.put(acquireInfoKey, I);

							if(!(C_u.isLessThanOrEqual(I))) {
								return true;
							}
							else {
								clearViews(state, t, a, x, C_u, I);
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
		LockVC LA_t = state.getVectorClock(state.lastAcquireThread, t);
		LockVC LALW_v = state.getVectorClock(state.lastAcquireWriteVariable, v);

		VectorClock C_pred_t = new VectorClock(C_t);

		state.incClockThread(t);
		C_t.updateWithMax(C_t, LW_v);
		LA_t.updateWithMax(LA_t, LALW_v);

		Triplet<VectorClock, VectorClock, LockVC> infoToStore = new Triplet<VectorClock, VectorClock, LockVC> (C_pred_t, new VectorClock(C_t), new LockVC(LA_t));
		state.accessInfo.get(t).get(EventType.READ).get(v).pushTop(infoToStore);

		boolean emptyLS = state.updateLocksetAtAccess(t, v, tp);
		boolean raceDetected = false;
		if(emptyLS) {
			raceDetected = checkRaces(state, t, v, tp, C_pred_t, LA_t);
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
		LockVC LA_t = state.getVectorClock(state.lastAcquireThread, t);
		LockVC LALW_v = state.getVectorClock(state.lastAcquireWriteVariable, v);

		VectorClock C_pred_t = new VectorClock(C_t);

		state.incClockThread(t);

		LW_v.copyFrom(C_t);
		LALW_v.copyFrom(LA_t);

		Triplet<VectorClock, VectorClock, LockVC> infoToStore = new Triplet<VectorClock, VectorClock, LockVC> (C_pred_t, new VectorClock(C_t), new LockVC(LA_t));
		state.accessInfo.get(t).get(EventType.WRITE).get(v).pushTop(infoToStore);

		boolean emptyLS = state.updateLocksetAtAccess(t, v, tp);
		boolean raceDetected = false;
		if(emptyLS) {
			raceDetected = checkRaces(state, t, v, tp, C_pred_t, LA_t);
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
		}
		return false;
	}

}
