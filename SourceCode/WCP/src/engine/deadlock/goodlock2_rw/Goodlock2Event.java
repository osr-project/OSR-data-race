package engine.deadlock.goodlock2_rw;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import engine.deadlock.DeadlockEvent;
import event.Lock;
import event.Thread;
import util.Pair;
import util.vectorclock.VectorClock;

public class Goodlock2Event extends DeadlockEvent<Goodlock2State>{

	public Goodlock2Event() {
		super();
	}

	public boolean Handle(Goodlock2State state){
		return this.HandleSub(state);
	}

	@Override
	public void printDeadlockInfoLockType(Goodlock2State state){
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
	public void printDeadlockInfoAccessType(Goodlock2State state){
		if(this.getType().isAccessType()){
			String str = "#";
			str += Integer.toString(getLocId());
			str += "|";
			str += this.getType().toString();
			str += "|";
			str += this.getVariable().getName();
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
	public void printDeadlockInfoExtremeType(Goodlock2State state){
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
	public void printDeadlockInfoTransactionType(Goodlock2State state) {
	}

	@Override
	public boolean HandleSubAcquire(Goodlock2State state){
		Thread t = this.getThread();
		Lock l = this.getLock();
		boolean deadlockPatternFound = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
//		System.out.println(this.getAuxId() +  " => " + C_t);
		HashSet<Lock> locksHeld_set = Goodlock2State.stackToSet(state.locksHeld.get(t));
		for(Lock l1: locksHeld_set){
			if(l1.getId() != l.getId()){
				if(!state.lockEdges.containsKey(l1)){
					state.lockEdges.put(l1, new HashMap<>());
				}
				if(!state.lockEdges.get(l1).containsKey(l)){
					state.lockEdges.get(l1).put(l, new HashMap<>());
				}
				if(!state.lockEdges.get(l1).get(l).containsKey(t)){
					state.lockEdges.get(l1).get(l).put(t, new HashSet<>());
				}
				Pair<VectorClock, HashSet<Lock>> pair_clock_lset = new Pair<VectorClock, HashSet<Lock>>(new VectorClock(C_t), locksHeld_set);
				Pair<Long, Pair<VectorClock, HashSet<Lock>>> pair_ec_lset = new Pair<Long, Pair<VectorClock, HashSet<Lock>>>(this.getId(), pair_clock_lset);
				state.lockEdges.get(l1).get(l).get(t).add(pair_ec_lset);
//				System.out.println(pair_ec_lset);

				if(state.lockEdges.containsKey(l)){
					if(state.lockEdges.get(l).containsKey(l1)){
						HashSet<Thread> l_l1_set = new HashSet<Thread>(state.lockEdges.get(l).get(l1).keySet());
						l_l1_set.remove(t);
						for(Thread t_prime: l_l1_set){
							for(Pair<Long, Pair<VectorClock, HashSet<Lock>>> ec_lset: state.lockEdges.get(l).get(l1).get(t_prime)){
								VectorClock C_tprime = ec_lset.second.first;
								boolean unordered_clocks = !C_tprime.isLessThanOrEqual(C_t);
								if(unordered_clocks){
									HashSet<Lock> lset_copy = new HashSet<Lock>(ec_lset.second.second);
									lset_copy.retainAll(locksHeld_set);
									if(lset_copy.size() == 0){
										deadlockPatternFound = true;
										System.out.println("==================");
										System.out.println("Thread " + t_prime.getName());
										System.out.println(ec_lset.first.toString() + " : " + l.getName() + " => " + l1.getName() + " : " + ec_lset.second.first.toString() + " : " + ec_lset.second.second.toString());
										System.out.println("-------" + (this.getId() - ec_lset.first) + "-----------");
										System.out.println("Thread " + t.getName());
										System.out.println(this.getId().toString() + " : " + l1.getName() + " => " + l.getName() + " : " + C_t.toString() + " : " + locksHeld_set.toString());
										System.out.println("==================");
										//break;
									}
								}
							}
							if(deadlockPatternFound){
								//break;
							}
						}
					}
				}
			}
			if(deadlockPatternFound){
				//break;
			}
		}
		state.locksHeld.get(t).push(l);
		return deadlockPatternFound;
	}

	@Override
	public boolean HandleSubRelease(Goodlock2State state) {
		//System.out.println("Release Start: " + this.getThread().toString() + " || " + this.getLock().toString());

		Lock l = state.locksHeld.get(this.getThread()).pop();
		if(l.getId() != this.getLock().getId()){
			throw new IllegalArgumentException("Releasing a lock that is not at the top of the stack!");
		}

		return false;
	}

/*
 * Block partial order
 * 
	@Override
	public boolean HandleSubRead(Goodlock2State state) {
		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock W_v = state.getVectorClock(state.writeBlock, getVariable());		
		C_t.updateWithMax(C_t, W_v);
		VectorClock R_v = state.getVectorClock(state.readBlock, getVariable());		
		R_v.updateWithMax(R_v, C_t);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubWrite(Goodlock2State state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock R_v = state.getVectorClock(state.readBlock, getVariable());
		VectorClock W_v = state.getVectorClock(state.writeBlock, getVariable());
		C_t.updateWithMax(C_t, R_v);
		String varName = this.getVariable().getName();
		if(state.specialWrites.containsKey(varName)){
			if(state.specialWrites.get(varName).contains(this.getAuxId())){
				C_t.updateWithMax(C_t, W_v);
			}
		}
//		System.out.println(this.getAuxId() +  " => " + C_t);
		W_v.updateWithMax(W_v, C_t);
		state.incClockThread(getThread());
		return false;
	}
*/
	
	@Override
	public boolean HandleSubRead(Goodlock2State state) {
		VectorClock C_t  = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock W_v = state.getVectorClock(state.writeBlock, getVariable());		
		C_t.updateWithMax(C_t, W_v);
		return false;
	}

	@Override
	public boolean HandleSubWrite(Goodlock2State state) {
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
		VectorClock W_v = state.getVectorClock(state.writeBlock, getVariable());
		W_v.copyFrom(C_t);
		state.incClockThread(getThread());
		return false;
	}

	@Override
	public boolean HandleSubFork(Goodlock2State state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());			
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_tc.copyFrom(C_t);
			state.setIndex(C_tc, this.getTarget(), 1);
			state.incClockThread(getThread());
			
			Stack<Lock> parent_stack = state.locksHeld.get(getThread());
			Stack<Lock> child_stack = state.locksHeld.get(getTarget());
			for(Lock l: parent_stack){
				child_stack.push(l);
			}
		}
		return false;
	}


	@Override
	public boolean HandleSubJoin(Goodlock2State state) {
		if (state.isThreadRelevant(this.getTarget())) {
			VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
			VectorClock C_tc = state.getVectorClock(state.clockThread, this.getTarget());
			C_t.updateWithMax(C_t, C_tc);
		}
		return false;
	}


	@Override
	public boolean HandleSubBegin(Goodlock2State state) {
		return false;
	}


	@Override
	public boolean HandleSubEnd(Goodlock2State state) {
		return false;
	}

	
	@Override
	public boolean HandleSubBranch(Goodlock2State state) {
		return false;
	}

}
