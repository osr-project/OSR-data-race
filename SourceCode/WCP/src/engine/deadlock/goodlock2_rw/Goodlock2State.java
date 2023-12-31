package engine.deadlock.goodlock2_rw;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import engine.deadlock.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Pair;
import util.vectorclock.VectorClock;


//Manages the clocks and other data structures used by the WCP algorithm
public class Goodlock2State extends State{

	// Internal data
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<VectorClock> clockThread;
	public ArrayList<VectorClock> readBlock;
	public ArrayList<VectorClock> writeBlock;

	public HashMap<Thread, Stack<Lock>> locksHeld;	
	public HashMap<Lock, HashMap<Lock, HashMap<Thread, HashSet<Pair<Long, Pair<VectorClock, HashSet<Lock>>>>>>> lockEdges; // l1 -> l2 -> t -> set_of_set_of_locks. // Lock l2 is acquired when lock l1 is held by t and the set of locks held at the point of acquiring l2 is one among lockEdges(l1)(l2)(t).second at event-id lockEdges(l1)(l2)(t).first .

	public  HashMap<String, HashSet<Long>> specialWrites;
	
	//parameter flags
	public int verbosity;

	public Goodlock2State(HashSet<Thread> tSet, HashMap<String, HashSet<Long>> specialWrites, int verbosity) {
		this.verbosity = verbosity;
		this.specialWrites = specialWrites;
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
		this.clockThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);		
		for (int i = 0; i < this.numThreads; i++) {
			VectorClock C_t = this.clockThread.get(i);
			C_t.setClockIndex(i, 1);
		}
		
		// initialize lastWriteVariable
		this.readBlock = new ArrayList<VectorClock>();
		this.writeBlock = new ArrayList<VectorClock>();
		
		locksHeld = new HashMap<Thread, Stack<Lock>> ();
		for(Thread t: tSet){
			locksHeld.put(t,  new Stack<Lock>());
		}
		lockEdges = new HashMap<Lock, HashMap<Lock, HashMap<Thread, HashSet<Pair<Long, Pair<VectorClock, HashSet<Lock>>>>>>>();
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
			readBlock.add(new VectorClock(this.numThreads));
			writeBlock.add(new VectorClock(this.numThreads));
		}
		return variableToIndex.get(v);
	}
	
	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = getVectorClock(clockThread, t);
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

	public void setIndex(VectorClock vc, Thread t, int val){
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}
	
	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}
	
	public void printThreadClock(){
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for(Thread thread : threadToIndex.keySet()){
			VectorClock C_t = getVectorClock(clockThread, thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}


	public static <T> HashSet<T> stackToSet(Stack<T> stack){
		HashSet<T> set = new HashSet<T>();
		for(T t : stack){
			set.add(t);
		}
		return set;
	}

	@Override
	public void printMemory() {

	}
}