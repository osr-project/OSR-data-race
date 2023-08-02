package engine.racedetection.soundfhb;

import engine.racedetection.RaceDetectionEvent;
import util.vectorclock.VectorClock;

public class SoundFHBEvent extends RaceDetectionEvent<SoundFHBState> {

	@Override
	public boolean Handle(SoundFHBState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(SoundFHBState state) {
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
	public void printRaceInfoAccessType(SoundFHBState state) {
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
	public void printRaceInfoExtremeType(SoundFHBState state) {
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
	public boolean HandleSubAcquire(SoundFHBState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		C_t.updateWithMax(C_t, L_l);
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(SoundFHBState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		this.printRaceInfo(state);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubRead(SoundFHBState state) {
				
		boolean raceDetected = false;
		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock W_v  = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
			
			state.addLocPair(state.getLWLocId(this.getVariable()), this.getLocId());
			System.out.println(state.getLocPairs());
			
			//Force order
			C_t.updateWithMax(C_t, W_v);
		}
		
		this.printRaceInfo(state);

		VectorClock R_v  = state.getVectorClock(state.readVariable, getVariable());
		R_v.updateWithMax(R_v, C_t);
		
		state.setLastReadData(this.getVariable(), this.getThread(), this.getLocId(), C_t);
		
		state.incClockThread(getThread());

		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(SoundFHBState state) {
		
		boolean raceDetected = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state);
		
		if(R_v.isLessThanOrEqual(W_v)){
			if (!(W_v.isLessThanOrEqual(C_t))) {
				raceDetected = true;
				
				state.addLocPair(state.getLWLocId(this.getVariable()), this.getLocId());
				System.out.println(state.getLocPairs());
			}
		}
		else{
			raceDetected = state.checkRaceWithReadsAndAddLocPairs(this.getThread(), this.getVariable(), C_t, this.getLocId());
		}
		
		C_t.updateWithMax(C_t, W_v, R_v);

		this.printRaceInfo(state);
		
		W_v.copyFrom(C_t);
		
		state.setLWLocId(this.getVariable(),this.getLocId());

		state.incClockThread(getThread());
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(SoundFHBState state) {
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
	public boolean HandleSubJoin(SoundFHBState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
