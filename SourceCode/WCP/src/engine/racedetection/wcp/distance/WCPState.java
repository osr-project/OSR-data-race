package engine.racedetection.wcp.distance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Stack;

import engine.racedetection.State;
import event.Lock;
import event.Thread;
import event.Variable;
import util.vectorclock.ClockPair;
import util.vectorclock.VectorClock;

//Manages the clocks and other data structures used by the WCP algorithm
public class WCPState extends State{

	// Internal data
	public HashMap<Thread, Integer> threadToIndex;
	private HashSet<Lock> lockSet;
	private HashSet<Variable> variableSet;

	private int numThreads;

	// Data used for algorithm
	private ArrayList<Integer> clockThread;
	private ArrayList<VectorClock> WCPThread;
	public ArrayList<VectorClock> HBPredecessorThread;
	public HashMap<Lock, VectorClock> HBPredecessorLock;
	//public HashMap<Lock, VectorClock> lastReleaseLock;
	public ArrayList<VectorClock> WCPPredecessorThread;
	public HashMap<Lock, VectorClock> WCPPredecessorLock;
	//	public HashMap<Lock, HashMap<Long, VectorClock>> lastReleaseLockReadVariable;
	//	public HashMap<Lock, HashMap<Long, VectorClock>> lastReleaseLockWriteVariable;

	public HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>> lastReleaseLockReadVariableThread;
	public HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>> lastReleaseLockWriteVariableThread;

	public HashMap<Variable, VectorClock> readVariable;
	public HashMap<Variable, VectorClock> writeVariable;
	/***The next two clocks are relevant for and only for the "forceOrder" feature ***/
	public HashMap<Variable, VectorClock> HBPredecessorReadVariable;
	public HashMap<Variable, VectorClock> HBPredecessorWriteVariable;
	/*********************************************************************************/
	public WCPView view;

	//Data used for online tracking of locks and variables
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
	public HashMap<Thread, Stack<HashSet<Variable>>> mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
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
	public boolean forceOrder;
	public boolean tickClockOnAccess;
	public int verbosity;

	// Distance stats
	public HashMap<Variable, HashMap<Thread, Long>> readVariableAuxId;
	public HashMap<Variable, HashMap<Thread, Long>> writeVariableAuxId;
	// == stats ==
	public long maxMaxDistance = 0;
	public long sumMaxDistance = 0;
	public long maxMinDistance = 0;
	public long sumMinDistance = 0;
	public long numRaces = 0;

	public WCPState(HashSet<Thread> tSet, boolean forceOrder, boolean tickClockOnAccess, int verbosity) {
		this.forceOrder = forceOrder;
		this.tickClockOnAccess = tickClockOnAccess;
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
		// Initialize clockThread
		this.clockThread = new ArrayList<Integer>();
		for (int i = 0; i < this.numThreads; i++) {
			this.clockThread.add((Integer)1);
		}

		// initialize WCPThread
		this.WCPThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.WCPThread, this.numThreads);

