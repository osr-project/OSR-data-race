package engine.annotation.written_dangling_vars;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import engine.racedetection.State;
import event.Thread;

public class WrittenDanglingVarsState extends State {

	// For an acquire event (at index i):
	// writtenVars(i) = {x | \exists w = w(x) \in CS(i)}
	// danglingVars(i) = {x | \exists r = r(x) \in CS(i), rf(r) \not\in CS(i)} \cup
	// {x | \exists r = r(x) \not\in CS(i), rf(r) \in CS(i)}

	// === Auxiliary data ===
	public HashMap<String, LinkedList<Long>> variableToListOfBoundaryIndices;
	public HashMap<String, LinkedList<Boolean>> variableToListOfThreadLocality;

	// ==== Data computed by the algorithm ====
	public HashMap<String, LinkedList<HashSet<String>>> lockToListOfWrittenVars;
	public HashMap<String, LinkedList<HashSet<String>>> lockToListOfDanglingVars;

	// === Data used by the algorithm ===
	public HashMap<Thread, HashMap<String, Integer>> locksHeld;
	private HashMap<String, Iterator<Long>> var_to_boundary_itr;
	private HashMap<String, Iterator<Boolean>> var_to_locality_itr;
	public HashMap<String, Long> var_to_boundary;
	public HashMap<String, Boolean> var_to_locality;

	public WrittenDanglingVarsState(HashMap<String, LinkedList<Long>> variableToListOfBoundaryIndices,
			HashMap<String, LinkedList<Boolean>> variableToListOfThreadLocality) {
		this.variableToListOfBoundaryIndices = variableToListOfBoundaryIndices;
		this.variableToListOfThreadLocality = variableToListOfThreadLocality;

		this.lockToListOfWrittenVars = new HashMap<String, LinkedList<HashSet<String>>>();
		this.lockToListOfDanglingVars = new HashMap<String, LinkedList<HashSet<String>>>();

		this.locksHeld = new HashMap<Thread, HashMap<String, Integer>>();
		this.var_to_boundary_itr = new HashMap<String, Iterator<Long>>();
		this.var_to_locality_itr = new HashMap<String, Iterator<Boolean>>();
		this.var_to_boundary = new HashMap<String, Long>();
		this.var_to_locality = new HashMap<String, Boolean>();
	}

	public void checkAndAddVariable(String vname) {
		if (!var_to_boundary_itr.containsKey(vname)) {
			Iterator<Long> boundary_itr = this.variableToListOfBoundaryIndices.get(vname).iterator();
			this.var_to_boundary_itr.put(vname, boundary_itr);

			Iterator<Boolean> locality_itr = this.variableToListOfThreadLocality.get(vname).iterator();
			this.var_to_locality_itr.put(vname, locality_itr);

			this.var_to_boundary.put(vname, -1L);
			this.var_to_locality.put(vname, false);
		}
	}

	public void checkAndAddLock(String lname) {
		if (!lockToListOfWrittenVars.containsKey(lname)) {
			this.lockToListOfWrittenVars.put(lname, new LinkedList<HashSet<String>>());
			this.lockToListOfDanglingVars.put(lname, new LinkedList<HashSet<String>>());
		}
	}

	public void updateLocksHeldAcquire(Thread t, String lname) {
		if (!this.locksHeld.containsKey(t)) {
			this.locksHeld.put(t, new HashMap<String, Integer>());
		}
		if (!this.locksHeld.get(t).containsKey(lname)) {
			this.locksHeld.get(t).put(lname, 0);
		}
		int origVal = this.locksHeld.get(t).get(lname);
		this.locksHeld.get(t).put(lname, origVal + 1);
	}

	// Returns true iff this is the outermost release on the lock lname
	public boolean updateLocksHeldRelease(Thread t, String lname) {
		int origVal = this.locksHeld.get(t).get(lname);
		if (origVal > 1) {
			this.locksHeld.get(t).put(lname, origVal - 1);
			return false;
		} else {
			this.locksHeld.get(t).remove(lname);
			return true;
		}
	}

	public void newWrite(String vname) {
		long curr_boundary = this.var_to_boundary_itr.get(vname).next();
		this.var_to_boundary.put(vname, curr_boundary);
		boolean curr_locality = this.var_to_locality_itr.get(vname).next();
		this.var_to_locality.put(vname, curr_locality);
	}

	@Override
	public void printMemory() {

	}

}
