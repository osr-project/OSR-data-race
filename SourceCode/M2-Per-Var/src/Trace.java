
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;



public class Trace {

	public int windowSize = (int) 1;		//Store one event map per window size
	public int numThreads;
	public List<Pair<Integer, Integer>> trace;
	public int [][] traceIndex;	//Given an index in a sequential trace, return the index in the trace 
	public Event [][]seqTraces;


	public boolean dropThreadLocalLocations;	//drop locations that are only accessed by a single thread

	public int initialNumEvents;

	public int num_locks, num_lock_events;

	public List<ArrayList<Integer>>eventRenameList;

	public Map<Integer, Location> locations;
	public int[] dropped, droppedHB;

	//Extra happens before edges in the trace. Does not contain the reverse observation edges
	public Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> HBedges, inverseHBEdges;


	public Map<Pair<Integer, Integer>, Pair<Integer, Integer>> observation, remoteObservation;
	public Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> reverseObservation;
	public List<BiMap<Integer, Integer>> lockMatch;		//Points to the matching lock event that closes the critical region
	public List<Map<Integer, Map<Integer, Integer>>> openAcquires;

	//The lock and global variable locations that appear in each sequential trace
	public List<Set<Integer>> seqTraceLockLocations, seqTraceGlobalLocations;
	public List<List<Set<Integer>>> pairWiseGlobalLocations;
	public Map<Integer, Integer> numLocAppearances;

	//Event maps
	private List<Map<Integer, EventMap>> mapRelease, mapAcquire, mapWrite, mapRead;

	private int[][] nextEventIncEdge, nextEventIncEdgeUnique;

	private List<List<Integer>> nextSeqEventMap;



	public Trace(Trace other, List<Pair<Integer, Integer>> trace, boolean buildMaps) {
		this.numThreads = other.numThreads;
		this.trace = new ArrayList<Pair<Integer, Integer>>(trace);
		this.traceIndex = new int[this.numThreads][];
		this.seqTraces = new Event[this.numThreads][];
		//Get the last events in the trace
		int[] lengths = new int[this.numThreads];
		for(int i=0; i< this.numThreads; i++) {
			lengths[i] = 0;
		}

		for(int i=0; i<this.trace.size(); i++) {
			Pair<Integer, Integer> u = this.trace.get(i);
			lengths[u.x]++;
		}

		for(int i=0; i<this.numThreads; i++) {
			this.seqTraces[i] = new Event[lengths[i]];
			this.traceIndex[i] = new int[lengths[i]];
		}

		for(int i=0; i<this.trace.size(); i++) {
			Pair<Integer, Integer> cur = this.trace.get(i);
			this.seqTraces[cur.x][cur.y] = other.seqTraces[cur.x][cur.y];
			this.traceIndex[cur.x][cur.y] = i;
		}


		this.HBedges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>(other.HBedges);
		this.inverseHBEdges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>(other.inverseHBEdges);
		this.buildObservation();
		//this.printTrace();

		this.testCorrectReordering(other, lengths);

		if(buildMaps) {
			this.initMaps();
			this.buildCritical();
			this.trackLocations();
		}
	}

	public String evPairStr(Pair<Integer, Integer> u, Pair<Integer, Integer> v) {
		return this.evStr(u) + " - " + this.evStr(v);
	}


	public void testCorrectReordering(Trace other, int[] lengths) {
		System.out.println("Testing witness");

		if(!this.respectsLocks()) {
			System.out.println("Error! New trace does not respect locks!");
			this.printTrace();
			this.printSeqTraces();
			try {System.in.read();}catch(Exception e) {}
		}

		Set<Pair<Integer, Integer>> readsToIgnore = new HashSet<Pair<Integer, Integer>>();
		for(int i=0; i<this.numThreads; i++) {
			if(lengths[i] > 0 && this.seqTraces[i][lengths[i]-1].type == EventType.READ) {
				readsToIgnore.add(new Pair<Integer, Integer>(i, lengths[i]-1));
			}
		}

		//this.printTrace();

		if(!this.matchesObservation(other, readsToIgnore)) {
			System.out.println("Error! New trace does not respect observations!");
			this.printTrace();
			other.printTrace();
			try {System.in.read();}catch(Exception e) {}
		}

		if(!this.matchesLocalThreads(other)) {
			System.out.println("Error! New trace does not respect local threads!");
			this.printTrace();
			other.printTrace();
			try {System.in.read();}catch(Exception e) {}
		}

		if(!this.consistentWithHB()) {
			System.out.println("Error! New trace does not respect HB!");
			this.printTrace();
			other.printTrace();
			try {System.in.read();}catch(Exception e) {}
		}

		System.out.println("Done");
	}



	//Shrink a trace by deleting all events in a location if
	//1. The location is accessed only by one thread
	//2. The location is written at most once
	public Trace(String filenameIn, boolean dropThreadLocalLocations, boolean buildMaps, boolean buildNextSeqEventMap) {

		this.dropThreadLocalLocations = dropThreadLocalLocations;


		if(this.dropThreadLocalLocations){
			Set<Integer> keptLocations = this.readTraceMem(filenameIn);
			this.readTraceFilter(filenameIn, keptLocations);
		}
		else {
			this.readTrace(filenameIn);
		}


		this.remoteObservation = null;
		this.buildObservation();
		this.buildRemoteObservation();


		if(buildMaps) {
			this.initMaps();
			this.buildCritical();
			this.trackLocations();
			this.buildEventsWithIncomingEdges();
		}
		if(buildNextSeqEventMap) {
			this.buildNextSeqEventMap();
		}

	}

	public Map<Integer, Location> computeLocations(boolean trackEvents){
		//Compute stats per location
		Map<Integer, Location> location = new HashMap<Integer, Location>();
		for(int i=0; i<this.trace.size(); i++) {

			if( this.trace.size() > 1e4 &&  i % (this.trace.size()/10) == 0 ){
				System.out.println("Computed " + i + "/" + this.trace.size() + " locations");
			}

			Pair<Integer, Integer> e = this.trace.get(i);
			Event ev = this.seqTraces[e.x][e.y];
			if(!location.containsKey(ev.location)) {
				location.put(ev.location, new Location(ev.location, this.numThreads, trackEvents));
			}
			Location loc = location.get(ev.location);
			loc.addOperation(ev, e, trackEvents);
		}
		return location;
	}

