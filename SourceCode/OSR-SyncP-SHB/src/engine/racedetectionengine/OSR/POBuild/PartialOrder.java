package engine.racedetectionengine.OSR.POBuild;

import util.Triplet;
import util.vectorclock.VectorClock;

import java.util.*;

// A partial order over nodes of the form p=(x,y), where p.first is the thread id and p.second is the local (incremental) id


public class PartialOrder {
	public int width;
//	public List<Map<Integer, RangeMinima>> successors;

	public List<Map<Integer, RangeMinimaQuery>> successors;

	public PartialOrder(Map<Integer, Map<Integer, ArrayList<Integer>>> succFromNode,
						Map<Integer, Map<Integer, ArrayList<Integer>>> succToNode, int numThreads) {
		this.width = numThreads;
		this.successors = new ArrayList<>();

		for(int i=0; i< this.width; i++) {
			this.successors.add(new HashMap<>());
			for(int j=0; j<this.width; j++) {
				if(i!=j) {
					ArrayList<Integer> fromNodes = succFromNode.get(i).get(j);
					ArrayList<Integer> toNodes = succToNode.get(i).get(j);
					this.successors.get(i).put(j, new RangeMinimaQuery(fromNodes, toNodes));
					toNodes = null;
					succFromNode.get(i).remove(j);
					succToNode.get(i).remove(j);
				}
			}
			succFromNode.remove(i);
			succToNode.remove(i);
		}
	}
	
	public void pause() {
		try {System.in.read();} catch(Exception e) {}
	}
	

	// Given an event e1 and ssp set, return the earliest events e2 in every thread, s.t. there is a
	// path from e1 to e2
	public VectorClock queryForEvent(Triplet<Integer, Integer, Long> event, VectorClock ssp){
		// event = <threadId, inThreadId, auxId>
		VectorClock ret = new VectorClock(ssp.getDim());
		int threadId = event.first;
		int inThreadId = event.second;
		int limit = ssp.getClockIndex(threadId);

		// init with direct edges
		for(int i=0; i<this.width; i++){
			if(i == threadId) {
				ret.setClockIndex(i, inThreadId);
				continue;
			}

			RangeMinimaQuery rangeMinima = successors.get(threadId).get(i);

			int firstInToThread = rangeMinima.getMinWithRange(inThreadId, limit);
			ret.setClockIndex(i, firstInToThread);
		}

//		System.out.println("middle : " + ret);

		// update VC with BellmanFord
		for(int i=0; i<this.width; i++){
			boolean hasChanged = false;

			for(int from=0; from < this.width; from++){
				if(from == threadId) continue;

				for(int to=0; to < this.width; to++){
					if (from == to || to == threadId) continue;
					int left = ret.getClockIndex(from);

					// left should exist in ssp, and not be infinite (-1)
					if(left != -1 && left <= ssp.getClockIndex(from)){
						int curVal = this.successors.get(from).get(to).getMinWithRange(left, ssp.getClockIndex(from));
						if(curVal != -1 && curVal < ret.getClockIndex(to)) {
							ret.setClockIndex(to, curVal);
							hasChanged = true;
						}
					}
				}
			}

			if(!hasChanged) break;
		}

		return ret;
	}

	public List<VectorClock> queryForEventLists(List<Triplet<Integer, Integer, Long>> events, VectorClock ssp){
		// input = list( <threadId, inThreadId, auxId> )
		// returns a vector clock for each event e, where vc[i] = e's first successor in thread i

		List<VectorClock> ret = new ArrayList<>();

		for(Triplet<Integer, Integer, Long> cur : events) {
			ret.add(this.queryForEvent(cur, ssp));
		}

		return ret;
	}
}
