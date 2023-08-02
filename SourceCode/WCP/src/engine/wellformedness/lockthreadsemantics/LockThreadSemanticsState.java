package engine.wellformedness.lockthreadsemantics;

import java.util.HashMap;
import java.util.HashSet;

import engine.wellformedness.State;
import event.Thread;

//Manages the clocks and other data structures used by the WCP algorithm
public class LockThreadSemanticsState extends State{
	
	// Data used for algorithm
	public HashMap<Thread, HashMap<Integer, Integer>> threadLockToDepth; 
	public HashMap<Integer, Thread> lockToThread;
	public HashMap<Thread, Thread> threadToParent;
	
	//parameter flags
	public int verbosity;

	public LockThreadSemanticsState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initData();
	}

	public void initData() {
		this.threadLockToDepth = new HashMap<Thread, HashMap<Integer, Integer>> ();
		this.lockToThread = new HashMap<Integer, Thread> ();
		this.threadToParent = new HashMap<Thread, Thread> ();
	}

	@Override
	public void printMemory() {
		// TODO Auto-generated method stub	
	}

}