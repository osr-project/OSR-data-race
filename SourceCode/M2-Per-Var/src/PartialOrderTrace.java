

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

// A partial order over nodes of the form p=(x,y), where p.x is the thread id and p.y is the local (incremental) id


public class PartialOrderTrace {
	public Trace trace;
	public PartialOrder partialOrder;
	public int[] activePrefixes; // Stores the latest event of each thread that is considered in our partial order -- used for saturation



	public boolean globalFlag;
	public boolean addedEdge;
	public boolean edgeCreatedCycle;

	public boolean modified;
	public boolean reversedTraceOrder, reversedTraceOrderOnConflicting;

	public PartialOrderTrace(Trace trace) {
		this.trace = trace;
		int[] lengths = new int[trace.numThreads];
		for(int i=0; i<lengths.length; i++) {
			lengths[i] = this.trace.seqTraces[i].length;
		}
		this.partialOrder = new PartialOrder(lengths);
		this.activePrefixes = new int[this.partialOrder.width];
	}


	public PartialOrderTrace(PartialOrderTrace other) {
		this.trace = other.trace;
		this.partialOrder = new PartialOrder(other.partialOrder);
		this.activePrefixes = new int[this.partialOrder.width];
	}


	//Get the earliest successor of p on thread i - returns -1 if no successor
	public int getSuccessor(Pair<Integer, Integer> p, int i) {
		return this.partialOrder.getSuccessor(p, i);
	}

	//Get the latest predecessor of p on thread i - returns -1 if no predecessor
	public int getPredecessor(Pair<Integer, Integer> p, int i) {
		return this.partialOrder.getPredecessor(p, i);
	}



	public boolean unordered(Pair<Integer, Integer> u, Pair<Integer, Integer> v) {
		return this.partialOrder.unordered(u,v);
	}


	// Returns the earliest successor p has on each thread
	public int[] getSuccessors(Pair<Integer, Integer> p) {
		return this.partialOrder.getSuccessors(p);
	}


	// Returns the latest predecessor p has on each thread
	public int[] getPredecessors(Pair<Integer, Integer> p) {
		return this.partialOrder.getPredecessors(p);
	}



	public boolean existsEdge(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
		return this.partialOrder.existsEdge(from, to);
	}


	//Adds edge and takes transitive closure. Assumes that edge does not create cycle
	//Returns the set of edges added due to transitive closure.
	public Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> addEdge(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
		return this.partialOrder.addEdge(from, to);
	}





	// Get the last w(tid, loc) that is ordered before u -- inclusive = true means that possibly w(tid,loc)=u
	public Pair<Integer, Integer> getLastWriteBefore(Pair<Integer, Integer> u, int tid, int loc, boolean inclusive){
		Pair<Integer, Integer> e = new Pair<Integer, Integer>(tid, this.getPredecessor(u, tid));
		return new Pair<Integer, Integer>(tid, this.trace.getToEfromWrites(e, loc, inclusive));
	}

	//Get the first w(tid, loc) that is ordered after u -- inclusive = true means that possibly w(tid,loc)=u
	public Pair<Integer, Integer> getFirstWriteAfter(Pair<Integer, Integer> u, int tid, int loc, boolean inclusive){
		Pair<Integer, Integer> e = new Pair<Integer, Integer>(tid, this.getSuccessor(u, tid));
		return new Pair<Integer, Integer>(tid, this.trace.getFromEtoWrites(e, loc, inclusive));
	}


	//Get the last r(tid, loc) that is ordered before u -- inclusive = true means that possibly r(tid,loc)=u
	public Pair<Integer, Integer> getLastReadBefore(Pair<Integer, Integer> u, int tid, int loc, boolean inclusive){
		Pair<Integer, Integer> e = new Pair<Integer, Integer>(tid, this.getPredecessor(u, tid));
		return new Pair<Integer, Integer>(tid, this.trace.getToEfromReads(e, loc, inclusive));
	}

	//Get the first r(tid, loc) that is ordered after u -- inclusive = true means that possibly r(tid,loc)=u
	public Pair<Integer, Integer> getFirstReadAfter(Pair<Integer, Integer> u, int tid, int loc, boolean inclusive){
		Pair<Integer, Integer> e = new Pair<Integer, Integer>(tid, this.getSuccessor(u, tid));
		return new Pair<Integer, Integer>(tid, this.trace.getFromEtoReads(e, loc, inclusive));
	}


