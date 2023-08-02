package engine.deadlock;

import event.Event;

public abstract class DeadlockEvent<S extends State> extends Event {
	public DeadlockEvent(){
		super();
	}
	
	public void copyFrom(Event fromEvent){
		super.copyFrom(fromEvent);
	}
	
	public void printDeadlockInfo(S state){
		if(this.getType().isLockType())  this.printDeadlockInfoLockType(state);
		if(this.getType().isAccessType()) this.printDeadlockInfoAccessType(state);
		if(this.getType().isExtremeType()) this.printDeadlockInfoExtremeType(state);
		if(this.getType().isTransactionType()) this.printDeadlockInfoTransactionType(state);
	}
	
//	public abstract boolean Handle(S state);
	public boolean Handle(S state) {
		return this.HandleSub(state);
	}
	
	public boolean HandleSub(S state){
		switch (this.getType()) {
			case ACQUIRE: return this.HandleSubAcquire(state);
			case RELEASE: return this.HandleSubRelease(state);
			case READ: return this.HandleSubRead(state);
			case WRITE: return this.HandleSubWrite(state);
			case FORK: return this.HandleSubFork(state);
			case JOIN: return this.HandleSubJoin(state);
			case BEGIN: return this.HandleSubBegin(state);
			case END: return this.HandleSubEnd(state);
			case BRANCH: return this.HandleSubBranch(state);
			default: throw new IllegalArgumentException("unknown event type");
		}
	}

	public abstract void printDeadlockInfoLockType(S state);
	public abstract void printDeadlockInfoAccessType(S state);
	public abstract void printDeadlockInfoExtremeType(S state);
	public abstract void printDeadlockInfoTransactionType(S state);
	
	public abstract boolean HandleSubAcquire(S state);
	public abstract boolean HandleSubRelease(S state);
	public abstract boolean HandleSubRead(S state);
	public abstract boolean HandleSubWrite(S state);
	public abstract boolean HandleSubFork(S state);
	public abstract boolean HandleSubJoin(S state);
	public abstract boolean HandleSubBegin(S state);
	public abstract boolean HandleSubEnd(S state);
	public abstract boolean HandleSubBranch(S state);

}
