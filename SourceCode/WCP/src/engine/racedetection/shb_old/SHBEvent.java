package engine.racedetection.shb_old;

import engine.racedetection.RaceDetectionEvent;
import util.vectorclock.VectorClock;

public class SHBEvent extends RaceDetectionEvent<SHBState> {

	@Override
	public boolean Handle(SHBState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(SHBState state) {
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
	public void printRaceInfoAccessType(SHBState state) {
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
	public void printRaceInfoExtremeType(SHBState state) {
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
	public boolean HandleSubAcquire(SHBState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(SHBState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		this.printRaceInfo(state);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(SHBState state) {
		boolean raceDetected = false;
		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		VectorClock W_v  = state.getVectorClock(state.writeVariable, getVariable());

//		this.printRaceInfo(state);

		if (!(LW_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			
			if(state.verbosity>=1){
				System.out.println("@" + this.getLocId() + '+' + state.getLWLocId(this.getVariable()) );
			}
			
			C_t.updateWithMax(C_t, LW_v);
		}
		
//		this.printRaceInfo(state);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		this.printRaceInfo(state);

		VectorClock R_v  = state.getVectorClock(state.readVariable, getVariable());
		int c_t_t = state.getIndex(C_t, this.getThread());
		state.setIndex(R_v, this.getThread(), c_t_t);

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(SHBState state) {
		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

//		this.printRaceInfo(state);
		
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		this.printRaceInfo(state);
		
		int c_t_t = state.getIndex(C_t, this.getThread());
		state.setIndex(W_v, this.getThread(), c_t_t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		LW_v.copyFrom(C_t);
		state.setLWLocId(this.getVariable(), this.getLocId());
		state.incClockThread(getThread());
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(SHBState state) {
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
	public boolean HandleSubJoin(SHBState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
