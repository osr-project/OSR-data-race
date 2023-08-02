import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

public class RaceDetection {

	public Trace trace;
	public PartialOrderTrace po;


	public Pair<Integer, Integer> centerU;
	public Pair<Integer, Integer> remoteV;
	public int [] seqTlengths;

	//public int [] lastEvent;
	public String filename;


	public boolean printRaces = false;

	//True if the causal past closed remote lock on thread i
	public boolean closedLock[];
	public boolean constructWitness;

	Map<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>> races;
	List<SortedSet<Integer>> racyEvents;

	public Set<Integer> racyEventsAdded = new HashSet<>();

	public Set<Integer> racyCodeLines = new HashSet<>();

	public RaceDetection(String filename, boolean dropHB, boolean constructWitness) {
		this.filename = filename;
//		System.out.println("Parsing trace " + this.filename);
		this.trace = new Trace(filename, dropHB, true, true);
		this.init(constructWitness);
		this.po = new PartialOrderTrace(this.trace);
	}

	public RaceDetection(Trace trace, boolean constructWitness) {
		this.trace = trace;
		this.init(constructWitness);
		this.po = new PartialOrderTrace(this.trace);

	}

	public RaceDetection(Trace trace, PartialOrderTrace po, boolean constructWitness) {
		this.trace = trace;
		this.init(constructWitness);
		this.po = po;
		//System.arraycopy( lastEvent, 0, this.lastEvent, 0, lastEvent.length );
	}

	public void init(boolean constructWitness) {
		this.constructWitness = constructWitness;
		this.seqTlengths = new int[this.trace.numThreads];
		this.races = new HashMap<Pair<Integer, Integer>, Set<Pair<Integer, Integer>>>();
		this.racyEvents = new ArrayList<SortedSet<Integer>>();
		for(int i=0; i<this.trace.numThreads; i++) {
			this.racyEvents.add(new TreeSet<Integer>());
			this.seqTlengths[i] = this.trace.seqTraces[i].length;
		}
		//this.lastEvent = new int[this.trace.numThreads];
		this.closedLock = new boolean[this.trace.numThreads];
	}