	//Get the last acq(tid, loc) that is ordered before u -- inclusive = true means that possibly acq(tid,loc)=u
	public Pair<Integer, Integer> getLastAcquireBefore(Pair<Integer, Integer> u, int tid, int loc, boolean inclusive){
		Pair<Integer, Integer> e = new Pair<Integer, Integer>(tid, this.getPredecessor(u, tid));
		return new Pair<Integer, Integer>(tid, this.trace.getToEfromAcquires(e, loc, inclusive));
	}

	//Get the first rel(tid, loc) that is ordered after u -- inclusive = true means that possibly rel(tid,loc)=u
	public Pair<Integer, Integer> getFirstReleaseAfter(Pair<Integer, Integer> u, int tid, int loc, boolean inclusive){
		Pair<Integer, Integer> e = new Pair<Integer, Integer>(tid, this.getSuccessor(u, tid));
		return new Pair<Integer, Integer>(tid, this.trace.getFromEtoReleases(e, loc, inclusive));
	}


	//Constructs the po-rf order by inserting the rf edges and the hb edges implied by forks/joins
	public void buildPORF() {

		// rf on remote writes
		for(Pair<Integer, Integer> r : this.trace.remoteObservation.keySet()) {
			//System.out.println("Adding rf edge " + this.trace.evPairStr(this.trace.remoteObservation.get(r), r));
			this.addEdge(this.trace.remoteObservation.get(r),r);
		}

		// hb edges
		for(Pair<Integer, Integer>  from : this.trace.HBedges.keySet()) {
			for(Pair<Integer, Integer> to : this.trace.HBedges.get(from)) {
				//System.out.println("Adding hb edge: " + this.trace.evPairStr(from, to));
				this.addEdge(from, to);
			}
		}
	}



	public void pushActivePrefix(Pair<Integer, Integer> u) {
		if(this.activePrefixes[u.x] < u.y) {
			this.activePrefixes[u.x] = u.y;
			this.modified = true;
		}
	}

	public void pushActivePrefix(int x, int y) {
		if(this.activePrefixes[x] < y) {
			this.activePrefixes[x] = y;
			this.modified = true;
		}
	}

	//Pushes this.activePrefixes to the predecessors of u (with u included)
	public void pushActivePredecessors(Pair<Integer, Integer> u) {
		this.pushActivePredecessors(u.x, u.y);
	}

	public void pushActivePredecessors(int x, int y) {
		this.pushActivePrefix(x, y);
		int [] predecessors = this.getPredecessors(new Pair<Integer, Integer>(x, y));
		for(int i=0; i<predecessors.length; i++) {
			this.pushActivePrefix(i, predecessors[i]);
		}
	}



	//Return the set of mo and fr edges that saturation implies but are not present -- if the set is empty, the partial order is saturated
	public Set<Pair<Pair<Integer, Integer>,Pair<Integer, Integer>>> saturationEdges() {
		Set<Pair<Pair<Integer, Integer>,Pair<Integer, Integer>>> S = new HashSet<Pair<Pair<Integer, Integer>,Pair<Integer, Integer>>>();
		for(Pair<Integer, Integer> r : this.trace.observation.keySet()) {
			int loc = this.trace.getLocation(r);
			Pair<Integer, Integer> w = this.trace.observation.get(r);
			if(this.activePrefixes[r.x] >= r.y) {
				for(int i=0; i<this.partialOrder.width; i++) {
					Pair<Integer, Integer> ww = this.getLastWriteBefore(r, i, loc, false);
					if(this.activePrefixes[i] >= ww.y && !ww.equals(w) && !this.existsEdge(ww,w)) {//Implied mo
						S.add(new Pair<Pair<Integer, Integer>,Pair<Integer, Integer>>(ww,w));
					}
					Pair<Integer, Integer> www = this.getFirstWriteAfter(w, i, loc, false);
					if(this.activePrefixes[i] >= www.y && !this.existsEdge(r,www)) {//Implied fr
						S.add(new Pair<Pair<Integer, Integer>,Pair<Integer, Integer>>(r,www));
					}
				}
			}
		}
		return S;
	}

