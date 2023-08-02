package engine.atomicity.conflictserializability.tc;


import engine.atomicity.AtomicityEvent;
import event.Lock;
import event.Thread;
import event.Variable;
import util.treeclock.iterative.TreeClock;
import util.vectorclock.VectorClockOpt;

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
		TreeClock L_l = state.getClock(state.clockLock, l);

		if(state.lastThreadToRelease.containsKey(l)) {
			if(!state.lastThreadToRelease.get(l).equals(t)) {
				violationDetected = state.checkAndGetClock(L_l, L_l, t);
			}
		}

		return violationDetected;
	}

	@Override
	public boolean HandleSubRelease(THBState state) {
		Thread t = this.getThread();
		Lock l = this.getLock();
		state.checkAndAddLock(l);
		TreeClock C_t = state.getClock(state.clockThread, t);
		TreeClock L_l = state.getClock(state.clockLock, l);

		L_l.monotoneCopy(C_t);
		state.lastThreadToRelease.put(l, t);
		if(state.threadToNestingDepth.get(t) == 0) {
			state.incClockThread(t);
		}
		return false;
	}

	@Override
	public boolean HandleSubRead(THBState state) {
		boolean violationDetected = false;	
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.checkAndAddVariable(v);
		TreeClock C_t = state.getClock(state.clockThread, t);
		TreeClock W_v = state.getClock(state.clockWriteVariable, v);

		if(state.lastThreadToWrite.containsKey(v)) {
			if(!state.lastThreadToWrite.get(v).equals(t)) {
				violationDetected |= state.checkAndGetClock(W_v, W_v, t);
			}
		}
		TreeClock R_v = state.getClock(state.clockReadVariable, v);
		R_v.join(C_t);
		VectorClockOpt chR_v = state.getClock(state.clockReadVariableCheck, v);
		state.updateCheckClock(chR_v, C_t);
		if(state.threadToNestingDepth.get(t) == 0) {
			state.incClockThread(t);
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubWrite(THBState state) {
		boolean violationDetected = false;
		Thread t = this.getThread();
		Variable v = this.getVariable();
		state.checkAndAddVariable(v);
		TreeClock W_v = state.getClock(state.clockWriteVariable, v);
		TreeClock R_v = state.getClock(state.clockReadVariable, v);
		VectorClockOpt chR_v = state.getClock(state.clockReadVariableCheck, v);
		TreeClock C_t = state.getClock(state.clockThread, t);

		if(state.lastThreadToWrite.containsKey(v)) {
			if(!state.lastThreadToWrite.get(v).equals(t)) {
				violationDetected |= state.checkAndGetClock(W_v, W_v, t);
			}
		}
		violationDetected |= state.checkAndGetClock(chR_v, R_v, t);
		W_v.monotoneCopy(C_t);
		state.lastThreadToWrite.put(v, t);
		if(state.threadToNestingDepth.get(t) == 0) {
			state.incClockThread(t);
		}
		return violationDetected;
	}

	@Override
	public boolean HandleSubFork(THBState state) {
		Thread u = this.getTarget();
		if(state.isThreadRelevant(u)) {
			Thread t = this.getThread();
			TreeClock C_u = state.getClock(state.clockThread, u);
			TreeClock C_t = state.getClock(state.clockThread, t);
			C_u.join(C_t);			
			if(state.threadToNestingDepth.get(t) == 0) {
				state.incClockThread(t);
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(THBState state) {
		Thread u = this.getTarget();
		if(state.isThreadRelevant(u)) {
			Thread t = this.getThread();
			TreeClock C_u = state.getClock(state.clockThread, u);
			return state.checkAndGetClock(C_u, C_u, t);
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
			TreeClock C_t = state.getClock(state.clockThread, t);				
			TreeClock C_t_begin = state.getClock(state.clockThreadBegin, t);
			C_t_begin.monotoneCopy(C_t);
		}
		// else Treat this as a no-op
		
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
		// else Treat this as no-op
		
		return violationDetected;
	}

}
