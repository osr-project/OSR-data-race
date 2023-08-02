package engine.atomicity.incorrect_conflictserializability.vc;

import engine.atomicity.AtomicityEvent;
import util.vectorclock.VectorClock;

public class VCConflictSerializabilityEvent extends AtomicityEvent<VCConflictSerializabilityState> {

	//	@Override
	//	public boolean Handle(VCVelodromeState state) {
	//		return this.HandleSub(state);
	//	}

	@Override
	public void printRaceInfoLockType(VCConflictSerializabilityState state) {
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
	public void printRaceInfoTransactionType(VCConflictSerializabilityState state) {
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
	public void printRaceInfoAccessType(VCConflictSerializabilityState state) {
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
	public void printRaceInfoExtremeType(VCConflictSerializabilityState state) {
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
	public boolean HandleSubAcquire(VCConflictSerializabilityState state) {
		VectorClock checkL_l = state.getVectorClock(state.checkLastReleaseLock, this.getLock());	
		int checkL_l_t = state.getIndex(checkL_l, this.getThread());
		boolean violationDetected = false;
		if(state.containsBlame(this.getThread(), checkL_l_t)){
			violationDetected = true;
		}

		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());	
		C_t.updateWithMax(C_t, L_l);

		this.printRaceInfo(state);

		return violationDetected;
	}

	@Override
	public boolean HandleSubRelease(VCConflictSerializabilityState state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());				
		VectorClock L_l = state.getVectorClock(state.lastReleaseLock, this.getLock());
		VectorClock checkL_l = state.getVectorClock(state.checkLastReleaseLock, this.getLock());
		L_l.copyFrom(C_t);
		state.updateCheckClock(checkL_l, C_t, this.getThread());
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubRead(VCConflictSerializabilityState state) {
		VectorClock checkW_v = state.getVectorClock(state.checkWriteVariable, this.getVariable());	
		int checkW_v_t = state.getIndex(checkW_v, this.getThread());
		boolean violationDetected = false;
		if(state.containsBlame(this.getThread(), checkW_v_t)){
			violationDetected = true;
		}

		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock W_v  = state.getVectorClock(state.writeVariable, getVariable());
		VectorClock R_v  = state.getVectorClock(state.readVariable, getVariable());
		VectorClock checkR_v  = state.getVectorClock(state.checkReadVariable, getVariable());
		
		C_t.updateWithMax(C_t, W_v);
		R_v.updateWithMax(R_v, C_t);
		state.updateCheckClock(checkR_v, C_t, this.getThread());
		
		this.printRaceInfo(state);

		return violationDetected;
	}

	@Override
	public boolean HandleSubWrite(VCConflictSerializabilityState state) {

		VectorClock checkW_v = state.getVectorClock(state.checkWriteVariable, this.getVariable());	
		int checkW_v_t = state.getIndex(checkW_v, this.getThread());
		VectorClock checkR_v = state.getVectorClock(state.checkReadVariable, this.getVariable());	
		int checkR_v_t = state.getIndex(checkR_v, this.getThread());

		boolean violationDetected = false;
		if(state.containsBlame(this.getThread(), checkW_v_t)){
			violationDetected = true;
		}
		else if(state.containsBlame(this.getThread(), checkR_v_t)){
			violationDetected = true;
		}

		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock W_v  = state.getVectorClock(state.writeVariable, getVariable());
		VectorClock R_v  = state.getVectorClock(state.readVariable, getVariable());
		
		C_t.updateWithMax(C_t, W_v, R_v);
		W_v.copyFrom(C_t);
		state.updateCheckClock(checkW_v, C_t, this.getThread());

		this.printRaceInfo(state);

		return violationDetected;

	}

	@Override
	public boolean HandleSubFork(VCConflictSerializabilityState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());			
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_tc.copyFrom(C_t);
			state.setIndex(C_tc, this.getTarget(), 1);
			this.printRaceInfo(state);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(VCConflictSerializabilityState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			int C_tc_t = state.getIndex(C_tc, this.getThread());
			boolean violationDetected = false;

			if(state.containsBlame(this.getThread(), C_tc_t)){
				violationDetected = true;
			}
			
			C_t.updateWithMax(C_t, C_tc);
			this.printRaceInfo(state);
			return violationDetected;
		}
		else return false;
	}

	@Override
	public boolean HandleSubBegin(VCConflictSerializabilityState state) {
		state.incClockThread(getThread());
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		int C_t_t = state.getIndex(C_t, this.getThread());
		state.pushBlame(this.getThread(), C_t_t);
		this.printRaceInfo(state);
		return false;
	}

	@Override
	public boolean HandleSubEnd(VCConflictSerializabilityState state) {
		state.popBlame(this.getThread());
		this.printRaceInfo(state);
		return false;
	}

}
