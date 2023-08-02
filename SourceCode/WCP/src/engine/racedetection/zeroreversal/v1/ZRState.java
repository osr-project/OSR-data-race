package engine.racedetection.zeroreversal.v1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import engine.racedetection.State;
import event.EventType;
import event.Lock;
import event.Thread;
import event.Variable;
import util.Quintet;
import util.Triplet;
import util.TripletComparators;
import util.ll.EfficientLLView;
import util.vectorclock.VectorClock;

public class ZRState extends State {

	public static EventType[] accessTypes =  {EventType.READ, EventType.WRITE};

	// == Internal data ==
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	private HashMap<Variable, Integer> variableToIndex;
	private int numThreads;
	private int numLocks;
	private int numVariables;
	private HashMap<Thread, HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>>> threadPairToAcquireInfoKeys;
	private HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> acquireInfoKeys;


	// == Data used for algorithm ==

	// 0. ThreadSet
	public HashSet<Thread> threads;
	public HashSet<Lock> locks;
	public HashSet<Variable> variables;

	// 1. Vector clocks
	public ArrayList<VectorClock> clockThread; // threadIndex -> VC
	public ArrayList<VectorClock> lastWriteVariable; // variableIndex -> VC
	// lastAcquire(t)(l) is the globalIndex of the last acquire on l 
	// which is in the ideal of e_t, where e_t is the last event performed by t.
	public ArrayList<LockVC> lastAcquireThread; // threadIndex -> lock-VC. 
	// lastAcquireWriteVariable(x)(l) is the globalIndex of the last acquire on l 
	// which is in the ideal of e_x, where e_x is the last write event on x.
	public ArrayList<LockVC> lastAcquireWriteVariable; // variableIndex -> lock-VC. 
	//
	public HashMap<Quintet<Thread, EventType, Thread, EventType, Variable>, VectorClock> lastIdeal;

	// 2. Scalars
	public long numAcquires; // counts the total number of acquire events seen

	// 3. Views
	public HashMap<Thread, HashMap<EventType, HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>>>>> accessInfo;
	public HashMap<Thread, HashMap<Lock, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>>>> acquireInfo;
	// == End of data used for algorithm ==

	public TripletComparators.FirstComparator<Integer, Long, VectorClock> firstComparatorAcquireInfo;
	public TripletComparators.SecondComparator<VectorClock, VectorClock, LockVC> secondComparatorAccessInfo;
	public Triplet<VectorClock, VectorClock, LockVC> bottomVCTriplet;

	// Lockset optimization
	public HashMap<Thread, HashMap<Lock, Integer>> threadToLockDepth;
	public HashMap<Thread, HashSet<Lock>> threadToLocksHeld;
	public HashMap<Variable, HashSet<Lock>> variableToLockset;
	private Lock readLock;
	private HashMap<Thread, Lock> threadLock;
	
	// Reducing number of pointers
	public HashMap<String, HashSet<String>> stringVariableToThreadSet; // name(x) -> set of thread-names that access x
	public HashMap<Variable, HashSet<Thread>> variableToThreadSet; // x -> set of threads that access x
	public HashMap<Thread, HashSet<Lock>> locksAccessed;

	// == parameter flags ==
	public boolean forceOrder;
	public int verbosity;

	public ZRState(HashSet<Thread> tSet, int verbosity) {
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
			this.threadToIndex.put(thread, (Integer)this.numThreads);
			this.numThreads ++;
		}
		this.lockToIndex = new HashMap<Lock, Integer>();
		this.numLocks = 0;
		this.variableToIndex = new HashMap<Variable, Integer>();
		this.numVariables = 0;

