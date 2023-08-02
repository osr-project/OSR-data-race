package engine.print;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import engine.Engine;
import engine.accesstimes.orderedvars.OrderedVarsEngine;
import event.Event;
import event.Thread;
import event.Lock;
import event.Variable;
import parse.ParserType;

public class PrintEngine extends Engine<Event> {

	long event_index;
	HashMap<Thread, HashMap<Variable, Long>> readMap;
	HashMap<Thread, HashMap<Variable, Long>> writeMap;
	HashMap<Thread, HashMap<Lock, Long>> acquireMap;
	HashMap<Thread, HashMap<Lock, Long>> releaseMap;
	HashMap<Thread, HashMap<Thread, Long>> forkMap;
	HashMap<Thread, HashMap<Thread, Long>> joinMap;
	ArrayList<String> eventStrings;

	private HashMap<String, HashSet<String>> lockToThreadSet;
	private HashSet<String> orderedVariables;

	private boolean removeThreadLocalEvents;

	public PrintEngine(ParserType pType, String trace_folder, boolean th) {

		super(pType);
		initializeReader(trace_folder);
		handlerEvent = new Event();
		event_index = 0;

		this.removeThreadLocalEvents = th;

		readMap = new HashMap<Thread, HashMap<Variable, Long>> ();
		writeMap = new HashMap<Thread, HashMap<Variable, Long>> ();
		acquireMap = new HashMap<Thread, HashMap<Lock, Long>> ();
		releaseMap = new HashMap<Thread, HashMap<Lock, Long>> ();
		forkMap = new HashMap<Thread, HashMap<Thread, Long>> ();
		joinMap = new HashMap<Thread, HashMap<Thread, Long>> ();
		eventStrings = new ArrayList<String> ();
		
		OrderedVarsEngine orderedVarsEngine = new OrderedVarsEngine(pType, trace_folder, 0);
		orderedVarsEngine.analyzeTrace(true);
		
		this.lockToThreadSet = orderedVarsEngine.getLockToThreadSet();
		this.orderedVariables = orderedVarsEngine.getOrdredVars();
	}

	public void analyzeTrace() {
		if(this.parserType.isRV()){
			analyzeTraceRV();
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV();
		}
		else if(this.parserType.isSTD()){
			analyzeTraceSTD();
		}
		else if(this.parserType.isRR()){
			analyzeTraceRR();
		}
		else if(this.parserType.isBIN()){
			analyzeTraceBIN();
		}
	}

	private void analyzeTraceRV() {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				if(! skipEvent(handlerEvent)){
					processEvent();
				}
//				else{
//					System.out.println("Skipping " + handlerEvent.toCompactString());
//				}
			}
//						System.out.println(event_index);
		}
	}

	private void analyzeTraceCSV() {
		for(int eCount = 0; eCount < trace.getSize(); eCount ++){
			handlerEvent = trace.getEventAt(eCount);
			if(! skipEvent(handlerEvent)){
				System.out.println("Not Skipping " + handlerEvent.toCompactString());
				processEvent();
			}
			
		}
	}
	
	private void analyzeTraceSTD() {
		while(stdParser.hasNext()){
			stdParser.getNextEvent(handlerEvent);
			if(! skipEvent(handlerEvent)){
				processEvent();
			}
		}
	}
	
	private void analyzeTraceBIN() {
		while(binParser.hasNext()){
			binParser.getNextEvent(handlerEvent);
			if(! skipEvent(handlerEvent)){
				processEvent();
			}
		}
	}
	
	private void analyzeTraceRR() {
		while(rrParser.checkAndGetNext(handlerEvent)){
			if(!skipEvent(handlerEvent)){
				processEvent();
			}
		}
	}
	
	private  boolean skipEvent(Event handlerEvent){
		if(!this.removeThreadLocalEvents) {
			return false;
		}
		if(handlerEvent.getType().isAccessType()){
			String var_name = handlerEvent.getVariable().getName();
			if(this.orderedVariables.contains(var_name)) {
				return true;
			}
		}
		else if(handlerEvent.getType().isLockType()){
			if(lockToThreadSet.get(handlerEvent.getLock().getName()).size() <= 1 ){
				return true;
			}
		}
		return false;	
	}

	private void processEvent(){
		System.out.println(handlerEvent.toStandardFormat());
	}

}
