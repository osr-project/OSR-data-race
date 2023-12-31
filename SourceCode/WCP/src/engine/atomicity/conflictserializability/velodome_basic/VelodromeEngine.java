package engine.atomicity.conflictserializability.velodome_basic;

import java.util.HashSet;

import engine.atomicity.AtomicityEngine;
import event.Thread;
import parse.ParserType;

public class VelodromeEngine extends AtomicityEngine<VelodromeState, VelodromeEvent>{

	public VelodromeEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new VelodromeState(this.threadSet, verbosity);
		handlerEvent = new VelodromeEvent();
	}

	@Override
	protected boolean skipEvent(VelodromeEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(VelodromeEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}

}
