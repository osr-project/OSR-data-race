package engine.racedetection.fhb;

import engine.racedetection.RaceDetectionEvent;
import util.vectorclock.VectorClock;

public class FHBEvent extends RaceDetectionEvent<FHBState> {

	@Override
	public boolean Handle(FHBState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(FHBState state) {
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
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public void printRaceInfoAccessType(FHBState state) {
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
				str += "@" + state.getThreadIndex(this.getThread());
				str += "|";
				str += this.getAuxId();
				System.out.println(str);
			}	
		}		
	}

	@Override
	public void printRaceInfoExtremeType(FHBState state) {
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
	public boolean HandleSubAcquire(FHBState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(FHBState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		this.printRaceInfo(state);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(FHBState state) {
		boolean raceDetected = false;
		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock W_v  = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			
			//Force order
			C_t.updateWithMax(C_t, W_v);
		}

		this.printRaceInfo(state);

		VectorClock R_v  = state.getVectorClock(state.readVariable, getVariable());
		R_v.updateWithMax(R_v, C_t);
		
		state.incClockThread(getThread());

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(FHBState state) {
		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state);
		
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		
		//Now force order
//		if (!(R_v.isLessThanOrEqual(C_t))) {
//			C_t.updateWithMax(C_t, R_v);
//		}
//		if (!(W_v.isLessThanOrEqual(C_t))) {
//			C_t.updateWithMax(C_t, W_v);
//		}
		
		C_t.updateWithMax(C_t, W_v, R_v);

		this.printRaceInfo(state);
		
		W_v.copyFrom(C_t);

		state.incClockThread(getThread());
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(FHBState state) {
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
	public boolean HandleSubJoin(FHBState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
