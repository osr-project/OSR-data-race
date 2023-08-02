

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

// A partial order over nodes of the form p=(x,y), where p.x is the thread id and p.y is the local (incremental) id


public class PartialOrder {
	public int width;
	public int[] lengths;
	public List<Map<Integer, SuffixMinima>> successors;
	
	
	public boolean globalFlag;

	public PartialOrder(int [] lengths) {
		this.width = lengths.length;
		this.successors = new ArrayList<Map<Integer, SuffixMinima>>();
		this.lengths = new int[this.width];
		

		for(int i=0; i< this.width; i++) {
			this.lengths[i] = lengths[i];
			this.successors.add(new HashMap<Integer, SuffixMinima>());
			for(int j=0; j<this.width; j++) {
				if(i!=j) {
					int [] maxValues = new int[this.lengths[i]];
					Arrays.fill(maxValues, Integer.MAX_VALUE);
					this.successors.get(i).put(j, new SuffixMinima(maxValues));
				}
			}
		}
	}
	
	
	public PartialOrder(PartialOrder other) {
		this.width = other.width;
		this.successors = new ArrayList<Map<Integer, SuffixMinima>>();
		this.lengths = new int[this.width];

		for(int i=0; i< this.width; i++) {
			this.lengths[i] = other.lengths[i];
			this.successors.add(new HashMap<Integer, SuffixMinima>());
			for(int j=0; j<this.width; j++) {
				if(i!=j) {
					this.successors.get(i).put(j, new SuffixMinima(other.successors.get(i).get(j)));
				}
			}
		}
	}
	
	public void pause() {
		try {System.in.read();} catch(Exception e) {}
	}
	
	//This is semantic equality, not merely comparing the array entries
	public boolean equals(Object o) {
		PartialOrder other= (PartialOrder) o;
		
		for(int i=0; i<this.width; i++) {
			for(int j=0; j<this.lengths[i]; j++) {
				Pair<Integer, Integer> u = new Pair<Integer, Integer>(i,j);
				for(int k=0; k<this.width; k++) {
					int thisSucc = this.getSuccessor(u, k);
					int otherSucc = other.getSuccessor(u,k);
					if(thisSucc != otherSucc) {
						//System.out.println("i: " + i + "\tj: " + j + "\tthisSucc: " + thisSucc + "\totherSucc: " + otherSucc);
						//this.pause();
						return false;
					}
				}
			}
		}
		return true;	
	}
	
	

	//Get the earliest successor of p on thread i - returns -1 if no successor
	public int getSuccessor(Pair<Integer, Integer> p, int i) {
		
		if(p.x.equals(i)) {
			return p.y < this.lengths[p.x]-1 ? p.y+1 : -1;
		}
		else {
			int v = this.successors.get(p.x).get(i).getMin(p.y);
			return v < Integer.MAX_VALUE ? v : -1;
		}
	}

	//Get the latest predecessor of p on thread i - returns -1 if no predecessor
	public int getPredecessor(Pair<Integer, Integer> p, int i) {
		if(p.x.equals(i)) {
			return p.y > 0 ? p.y-1 : -1;
		}
		else {
			int v = this.successors.get(i).get(p.x).getArgMin(p.y);
			return v > Integer.MIN_VALUE ? v : -1;
		}
	}

	
	public void addSuccessor(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
		if(!from.x.equals(to.x)) {
			this.successors.get(from.x).get(to.x).update(from.y, to.y);
		}
	}

	
	public boolean unordered(Pair<Integer, Integer> u, Pair<Integer, Integer> v) {
		return ! (this.existsEdge(u, v) || this.existsEdge(v, u));
	}


	// Returns the earliest successor p has on each thread
	public int[] getSuccessors(Pair<Integer, Integer> p) {
		int s[] = new int[this.width];
		for (int i=0; i< this.width; i++) {
			s[i] = this.getSuccessor(p, i);
		}
		return s;
	}


	// Returns the latest predecessor p has on each thread
	public int[] getPredecessors(Pair<Integer, Integer> p) {
		int s[] = new int[this.width];
		for (int i=0; i< this.width; i++) {
			s[i] = this.getPredecessor(p, i);
		}
		return s;
	}

	
	
	public boolean existsEdge(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
		if(from.x.equals(to.x)) {
			return from.y < to.y;
		}
		int v = this.getSuccessor(from, to.x);
		return  v>=0 && v <= to.y;
	}



	//Adds edge and takes transitive closure. Assumes that edge does not create cycle
	//Returns the set of edges added due to transitive closure.
	public Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> addEdge(Pair<Integer, Integer> from, Pair<Integer, Integer> to) {
		Set<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> addedEdges = new HashSet<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();

		if(this.existsEdge(from, to)) {
			return addedEdges;
		}
		
		Stack<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>> q = new Stack<Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>>();
		q.push(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(from, to));
		while(!q.isEmpty()) {
			Pair<Pair<Integer, Integer>, Pair<Integer, Integer>> e = q.pop();
			Pair<Integer, Integer> f = e.x;
			Pair<Integer, Integer> t = e.y;
			if(this.existsEdge(f, t)) {
				continue;
			}
			
			this.addSuccessor(f, t);
			addedEdges.add(e);
			int [] succ = this.getSuccessors(t);
			int [] pred = this.getPredecessors(f);
			for(int i=0; i<this.width; i++) {
				if(i!=f.x && i!=t.x) {
					Pair<Integer, Integer> tt = new Pair<Integer, Integer>(i, succ[i]);
					if(succ[i]>=0) {
						q.push(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(f, tt));
					}
					Pair<Integer, Integer> ff = new Pair<Integer, Integer>(i, pred[i]);
					if(pred[i]>=0) {
						q.push(new Pair<Pair<Integer, Integer>, Pair<Integer, Integer>>(ff, t));
					}
				}	
			}			
		}
		return addedEdges;
	}
	
	
	public void printPO() {
			System.out.println("==================================");
			System.out.println("PO edges. Width: " + this.width);
			System.out.println("==================================");
			for(int i1=0; i1<this.width; i1++) {
				for(int j1=0; j1<this.lengths[i1]; j1++) {
					for(int i2=0; i2<this.width; i2++) {
						if(i1!=i2) {
							Pair<Integer, Integer> u = new Pair<Integer, Integer>(i1, j1);
							Pair<Integer, Integer> v = new Pair<Integer, Integer>(i2, this.getSuccessor(u, i2));
							System.out.println(u.toString() + "\t->\t" + v.toString());
						}
					}
				}
			}
		}

	
//	public void printPOArrays() {
//		System.out.println("==================================");
//		System.out.println("PO arrays. Width: " + this.width);
//		System.out.println("==================================");
//		for(int i1=0; i1<this.width; i1++) {
//			for(int i2=0; i2<this.width; i2++) {
//				if(i1!=i2) {
//					System.out.println(i1 + " -> " + i2 + ":\t" + this.successors.get(i1).get(i2).numArray.numArray2Str());
//				}
//			}
//		}
//	}
}
