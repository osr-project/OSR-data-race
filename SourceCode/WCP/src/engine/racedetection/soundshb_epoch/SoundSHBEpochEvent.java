package engine.racedetection.soundshb_epoch;

import engine.racedetection.RaceDetectionEvent;
import util.vectorclock.FullAdaptiveVC;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class SoundSHBEpochEvent extends RaceDetectionEvent<SoundSHBEpochState> {

	@Override
	public boolean Handle(SoundSHBEpochState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(SoundSHBEpochState state) {
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
	public void printRaceInfoAccessType(SoundSHBEpochState state) {
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
	public void printRaceInfoExtremeType(SoundSHBEpochState state) {
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
	public boolean HandleSubAcquire(SoundSHBEpochState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(SoundSHBEpochState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		this.printRaceInfo(state);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(SoundSHBEpochState state) {
		boolean raceDetected = false;
		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		FullAdaptiveVC W_v  = state.getAdaptiveVC(state.writeVariable, getVariable());

		if (!(LW_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			
			if(state.verbosity>=1){
				System.out.println("@" + this.getLocId() + '+' + state.getLWLocId(this.getVariable()) );
			}
			
			C_t.updateWithMax(C_t, LW_v);
		}
		

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		this.printRaceInfo(state);

		SemiAdaptiveVC R_v  = state.getAdaptiveVC(state.readVariable, getVariable());
		R_v.updateWithMax(C_t, state.getThreadIndex(this.getThread()));
		
		state.reInitializeFWALROnRead(this.getThread(), this.getVariable());

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(SoundSHBEpochState state) {
		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		FullAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());
		
		raceDetected = state.checkReadWriteRaceUsingFWALR(this.getVariable(), C_t, R_v);

		this.printRaceInfo(state); //THe position of this print statement has been changed; This is because once the next statement is executed, C_t can get altered. It is important to print C_t before this change.
		
		boolean W_v_isLTE_C_t = W_v.isLTEUpdateWithMax(C_t, state.getThreadIndex(this.getThread()));
		if(! W_v_isLTE_C_t){
			raceDetected = true;
		}
		
		VectorClock LW_v = state.getVectorClock(state.lastWriteVariable, getVariable());
		LW_v.copyFrom(C_t);
		
		state.setLWLocId(this.getVariable(), this.getLocId());
		
		int c_t_t = state.getIndex(C_t, this.getThread());
		state.setFWALROnWrite(this.getThread(), this.getVariable(), c_t_t);
		
		state.incClockThread(getThread());
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(SoundSHBEpochState state) {
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
	public boolean HandleSubJoin(SoundSHBEpochState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
