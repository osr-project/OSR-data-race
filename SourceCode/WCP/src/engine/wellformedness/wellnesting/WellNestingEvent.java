package engine.wellformedness.wellnesting;

import java.util.HashSet;
import java.util.Stack;

import engine.wellformedness.WellFormednessEvent;;

public class WellNestingEvent extends WellFormednessEvent<WellNestingState>{

	public WellNestingEvent() {
		super();
	}

	@Override
	public boolean Handle(WellNestingState state){		
		//Do some things first
		if(! (state.lock_stack.containsKey(this.thread)) ){
			state.lock_stack.put(this.getThread(), new Stack<Integer>());
			state.forked_threads_stack.put(this.getThread(), new Stack<HashSet<Integer>> ());
		}
		return this.HandleSub(state);
	}

	@Override
	public boolean HandleSubAcquire(WellNestingState state){
		state.lock_stack.get(this.getThread()).push(this.getLock().getId());
		state.forked_threads_stack.get(this.getThread()).push(new HashSet<Integer> ());
		return false;
	}

	@Override
	public boolean HandleSubRelease(WellNestingState state) {
		Stack<Integer> l_stack =  state.lock_stack.get(this.getThread());
		Stack<HashSet<Integer>> tset_stack = state.forked_threads_stack.get(this.getThread());

		if(l_stack.isEmpty() || tset_stack.isEmpty()){
			System.out.println("Violation - 1 ");
			return true;
		}
		int l = l_stack.pop();
		HashSet<Integer> tset = tset_stack.pop();
		if(l != this.getLock().getId()){
			System.out.println("Violation - 2");
			return true;
		}
		if(!tset.isEmpty()){
			System.out.println("Violation - 3");
			System.out.println(this.getAuxId());
			System.out.println(this.toCompactString());
			return true;
		}
		return false;
	}

	@Override
	public boolean HandleSubRead(WellNestingState state) {
		return false;
	}

	@Override
	public boolean HandleSubWrite(WellNestingState state) {
		return false;
	}

	@Override
	public boolean HandleSubFork(WellNestingState state) {
		Stack<HashSet<Integer>> tset_stack = state.forked_threads_stack.get(this.getThread());
		if(!tset_stack.isEmpty()){
			tset_stack.peek().add(this.getTarget().getId());
		}
		return false;
	}

	@Override
	public boolean HandleSubJoin(WellNestingState state) {
		Stack<HashSet<Integer>> tset_stack = state.forked_threads_stack.get(this.getThread());
		if(!tset_stack.isEmpty()){
			int target = this.getTarget().getId();
			if(!tset_stack.peek().contains(target)){
				System.out.println("Violation - 4. Set is " + tset_stack.peek().toString() + " target is " + target);
				return true;
			}
			else{
				tset_stack.peek().remove(target);
			}
		}
		return false;
	}

	@Override
	public boolean HandleSubBegin(WellNestingState state) {
		return false;
	}

	@Override
	public boolean HandleSubEnd(WellNestingState state) {
		return false;
	}

	@Override
	public boolean HandleSubBranch(WellNestingState state) {
		return false;
	}

}
