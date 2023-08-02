package engine.racedetection.lockset;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEvent;
import event.Lock;

public class LockSetEvent extends RaceDetectionEvent<LockSetState> {

	@Override
	public boolean Handle(LockSetState state) {
		return this.HandleSub(state);
	}

	@Override
	public void printRaceInfoLockType(LockSetState state) {
		System.out.println("Dummy method called");		
	}

	@Override
	public void printRaceInfoAccessType(LockSetState state) {
		System.out.println("Dummy method called");	
	}

	@Override
	public void printRaceInfoExtremeType(LockSetState state) {
		System.out.println("Dummy method called");
	}

	@Override
	public boolean HandleSubAcquire(LockSetState state) {
		int l = 1;
		if(state.locksHeldNesting.get(this.getThread()).containsKey(this.getLock())){
			l = 1 + state.locksHeldNesting.get(this.getThread()).get(this.getLock());
		}
		state.locksHeldNesting.get(this.getThread()).put(this.getLock(), l);
		state.locksHeldSet.get(this.getThread()).add(this.getLock());
		return false;
	}

	@Override
	public boolean HandleSubRelease(LockSetState state) {
		int l;
		if(state.locksHeldNesting.get(this.getThread()).containsKey(this.getLock())){
			l = state.locksHeldNesting.get(this.getThread()).get(this.getLock()) - 1;
		}
		else{
			throw new IllegalArgumentException("Thread " + this.getThread().getName() + " is releasing lock " + this.getLock().getName() + " without acquiring it enough number of times .");
		}
		state.locksHeldNesting.get(this.getThread()).put(this.getLock(), l);
		if(l == 0){
			state.locksHeldSet.get(this.getThread()).remove(this.getLock());
		}
		return false;
	}

	@Override
	public boolean HandleSubRead(LockSetState state) {
		boolean raceDetected = false;
		if(!state.lockSet.containsKey(this.getVariable())){
			state.lockSet.put(getVariable(), new HashSet<Lock> (state.locksHeldSet.get(this.getThread())));
			state.lockSet.get(this.getVariable()).add(state.dummyReadLock);
		}
		if(state.lockSet.get(this.getVariable()).contains(state.dummyReadLock)){
			state.lockSet.get(this.getVariable()).retainAll(state.locksHeldSet.get(this.getThread()));
			state.lockSet.get(this.getVariable()).add(state.dummyReadLock);
		}
		else{
			state.lockSet.get(this.getVariable()).retainAll(state.locksHeldSet.get(this.getThread()));
		}
		if(state.lockSet.get(this.getVariable()).isEmpty()){
			raceDetected = true;
		}
		if(raceDetected){
			System.out.println("LockSet discipline violated on variable " + this.getVariable().getName());
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubWrite(LockSetState state) {
		boolean raceDetected = false;
		if(!state.lockSet.containsKey(this.getVariable())){
			state.lockSet.put(getVariable(), new HashSet<Lock> (state.locksHeldSet.get(this.getThread())));
		}
		state.lockSet.get(this.getVariable()).retainAll(state.locksHeldSet.get(this.getThread()));
		if(state.lockSet.get(this.getVariable()).isEmpty()){
			raceDetected = true;
		}
		if(raceDetected){
			System.out.println("LockSet discipline violated on variable " + this.getVariable().getName());
		}
		return raceDetected;
	}

	@Override
	public boolean HandleSubFork(LockSetState state) {
		return false;
	}

	@Override
	public boolean HandleSubJoin(LockSetState state) {
		return false;
	}

}
