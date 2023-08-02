package engine.accesstimes;

import java.util.HashMap;
import java.util.HashSet;
import engine.Engine;
import event.Event;
import parse.ParserType;

public class AccessTimesEngine extends Engine<Event> {
	
	public HashMap<String, HashSet<String>> variableToThreadSet;
	public HashSet<String> variablesWritten;
	public HashMap<String, HashSet<String>> lockToThreadSet;
	
 	public AccessTimesEngine(ParserType pType, String trace_folder) {
 		
		super(pType);
		initializeReader(trace_folder);
		handlerEvent = new Event();
		
		variableToThreadSet = new HashMap<String, HashSet<String>>();
		variablesWritten = new HashSet<String> ();
		lockToThreadSet = new HashMap<String, HashSet<String>>();
	}
	
	public void computeLastAccessTimes() {
		if(this.parserType.isRV()){
			computeLastAccessTimesRV();
		}
		else if(this.parserType.isCSV()){
			computeLastAccessTimesCSV();
		}
		else if(this.parserType.isSTD()){
			computeLastAccessTimesSTD();
		}
		else if(this.parserType.isRR()){
			computeLastAccessTimesRR();
		}
	}
	
	private void processEvent(){
		if(handlerEvent.getType().isLockType()){
			if(!lockToThreadSet.containsKey(handlerEvent.getLock().getName())){
				lockToThreadSet.put(handlerEvent.getLock().getName(), new HashSet<String>());
			}
			lockToThreadSet.get(handlerEvent.getLock().getName()).add(handlerEvent.getThread().getName());
		}
		if(handlerEvent.getType().isAccessType()){
			if(!variableToThreadSet.containsKey(handlerEvent.getVariable().getName())){
				variableToThreadSet.put(handlerEvent.getVariable().getName(), new HashSet<String>());
			}
			variableToThreadSet.get(handlerEvent.getVariable().getName()).add(handlerEvent.getThread().getName());
			if(handlerEvent.getType().isWrite()) {
				variablesWritten.add(handlerEvent.getVariable().getName());
			}
		}
	}
	
	private void computeLastAccessTimesRV() {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				processEvent();
			}
		}
	}
	
	private void computeLastAccessTimesCSV() {
		for(int eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent = trace.getEventAt(eventCount);
			processEvent();
		}
	}

	private void computeLastAccessTimesSTD() {
		while(stdParser.hasNext()){
			stdParser.getNextEvent(handlerEvent);
			processEvent();
		}
	}
	
	private void computeLastAccessTimesRR() {
		while(rrParser.checkAndGetNext(handlerEvent)){
			processEvent();
		}
	}

}
