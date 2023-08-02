package engine.dataflow.lastwrite;

import java.util.HashMap;

import engine.dataflow.DataFlowEvent;
import event.Thread;
import event.Variable;
import util.vectorclock.VectorClock;

public class LastWriteEvent extends DataFlowEvent<LastWriteState>{

	public LastWriteEvent() {
		super();
	}

	@Override
	public void HandleSubAcquire(LastWriteState state){
	}

	@Override
	public void HandleSubRelease(LastWriteState state) {
	}
	
	@Override
	public void HandleSubRead(LastWriteState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		VectorClock C_t  = state.getVectorClock(state.threadClock, t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteClock, v);		
		C_t.updateWithMax(C_t, LW_v);
		boolean v_t_exists = false;
		if(state.firstAccessEventClock.containsKey(v)){
			if(state.firstAccessEventClock.get(v).containsKey(t)){
				v_t_exists = true;
			}
		}
		else{
			state.firstAccessEventClock.put(v, new HashMap<Thread, VectorClock> ());
		}
		if(!v_t_exists){
//			System.out.println("setting first access of [" + v.getName() + "][" + t.getName() + "] = " + C_t);
			state.firstAccessEventClock.get(v).put(t, new VectorClock(C_t));
		}
	}

	@Override
	public void HandleSubWrite(LastWriteState state) {
		Thread t = this.getThread();
		Variable v = this.getVariable();
		VectorClock C_t = state.getVectorClock(state.threadClock, t);
		VectorClock LW_v = state.getVectorClock(state.lastWriteClock, v);
		LW_v.copyFrom(C_t);
		/*
		boolean v_t_exists = false;
		if(state.firstAccessEventClock.containsKey(v)){
			if(state.firstAccessEventClock.get(v).containsKey(t)){
				v_t_exists = true;
			}
		}
		else{
			state.firstAccessEventClock.put(v, new HashMap<Thread, VectorClock> ());
		}
		if(!v_t_exists){
			System.out.println("setting first access of [" + v.getName() + "][" + t.getName() + "] = " + C_t);
			state.firstAccessEventClock.get(v).put(t, new VectorClock(C_t));
		}
		*/
		state.incThreadClockLocally(getThread());
	}

	@Override
	public void HandleSubFork(LastWriteState state) {
	}

	@Override
	public void HandleSubJoin(LastWriteState state) {
	}

	@Override
	public void HandleSubBegin(LastWriteState state) {
	}

	@Override
	public void HandleSubEnd(LastWriteState state) {
	}
	

	@Override
	public void HandleSubBranch(LastWriteState state) {
		VectorClock C_t = state.getVectorClock(state.threadClock, this.getThread());
		VectorClock B = state.branchClock;
		B.updateWithMax(B, C_t);
	}
}
