package parse.rv;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import event.Event;
import event.EventType;
import event.Lock;
import event.Thread;
import event.Variable;
import rvparse.RVEvent;
import rvparse.RVEventReader;
import util.trace.Trace;
import util.trace.TraceAndDataSets;

public class Parse {

	public static HashMap<String, Thread> threadMap = new HashMap<String, Thread>();
	public static HashMap<String, Lock> lockMap = new HashMap<String, Lock>();
	public static HashMap<String, Variable> variableMap = new HashMap<String, Variable>();

	public static HashMap<Variable, HashSet<Thread>> threads_for_var = new HashMap<Variable, HashSet<Thread>>();
	public static HashMap<Variable, HashSet<Thread>> threads_for_readvar = new HashMap<Variable, HashSet<Thread>>();
	public static HashMap<Variable, HashSet<Thread>> threads_for_writevar = new HashMap<Variable, HashSet<Thread>>();

	public static HashSet<Variable> retainedVariables = new HashSet<Variable>();
	public static HashSet<Variable> discardedVariables = new HashSet<Variable>();

	public static HashSet<Thread> threadSet = new HashSet<Thread>();
	public static HashSet<Lock> lockSet = new HashSet<Lock>();

	public static ArrayList<Path> listSourceFiles(Path dir) throws IOException {
		ArrayList<Path> result = new ArrayList<Path>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*_trace.bin")) {
			for (Path entry : stream) {
				result.add(entry);
			}
		} catch (DirectoryIteratorException ex) {
			// I/O error encountered during the iteration, the cause is an
			// IOException
			throw ex.getCause();
		}
		return result;
	}

	public static void updateThreadsForReadVar(Variable v, Thread t) {
		if (!(threads_for_var.containsKey(v))) {
			threads_for_var.put(v, new HashSet<Thread>());
		}
		threads_for_var.get(v).add(t);

		if (!(threads_for_readvar.containsKey(v))) {
			threads_for_readvar.put(v, new HashSet<Thread>());
		}
		threads_for_readvar.get(v).add(t);
	}

	public static void updateThreadsForWriteVar(Variable v, Thread t) {
		if (!(threads_for_var.containsKey(v))) {
			threads_for_var.put(v, new HashSet<Thread>());
		}
		threads_for_var.get(v).add(t);

		if (!(threads_for_writevar.containsKey(v))) {
			threads_for_writevar.put(v, new HashSet<Thread>());
		}
		threads_for_writevar.get(v).add(t);
	}

	public static Event RVEvent2Event(RVEvent rve) {
		long TID = rve.getTID();
		String tname = "T" + Long.toString(TID);
		if (!(threadMap.containsKey(tname))) {
			threadMap.put(tname, new Thread(tname));
			threadSet.add(threadMap.get(tname));
		}
		Thread t = threadMap.get(tname);

		long GID = rve.getGID();
		int LID = rve.getLocId();
		String ename = "E" + Long.toString(GID);
		Event e = null;

		if (rve.isRead()) {
			long ADDR = rve.getAddr();
			int addrl = (int) (ADDR >> 32);
			int addrr = rve.getFieldIdOrArrayIndex();
			String addr = addrr < 0 ? Integer.toHexString(addrl) + "." + -addrr
					: Integer.toHexString(addrl) + "[" + addrr + "]";
			String vname = "V" + addr;
			if (!(variableMap.containsKey(vname))) {
				variableMap.put(vname, new Variable(vname));
				retainedVariables.add(variableMap.get(vname));
			}
			EventType tp = EventType.READ;
			Variable v = variableMap.get(vname);
			e = new Event(GID, LID, ename, tp, t, null, v, null);

			updateThreadsForReadVar(v, t);
		}

		else if (rve.isWrite()) {
			long ADDR = rve.getAddr();
			int addrl = (int) (ADDR >> 32);
			int addrr = rve.getFieldIdOrArrayIndex();
			String addr = addrr < 0 ? Integer.toHexString(addrl) + "." + -addrr
					: Integer.toHexString(addrl) + "[" + addrr + "]";
			String vname = "V" + addr;
			if (!(variableMap.containsKey(vname))) {
				variableMap.put(vname, new Variable(vname));
				retainedVariables.add(variableMap.get(vname));
			}
			EventType tp = EventType.WRITE;
			Variable v = variableMap.get(vname);
			e = new Event(GID, LID, ename, tp, t, null, v, null);

			updateThreadsForWriteVar(v, t);
		}

		else if (rve.isLock()) {
			String lname = "L" + Long.toHexString(rve.getSyncObject());
			if (!(lockMap.containsKey(lname))) {
				lockMap.put(lname, new Lock(lname));
				lockSet.add(lockMap.get(lname));
			}
			EventType tp = EventType.ACQUIRE;
			Lock l = lockMap.get(lname);
			e = new Event(GID, LID, ename, tp, t, l, null, null);
		}

		else if (rve.isUnlock()) {
			String lname = "L" + Long.toHexString(rve.getSyncObject());
			if (!(lockMap.containsKey(lname))) {
				lockMap.put(lname, new Lock(lname));
				lockSet.add(lockMap.get(lname));
			}
			EventType tp = EventType.RELEASE;
			Lock l = lockMap.get(lname);
			e = new Event(GID, LID, ename, tp, t, l, null, null);
		}

		else if (rve.isStart()) {
			String target_name = "T" + Long.toString(rve.getSyncObject());
			if (!(threadMap.containsKey(target_name))) {
				threadMap.put(target_name, new Thread(target_name));
				threadSet.add(threadMap.get(target_name));
			}
			EventType tp = EventType.FORK;
			Thread target = threadMap.get(target_name);
			e = new Event(GID, LID, ename, tp, t, null, null, target);
		}

		else if (rve.isJoin()) {
			String target_name = "T" + Long.toString(rve.getSyncObject());
			if (!(threadMap.containsKey(target_name))) {
				threadMap.put(target_name, new Thread(target_name));
				threadSet.add(threadMap.get(target_name));
			}
			EventType tp = EventType.JOIN;
			Thread target = threadMap.get(target_name);
			e = new Event(GID, LID, ename, tp, t, null, null, target);
		}

		else {
			throw new IllegalArgumentException("Illegal type of event " + rve.getType().toString());
		}

		return e;
	}

	public static ArrayList<Trace> getTraceList(String trace_folder) {
		int cnt = 0;
		Path path = Paths.get(trace_folder);
		ArrayList<Path> pathList = null;

		try {
			pathList = listSourceFiles(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		ArrayList<Trace> traceList = null;

		if (pathList != null) {
			traceList = new ArrayList<Trace>(pathList.size());
			for (int tr_i = 0; tr_i < pathList.size(); tr_i++) {
				Trace trace = new Trace();
				try {
					RVEventReader reader = new RVEventReader(pathList.get(tr_i));
					try {
						while (true) {
							RVEvent rve = reader.readEvent();
							if (rve.isReadOrWrite() || rve.isSyncEvent()) {

								Event e = RVEvent2Event(rve);
								if (e == null) {
									throw new IllegalArgumentException("Event e is null");
								}
								cnt = cnt + 1;
								//System.out.println(cnt);
								trace.addEvent(e);
							}
						}
					} catch (EOFException e) {
						reader.close();
					}
				} catch (IOException e) {
					System.out.println("Could not open reader");
					e.printStackTrace();
				}
				traceList.add(trace);
			}
			// System.out.println(pathList.size());
			// System.out.println(traceList.toString());
		} else {
			throw new IllegalArgumentException("pathList is null");
		}

		return traceList;

	}

	public static TraceAndDataSets parse(boolean online, String trace_folder) {
		ArrayList<Trace> traceList = getTraceList(trace_folder);
		if(online){
			/** Do not annotate **/
			//annotateOnlineTraceList(traceList);
		}
		else{
			throw new IllegalArgumentException("Offline version not supported");
			//traceList = removeThreadLocalEvents(traceList);
			//annotateTraceList(traceList);
		}
		Trace trace = Trace.mergeTraceList(traceList);
		return new TraceAndDataSets(trace, threadSet, lockSet, retainedVariables);
	}

	public static void main(String[] args) {
		String trace_folder = args[0];
		Trace trace = parse(true, trace_folder).getTrace();
		//System.out.println("thread count is " + Thread.threadCountTracker);
		System.out.println(trace.toPrettyString());
		//trace.printPrototypeStyle();
	}
}
