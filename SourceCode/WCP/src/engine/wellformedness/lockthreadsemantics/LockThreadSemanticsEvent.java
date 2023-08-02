package engine.wellformedness.lockthreadsemantics;

import java.util.HashMap;
import event.Thread;

import engine.wellformedness.WellFormednessEvent;;

public class LockThreadSemanticsEvent extends WellFormednessEvent<LockThreadSemanticsState>{

	public LockThreadSemanticsEvent() {
		super();
	}

	@Override
	public boolean Handle(LockThreadSemanticsState state){		
		//Do some things first
		Thread t = this.getThread();
		if(! (state.threadLockToDepth.containsKey(t)) ){
			state.threadLockToDepth.put(t, new HashMap<Integer, Integer> ());
		}
		return this.HandleSub(state);
	}

	@Override
	public boolean HandleSubAcquire(LockThreadSemanticsState state){
		Thread t = this.getThread();
		int lId = this.getLock().getId();
		HashMap<Integer, Integer> tldMap = state.threadLockToDepth.get(t);
		if(!tldMap.containsKey(lId)) {
			tldMap.put(lId, 0);
		}
		int depth = tldMap.get(lId);
		tldMap.put(lId, depth + 1);
		
		// Check if for all other threads, lock l is not held
		for(Thread u: state.threadLockToDepth.keySet()) {
			if(u.equals(t)) continue;
			if(state.threadLockToDepth.get(u).containsKey(lId)) {
				System.out.println("Lock " + this.getLock().getName() + " acquired by thread " + t.getName() + " while being held by thread " + u.getName());
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubRelease(LockThreadSemanticsState state) {
		Thread t = this.getThread();
		int lId = this.getLock().getId();
		HashMap<Integer, Integer> tldMap = state.threadLockToDepth.get(t);
		
		boolean violation = false;
		int depth = 0;
		if(!tldMap.containsKey(lId)) {
			violation = true;
		}
		else {
			depth = tldMap.get(lId);
			if(depth <= 0) {
				violation = false;
			}
		}
		
		if(violation) {
			System.out.println("Lock " + this.getLock().getName() + " released by thread " + t.getName() + " without a matching acquire!" );
		}
		else {
			depth = depth - 1;
			if(depth == 0) {
				tldMap.remove(lId);
			}
			else {
				tldMap.put(lId, depth - 1);
			}
		}
		
		return violation;
	}

	@Override
	public boolean HandleSubRead(LockThreadSemanticsState state) {
		return false;
	}

	@Override
	public boolean HandleSubWrite(LockThreadSemanticsState state) {
		return false;
	}

	@Override
	public boolean HandleSubFork(LockThreadSemanticsState state) {
		Thread t = this.getThread();
		Thread u = this.getTarget();
		if(state.threadToParent.containsKey(u)) {
			System.out.println("Thread u is being forked by thread " + t.getName() + " despite being previously forked by thread " + state.threadToParent.get(u).getName());
			return true;
		}
		state.threadToParent.put(u, t);
		return false;
	}

	@Override
	public boolean HandleSubJoin(LockThreadSemanticsState state) {
		Thread t = this.getThread();
		Thread u = this.getTarget();
		if(!state.threadToParent.containsKey(u)) {
			System.out.println("Thread " + u.getName() +" is being joined by thread " + t.getName() + " without being forked earlier at all!");
			return true;
		}
		Thread p = state.threadToParent.get(u);
		if(!p.equals(t)) {
			System.out.println("Thread " + u.getName() +" is being joined by thread " + t.getName() + " without being forked earlier by a different thread " + p.getName());
			return true;
		}
		return false;
	}

	@Override
	public boolean HandleSubBegin(LockThreadSemanticsState state) {
		return false;
	}

	@Override
	public boolean HandleSubEnd(LockThreadSemanticsState state) {
		return false;
	}

	@Override
	public boolean HandleSubBranch(LockThreadSemanticsState state) {
		return false;
	}

}