	//Saturate the partial order, considering only events up to the active prefix of each thread
	public void saturate() {

		Set<Pair<Pair<Integer, Integer>,Pair<Integer, Integer>>> S = this.saturationEdges();

		do {
			for(Pair<Pair<Integer, Integer>,Pair<Integer, Integer>> edge : S) {
				this.addEdge(edge.x, edge.y);
			}
			S = this.saturationEdges();
		}
		while(!S.isEmpty());

	}


	//Adds an edge and performs a saturation on the new PO. Assumes that PO without the edge is already saturated
	//Returns false if a cycle is created
	// isObsEdge=true if this is an RF edge -- the current implementation likely always has isObsEdge=False
	public boolean addEdgeAndSaturate(Pair<Integer, Integer> from, Pair<Integer, Integer> to, boolean isObsEdge) {

		//System.out.println("addEdgeAndClose");
		this.edgeCreatedCycle = false;
		//System.out.println("addEdgeAndClose" + this.trace.evStr(from) + " -> " + this.trace.evStr(to) + " isObsEdge? " + isObsEdge);

		//Stack<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> s = new Stack<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();
		Queue<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> s = new LinkedList<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();

		s.add(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(from, to));
		if(isObsEdge) {
			s.addAll(this.resolveObservationsOfObEdge(from, to));
		}

		this.addedEdge = false;
		this.reversedTraceOrder = false;
		this.reversedTraceOrderOnConflicting = false;
		this.edgeCreatedCycle = false;

		while(!s.isEmpty()) {

			Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> e = s.poll();


			if(e.x.equals(e.y) || this.existsEdge(e.x, e.y)) {
				continue;
			}

			//System.out.println("addEdgeAndSaturate, adding edge " + this.trace.evPairStr(e.x, e.y));


			if(this.existsEdge(e.y, e.x)) {
				this.edgeCreatedCycle = true;
				return false;
			}


			//Add edge, retrieve transitive closure, and for each node edge added in the transitive closure compute lock and observation closure
			for(Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> ee : this.addEdge(e.x, e.y)) {
				s.addAll(this.resolveLocks(ee.x, ee.y));
				s.addAll(this.resolveObservations(ee.x, ee.y));
				this.modified = true;
			}
		}


		return this.addedEdge;
	}


	private Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> resolveObservationsOfObEdge(Pair<Integer, Integer> from, Pair<Integer, Integer> to) { 
		Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> edgesToAdd = new HashSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();

		//Special case if this is an observation edge
		int loc = this.trace.seqTraces[from.x][from.y].location;

		//Resolve edges w'->r
		for(int i=0; i<this.trace.numThreads; i++) {
			int pred = this.getPredecessor(to, i);
			if(pred > 0) {
				int wp = this.trace.getToEfromWrites(new Pair<Integer, Integer>(i, pred), loc, true);
				if(wp > 0) {
					Pair<Integer, Integer> Wp = new Pair<Integer, Integer>(i, wp);
					if(!Wp.equals(from)) {
						edgesToAdd.add(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(Wp, from));
					}
				}
			}
		}

		//Resolve edges w -> w'
		for(int i=0; i<this.trace.numThreads; i++) {
			int succ = this.getSuccessor(from, i);
			if(succ > 0) {
				int wp = this.trace.getFromEtoWrites(new Pair<Integer, Integer>(i, succ), loc, true);
				if(wp > 0) {
					Pair<Integer, Integer> Wp = new Pair<Integer, Integer>(i, wp);
					if(!Wp.equals(from)) {
						edgesToAdd.add(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(to, Wp));
					}
				}
			}
		}

		return edgesToAdd;
	}



	//Ensures strong lock closure between the events that the edge connects - returns a set of edges to be added
	private Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> resolveLocks(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
		Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> edgesToAdd = new HashSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();

		//for(int l : this.trace.locksInCritical.get(from.x).get(from.y)) {
		for(int l : this.trace.getLocalGuardingLocks(from)) {
			int toRel = this.trace.getFromEtoReleases(to, l, true);
			if(toRel<0) {
				continue;
			}
			int toAcq = this.trace.lockMatch.get(to.x).get(toRel);
			Pair<Integer, Integer> fromRelease = new Pair<Integer, Integer>(from.x, this.trace.getFromEtoReleases(from, l, true));
			Pair<Integer, Integer> toAcquire = new Pair<Integer, Integer>(to.x, toAcq);
			edgesToAdd.add(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(fromRelease, toAcquire));
		}
		//System.out.println("Locks Edges to add " + edgesToAdd);
		return edgesToAdd;
	}