	public void writeResults(String filenameOut, String elapsedTime) {

		System.out.println("Writing " + this.numRaces() + " races to file " + filenameOut);


		BufferedWriter bw = null;
		FileWriter fw = null;
		try {
			fw = new FileWriter(filenameOut);
			bw = new BufferedWriter(fw);
			bw.write("Input: " + this.filename + "\n");
			bw.write("constructWitness: " + this.constructWitness + "\n");
			bw.write("Time: " + elapsedTime + "\n");
			bw.write("numRaces: " + this.numRaces() + "\n");
			bw.write("Races:\n");
			for(Pair<Integer, Integer> u : this.races.keySet()) {
				for(Pair<Integer, Integer> v : this.races.get(u)) {
					bw.write(this.trace.evStr(u) + "\t-\t" + this.trace.evStr(v) + "\n");
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

	public int getNextConflicting(Pair<Integer, Integer> u, int current, int remote, boolean inclusive) {


		int j = current;
		Pair<Integer, Integer> cur = new Pair<Integer, Integer>(remote, j);
		Event ev = this.trace.seqTraces[u.x][u.y];
		if(ev.type == EventType.READ) {
			j = this.trace.getFromEtoWrites(cur, ev.location, inclusive);

			if(j<0) {
				return -1;
			}
		}
		else {
			int j1 = this.trace.getFromEtoWrites(cur, ev.location, inclusive);
			int j2 = this.trace.getFromEtoReads(cur, ev.location, inclusive);

			if(j1<0 && j2<0) {
				return -1;
			}
			if(j1<0) {
				j=j2;
			}
			else if(j2<0) {
				j = j1;
			}
			else {
				j = Integer.min(j1, j2);
			}
		}

		return j;
	}



	public void pause() {
		try {System.in.read();} catch(Exception e) {}
	}


	// Construct the event set for checking a race on (this.centerU, this.remoteV).
	// This will be stored in this.po.activePrefixes
	public void buildEventSet() {
		this.po.pushActivePredecessors(this.centerU.x, this.centerU.y-1);
		this.po.pushActivePredecessors(this.remoteV.x, this.remoteV.y-1);

		boolean converged = false;

		while(!converged) {
			converged = true;
			for(int i=0; i<this.trace.numThreads; i++) {
				if(i!=this.centerU.x && i!= this.remoteV.x) {
					Pair<Integer, Integer> e = new Pair<Integer, Integer>(i, this.po.activePrefixes[i]);
					int acq = this.trace.getTopLocalGuardingAcquire(e);
					if(acq >= 0) {
						Pair<Integer, Integer> acquire = new Pair<Integer, Integer>(i,acq);
						int rel = this.trace.getMatchingRelease(acquire);
						if(rel > this.po.activePrefixes[i]) { //This check should be redundant if we did not have hb edges in the trace
							this.po.pushActivePredecessors(i, rel);
							converged = false;
						}
					}
				}
			}
		}
	}



	//Get the open locks and acquires of centerU and remoteV
	public Map<Integer, Pair<Integer, Integer>> getOpenLocksAndAcquires(){
		Map<Integer, Pair<Integer, Integer>> openLocksAndAcquires = new HashMap<Integer, Pair<Integer, Integer>>();

		Map<Integer, Integer> locksToAcquires = this.trace.getLocalGuardingLocksAndAcquiers(this.centerU);
		for(int l: locksToAcquires.keySet()) {
			openLocksAndAcquires.put(l, new Pair<Integer, Integer>(this.centerU.x, locksToAcquires.get(l)));
		}

		locksToAcquires = this.trace.getLocalGuardingLocksAndAcquiers(this.remoteV);
		for(int l: locksToAcquires.keySet()) {
			openLocksAndAcquires.put(l, new Pair<Integer, Integer>(this.remoteV.x, locksToAcquires.get(l)));
		}

		return openLocksAndAcquires;
	}


	//Insert orderings from all releases to the acquires guarding the focal pair (this.centerU, this.remoteV). Returns true iff it does not fail
	public boolean makeRespect() {

		Map<Integer, Pair<Integer, Integer>> openLocksAndAcquires = this.getOpenLocksAndAcquires();

		for(int l : openLocksAndAcquires.keySet()) {
			Pair<Integer, Integer> acq = openLocksAndAcquires.get(l);
			for(int i=0; i<this.trace.numThreads; i++) {
				if(i != acq.x) {
					//Pair<Integer, Integer> u = new Pair<Integer, Integer>(i, this.lastEvent[i]);
					Pair<Integer, Integer> u = new Pair<Integer, Integer>(i, this.po.activePrefixes[i]);
					int a = this.trace.getToEfromAcquires(u, l, false);
					if(a > 0) {
						Pair<Integer, Integer> lastA = new Pair<Integer, Integer>(i, a);
						int r = this.trace.getMatchingRelease(lastA);
						Pair<Integer, Integer> lastR = new Pair<Integer, Integer>(i, r);
//						System.out.println("makeRespect, Adding edge " + this.trace.evPairStr(lastR, acq));
						this.po.addEdgeAndSaturate(lastR, acq, false);
						if(this.po.edgeCreatedCycle) {
							return false;
						}
					}
				}
			}
		}
		return true;
	}

	public String race2String(Pair<Integer, Integer> u, Pair<Integer, Integer> v) {
		return this.trace.evStr(u) + "\t-\t" + this.trace.evStr(v);
	}




	public void addRace(Pair<Integer, Integer> u, Pair<Integer, Integer> v) {

		if(this.printRaces) {
			System.out.println("Race: " + this.race2String(u, v));
		}


		this.racyEvents.get(u.x).add(u.y);
		this.racyEvents.get(v.x).add(v.y);

		if(!this.races.containsKey(u)) {
			this.races.put(u, new HashSet<Pair<Integer, Integer>>());
		}
		this.races.get(u).add(v);

	}



	public boolean hasRace(Pair<Integer, Integer> u, Pair<Integer, Integer> v) {
		return (this.races.containsKey(u) && this.races.get(u).contains(v)) || (this.races.containsKey(v) && this.races.get(v).contains(u)); 

	}


	public int numRaces() {
		int n = 0;
		for(Pair<Integer, Integer> u : this.races.keySet()) {
			n += this.races.get(u).size();
		}
		return n;
	}




	public int getTop(int[] frontier, int []stops) {

		//Arrays.sort(months, (a, b) -> a.length() - b.length());

		List<Integer> sortedFrontier = new ArrayList<Integer>();
		for(int i=0; i<frontier.length; i++) {
			if(stops[i] >= frontier[i]) {
				sortedFrontier.add(i);
			}
		}

		if(sortedFrontier.isEmpty()) {
			return -1;
		}
		sortedFrontier.sort((a,b) -> this.trace.traceIndex[a][frontier[a]] - this.trace.traceIndex[b][frontier[b]]);

		Pair<Integer, Integer> top = new Pair<Integer, Integer>(sortedFrontier.get(0), frontier[sortedFrontier.get(0)]);
		for(int i=1; i < sortedFrontier.size(); i++) {
			Pair<Integer, Integer> next = new Pair<Integer, Integer>(sortedFrontier.get(i), frontier[sortedFrontier.get(i)]);
			if(this.po.existsEdge(next, top)) {
				top.x = next.x;
				top.y = next.y;
			}
		}

		return top.x;
	}


	public List<Pair<Integer, Integer>> linearize(int [] stops){
		
		//Pair<Integer, Integer> myFrom = new Pair<Integer, Integer>(2,30);
		//Pair<Integer, Integer> myTo = new Pair<Integer, Integer>(6,0);
		//System.out.println("Linearize. PO edges: ");
		//this.po.partialOrder.printPOArrays();
		//System.out.println("exists edge " + this.trace.evPairStr(myFrom, myTo) + " ? " + this.po.existsEdge(myFrom, myTo));
		
		List<Pair<Integer, Integer>> newTrace =  new ArrayList<Pair<Integer, Integer>> ();
		int [] frontier = new int[this.trace.numThreads];

		int top;
		while((top = this.getTop(frontier, stops)) >= 0) {
			newTrace.add(new Pair<Integer, Integer>(top, frontier[top]));
			frontier[top]++;
		}

		return newTrace;
	}




	public Trace getWitness(Pair<Integer, Integer> u, Pair<Integer, Integer> v, int[] lastEvents) {

		//System.out.println("Getting witness on " + this.trace.evStr(u) + " and " + this.trace.evStr(v));
		//System.out.println("Event set is: " + Arrays.toString(lastEvents));
		//System.out.println("Partial Order is ");
		//this.po.partialOrder.printPO();

		int [] stops = new int[this.trace.numThreads]; 
		for(int i=0; i<lastEvents.length; i++) {
			if(lastEvents[i] > 0) {
				stops[i] = lastEvents[i];
			}
			else {
				stops[i]=0;
			}
		}

		stops[u.x] = u.y-1;
		stops[v.x] = v.y-1;

		List<Pair<Integer, Integer>> a = this.linearize(stops);
		a.add(u);
		a.add(v);

		Trace tr = new Trace(this.trace, a ,false);
		if(tr.seqTraces[u.x].length < u.y -1 || tr.seqTraces[v.x].length < v.y -1) {
			System.out.println("ERROR: Witness trace does not contain racing event");
			this.pause();
		}
		return tr;
	}



	//Try ordering unordered conflicting events in the trace order. True if succeed without creating cycle
	public boolean tryResolve() {

		int [] stops = new int[this.trace.numThreads]; 
		for(int i=0; i < this.po.activePrefixes.length; i++) {
			if(this.po.activePrefixes[i] > 0) {
				stops[i] = this.po.activePrefixes[i];
			}
			else {
				stops[i]=0;
			}
		}

		int [] frontier = new int[this.trace.numThreads];

		int top;
		//Get the first element among the frontier to be appended to the trace (earliest events first)
		while((top = this.getTop(frontier, stops)) >= 0) {
			Pair<Integer, Integer> topU = new Pair<Integer, Integer>(top, frontier[top]);
			for(int i=0; i< this.po.activePrefixes.length; i++) {
				if( i!= top) {

					int j = this.getNextConflicting(topU, frontier[i], i, true);
					if(j>=0) {
						Pair<Integer, Integer> nextV = new Pair<Integer, Integer>(i, j);
						if(this.po.unordered(topU, nextV)) {
							//We are forcing the top element before its next element in thread i that top conflicts with
							Pair<Integer, Integer> from, to;
							if(this.trace.seqTraces[topU.x][topU.y].inputTraceIndex < this.trace.seqTraces[nextV.x][nextV.y].inputTraceIndex) {
								from = topU;
								to = nextV;
							}
							else {
								from = nextV;
								to = topU;
							}
							this.po.addEdgeAndSaturate(from, to, false);
							if(this.po.edgeCreatedCycle) {
								return false;
							}
						}
					}

				}
			}
			frontier[top]++;
		}
		if(this.constructWitness) {
			Trace t = this.getWitness(this.centerU, this.remoteV, this.po.activePrefixes);
			t.printTrace();
		}

//		System.out.println("Race found: " + this.trace.evPairStr(this.centerU, this.remoteV));
		return true;
	}



	public boolean isRace() {

		// Step 1. Construct the event set
		this.buildEventSet();
		if(this.po.activePrefixes[this.centerU.x] >= this.centerU.y || this.po.activePrefixes[this.remoteV.x] >= this.remoteV.y) {
			return false;
		}

		//	Step 2. Make the po respect the event set, meaning that all open acquires are ordered after conflicting releases (and saturating)
		// !passed means that a cycle was created
		boolean passed = this.makeRespect();
		if(!passed) {
			return false;
		}

		// Step 3. Try realizing the pair as a race by resolving in trace order the order of conflicting events
		// In original m2 there is an optimization step: if the edges added in step 2 do not reverse the order of conflicting events compared to their order in the input trace,
		// we don't need to try step 3. We know it will succeed. This is not present here, but we can consider moving it in later
		return tryResolve();

	}


	public void getRacyEvents() {
		this.po.buildPORF();

		for(int i=0; i<this.trace.trace.size(); i++) {
			Pair<Integer, Integer> u = this.trace.trace.get(i);
			int uThId = u.x;
			int uInThId = u.y;
			Event eventU = this.trace.seqTraces[uThId][uInThId];
			int uAuxId = eventU.inputTraceIndex;

			boolean foundRace = false;

			for(int j=0; j<this.trace.numThreads; j++) {
				if (foundRace) break;

				if(!u.x.equals(j)) {
					//int y =  this.getNextConflicting(u, this.po.getPredecessor(u, j)+1, j, true);
					int y =  this.getNextConflicting(u, this.po.getPredecessor(new Pair<Integer, Integer>(u.x, u.y-1), j)+1, j, true); // y = localId of v in thread j that's conflicting with u

					while(y > 0 && i > this.trace.traceIndex[j][y]) {
						Pair<Integer, Integer> v = new Pair<Integer, Integer>(j, y);

						//if(!this.po.existsEdge(v, u) && !this.trace.protectedSameLock(u, v)) {
						if(!this.trace.protectedSameLock(u, v)) {
							PartialOrderTrace temp = new PartialOrderTrace(this.po);
							this.centerU = u;
							this.remoteV = v;
							if(this.isRace()){
								this.racyEventsAdded.add(uAuxId);
								this.racyCodeLines.add(eventU.codeLine);
								this.po = temp;
								foundRace = true;
								break;
							}
							this.po = temp;
						}
						// add race detection here
						y = this.getNextConflicting(u, y+1, j, true);
					}
				}
			}
		}

	}

	public boolean checkRace(){
		if(this.centerU.x.equals(this.remoteV.x) || this.trace.protectedSameLock(this.centerU, this.remoteV)){
			return false;
		}

		return this.isRace();
	}


	public void getRaces() {
		this.po.buildPORF();
		//this.po.partialOrder.printPOArrays();
		//this.po.partialOrder.printPO();
		Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> potentialRaces = this.getPotentialRaces();
		System.out.println("Number of potential races: " + potentialRaces.size());
		for(Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> potentialRace : potentialRaces) {
			this.centerU = potentialRace.x;
			this.remoteV = potentialRace.y;
			PartialOrderTrace temp = new PartialOrderTrace(this.po);

			//this.po.partialOrder.printPO();
			//System.out.println("----------------");
			//temp.partialOrder.printPO();
			//System.out.println("Equal? " + this.po.partialOrder.equals(temp.partialOrder));
			//this.pause();
			
			if(this.isRace()) {
				this.addRace(potentialRace.x, potentialRace.y);
			}
			this.po = temp;
		}
	}


	//Returns pairs of events that are potential data races, to be checked later by RaceDecision
	private Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> getPotentialRaces(){
		Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> S = new HashSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();

		for(int i=0; i<this.trace.trace.size(); i++) {
			Pair<Integer, Integer> u = this.trace.trace.get(i);
			int uThId = u.x;
			int uInThId = u.y;
			Event eventU = this.trace.seqTraces[uThId][uInThId];
			int uAuxId = eventU.inputTraceIndex;

			for(int j=0; j<this.trace.numThreads; j++) {
				if(!u.x.equals(j)) {
					//int y =  this.getNextConflicting(u, this.po.getPredecessor(u, j)+1, j, true);
					int y =  this.getNextConflicting(u, this.po.getPredecessor(new Pair<Integer, Integer>(u.x, u.y-1), j)+1, j, true);
					while(y > 0 && i > this.trace.traceIndex[j][y]) {
						Pair<Integer, Integer> v = new Pair<Integer, Integer>(j, y);

						//if(!this.po.existsEdge(v, u) && !this.trace.protectedSameLock(u, v)) {
						if(!this.trace.protectedSameLock(u, v)) {
							S.add(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(u, v));
						}
						y = this.getNextConflicting(u, y+1, j, true);
					}
				}
			}

		}
		return S;
	}

}


