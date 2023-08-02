package util.treeclock.word_tricks_list;

import java.lang.reflect.Field;
import java.util.ArrayList;

import util.TripletHardCodeWordTricks;
//import util.Pair;
import util.dll.DoublyLinkedList;
import util.tree.EfficientTreeNodeHardCodeWordTricks;

public class ForestClock {

	public int dim;
	public EfficientTreeNodeHardCodeWordTricks rootsHead;

	// public HashMap<Short, EfficientTreeNodeHardCodeWordTricks> threadMap;
	public ArrayList<EfficientTreeNodeHardCodeWordTricks> threadMap;

	public ForestClock(int dim) {
		this.dim = dim;
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		for (int i = 0; i < this.dim; i++) {
			this.threadMap.add(null);
		}

	}

	public ForestClock(short tid, int dim) {
		this.dim = dim;
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		for (int i = 0; i < this.dim; i++) {
			this.threadMap.add(null);
		}
		EfficientTreeNodeHardCodeWordTricks root = new EfficientTreeNodeHardCodeWordTricks();
		root.data = TripletHardCodeWordTricks.setDataWithTid(tid);
		threadMap.set(tid, root);
		this.rootsHead = root;

	}

	public ForestClock(short tid, int tval, int dim) {
		// System.out.println("Initializing TC with " + dim + " threads");
		this.dim = dim;
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		for (int i = 0; i < this.dim; i++) {
			this.threadMap.add(null);
		}
		EfficientTreeNodeHardCodeWordTricks root = new EfficientTreeNodeHardCodeWordTricks();
		root.data = TripletHardCodeWordTricks.setDataWithTidAndClock(tid, tval);
		threadMap.set(tid, root);
		this.rootsHead = root;
	}

	public String threadMapToString() {
		String s = "";
		for (int i = 0; i < this.dim; i++) {
			EfficientTreeNodeHardCodeWordTricks u = this.threadMap.get(i);
			if (u == null) {
				s += "null\t";
			} else {
				s += TripletHardCodeWordTricks.toString(u.data) + "\t";
			}
		}
		return s;
	}

	private static EfficientTreeNodeHardCodeWordTricks deepCopyTreeAndMap(
			EfficientTreeNodeHardCodeWordTricks n,
			ArrayList<EfficientTreeNodeHardCodeWordTricks> newThreadMap) {
		if (n == null) {
			return null;
		}

		// Copy root
		EfficientTreeNodeHardCodeWordTricks n_copy = new EfficientTreeNodeHardCodeWordTricks();
		// n_copy.deepCopyData(n.getData());
		n_copy.data = n.data;

		newThreadMap.set(TripletHardCodeWordTricks.getTid(n_copy.getData()), n_copy);

		// Push root's children in empty stack
		DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();
		EfficientTreeNodeHardCodeWordTricks children_itr = n.getHeadChild();
		while (children_itr != null) {
			S.pushLatest(children_itr);
			children_itr = children_itr.getNext();
		}

		EfficientTreeNodeHardCodeWordTricks c_copy_parent = n_copy;

		// Now pop from the stack and process
		while (!S.isEmpty()) {
			EfficientTreeNodeHardCodeWordTricks c = S.popLatest();

			if (c == null) {
				c_copy_parent = S.popLatest();
				continue;
			}

			// Insert a copy of c in the current tree and threadMap
			EfficientTreeNodeHardCodeWordTricks c_copy = new EfficientTreeNodeHardCodeWordTricks();
			// c_copy.deepCopyData(c.getData());
			c_copy.data = c.data;
			newThreadMap.set(TripletHardCodeWordTricks.getTid(c_copy.getData()), c_copy);
			// short c_parent_tid =
			// TripletHardCodeWordTricks.getTid(c.getParent().getData());
			// EfficientTreeNodeHardCodeWordTricks c_copy_parent =
			// newThreadMap.get(c_parent_tid);
			c_copy_parent.addLatestChild(c_copy);

			S.pushLatest(c_copy_parent);
			S.pushLatest(null);
			c_copy_parent = c_copy;

			// Now, push all children of c in the stack
			EfficientTreeNodeHardCodeWordTricks c_children_itr = c.getHeadChild();
			while (c_children_itr != null) {
				S.pushLatest(c_children_itr);
				c_children_itr = c_children_itr.getNext();
			}
		}

		return n_copy;
	}

