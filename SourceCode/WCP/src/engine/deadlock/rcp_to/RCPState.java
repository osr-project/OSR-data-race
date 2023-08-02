package engine.deadlock.rcp_to;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;

import engine.deadlock.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Pair;
import util.vectorclock.ClockEpochPair;
import util.vectorclock.Epoch;
import util.vectorclock.VectorClock;

//Manages the clocks and other data structures used by the WCP algorithm
public class RCPState extends State{

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashSet<Lock> lockSet;
	private HashSet<Variable> variableSet;
	
	private static final int NIL = -1; 

	private int numThreads;

	// Data used for algorithm
	public ArrayList<VectorClock> TOThread; //T_t // Thread -> VC
	public ArrayList<VectorClock> RHBThread; //H_t // Thread -> VC
	public ArrayList<VectorClock> StrictRCPThread; //P_t //Thread->VC
	public HashMap<Lock, VectorClock> RHBLock; //H_l //Lock -> VC
	public HashMap<Lock, VectorClock> StrictRCPLock; //P_l //Lock-> VC
	public HashMap<Variable, ArrayList<VectorClock>> RHBReadVariableThread; //R_{x,t} //Variable -> Thread -> VC
	public HashMap<Variable, VectorClock> RHBWriteVariable; //W_x //Variable -> VC
	public RCPView view;
	public HashMap<Variable, HashSet<Integer>> threadsReadingSinceLastWrite; //rThreads_x //Variable -> Set_of_Threads
	public HashMap<Variable, Integer> lastWritingThread; //lastWThread_x //Variable ->Thread

	//Data used for checking deadlocks
	public HashMap<Lock, ArrayList<Pair<HashSet<Lock>, Epoch>>> lockAcquisitionHistory; //Lock -> List<(LocksHeld, epoch)>

	//Data used for online tracking of locks
	public HashMap<Thread, Stack<Lock>> mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();

	//Data for offline space saving
	//public HashMap<String, HashSet<String>> lockReadVariables;
	//public HashMap<String, HashSet<String>> lockWriteVariables;
	public HashMap<String, HashMap<String, HashSet<String>>> existsLockReadVariableThreads;
	public HashMap<String, HashMap<String, HashSet<String>>> existsLockWriteVariableThreads;
	public HashMap<String, Long> variableToReadEquivalenceClass;
	public HashMap<String, Long> variableToWriteEquivalenceClass;
	//public HashMap<String, HashSet<String>> variableToAccessedLocks;

	//Space saving
	private VectorClock local_vc_imax;

	//parameter flags
	public int verbosity;

	public RCPState(HashSet<Thread> tSet, int verbosity) {
		this.verbosity = verbosity;
		initInternalData(tSet);
		initData(tSet);
		initOnlineData();
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

		this.lockSet = new HashSet<Lock>();
		this.variableSet = new HashSet<Variable>();

		local_vc_imax = new VectorClock(this.numThreads);
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	public void initData(HashSet<Thread> tSet) {
		// initialize RCPThread
		this.TOThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.TOThread, this.numThreads);
		for(int tIndex = 0; tIndex < this.numThreads; tIndex ++){
			this.TOThread.get(tIndex).setClockIndex(tIndex, 1);
		}

		// initialize RHBThread
		this.RHBThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.RHBThread, this.numThreads);
		for(int tIndex = 0; tIndex < this.numThreads; tIndex ++){
			this.RHBThread.get(tIndex).setClockIndex(tIndex, 1);
		}

