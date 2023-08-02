package engine.racedetection.hb_treeclock;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import event.Lock;
import event.Thread;
import util.treeclock.iterative.TreeClock;

public class HBEvent extends RaceDetectionEvent<HBState> {

	@Override
	public boolean Handle(HBState state) {
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
	public void printRaceInfoLockType(HBState state) {
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
				TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
				str += C_t.toString();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(HBState state) {
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
				TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
				str += C_t.toString();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoExtremeType(HBState state) {
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
				TreeClock C_t = state.getTreeClock(state.clockThread, this.getThread());
				str += C_t.toString();
				System.out.println(str);
			}
		}
	}

	@Override
	public boolean HandleSubAcquire(HBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.addLock(l, t);
		state.incClockThread(getThread());

		TreeClock C_t = state.getTreeClock(state.clockThread, t);
		TreeClock L_l = state.getTreeClock(state.lastReleaseLock, l);

//		System.err.println("At acquire L_" + l.getName());
//		System.err.println(L_l.toString());

		C_t.join(L_l);

//		System.out.println("C_" + t.getName());
//		System.out.println(C_t.toString());

//		System.out.println("Hashcode of C_t = "+ C_t.root.hashCode());
//		System.out.println("Hashcode of L_l = "+ L_l.root.hashCode());

//		System.out.println("=======\n");

		return false;
	}

	@Override
	public boolean HandleSubRelease(HBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.addLock(l, t);
		state.incClockThread(getThread());

		TreeClock C_t = state.getTreeClock(state.clockThread, t);
		TreeClock L_l = state.getTreeClock(state.lastReleaseLock, l);

//		System.err.println("At release, before monotonecopy L_" + l.getName());
//		System.err.println(L_l.toString());

		L_l.monotoneCopy(C_t);

//		System.out.println("C_" + t.getName());
//		System.out.println(C_t.toString());

//		System.err.println("At release L_" + l.getName());
//		System.err.println(L_l.toString());

//		System.out.println("Hashcode of C_t = "+ C_t.root.hashCode());
//		System.out.println("Hashcode of L_l = "+ L_l.root.hashCode());

//		System.out.println("=======\n");
		return false;
	}

	@Override
	public boolean HandleSubRead(HBState state) {
		return false;
		/*
		 * boolean raceDetected = false; VectorClock C_t =
		 * state.generateVectorClockFromClockThread(this.getThread()); VectorClock R_v =
		 * state.getVectorClock(state.readVariable, getVariable()); VectorClock W_v =
		 * state.getVectorClock(state.writeVariable, getVariable());
		 * 
		 * this.printRaceInfo(state);
		 * 
		 * if (!(W_v.isLessThanOrEqual(C_t))) { raceDetected = true; }
		 * 
		 * if(raceDetected){ // System.out.println("HB race detected on variable " +
		 * this.getVariable().getName()); }
		 * 
		 * if( ! state.forceOrder){ R_v.updateWithMax(R_v, C_t); //
		 * this.printRaceInfo(state); if(state.tickClockOnAccess){
		 * state.incClockThread(getThread()); } } else{ VectorClock H_t =
		 * state.getVectorClock(state.HBPredecessorThread, this.getThread());
		 * 
		 * H_t.updateWithMax(H_t, W_v); C_t =
		 * state.generateVectorClockFromClockThread(this.getThread());
		 * this.printRaceInfo(state); R_v.updateWithMax(R_v, C_t);
		 * state.incClockThread(getThread()); } return raceDetected;
		 */
	}

	@Override
	public boolean HandleSubWrite(HBState state) {
		return false;
		/*
		 * boolean raceDetected = false; VectorClock C_t =
		 * state.generateVectorClockFromClockThread(this.getThread()); VectorClock R_v =
		 * state.getVectorClock(state.readVariable, getVariable()); VectorClock W_v =
		 * state.getVectorClock(state.writeVariable, getVariable());
		 * 
		 * this.printRaceInfo(state);
		 * 
		 * if (!(R_v.isLessThanOrEqual(C_t))) { raceDetected = true; } if
		 * (!(W_v.isLessThanOrEqual(C_t))) { raceDetected = true; }
		 * 
		 * if(raceDetected){ // System.out.println("HB race detected on variable " +
		 * this.getVariable().getName()); }
		 * 
		 * if( ! state.forceOrder){ W_v.updateWithMax(W_v, C_t); //
		 * this.printRaceInfo(state); if(state.tickClockOnAccess){
		 * state.incClockThread(getThread()); } } else{ VectorClock H_t =
		 * state.getVectorClock(state.HBPredecessorThread, this.getThread());
		 * 
		 * H_t.updateWithMax(H_t, W_v, R_v); C_t =
		 * state.generateVectorClockFromClockThread(this.getThread());
		 * this.printRaceInfo(state); W_v.updateWithMax(W_v, C_t);
		 * state.incClockThread(getThread()); } return raceDetected;
		 */
	}

	@Override
	public boolean HandleSubFork(HBState state) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			state.incClockThread(t);
			TreeClock C_t = state.getTreeClock(state.clockThread, t);
			TreeClock C_tc = state.getTreeClock(state.clockThread, tc);

			C_tc.join(C_t);

//			System.out.println(C_t.toString());
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(HBState state) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			state.incClockThread(getThread());
			TreeClock C_t = state.getTreeClock(state.clockThread, t);
			TreeClock C_tc = state.getTreeClock(state.clockThread, tc);

			C_t.join(C_tc);

//			System.out.println(C_t.toString());
		}
		return false;
	}

}