	public ForestClock(TreeClock fromTreeClock) {
		this.dim = fromTreeClock.dim;
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		/*
		 * for(int i=0; i<this.dim; i++) { this.threadMap.add(null); }
		 */

		Field f;
		try {
			f = ArrayList.class.getDeclaredField("size");
			f.setAccessible(true);
			f.setInt(this.threadMap, dim);
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // cache this
		catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.rootsHead = deepCopyTreeAndMap(fromTreeClock.root, this.threadMap);

	}

	public void deepCopy(TreeClock fromTreeClock) {
		this.dim = fromTreeClock.dim;
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		/*
		 * for(int i=0; i<this.dim; i++) { this.threadMap.add(null); }
		 */

		Field f;
		try {
			f = ArrayList.class.getDeclaredField("size");
			f.setAccessible(true);
			f.setInt(this.threadMap, dim);
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} // cache this
		catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		this.rootsHead = deepCopyTreeAndMap(fromTreeClock.root, this.threadMap);
	}

	public int getLocalClock(short tid) {
		EfficientTreeNodeHardCodeWordTricks n = this.threadMap.get(tid);
		if (n != null) {
			return TripletHardCodeWordTricks.getClock(n.getData());
		}
		return 0;
	}

	// Join from a treeclock tc. Assumes that tc has progressed.
	public void join(TreeClock tc) {

		if (tc.root == null) {
			return;
		}

		// String initTree = this.toString() + "\n" + this.threadMapToString();
		// System.out.println("-------------FOREST JOIN
		// BEGIN--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree );

		DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();
		S.pushLatest(tc.root);

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNodeHardCodeWordTricks uprime = S.popLatest();
			long uprime_data = uprime.data;
			short uprime_tid = TripletHardCodeWordTricks.getTid(uprime_data);
			EfficientTreeNodeHardCodeWordTricks u = this.threadMap.get(uprime_tid);
			int u_clock = -1;
			if (u == null) {
				u = new EfficientTreeNodeHardCodeWordTricks();
				threadMap.set(uprime_tid, u);
			} else {
				u_clock = TripletHardCodeWordTricks.getClock(u.data);
				if (u.hasParent()) {
					EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(u);
				} else {
					// u is one of the roots, treat separately
					EfficientTreeNodeHardCodeWordTricks u_next = u.getNext();
					EfficientTreeNodeHardCodeWordTricks u_prev = u.getPrev();

					if (u_prev != null) {
						u_prev.setNext(u_next);
					} else {
						this.rootsHead = u_next;
					}

					if (u_next != null) {
						u_next.setPrev(u_prev);
					}

				}

			}
			u.data = uprime_data;

			// Attach back
			if (uprime_tid != tc.rootTid) {
				EfficientTreeNodeHardCodeWordTricks y = this.threadMap
						.get(TripletHardCodeWordTricks.getTid(uprime.getParent().data));
				if (y.getHeadChild() != null) {
					y.getHeadChild().setPrev(u);
				}
				u.setNext(y.getHeadChild());
				u.setParent(y);
				y.setHeadChild(u);
			} else {
				u.setParent(null);
				if (this.rootsHead != null) {
					this.rootsHead.setPrev(u);
				}
				u.setNext(this.rootsHead);
				u.setPrev(null);
				this.rootsHead = u;
			}

			// Expand to children of u'
			EfficientTreeNodeHardCodeWordTricks vprime = uprime.getHeadChild();
			while (vprime != null) {

				long vprime_data = vprime.data;
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int v_clock = getLocalClock(vprime_tid);
				if (v_clock < TripletHardCodeWordTricks.getClock(vprime_data)) {
					S.pushLatest(vprime);
				} else {
					if (TripletHardCodeWordTricks.getPclock(vprime_data) <= u_clock) {
						break;
					}
				}
				vprime = vprime.getNext();
			}

		}

		// System.out.println("-------------FOREST JOIN END--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree + "\n\nfinalTree:\n" + this.toString()
		// + "\n" + this.threadMapToString());

	}

