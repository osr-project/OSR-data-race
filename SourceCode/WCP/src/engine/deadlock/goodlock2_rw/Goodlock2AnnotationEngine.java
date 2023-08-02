package engine.deadlock.goodlock2_rw;

import java.util.HashMap;
import java.util.HashSet;

import engine.Engine;
import event.Event;
import parse.ParserType;
import parse.csv.ParseCSV;
import parse.rv.ParseRVPredict;
import util.trace.TraceAndDataSets;

public class Goodlock2AnnotationEngine extends Engine<Event> {

	public HashMap<String, HashSet<Long>> specialWrites;
	private HashMap<String, Long> eventIdxOfLastWrite;
	
	public Goodlock2AnnotationEngine(ParserType pType, String trace_folder) {
		super(pType);
		initializeReader(trace_folder);
		handlerEvent = new Event();
		specialWrites = new HashMap<String, HashSet<Long>> () ;
		eventIdxOfLastWrite = new HashMap<String, Long> ();
	}

	protected void initializeReaderRV(String trace_folder){
		rvParser = new ParseRVPredict(trace_folder, null);
	}

	protected void initializeReaderCSV(String trace_file){
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.trace = traceAndDataSets.getTrace();
	}

	private void processEvent(){
		long eventIndex = handlerEvent.getAuxId();

		if(handlerEvent.getType().isRead()){
			String varName = handlerEvent.getVariable().getName();
			if(! specialWrites.containsKey(varName)){
				specialWrites.put(varName, new HashSet<Long> ());
			}
			if(eventIdxOfLastWrite.containsKey(varName)){
				specialWrites.get(varName).add(eventIdxOfLastWrite.get(varName));
			}
		}

		if(handlerEvent.getType().isWrite()){
			String varName = handlerEvent.getVariable().getName();
			eventIdxOfLastWrite.put(varName, eventIndex);
		}
	}

	public void analyzeTrace(){
		if(this.parserType.isRV()){
			analyzeTraceRV();
		}
		else if(this.parserType.isCSV()){
			analyzeTraceCSV();
		}
	}

	private void analyzeTraceCSV() {
		for(int eventCount = 0; eventCount < trace.getSize(); eventCount ++){
			handlerEvent = trace.getEventAt(eventCount);
			processEvent();
		}
	}

	private void analyzeTraceRV() {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				processEvent();
			}
		}
	}
}
