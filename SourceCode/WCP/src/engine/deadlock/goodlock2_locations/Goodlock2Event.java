package engine.deadlock.goodlock2_locations;

import java.util.HashMap;
import java.util.HashSet;

import engine.deadlock.goodlock2_rw.Goodlock2State;
import event.Lock;
import event.Thread;
import util.Pair;
import util.vectorclock.VectorClock;

public class Goodlock2Event extends engine.deadlock.goodlock2.Goodlock2Event{
	@Override
	public boolean HandleSubAcquire(Goodlock2State state){
		Thread t = this.getThread();
		Lock l = this.getLock();
		
		String t_loc_this = t.getName() + "." + Integer.toString(this.getLocId());
		
		boolean deadlockPatternFound = false;
		VectorClock C_t = state.getVectorClock(state.clockThread, this.getThread());
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
				Pair<Long, Pair<VectorClock, HashSet<Lock>>> pair_ec_lset = new Pair<Long, Pair<VectorClock, HashSet<Lock>>>((long)this.getLocId(), pair_clock_lset);
				state.lockEdges.get(l1).get(l).get(t).add(pair_ec_lset);

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
										String t_loc_prime = t_prime.getName() + "." + ec_lset.first.toString();
										System.out.println(t_loc_prime + " " + t_loc_this);
									}
								}
							}
						}
					}
				}
			}
		}
		state.locksHeld.get(t).push(l);
		return deadlockPatternFound;
	}
}
