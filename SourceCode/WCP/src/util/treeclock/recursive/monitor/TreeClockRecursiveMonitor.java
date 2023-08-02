package util.treeclock.recursive.monitor;

import debug.TreeClockStatistics;
import util.Triplet;
import util.dll.DoublyLinkedList;
import util.dll.EfficientDLNode;
import util.tree.EfficientTreeNode;
import util.treeclock.iterative.TreeClock;
import util.treeclock.recursive.TreeClockRecursive;

public class TreeClockRecursiveMonitor extends TreeClockRecursive {
	
	public TreeClockRecursiveMonitor() {
		super();
	}

	public TreeClockRecursiveMonitor(short tid) {
		super(tid);
	}

	public TreeClockRecursiveMonitor(short tid, int tval) {
		super(tid, tval);
	}

	public TreeClockRecursiveMonitor(TreeClockRecursiveMonitor fromTreeClock) {
		super(fromTreeClock);
	}
	
	private void getUpdatedNodesForJoin(
			DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S, 
			EfficientTreeNode<Triplet<Short, Integer, Integer>> uprime) 
	{
		
		short uprime_tid = uprime.getData().first;
		int uprime_local_clock = getLocalClock(uprime_tid);

		EfficientTreeNode<Triplet<Short, Integer, Integer>> vprime = uprime.getHeadChild();
		while(vprime != null) {
			
			if(TreeClockStatistics.isEnabled()) {
				TreeClockStatistics.incrementOpsInJoinsBy(1);
			}
			
			short vprime_tid = vprime.getData().first;
			int vprime_local_clock = getLocalClock(vprime_tid);
			if(vprime_local_clock < vprime.getData().second) {
				getUpdatedNodesForJoin(S, vprime);
			}
			else {
				if(vprime.getData().third <= uprime_local_clock) break;
			}
			vprime = vprime.getNext();
		}
		S.pushLatest(uprime);
	}

	private void getUpdatedNodesForCopy(
			DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S, 
			EfficientTreeNode<Triplet<Short, Integer, Integer>> uprime, 
			EfficientTreeNode<Triplet<Short, Integer, Integer>> z) 
	{
		short uprime_tid = uprime.getData().first;
		int uprime_local_clock = getLocalClock(uprime_tid);

		EfficientTreeNode<Triplet<Short, Integer, Integer>> vprime = uprime.getHeadChild();
		while(vprime != null) {
			
			if(TreeClockStatistics.isEnabled()) {
				TreeClockStatistics.incrementOpsInMonotoneCopyBy(1);
			}
			
			short vprime_tid = vprime.getData().first;
			int vprime_local_clock = getLocalClock(vprime_tid);
			if(vprime_local_clock < vprime.getData().second) {
				getUpdatedNodesForCopy(S, vprime, z);
			}
			else {
				if(z != null) {
					short z_tid = z.getData().first;
					if(vprime_tid == z_tid) {
						S.pushLatest(vprime);
					}
				}
				if(vprime.getData().third <= uprime_local_clock) break;
			}
			vprime = vprime.getNext();
		}
		S.pushLatest(uprime);
	}

	private void detachNodes(DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S) {
		EfficientDLNode<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S_node = S.getHeadNode();
		while(S_node != null) {
			EfficientTreeNode<Triplet<Short, Integer, Integer>> vprime = S_node.getData();
			if(threadMap.containsKey(vprime.getData().first)) {
				EfficientTreeNode<Triplet<Short, Integer, Integer>> v = threadMap.get(vprime.getData().first);
				EfficientTreeNode<Triplet<Short, Integer, Integer>> x = v.getParent();
				if (x != null) {
					EfficientTreeNode.detachFromNeighbors(v);
				}
			}
			S_node = S_node.getNext();
		}
	}

	// TODO: make this inline
	private static void pushChild(
			EfficientTreeNode<Triplet<Short, Integer, Integer>> u, 
			EfficientTreeNode<Triplet<Short, Integer, Integer>> v) 
	{
		v.addLatestChild(u);	
	}

	private void attachNodes(DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S) {
		while(!S.isEmpty()) {
			EfficientTreeNode<Triplet<Short, Integer, Integer>> uprime = S.popLatest();
			short uprime_tid = uprime.getData().first;
			EfficientTreeNode<Triplet<Short, Integer, Integer>> u = null;
			if(threadMap.containsKey(uprime_tid)) {
				u = threadMap.get(uprime_tid);
			}
			else {
				Triplet<Short, Integer, Integer> u_data = new Triplet<Short, Integer, Integer> (uprime_tid, 0, 0);
				u = new EfficientTreeNode<Triplet<Short, Integer, Integer>> (u_data);
				threadMap.put(uprime_tid, u);
			}
			u.getData().second = uprime.getData().second;
			EfficientTreeNode<Triplet<Short, Integer, Integer>> yprime = uprime.getParent();
			if(yprime != null) {
				u.getData().third = uprime.getData().third;
				EfficientTreeNode<Triplet<Short, Integer, Integer>> y = threadMap.get(yprime.getData().first);
				pushChild(u, y);
			}
		}
	}

	@Override
	public void join(TreeClock tc) {
		if(TreeClockStatistics.isEnabled()) {
			TreeClockStatistics.incrementNumJoinsBy(1);
		}
		
		EfficientTreeNode<Triplet<Short, Integer, Integer>> zprime = tc.root;
		short zprime_tid = zprime.getData().first;
		if(zprime.getData().second <= getLocalClock(zprime_tid)) {
			if(TreeClockStatistics.isEnabled()) {
				TreeClockStatistics.incrementNumJoinsSkippedBy(1);
			}
			return;
		}
		
		DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		
		if(TreeClockStatistics.isEnabled()) {
			TreeClockStatistics.incrementOpsInJoinsBy(1);
		}
		
		getUpdatedNodesForJoin(S, zprime);
		detachNodes(S);
		attachNodes(S);

		EfficientTreeNode<Triplet<Short, Integer, Integer>> w = threadMap.get(zprime_tid);
		w.getData().third = root.getData().second;
		pushChild(w, root);
	}

	@Override
	public void monotoneCopy(TreeClock tc) {
		
		if(TreeClockStatistics.isEnabled()) {
			TreeClockStatistics.incrementNumMonotoneCopyBy(1);
		}
		
		DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		
		if(TreeClockStatistics.isEnabled()) {
			TreeClockStatistics.incrementOpsInMonotoneCopyBy(1);
		}
		
		getUpdatedNodesForCopy(S, tc.root, root);
		detachNodes(S);
		attachNodes(S);
		root = threadMap.get(tc.root.getData().first);
	}

	@Override
	public String toString() {
		return this.root.toTreeString();
	}
}