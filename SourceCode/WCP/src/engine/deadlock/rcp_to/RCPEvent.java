package engine.deadlock.rcp_to;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Stack;

import engine.deadlock.DeadlockEvent;
import event.Lock;
import util.Pair;
import util.vectorclock.Epoch;
import util.vectorclock.VectorClock;

public class RCPEvent extends DeadlockEvent<RCPState>{

	public RCPEvent() {
		super();
	}

	public boolean Handle(RCPState state){		
		//Do some things first
		if(! (state.mapThreadLockStack.containsKey(this.thread)) ){
			state.mapThreadLockStack.put(this.thread, new Stack<Lock>());
		}
		// TODO: This should not call sub directly
		return this.HandleSub(state);
	}

	@Override
	public void printDeadlockInfoLockType(RCPState state){
		if(this.getType().isLockType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getLock().toString();
				str += "|";
				VectorClock C_t = state.generateTimestamp(this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printDeadlockInfoAccessType(RCPState state){
		if(this.getType().isAccessType()){
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getVariable().getName();
			str += "|";
			VectorClock C_t = state.generateTimestamp(this.getThread());
			str += C_t.toString();
			str += "|";
			str += this.getThread().getName();
			str += "|" + Long.toString(this.getAuxId());
			if(state.verbosity == 1 || state.verbosity == 2){
				System.out.println(str);
			}
			if(state.verbosity == 3){
				System.out.print(str);
			}
		}
	}

	@Override
	public void printDeadlockInfoExtremeType(RCPState state){
		if(this.getType().isExtremeType()){
			if(state.verbosity == 2){
				String str = "#";
				str += Integer.toString(getLocId());
				str += "|";
				str += this.getType().toString();
				str += "|";
				str += this.getTarget().toString();
				str += "|";
				VectorClock C_t = state.generateTimestamp(this.getThread());
				str += C_t.toString();
				str += "|";
				str += this.getThread().getName();
				System.out.println(str);
			}
		}
	}

	@Override
	public void printDeadlockInfoTransactionType(RCPState state) {
	}

	@Override
	public boolean HandleSubAcquire(RCPState state){
		//System.out.println("Acquire Start: " + this.getThread().toString() + " || " + this.getLock().toString());

		/*** Extra Pre-processing for reEntrants *****/
		boolean reEntrant = state.isLockAcquired(this.getThread(), this.getLock());
		/*** Extra Pre-processing for reEntrants done *****/

		/****** Annotation phase starts **********/
		HashSet<Lock> locksHeld = state.getSetFromStack(this.getThread());
//		System.out.println("locksHeld = " + locksHeld);
//		System.out.println("lockAcquisitionHistory = " + state.lockAcquisitionHistory);
		state.mapThreadLockStack.get(getThread()).push(getLock());
		/****** Annotation phase ends **********/

		VectorClock H_t = state.getVectorClock(state.RHBThread, this.getThread());
		VectorClock H_l = state.getVectorClock(state.RHBLock, this.getLock());	

		H_t.updateWithMax(H_t, H_l);

		VectorClock P_t = state.getVectorClock(state.StrictRCPThread, getThread());
		VectorClock P_l = state.getVectorClock(state.StrictRCPLock, getLock());

		P_t.updateWithMax(P_t, P_l);

		if(!reEntrant){
			state.updateViewAsWriterAtAcquire(getLock(), getThread());
		}

		/*** Detect deadlock and add (locksHeld, timestamp) to acquisition history *****/
		VectorClock C_t = state.generateTimestamp(this.getThread());

		//TODO: Take care of re-entrants. The locksHeld may contain this.getLock() too and a deadlock can then be missed
		boolean deadlockDetected = false;
		for(Lock l : locksHeld){
			ArrayList<Pair<HashSet<Lock>, Epoch>> history_l = state.lockAcquisitionHistory.get(l);
			for(Pair<HashSet<Lock>, Epoch> pair : history_l){
				HashSet<Lock> locksHeld_l = pair.first;
				Epoch timestamp_l = pair.second;
				if(locksHeld_l.contains(this.getLock())){
					boolean check1 = Collections.disjoint(locksHeld, locksHeld_l) ;
					boolean check2 = !(timestamp_l.isLessThanOrEqual(C_t));
					if(check1 && check2){
						deadlockDetected = true;
						break;
					}
				}
			}
			if(deadlockDetected){
				break;
			}
		}

		ArrayList<Pair<HashSet<Lock>, Epoch>> history = state.lockAcquisitionHistory.get(this.getLock());
		boolean alreadyPresent = false;
		Epoch timestamp = state.getEpoch(C_t, this.getThread());
		for(Pair<HashSet<Lock>, Epoch> pair : history){
			HashSet<Lock> locksHeld_history = pair.first;
			if(locksHeld_history.containsAll(locksHeld) && locksHeld.containsAll(locksHeld_history)){
				alreadyPresent = true;
				pair.second = timestamp;
				break;
			}
		}
		if(!alreadyPresent){
			history.add(new Pair<HashSet<Lock>, Epoch> (locksHeld, timestamp));
		}
		/*** Done adding (locksHeld, timestamp) to acquisition history *****/

		this.printDeadlockInfo(state);
		return deadlockDetected;
	}

	@Override
	public boolean HandleSubRelease(RCPState state) {
		//System.out.println("Release Start: " + this.getThread().toString() + " || " + this.getLock().toString());

		/****** Annotation phase starts **********/
		state.mapThreadLockStack.get(getThread()).pop();
		/****** Annotation phase ends **********/

		state.readViewOfWriters(getLock(), getThread());

		VectorClock H_t = state.getVectorClock(state.RHBThread, getThread());
		VectorClock H_l = state.getVectorClock(state.RHBLock, this.getLock());
		H_l.copyFrom(H_t);

		VectorClock P_l = state.getVectorClock(state.StrictRCPLock, this.getLock());
		VectorClock P_t = state.getVectorClock(state.StrictRCPThread, getThread());
		P_l.copyFrom(P_t);

		state.updateViewAsWriterAtRelease(getLock(), getThread());

		this.printDeadlockInfo(state);

		state.incClockThread(getThread());
		state.incRHBThread(getThread());

		return false;
	}

	@Override
	public boolean HandleSubRead(RCPState state) {
		VectorClock P_t = state.getVectorClock(state.StrictRCPThread, this.getThread());
		VectorClock T_t = state.getVectorClock(state.TOThread, this.getThread());
		VectorClock W_v = state.getVectorClock(state.RHBWriteVariable, this.getVariable());
		VectorClock H_t = state.getVectorClock(state.RHBThread, getThread());

		int tIndex = state.threadToIndex.get(this.getThread());
		int lwt_x = state.lastWritingThread.get(this.getVariable());
		if(lwt_x >= 0){
			if(W_v.getClockIndex(lwt_x) > T_t.getClockIndex(lwt_x)){
				P_t.updateWithMax(P_t, W_v);
				H_t.updateWithMax(H_t, W_v);
			}
		}
		
		VectorClock R_v_t = state.getVectorClock(state.RHBReadVariableThread, this.getVariable(), this.getThread());
		R_v_t.copyFrom(H_t);

		state.threadsReadingSinceLastWrite.get(this.getVariable()).add(tIndex);

		this.printDeadlockInfo(state);

		state.incClockThread(this.getThread());
		state.incRHBThread(getThread());

		return false;
	}

	@Override
	public boolean HandleSubWrite(RCPState state) {
		VectorClock P_t = state.getVectorClock(state.StrictRCPThread, this.getThread());
		VectorClock T_t = state.getVectorClock(state.TOThread, this.getThread());
		VectorClock W_v = state.getVectorClock(state.RHBWriteVariable, this.getVariable());
		VectorClock H_t = state.getVectorClock(state.RHBThread, getThread());


		state.updateWithNonLocalAccessHistory(P_t, this.getThread(), this.getVariable(), state.RHBReadVariableThread);
		state.updateWithNonLocalAccessHistory(H_t, this.getThread(), this.getVariable(), state.RHBReadVariableThread);

		int lwt_x = state.lastWritingThread.get(this.getVariable());
		if(lwt_x >= 0){
			if(W_v.getClockIndex(lwt_x) > T_t.getClockIndex(lwt_x)){
				P_t.updateWithMax(P_t, W_v);
				H_t.updateWithMax(H_t, W_v);
			}
		}
		
		W_v.copyFrom(H_t);

		state.threadsReadingSinceLastWrite.get(this.getVariable()).clear();

		int tIndex = state.threadToIndex.get(this.getThread());
		state.lastWritingThread.put(this.getVariable(), tIndex);

		this.printDeadlockInfo(state);

		state.incClockThread(this.getThread());
		state.incRHBThread(getThread());

		return false;
	}

	@Override
	public boolean HandleSubFork(RCPState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			
			/****** Annotation phase starts **********/
			if(! (state.mapThreadLockStack.containsKey(this.getTarget())) ){
				state.mapThreadLockStack.put(this.getTarget(), new Stack<Lock>());
			}
			Stack<Lock> parent_stack = state.mapThreadLockStack.get(getThread());
			Stack<Lock> child_stack = state.mapThreadLockStack.get(getTarget());
			for(Lock l: parent_stack){
				child_stack.push(l);
			}
			/****** Annotation phase ends **********/
			
			int t_c_Index = state.threadToIndex.get(this.getTarget());

			VectorClock H_t = state.getVectorClock(state.RHBThread, this.getThread());
			VectorClock H_tc = state.getVectorClock(state.RHBThread, this.getTarget());
			H_tc.copyFrom(H_t);
			H_tc.setClockIndex(t_c_Index, 1);

			VectorClock T_t = state.getVectorClock(state.TOThread, this.getThread());
			VectorClock T_tc = state.getVectorClock(state.TOThread, this.getTarget());
			T_tc.copyFrom(T_t);
			T_tc.setClockIndex(t_c_Index, 1);

			VectorClock P_t = state.getVectorClock(state.StrictRCPThread, this.getThread());
			VectorClock P_tc = state.getVectorClock(state.StrictRCPThread, this.getTarget());
			P_tc.copyFrom(P_t);

			this.printDeadlockInfo(state);
			state.incClockThread(this.getThread());
			state.incRHBThread(getThread());
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(RCPState state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock H_t = state.getVectorClock(state.RHBThread, this.getThread());
			VectorClock H_tc = state.getVectorClock(state.RHBThread, this.getTarget());
			VectorClock T_t = state.getVectorClock(state.TOThread, this.getThread());
			VectorClock T_tc = state.getVectorClock(state.TOThread, this.getTarget());
			VectorClock P_t = state.getVectorClock(state.StrictRCPThread, this.getThread());
			VectorClock P_tc = state.getVectorClock(state.StrictRCPThread, this.getTarget());

			H_t.updateWithMax(H_t, H_tc);
			T_t.updateWithMax(T_t, T_tc);
			P_t.updateWithMax(P_t, P_tc);

			this.printDeadlockInfo(state);
		}
		return false;
	}

	@Override
	public boolean HandleSubBegin(RCPState state) {
		return false;
	}

	@Override
	public boolean HandleSubEnd(RCPState state) {
		return false;
	}

	@Override
	public boolean HandleSubBranch(RCPState state) {
		return false;
	}

}