	//Ensures observation closure between the events that the edge connects
	private Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> resolveObservations(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
		Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> edgesToAdd = new HashSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();

		//System.out.println("resolveObservations of edge: " + this.trace.evPairStr(from, to));
		//System.out.println("Common locations: " + this.trace.getGlobalLocs(from.x, to.x));

		for(int g : this.trace.getGlobalLocs(from.x, to.x)) {

			int w = this.trace.getToEfromWrites(from, g, true);
			if(w<0) {
				//System.out.println("resolveObservations, to E from writes: " + " location: " + g + "\t" + this.trace.evStr(from) + " returned " + w);
				continue;
			}

			Pair<Integer, Integer> fromW = new Pair<Integer, Integer>(from.x, w);

			//Idenfity w-hb->r paths
			int r = this.trace.getFromEtoReads(to, g, true);
			//System.out.println("resolveObservations, from E to reads: " + this.trace.evStr(to) + " returned " + r);
			if(r > 0 && r <= this.activePrefixes[to.x]) {
				Pair<Integer, Integer> toR = new Pair<Integer, Integer>(to.x, r);

				Pair<Integer, Integer> obsR = this.trace.observation.get(toR);
				if(!obsR.equals(fromW)) {
					Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> e= new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(fromW, obsR);
					edgesToAdd.add(e);
				}
			}


			//Identify w-hb->ww paths
			int ww = this.trace.getFromEtoWrites(to, g, true);
			if(ww > 0 && ww <= this.activePrefixes[to.x]) {
				if(this.trace.reverseObservation.containsKey(fromW)) {
					Pair<Integer, Integer> toWW = new Pair<Integer, Integer>(to.x, ww);
					for(Pair<Integer, Integer> fromRR : this.trace.reverseObservation.get(fromW)) {
						if(to.x.equals(fromRR.x)) {
							continue;
						}
						if(fromRR.y <= this.activePrefixes[fromRR.x]) {
							Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> e= new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(fromRR, toWW);
							edgesToAdd.add(e);
						}
					}
				}
			}
		}


		return edgesToAdd;
	}


	/*
	 * //Ensures observation closure between the events that the edge connects
	private Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> resolveObservations(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
		Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> edgesToAdd = new HashSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();

		System.out.println("resolveObservations of edge: " + this.trace.evPairStr(from, to));
		System.out.println("Common locations: " + this.trace.getGlobalLocs(from.x, to.x));

		for(int g : this.trace.getGlobalLocs(from.x, to.x)) {

			int w = this.trace.getToEfromWrites(from, g, true);
			if(w<0) {
				System.out.println("resolveObservations, to E from writes: " + " location: " + g + "\t" + this.trace.evStr(from) + " returned " + w);
				continue;
			}
			int r = this.trace.getFromEtoReads(to, g, true);
			//System.out.println("resolveObservations, from E to reads: " + this.trace.evStr(to) + " returned " + r);
			if(r<0) {
				continue;
			}
			Pair<Integer, Integer> toR = new Pair<Integer, Integer>(to.x, r);

			//System.out.println("resolveObservations, found toR: " + this.trace.evStr(toR));

			Pair<Integer, Integer> fromW = new Pair<Integer, Integer>(from.x, w);			
			Pair<Integer, Integer> obsR = this.trace.observation.get(toR);

			if(!obsR.equals(fromW) && this.activePrefixes[toR.x] >= toR.y) {
				Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> e= new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(fromW, obsR);
				edgesToAdd.add(e);
			}
			int ww = this.trace.getFromEtoWrites(to, g, true);
			if(ww<0 || this.activePrefixes[to.x] < ww) {
				continue;
			}
			if(this.trace.reverseObservation.containsKey(fromW)) {
				Pair<Integer, Integer> toWW = new Pair<Integer, Integer>(to.x, ww);
				for(Pair<Integer, Integer> fromRR : this.trace.reverseObservation.get(fromW)) {
					if(to.x.equals(fromRR.x)) {
						continue;
					}
					//if(!this.fixedReads.contains(fromRR)) {
					if(this.activePrefixes[fromRR.x] < fromRR.y) {
						continue;
					}
					Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> e= new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(fromRR, toWW);
					edgesToAdd.add(e);
				}
			}
		}
		return edgesToAdd;
	}

	 */

}
