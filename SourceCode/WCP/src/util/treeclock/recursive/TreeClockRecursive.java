package util.treeclock.recursive;

import java.util.HashMap;

import util.Triplet;
import util.dll.DoublyLinkedList;
import util.dll.EfficientDLNode;
import util.tree.EfficientTreeNode;
import util.treeclock.iterative.TreeClock;

public class TreeClockRecursive extends TreeClock {
	
	public TreeClockRecursive() {
		root = null;
		threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
	}

	public TreeClockRecursive(short tid) {
		Triplet<Short, Integer, Integer> root_data = new Triplet<Short, Integer, Integer> (tid, 0, 0);
		root = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(root_data);
		threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		threadMap.put(tid, root);
	}

	public TreeClockRecursive(short tid, int tval) {
		Triplet<Short, Integer, Integer> root_data = new Triplet<Short, Integer, Integer> (tid, tval, 0);
		root = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(root_data);
		threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		threadMap.put(tid, root);
	}

	public TreeClockRecursive(TreeClockRecursive fromTreeClock) {
		this.root = EfficientTreeNode.deepCopyTree(fromTreeClock.root);
		this.threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> (fromTreeClock.threadMap);
	}

	/*
	private void getUpdatedNodes(TreeDLL<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S, EfficientTreeNode<Triplet<Short, Integer, Integer>> u) {
		if(u.getData().second <= getLocalClock(u.getData().first)) return;
		EfficientTreeNode<Triplet<Short, Integer, Integer>> child = u.getHeadChild();
		short u_tid = u.getData().first;
		int u_local_clock = getLocalClock(u_tid);
		while(child != null) {
			if(u_local_clock >= child.getData().third) break;
			getUpdatedNodes(S, child);
			child = child.getNext();
		}
		S.pushLatest(u);
	}
	 */

	private void getUpdatedNodesForJoin(
			DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S, 
			EfficientTreeNode<Triplet<Short, Integer, Integer>> uprime) 
	{
		short uprime_tid = uprime.getData().first;
		int uprime_local_clock = getLocalClock(uprime_tid);

		EfficientTreeNode<Triplet<Short, Integer, Integer>> vprime = uprime.getHeadChild();
		while(vprime != null) {
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
//		u.getData().third = v.getData().second;
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

	/*
	public void join(TreeClock tc) {
		EfficientTreeNode<Triplet<Short, Integer, Integer>> uprime = tc.root;
		if(getLocalClock(uprime.getData().first) >= uprime.getData().second) return;
		TreeDLL<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new TreeDLL<EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		getUpdatedNodesForJoin(S, uprime);
		detachNodes(S);
		attachNodes(S);
	}
	 */

	public void join(TreeClock tc) {
		EfficientTreeNode<Triplet<Short, Integer, Integer>> zprime = tc.root;
		short zprime_tid = zprime.getData().first;
		if(zprime.getData().second <= getLocalClock(zprime_tid)) return;
		DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
//		System.out.println("Calling getUpdated Nodes");
		getUpdatedNodesForJoin(S, zprime);
//		System.out.println("Done with getUpdated Nodes");
//		System.out.println("S = " + S);
		detachNodes(S);
		attachNodes(S);

		EfficientTreeNode<Triplet<Short, Integer, Integer>> w = threadMap.get(zprime_tid);
		w.getData().third = root.getData().second;
		pushChild(w, root);
	}

	public void monotoneCopy(TreeClock tc) {
		DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		getUpdatedNodesForCopy(S, tc.root, root);
//		System.out.println("S = " + S);
		detachNodes(S);
		attachNodes(S);
		root = threadMap.get(tc.root.getData().first);
	}

	@Override
	public String toString() {
		return this.root.toTreeString();
	}
}