package engine.dataflow;

import event.Event;

public abstract class DataFlowEvent<S extends State> extends Event {
	public DataFlowEvent(){
		super();
	}
	
	public void copyFrom(Event fromEvent){
		super.copyFrom(fromEvent);
	}
	
	public void Handle(S state) {
		switch (this.getType()) {
			case ACQUIRE: 
				this.HandleSubAcquire(state); 
				break;
			case RELEASE: 
				this.HandleSubRelease(state); 
				break;
			case READ: 
				this.HandleSubRead(state);
				break;
			case WRITE: 
				this.HandleSubWrite(state);
				break;
			case FORK: 
				this.HandleSubFork(state);
				break;
			case JOIN: 
				this.HandleSubJoin(state);
				break;
			case BEGIN: 
				this.HandleSubBegin(state);
				break;
			case END: 
				this.HandleSubEnd(state);
				break;
			case BRANCH: 
				this.HandleSubBranch(state);
				break;
			default: throw new IllegalArgumentException("unknown event type " + this.getType());
		}
	}
	
	public abstract void HandleSubAcquire(S state);
	public abstract void HandleSubRelease(S state);
	public abstract void HandleSubRead(S state);
	public abstract void HandleSubWrite(S state);
	public abstract void HandleSubFork(S state);
	public abstract void HandleSubJoin(S state);
	public abstract void HandleSubBegin(S state);
	public abstract void HandleSubEnd(S state);
	public abstract void HandleSubBranch(S state);

}
