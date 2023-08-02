package util.treeclock.iterative;

import java.util.HashMap;
import java.util.HashSet;

import util.Triplet;
import util.dll.DoublyLinkedList;
import util.tree.EfficientTreeNode;

public class TreeClock {

	public EfficientTreeNode<Triplet<Short, Integer, Integer>> root;
	public HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> threadMap;

	public TreeClock() {
		root = null;
		threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>>();
	}

	public TreeClock(short tid) {
		Triplet<Short, Integer, Integer> root_data = new Triplet<Short, Integer, Integer>(
				tid, 0, 0);
		root = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(root_data);
		threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>>();
		threadMap.put(tid, root);
	}

	public TreeClock(short tid, int tval) {
		Triplet<Short, Integer, Integer> root_data = new Triplet<Short, Integer, Integer>(
				tid, tval, 0);
		root = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(root_data);
		threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>>();
		threadMap.put(tid, root);
	}

	public TreeClock(HashSet<Short> tids, short root_tid, int root_val) {
		Triplet<Short, Integer, Integer> root_data = new Triplet<Short, Integer, Integer>(
				root_tid, root_val, 0);
		root = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(root_data);
		threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>>();
		threadMap.put(root_tid, root);

		EfficientTreeNode<Triplet<Short, Integer, Integer>> parent = root;
		for (short tid : tids) {
			if (tid != root_tid) {
				Triplet<Short, Integer, Integer> tid_data = new Triplet<Short, Integer, Integer>(
						tid, 0, 0);
				EfficientTreeNode<Triplet<Short, Integer, Integer>> curr = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(
						tid_data);
				threadMap.put(tid, curr);
				parent.addLatestChild(curr);
				parent = curr;
			}
		}
	}

	private static EfficientTreeNode<Triplet<Short, Integer, Integer>> deepCopyTreeAndMap_recursive(
			EfficientTreeNode<Triplet<Short, Integer, Integer>> n,
			HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> newThreadMap) {
		if (n == null)
			return null;
		else {
			EfficientTreeNode<Triplet<Short, Integer, Integer>> n_copy = new EfficientTreeNode<Triplet<Short, Integer, Integer>>();
			n_copy.deepCopyData(n.getData());
			newThreadMap.put(n_copy.getData().first, n_copy);
			EfficientTreeNode<Triplet<Short, Integer, Integer>> children_itr = n
					.getHeadChild();
			boolean isHead = true;
			EfficientTreeNode<Triplet<Short, Integer, Integer>> n_copy_children_prev = null;
			while (!(children_itr == null)) {
				EfficientTreeNode<Triplet<Short, Integer, Integer>> child_copy = deepCopyTreeAndMap_recursive(
						children_itr, newThreadMap);
				child_copy.setParent(n_copy);
				child_copy.setPrev(n_copy_children_prev);
				if (isHead) {
					isHead = false;
					n_copy.setHeadChild(child_copy);
				} else {
					n_copy_children_prev.setNext(child_copy);
				}
				n_copy_children_prev = child_copy;
				children_itr = children_itr.getNext();
			}
			return n_copy;
		}
	}

	private static EfficientTreeNode<Triplet<Short, Integer, Integer>> deepCopyTreeAndMap(
			EfficientTreeNode<Triplet<Short, Integer, Integer>> n,
			HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> newThreadMap) {
		if (n == null)
			return null;

		// Copy root
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n_copy = new EfficientTreeNode<Triplet<Short, Integer, Integer>>();
		n_copy.deepCopyData(n.getData());
		newThreadMap.put(n_copy.getData().first, n_copy);

		// Push root's children in empty stack
		DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>>();
		EfficientTreeNode<Triplet<Short, Integer, Integer>> children_itr = n
				.getHeadChild();
		while (children_itr != null) {
			S.pushLatest(children_itr);
			children_itr = children_itr.getNext();
		}

		// Now pop from the stack and process
		while (!S.isEmpty()) {
			EfficientTreeNode<Triplet<Short, Integer, Integer>> c = S.popLatest();

			// Insert a copy of c in the current tree and threadMap
			EfficientTreeNode<Triplet<Short, Integer, Integer>> c_copy = new EfficientTreeNode<Triplet<Short, Integer, Integer>>();
			c_copy.deepCopyData(c.getData());
			newThreadMap.put(c_copy.getData().first, c_copy);
			short c_parent_tid = c.getParent().getData().first;
			EfficientTreeNode<Triplet<Short, Integer, Integer>> c_copy_parent = newThreadMap
					.get(c_parent_tid);
			c_copy_parent.addLatestChild(c_copy);

			// Now, push all children of c in the stack
			EfficientTreeNode<Triplet<Short, Integer, Integer>> c_children_itr = c
					.getHeadChild();
			while (c_children_itr != null) {
				S.pushLatest(c_children_itr);
				c_children_itr = c_children_itr.getNext();
			}

		}
		return n_copy;

	}

