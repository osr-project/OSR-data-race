package engine.deadlock;

import java.util.HashSet;

import engine.Engine;
import event.Thread;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rr.ParseRoadRunner;
import parse.rv.ParseRVPredict;
import parse.std.ParseStandard;
import util.trace.TraceAndDataSets;

public abstract class DeadlockEngine<St extends State, DDE extends DeadlockEvent<St>> extends Engine<DDE> {

	public St state;
	protected HashSet<Thread> threadSet;

	protected long eventCount;
	protected Long deadlockCount;
	protected Long totalSkippedEvents;

	public DeadlockEngine(ParserType pType) {
		super(pType);
	}

	protected void initializeReaderRV(String trace_folder){
		rvParser = new ParseRVPredict(trace_folder, this.threadSet);
	}

	protected void initializeReaderCSV(String trace_file){
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.threadSet = traceAndDataSets.getThreadSet();
		this.trace = traceAndDataSets.getTrace();
	}
	
	protected void initializeReaderSTD(String trace_folder){
		stdParser = new ParseStandard(trace_folder, true);
		threadSet = stdParser.getThreadSet();
	}
	
	protected void initializeReaderRR(String trace_folder){
		rrParser = new ParseRoadRunner(trace_folder, true);
		threadSet = rrParser.getThreadSet();
	}

	protected boolean analyzeEvent(DDE handlerEvent, Long eventCount){
		boolean violationDetected = false;
		//TODO: Remove try catch
		try{
			violationDetected = handlerEvent.Handle(state);
		}
		catch(OutOfMemoryError oome){
			oome.printStackTrace();
			System.err.println("Number of events = " + Long.toString(eventCount));
			state.printMemory();
		}
		return violationDetected;
	}

	public void analyzeTrace(boolean multipleDeadlock){
		eventCount =  (long) 0;
		deadlockCount = (long) 0;
		totalSkippedEvents = (long) 0;
		if(this.parserType.isRV()){
			analyzeTraceRV(multipleDeadlock);		
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV(multipleDeadlock);
		}
		else if(this.parserType.isSTD()){
			analyzeTraceSTD(multipleDeadlock);
		}
		else if(this.parserType.isRR()){
			analyzeTraceRR(multipleDeadlock);
		}

		System.out.println("Analysis complete");
		System.out.println("Number of violations found = " + Long.toString(deadlockCount));
	}

	protected void analyzeTraceCSV(boolean multipleDeadlock) {
		for(eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent.copyFrom(trace.getEventAt((int)eventCount));
			if(skipEvent(handlerEvent)){
				totalSkippedEvents = totalSkippedEvents + 1;
			}
			else{
				boolean deadlockDetected = analyzeEvent(handlerEvent, eventCount);
				if(deadlockDetected){
					deadlockCount ++;
					if (!multipleDeadlock){
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}		
	}

	protected void analyzeTraceRV(boolean multipleDeadlock) {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				eventCount = eventCount + 1;
				rvParser.getNextEvent(handlerEvent);

				if(skipEvent(handlerEvent)){
					totalSkippedEvents = totalSkippedEvents + 1;
				}
				else{
//					if(handlerEvent.getId() >= 12183120 && handlerEvent.getId() <= 12189735){
//						System.out.println(handlerEvent.toCompactString());
//					}
					boolean deadlockDetected = analyzeEvent(handlerEvent, (long) eventCount);
					if(deadlockDetected){
//						System.out.println("Total Skipped = " + totalSkippedEvents);
						deadlockCount ++;
						if (!multipleDeadlock){
							break;
						}
					}
					postHandleEvent(handlerEvent);
				}
			}
		}
	}

	protected void analyzeTraceSTD(boolean multipleDeadlock) {
		while(stdParser.hasNext()){
			eventCount = eventCount + 1;
			stdParser.getNextEvent(handlerEvent);

			if(skipEvent(handlerEvent)){
				totalSkippedEvents = totalSkippedEvents + 1;
			}
			else{
				boolean deadlockDetected = analyzeEvent(handlerEvent, (long) eventCount);
				if(deadlockDetected){
					deadlockCount ++;
					if (!multipleDeadlock){
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}
	}
	
	protected void analyzeTraceRR(boolean multipleDeadlock) {
		while(rrParser.checkAndGetNext(handlerEvent)){
			eventCount = eventCount + 1;

			if(skipEvent(handlerEvent)){
				totalSkippedEvents = totalSkippedEvents + 1;
			}
			else{
				boolean deadlockDetected = analyzeEvent(handlerEvent, (long) eventCount);
				if(deadlockDetected){
					deadlockCount ++;
					if (!multipleDeadlock){
						break;
					}
				}
				postHandleEvent(handlerEvent);
			}
		}
	}

	protected abstract boolean skipEvent(DDE handlerEvent);

	protected abstract void postHandleEvent(DDE handlerEvent);

}
