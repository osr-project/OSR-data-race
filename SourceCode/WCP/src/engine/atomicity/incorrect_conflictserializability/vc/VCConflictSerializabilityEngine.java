package engine.atomicity.incorrect_conflictserializability.vc;

import java.util.HashSet;

import engine.atomicity.AtomicityEngine;
import event.Thread;
import parse.ParserType;

public class VCConflictSerializabilityEngine extends AtomicityEngine<VCConflictSerializabilityState, VCConflictSerializabilityEvent>{

	public VCConflictSerializabilityEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new VCConflictSerializabilityState(this.threadSet, verbosity);
		handlerEvent = new VCConflictSerializabilityEvent();
	}

	@Override
	protected boolean skipEvent(VCConflictSerializabilityEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(VCConflictSerializabilityEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
