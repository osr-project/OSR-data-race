package util.treeclock.word_tricks_list;

import java.lang.reflect.Field;
import java.util.ArrayList;

import debug.EventStatistics;
import util.TripletHardCodeWordTricks;
//import util.Pair;
import util.dll.DoublyLinkedList;
import util.tree.EfficientTreeNodeHardCodeWordTricks;

public class TreeClock {

	public int dim;
	public EfficientTreeNodeHardCodeWordTricks root;
	public short rootTid = -1;
	// public HashMap<Short, EfficientTreeNodeHardCodeWordTricks> threadMap;
	public ArrayList<EfficientTreeNodeHardCodeWordTricks> threadMap;

	public TreeClock(int dim) {
		this.dim = dim;
		// root = new EfficientTreeNodeHardCodeWordTricks();
		this.root = null;
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		for (int i = 0; i < this.dim; i++) {
			this.threadMap.add(null);
		}

	}

	public TreeClock(short tid, int dim) {
		this.dim = dim;
		root = new EfficientTreeNodeHardCodeWordTricks();
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		for (int i = 0; i < this.dim; i++) {
			this.threadMap.add(null);
		}
		root.data = TripletHardCodeWordTricks.setDataWithTid(tid);
		threadMap.set(tid, root);
		this.rootTid = tid;
	}

	public TreeClock(short tid, int tval, int dim) {
		// //System.out.println("Initializing TC with " + dim + " threads");
		this.dim = dim;
		root = new EfficientTreeNodeHardCodeWordTricks();
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		for (int i = 0; i < this.dim; i++) {
			this.threadMap.add(null);
		}
		root.data = TripletHardCodeWordTricks.setDataWithTidAndClock(tid, tval);
		threadMap.set(tid, root);
		this.rootTid = tid;
	}

