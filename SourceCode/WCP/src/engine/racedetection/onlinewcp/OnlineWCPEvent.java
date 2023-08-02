package engine.racedetection.onlinewcp;

import java.util.HashSet;
import java.util.Stack;

import engine.racedetection.RaceDetectionEvent;
import event.Lock;
import event.Variable;
import util.vectorclock.VectorClock;

public class OnlineWCPEvent extends RaceDetectionEvent<OnlineWCPState>{

	public OnlineWCPEvent() {
		super();
	}

	@Override
	public boolean Handle(OnlineWCPState state) {
		//Do some things first
		if(! (state.mapThreadLockStack.containsKey(thread)) ){
			state.mapThreadLockStack.put(thread, new Stack<Lock>());
			state.mapThreadReadVarSetStack.put(thread, new Stack<HashSet<Variable>>());
			state.mapThreadWriteVarSetStack.put(thread, new Stack<HashSet<Variable>>());	
		}

		//Now call child's function
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(OnlineWCPState state){
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
	public void printRaceInfoAccessType(OnlineWCPState state){
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
				System.out.println(str);
			}	
		}
	}

	@Override
	public void printRaceInfoExtremeType(OnlineWCPState state){
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
	public boolean HandleSubAcquire(OnlineWCPState state) {
	//System.out.println("Acquire Start: " + this.getThread().toString() + " || " + this.getLock().toString());
		
		/*** Extra Pre-processing for reEntrants *****/
		boolean reEntrant = state.isLockAcquired(this.getThread(), this.getLock());
		/*** Extra Pre-processing for reEntrants done *****/
		
		/****** Annotation phase starts **********/
		state.mapThreadLockStack.get(getThread()).push(getLock());
		state.mapThreadReadVarSetStack.get(getThread()).push(new HashSet<Variable>());
		state.mapThreadWriteVarSetStack.get(getThread()).push(new HashSet<Variable>());
		/****** Annotation phase ends **********/
		
		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
		VectorClock H_l = state.getVectorClock(state.HBPredecessorLock, this.getLock());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());				
		H_t.updateWithMax(H_t, H_l, L_l);
		
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, getLock());
		P_t.updateWithMax(P_t, P_l);
		
		if(!reEntrant){
			state.updateViewAsWriterAtAcquire(getLock(), getThread());
		}
		//System.out.println("Acquire :");
		//System.out.println(state.view.toString());
		
		this.printRaceInfo(state);

