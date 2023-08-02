package parse.rv;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.PriorityQueue;

import event.Event;
import event.EventType;
import event.Lock;
import event.Thread;
import event.Variable;
import rvparse.RVEvent;
import rvparse.RVEventReader;

public class ParseRVPredict {
	private int totThreads;
	private ArrayList<Path> pathList;
	private RVEventReader readers[];
	private boolean readerClosed[];
	private PriorityQueue<Event> q;
	private HashMap<Thread, Integer> tMap;
	private int[] eventCounter;
	private HashMap<String, Thread> threadMap;
	private HashMap<String, Lock> lockMap;
	private HashMap<String, Variable> variableMap;

	public ParseRVPredict(String trace_folder, HashSet<Thread> threadSet){
		threadMap = new HashMap<String, Thread>();
		lockMap = new HashMap<String, Lock>();
		variableMap = new HashMap<String, Variable>();

		Path path = Paths.get(trace_folder);
		pathList = null;
		try {
			pathList = listSourceFiles(path);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if(this.pathList == null){
			throw new IllegalArgumentException("pathList is null");
		}

		this.totThreads = pathList.size();

		readers = new RVEventReader[this.totThreads];
		readerClosed = new boolean[this.totThreads];
		for (int tr_i = 0; tr_i < pathList.size(); tr_i++) {
			try{
				readers[tr_i] = new RVEventReader(pathList.get(tr_i));
				readerClosed[tr_i] = false;
			}
			catch (IOException e) {
				readerClosed[tr_i] = true;
				System.out.println("Could not open reader : " + pathList.get(tr_i).toString());
				e.printStackTrace();
			}
		}

		this.q = new PriorityQueue<Event>(this.totThreads, new Comparator<Event>() {
			public int compare(Event a, Event b) {
				if (a.getAuxId() > b.getAuxId())
					return 1;
				else if (a.getAuxId() == b.getAuxId())
					return 0;
				else
					return -1;
			}
		});

		// The following container will map thread-names to indices in the
		// actual consolidated trace
		tMap = new HashMap<Thread, Integer>();
		eventCounter = new int[this.totThreads]; // This is not necessarily right. The GID's were Long

		for (int tr_i = 0; tr_i < this.totThreads; tr_i++) {
			if (!readerClosed[tr_i]) {
				try {
					Event e = new Event(); 
					getNextEventThread(e, tr_i);
					while(e.getType().isDummyType()){
						//e.updateEvent();
						getNextEventThread(e, tr_i);
					}
					if(!e.getType().isDummyType()){
						q.add(e);
						eventCounter[tr_i] = 0;
						tMap.put(e.getThread(), (Integer) tr_i);
						if(threadSet != null){
							threadSet.add(e.getThread());
						}
					}

				} catch (EOFException ex){
					readerClosed[tr_i] = true;
					try{
						readers[tr_i].close();
					}
					catch (IOException e) {
						System.out.println("Could not close reader : " + pathList.get(tr_i).toString());
						e.printStackTrace();
					}
				}
				catch(IOException ex){
					System.err.println("Could not get next event : getNextEventThread(" + tr_i + ")");
					ex.printStackTrace();
				}
			}
		}		
	}

	public void RVEvent2Event(Event e, RVEvent rve) {
		long TID = rve.getTID();
		String tname = "T" + Long.toString(TID);
		if (!(threadMap.containsKey(tname))) {
			threadMap.put(tname, new Thread(tname));
		}
		Thread t = threadMap.get(tname);

		long GID = rve.getGID();
		int LID = rve.getLocId();
		String ename = "E" + Long.toString(GID);

		if (rve.isRead()) {
			long ADDR = rve.getAddr();
			int addrl = (int) (ADDR >> 32);
			int addrr = rve.getFieldIdOrArrayIndex();
			String addr = addrr < 0 ? Integer.toHexString(addrl) + "." + -addrr
					: Integer.toHexString(addrl) + "[" + addrr + "]";
			String vname = "V" + addr;
			if (!(variableMap.containsKey(vname))) {
				variableMap.put(vname, new Variable(vname));
			}
			EventType tp = EventType.READ;
			Variable v = variableMap.get(vname);

			e.updateEvent(GID, LID, ename, tp, t, null, v, null);
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
			}
			EventType tp = EventType.WRITE;
			Variable v = variableMap.get(vname);

			e.updateEvent(GID, LID, ename, tp, t, null, v, null);
		}

		else if (rve.isLock()) {
			String lname = "L" + Long.toHexString(rve.getSyncObject());
			if (!(lockMap.containsKey(lname))) {
				lockMap.put(lname, new Lock(lname));
			}
			EventType tp = EventType.ACQUIRE;
			Lock l = lockMap.get(lname);

			e.updateEvent(GID, LID, ename, tp, t, l, null, null);
		}

		else if (rve.isUnlock()) {
			String lname = "L" + Long.toHexString(rve.getSyncObject());
			if (!(lockMap.containsKey(lname))) {
				lockMap.put(lname, new Lock(lname));
			}
			EventType tp = EventType.RELEASE;
			Lock l = lockMap.get(lname);

			e.updateEvent(GID, LID, ename, tp, t, l, null, null);
		}

		else if (rve.isStart()) {
			String target_name = "T" + Long.toString(rve.getSyncObject());
			if (!(threadMap.containsKey(target_name))) {
				threadMap.put(target_name, new Thread(target_name));
			}
			EventType tp = EventType.FORK;
			Thread target = threadMap.get(target_name);

			e.updateEvent(GID, LID, ename, tp, t, null, null, target);
		}

		else if (rve.isJoin()) {
			String target_name = "T" + Long.toString(rve.getSyncObject());
			if (!(threadMap.containsKey(target_name))) {
				threadMap.put(target_name, new Thread(target_name));
			}
			EventType tp = EventType.JOIN;
			Thread target = threadMap.get(target_name);

			e.updateEvent(GID, LID, ename, tp, t, null, null, target);
		}
		
		else if (rve.isInvokeMethod()) {
			EventType tp = EventType.BEGIN;
			e.updateEvent(GID, LID, ename, tp, t, null, null, null);
		}
		
		else if (rve.isCallStackEvent() && !rve.isInvokeMethod()) {
			EventType tp = EventType.END;
			e.updateEvent(GID, LID, ename, tp, t, null, null, null);
		}

		else {
			//throw new IllegalArgumentException("Illegal type of event " + rve.getType().toString());
		}
	}