	public boolean threadMapIsConsistent() {
		if (this.root == null) {
			return true;
		}

		DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();
		S.pushLatest(this.root);
		while (!S.isEmpty()) {
			EfficientTreeNodeHardCodeWordTricks n = S.popLatest();
			if (this.threadMap.get(TripletHardCodeWordTricks.getTid(n.data)) != n) {
				/*
				 * //System.out.println("Failed on node " +
				 * TripletHardCodeWordTricks.toString(n.data));
				 * //System.out.println("Root is " +
				 * TripletHardCodeWordTricks.toString(this.root.data));
				 * //System.out.println("Is map entry null? " +
				 * (this.threadMap.get(TripletHardCodeWordTricks.getTid(n.data))==null));
				 * try {System.in.read();} catch(Exception e) {}
				 */
				return false;
			}
			EfficientTreeNodeHardCodeWordTricks children_itr = n.getHeadChild();
			while (children_itr != null) {
				S.pushLatest(children_itr);
				children_itr = children_itr.getNext();
			}
		}
		return true;
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

	private static EfficientTreeNodeHardCodeWordTricks deepCopyTree(
			EfficientTreeNodeHardCodeWordTricks n) {

		// Copy root
		EfficientTreeNodeHardCodeWordTricks n_copy = new EfficientTreeNodeHardCodeWordTricks();
		// n_copy.deepCopyData(n.getData());
		n_copy.data = n.data;

		// newThreadMap.set(TripletHardCodeWordTricks.getTid(n_copy.getData()), n_copy);

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
			// newThreadMap.set(TripletHardCodeWordTricks.getTid(c_copy.getData()),
			// c_copy);
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

	public TreeClock(TreeClock fromTreeClock) {
		this.dim = fromTreeClock.dim;
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(this.dim);
		/*
		 * for(int i=0; i<this.dim; i++) { this.threadMap.add(null); }
		 */

		Field f;
		try {
			f = ArrayList.class.getDeclaredField("size");
			f.setAccessible(true);
			f.setInt(this.threadMap, this.dim);
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

		// //System.out.println("Deep Copy");
		this.root = deepCopyTreeAndMap(fromTreeClock.root, this.threadMap);
		this.rootTid = fromTreeClock.rootTid;
		// this.pointersConsistent("DeepCopy on\n" + this.toString() + "\nfrom\n" +
		// fromTreeClock.toString() );
		// //System.out.println("Deep Copy Done");
	}

	public void deepCopy(TreeClock fromTreeClock) {
		this.threadMap = new ArrayList<EfficientTreeNodeHardCodeWordTricks>(dim);
		/*
		 * for(int i=0; i<this.dim; i++) { this.threadMap.add(null); }
		 */
		
		// LCOUNT + dim
		EventStatistics.updateLightGreyCount(dim);
		// DCOUNT + dim
		EventStatistics.updateDarkGreyCount(dim);

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

		this.root = deepCopyTreeAndMap(fromTreeClock.root, this.threadMap);
		this.rootTid = fromTreeClock.rootTid;
	}

	// Like deepCopy but does not initialize the threadMap
	public void deepCopyLight(TreeClock fromTreeClock) {
		this.dim = fromTreeClock.dim;
		this.root = deepCopyTree(fromTreeClock.root);
		this.rootTid = fromTreeClock.rootTid;

	}

	public int getLocalClock(short tid) {
		EfficientTreeNodeHardCodeWordTricks n = this.threadMap.get(tid);
		if (n != null) {
			return TripletHardCodeWordTricks.getClock(n.getData());
		}
		return -1;
	}

	public Long getLocalRootData() {
		return this.root.getData();
	}

	public void incrementBy(int val) {
		this.root.setData(
				TripletHardCodeWordTricks.incrementClockBy(val, this.root.getData()));
	}

	public boolean isLessThanOrEqual(TreeClock tc) {
		if (this.root == null) {
			return true;
		}
		return TripletHardCodeWordTricks.getClock(this.root.data) <= tc
				.getLocalClock(this.rootTid);

	}

	public void join(ForestClock tc) {

		// String initTree = this.toString() + "\n" + this.threadMapToString();
		// System.out.println("-------------JOIN FOREST
		// BEGIN--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree );

		EfficientTreeNodeHardCodeWordTricks zprime = tc.rootsHead;
		while (zprime != null) {

			long zprime_data = zprime.data;
			short zprime_tid = TripletHardCodeWordTricks.getTid(zprime_data);

			if (this.rootTid == zprime_tid) {
				zprime = zprime.getNext();
				continue;
			}

			int zprime_clock = TripletHardCodeWordTricks.getClock(zprime_data);
			int z_clock = -1;

			EfficientTreeNodeHardCodeWordTricks z = this.threadMap.get(zprime_tid);

			if (z == null) {
				z = new EfficientTreeNodeHardCodeWordTricks();
				threadMap.set(zprime_tid, z);
			} else {
				z_clock = TripletHardCodeWordTricks.getClock(z.data);
				if (zprime_clock <= z_clock) {
					zprime = zprime.getNext();
					continue;
				} else {
					EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(z);
				}
			}

			z.data = TripletHardCodeWordTricks.copyClockToPclock(this.root.data,
					zprime_data);

			if (this.root.getHeadChild() != null) {
				this.root.getHeadChild().setPrev(z);
			}
			// z.setPrev(null);
			z.setNext(this.root.getHeadChild());
			z.setParent(this.root);
			this.root.setHeadChild(z);

			DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();

			EfficientTreeNodeHardCodeWordTricks vprime = zprime.getHeadChild();
			while (vprime != null) {
				long vprime_data = vprime.getData();
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int v_clock = getLocalClock(vprime_tid);
				if (v_clock < TripletHardCodeWordTricks.getClock(vprime_data)) {
					S.pushLatest(vprime);
				} else {
					if (TripletHardCodeWordTricks.getPclock(vprime_data) <= z_clock) {
						break;
					}
				}
				vprime = vprime.getNext();
			}

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
					EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(u);
				}
				u.data = uprime_data;

				// Attach back
				EfficientTreeNodeHardCodeWordTricks y = this.threadMap
						.get(TripletHardCodeWordTricks.getTid(uprime.getParent().data));
				if (y.getHeadChild() != null) {
					y.getHeadChild().setPrev(u);
				}
				// u.setPrev(null);
				u.setNext(y.getHeadChild());
				u.setParent(y);
				y.setHeadChild(u);

				// Expand to children of u'
				vprime = uprime.getHeadChild();
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
			zprime = zprime.getNext();
		}

		// System.out.println("-------------JOIN FOREST END--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree + "\n\nfinalTree:\n" + this.toString()
		// + "\n" + this.threadMapToString());

	}

	public void join(TreeClock tc) {
		
		// LCOUNT + 1
		EventStatistics.updateLightGreyCount(1);

		if (tc.root == null || this.rootTid == tc.rootTid) {
			return;
		}

		EfficientTreeNodeHardCodeWordTricks zprime = tc.root;

		// String initTree = this.toString() + "\n" + this.threadMapToString();
//		System.out.println("-------------JOIN BEGIN--------------------------");
//		System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
//				+ "\n\nInitial Tree:\n" + initTree);

		long zprime_data = zprime.data;
		short zprime_tid = TripletHardCodeWordTricks.getTid(zprime_data);
		int zprime_clock = TripletHardCodeWordTricks.getClock(zprime_data);
		int z_clock = -1;

		EfficientTreeNodeHardCodeWordTricks z = this.threadMap.get(zprime_tid);

		if (z == null) {
			z = new EfficientTreeNodeHardCodeWordTricks();
			threadMap.set(zprime_tid, z);
		} else {
			z_clock = TripletHardCodeWordTricks.getClock(z.data);
			if (zprime_clock <= z_clock) {
				return;
			} else {
				EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(z);
			}
		}
		
		// DCOUNT + 1
		EventStatistics.updateDarkGreyCount(1);

		z.data = TripletHardCodeWordTricks.copyClockToPclock(this.root.data, zprime_data);

		if (this.root.getHeadChild() != null) {
			this.root.getHeadChild().setPrev(z);
		}
		// z.setPrev(null);
		z.setNext(this.root.getHeadChild());
		z.setParent(this.root);
		this.root.setHeadChild(z);

		DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();

		EfficientTreeNodeHardCodeWordTricks vprime = zprime.getHeadChild();
		while (vprime != null) {
			// LCOUNT + 1
			EventStatistics.updateLightGreyCount(1);
			long vprimeData = vprime.getData();
			short vprime_tid = TripletHardCodeWordTricks.getTid(vprimeData);
			int vprime_local_clock = getLocalClock(vprime_tid);
			if (vprime_local_clock < TripletHardCodeWordTricks.getClock(vprimeData)) {
				S.pushLatest(vprime);
				// DCOUNT + 1
				EventStatistics.updateDarkGreyCount(1);
			} else {
				if (TripletHardCodeWordTricks.getPclock(vprimeData) <= z_clock) {
					break;
				}
			}
			vprime = vprime.getNext();
		}

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
				EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(u);
			}
			u.data = uprime_data;

			// Attach back
			EfficientTreeNodeHardCodeWordTricks y = this.threadMap
					.get(TripletHardCodeWordTricks.getTid(uprime.getParent().data));
			if (y.getHeadChild() != null) {
				y.getHeadChild().setPrev(u);
			}
			// u.setPrev(null);
			u.setNext(y.getHeadChild());
			u.setParent(y);
			y.setHeadChild(u);

			// Expand to children of u'
			vprime = uprime.getHeadChild();
			while (vprime != null) {
				// LCOUNT + 1
				EventStatistics.updateLightGreyCount(1);
				long vprime_data = vprime.data;
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int v_clock = getLocalClock(vprime_tid);
				if (v_clock < TripletHardCodeWordTricks.getClock(vprime_data)) {
					// DCOUNT + 1
					EventStatistics.updateDarkGreyCount(1);
					S.pushLatest(vprime);
				} else {
					if (TripletHardCodeWordTricks.getPclock(vprime_data) <= u_clock) {
						break;
					}
				}
				vprime = vprime.getNext();
			}

		}

		// System.out.println("-------------JOIN END--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree + "\n\nfinalTree:\n" + this.toString() +
		// "\n" +
		// this.threadMapToString());

	}