		return false;
	}

	@Override
	public boolean HandleSubRelease(OnlineWCPState state) {
		//System.out.println("Release Start: " + this.getThread().toString() + " || " + this.getLock().toString());
		
		/****** Annotation phase starts **********/
		HashSet<Variable> readVarSet = state.mapThreadReadVarSetStack.get(getThread()).pop();
		HashSet<Variable> writeVarSet = state.mapThreadWriteVarSetStack.get(getThread()).pop();

		this.setReadVarSet(readVarSet);
		this.setWriteVarSet(writeVarSet);
		
		state.mapThreadLockStack.get(getThread()).pop();
		
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadReadVarSetStack.get(getThread()).peek().addAll(readVarSet);
			state.mapThreadWriteVarSetStack.get(getThread()).peek().addAll(writeVarSet);
		}
		/****** Annotation phase ends **********/
		
		state.readViewOfWriters(getLock(), getThread());

		VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, getThread());
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());		
		for (Variable r : this.readVarSet) {
			VectorClock L_r_l_x = state.getVectorClock(state.lastReleaseLockReadVariable, this.getLock(), r);
			L_r_l_x.updateWithMax(L_r_l_x,H_t, C_t);
		}
		
		for (Variable w : this.writeVarSet) {
			VectorClock L_w_l_x = state.getVectorClock(state.lastReleaseLockWriteVariable, this.getLock(), w);
			L_w_l_x.updateWithMax(L_w_l_x, H_t, C_t);
		}

		VectorClock H_l = state.getVectorClock(state.HBPredecessorLock, this.getLock());
		H_l.copyFrom(H_t);
		
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		
		VectorClock P_l = state.getVectorClock(state.WCPPredecessorLock, this.getLock());
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, getThread());
		P_l.copyFrom(P_t);

		state.updateViewAsWriterAtRelease(getLock(), getThread());
		state.incClockThread(getThread());
		
		//System.out.println("Release :");
		//System.out.println(state.view.toString());
		
		this.printRaceInfo(state);

		
		return false;
	}

	@Override
	public boolean HandleSubRead(OnlineWCPState state) {
		/****** Annotation phase starts **********/
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadReadVarSetStack.get(getThread()).peek().add(getVariable());
			this.setLockSet(state.getSetFromStack(getThread()));
		}
		/****** Annotation phase ends **********/
		
		/***** Update P_t **************/
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, this.getThread());
		for (Lock l : this.getLockSet()) {
			VectorClock writeClock = state.getVectorClock(state.lastReleaseLockWriteVariable, l, this.getVariable());
			P_t.updateWithMax(P_t, writeClock);
		}
		/***** P_t updated **************/
		
		boolean raceDetected = false;
		VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
		VectorClock R_v = state.getVectorClock(state.readVariable, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeVariable, getVariable());
		
		this.printRaceInfo(state);
		
		if (!(W_v.isLessThanOrEqual(C_t))) {
			raceDetected = true;
		}

		if( ! state.forceOrder){
			R_v.updateWithMax(R_v, C_t);	
			if(state.tickClockOnAccess){
				state.incClockThread(getThread());
			}
		}
		else{
			VectorClock RH_v = state.getVectorClock(state.HBPredecessorReadVariable, getVariable());
			VectorClock WH_v = state.getVectorClock(state.HBPredecessorWriteVariable, getVariable());
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
			
			P_t.updateWithMax(P_t, WH_v);
			H_t.updateWithMax(H_t, WH_v);
			C_t = state.generateVectorClockFromClockThread(this.getThread());
			this.printRaceInfo(state);
			R_v.updateWithMax(R_v, C_t);
			RH_v.updateWithMax(RH_v, H_t, C_t);
			state.incClockThread(getThread());
		}
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(OnlineWCPState state) {
		/****** Annotation phase starts **********/
		if (!(state.mapThreadLockStack.get(getThread()).isEmpty())) {
			state.mapThreadWriteVarSetStack.get(getThread()).peek().add(getVariable());
			this.setLockSet(state.getSetFromStack(getThread()));
		}
		/****** Annotation phase ends **********/
		
		/***** Update P_t **************/
		VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, this.getThread());
		for (Lock l : this.getLockSet()) {
			VectorClock writeClock = state.getVectorClock(state.lastReleaseLockWriteVariable, l, this.getVariable());
			P_t.updateWithMax(P_t, writeClock);
			VectorClock readClock = state.getVectorClock(state.lastReleaseLockReadVariable, l, this.getVariable());
			P_t.updateWithMax(P_t, readClock);
		}
		/***** P_t updated **************/
		
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

		if( ! state.forceOrder){
			W_v.updateWithMax(W_v, C_t);			
			if(state.tickClockOnAccess){
				state.incClockThread(getThread());
			}
		}
		else{
			VectorClock RH_v = state.getVectorClock(state.HBPredecessorReadVariable, getVariable());
			VectorClock WH_v = state.getVectorClock(state.HBPredecessorWriteVariable, getVariable());
			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
			
			P_t.updateWithMax(P_t, WH_v, RH_v);
			H_t.updateWithMax(H_t, WH_v, RH_v);
			C_t = state.generateVectorClockFromClockThread(this.getThread());
			this.printRaceInfo(state);
			W_v.updateWithMax(W_v, C_t);
			WH_v.updateWithMax(WH_v, H_t, C_t);
			state.incClockThread(getThread());
		}
		
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(OnlineWCPState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			/****** Annotation phase starts **********/
			// No annotation required
			/****** Annotation phase ends **********/

			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
			VectorClock C_t = state.generateVectorClockFromClockThread(this.getThread());
			
			VectorClock H_tc = state.getVectorClock(state.HBPredecessorThread, this.getTarget());
			// System.out.println("Fork : Setting HB of target");
			H_tc.updateWithMax(C_t, H_t);
			
			VectorClock P_tc = state.getVectorClock(state.WCPPredecessorThread, this.getTarget());
			// System.out.println("Fork : Setting WCP of target");
			P_tc.updateWithMax(C_t, H_t);

			state.incClockThread(this.getThread());
			this.printRaceInfo(state);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(OnlineWCPState state) {
		if (state.isThreadRelevant(this.getTarget())) {

			/****** Annotation phase starts **********/
			// No annotation required
			/****** Annotation phase ends **********/

			VectorClock H_t = state.getVectorClock(state.HBPredecessorThread, this.getThread());
			VectorClock H_tc = state.getVectorClock(state.HBPredecessorThread, this.getTarget());
			VectorClock C_tc = state.generateVectorClockFromClockThread(this.getTarget());
			VectorClock P_t = state.getVectorClock(state.WCPPredecessorThread, this.getThread());

			H_t.updateWithMax(H_t, H_tc, C_tc);
			P_t.updateWithMax(P_t, H_tc, C_tc);
			this.printRaceInfo(state);
		}
		return false;
	}

}