	public void getNextEventThread(Event e, int tr_i) throws IOException {
		RVEvent rve = readers[tr_i].readEvent();
		if (rve.isReadOrWrite() || rve.isSyncEvent() || rve.isMetaEvent()) {
			RVEvent2Event(e, rve);
			if (e == null) {
				throw new IllegalArgumentException("Event e is null");
			}
		}
	}

	public void getNextEvent(Event e){ //e is supposed to be over-written (deep copy) by the top-most event of the priority queue
		Event e_top = this.q.poll();
		e.copyFrom(e_top);
		int tr_i = this.tMap.get(e_top.getThread());
		eventCounter[tr_i]++;
		if (!this.readerClosed[tr_i]) {
			try {
				e_top.updateEvent();
				getNextEventThread(e_top, tr_i);
				while(e_top.getType().isDummyType()){
					//e_top.updateEvent();
					getNextEventThread(e_top, tr_i);
				}
				if(!e_top.getType().isDummyType()){
					q.add(e_top);
				}
			} 
			catch (EOFException ex){
				readerClosed[tr_i] = true;
				try{
					readers[tr_i].close();
				}
				catch (IOException ex1) {
					System.out.println("Could not close reader : " + pathList.get(tr_i).toString());
					ex1.printStackTrace();
				}
			}
			catch(IOException ex){
				System.err.println("Could not get next event : getNextEventThread(" + tr_i + ")");
				ex.printStackTrace();
			}

		}
	}

	public boolean pathListNotNull(){
		return (pathList != null);
	}

	public boolean hasNext(){
		return (this.q.size() > 0);
	}

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
	
	public int getTotalThreads(){
		return this.totThreads;
	}

}
