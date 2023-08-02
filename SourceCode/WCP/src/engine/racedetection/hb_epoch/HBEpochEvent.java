package engine.racedetection.hb_epoch;

import engine.racedetection.RaceDetectionEvent;
import util.vectorclock.SemiAdaptiveVC;
import util.vectorclock.VectorClock;

public class HBEpochEvent extends RaceDetectionEvent<HBEpochState> {

	@Override
	public boolean Handle(HBEpochState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(HBEpochState state) {
		if(this.getType().isLockType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public void printRaceInfoAccessType(HBEpochState state) {
		if(this.getType().isAccessType()){
			if(state.verbosity == 1 || state.verbosity == 2){
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
				str += "|";
				str += this.getAuxId();
				System.out.println(str);
			}	
		}		
	}

	@Override
	public void printRaceInfoExtremeType(HBEpochState state) {
		if(this.getType().isExtremeType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public boolean HandleSubAcquire(HBEpochState state) {
		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		H_t.updateWithMax(H_t, L_l);
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(HBEpochState state) {
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		state.incClockThread(getThread());
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRead(HBEpochState state) {
		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

		this.printRaceInfo(state);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
//			System.out.println("HB race detected on variable " + this.getVariable().getName());
		}
		else{
			int tIndex = state.getThreadIndex(this.getThread());
			int c = C_t.getClockIndex(tIndex);
			if(!R_v.isSameEpoch(c, tIndex)){
				R_v.updateWithMax(C_t, state.getThreadIndex(this.getThread()));
			}
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(HBEpochState state) {
		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		SemiAdaptiveVC R_v = state.getAdaptiveVC(state.readVariable, getVariable());
		SemiAdaptiveVC W_v = state.getAdaptiveVC(state.writeVariable, getVariable());

		this.printRaceInfo(state);

		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		if(raceDetected){
//			System.out.println("HB race detected on variable " + this.getVariable().getName());
		}
		else{
			int tIndex = state.getThreadIndex(this.getThread());
			int c = C_t.getClockIndex(tIndex);
			if(!W_v.isSameEpoch(c, tIndex)){
				W_v.setEpoch(c, tIndex);
				if(!R_v.isEpoch()){
					R_v.forceBottomEpoch();
				}
			}
		}


		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(HBEpochState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());			
			VectorClock H_tc = state.getVectorClock(state.HBPredecessorThread, this.getTarget());
			// System.out.println("Fork : Setting HB of target");
			H_tc.copyFrom(C_t);
			state.incClockThread(this.getThread());
			this.printRaceInfo(state);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(HBEpochState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
			VectorClock C_tc = state.generateVectorClockFromClockThread(this.getTarget());
			H_t.updateWithMax(H_t, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