	public Set<Integer> filterLocalEvents() {

		this.dropped = new int [this.numThreads];
		for(int i=0; i<this.numThreads; i++) {
			this.dropped[i] = 0;
		}

		System.out.println("filterLocalEvents out of " + this.locations.size() + " locations.");

		//this.locations = this.computeLocations(false);
		Set<Integer> keptLocs = new HashSet<Integer>();
		int totalDropped = 0;

		System.out.println("Dropping locations...");
		for(int l:this.locations.keySet()) {
			Location loc = this.locations.get(l);
			//if( (loc.counts.size() < 2)  || (loc.kind == EventKind.WRITE_READ && loc.numWrites < 1)) {
			if( (loc.counts.size() < 2) && loc.kind == EventKind.WRITE_READ) {
				totalDropped += loc.getTotalEvents();
				for(int tid : loc.counts.keySet()) {
					this.dropped[tid] += loc.counts.get(tid);
				}
			}
			else {
				keptLocs.add(l);
			}
		}

		System.out.println("Dropped Local = " + Arrays.toString(this.dropped) + " Total = " + totalDropped);
		return keptLocs;
	}



	public void readTrace(String filename) {
//		System.out.println(filename);
		this.num_locks = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			line = br.readLine();	//Read num of threads
			this.numThreads = Integer.parseInt(line.split(":")[1].trim());
			this.seqTraces = new Event[this.numThreads][];
			this.traceIndex = new int[this.numThreads][];

			line = br.readLine();	//Read num of events in each thread
			int i = 0;
			int lenTrace = 0;
			for (String ss : line.split(":")[1].split(",")) {
				int l =Integer.parseInt(ss.trim());
				this.seqTraces[i] = new Event[l];
				this.traceIndex[i] = new int[l];
				i++;
				lenTrace += l;
			}

			this.trace = new ArrayList<Pair<Integer, Integer>>(lenTrace);
			this.HBedges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
			this.inverseHBEdges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
			int [] lastIndex = new int[this.numThreads];
			// Read the trace
			for(i=0; i< lenTrace; i++) {

				if( lenTrace > 1e4 &&  i % (lenTrace/10) == 0 ){
//					System.out.println("Read " + i + "/" + lenTrace + " events");
				}

				line = br.readLine();
				//System.out.println(line);
				String[] parts2 = line.split("\\|");
				String[] parts3 = parts2[1].split(",");
				int eventID = Integer.parseInt(parts3[0].trim());
				int codeLocation = -1;
				if(parts3.length > 1) {
					codeLocation = Integer.parseInt(parts3[1].trim());
				}
				String [] parts = parts2[0].split(",");
				int j;
				int location=-1;
				EventType type=null;
				EventKind kind=null;
				for(j=0; j< this.numThreads; j++) {
					if(parts[j].length()>2) {
						String [] eParts = parts[j].split(":");
						location = Integer.parseInt(eParts[1].trim());

						if(eParts[0].equals("w")) {
							type = EventType.WRITE;
							kind = EventKind.WRITE_READ;
						}
						else if(eParts[0].trim().equals("r")) {
							type = EventType.READ;
							kind = EventKind.WRITE_READ;
						}
						else if(eParts[0].trim().equals("acq")) {
							type = EventType.ACQUIRE;
							kind = EventKind.ACQUIRE_RELEASE;
							//lockLocations.add(location);
						}
						else if(eParts[0].trim().equals("rel")) {
							type = EventType.RELEASE;
							kind = EventKind.ACQUIRE_RELEASE;
						}
						else if(eParts[0].trim().equals("noop")) {
							type = EventType.NOOP;
							kind = EventKind.OTHER;
						}
						else {
							System.out.println("COULD NOT ASSIGN EVENT TYPE");
						}
						break;
					}
				}


				Pair<Integer, Integer> u = new Pair<Integer, Integer> (j, lastIndex[j]);
				this.seqTraces[u.x][u.y] = new Event(type, kind, location, eventID, codeLocation);
				this.traceIndex[u.x][u.y] =this.trace.size(); 

				this.trace.add(u);
				lastIndex[j]++;
			}

			//Read the extra HB edges
			while ((line = br.readLine()) != null) {
				if(line.length()> 4) {
					String [] parts = line.split("-");
					int ux = Integer.parseInt(parts[0].split("\\.")[0].trim()), uy=Integer.parseInt(parts[0].split("\\.")[1].trim());
					int vx = Integer.parseInt(parts[1].split("\\.")[0].trim()), vy=Integer.parseInt(parts[1].split("\\.")[1].trim());
					Pair<Integer, Integer> u =new Pair<Integer, Integer>(ux, uy);
					Pair<Integer, Integer> v = new Pair<Integer, Integer>(vx, vy);

					if(!this.HBedges.containsKey(u)) {
						this.HBedges.put(u, new HashSet<Pair<Integer, Integer>>());
					}
					this.HBedges.get(u).add(v);

					if(!this.inverseHBEdges.containsKey(v)) {
						this.inverseHBEdges.put(v, new HashSet<Pair<Integer, Integer>>());
					}
					this.inverseHBEdges.get(v).add(u);

				}
			}

			br.close();
			//System.out.println("Locations read: " + locationsRead.size());
			//System.out.println("Locations written: " + locationsWritten.size());
		}
		catch(Exception e) {
			System.out.println("Except");
			e.printStackTrace();
		}
	}



	public Set<Integer> readTraceMem(String filename) {

		System.out.println("Pre-reading trace to compute kept locations");

		this.num_locks = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			line = br.readLine();	//Read num of threads
			this.numThreads = Integer.parseInt(line.split(":")[1].trim());
			this.seqTraces = new Event[this.numThreads][];

			line = br.readLine();	//Read num of events in each thread
			int i = 0;
			int lenTrace = 0;
			for (String s : line.split(":")[1].split(",")) {
				int l =Integer.parseInt(s.trim());
				this.seqTraces[i] = new Event[l];
				i++;
				lenTrace += l;
			}

			this.initialNumEvents = lenTrace;
			this.locations = new HashMap<Integer, Location>();
			this.HBedges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
			this.inverseHBEdges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
			int [] lastIndex = new int[this.numThreads];
			// Read the trace
			for(i=0; i< lenTrace; i++) {

				if( lenTrace > 1e4 &&  i % (lenTrace/10) == 0 ){
//					System.out.println("Read " + i + "/" + lenTrace + " events");
				}

				line = br.readLine();
				String[] parts2 = line.split("\\|");
				String[] parts3 = parts2[1].split(",");
				int eventID = Integer.parseInt(parts3[0].trim());
				int codeLocation = -1;
				if(parts3.length > 1) {
					codeLocation = Integer.parseInt(parts3[1].trim());
				}
				String [] parts = parts2[0].split(",");
				int j;
				int location=-1;
				EventType type=null;
				EventKind kind=null;
				//System.out.println(line);
				for(j=0; j< this.numThreads; j++) {
					if(parts[j].length()>2) {
						String [] eParts = parts[j].split(":");
						location = Integer.parseInt(eParts[1].trim());
						if(eParts[0].equals("w")) {
							type = EventType.WRITE;
							kind = EventKind.WRITE_READ;
						}
						else if(eParts[0].trim().equals("r")) {
							type = EventType.READ;
							kind = EventKind.WRITE_READ;
						}
						else if(eParts[0].trim().equals("acq")) {
							type = EventType.ACQUIRE;
							kind = EventKind.ACQUIRE_RELEASE;
							//lockLocations.add(location);
						}
						else if(eParts[0].trim().equals("rel")) {
							type = EventType.RELEASE;
							kind = EventKind.ACQUIRE_RELEASE;
						}
						else if(eParts[0].trim().equals("noop")) {
							type = EventType.NOOP;
							kind = EventKind.OTHER;
						}
						break;
					}
				}
				Pair<Integer, Integer> u = new Pair<Integer, Integer> (j, lastIndex[j]);
				lastIndex[j]++;
				Event ev = new Event(type, kind, location, eventID, codeLocation);
				if(!this.locations.containsKey(ev.location)) {
					this.locations.put(ev.location, new Location(ev.location, this.numThreads, false));
				}
				Location loc = this.locations.get(ev.location);
				loc.addOperation(ev, u, false);
			}

			//Read the extra HB edges
			while ((line = br.readLine()) != null) {
				if(line.length()> 4) {
					String [] parts = line.split("-");
					int ux = Integer.parseInt(parts[0].split("\\.")[0].trim()), uy=Integer.parseInt(parts[0].split("\\.")[1].trim());
					int vx = Integer.parseInt(parts[1].split("\\.")[0].trim()), vy=Integer.parseInt(parts[1].split("\\.")[1].trim());
					Pair<Integer, Integer> u =new Pair<Integer, Integer>(ux, uy);
					Pair<Integer, Integer> v = new Pair<Integer, Integer>(vx, vy);

					if(!this.HBedges.containsKey(u)) {
						this.HBedges.put(u, new HashSet<Pair<Integer, Integer>>());
					}
					this.HBedges.get(u).add(v);

					if(!this.inverseHBEdges.containsKey(v)) {
						this.inverseHBEdges.put(v, new HashSet<Pair<Integer, Integer>>());
					}
					this.inverseHBEdges.get(v).add(u);

				}
			}

			br.close();
		}
		catch(Exception e) {
			System.out.println("Except");
			e.printStackTrace();
		}

		System.out.println("Computing kept locations");

		//Update the HB edges based on kept locations
		Set<Integer> keptLocations = this.filterLocalEvents();

		return keptLocations;
	}


	public void addFrontier(List<List<LinkedList<Pair<Integer, Integer>>>> s, Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> e) {

		if(e.x.x == e.y.x) {
			return;
		}
		LinkedList<Pair<Integer, Integer>> sTT = s.get(e.x.x).get(e.x.x>e.y.x? e.y.x:e.y.x-1);
		ListIterator<Pair<Integer, Integer>> it = sTT.listIterator();
		boolean dominated = false;
		while(it.hasNext()) {
			Pair<Integer, Integer> x = it.next();
			if(x.x >= e.x.y && x.y <= e.y.y) {
				dominated = true;
				break;
			}
			if(e.x.y >= x.x && e.y.y <= x.y) {
				it.remove();
			}
		}
		if(!dominated) {
			sTT.add(new Pair<Integer, Integer>(e.x.y, e.y.y));
		}
	}



	public void readTraceFilter(String filename, Set<Integer> keepLocations) {

//		System.out.println("Reading trace while keeping only specific locations");

		this.initEventRenameList();

		List<List<LinkedList<Pair<Integer, Integer>>>> s = new ArrayList<List<LinkedList<Pair<Integer, Integer>>>>();
		for(int i=0; i<this.numThreads; i++) {
			List<LinkedList<Pair<Integer, Integer>>> sT = new ArrayList<LinkedList<Pair<Integer, Integer>>>();
			for(int j=0; j<this.numThreads-1; j++) {
				LinkedList<Pair<Integer, Integer>> sTT = new LinkedList<Pair<Integer, Integer>>();
				sT.add(sTT);
			}
			s.add(sT);
		}

		this.num_locks = 0;
		try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
			String line;
			line = br.readLine();	//Read num of threads
			this.numThreads = Integer.parseInt(line.split(":")[1].trim());
			this.seqTraces = new Event[this.numThreads][];
			this.traceIndex = new int[this.numThreads][];

			line = br.readLine();	//Read num of events in each thread
			int i = 0;
			int lenTrace = 0;
			for (String ss : line.split(":")[1].split(",")) {
				int l =Integer.parseInt(ss.trim());
				this.seqTraces[i] = new Event[l - this.dropped[i]];
				this.traceIndex[i] = new int[l - this.dropped[i]];
				i++;
				lenTrace += l;
			}

			//Set<Integer> lockLocations = new HashSet<Integer>();
			Map<Integer, Pair<Integer,Integer>> lastWrite = new HashMap<Integer, Pair<Integer,Integer>>();
			this.trace = new ArrayList<Pair<Integer, Integer>>(lenTrace);
			//this.HBedges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
			//this.inverseHBEdges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
			int [] lastIndex = new int[this.numThreads];
			int [] oldLastIndex = new int[this.numThreads];
			// Read the trace
			for(i=0; i< lenTrace; i++) {

				if( lenTrace > 1e4 &&  i % (lenTrace/10) == 0 ){
//					System.out.println("Read " + i + "/" + lenTrace + " events");
				}

				line = br.readLine();

				String[] parts2 = line.split("\\|");
				String[] parts3 = parts2[1].split(",");
				int eventID = Integer.parseInt(parts3[0].trim());
				int codeLocation = -1;
				if(parts3.length > 1) {
					codeLocation = Integer.parseInt(parts3[1].trim());
				}

				String [] parts = parts2[0].split(",");
				int j;
				int location=-1;
				EventType type=null;
				EventKind kind=null;
				for(j=0; j< this.numThreads; j++) {
					if(parts[j].length()>2) {
						String [] eParts = parts[j].split(":");
						location = Integer.parseInt(eParts[1].trim());

						if(eParts[0].equals("w")) {
							type = EventType.WRITE;
							kind = EventKind.WRITE_READ;
						}
						else if(eParts[0].trim().equals("r")) {
							type = EventType.READ;
							kind = EventKind.WRITE_READ;
						}
						else if(eParts[0].trim().equals("acq")) {
							type = EventType.ACQUIRE;
							kind = EventKind.ACQUIRE_RELEASE;
							//lockLocations.add(location);
						}
						else if(eParts[0].trim().equals("rel")) {
							type = EventType.RELEASE;
							kind = EventKind.ACQUIRE_RELEASE;
						}
						else if(eParts[0].trim().equals("noop")) {
							type = EventType.NOOP;
							kind = EventKind.OTHER;
						}
						else {
							System.out.println("COULD NOT ASSIGN EVENT TYPE");
						}
						break;
					}
				}

				Pair<Integer, Integer> oldU = new Pair<Integer, Integer>(j, oldLastIndex[j]);
				if(type == EventType.WRITE) {
					lastWrite.put(location, oldU);
				}

				//Dropped event might enforce a HB edge, track it
				if(!keepLocations.contains(location)) {
					if(type == EventType.READ) {
						this.addFrontier(s, new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(lastWrite.get(location), oldU));
					}
					oldLastIndex[j]++;
					continue;
				}

				Pair<Integer, Integer> u = new Pair<Integer, Integer> (j, lastIndex[j]);
				this.seqTraces[u.x][u.y] = new Event(type, kind, location, eventID, codeLocation);
				this.traceIndex[u.x][u.y] =this.trace.size(); 

				this.addEventRename(oldU);
				this.trace.add(u);
				oldLastIndex[j]++;
				lastIndex[j]++;
			}
			//The frontier edges have been added t)o s due to write->read edges of events not loaded
			//Rename the edges to respect the event renaming
			this.eventRenameHBEdges();
			//this.printHBedges();
			System.out.println("Renamed HB edges.");
			br.close();
		}
		catch(Exception e) {
			System.out.println("Except");
			e.printStackTrace();
		}
	}

	public int getLocation(Pair<Integer, Integer> u) {
		Event ev = this.seqTraces[u.x][u.y];
		return ev.location;
	}

	public void buildEventsWithIncomingEdges() {
		this.nextEventIncEdge = new int[this.numThreads][];
		this.nextEventIncEdgeUnique = new int[this.numThreads][];
		for(int i=0; i<this.numThreads; i++) {
			this.nextEventIncEdge[i] = new int[this.seqTraces[i].length];
			this.nextEventIncEdgeUnique[i] = new int[this.seqTraces[i].length];
			int lastEventIncEdge=-1, lastEventIncEdgeUnique = -1;
			for(int j = this.seqTraces[i].length-1; j>=0; j--) {
				this.nextEventIncEdge[i][j] = lastEventIncEdge;
				this.nextEventIncEdgeUnique[i][j] = lastEventIncEdgeUnique;
				Pair<Integer, Integer> u =new Pair<Integer,Integer>(i,j);
				if(this.hasIncomingEdge(u)) {
					lastEventIncEdge = j;
				}
				if(this.hasIncomingEdgeUnique(u)) {
					lastEventIncEdgeUnique = j;
				}
			}
		}
	}


	private void initMaps() {

//		System.out.println("Build event maps");

		this.mapRelease = new ArrayList<Map<Integer, EventMap>>();
		this.mapAcquire = new ArrayList<Map<Integer, EventMap>>();
		this.mapWrite = new ArrayList<Map<Integer, EventMap>>();
		this.mapRead = new ArrayList<Map<Integer, EventMap>>();

		for(int i=0; i<this.numThreads; i++) {
			this.mapRelease.add(new HashMap<Integer, EventMap>());
			this.mapAcquire.add(new HashMap<Integer, EventMap>());
			this.mapWrite.add(new HashMap<Integer, EventMap>());
			this.mapRead.add(new HashMap<Integer, EventMap>());
		}

		for(int i=0; i<this.trace.size(); i++) {
			Pair<Integer, Integer> u = this.trace.get(i);
			Event ev = this.seqTraces[u.x][u.y];

			Map<Integer, EventMap> map = null;
			if(ev.type == EventType.RELEASE) {
				map = this.mapRelease.get(u.x);
			}
			else if(ev.type == EventType.ACQUIRE) {
				map = this.mapAcquire.get(u.x);
			}
			else if(ev.type == EventType.READ) {
				map = this.mapRead.get(u.x);
			}
			else if(ev.type == EventType.WRITE) {
				map = this.mapWrite.get(u.x);
			}
			else {
				continue;
			}

			if(!map.containsKey(ev.location)) {
				map.put(ev.location, new EventMap());
			}
			map.get(ev.location).addEvent(u.y);
		}
	}


	public int getLength() {
		return this.trace.size();
	}

	public void initEventRenameList() {
		this.eventRenameList = new ArrayList<ArrayList<Integer>>();
		for(int i=0; i<this.numThreads; i++) {
			this.eventRenameList.add(new ArrayList<Integer>());
		}

	}

	public void addEventRename(Pair<Integer, Integer> u) {
		this.eventRenameList.get(u.x).add(u.y);
	}

	public int getEventOldName(Pair<Integer, Integer> u) {
		return this.eventRenameList.get(u.x).get(u.y);
	}

	//Returns the last active event that happens before (or equal to) u in its local trace
	public int getEventLast(Pair<Integer, Integer> u) {
		//System.out.println("Getting last event " + u);
		//try {System.in.read();} catch(Exception e) {}
		ArrayList<Integer> l = this.eventRenameList.get(u.x);
		int left=0, right=l.size()-1;
		while(left<right) {
			int mid = (int)Math.ceil((right+left)/2.0);
			int x = l.get(mid);
			//System.out.println("left= " + left + " right=" + right + " mid="+mid + " x="+x);
			//try {System.in.read();} catch(Exception e) {}
			if(x == u.y) {
				//System.out.println("On " + u + " returning " + mid);
				return mid;
			}
			if(x > u.y) {
				right = mid-1;
			}
			else {
				left=mid;
			}
		}
		//System.out.println("On " + u + " returning " + left);
		return left;
	}

	public boolean eventActive(Pair<Integer, Integer> u, int last) {
		return this.eventRenameList.get(u.x).get(last).equals(u.y);
	}

	public void eventRenameHBEdges() {

		//this.printEventRenames();

		Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> oldHBedges;
		oldHBedges = this.HBedges;

		this.HBedges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
		this.inverseHBEdges = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();

		for(Pair<Integer, Integer> u : oldHBedges.keySet()) {
			Pair<Integer, Integer> uAlias = new Pair<Integer, Integer>(u.x, this.getEventLast(u) );
			for(Pair<Integer, Integer> v : oldHBedges.get(u)) {
				int vLast = this.getEventLast(v);
				//System.out.println("v=" + v + " vLast=" + vLast);
				boolean vActive = this.eventActive(v, vLast);

				Pair<Integer, Integer> vAlias = new Pair<Integer, Integer>(v.x, vActive ? vLast : vLast + 1);
				this.insertEdge(uAlias, vAlias);
			}
		}


	}



	public boolean hasIncomingEdge(Pair<Integer, Integer> u) {
		return this.inverseHBEdges.containsKey(u) || (this.seqTraces[u.x][u.y].type == EventType.READ && this.observation.get(u).x != u.x);
	}

	public boolean hasIncomingEdgeUnique(Pair<Integer, Integer> u) {
		if(this.inverseHBEdges.containsKey(u)) {
			return true;
		}

		Event evU = this.seqTraces[u.x][u.y];
		Pair<Integer, Integer> w = this.observation.get(u);
		if(evU.type == EventType.READ && w.x != u.x) {
			int r = this.getToEfromReads(u, evU.location, false);
			if(r > 0) {
				Pair<Integer, Integer> uu = new Pair<Integer, Integer>(u.x, r);
				Pair<Integer, Integer> ww = this.observation.get(uu);
				return !w.equals(ww);
			}
			return true;
		}

		return false;
	}

	public int numHBEdges() {
		int total = 0;
		for(Pair<Integer, Integer> u: this.HBedges.keySet()) {
			total += this.HBedges.get(u).size();
		}
		return total;
	}

	//Return true if u and v are protected by the same lock
	public boolean protectedSameLock(Pair<Integer, Integer> u, Pair<Integer, Integer>  v) {
		return CollectionUtils.containsAny(this.getLocalGuardingLocks(u), this.getLocalGuardingLocks(v));
	}



	//Return the locks that guard the event
	public Set<Integer> getLocalGuardingLocks(Pair<Integer, Integer> u){
		Set<Integer> s = new HashSet<Integer>();
		if(this.openAcquires.get(u.x).containsKey(u.y)) {
			s.addAll(this.openAcquires.get(u.x).get(u.y).keySet());
		}
		return s;
	}

	//Return the *local* acquires that guard the event
	public Set<Integer> getLocalGuardingAcquires(Pair<Integer, Integer> u){
		Set<Integer> s = new HashSet<Integer>();
		if(this.openAcquires.get(u.x).containsKey(u.y)) {
			s.addAll(this.openAcquires.get(u.x).get(u.y).values());
		}
		return s;
	}

	public int getTopLocalGuardingAcquire(Pair<Integer, Integer> u) {
		Set<Integer> localGuardingAcquires = this.getLocalGuardingAcquires(u);
		return localGuardingAcquires.isEmpty() ? -1 : Collections.min(localGuardingAcquires);
	}

	public Map<Integer, Integer> getLocalGuardingLocksAndAcquiers(Pair<Integer, Integer> u){
		Map<Integer, Integer> m = new HashMap<Integer, Integer>();

		if(this.openAcquires.get(u.x).containsKey(u.y)) {
			m.putAll(this.openAcquires.get(u.x).get(u.y));
		}

		return m;
	}


	public int getMatchingAcquire(Pair<Integer, Integer> u) {
		if(this.lockMatch.get(u.x).containsKey(u.y)) {
			return lockMatch.get(u.x).get(u.y);
		}
		return -1;
	}

	public int getMatchingRelease(Pair<Integer, Integer> u) {
		if(this.lockMatch.get(u.x).inverse().containsKey(u.y)) {
			return lockMatch.get(u.x).inverse().get(u.y);
		}
		return -1;
	}

	//Get the common globals of threads i and j
	public Set<Integer> getGlobalLocs(int i, int j){
		if(i>j) {
			int l=i;
			i=j;
			j=l;
		}
		return this.pairWiseGlobalLocations.get(i).get(j-i-1);
	}


	//Build the observation and reverse observation functions 
	//Assumes that every read has at least one conflicting preceding write in the trace
	public void buildObservation() {
		//Build the observation function
		this.observation = new HashMap<Pair<Integer, Integer>, Pair<Integer, Integer>>();
		Map<Integer, Pair<Integer, Integer>> lastLocation = new HashMap<Integer, Pair<Integer, Integer>>();
		for (int i=0; i<this.trace.size(); i++){
			Event curEv = seqTraces[this.trace.get(i).x][this.trace.get(i).y];
			if(curEv.type == EventType.WRITE) {
				lastLocation.put(curEv.location, this.trace.get(i));
			}
			else if(curEv.type == EventType.READ) {
				this.observation.put(new Pair<Integer, Integer>(this.trace.get(i).x, this.trace.get(i).y), lastLocation.get(curEv.location));
			}
		}
		//Build the reverse observation function
		this.reverseObservation = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
		for(Pair<Integer, Integer> r: this.observation.keySet()) {
			Pair<Integer, Integer> w = this.observation.get(r);
			if(!this.reverseObservation.containsKey(w)) {
				this.reverseObservation.put(w, new HashSet<Pair<Integer, Integer>>());
			}
			this.reverseObservation.get(w).add(r);
		}

	}

	public void buildRemoteObservation() {
		//Build the observation function
		this.remoteObservation = new HashMap<Pair<Integer, Integer>, Pair<Integer, Integer>>();
		Map<Integer, Pair<Integer, Integer>> lastLocation = new HashMap<Integer, Pair<Integer, Integer>>();
		for (int i=0; i<this.trace.size(); i++){
			Event curEv = seqTraces[this.trace.get(i).x][this.trace.get(i).y];
			if(curEv.type == EventType.WRITE) {
				lastLocation.put(curEv.location, this.trace.get(i));
			}
			else if(curEv.type == EventType.READ) {
				if (this.trace.get(i).x != lastLocation.get(curEv.location).x) {
					this.remoteObservation.put(new Pair<Integer, Integer>(this.trace.get(i).x, this.trace.get(i).y), lastLocation.get(curEv.location));
				}

			}
		}
	}


	public void insertEdge(Pair<Integer, Integer> u, Pair<Integer, Integer> v) {
		if(u.x != v.x) {
			if(!this.HBedges.containsKey(u)) {
				this.HBedges.put(u, new HashSet<Pair<Integer, Integer>>());
			}
			this.HBedges.get(u).add(v);

			if(!this.inverseHBEdges.containsKey(v)) {
				this.inverseHBEdges.put(v, new HashSet<Pair<Integer, Integer>>());
			}
			this.inverseHBEdges.get(v).add(u);
		}
	}

	//For every index in the trace, it stores the index of the next event from each thread (inclusive)
	public void buildNextSeqEventMap() {
		this.nextSeqEventMap = new ArrayList<List<Integer>>(this.trace.size());
		for(int i=0; i< this.trace.size(); i++) {
			this.nextSeqEventMap.add(null);
		}
		List<Integer> seqEventMap = new ArrayList<Integer>();
		for(int i=0; i<this.numThreads; i++) {
			seqEventMap.add(-1);
		}
		for(int i=this.trace.size()-1; i>=0; i--) {
			Pair<Integer, Integer> u =  this.trace.get(i);
			seqEventMap.set(u.x, i);
			this.nextSeqEventMap.set(i, new ArrayList<Integer>(seqEventMap));
		}
	}

	//Return the next event of thread j that occurs after index i in the trace
	public int getNextSeqEvent(int i, int j) {
		return this.nextSeqEventMap.get(i).get(j);
	}

	public Set<Pair<Integer, Integer>> getNextReads(Pair<Integer, Integer> w){
		Set<Pair<Integer, Integer>> nextReads = new HashSet<Pair<Integer, Integer>>();
		Event ev = this.seqTraces[w.x][w.y];
		int indx = this.traceIndex[w.x][w.y];
		for(int i=0; i<this.numThreads; i++) {
			int j = this.getNextSeqEvent(indx, i);
			if( j>= 0) {
				int jj = this.getFromEtoReads(this.trace.get(j), ev.location , true);
				if(jj>0) {
					nextReads.add(new Pair<Integer, Integer>(i, jj));
				}
			}

		}

		return nextReads;
	}

	//Computes which locations appear in each sequential trace
	public void trackLocations() {
		//Compute which locks appear in which thread
		this.seqTraceLockLocations = new ArrayList<Set<Integer>>();
		this.seqTraceGlobalLocations = new ArrayList<Set<Integer>>();
		for(int i=0; i<this.numThreads; i++) {
			Set<Integer> locklocs = new HashSet<Integer>();
			Set<Integer> globallocs = new HashSet<Integer>();
			for(int j=0; j<this.seqTraces[i].length; j++) {
				Event e = this.seqTraces[i][j];
				if(e.kind == EventKind.ACQUIRE_RELEASE) {
					locklocs.add(e.location);
				}
				else if(e.kind == EventKind.WRITE_READ) {
				//else if(e.type == EventType.WRITE) {
					globallocs.add(e.location);
				}
			}
			this.seqTraceLockLocations.add(locklocs);
			this.seqTraceGlobalLocations.add(globallocs);
		}

		//Compute pairwise intersections of lock and global locations
		this.pairWiseGlobalLocations = new ArrayList<List<Set<Integer>>>();

		for(int i=0; i<this.numThreads-1; i++) {
			List<Set<Integer>> g = new ArrayList<Set<Integer>>();
			for(int j=i+1; j<this.numThreads; j++) {	
				Set<Integer> globalintersection = new HashSet<Integer>(this.seqTraceGlobalLocations.get(i));
				globalintersection.retainAll(this.seqTraceGlobalLocations.get(j));
				//System.out.println("globalIntersection between threads " + i + " and " + j + "\t" + globalintersection);
				g.add(globalintersection);

			}
			this.pairWiseGlobalLocations.add(g);
		}
	}


	//True iff every lock acquire is on a free lock
	public boolean respectsLocks() {
		Set<Integer> openLocks = new HashSet<Integer>();
		for(Pair<Integer, Integer> u : this.trace) {
			Event ev = this.seqTraces[u.x][u.y];
			if(ev.type == EventType.ACQUIRE) {
				if(openLocks.contains(ev.location)) {
					System.out.println("Failed on " + this.evStr(u));
					return false;
				}
				else{
					openLocks.add(ev.location);
				}
			}
			else if(ev.type == EventType.RELEASE) {
				openLocks.remove(ev.location);
			}
		}

		return true;
	}


	public boolean matchesObservation(Trace other, Set<Pair<Integer, Integer>> readsToIgnore) {
		Set< Pair<Integer, Integer>> commonReads = this.observation.keySet();
		commonReads.retainAll(other.observation.keySet());
		commonReads.removeAll(readsToIgnore);
		for(Pair<Integer, Integer> r : commonReads) {
			//System.out.println("Testing observations on " + this.evStr(r) + " this obs " + this.observation.get(r));
			Pair<Integer, Integer> w1 = this.observation.get(r);
			Pair<Integer, Integer> w2 = other.observation.get(r);
			if(!w1.equals(w2)) {
				System.out.println("This :" + this.evStr(r) + "\t->\t" + this.evStr(w1));
				System.out.println("Other :" + this.evStr(r) + "\t->\t" + other.evStr(w2));
				boolean appears = this.seqTraces[w2.x].length >= w2.y;
				System.out.println("Other obs appears in current trace? " + appears);
				if(appears) {
					System.out.println("This: Index of read: " + this.traceIndex[r.x][r.y]);
					System.out.println("This: Index of write: " + this.traceIndex[w1.x][w1.y]);
					System.out.println("This: Index of other write: " + this.getIndexofEvent(other.seqTraces[w2.x][w2.y].inputTraceIndex));

					System.out.println("Other: Index of read: " + other.traceIndex[r.x][r.y]);
					System.out.println("Other: Index of write: " + other.traceIndex[w2.x][w2.y]);
					System.out.println("Other: Index of other write: " + other.getIndexofEvent(this.seqTraces[w1.x][w1.y].inputTraceIndex));
				}

				return false;
			}
		}
		return true;
	}

	public int getIndexofEvent(int inputTraceIndex) {

		for(int i=0; i<this.trace.size(); i++) {
			Pair<Integer, Integer> u = this.trace.get(i);
			Event ev = this.seqTraces[u.x][u.y];
			if(ev.inputTraceIndex == inputTraceIndex) {
				return i;
			}
		}

		return -1;
	}


	public boolean matchesLocalThreads(Trace other) {

		if(this.numThreads != other.numThreads) {
			return false;
		}

		for(int i=0; i<this.numThreads; i++) {
			for(int j=0; j< Math.min(this.seqTraces[i].length, other.seqTraces[i].length); j++) {
				if(!this.seqTraces[i][j].equals(other.seqTraces[i][j])) {
					return false;
				}
			}
		}
		return true;
	}

	public boolean consistentWithHB() {

		for(Pair<Integer, Integer> u : this.HBedges.keySet()) {
			if(u.y < this.seqTraces[u.x].length) {
				for(Pair<Integer, Integer> v : this.HBedges.get(u)) {
					if(v.y < this.seqTraces[v.x].length && this.traceIndex[u.x][u.y] > this.traceIndex[v.x][v.y]) {
						System.out.println("HB edge reversed: " + this.evPairStr(u, v));
						return false;
					}
				}
			}
		}

		return true;
	}



	//Computes the critical regions that surround each event, and matches acquires with releases
	//A lock-acquire is not part of its critical section, but a lock-release is
	public void buildCritical() {

		this.openAcquires = new ArrayList<Map<Integer, Map<Integer, Integer>>>();
		this.lockMatch = new ArrayList<BiMap<Integer, Integer>>();
		for(int i=0; i<this.numThreads; i++) {
			Map<Integer, Map<Integer, Integer>> openAcquiresT = new HashMap<Integer, Map<Integer, Integer>>();
			BiMap<Integer, Integer> lockMatchT = HashBiMap.create();
			Map<Integer, Integer> openLocks = new HashMap<Integer,Integer>();
			//System.out.println("Thread: " + i + " length: " + this.seqTraces[i].length);
			for(int j=0; j< this.seqTraces[i].length; j++) {
				Event e = this.seqTraces[i][j];
				//System.out.println(i + " " + j);
				//System.out.println(this.evStr(new Pair<Integer, Integer>(i,j)));
				if(e.type == EventType.ACQUIRE) {
					if(!openLocks.isEmpty()) {
						openAcquiresT.put(j, new HashMap<Integer, Integer>(openLocks));
					}
					openLocks.put(e.location, j);
				}
				else {
					if(e.type == EventType.RELEASE) {
						//key: release, value: acquire
						lockMatchT.put(j, openLocks.get(e.location));
						openLocks.remove(e.location);
					}
					if(!openLocks.isEmpty()) {
						openAcquiresT.put(j, new HashMap<Integer, Integer>(openLocks));
					}
				}
			}
			this.openAcquires.add(openAcquiresT);
			this.lockMatch.add(lockMatchT);
		}

	}


	public int getFromEtoReleases(Pair<Integer, Integer> e, int loc, boolean inclusive) {

		Event ev = this.seqTraces[e.x][e.y];
		if(inclusive && ev.type == EventType.RELEASE && ev.location == loc) {
			return e.y;
		}

		if(!this.mapRelease.get(e.x).containsKey(loc)) {
			return -1;
		}
		//System.out.println("getFromEtoReleases " + e + " loc: " + loc + " inclusive? " + inclusive);
		return this.mapRelease.get(e.x).get(loc).getNext(e.y);

	}

	public int getToEfromAcquires(Pair<Integer, Integer> e, int loc, boolean inclusive) {

		Event ev = this.seqTraces[e.x][e.y];
		if(inclusive && ev.type == EventType.ACQUIRE && ev.location == loc) {
			return e.y;
		}

		if(!this.mapAcquire.get(e.x).containsKey(loc)) {
			return -1;
		}
		//System.out.println("getToEfromAcquires " + e + " loc: " + loc + " inclusive? " + inclusive);
		return this.mapAcquire.get(e.x).get(loc).getPrevious(e.y);
	}

	public int getFromEtoWrites(Pair<Integer, Integer> e, int loc, boolean inclusive) {

		//System.out.println("getFromEtoWrites " + e + " loc: " + loc + " inclusive? " + inclusive);

		Event ev = this.seqTraces[e.x][e.y];
		if(inclusive && ev.type == EventType.WRITE && ev.location == loc) {
			return e.y;
		}

		if(!this.mapWrite.get(e.x).containsKey(loc)) {
			//System.out.println("Location not found");
			return -1;
		}


		//System.out.println("getFromEtoWrites " + e + " loc: " + loc + " inclusive? " + inclusive);
		return this.mapWrite.get(e.x).get(loc).getNext(e.y);

	}


	public int getToEfromWrites(Pair<Integer, Integer> e, int loc, boolean inclusive) {

		Event ev = this.seqTraces[e.x][e.y];
		if(inclusive && ev.type == EventType.WRITE && ev.location == loc) {
			return e.y;
		}

		if(!this.mapWrite.get(e.x).containsKey(loc)) {
			return -1;
		}
		//System.out.println("getToEfromWrites " + e + " loc: " + loc + " inclusive? " + inclusive);
		return this.mapWrite.get(e.x).get(loc).getPrevious(e.y);

	}


	public int getToEfromReads(Pair<Integer, Integer> e, int loc, boolean inclusive) {

		Event ev = this.seqTraces[e.x][e.y];
		if(inclusive && ev.type == EventType.READ && ev.location == loc) {
			return e.y;
		}

		if(!this.mapRead.get(e.x).containsKey(loc)) {
			return -1;
		}
		//System.out.println("getToEfromWrites " + e + " loc: " + loc + " inclusive? " + inclusive);
		return this.mapRead.get(e.x).get(loc).getPrevious(e.y);

	}


	public int getFromEtoReads(Pair<Integer, Integer> e, int loc, boolean inclusive) {

		Event ev = this.seqTraces[e.x][e.y];
		if(inclusive && ev.type == EventType.READ && ev.location == loc) {
			return e.y;
		}

		if(!this.mapRead.get(e.x).containsKey(loc)) {
			return -1;
		}
		//System.out.println("getFromEtoReads " + e + " loc: " + loc + " inclusive? " + inclusive);
		return this.mapRead.get(e.x).get(loc).getNext(e.y);
	}

	public Event getEvent(Pair<Integer, Integer> u) {
		return this.seqTraces[u.x][u.y];
	}

	public Pair<Integer, Integer> getObservation(Pair<Integer, Integer> u){
		return this.observation.get(u);
	}

	public void writeTrace(String filenameOut) {

		System.out.println("Writing trace to" + filenameOut);

		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(filenameOut);
			bw = new BufferedWriter(fw);

			bw.write("Num of threads: " + this.numThreads + "\n");
			String s = "";
			for(int i=0; i<this.numThreads; i++) {
				s = s + this.seqTraces[i].length;
				if(i<this.numThreads - 1) {
					s = s + ",";
				}
			}
			bw.write("Num of events: " + s + "\n");
			for(Pair<Integer, Integer> u  : this.trace ) {
				Event e = this.seqTraces[u.x][u.y];
				String evType = null;
				if(e.type == EventType.ACQUIRE) {
					evType = "acq";
				}
				else if(e.type == EventType.RELEASE) {
					evType = "rel";
				}
				else if(e.type == EventType.NOOP) {
					evType = "noop";
				}
				else if(e.type == EventType.READ) {
					evType = "r";
				}
				else if(e.type == EventType.WRITE) {
					evType = "w";
				}
				String evStr = "";
				for(int i=0; i<this.numThreads; i++) {
					if(i==u.x) {
						evStr += evType + ":" + e.location;
					}
					if(i<this.numThreads - 1) {
						evStr += ",";
					}
				}
				evStr += "|" + e.inputTraceIndex;
				bw.write(evStr + "\n");
			}

			for(Pair<Integer, Integer> u : this.HBedges.keySet()) {
				for(Pair<Integer, Integer> v : this.HBedges.get(u)) {
					bw.write(u.x+"."+u.y + "-" + v.x+"."+v.y + "\n");
				}
			}

			bw.flush();

		} catch (Exception ex) {ex.printStackTrace();}
		finally {
			try {
				fw.close();
				bw.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}




	//----------------Printers for debugging------------------------
	public void printEventRenames() {
		System.out.println("==============eventRenames============");
		for(int i=0; i<this.numThreads; i++) {
			System.out.println("--------- " + "Thread " + i + " ---------");
			for(int j=0; j<this.eventRenameList.get(i).size(); j++) {
				System.out.println(j + " -> " + this.eventRenameList.get(i).get(j));
			}
		}
	}
	public void printSeqTraces() {
		System.out.println("==================================");
		System.out.println("Trace.seqTraces");
		System.out.println("==================================");
		for(int i=0; i< this.seqTraces.length; i++) {
			System.out.println("------ SeqTrace " + i + "------");
			for(int j=0; j<this.seqTraces[i].length; j++) {
				System.out.println(String.format("%5s", j) + ")\t" + this.seqTraces[i][j]);
			}
		}
	}

	public void printSeqTrace(int i, int j) {
		System.out.println("==================================");
		System.out.println("Trace.seqTrace " + i);
		System.out.println("==================================");
		for(int k=0; k< (j < this.seqTraces[i].length? j : this.seqTraces[i].length) ; k++) {
			String suff = "";
			Pair<Integer, Integer> u =new Pair<Integer, Integer>(i,k);
			if(this.observation.containsKey(u)) {
				suff = " observes " + this.evStr(this.observation.get(u));
			}
			System.out.println(this.evStr(u) + suff);
		}

	}
	public void printTraceIndex() {
		System.out.println("==================================");
		System.out.println("Trace.traceIndex");
		System.out.println("==================================");
		for(int i=0; i< this.seqTraces.length; i++) {
			System.out.println("------ SeqTrace " + i + "------");
			for(int j=0; j<this.seqTraces[i].length; j++) {
				System.out.println(String.format("%5s", j) + ")\t" + String.format("%5s", this.traceIndex[i][j]));
			}
		}
	}
	public void printTrace() {
		System.out.println("==================================");
		System.out.println("Trace.trace");
		System.out.println("==================================");
		for(int i=0; i< this.trace.size(); i++) {
			Pair<Integer, Integer> u = this.trace.get(i);
			System.out.println(String.format("%5s", i) + ")\t" + String.format("%5s", u) + "\t" +
					this.seqTraces[u.x][u.y]);
		}
	}

	public void printTrace(int [] lastEvent) {
		System.out.println("==================================");
		System.out.println("Trace.trace");
		System.out.println("==================================");
		for(int i=0; i< this.trace.size(); i++) {
			Pair<Integer, Integer> u = this.trace.get(i);
			if(u.y <= lastEvent[u.x]) {
				System.out.println(String.format("%5s", i) + ")\t" + String.format("%5s", u) + "\t" +
						this.seqTraces[u.x][u.y]);
			}

		}
	}
	public void printObservation() {
		System.out.println("==================================");
		System.out.println("Trace.observation");
		System.out.println("==================================");
		for(Pair<Integer, Integer> r : this.observation.keySet()) {
			Pair<Integer, Integer> w = this.observation.get(r);
			System.out.println(r.toString() + " " + this.seqTraces[r.x][r.y].toString() +"\t->\t" +
					w.toString() + " " + this.seqTraces[w.x][w.y].toString());
		}
	}
	public void printReverseObservation() {
		System.out.println("==================================");
		System.out.println("Trace.reverseObservation");
		System.out.println("==================================");
		for(Pair<Integer, Integer> w : this.reverseObservation.keySet()) {
			System.out.print(w.toString() + " " + this.seqTraces[w.x][w.y].toString() +"\t->\t");
			for(Pair<Integer, Integer> r: this.reverseObservation.get(w)) {
				System.out.print(r.toString() + " " + this.seqTraces[r.x][r.y].toString());
			}
			System.out.println("");
		}
	}
	public void printHBedges() {
		System.out.println("==================================");
		System.out.println("Trace.HBedges");
		System.out.println("==================================");
		for(Pair<Integer, Integer> u : this.HBedges.keySet()) {
			for(Pair<Integer, Integer> v : this.HBedges.get(u)) {
				System.out.println(u + " -> " + v);
			}
		}
	}
	public void printLockMatch() {
		System.out.println("==================================");
		System.out.println("Trace.lockMatch");
		System.out.println("==================================");
		for(int i=0; i< this.numThreads; i++) {
			System.out.println("Thread " + i);
			System.out.println("------------");
			for(int rel : this.lockMatch.get(i).keySet()) {
				int acq = this.lockMatch.get(i).get(rel);
				System.out.println(new Pair<Integer, Integer>(i, acq).toString() + " " + this.seqTraces[i][acq].toString() +"\t->\t" +
						new Pair<Integer, Integer>(i, rel).toString() + " " + this.seqTraces[i][rel].toString());
			}
		}
	}
	public void printseqTraceLockLocations() {
		System.out.println("==================================");
		System.out.println("Trace.seqTraceLockLocations");
		System.out.println("==================================");
		for(int i=0; i< this.numThreads; i++) {
			System.out.println("------------");
			System.out.println("Thread " + i);
			System.out.println("------------");
			System.out.println(this.seqTraceLockLocations.get(i).toString());
		}
	}
	public void printPairWiseGlobalLocations() {
		System.out.println("==================================");
		System.out.println("Trace.pairWiseGlobalLocations");
		System.out.println("==================================");
		for(int i=0; i< this.numThreads; i++) {
			for(int j=i+1; j< this.numThreads; j++) {
				System.out.println("------------");
				System.out.println("Threads " + i + "," + j);
				System.out.println("------------");
				System.out.println(this.seqTraceGlobalLocations.get(i).toString());
			}

		}
	}
	public void printseqTraceGlobalLocations() {
		System.out.println("==================================");
		System.out.println("Trace.seqTraceGlobalLocations");
		System.out.println("==================================");
		for(int i=0; i< this.numThreads; i++) {
			System.out.println("------------");
			System.out.println("Thread " + i);
			System.out.println("------------");
			System.out.println(this.seqTraceGlobalLocations.get(i).toString());
		}
	}
	public void printListMapMap(String s, List<Map<Integer, Map<Integer, Integer>>> m) {
		System.out.println("==================================");
		System.out.println(s);
		System.out.println("==================================");
		for(int i=0; i< m.size(); i++) {
			System.out.println("------------");
			System.out.println("Thread " + i);
			System.out.println("------------");
			for(int j: m.get(i).keySet() ) {
				System.out.print(new Pair<Integer, Integer>(i, j).toString() + " " + this.seqTraces[i][j].toString() +"\t->");
				for(int l : m.get(i).get(j).keySet()) {
					System.out.println(new Pair<Integer, Integer>(i, m.get(i).get(j).get(l)).toString() + " " +
							this.seqTraces[i][m.get(i).get(j).get(l)].toString());
				}
				System.out.println("");
			}
		}
	}

	public void printAll() {
		this.printSeqTraces();
		this.printTraceIndex();
		this.printObservation();
		this.printReverseObservation();
		this.printHBedges();
		this.printLockMatch();
		this.printseqTraceLockLocations();
		this.printseqTraceGlobalLocations();
		this.printPairWiseGlobalLocations();
		//this.printAllEventMaps();
		//this.printLocksInCritical();
	}

	public String evStr(Pair<Integer, Integer> u) {
		return u.toString() + " " + this.seqTraces[u.x][u.y].toString();
	}



}