	// Join from a ForestClock tc. Does not assume that any of the trees of tc have
	// progressed
	public void join(ForestClock tc) {

		// String initTree = this.toString() + "\n" + this.threadMapToString();
		// System.out.println("-------------FOREST JOIN FOREST
		// BEGIN--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree );

		EfficientTreeNodeHardCodeWordTricks zprime = tc.rootsHead;
		while (zprime != null) {

			long zprime_data = zprime.data;
			short zprime_tid = TripletHardCodeWordTricks.getTid(zprime_data);
			int zprime_clock = TripletHardCodeWordTricks.getClock(zprime_data);

			if (zprime_clock <= this.getLocalClock(zprime_tid)) {
				zprime = zprime.getNext();
				continue;
			}

			DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();
			S.pushLatest(zprime);

			// Handle the rest
			while (!S.isEmpty()) {
				EfficientTreeNodeHardCodeWordTricks uprime = S.popLatest();
				long uprime_data = uprime.data;
				short uprime_tid = TripletHardCodeWordTricks.getTid(uprime_data);
				EfficientTreeNodeHardCodeWordTricks u = this.threadMap.get(uprime_tid);
				int u_clock = -1;
				if (u == null) {
					u = new EfficientTreeNodeHardCodeWordTricks();
					threadMap.set(uprime_tid, u);
				} else {
					u_clock = TripletHardCodeWordTricks.getClock(u.data);
					if (u.hasParent()) {
						EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(u);
					} else {
						// u is one of the roots, treat separately
						EfficientTreeNodeHardCodeWordTricks u_next = u.getNext();
						EfficientTreeNodeHardCodeWordTricks u_prev = u.getPrev();

						if (u_prev != null) {
							u_prev.setNext(u_next);
						} else {
							this.rootsHead = u_next;
						}

						if (u_next != null) {
							u_next.setPrev(u_prev);
						}

					}

				}
				u.data = uprime_data;

				// Attach back
				if (uprime_tid != TripletHardCodeWordTricks.getTid(zprime.data)) {
					EfficientTreeNodeHardCodeWordTricks y = this.threadMap.get(
							TripletHardCodeWordTricks.getTid(uprime.getParent().data));
					if (y.getHeadChild() != null) {
						y.getHeadChild().setPrev(u);
					}
					u.setNext(y.getHeadChild());
					u.setParent(y);
					y.setHeadChild(u);
				} else {
					u.setParent(null);
					if (this.rootsHead != null) {
						this.rootsHead.setPrev(u);
					}
					u.setNext(this.rootsHead);
					u.setPrev(null);
					this.rootsHead = u;
				}

				// Expand to children of u'
				EfficientTreeNodeHardCodeWordTricks vprime = uprime.getHeadChild();
				while (vprime != null) {

					long vprime_data = vprime.data;
					short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
					int v_clock = getLocalClock(vprime_tid);
					if (v_clock < TripletHardCodeWordTricks.getClock(vprime_data)) {
						S.pushLatest(vprime);
					} else {
						if (TripletHardCodeWordTricks.getPclock(vprime_data) <= u_clock) {
							break;
						}
					}
					vprime = vprime.getNext();
				}

			}

		}

		// System.out.println("-------------FOREST JOIN FOREST
		// END--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree + "\n\nfinalTree:\n" + this.toString()
		// + "\n" + this.threadMapToString());

	}

	@Override
	public String toString() {
		String s = "";
		EfficientTreeNodeHardCodeWordTricks root = this.rootsHead;
		while (root != null) {
			s += root.toTreeString() + "\n";
			root = root.getNext();
		}
		return s;
	}

	public String timesToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (short i = 0; i < this.dim; i++) {
			sb.append(this.getLocalClock(i));
			if (i < this.dim - 1) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();

	}
}