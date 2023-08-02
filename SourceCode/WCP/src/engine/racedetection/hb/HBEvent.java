package engine.racedetection.hb;

import debug.EventStatistics;
import engine.racedetection.RaceDetectionEvent;
import util.vectorclock.VectorClock;

public class HBEvent extends RaceDetectionEvent<HBState> {

	@Override
	public boolean Handle(HBState state) {
		if(EventStatistics.isEnabled()) {
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
	public void printRaceInfoAccessType(HBState state) {
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
	public void printRaceInfoExtremeType(HBState state) {
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
	public boolean HandleSubAcquire(HBState state) {
		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		H_t.updateWithMax(H_t, L_l);
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRelease(HBState state) {
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		state.incClockThread(getThread());
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRead(HBState state) {
		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());

		this.printRaceInfo(state);
		
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		
		if(raceDetected){
//			System.out.println("HB race detected on variable " + this.getVariable().getName());
		}
		
		if( ! state.forceOrder){
			R_v.updateWithMax(R_v, C_t);
//			this.printRaceInfo(state);
			if(state.tickClockOnAccess){
				state.incClockThread(getThread());
			}
		}
		else{
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());

			H_t.updateWithMax(H_t, W_v);
			C_t = state.generateVectorClockFromClockThread(this.getThread());
			this.printRaceInfo(state);
			R_v.updateWithMax(R_v, C_t);
			state.incClockThread(getThread());
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(HBState state) {
		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());
		
		this.printRaceInfo(state);

		if (!(R_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}
		
		if(raceDetected){
//			System.out.println("HB race detected on variable " + this.getVariable().getName());
		}

		if( ! state.forceOrder){
			W_v.updateWithMax(W_v, C_t);
//			this.printRaceInfo(state);
			if(state.tickClockOnAccess){
				state.incClockThread(getThread());
			}
		}
		else{
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
			
			H_t.updateWithMax(H_t, W_v, R_v);
			C_t = state.generateVectorClockFromClockThread(this.getThread());
			this.printRaceInfo(state);
			W_v.updateWithMax(W_v, C_t);
			state.incClockThread(getThread());
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(HBState state) {
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
	public boolean HandleSubJoin(HBState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
			VectorClock C_tc = state.generateVectorClockFromClockThread(this.getTarget());
			H_t.updateWithMax(H_t, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
