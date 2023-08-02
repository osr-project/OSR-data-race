package engine.racedetection;

import event.Event;

public abstract class RaceDetectionEvent<S extends State> extends Event {
	public RaceDetectionEvent(){
		super();
	}
	
	public void copyFrom(Event fromEvent){
		super.copyFrom(fromEvent);
	}
	
	public void printRaceInfo(S state){
		if(this.getType().isLockType())  this.printRaceInfoLockType(state);
		if(this.getType().isAccessType()) this.printRaceInfoAccessType(state);
		if(this.getType().isExtremeType()) this.printRaceInfoExtremeType(state);
	}
	
	public abstract boolean Handle(S state);
	
	public boolean HandleSub(S state){
		boolean raceDetected = false;

		if(this.getType().isAcquire()) 	raceDetected = this.HandleSubAcquire(state);
		if(this.getType().isRelease()) 	raceDetected = this.HandleSubRelease(state);
		if(this.getType().isRead())		raceDetected = this.HandleSubRead(state);
		if(this.getType().isWrite())	raceDetected = this.HandleSubWrite(state);
		if(this.getType().isFork()) 	raceDetected = this.HandleSubFork(state);
		if(this.getType().isJoin())		raceDetected = this.HandleSubJoin(state);

		return raceDetected;
	}
	
	public abstract void printRaceInfoLockType(S state);
	public abstract void printRaceInfoAccessType(S state);
	public abstract void printRaceInfoExtremeType(S state);
	
	public abstract boolean HandleSubAcquire(S state);
	public abstract boolean HandleSubRelease(S state);
	public abstract boolean HandleSubRead(S state);
	public abstract boolean HandleSubWrite(S state);
	public abstract boolean HandleSubFork(S state);
	public abstract boolean HandleSubJoin(S state);

}
