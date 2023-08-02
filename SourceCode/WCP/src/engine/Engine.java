package engine;

import event.Event;
import parse.ParserType;
import parse.bin.ParseBinary;
import parse.csv.ParseCSV;
import parse.rr.ParseRoadRunner;
import parse.rv.ParseRVPredict;
import parse.std.ParseStandard;
import util.trace.Trace;
import util.trace.TraceAndDataSets;

public abstract class Engine<E extends Event> {
	protected ParserType parserType;
	protected Trace trace; // CSV
	protected ParseRVPredict rvParser;// RV
	protected ParseStandard stdParser;// STD
	protected ParseRoadRunner rrParser;// RR
	protected ParseBinary binParser;// BIN
	protected E handlerEvent;

	public Engine(ParserType pType) {
		this.parserType = pType;
	}

	protected void initializeReader(String trace_folder) {
		if (this.parserType.isRV()) {
			initializeReaderRV(trace_folder);
		} else if (this.parserType.isCSV()) {
			initializeReaderCSV(trace_folder);
		} else if (this.parserType.isSTD()) {
			initializeReaderSTD(trace_folder);
		} else if (this.parserType.isRR()) {
			initializeReaderRR(trace_folder);
		} else if (this.parserType.isBIN()) {
			initializeReaderBIN(trace_folder);
		}
	}

	protected void initializeReaderRV(String trace_folder) {
		rvParser = new ParseRVPredict(trace_folder, null);
	}

	protected void initializeReaderCSV(String trace_file) {
		TraceAndDataSets traceAndDataSets = ParseCSV.parse(true, trace_file);
		this.trace = traceAndDataSets.getTrace();
	}

	protected void initializeReaderSTD(String trace_file) {
		stdParser = new ParseStandard(trace_file);
	}

	protected void initializeReaderRR(String trace_file) {
		rrParser = new ParseRoadRunner(trace_file);
	}
	
	protected void initializeReaderBIN(String trace_file) {
		binParser = new ParseBinary(trace_file);
	}

	protected void printCompletionStatus() {
	}
}
