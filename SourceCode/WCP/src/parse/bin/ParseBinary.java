package parse.bin;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;

import event.Event;
import event.EventType;
import event.Lock;
import event.Thread;
import event.Variable;

public class ParseBinary {
	private HashMap<Integer, Thread> threadMap;
	private HashMap<Integer, Lock> lockMap;
	private HashMap<Integer, Variable> variableMap;
	
	DataInputStream dataStream;
	long event_numeric;
	
	public int numThreads;
	public int numLocks;
	public int numVariables;
	public long numEvents;

	public ParseBinary(String traceFile){
		threadMap = new HashMap<Integer, Thread>();
		lockMap = new HashMap<Integer, Lock>();
		variableMap = new HashMap<Integer, Variable>();
		
		numThreads = 0;
		numEvents = 0;
		numVariables = 0;
		numEvents = 0L;

		dataStream = null;
		try{
			dataStream = new DataInputStream(new FileInputStream(traceFile));
			
			numThreads = dataStream.readShort() & BinaryFormat.NUMBER_OF_THREADS_MASK;
			numLocks = dataStream.readInt() & BinaryFormat.NUMBER_OF_LOCKS_MASK;
			numVariables = dataStream.readInt() & BinaryFormat.NUMBER_OF_VARS_MASK;
			numEvents = dataStream.readLong() & BinaryFormat.NUMBER_OF_EVENTS_MASK;
		}
		catch (FileNotFoundException ex) {
			System.err.println("Unable to open file '" + traceFile + "'. Exiting ...");
			System.exit(0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		event_numeric = -1;
	}
	
	public HashSet<Thread> getThreadSet(){
		return new HashSet<Thread> (this.threadMap.values());
	}

	public void numeric2Event(Event e) {
		int tid = (int) ((event_numeric & BinaryFormat.THREAD_MASK) >> BinaryFormat.THREAD_BIT_OFFSET);
		short op = (short) ((event_numeric & BinaryFormat.OP_MASK) >> BinaryFormat.OP_BIT_OFFSET);
		int decor = (int) ((event_numeric & BinaryFormat.DECOR_MASK) >> BinaryFormat.DECOR_BIT_OFFSET);
		int LID = (int) ((event_numeric & BinaryFormat.LOC_MASK) >> BinaryFormat.LOC_BIT_OFFSET);
		
		if (!(threadMap.containsKey(tid))) {
			threadMap.put(tid, new Thread(tid));
		}
		Thread t = threadMap.get(tid);

		final String ename = null;
		
		EventType tp = BinaryFormat.getBinaryEventType(op);

		if (tp.isAccessType()) {
			if (!(variableMap.containsKey(decor))) {
				variableMap.put(decor, new Variable(decor));
			}
			Variable v = variableMap.get(decor);
			e.updateEvent(0L, LID, ename, tp, t, null, v, null);
		}

		else if (tp.isLockType()) {
			if (!(lockMap.containsKey(decor))) {
				lockMap.put(decor, new Lock(decor));
			}
			Lock l = lockMap.get(decor);

			e.updateEvent(0L, LID, ename, tp, t, l, null, null);
		}

		else if (tp.isExtremeType()) {
			if (!(threadMap.containsKey(decor))) {
				threadMap.put(decor, new Thread(decor));
			}
			Thread target = threadMap.get(decor);

			e.updateEvent(0L, LID, ename, tp, t, null, null, target);
		}
		
		else if (tp.isTransactionType()) {
			e.updateEvent(0L, LID, ename, tp, t, null, null, null);
		}
		
		else if (tp.isBranch()) {
			e.updateEvent(0L, LID, ename, tp, t, null, null, null);
		}		
	}

	public void getNextEvent(Event e){ //e is supposed to be over-written (deep copy) by the event-generated from the line read
		numeric2Event(e);
	}

	public boolean hasNext(){
		boolean endOfFile = false;
		try {
			event_numeric = dataStream.readLong();
		} catch (Exception ex) {
			try {
				dataStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			endOfFile = true;
		}
		return !endOfFile;
	}
	
	public static void demo(){
		String traceFile = "/Users/umang/Desktop/fibonacci.dat";
		Event e = new Event();
		ParseBinary parser = new ParseBinary(traceFile);
		while(parser.hasNext()){
			parser.getNextEvent(e);
			System.out.println(e.toCompactString());
		}
	}
	
	public static void main(String args[]){
		demo();
	}

}
