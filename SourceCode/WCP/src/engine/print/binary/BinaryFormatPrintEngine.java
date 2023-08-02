package engine.print.binary;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import engine.Engine;
import engine.accesstimes.orderedvars.OrderedVarsEngine;
import event.Event;
import parse.ParserType;

public class BinaryFormatPrintEngine extends Engine<Event> {
	
	DataOutputStream os;
	
	private HashMap<String, HashSet<String>> lockToThreadSet;
	private HashSet<String> orderedVariables;

	private boolean removeThreadLocalEvents;
	
	public short numThreads;
	public int numLocks;
	public int numVariables;
	public long numEvents;

	public BinaryFormatPrintEngine(ParserType pType, String trace_folder, boolean th, String output_path) {

		super(pType);

		initializeReader(trace_folder);
		handlerEvent = new Event();

		this.removeThreadLocalEvents = th;
		
		OrderedVarsEngine orderedVarsEngine = new OrderedVarsEngine(pType, trace_folder, 0);
		orderedVarsEngine.analyzeTrace(true);
		
		this.lockToThreadSet = orderedVarsEngine.getLockToThreadSet();
		this.orderedVariables = orderedVarsEngine.getOrdredVars();
		
		numThreads = (short) orderedVarsEngine.getNumThreads();
		numLocks = orderedVarsEngine.getLockToThreadSet().keySet().size();
		numVariables = orderedVarsEngine.getVariableToThreadSet().keySet().size();
		numEvents = orderedVarsEngine.numEvents;
		
		File output_file = new File(output_path);
		try {
			output_file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			os = new DataOutputStream(new FileOutputStream(output_file));
			
			os.writeShort(numThreads);
			os.writeInt(numLocks);
			os.writeInt(numVariables);
			os.writeLong(numEvents);
			
		} catch (FileNotFoundException e) {
			System.err.println("File does not exist : " + output_path);
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("Unable to write to stream");
			e.printStackTrace();
		}
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
		try {
			os.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void analyzeTraceRV() {
		if(rvParser.pathListNotNull()){
			while(rvParser.hasNext()){
				rvParser.getNextEvent(handlerEvent);
				if(! skipEvent(handlerEvent)){
					processEvent();
				}
			}
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
	
	private void analyzeTraceRR() {
		while(rrParser.checkAndGetNext(handlerEvent)){
			if(!skipEvent(handlerEvent)){
				processEvent();
			}
		}
	}
	
	private void analyzeTraceBIN() {
		while(binParser.hasNext()){
			binParser.getNextEvent(handlerEvent);
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
		try {
			os.writeLong(handlerEvent.toBinaryFormat());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