		// initialize HBPredecessorThread
		this.HBPredecessorThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.HBPredecessorThread, this.numThreads);

		// initialize HBPredecessorLock
		this.HBPredecessorLock = new HashMap<Lock, VectorClock>();

		// initialize WCPPredecessorThread
		this.WCPPredecessorThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.WCPPredecessorThread, this.numThreads);

		// initialize WCPPredecessorLock
		this.WCPPredecessorLock = new HashMap<Lock, VectorClock>();

		// initialize lastReleaseLockReadVariable
		//this.lastReleaseLockReadVariable = new HashMap<Lock, HashMap<Variable, VectorClock>>();
		//this.lastReleaseLockReadVariable = new HashMap<Lock, HashMap<Long, VectorClock>>();
		this.lastReleaseLockReadVariableThread = new HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>>();

		// initialize lastReleaseLockWriteVariable
		//this.lastReleaseLockWriteVariable = new HashMap<Lock, HashMap<Variable, VectorClock>>();
		//this.lastReleaseLockWriteVariable = new HashMap<Lock, HashMap<Long, VectorClock>>();
		this.lastReleaseLockWriteVariableThread = new HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>>>();

		// initialize readVariable
		this.readVariable = new HashMap<Variable, VectorClock>();

		// initialize writeVariable
		this.writeVariable = new HashMap<Variable, VectorClock>();

		// initialize HBPredecessorReadVariable
		this.HBPredecessorReadVariable = new HashMap<Variable, VectorClock>();

		// initialize HBPredecessorWriteVariable
		this.HBPredecessorWriteVariable = new HashMap<Variable, VectorClock>();

		// initialize view
		this.view = new WCPView(tSet);

		// distance stats
		this.readVariableAuxId = new HashMap<Variable, HashMap<Thread, Long>> ();
		this.writeVariableAuxId = new HashMap<Variable, HashMap<Thread, Long>> ();
	}

	public void initOnlineData(){
		mapThreadReadVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadWriteVarSetStack = new HashMap<Thread, Stack<HashSet<Variable>>>();
		mapThreadLockStack = new HashMap<Thread, Stack<Lock>>();
	}

	// Access methods
	private <E> E getVectorClockFrom1DArray(ArrayList<E> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	private void checkAndAddLock(Lock l){
		if(!lockSet.contains(l)){
			lockSet.add(l);

			HBPredecessorLock.put(l, new VectorClock(this.numThreads));
			WCPPredecessorLock.put(l, new VectorClock(this.numThreads));

			//System.out.println(existsLockReadVariableThreads);

			String lName = l.getName();
			//System.out.println("lName ::= " + lName);
			if(existsLockReadVariableThreads.containsKey(lName)){
				this.lastReleaseLockReadVariableThread.put(l, new HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>> ());
				for (Variable v : variableSet){
					String vName = v.getName();
					Long vEquivRead = this.variableToReadEquivalenceClass.get(vName);
					Long vEquivWrite = this.variableToWriteEquivalenceClass.get(vName);
					//					System.out.println("vName = " + vName);
					//					System.out.println("vEquiv = " + vEquivRead + "," + vEquivWrite);
					if(existsLockReadVariableThreads.get(lName).containsKey(vName)){
						if(!lastReleaseLockReadVariableThread.get(l).containsKey(vEquivRead)){
							this.lastReleaseLockReadVariableThread.get(l).put(
									vEquivRead, new HashMap<Long, HashMap<Thread, VectorClock>>());
						}
						if(!lastReleaseLockReadVariableThread.get(l).get(vEquivRead).containsKey(vEquivWrite)){
							this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).put(
									vEquivWrite, new HashMap<Thread, VectorClock>());
						}
						for(Thread t : this.threadToIndex.keySet()){
							if(existsLockReadVariableThreads.get(lName).get(vName).contains(t.getName())){
								//System.out.println("Lock: Adding to Read " + l + ", (" + vEquivRead + "," + vEquivWrite + "), " + t);
								this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).get(vEquivWrite).put(t, new VectorClock(this.numThreads));
							}
						}



					}

				}
			}

			if(existsLockWriteVariableThreads.containsKey(lName)){
				this.lastReleaseLockWriteVariableThread.put(l, new HashMap<Long, HashMap<Long, HashMap<Thread, VectorClock>>> ());
				for (Variable v : variableSet){
					String vName = v.getName();
					Long vEquivRead = this.variableToReadEquivalenceClass.get(vName);
					Long vEquivWrite = this.variableToWriteEquivalenceClass.get(vName);
					if(existsLockWriteVariableThreads.get(lName).containsKey(vName)){
						if(!lastReleaseLockWriteVariableThread.get(l).containsKey(vEquivRead)){
							this.lastReleaseLockWriteVariableThread.get(l).put(vEquivRead,
									new HashMap<Long, HashMap<Thread, VectorClock>>());
						}
						if(!lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).containsKey(vEquivWrite)){
							this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).put(vEquivWrite,
									new HashMap<Thread, VectorClock>());
						}
						for(Thread t : this.threadToIndex.keySet()){
							if(existsLockWriteVariableThreads.get(lName).get(vName).contains(t.getName())){
								//System.out.println("Lock: Adding to Write " + l + ", (" + vEquivRead + "," + vEquivWrite + "), " + t);
								this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).get(vEquivWrite).put(t, new VectorClock(this.numThreads));
							}
						}
					}
				}
			}

			view.checkAndAddLock(l);
		}
	}

	private void checkAndAddVariable(Variable v){
		if(!variableSet.contains(v)){
			//System.out.println("Adding variable " + v.getName().toString());
			variableSet.add(v);

			Long vEquivRead = this.variableToReadEquivalenceClass.get(v.getName());
			Long vEquivWrite = this.variableToWriteEquivalenceClass.get(v.getName());

			for (Lock l : lockSet){
				if(existsLockReadVariableThreads.containsKey(l.getName())){
					if(existsLockReadVariableThreads.get(l.getName()).containsKey(v.getName())){
						if(!this.lastReleaseLockReadVariableThread.get(l).containsKey(vEquivRead)){
							this.lastReleaseLockReadVariableThread.get(l).put(vEquivRead, new HashMap<Long, HashMap<Thread, VectorClock>>());	
						}
						if(!this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).containsKey(vEquivWrite)){
							this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).put(vEquivWrite, new HashMap<Thread, VectorClock>());	
						}
						HashSet<String> threads = existsLockReadVariableThreads.get(l.getName()).get(v.getName());
						for(Thread t: this.threadToIndex.keySet()){
							if(threads.contains(t.getName())){
								if(!lastReleaseLockReadVariableThread.get(l).get(vEquivRead).get(vEquivWrite).containsKey(t)){
									//System.out.println("Var: Adding to Read " + l + ", " + v + ", " + t);
									this.lastReleaseLockReadVariableThread.get(l).get(vEquivRead).get(vEquivWrite).put(t, new VectorClock(this.numThreads));
								}
							}
						}
					}
				}


				if(existsLockWriteVariableThreads.containsKey(l.getName())){
					if(existsLockWriteVariableThreads.get(l.getName()).containsKey(v.getName())){
						if(!this.lastReleaseLockWriteVariableThread.get(l).containsKey(vEquivRead)){
							this.lastReleaseLockWriteVariableThread.get(l).put(vEquivRead, new HashMap<Long, HashMap<Thread, VectorClock>>());	
						}
						if(!this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).containsKey(vEquivWrite)){
							this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).put(vEquivWrite, new HashMap<Thread, VectorClock>());	
						}
						HashSet<String> threads = existsLockWriteVariableThreads.get(l.getName()).get(v.getName());
						for(Thread t: this.threadToIndex.keySet()){
							if(threads.contains(t.getName())){
								if(!lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).get(vEquivWrite).containsKey(t)){
									//System.out.println("Var: Adding to Write " + l + ", " + v + ", " + t);
									this.lastReleaseLockWriteVariableThread.get(l).get(vEquivRead).get(vEquivWrite).put(t, new VectorClock(this.numThreads));
								}
							}
						}
					}
				}
			}



			readVariable.put(v, new VectorClock(this.numThreads));
			writeVariable.put(v, new VectorClock(this.numThreads));

			HBPredecessorReadVariable.put(v, new VectorClock(this.numThreads));
			HBPredecessorWriteVariable.put(v, new VectorClock(this.numThreads));
			
			readVariableAuxId.put(v, new HashMap<Thread, Long> ());
			writeVariableAuxId.put(v, new HashMap<Thread, Long> ());

		}
	}

	public int getClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		return clockThread.get(tIndex);
	}

	public VectorClock generateVectorClockFromClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		//System.out.println("numThreads = " + this.numThreads);
		VectorClock wcpClock = getVectorClock(WCPThread, t);
		VectorClock pred = getVectorClock(WCPPredecessorThread, t);
		//System.out.println("numThreads = " + this.numThreads);
		int tValue = getClockThread(t);

		wcpClock.copyFrom(pred);
		wcpClock.setClockIndex(tIndex, tValue);
		return wcpClock;
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		int origVal = clockThread.get(tIndex);
		clockThread.set(tIndex, (Integer)(origVal + 1));
	}

	public <E> E getVectorClock(ArrayList<E> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getVectorClockFrom1DArray(arr, tIndex);
	}

	public <E> E getVectorClock(HashMap<Lock, E> arr, Lock l) {
		checkAndAddLock(l);
		return arr.get(l);
	}

	public <E> E getVectorClock(HashMap<Variable, E> arr, Variable v) {
		checkAndAddVariable(v);
		return arr.get(v);
	}

	public <E> E getVectorClock(HashMap<Lock, HashMap<Long, HashMap<Long, HashMap<Thread, E>>>> arr, Lock l, Variable v, Thread t) {
		//System.out.println("Here : " + l + " " + v + " " + t);
		checkAndAddLock(l);
		checkAndAddVariable(v);
		//		if(existsLockReadVariableThreads.containsKey(l.getName())){
		//			System.out.println(l + " has an entry");
		//			if(existsLockReadVariableThreads.get(l.getName()).containsKey(v.getName())){
		//				System.out.println(v + " has an entry");
		//				System.out.println("Equivalence class of " + v + " = " + variableToReadEquivalenceClass.get(v.getName()));
		//				if(existsLockReadVariableThreads.get(l.getName()).get(v.getName()).contains(t.getName())){
		//					System.out.println(t + " has an entry");
		//					System.out.println(lastReleaseLockReadVariableThread.get(l).get(variableToReadEquivalenceClass.get(v.getName())).get(t));
		//				}
		//			}
		//		}

		Long vEquivRead = this.variableToReadEquivalenceClass.get(v.getName());
		Long vEquivWrite = this.variableToWriteEquivalenceClass.get(v.getName());	
		//System.out.println("getClock = (" + vEquivRead + "," + vEquivWrite + ")  " );
		if (!arr.containsKey(l) )
			throw new IllegalArgumentException("No l found"); 
		if (!arr.get(l).containsKey(vEquivRead) )
			throw new IllegalArgumentException("No vEquivRead"); 
		if (!arr.get(l).get(vEquivRead).containsKey(vEquivWrite) )
			throw new IllegalArgumentException("No vEquivRead");
		if (!arr.get(l).get(vEquivRead).get(vEquivWrite).containsKey(t) )
			throw new IllegalArgumentException("No t found"); 


		return arr.get(l).get(vEquivRead).get(vEquivWrite).get(t);
	}

	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		//System.out.println("updateViewAsWriterAtAcquire : (t, l) = (" + tIndex + ", " + lIndex + ")" );
		VectorClock C_t = generateVectorClockFromClockThread(t);
		view.pushClockPair(l, new ClockPair(C_t));
		//System.out.println(view);
	}

	/*
	public void readViewOfWriters(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		//System.out.println("readViewOfWriters : (t, l) = (" + tIndex + ", " + lIndex + ")" );
		local_vc_imax.setToZero();
		VectorClock P_t = getVectorClock(WCPPredecessorThread, t);
		VectorClock C_t = generateVectorClockFromClockThread(t);
		view.getMaxLowerBound(t, l, C_t, local_vc_imax);
		P_t.updateWithMax(P_t, local_vc_imax);		
	}
	 */

	public void readViewOfWriters(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		//System.out.println("readViewOfWriters : (t, l) = (" + tIndex + ", " + lIndex + ")" );
		local_vc_imax.setToZero();
		VectorClock P_t = getVectorClock(WCPPredecessorThread, t);
		VectorClock C_t = generateVectorClockFromClockThread(t);
		view.getMaxLowerBound(t, l, C_t, local_vc_imax);
		P_t.updateWithMax(P_t, local_vc_imax);		
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		checkAndAddLock(l);
		if(!this.view.lockThreadLastInteraction.get(l.getName()).containsKey(t.getName())){
			throw new IllegalArgumentException("Invalid operation : No critical section on lock " + l.getName() + " in thread " + t.getName());
		}
		VectorClock C_t = generateVectorClockFromClockThread(t);
		VectorClock H_t = getVectorClock(HBPredecessorThread, t);

		//		System.out.println("DEBUG");
		//		System.out.println(this.view);
		//		System.out.println("DEBUG\n");

		view.updateTopRelease(l, C_t, H_t);
	}

	public void printThreadClock(){
		ArrayList<VectorClock> printVC = new ArrayList<VectorClock>();
		for(Thread thread : threadToIndex.keySet()){
			VectorClock C_t = generateVectorClockFromClockThread(thread);
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

	public boolean isThreadRelevant(Thread t){
		return this.threadToIndex.containsKey(t);
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
	
	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}
	
	public long getMaxAuxId(VectorClock confVC, VectorClock currVC, HashMap<Thread, Long> auxId_conf) {
		long maxId = -1;
		for(Thread t: auxId_conf.keySet()) {
			int tIndex_conf = getIndex(confVC, t);
			int tIndex_curr = getIndex(currVC, t);
			if(tIndex_conf > tIndex_curr) {
				long auxId_VC = auxId_conf.get(t);
				if(maxId < auxId_VC) {
					maxId = auxId_VC;
				}
			}
		}
		return maxId;
	}

	public long getMinAuxId(VectorClock confVC, VectorClock currVC, HashMap<Thread, Long> auxId_conf) {
		long minId = -1;
		for(Thread t: auxId_conf.keySet()) {
			int tIndex_conf = getIndex(confVC, t);
			int tIndex_curr = getIndex(currVC, t);
			if(tIndex_conf > tIndex_curr) {
				long auxId_VC = auxId_conf.get(t);
				if(minId == -1) {
					minId = auxId_VC;
				}
				else {
					if(minId > auxId_VC) {
						minId = auxId_VC;
					}
				}
				
			}
		}
		return minId;
	}

	public void destroyLock(Lock l){
		if(!lockSet.contains(l)){
			throw new IllegalArgumentException("Cannot delete non-existent lock " + l.getName());
		}
		else{
			//System.out.println("Destroying lock " + l.getName().toString());
			lockSet.remove(l);

			HBPredecessorLock.remove(l);
			WCPPredecessorLock.remove(l);

			lastReleaseLockReadVariableThread.remove(l);
			lastReleaseLockWriteVariableThread.remove(l);

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


	//	public void destroyVariable(Variable v){
	//		if(!variableSet.contains(v)){
	//			throw new IllegalArgumentException("Cannot delete non-existent variable " + v.getName());
	//		}
	//		else{
	//			//System.out.println("Destroying variable " + v.getName().toString());
	//			variableSet.remove(v);
	//			
	//			for (Lock l : lockSet){
	//				lastReleaseLockReadVariableThread.get(l).remove(v);
	//				lastReleaseLockWriteVariableThread.get(l).remove(v);
	//			}
	//			
	//			readVariable.remove(v);
	//			writeVariable.remove(v);

	//			HBPredecessorReadVariable.remove(v);
	//			HBPredecessorWriteVariable.remove(v);	
	//		}
	//	}
}