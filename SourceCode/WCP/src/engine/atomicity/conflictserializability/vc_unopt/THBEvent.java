package engine.atomicity.conflictserializability.vc_unopt;


import engine.atomicity.AtomicityEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

public class THBEvent extends AtomicityEvent<THBState> {

	//	@Override
	//	public boolean Handle(VCVelodromeState state) {
	//		return this.HandleSub(state);
	//	}

	@Override
	public void printRaceInfoLockType(THBState state) {
		if(this.getType().isLockType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public void printRaceInfoTransactionType(THBState state) {
		if(this.getType().isLockType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printRaceInfoAccessType(THBState state) {
		if(this.getType().isAccessType()){
			if(state.verbosity == 1 || state.verbosity == 2){
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
				System.out.println(str);
			}	
		}		
	}

	@Override
	public void printRaceInfoExtremeType(THBState state) {
		if(this.getType().isExtremeType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}		
	}

	@Override
	public boolean HandleSubAcquire(THBState state) {
		boolean violationDetected = false;
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.checkAndAddLock(l);
		VectorClock L_l = state.getVectorClock(state.clockLock, l);

		if(state.lastThreadToRelease.containsKey(l)) {
			if(!state.lastThreadToRelease.get(l).equals(t)) {
				violationDetected = state.checkAndGetClock(L_l, t);
			}
		}

		return violationDetected;
	}

	@Override
	public boolean HandleSubRelease(THBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.checkAndAddLock(l);
		VectorClock C_t = state.getVectorClock(state.clockThread, t);
		VectorClock L_l = state.getVectorClock(state.clockLock, l);
		
		L_l.updateWithMax(L_l, C_t);
		state.lastThreadToRelease.put(l, t);
		state.incClockThread(t);
		
		return false;
	}

	@Override
	public boolean HandleSubRead(THBState state) {
		boolean violationDetected = false;	
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.checkAndAddVariable(v);
		VectorClock C_t = state.getVectorClock(state.clockThread, t);
		VectorClock W_v = state.getVectorClock(state.clockWriteVariable, v);
		
		if(state.lastThreadToWrite.containsKey(v)) {
			if(!state.lastThreadToWrite.get(v).equals(t)) {
				violationDetected |= state.checkAndGetClock(W_v, t);
			}
		}
		VectorClock R_v_t = state.getVectorClock(state.clockReadVariableThread, v, t);
		R_v_t.copyFrom(C_t);
		state.incClockThread(t);
		
		return violationDetected;
	}

	@Override
	public boolean HandleSubWrite(THBState state) {
		boolean violationDetected = false;
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.checkAndAddVariable(v);
		VectorClock W_v = state.getVectorClock(state.clockWriteVariable, v);
		VectorClock C_t = state.getVectorClock(state.clockThread, t);
		
		if(state.lastThreadToWrite.containsKey(v)) {
			if(!state.lastThreadToWrite.get(v).equals(t)) {
				violationDetected |= state.checkAndGetClock(W_v, t);
			}
		}
		for(Thread u: state.threadToIndex.keySet()) {
			if(!u.equals(t)) {
				VectorClock R_v_u = state.getVectorClock(state.clockReadVariableThread, v, u);
				violationDetected |= state.checkAndGetClock(R_v_u, t);
			}
		}
		W_v.updateWithMax(W_v, C_t);
		state.lastThreadToWrite.put(v, t);
		state.incClockThread(t);

		return violationDetected;
	}

	@Override
	public boolean HandleSubFork(THBState state) {
		Thread u = this.getTarget();
		if(state.isThreadRelevant(u)) {
			Thread t = this.getThread();
			VectorClock C_u = state.getVectorClock(state.clockThread, u);
			VectorClock C_t = state.getVectorClock(state.clockThread, t);
			C_u.updateWithMax(C_u, C_t);
			state.incClockThread(t);
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(THBState state) {
		Thread u = this.getTarget();
		if(state.isThreadRelevant(u)) {
			Thread t = this.getThread();
			VectorClock C_u = state.getVectorClock(state.clockThread, u);
			return state.checkAndGetClock(C_u, t);
		}
		else return false;
	}

	@Override
	public boolean HandleSubBegin(THBState state) {
		Thread t = this.getThread();
		int cur_depth = state.threadToNestingDepth.get(t);
		state.threadToNestingDepth.put(t,  cur_depth + 1);
		boolean violationDetected = false;
		
		if(cur_depth == 0){
			VectorClock C_t = state.getVectorClock(state.clockThread, t);				
			VectorClock C_t_begin = state.getVectorClock(state.clockThreadBegin, t);
			C_t_begin.copyFrom(C_t);
		}
		else{
			// Treat this as a no-op
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubEnd(THBState state) {
		Thread t = this.getThread();
		int cur_depth = state.threadToNestingDepth.get(t);
		state.threadToNestingDepth.put(t,  cur_depth - 1);
		boolean violationDetected = false;

		if(cur_depth == 1) {
			violationDetected = state.handshakeAtEndEvent(t);
			state.incClockThread(t);
		}
		else {
			// Treat this as no-op
		}
		return violationDetected;
	}

}
