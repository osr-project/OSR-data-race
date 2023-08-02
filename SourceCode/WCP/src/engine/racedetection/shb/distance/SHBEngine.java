package engine.racedetection.shb.distance;

import java.util.HashSet;

import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class SHBEngine extends RaceDetectionEngine<SHBState, SHBEvent>{

	public SHBEngine(ParserType pType, String trace_folder, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread> ();
		initializeReader(trace_folder);
		this.state = new SHBState(this.threadSet, verbosity);
		handlerEvent = new SHBEvent();
	}

	@Override
	protected boolean skipEvent(SHBEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(SHBEvent handlerEvent) {	
//		if(handlerEvent.getType().isAccessType()){
//			if(state.verbosity == 1 || state.verbosity == 2){
//				System.out.println();
//			}	
//		}
	}
	
	@Override
	protected void printCompletionStatus() {
		super.printCompletionStatus();
		if(enablePrintStatus) {
			System.out.println("Num races = " + state.numRaces);
			System.out.println("Max MaxRace distance = " + state.maxMaxDistance);
			double avg_d_max = ((double) state.sumMaxDistance) / ((double) state.numRaces);
			System.out.println("Avg. MaxRace distance = " + avg_d_max);
			System.out.println("Max MinRace distance = " + state.maxMinDistance);
			double avg_d_min = ((double) state.sumMinDistance) / ((double) state.numRaces);
			System.out.println("Avg. MinRace distance = " + avg_d_min);
		}
	}


}
