package engine.racedetection.dc.no_race.no_space_reduction;

import java.util.HashSet;
import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class DCEngine extends RaceDetectionEngine<DCState, DCEvent> {

	private int maxStackSize;

	public DCEngine(ParserType pType, String trace_folder, boolean forceOrdering,
			boolean tickClockOnAccess, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread>();
		initializeReader(trace_folder);
		this.state = new DCState(this.threadSet, forceOrdering, tickClockOnAccess,
				verbosity);
		handlerEvent = new DCEvent();

		this.state = new DCState(this.threadSet, forceOrdering, tickClockOnAccess,
				verbosity);
	}

	@Override
	public void analyzeTrace(boolean multipleRace) {
		eventCount = (long) 0;
		raceCount = (long) 0;
		totalSkippedEvents = (long) 0;
		maxStackSize = 0;

		if (this.parserType.isRV()) {
			analyzeTraceRV(multipleRace);
		} else if (this.parserType.isCSV()) {
			analyzeTraceCSV(multipleRace);
		} else if (this.parserType.isRR()) {
			analyzeTraceRR(multipleRace);
		} else if (this.parserType.isSTD()) {
			analyzeTraceSTD(multipleRace);
		} else {
			throw new IllegalArgumentException("Unkown parser type " + this.parserType);
		}
		System.out.println("Analysis complete");
		System.out.println("Number of races found = " + Long.toString(raceCount));
//		System.out.println("Maximum stack size = " + Integer.toString(maxStackSize));
	}

	@Override
	protected boolean skipEvent(DCEvent handlerEvent) {
		return false;
	}

	@Override
	protected void postHandleEvent(DCEvent handlerEvent) {
		int stackSize = state.view.getSize();
		if (maxStackSize < stackSize) {
			maxStackSize = stackSize;
		}
	}

}
