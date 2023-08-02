package engine.racedetection.soundshb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

public class SoundSHBState extends State {

	// Internal data
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;

	// Data used for algorithm
	public ArrayList<VectorClock> clockThread;
	public ArrayList<VectorClock> lastReleaseLock;
	public ArrayList<VectorClock> readVariable;
	public ArrayList<VectorClock> writeVariable;
	public ArrayList<VectorClock> lastWriteVariable;
	public ArrayList<ArrayList<ArrayList<Integer>>> FWALR; // x -> t -> t'  -> Int
	
	//Book-keeping the last-write's location
	public ArrayList<Integer> lastWriteVariableLocId;
	
	//parameter flags
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	public SoundSHBState(HashSet<Thread> tSet, int verbosity) {
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
		
		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
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
		
		// initialize lastReleaseLock
		this.lastReleaseLock = new ArrayList<VectorClock>();

		// initialize readVariable
		this.readVariable = new ArrayList<VectorClock>();

		// initialize writeVariable
		this.writeVariable = new ArrayList<VectorClock>();
		
		// initialize lastWriteVariable
		this.lastWriteVariable = new ArrayList<VectorClock>();
		
		//initialize FWALR
		this.FWALR = new ArrayList<ArrayList<ArrayList<Integer>>>();
		
		//initialize locationIds
		this.lastWriteVariableLocId = new ArrayList<Integer> ();
	}
	
	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	private int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			//System.err.println("New lock found " + this.numLocks);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			
			lastReleaseLock.add(new VectorClock(this.numThreads));
		}
		return lockToIndex.get(l);
	}
	
	private int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			readVariable			.add(new VectorClock(this.numThreads));
			writeVariable			.add(new VectorClock(this.numThreads));
			lastWriteVariable		.add(new VectorClock(this.numThreads));
			ArrayList<ArrayList<Integer>> FWALR_var = new ArrayList<ArrayList<Integer>> ();
			for (int i = 0; i < this.numThreads; i++) {
				ArrayList<Integer> intermediateList = new ArrayList<Integer> ();
				for (int j = 0; j < this.numThreads; j++) {
					intermediateList.add(0);
				}	
				FWALR_var.add(intermediateList);
			}
			FWALR.add(FWALR_var);
			lastWriteVariableLocId	.add(-1); //Initialize loc id's to be -1
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

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Lock l) {
		int lIndex = checkAndAddLock(l);
		return getVectorClockFrom1DArray(arr, lIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr, vIndex);
	}
	
	public void reInitializeFWALROnRead(Thread t, Variable v){
		int vIndex = checkAndAddVariable(v);
		int tIndex = threadToIndex.get(t);
		ArrayList<Integer> FWALR_x_t = this.FWALR.get(vIndex).get(tIndex);
		for(int uIndex = 0; uIndex < this.numThreads; uIndex++){
			FWALR_x_t.set(uIndex, 0);
		}
	}
	
	public void setFWALROnWrite(Thread t, Variable v, int localClock){
		//maybe local clock need not be passed.
		int vIndex = checkAndAddVariable(v);
		int tIndex = threadToIndex.get(t);
		ArrayList<ArrayList<Integer>> FWALR_x = this.FWALR.get(vIndex);
		for(int uIndex = 0; uIndex < this.numThreads; uIndex++){
			if(FWALR_x.get(uIndex).get(tIndex) == 0){
				FWALR_x.get(uIndex).set(tIndex, localClock);	
			}
		}
	}
	
	public boolean checkReadWriteRaceUsingFWALR(Variable v, VectorClock C_t, VectorClock R_v){
		int vIndex = checkAndAddVariable(v);
		boolean raceDetected = false;
		
		for(int uIndex = 0; uIndex < this.numThreads; uIndex ++){
			if(R_v.getClockIndex(uIndex) > C_t.getClockIndex(uIndex)){
				boolean condition_on_FWALR = true;
				for(int tPrimeIndex = 0; tPrimeIndex < this.numThreads; tPrimeIndex ++){
					int FWALR_x_u_tPrime = this.FWALR.get(vIndex).get(uIndex).get(tPrimeIndex) ;
					boolean cond1 = FWALR_x_u_tPrime > 0;
					boolean cond2 = FWALR_x_u_tPrime > C_t.getClockIndex(tPrimeIndex);
					if(cond1 && !cond2){
						condition_on_FWALR = false;
						break;
					}
				}
				if(condition_on_FWALR){
					raceDetected = true;
					break;
				}
			}
		}
		
		return raceDetected;
	}
	
	public int getLWLocId(Variable v){
		int vIndex = checkAndAddVariable(v);
		return this.lastWriteVariableLocId.get(vIndex);
	}
	
	public void setLWLocId(Variable v, int loc){
		int vIndex = checkAndAddVariable(v);
		this.lastWriteVariableLocId.set(vIndex, loc);
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
	
	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.numLocks));
		System.err.println("Number of variables = " + Integer.toString(this.numVariables));
	}
}