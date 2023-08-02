package engine.wellformedness.wellnesting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Stack;

import engine.wellformedness.State;
import event.Thread;

//Manages the clocks and other data structures used by the WCP algorithm
public class WellNestingState extends State{
	
	// Data used for algorithm
	public HashMap<Thread, Stack<Integer>> lock_stack; 
	public HashMap<Thread, Stack<HashSet<Integer>>> forked_threads_stack;
	
	//parameter flags
	public int verbosity;

	public WellNestingState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initData();
	}

	public void initData() {
		// initialize RCPThread
		this.lock_stack = new HashMap<Thread, Stack<Integer>>();
		this.forked_threads_stack = new HashMap<Thread, Stack<HashSet<Integer>>> ();
	}

	@Override
	public void printMemory() {
		// TODO Auto-generated method stub	
	}

}