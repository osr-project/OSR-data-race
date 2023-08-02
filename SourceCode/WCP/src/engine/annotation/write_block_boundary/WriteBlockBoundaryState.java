package engine.annotation.write_block_boundary;

import java.util.HashMap;
import java.util.LinkedList;

import engine.racedetection.State;
import event.Thread;

public class WriteBlockBoundaryState extends State {

	// For a write event (at index i), boundary(i) = j such that
	// j is the index of the last reader of i (if any).
	// if no such reader exists, then j = i.
	// threadlocality(i) = true iff all readers of i belong to thread of i 

	// ==== Data used for algorithm ====
	public HashMap<String, LinkedList<Long>> variableToListOfBoundaryIndices;
	public HashMap<String, LinkedList<Boolean>> variableToListOfThreadLocality;
	public HashMap<String, Thread> lastWritingThread;
	
	public WriteBlockBoundaryState() {
		this.variableToListOfBoundaryIndices = new HashMap<String, LinkedList<Long>> ();
		this.variableToListOfThreadLocality  = new HashMap<String, LinkedList<Boolean>> ();
		this.lastWritingThread = new HashMap<String, Thread> ();
	}

	public void checkAndAddVariable(String vname) {
		if (!variableToListOfBoundaryIndices.containsKey(vname)) {
			this.variableToListOfBoundaryIndices.put(vname, new LinkedList<Long> ());
			this.variableToListOfThreadLocality.put(vname, new LinkedList<Boolean> ());
			this.lastWritingThread.put(vname, null);
		}
	}

	@Override
	public void printMemory() {

	}

}
