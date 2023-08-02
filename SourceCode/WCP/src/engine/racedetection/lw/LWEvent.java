package engine.racedetection.lw;

import java.util.HashMap;

import engine.racedetection.RaceDetectionEvent;
import util.vectorclock.VectorClockBitMap;
import event.Thread;
import event.Lock;
import event.Variable;

public class LWEvent extends RaceDetectionEvent<LWState> {

	@Override
	public boolean Handle(LWState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(LWState state) {
		if(this.getType().isLockType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClockBitMap C_t = state.getVectorClock(state.clockThread, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public void printRaceInfoAccessType(LWState state) {
		if(this.getType().isAccessType()){
			if(state.verbosity == 1 || state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getVariable().getName();
				str += "|";
				VectorClockBitMap C_t = state.getVectorClock(state.clockThread, this.getThread());
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
	public void printRaceInfoExtremeType(LWState state) {
		if(this.getType().isExtremeType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClockBitMap C_t = state.getVectorClock(state.clockThread, this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public boolean HandleSubAcquire(LWState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		
		// ==== Maintain open locks ====
		HashMap<Lock, Integer> lock_to_depth = state.threadToLockNestingDepth.get(t);
		if(!lock_to_depth.containsKey(l)) {
			lock_to_depth.put(l, 0);
		}
		int curr_depth = lock_to_depth.get(l);
		lock_to_depth.put(l, curr_depth + 1);
		if (curr_depth == 0) {
			state.threadToOpenLocks.put(t, state.threadToOpenLocks.get(t) + 1);
		}
		// =============================
		
		VectorClockBitMap C_t = state.getVectorClock(state.clockThread, t);
		state.setBitIndex(C_t, t, true);

		this.printRaceInfo(state);
		
		state.incClockThread(t);
		return false;
	}

	@Override
	public boolean HandleSubRelease(LWState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		
		// ==== Maintain open locks ====
		HashMap<Lock, Integer> lock_to_depth = state.threadToLockNestingDepth.get(t);
		int curr_depth = lock_to_depth.get(l);
		lock_to_depth.put(l, curr_depth - 1);
		if (curr_depth == 1) {
			state.threadToOpenLocks.put(t, state.threadToOpenLocks.get(t) - 1);
		}
		// =============================
		
		VectorClockBitMap C_t = state.getVectorClock(state.clockThread, t);	
		state.setBitIndex(C_t, t, state.threadToOpenLocks.get(t) != 0);
		
		this.printRaceInfo(state);
		
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(LWState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		
		VectorClockBitMap C_t  = state.getVectorClock(state.clockThread, t);
		VectorClockBitMap LW_v = state.getVectorClock(state.lastWriteVariable, v);
		VectorClockBitMap W_v  = state.getVectorClock(state.writeVariable, v);

		this.printRaceInfo(state);

		boolean raceDetected = false;
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		
		C_t.updateWithMax(C_t, LW_v);

		if (C_t.isBitMapZero()) {
			VectorClockBitMap R_v  = state.getVectorClock(state.readVariable, v);
			int c_t_t = state.getClockIndex(C_t, this.getThread());
			state.setClockIndex(R_v, this.getThread(), c_t_t);
		}
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(LWState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		
		VectorClockBitMap C_t = state.getVectorClock(state.clockThread, t);
		VectorClockBitMap R_v = state.getVectorClock(state.readVariable, v);
		VectorClockBitMap W_v = state.getVectorClock(state.writeVariable, v);

		this.printRaceInfo(state);
		
		boolean raceDetected = false;
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		
		VectorClockBitMap LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		LW_v.copyFrom(C_t);
		state.setLWLocId(this.getVariable(), this.getLocId());
		state.incClockThread(getThread());
		
		if (C_t.isBitMapZero()) {
			int c_t_t = state.getClockIndex(C_t, this.getThread());
			state.setClockIndex(W_v, this.getThread(), c_t_t);
		}
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(LWState state) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			VectorClockBitMap C_t = state.getVectorClock(state.clockThread, t);			
			VectorClockBitMap C_tc = state.getVectorClock(state.clockThread, tc);
			C_tc.copyFrom(C_t);
			state.setClockIndex(C_tc, tc, 1);
			this.printRaceInfo(state);
			state.incClockThread(getThread());
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(LWState state) {
		Thread t = this.getThread();
		Thread tc = this.getTarget();
		if (state.isThreadRelevant(tc)) {
			VectorClockBitMap C_t = state.getVectorClock(state.clockThread, t);
			VectorClockBitMap C_tc = state.getVectorClock(state.clockThread, tc);
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
