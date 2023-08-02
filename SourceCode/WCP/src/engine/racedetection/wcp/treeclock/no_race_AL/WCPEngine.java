package engine.racedetection.wcp.treeclock.no_race_AL;

import java.util.HashMap;
import java.util.HashSet;

import engine.accesstimes.RefinedAccessTimesEngine;
import engine.racedetection.RaceDetectionEngine;
import event.Thread;
import parse.ParserType;

public class WCPEngine extends RaceDetectionEngine<WCPState, WCPEvent> {

	private HashMap<String, Long> lockEndTimes;
	private int maxStackSize;

	public WCPEngine(ParserType pType, String trace_folder, boolean forceOrdering,
			boolean tickClockOnAccess, int verbosity) {
		super(pType);
		this.threadSet = new HashSet<Thread>();
		initializeReader(trace_folder);
		this.state = new WCPState(this.threadSet, forceOrdering, tickClockOnAccess,
				verbosity);
		handlerEvent = new WCPEvent();

		boolean time_reporting = true;

		long startTimeAnalysis = 0;
		if (time_reporting) {
			startTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
		}

		RefinedAccessTimesEngine accessTimesEngine = new RefinedAccessTimesEngine(pType,
				trace_folder);
		accessTimesEngine.computeLastAccessTimes();

		if (time_reporting) {
			long stopTimeAnalysis = System.currentTimeMillis(); // System.nanoTime();
			long timeAnalysis = stopTimeAnalysis - startTimeAnalysis;
			System.out.println("Time for Phase-1 = " + timeAnalysis + " milliseconds");
		}

		lockEndTimes = accessTimesEngine.lockLast;
		// variableEndTimes = accessTimesEngine.variableLast;
		this.state = new WCPState(this.threadSet, forceOrdering, tickClockOnAccess,
				verbosity);
		this.state.view.lockThreadLastInteraction = accessTimesEngine.lockThreadLast;
		this.state.existsLockReadVariableThreads = accessTimesEngine.existsLockReadVariableThreads;
		this.state.existsLockWriteVariableThreads = accessTimesEngine.existsLockWriteVariableThreads;
		this.state.variableToReadEquivalenceClass = accessTimesEngine.variableToReadEquivalenceClass;
		this.state.variableToWriteEquivalenceClass = accessTimesEngine.variableToWriteEquivalenceClass;
		// this.state.variableToAccessedLocks =
		// accessTimesEngine.variableToAccessdLocks;
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
	protected boolean skipEvent(WCPEvent handlerEvent) {
		/*
		 * boolean skip = false; if (handlerEvent.getType().isAccessType()) { if
		 * (variableToThreadSet.get(handlerEvent.getVariable().getName()) .size() <= 1)
		 * { skip = true; } } if (handlerEvent.getType().isLockType()) { if
		 * (lockToThreadSet.get(handlerEvent.getLock().getName()).size() <= 1) { skip =
		 * true; } } return skip;
		 */
		return false;
	}

	@Override
	protected void postHandleEvent(WCPEvent handlerEvent) {
		int stackSize = state.view.getSize();
		if (maxStackSize < stackSize) {
			maxStackSize = stackSize;
		}
		if (handlerEvent.getType().isLockType()) {
			long currEventIndex = handlerEvent.getAuxId();
			long lockThreadEndIndex = this.state.view.lockThreadLastInteraction
					.get(handlerEvent.getLock().getName())
					.get(handlerEvent.getThread().getName());
			if (currEventIndex >= lockThreadEndIndex) {
				state.destroyLockThreadStack(handlerEvent.getLock(),
						handlerEvent.getThread());
			}
			// If the lock has to be deleted, it should be done at the end
			long lockEndIndex = lockEndTimes.get(handlerEvent.getLock().getName());
			if (currEventIndex >= lockEndIndex) {
				state.destroyLock(handlerEvent.getLock());
			}
		}
	}

}
