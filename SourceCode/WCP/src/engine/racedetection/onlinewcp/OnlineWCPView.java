package engine.racedetection.onlinewcp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ListIterator;

import event.Lock;
import event.Thread;
import util.vectorclock.ClockPair;
import util.vectorclock.VectorClock;

public class OnlineWCPView {

	private int numLocks;
	private int numThreads;
	private ArrayList<ArrayList<Store>> view; // (WRITER, LOCK) -> Store
	private ArrayList<ArrayList<ArrayList<VectorClock>>> readerStackBottomPointer; //(WRITER, LOCK) -> (READER -> VectorClock)
	private ArrayList<ArrayList<ArrayList<Boolean>>> readerStackEmpty; //(WRITER, LOCK) -> (READER -> Boolean)
	private HashMap<Thread, Integer> threadToIndex;
	private HashMap<Lock, Integer> lockToIndex;
	
	private VectorClock tempMin;

	OnlineWCPView(HashSet<Thread> tSet) {
		this.threadToIndex = new HashMap<Thread, Integer>();
		this.numThreads = 0;
		Iterator<Thread> tIter = tSet.iterator();
		while (tIter.hasNext()) {
			Thread thread = tIter.next();
			//System.out.println("Adding thread to map " + thread.toString());
			this.threadToIndex.put(thread, (Integer)this.numThreads);
			this.numThreads ++;
		}
		
		this.numLocks = 0;
		this.lockToIndex = new HashMap<Lock, Integer> ();
		
		this.view = new ArrayList<ArrayList<Store>>();
		for(int i = 0; i < this.numThreads; i++){
			view.add(new ArrayList<Store> ());
		}
		
		this.readerStackBottomPointer = new ArrayList<ArrayList<ArrayList<VectorClock>>>();
		for(int i = 0; i < this.numThreads; i++){
			readerStackBottomPointer.add(new ArrayList<ArrayList<VectorClock>> ());
		}
		
		this.readerStackEmpty = new ArrayList<ArrayList<ArrayList<Boolean>>>();
		for(int i = 0; i < this.numThreads; i++){
			readerStackEmpty.add(new ArrayList<ArrayList<Boolean>> ());
		}
		
		this.tempMin = new VectorClock(this.numThreads) ;
	}
	
	public int checkAndAddLock(Lock l){
		if(!lockToIndex.containsKey(l)){
			lockToIndex.put(l, this.numLocks);
			this.numLocks ++;
			
			for (int i = 0; i < this.numThreads; i++) {
				view.get(i).add(new Store(this.numThreads));
			}
			
			for (int i = 0; i < this.numThreads; i++) {
				readerStackBottomPointer.get(i).add(new ArrayList<VectorClock> ());
				readerStackEmpty.get(i).add(new ArrayList<Boolean> ());
				int lIndex = this.numLocks - 1;
				for(int j = 0; j < this.numThreads; j ++){
					readerStackBottomPointer.get(i).get(lIndex).add(new VectorClock(this.numThreads));
					readerStackEmpty.get(i).get(lIndex).add((Boolean)true);
				}
			}
		}
		return lockToIndex.get(l);
	}
	
	/*
	public int checkAndAddThread(Thread t){
		if(!threadToIndex.containsKey(t)){
			//System.out.println("View: Adding tIndex " + this.numThreads);
			threadToIndex.put(t, this.numThreads);
			this.numThreads ++;
			
			view.add(new ArrayList<ArrayList<Store>> ());
			for(int i = 0; i < this.numThreads; i ++){
				while(view.get(i).size() < this.numThreads){
					view.get(i).add(new ArrayList<Store> ());
				}
				for(int j = 0; j < this.numThreads; j ++){
					while(view.get(i).get(j).size() < this.numLocks){
						view.get(i).get(j).add(new Store(this.totThreads, true));
					}
				}
			}
		}
		return threadToIndex.get(t);
	}
	*/

	public void pushClockPair(Thread t, Lock l, ClockPair clockPair) {
		int tIndex = threadToIndex.get(t);
		int lIndex = checkAndAddLock(l);
		
		//System.out.println("(t1, t2, l) = (" + t1Index + ", " + t2Index + ", " + lIndex + ")" );
		
		Store st = this.view.get(tIndex).get(lIndex);
		st.pushTop(clockPair);
		for(int i = 0; i < this.numThreads; i ++){
			if(this.readerStackEmpty.get(tIndex).get(lIndex).get(i)){
				this.readerStackEmpty.get(tIndex).get(lIndex).set(i, (Boolean)false);
				//this.readerStackBottomPointer.get(tIndex).get(lIndex).set(i, clockPair.getAcquire());
				this.readerStackBottomPointer.get(tIndex).get(lIndex).get(i).copyFrom(clockPair.getAcquire());
			}
		}
	}
	
