package engine.dataflow.lastwrite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.dataflow.State;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;


//Manages the clocks and other data structures for computing data flow.
public class LastWriteState extends State{

	// Internal data
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<VectorClock> threadClock;
	public ArrayList<VectorClock> lastWriteClock;
	
	// Clocks to compute data flow
	public VectorClock branchClock;
	public HashMap<Variable, HashMap<Thread, VectorClock>> firstAccessEventClock;
	
	//parameter flags
	public int verbosity;

	public LastWriteState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
	}
	
	private void initInternalData(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Integer>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			//System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (Integer)this.numThreads);
			this.numThreads ++;
		}
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {
		// initialize clockThread
		this.threadClock = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.threadClock, this.numThreads);		
		for (int i = 0; i < this.numThreads; i++) {
			VectorClock C_t = this.threadClock.get(i);
			C_t.setClockIndex(i, 1);
		}
		
		// initialize lastWriteClock
		this.lastWriteClock = new ArrayList<VectorClock>();
		
		// initialize branch clock
		this.branchClock = new VectorClock(this.numThreads);
		
		// initialize first access clocks
		this.firstAccessEventClock = new HashMap<Variable, HashMap<Thread, VectorClock>> ();
	}
	
	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}
	
	private int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			this.lastWriteClock.add(new VectorClock(this.numThreads));
		}
		return variableToIndex.get(v);
	}
	
	public void incThreadClockLocally(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = getVectorClock(threadClock, t);
		int origVal = C_t.getClockIndex(tIndex);
		C_t.setClockIndex(tIndex, origVal + 1);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr, vIndex);
	}
	
	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}

	@Override
	public void printMemory() {		
	}
	
	public HashSet<String> getUsefulVariables(){
		HashSet<String> usefulVars = new HashSet<String> ();
		for(Variable v: this.firstAccessEventClock.keySet()){
			for(Thread t: this.firstAccessEventClock.get(v).keySet()){
				VectorClock C_v_t = this.firstAccessEventClock.get(v).get(t);
				if(C_v_t.isLessThanOrEqual(this.branchClock)){
					usefulVars.add(v.getName());
					break;
				}
			}
		}
		return usefulVars;
	}
	
}