	public TreeClock(TreeClock fromTreeClock) {
		this.threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>>();
		this.root = deepCopyTreeAndMap(fromTreeClock.root, this.threadMap);
	}

	public void deepCopy(TreeClock fromTreeClock) {
		this.threadMap = new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>>();
		this.root = deepCopyTreeAndMap(fromTreeClock.root, this.threadMap);
	}

	public int getLocalClock(short tid) {
		if (this.threadMap.containsKey(tid)) {
			return threadMap.get(tid).getData().second;
		}
		return 0;
	}

	public void incrementBy(int val) {
		Triplet<Short, Integer, Integer> root_data = root.getData();
		root_data.second = root_data.second + val;
	}

	public boolean isLessThanOrEqual(TreeClock tc) {
		if (this.root == null) {
			return true;
		}
		short root_tid = root.getData().first;
		int root_val = root.getData().second;
		return root_val <= tc.getLocalClock(root_tid);
	}

	public void join(TreeClock tc) {
		if (tc.root == null)
			return;

		if (this.root == null)
			deepCopy(tc);

		EfficientTreeNode<Triplet<Short, Integer, Integer>> zprime = tc.root;
		short zprime_tid = zprime.getData().first;
		int zprime_local_clock = getLocalClock(zprime_tid);
		int zprime_clk = zprime.getData().second;
		if (zprime_clk <= zprime_local_clock)
			return;

		DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>>();

		// Handle the root first
		EfficientTreeNode<Triplet<Short, Integer, Integer>> vprime = zprime
				.getHeadChild();
		while (vprime != null) {
			short vprime_tid = vprime.getData().first;
			int vprime_local_clock = getLocalClock(vprime_tid);
			if (vprime_local_clock < vprime.getData().second) {
				S.pushLatest(vprime);
			} else {
				if (vprime.getData().third <= zprime_local_clock) {
					break;
				}
			}
			vprime = vprime.getNext();
		}

		EfficientTreeNode<Triplet<Short, Integer, Integer>> u = null;

		if (this.threadMap.containsKey(zprime_tid)) {
			// Detach node with tid = z'.tid from T
			u = this.threadMap.get(zprime_tid);
			EfficientTreeNode.detachFromNeighbors(u);
		} else {
			Triplet<Short, Integer, Integer> u_data = new Triplet<Short, Integer, Integer>(
					zprime_tid, 0, 0);
			u = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(u_data);
			threadMap.put(zprime_tid, u);
		}

		// Update the clock of u
		u.getData().second = zprime_clk;

		// Place the updated subtree under the root of T
		EfficientTreeNode<Triplet<Short, Integer, Integer>> z = this.root;
		u.getData().third = z.getData().second;
		if (z.getHeadChild() != null) {
			z.getHeadChild().setPrev(u);
		}
		u.setPrev(null);
		u.setNext(z.getHeadChild());
		u.setParent(z);
		z.setHeadChild(u);

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNode<Triplet<Short, Integer, Integer>> uprime = S.popLatest();

			short uprime_tid = uprime.getData().first;

			// Expand to children of u'
			vprime = uprime.getHeadChild();
			while (vprime != null) {
				short vprime_tid = vprime.getData().first;
				int vprime_local_clock = getLocalClock(vprime_tid);
				if (vprime_local_clock < vprime.getData().second) {
					S.pushLatest(vprime);
				} else {
					if (vprime.getData().third <= this.getLocalClock(uprime_tid)) {
						break;
					}
				}
				vprime = vprime.getNext();
			}

			if (this.threadMap.containsKey(uprime_tid)) {
				// Detach node with tid = u'.tid from T
				u = this.threadMap.get(uprime_tid);
				EfficientTreeNode.detachFromNeighbors(u);
			} else {
				Triplet<Short, Integer, Integer> u_data = new Triplet<Short, Integer, Integer>(
						uprime_tid, 0, 0);
				u = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(u_data);
				threadMap.put(uprime_tid, u);
			}

			// Update the clock of u
			u.getData().second = uprime.getData().second;
			u.getData().third = uprime.getData().third;

			// Attach back
			EfficientTreeNode<Triplet<Short, Integer, Integer>> y = this.threadMap
					.get(uprime.getParent().getData().first);
			if (y.getHeadChild() != null) {
				y.getHeadChild().setPrev(u);
			}
			u.setPrev(null);
			u.setNext(y.getHeadChild());
			u.setParent(y);
			y.setHeadChild(u);
		}
	}

	// Assumes tc.root is not null
	public void monotoneCopy(TreeClock tc) {
		if (this.root == null) {
			this.deepCopy(tc);
			return;
		}

		EfficientTreeNode<Triplet<Short, Integer, Integer>> zprime = tc.root;
		short zprime_tid = zprime.getData().first;
		int zprime_local_clock = getLocalClock(zprime_tid);

		EfficientTreeNode<Triplet<Short, Integer, Integer>> z = this.root;
		short z_tid = z.getData().first;

		DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>> S = new DoublyLinkedList<EfficientTreeNode<Triplet<Short, Integer, Integer>>>();

		// Handle the root first
		EfficientTreeNode<Triplet<Short, Integer, Integer>> vprime = zprime
				.getHeadChild();
		while (vprime != null) {
			short vprime_tid = vprime.getData().first;
			int vprime_local_clock = getLocalClock(vprime_tid);
			if (vprime_local_clock < vprime.getData().second) {
				S.pushLatest(vprime);
			} else {
				if (vprime_tid == z_tid) {
					S.pushLatest(vprime);
				}
				if (vprime.getData().third <= zprime_local_clock) {
					break;
				}
			}
			vprime = vprime.getNext();
		}

		EfficientTreeNode<Triplet<Short, Integer, Integer>> u = null;
		if (this.threadMap.containsKey(zprime_tid)) {
			// Detach node with tid = z'.tid from T
			u = this.threadMap.get(zprime_tid);
			EfficientTreeNode.detachFromNeighbors(u);
		} else {
			Triplet<Short, Integer, Integer> u_data = new Triplet<Short, Integer, Integer>(
					zprime_tid, 0, 0);
			u = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(u_data);
			threadMap.put(zprime_tid, u);
		}

		// Update the clock of u
		u.getData().second = zprime.getData().second;

		// Make u the root
		u.setPrev(null);
		this.root = u;

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNode<Triplet<Short, Integer, Integer>> uprime = S.popLatest();
			short uprime_tid = uprime.getData().first;
			int uprime_local_clock = getLocalClock(uprime_tid);

			// Expand to children of u'
			vprime = uprime.getHeadChild();
			while (vprime != null) {
				short vprime_tid = vprime.getData().first;
				int vprime_local_clock = getLocalClock(vprime_tid);
				if (vprime_local_clock < vprime.getData().second) {
					S.pushLatest(vprime);
				} else {
					if (vprime_tid == z_tid) {
						S.pushLatest(vprime);
					}
					if (vprime.getData().third <= uprime_local_clock) {
						break;
					}
				}
				vprime = vprime.getNext();
			}

			if (this.threadMap.containsKey(uprime_tid)) {
				// Detach node with tid = u'.tid from T
				u = this.threadMap.get(uprime_tid);
				EfficientTreeNode.detachFromNeighbors(u);
			} else {
				Triplet<Short, Integer, Integer> u_data = new Triplet<Short, Integer, Integer>(
						uprime_tid, 0, 0);
				u = new EfficientTreeNode<Triplet<Short, Integer, Integer>>(u_data);
				threadMap.put(uprime_tid, u);
			}

			// Update the clock of u
			u.getData().second = uprime.getData().second;
			u.getData().third = uprime.getData().third;

			// Attach back
			EfficientTreeNode<Triplet<Short, Integer, Integer>> y = this.threadMap
					.get(uprime.getParent().getData().first);
			if (y.getHeadChild() != null) {
				y.getHeadChild().setPrev(u);
			}
			u.setPrev(null);
			u.setNext(y.getHeadChild());
			u.setParent(y);
			y.setHeadChild(u);
		}
	}

	@Override
	public String toString() {
		return this.root.toTreeString();
	}
}