		this.readLock = new Lock("__READ-LOCK__");
		this.threadLock = new HashMap<Thread, Lock> ();
		for(Thread t: tSet) {
			Lock tLock = new Lock("__Thred-" + t.getName() + "-LOCK__");
			this.threadLock.put(t,  tLock);
		}
	}

	private void initialize1DArrayOfVectorClocksWithBottom(ArrayList<VectorClock> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new VectorClock(this.numThreads));
		}
	}

	private void initialize1DArrayOfLockVCWithBottom(ArrayList<LockVC> arr, int len) {
		for (int i = 0; i < len; i++) {
			arr.add(new LockVC(0));
		}
	}

	public void initData(HashSet<Thread> tSet) {

		this.threads = new HashSet<Thread> (tSet);
		this.locks = new HashSet<Lock> ();
		this.variables = new HashSet<Variable> ();

		this.threadPairToAcquireInfoKeys = new HashMap<Thread, HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>>> ();
		for(Thread t1: tSet) {
			HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>> acqInfo_t1 = new HashMap<Thread, HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>>> ();
			this.threadPairToAcquireInfoKeys.put(t1, acqInfo_t1);
			for(Thread t2: tSet) {
				if(t1.equals(t2)) {
					continue;
				}
				this.threadPairToAcquireInfoKeys.get(t1).put(t2,  new HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> ());
			}
		}
		this.acquireInfoKeys = new HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> ();

		// initialize clockThread
		this.clockThread = new ArrayList<VectorClock>();
		initialize1DArrayOfVectorClocksWithBottom(this.clockThread, this.numThreads);		
		for (int i = 0; i < this.numThreads; i++) {
			VectorClock C_t = this.clockThread.get(i);
			C_t.setClockIndex(i, 1);
		}

		// initialize lastWriteVariable
		this.lastWriteVariable = new ArrayList<VectorClock>();

		// initialize lastAcquireThread
		this.lastAcquireThread = new ArrayList<LockVC>();
		initialize1DArrayOfLockVCWithBottom(this.lastAcquireThread, this.numThreads);	

		// initialize lastWriteVariable
		this.lastAcquireWriteVariable = new ArrayList<LockVC>();
		
		// initialize lastIdeal
		this.lastIdeal = new HashMap<Quintet<Thread, EventType, Thread, EventType, Variable>, VectorClock> ();

		// initialize numAcquires
		this.numAcquires = 0L;

		// initialize ReadInfo and WriteInfo
		this.accessInfo = new HashMap<Thread, HashMap<EventType, HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>>>>> ();
		for (Thread t: tSet) {
			this.accessInfo.put(t, new HashMap<EventType, HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>>>> ());
			this.accessInfo.get(t).put(EventType.READ, new HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>>> ());
			this.accessInfo.get(t).put(EventType.WRITE, new HashMap<Variable, EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>>> ());			
		}

		// initialize AcquireInfo
		this.acquireInfo = new HashMap<Thread, HashMap<Lock, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>>>> ();
		for(Thread t: tSet) {
			this.acquireInfo.put(t,  new HashMap<Lock, EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>>>());
		}

		firstComparatorAcquireInfo = new TripletComparators.FirstComparator<Integer, Long, VectorClock> ();
		secondComparatorAccessInfo = new TripletComparators.SecondComparator<VectorClock, VectorClock, LockVC> ();
		bottomVCTriplet = new Triplet<VectorClock, VectorClock, LockVC> (new VectorClock(this.numThreads), new VectorClock(this.numThreads), new LockVC(0));

		this.threadToLockDepth = new HashMap<Thread, HashMap<Lock, Integer>> ();
		this.threadToLocksHeld = new HashMap<Thread, HashSet<Lock>> ();
		for(Thread t: tSet) {
			this.threadToLockDepth.put(t,  new HashMap<Lock, Integer> ());
			this.threadToLocksHeld.put(t,  new HashSet<Lock> ());
		}
		this.variableToLockset = new HashMap<Variable, HashSet<Lock>> ();
		
		this.stringVariableToThreadSet = null;
		this.variableToThreadSet = new HashMap<Variable, HashSet<Thread>> ();
		this.locksAccessed = new HashMap<Thread, HashSet<Lock>> ();
		for(Thread t: tSet) {
			this.locksAccessed.put(t,  new HashSet<Lock> ());
		}
	}

	// Access methods
	private static <E> E getElementFrom1DArray(ArrayList<E> arr, int index) {
		if (index < 0 || index >= arr.size()) {
			throw new IllegalArgumentException("Illegal Out of Bound access");
		}
		return arr.get(index);
	}

	public int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			locks.add(l);
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			for(Thread t: threadToIndex.keySet()) {
				this.acquireInfo.get(t).put(l, new EfficientLLView<Quintet<Thread, EventType, Thread, EventType, Variable>, Triplet<Integer, Long, VectorClock>> (this.acquireInfoKeys));
			}
		}
		return lockToIndex.get(l);
	}

	public int checkAndAddVariable(Variable v){
		if(!variableToIndex.containsKey(v)){
			variables.add(v);
			variableToIndex.put(v, this.numVariables);
			this.numVariables ++;
			this.lastWriteVariable.add(new VectorClock(this.numThreads));
			this.lastAcquireWriteVariable.add(new LockVC(0));
			
			
			HashSet<Thread> threadsAccessingVar = new HashSet<Thread> ();
			if(this.stringVariableToThreadSet == null) {
				this.variableToThreadSet.put(v, this.threads);
			}
			else if(!this.stringVariableToThreadSet.containsKey(v.getName())) {
				this.variableToThreadSet.put(v, threadsAccessingVar);
			}
			else {
				HashSet<String> stringthreadsAccessingVar = this.stringVariableToThreadSet.get(v.getName());
				for(Thread t: this.threads) {
					if (stringthreadsAccessingVar.contains(t.getName())) {
						threadsAccessingVar.add(t);
					}
				}
				this.variableToThreadSet.put(v, threadsAccessingVar);
			}
			
			
			for(Thread t: threadToIndex.keySet()) {
				if(!threadsAccessingVar.contains(t)) {
					continue;
				}
				this.accessInfo.get(t).get(EventType.READ).put(v, new EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>> (threadsAccessingVar));
				this.accessInfo.get(t).get(EventType.WRITE).put(v, new EfficientLLView<Thread, Triplet<VectorClock, VectorClock, LockVC>> (threadsAccessingVar));
				for(Thread u: threadToIndex.keySet()) {
					if(u.equals(t)) {
						continue;
					}
					
					if(!threadsAccessingVar.contains(u)) {
						continue;
					}
					
					Quintet<Thread, EventType, Thread, EventType, Variable> new_key_read_write = new Quintet<Thread, EventType, Thread, EventType, Variable> (t, EventType.READ, u, EventType.WRITE, v);
					Quintet<Thread, EventType, Thread, EventType, Variable> new_key_write_read = new Quintet<Thread, EventType, Thread, EventType, Variable> (t, EventType.WRITE, u, EventType.READ, v);
					Quintet<Thread, EventType, Thread, EventType, Variable> new_key_write_write = new Quintet<Thread, EventType, Thread, EventType, Variable> (t, EventType.WRITE, u, EventType.WRITE, v);

					HashSet<Quintet<Thread, EventType, Thread, EventType, Variable>> otherKeys = this.threadPairToAcquireInfoKeys.get(t).get(u);			
					for(Thread s: this.threads){
						for(Lock l: this.locks) {
							this.acquireInfo.get(s).get(l).addKeyToTopOfKeys(new_key_read_write, otherKeys);
							this.acquireInfo.get(s).get(l).addKeyToTopOfKeys(new_key_write_read, otherKeys);
							this.acquireInfo.get(s).get(l).addKeyToTopOfKeys(new_key_write_write, otherKeys);
						}
					}

					this.acquireInfoKeys.add(new_key_read_write);
					this.acquireInfoKeys.add(new_key_write_read);
					this.acquireInfoKeys.add(new_key_write_write);
					this.threadPairToAcquireInfoKeys.get(t).get(u).add(new_key_read_write);
					this.threadPairToAcquireInfoKeys.get(t).get(u).add(new_key_write_read);
					this.threadPairToAcquireInfoKeys.get(t).get(u).add(new_key_write_write);
					
					this.lastIdeal.put(new_key_read_write, new VectorClock(this.numThreads));
					this.lastIdeal.put(new_key_write_read, new VectorClock(this.numThreads));
					this.lastIdeal.put(new_key_write_write, new VectorClock(this.numThreads));
				}
			}
			this.variableToLockset.put(v, null);
		}
		return variableToIndex.get(v);
	}

	public void incClockThread(Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = getVectorClock(clockThread, t);
		int origVal = C_t.getClockIndex(tIndex);
		C_t.setClockIndex(tIndex, origVal + 1);
	}

	public <E> E getVectorClock(ArrayList<E> arr, Thread t) {
		int tIndex = threadToIndex.get(t);
		return getElementFrom1DArray(arr, tIndex);
	}

	public VectorClock getVectorClock(ArrayList<VectorClock> arr, Lock l) {
		int lIndex = checkAndAddLock(l);
		return getElementFrom1DArray(arr, lIndex);
	}

	public <E> E getVectorClock(ArrayList<E> arr, Variable v) {
		int vIndex = checkAndAddVariable(v);
		return getElementFrom1DArray(arr, vIndex);
	}

	public void setIndex(VectorClock vc, Thread t, int val){
		int tIndex = threadToIndex.get(t);
		vc.setClockIndex(tIndex, val);
	}

	public int getIndex(VectorClock vc, Thread t){
		int tIndex = threadToIndex.get(t);
		return vc.getClockIndex(tIndex);
	}

	public void setIndex(LockVC vc, Lock l, long val){
		int lIndex = checkAndAddLock(l);
		vc.setClockIndex(lIndex, val);
	}

	public long getIndex(LockVC vc, Lock l){
		int lIndex = lockToIndex.get(l);
		return vc.getClockIndex(lIndex);
	}

	public void addLockHeld(Thread t, Lock l) {
		HashMap<Lock, Integer> tLocks = this.threadToLockDepth.get(t);
		if(!tLocks.containsKey(l)) {
			tLocks.put(l, 0); 
		}
		tLocks.put(l, tLocks.get(l) + 1);
		this.threadToLocksHeld.get(t).add(l);
	}

	public void removeLockHeld(Thread t, Lock l) {
		HashMap<Lock, Integer> tLocks = this.threadToLockDepth.get(t);
		if(!tLocks.containsKey(l)) {
			throw new IllegalArgumentException("Lock " + l + "released by thread " + t + " without being acquired");
		}
		int tl_depth = tLocks.get(l);
		if(tl_depth <= 0) {
			throw new IllegalArgumentException("Lock " + l + "released by thread " + t + " without being acquired");
		}
		tLocks.put(l, tl_depth - 1);
		if(tl_depth == 1) {
			this.threadToLocksHeld.get(t).remove(l);
		}
	}

	public boolean updateLocksetAtAccess(Thread t, Variable x, EventType tp) {
//		this.checkAndAddVariable(x);
		HashSet<Lock> vSet = this.variableToLockset.get(x);
		HashSet<Lock> lockset = new HashSet<Lock> ();
		if(tp.isRead()) {
			lockset.add(this.readLock);
		}
		lockset.add(this.threadLock.get(t));
		lockset.addAll(this.threadToLocksHeld.get(t));
		if(vSet == null) {
			this.variableToLockset.put(x, lockset);
		}
		else {
			vSet.retainAll(lockset);
		}		
		return this.variableToLockset.get(x).isEmpty();
	}

	public void updateViewAsWriterAtAcquire(Lock l, Thread t) {
//		checkAndAddLock(l);
		int tIndex = threadToIndex.get(t);
		VectorClock C_t = this.clockThread.get(tIndex);
		int n = C_t.getClockIndex(tIndex);
		long m = this.numAcquires;
		acquireInfo.get(t).get(l).pushTop(new Triplet<Integer, Long, VectorClock>(n, m, null));
	}

	public void updateViewAsWriterAtRelease(Lock l, Thread t) {
		int tIndex = threadToIndex.get(t);
		VectorClock C_t_copy = new VectorClock(this.clockThread.get(tIndex));
		Triplet<Integer, Long, VectorClock> info = acquireInfo.get(t).get(l).top();
		Triplet<Integer, Long, VectorClock> new_info = new Triplet<Integer, Long, VectorClock>(info.first, info.second, C_t_copy);
		acquireInfo.get(t).get(l).setTop(new_info);
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

	public boolean isReEntrant(Thread t, Lock l) {
		if(this.threadToLockDepth.containsKey(t)) {
			if(this.threadToLockDepth.get(t).containsKey(l)) {
				if(this.threadToLockDepth.get(t).get(l) > 0) {
					return true;
				}
			}
		}
		return false;
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