		// initialize StrictRCPThread
		this.StrictRCPThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.StrictRCPThread, this.numThreads);

		// initialize RHBLock
		this.RHBLock = new HashMap<Lock, VectorClock>();


		// initialize StrictRCPLock
		this.StrictRCPLock = new HashMap<Lock, VectorClock>();

		// initialize readVariable
		this.RHBReadVariableThread = new HashMap<Variable, ArrayList<VectorClock>>();

		// initialize writeVariable
		this.RHBWriteVariable = new HashMap<Variable, VectorClock>();

		// initialize view
		this.view = new RCPView(tSet);

		// initialize threadsReadingSinceLastWrite
		this.threadsReadingSinceLastWrite = new HashMap<Variable, HashSet<Integer>> ();

		// initialize lastWritingThread
		this.lastWritingThread = new HashMap<Variable, Integer> ();

		//initialize lockAcquisition History
		this.lockAcquisitionHistory = new HashMap<Lock, ArrayList<Pair<HashSet<Lock>, Epoch>>> ();
	}

	public void initOnlineData(){
		mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	}

	// Access methods
	private VectorClock getVectorClockFrom1DArray(ArrayList<VectorClock> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	private void checkAndAddLock(Lock l){
		if(!lockSet.contains(l)){
			lockSet.add(l);
			RHBLock.put(l, new VectorClock(this.numThreads));
			StrictRCPLock.put(l, new VectorClock(this.numThreads));
			view.checkAndAddLock(l);
			this.lockAcquisitionHistory.put(l, new ArrayList<Pair<HashSet<Lock>, Epoch>> ());
		}
	}

	private void checkAndAddVariable(Variable v){
		if(!variableSet.contains(v)){
			variableSet.add(v);
			RHBReadVariableThread.put(v, new ArrayList<VectorClock> ());
			for(int tIndex = 0; tIndex < this.numThreads; tIndex ++){
				RHBReadVariableThread.get(v).add(new VectorClock(this.numThreads));
			}
			RHBWriteVariable.put(v, new VectorClock(this.numThreads));
			threadsReadingSinceLastWrite.put(v, new HashSet<Integer> ());
			lastWritingThread.put(v, NIL);
		}
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock T_t = getVectorClock(TOThread, t);
		int origVal = T_t.getClockIndex(tIndex);
		T_t.setClockIndex(tIndex, (Integer)(origVal + 1));
	}
	
	public void incRHBThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock RHB_t = getVectorClock(RHBThread, t);
		int origVal = RHB_t.getClockIndex(tIndex);
		RHB_t.setClockIndex(tIndex, (Integer)(origVal + 1));
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(HashMap<Lock, VectorClock> arr, Lock l) {
		checkAndAddLock(l);
		return arr.get(l);
	}

	public VectorClock getVectorClock(HashMap<Variable, VectorClock> arr, Variable v) {
		checkAndAddVariable(v);
		return arr.get(v);
	}

	public VectorClock getVectorClock(HashMap<Variable, ArrayList<VectorClock>> arr, Variable v, Thread t) {
		int tIndex = threadToIndex.get(t);
		checkAndAddVariable(v);
		return getVectorClockFrom1DArray(arr.get(v), tIndex);
	}

	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		VectorClock T_t = getVectorClock(TOThread, t);
		Epoch epoch_t = getEpoch(T_t, t);
		view.pushClockPair(l, new ClockEpochPair(epoch_t));
//		System.out.println("Pushed " + epoch_t + " to " + view);
	}

	public void readViewOfWriters(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		local_vc_imax.setToZero();
		VectorClock P_t = getVectorClock(StrictRCPThread, t);
		VectorClock T_t = getVectorClock(TOThread, t);
		view.getMaxLowerBoundUntil(t, l, P_t, T_t, local_vc_imax);
		P_t.updateWithMax(P_t, local_vc_imax);
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		VectorClock H_t = this.getVectorClock(RHBThread, t);
		view.updateTopRelease(l, H_t);
	}

	public void printThreadClock(){
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for(Thread thread : threadToIndex.keySet()){
			VectorClock C_t = this.getVectorClock(TOThread, thread);
			printVC.add(C_t);
		}
		System.out.println(printVC);
		System.out.println();
		System.out.println("%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%");
	}

	public <T> HashSet<T> stackToSet(Stack<T> stack){
		HashSet<T> set = new HashSet<T>();
		for(T t : stack){
			set.add(t);
		}
		return set;
	}

	public HashSet<Lock> getSetFromStack(Thread t){
		return stackToSet(this.mapThreadLockStack.get(t));
	}

	public boolean isLockAcquired(Thread t, Lock l){
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		return this.getSetFromStack(t).contains(l);
	}
	
	public VectorClock generateTimestamp(Thread t){
		VectorClock C_t = new VectorClock(getVectorClock(TOThread, t));
		VectorClock P_t = getVectorClock(StrictRCPThread, t);
		C_t.updateWithMax(C_t, P_t);
		return C_t;
	}

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
	}

	public void updateWithNonLocalAccessHistory(VectorClock vc, Thread t, Variable v, HashMap<Variable, ArrayList<VectorClock>> AH){
		VectorClock T_t = getVectorClock(TOThread, t);
		VectorClock tmp_vc;
		for(Entry<Thread, Integer> entry : this.threadToIndex.entrySet()){
			Thread tPrime = entry.getKey();
			int tPrimeIndex = entry.getValue();
			tmp_vc = this.getVectorClock(AH, v, tPrime);
			int local_clock_tprime = tmp_vc.getClockIndex(tPrimeIndex);
			if(local_clock_tprime > T_t.getClockIndex(tPrimeIndex)){
				vc.updateWithMax(vc, tmp_vc);
			}
		}
	}

	public void printViewSize(){
		this.view.printSize();
	}

	public void printMemory(){
		System.err.println("Number of threads = " + Integer.toString(this.numThreads));
		System.err.println("Number of locks = " + Integer.toString(this.lockSet.size()));
		System.err.println("Number of variables = " + Integer.toString(this.variableSet.size()));
		this.view.printSize();
	}

	public void destroyLock(Lock l){
		if(!lockSet.contains(l)){
			throw new IllegalArgumentException("Cannot delete non-existent lock " + l.getName());
		}
		else{
			//System.out.println("Destroying lock " + l.getName().toString());
			lockSet.remove(l);
			RHBLock.remove(l);
			StrictRCPLock.remove(l);
			view.destroyLock(l);
		}
	}

	public void destroyLockThreadStack(Lock l, Thread t){
		if(!lockSet.contains(l)){
			throw new IllegalArgumentException("Cannot delete stacks for non-existent lock " + l.getName());
		}
		else if(!threadToIndex.containsKey(t)){
			throw new IllegalArgumentException("Cannot delete stacks for non-existent thread " + t.getName());
		}
		else if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		else{
			//System.out.println("Destroying stack for lock " + l.getName().toString() + " and Thread " + t.getName().toString());
			view.destroyLockThreadStack(l, t);
		}
	}

	public Epoch getEpoch(VectorClock vc, Thread t){
		Epoch ep = new Epoch();
		int tIndex = threadToIndex.get(t);
		int c = vc.getClockIndex(tIndex);
		ep.setClock(c);
		ep.setThreadIndex(tIndex);
		return ep;
	}

	/*
	public void destroyVariable(Variable v){
		if(!variableSet.contains(v)){
			throw new IllegalArgumentException("Cannot delete non-existent variable " + v.getName());
		}
		else{
			//System.out.println("Destroying variable " + v.getName().toString());
			variableSet.remove(v);

			for (Lock l : lockSet){
				lastReleaseLockReadVariableThread.get(l).remove(v);
				lastReleaseLockWriteVariableThread.get(l).remove(v);
			}

			readVariable.remove(v);
			writeVariable.remove(v);	
		}
	}
	 */
}