	private boolean updateTempWithMinClockStore(Thread t, Lock l){
		int tIndex = threadToIndex.get(t);
		int lIndex = checkAndAddLock(l);
		this.tempMin.setToZero();
		boolean atLeastOne = false;
		for(int i = 0; i < this.numThreads; i ++){
			if( ! this.readerStackEmpty.get(tIndex).get(lIndex).get(i)){
				VectorClock vc = this.readerStackBottomPointer.get(tIndex).get(lIndex).get(i);
				if(! atLeastOne){
					atLeastOne = true;
					tempMin.copyFrom(vc);
				}
				else{
					tempMin.updateWithMin(tempMin, vc);
				}
			}
		}
		return atLeastOne;
	}
	
	private void updateStoreToMatchBottomWithMin(Thread t, Lock l){
		int tIndex = threadToIndex.get(t);
		int lIndex = checkAndAddLock(l);
		Store st = this.view.get(tIndex).get(lIndex);
		int sz = st.getLength();
		if(sz > 0){
			boolean atLeastOne = this.updateTempWithMinClockStore(t, l); 
			if(atLeastOne){
				//Empty stack until tempMin
				VectorClock acquireBottom = st.bottom().getAcquire();
				while(! acquireBottom.isEqual(tempMin)){
					st.removeBottom();
					acquireBottom = st.bottom().getAcquire();
				}
			}
			else{
				while(st.getLength() > 0){
					st.removeBottom();
				}
			}
		}
	}

	// result is going to be overwritten with the release of the largest acq <= ct
	public void getMaxLowerBound(Thread t1, Thread t2, Lock l, VectorClock ct, VectorClock result) {
		int t1Index = threadToIndex.get(t1);
		int t2Index = threadToIndex.get(t2);
		int lIndex = checkAndAddLock(l);
		Store st = this.view.get(t1Index).get(lIndex);
		result.setToZero();
		ClockPair clockPair = null;
		boolean pairFound = false;
		ListIterator<ClockPair> lIter = null;
		
		if(! this.readerStackEmpty.get(t1Index).get(lIndex).get(t2Index)){
			//This was a BUG !
			int index = this.view.get(t1Index).get(lIndex).getIndexOfAcquire( readerStackBottomPointer.get(t1Index).get(lIndex).get(t2Index));
			if(index >= 0){
				lIter = st.getIterator(index);
				while(lIter.hasNext()){
					clockPair = lIter.next();
					VectorClock acquireClock = clockPair.getAcquire();
					if (acquireClock.isLessThanOrEqual(ct)) {
						pairFound = true;
					} else {
						break;
					}
				}
			}
		}
		
		if(pairFound){
			result.copyFrom(clockPair.getRelease());
			if(lIter.hasNext()){
				this.readerStackBottomPointer.get(t1Index).get(lIndex).get(t2Index).copyFrom(lIter.next().getAcquire());
			}
			else{
				this.readerStackEmpty.get(t1Index).get(lIndex).set(t2Index,true);
			}
			this.updateStoreToMatchBottomWithMin(t1, l);
		}
	}
	
	public void updateTopRelease(Thread t, Lock l, VectorClock ct, VectorClock ht) {
		int tIndex = threadToIndex.get(t);
		int lIndex = checkAndAddLock(l);
		//System.out.println("(t1, t2, l) = (" + t1Index + ", " + t2Index + ", " + lIndex + ")" );
		ClockPair clockPair = this.view.get(tIndex).get(lIndex).top();
		clockPair.getRelease().updateWithMax(ct, ht);
	}
	
	public String toString(){
		String str = "";
		for(int i = 0; i < this.numThreads; i ++){
			for(int j = 0; j < this.numLocks; j ++){
				str += "[" + i + "]";
				str += "[" + j + "]";
				str += " : " + view.get(i).get(j).toString() + "\n";
			}
		}
		str += "\n";
		return str;
	}
	
	public int getSize(){
		int sz = 0;
		for(int i = 0; i < this.numThreads; i ++){
			for(int j = 0; j < this.numLocks; j ++){
				sz += view.get(i).get(j).getLength();
			}
		}
		return sz;
	}
	
	public void printSize(){
		System.err.println("Stack size = " + Integer.toString(this.getSize()));
	}

}