	// Assumes tc.root is not null
	// Returns true if copy was monotone
	public boolean monotoneCopy(TreeClock tc) {
		// LCOUNT + 1
		EventStatistics.updateLightGreyCount(1);
		// DCOUNT + 1
		EventStatistics.updateDarkGreyCount(1);
		
		if (this.root == null) {
			this.deepCopy(tc);
			return false;
		}

		// String initTree = this.toString() + "\n" + this.threadMapToString();
		// System.out.println("-------------MONOTONECOPY
		// BEGIN--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree );

		EfficientTreeNodeHardCodeWordTricks zprime = tc.root;
		long zprime_data = zprime.data;
		short zprime_tid = TripletHardCodeWordTricks.getTid(zprime_data);
		int z_clock = -1;

		EfficientTreeNodeHardCodeWordTricks z = this.threadMap.get(zprime_tid);

		if (z == null) {
			z = new EfficientTreeNodeHardCodeWordTricks();
			threadMap.set(zprime_tid, z);
		} else {
			z_clock = TripletHardCodeWordTricks.getClock(z.data);
			if (zprime_tid != this.rootTid) {
				EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(z);
			}
		}

		z.data = zprime_data;
		// Make z the root - set the rootTid later!
		// z.setPrev(null);
		z.setParent(null);

		DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();

		// Handle the root first
		EfficientTreeNodeHardCodeWordTricks vprime = zprime.getHeadChild();
		while (vprime != null) {
			// LCOUNT + 1
			EventStatistics.updateLightGreyCount(1);
			long vprime_data = vprime.getData();
			short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
			int v_clock = getLocalClock(vprime_tid);
			if (v_clock < TripletHardCodeWordTricks.getClock(vprime_data)) {
				// DCOUNT + 1
				EventStatistics.updateDarkGreyCount(1);
				S.pushLatest(vprime);
			} else {
				if (vprime_tid == this.rootTid) {
					// DCOUNT + 1
					EventStatistics.updateDarkGreyCount(1);
					S.pushLatest(vprime);
				}
				if (TripletHardCodeWordTricks.getPclock(vprime_data) <= z_clock) {
					break;
				}
			}
			vprime = vprime.getNext();
		}

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNodeHardCodeWordTricks uprime = S.popLatest();
			long uprime_data = uprime.getData();
			short uprime_tid = TripletHardCodeWordTricks.getTid(uprime_data);
			EfficientTreeNodeHardCodeWordTricks u = this.threadMap.get(uprime_tid);

			int u_clock = -1;
			if (u == null) {
				u = new EfficientTreeNodeHardCodeWordTricks();
				threadMap.set(uprime_tid, u);
			} else {
				u_clock = TripletHardCodeWordTricks.getClock(u.data);
				if (uprime_tid != this.rootTid) {
					EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(u);
				}
			}
			u.data = uprime.data;
			// Attach back
			EfficientTreeNodeHardCodeWordTricks y = this.threadMap
					.get(TripletHardCodeWordTricks.getTid(uprime.getParent().data));
			if (y.getHeadChild() != null) {
				y.getHeadChild().setPrev(u);
			}
			u.setNext(y.getHeadChild());
			u.setParent(y);
			y.setHeadChild(u);

			// Expand to children of u'
			vprime = uprime.getHeadChild();
			while (vprime != null) {
				// LCOUNT + 1
				EventStatistics.updateLightGreyCount(1);
				long vprime_data = vprime.getData();
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int v_clock = getLocalClock(vprime_tid);
				if (v_clock < TripletHardCodeWordTricks.getClock(vprime_data)) {
					// DCOUNT + 1
					EventStatistics.updateDarkGreyCount(1);
					S.pushLatest(vprime);
				} else {
					if (vprime_tid == this.rootTid) {
						// DCOUNT + 1
						EventStatistics.updateDarkGreyCount(1);
						S.pushLatest(vprime);
					}
					if (TripletHardCodeWordTricks.getPclock(vprime_data) <= u_clock) {
						break;
					}
				}
				vprime = vprime.getNext();
			}

		}

		this.rootTid = zprime_tid;
		this.root = z;

		// System.out.println("-------------MONOTONECOPY
		// END--------------------------");
		// System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		// + "\n\nInitial Tree:\n" + initTree + "\n\nfinalTree:\n" + this.toString() +
		// "\n" + this.threadMapToString());

		return true;
	}

	@Override
	public String toString() {
		return this.root.toTreeString();
	}

	public String timesToString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (short i = 0; i < this.dim; i++) {
			sb.append(Math.max(this.getLocalClock(i), 0));
			if (i < this.dim - 1) {
				sb.append(", ");
			}
		}
		sb.append("]");
		return sb.toString();

	}
}