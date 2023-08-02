package util.treeclock.word_tricks_map;

import java.util.HashMap;

import util.TripletHardCodeWordTricks;
//import util.Pair;
import util.dll.DoublyLinkedList;
import util.tree.EfficientTreeNodeHardCodeWordTricks;

public class TreeClock {

	public int dim;
	public EfficientTreeNodeHardCodeWordTricks root;
	public short rootTid = -1;
	// public HashMap<Short, EfficientTreeNodeHardCodeWordTricks> threadMap;
	public HashMap<Short, EfficientTreeNodeHardCodeWordTricks> threadMap;

	public TreeClock(int dim) {
		this.dim = dim;
		// root = new EfficientTreeNodeHardCodeWordTricks();
		this.root = null;
		this.threadMap = new HashMap<Short, EfficientTreeNodeHardCodeWordTricks>(dim);

	}

	public TreeClock(short tid, int dim) {
		this.dim = dim;
		root = new EfficientTreeNodeHardCodeWordTricks();
		this.threadMap = new HashMap<Short, EfficientTreeNodeHardCodeWordTricks>(dim);

		root.data = TripletHardCodeWordTricks.setDataWithTid(tid);
		threadMap.put(tid, root);
		this.rootTid = tid;
	}

	public TreeClock(short tid, int tval, int dim) {
		// System.out.println("Initializing TC with " + dim + " threads");
		this.dim = dim;
		root = new EfficientTreeNodeHardCodeWordTricks();
		this.threadMap = new HashMap<Short, EfficientTreeNodeHardCodeWordTricks>(dim);

		root.data = TripletHardCodeWordTricks.setDataWithTidAndClock(tid, tval);
		threadMap.put(tid, root);
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
				 * System.out.println("Failed on node " +
				 * TripletHardCodeWordTricks.toString(n.data));
				 * System.out.println("Root is " +
				 * TripletHardCodeWordTricks.toString(this.root.data));
				 * System.out.println("Is map entry null? " +
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

		if (this.dim < 1) {
			System.out.println("ERROR, DIMENSION is 0");
		}

		String s = "";
		for (short i = 0; i < this.dim; i++) {
			EfficientTreeNodeHardCodeWordTricks u = this.threadMap.get(i);
			if (u == null) {
				s += "null\t";
			} else {
				s += TripletHardCodeWordTricks.toString(u.data) + "\t";
			}
		}
		return s;
	}

	public int numActiveThreadsMap() {
		return this.threadMap.size();
	}

	public int numActiveThreadsTree() {
		int s = 0;

		DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();
		S.pushLatest(this.root);

		while (!S.isEmpty()) {
			s += 1;
			EfficientTreeNodeHardCodeWordTricks u = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks children_itr = u.getHeadChild();
			while (children_itr != null) {
				S.pushLatest(children_itr);
				children_itr = children_itr.getNext();
			}
		}

		return s;
	}

	public boolean pointersConsistent(String diagnostic) {

		for (short i = 0; i < this.dim; i++) {
			EfficientTreeNodeHardCodeWordTricks u = this.threadMap.get(i);

			if (u != null) {

				if (u != this.root && u.getParent() == null) {
					System.out.println("Pointers failing on "
							+ TripletHardCodeWordTricks.toString(u.data));
					System.out.println("u is not root but parent is null");
					System.out.println("The root is "
							+ TripletHardCodeWordTricks.toString(this.root.data));
					System.out.println("Diagnostic\n" + diagnostic);
					try {
						System.in.read();
					} catch (Exception e) {
					}
				}

				if (u.hasParent()) {
					EfficientTreeNodeHardCodeWordTricks uParent = u.getParent();
					EfficientTreeNodeHardCodeWordTricks uPrev = u.getPrev();
					EfficientTreeNodeHardCodeWordTricks uNext = u.getNext();

					if (uParent.getHeadChild() == null) {
						System.out.println("Pointers failing on "
								+ TripletHardCodeWordTricks.toString(u.data));
						System.out.println("Parent's headChild is null");
						System.out.println("Parent is "
								+ TripletHardCodeWordTricks.toString(uParent.data));
						System.out.println("Diagnostic\n" + diagnostic);
						try {
							System.in.read();
						} catch (Exception e) {
						}
					}

					if (uParent.getHeadChild() != u) {
						if (uPrev == null) {
							System.out.println("Pointers failing on "
									+ TripletHardCodeWordTricks.toString(u.data));
							System.out.println("Not head child but prev is null");
							System.out
									.println("Head child is " + TripletHardCodeWordTricks
											.toString(uParent.getHeadChild().data));
							System.out.println("Diagnostic\n" + diagnostic);
							try {
								System.in.read();
							} catch (Exception e) {
							}
						} else if (uPrev.getNext() != u) {
							System.out.println("Pointers failing on "
									+ TripletHardCodeWordTricks.toString(u.data));
							System.out.println(
									"Not head child, but prev's next does not point to me");
							EfficientTreeNodeHardCodeWordTricks v = uPrev.getNext();
							System.out.println("Prev's next points to " + (v != null
									? TripletHardCodeWordTricks.toString(v.data)
									: "null"));
							System.out.println("Diagnostic\n" + diagnostic);
							try {
								System.in.read();
							} catch (Exception e) {
							}

						}
					}
				}
			}
		}

		return true;
	}

	private static EfficientTreeNodeHardCodeWordTricks deepCopyTreeAndMap(
			EfficientTreeNodeHardCodeWordTricks n,
			HashMap<Short, EfficientTreeNodeHardCodeWordTricks> newThreadMap) {
		if (n == null) {
			return null;
		}

		// Copy root
		EfficientTreeNodeHardCodeWordTricks n_copy = new EfficientTreeNodeHardCodeWordTricks();
		// n_copy.deepCopyData(n.getData());
		n_copy.data = n.data;

		newThreadMap.put(TripletHardCodeWordTricks.getTid(n_copy.getData()), n_copy);

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
			newThreadMap.put(TripletHardCodeWordTricks.getTid(c_copy.getData()), c_copy);
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
		this.threadMap = new HashMap<Short, EfficientTreeNodeHardCodeWordTricks>();
		// System.out.println("Deep Copy");
		this.root = deepCopyTreeAndMap(fromTreeClock.root, this.threadMap);
		this.rootTid = fromTreeClock.rootTid;
		// this.pointersConsistent("DeepCopy on\n" + this.toString() + "\nfrom\n" +
		// fromTreeClock.toString() );
		// System.out.println("Deep Copy Done");
	}

	public void deepCopy(TreeClock fromTreeClock) {
		this.threadMap = new HashMap<Short, EfficientTreeNodeHardCodeWordTricks>();
		this.root = deepCopyTreeAndMap(fromTreeClock.root, this.threadMap);
		// this.pointersConsistent("DeepCopy on\n" + this.toString() + "\n" +
		// this.threadMapToString() + "\nfrom\n" + fromTreeClock.toString() + "\n" +
		// fromTreeClock.threadMapToString());
		this.rootTid = fromTreeClock.rootTid;
	}

	public int getLocalClock(short tid) {
		EfficientTreeNodeHardCodeWordTricks n = this.threadMap.get(tid);
		if (n != null) {
			return TripletHardCodeWordTricks.getClock(n.getData());
		}
		return 0;
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

	public void join(TreeClock tc) {

		if (tc.root == null || this.rootTid == tc.rootTid) {
			return;
		}

		/*
		 * String initTree = this.toString() + "\n" + this.threadMapToString();
		 * System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		 * + "\n\nInitial Tree:\n" + initTree);
		 */
		EfficientTreeNodeHardCodeWordTricks zprime = tc.root;

		long zprime_data = zprime.data;
		short zprime_tid = TripletHardCodeWordTricks.getTid(zprime_data);
		int zprime_clock = TripletHardCodeWordTricks.getClock(zprime_data);
		EfficientTreeNodeHardCodeWordTricks z = this.threadMap.get(zprime_tid);
		int z_clock = -1;

		if (z == null) {
			z = new EfficientTreeNodeHardCodeWordTricks();
			this.threadMap.put(zprime_tid, z);
		} else {
			z_clock = TripletHardCodeWordTricks.getClock(z.data);
			if (zprime_clock <= z_clock) {
				return;
			}

			else {
				EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(z);
			}
		}

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
			long vprime_data = vprime.getData();
			short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
			int vprime_clock = TripletHardCodeWordTricks.getClock(vprime_data);
			EfficientTreeNodeHardCodeWordTricks v = this.threadMap.get(vprime_tid);
			int v_clock = -1;

			if (v == null) {
				v = new EfficientTreeNodeHardCodeWordTricks();
				this.threadMap.put(vprime_tid, v);
				S.pushLatest(vprime);
				S.pushLatest(v);
				S.pushLatest(z);
			} else {
				v_clock = TripletHardCodeWordTricks.getClock(v.data);
				if (v_clock < vprime_clock) {
					EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(v);
					S.pushLatest(vprime);
					S.pushLatest(v);
					S.pushLatest(z);
				} else {
					int vprime_pclock = TripletHardCodeWordTricks.getPclock(vprime_data);
					if (vprime_pclock <= z_clock) {
						break;
					}
				}
			}
			vprime = vprime.getNext();
		}

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNodeHardCodeWordTricks u_parent = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks u = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks uprime = S.popLatest();

			int u_clock = TripletHardCodeWordTricks.getClock(u.data);
			u.data = uprime.data;

			// Attach back
			if (u_parent.getHeadChild() != null) {
				u_parent.getHeadChild().setPrev(u);
			}
			u.setNext(u_parent.getHeadChild());
			u.setParent(u_parent);
			u_parent.setHeadChild(u);

			// Iterate over the children of uprime
			u_parent = u;
			vprime = uprime.getHeadChild();
			while (vprime != null) {
				long vprime_data = vprime.getData();
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int vprime_clock = TripletHardCodeWordTricks.getClock(vprime_data);
				EfficientTreeNodeHardCodeWordTricks v = this.threadMap.get(vprime_tid);
				int v_clock = -1;

				if (v == null) {
					v = new EfficientTreeNodeHardCodeWordTricks();
					this.threadMap.put(vprime_tid, v);
					S.pushLatest(vprime);
					S.pushLatest(v);
					S.pushLatest(u);
				} else {
					v_clock = TripletHardCodeWordTricks.getClock(v.data);
					if (v_clock < vprime_clock) {
						EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(v);
						S.pushLatest(vprime);
						S.pushLatest(v);
						S.pushLatest(u);
					} else {
						int vprime_pclock = TripletHardCodeWordTricks
								.getPclock(vprime_data);
						if (vprime_pclock <= u_clock) {
							break;
						}
					}
				}
				vprime = vprime.getNext();
			}
		}

		/*
		 * System.out.println("-------------JOIN END--------------------------");
		 * System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		 * + "\n\nInitial Tree:\n" + initTree + "\n\nfinalTree:\n" + this.toString() +
		 * "\n" + this.threadMapToString()); this.pointersConsistent("Join END on\n");
		 */
	}

	public void subRootJoin(TreeClock tc) {

		if (tc.root == null) {
			return;
		}

		/*
		 * String initTree = this.toString() + "\n" + this.threadMapToString();
		 * System.out.println("-------------SUBROOTJOIN BEGIN--------------------------"
		 * ); System.out.println("From:\n" + tc.toString() + "\n" +
		 * tc.threadMapToString() + "\n\nInitial Tree:\n" + initTree );
		 */

		DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> Z = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();
		EfficientTreeNodeHardCodeWordTricks zprime = tc.root;
		long zprime_data = zprime.data;
		short zprime_tid = TripletHardCodeWordTricks.getTid(zprime_data);
		int zprime_clock = TripletHardCodeWordTricks.getClock(zprime_data);
		EfficientTreeNodeHardCodeWordTricks z = this.threadMap.get(zprime_tid);
		int z_clock = -1;

		if (z == null) {
			z = new EfficientTreeNodeHardCodeWordTricks();
			this.threadMap.put(zprime_tid, z);
			Z.pushLatest(zprime);
			Z.pushLatest(z);
		} else {
			z_clock = TripletHardCodeWordTricks.getClock(z.data);
			z_clock = TripletHardCodeWordTricks.getClock(z.data);
			if (z_clock < zprime_clock) {
				EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(z);
				Z.pushLatest(zprime);
				Z.pushLatest(z);
			}

		}

		EfficientTreeNodeHardCodeWordTricks cprime = tc.root.getHeadChild();
		while (zprime != null) {
			long cprime_data = zprime.data;
			short cprime_tid = TripletHardCodeWordTricks.getTid(cprime_data);
			int cprime_clock = TripletHardCodeWordTricks.getClock(cprime_data);
			EfficientTreeNodeHardCodeWordTricks c = this.threadMap.get(cprime_tid);
			int c_clock = -1;

			if (c == null) {
				c = new EfficientTreeNodeHardCodeWordTricks();
				this.threadMap.put(cprime_tid, c);
				Z.pushLatest(cprime);
				Z.pushLatest(c);
			} else {
				c_clock = TripletHardCodeWordTricks.getClock(c.data);
				if (c_clock < cprime_clock) {
					EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(c);
					Z.pushLatest(cprime);
					Z.pushLatest(c);
				} else {
					int cprime_pclock = TripletHardCodeWordTricks.getPclock(cprime_data);
					if (cprime_pclock <= z_clock) {
						break;
					}

				}

			}
		}

		DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks> S = new DoublyLinkedList<EfficientTreeNodeHardCodeWordTricks>();

		while (!Z.isEmpty()) {
			z = Z.popLatest();
			zprime = Z.popLatest();

			z_clock = TripletHardCodeWordTricks.getClock(z.data);
			z.data = zprime.data;

			// Attach to the root
			if (this.root.getHeadChild() != null) {
				this.root.getHeadChild().setPrev(z);
			}
			z.setNext(this.root.getHeadChild());
			z.setParent(this.root);
			this.root.setHeadChild(z);

			// Iterate over the children of zprime
			EfficientTreeNodeHardCodeWordTricks vprime = zprime.getHeadChild();
			while (vprime != null) {
				long vprime_data = vprime.getData();
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int vprime_clock = TripletHardCodeWordTricks.getClock(vprime_data);
				EfficientTreeNodeHardCodeWordTricks v = this.threadMap.get(vprime_tid);
				int v_clock = -1;

				if (v == null) {
					v = new EfficientTreeNodeHardCodeWordTricks();
					this.threadMap.put(vprime_tid, v);
					S.pushLatest(vprime);
					S.pushLatest(v);
					S.pushLatest(z);
				} else {
					v_clock = TripletHardCodeWordTricks.getClock(v.data);
					if (v_clock < vprime_clock) {
						EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(v);
						S.pushLatest(vprime);
						S.pushLatest(v);
						S.pushLatest(z);
					} else {
						int vprime_pclock = TripletHardCodeWordTricks
								.getPclock(vprime_data);
						if (vprime_pclock <= z_clock) {
							break;
						}
					}
				}
				vprime = vprime.getNext();
			}
		}

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNodeHardCodeWordTricks u_parent = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks u = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks uprime = S.popLatest();

			int u_clock = TripletHardCodeWordTricks.getClock(u.data);
			u.data = uprime.data;

			// Attach back
			if (u_parent.getHeadChild() != null) {
				u_parent.getHeadChild().setPrev(u);
			}
			u.setNext(u_parent.getHeadChild());
			u.setParent(u_parent);
			u_parent.setHeadChild(u);

			// Iterate over the children of uprime
			u_parent = u;
			EfficientTreeNodeHardCodeWordTricks vprime = uprime.getHeadChild();
			while (vprime != null) {
				long vprime_data = vprime.getData();
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int vprime_clock = TripletHardCodeWordTricks.getClock(vprime_data);
				EfficientTreeNodeHardCodeWordTricks v = this.threadMap.get(vprime_tid);
				int v_clock = -1;

				if (v == null) {
					v = new EfficientTreeNodeHardCodeWordTricks();
					this.threadMap.put(vprime_tid, v);
					S.pushLatest(vprime);
					S.pushLatest(v);
					S.pushLatest(u);
				} else {
					v_clock = TripletHardCodeWordTricks.getClock(v.data);
					if (v_clock < vprime_clock) {
						EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(v);
						S.pushLatest(vprime);
						S.pushLatest(v);
						S.pushLatest(u);
					} else {
						int vprime_pclock = TripletHardCodeWordTricks
								.getPclock(vprime_data);
						if (vprime_pclock <= u_clock) {
							break;
						}
					}
				}
				vprime = vprime.getNext();
			}
		}

		/*
		 * System.out.println("-------------SUBROOTJOIN END--------------------------");
		 * System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		 * + "\n\nInitial Tree:\n" + initTree + "\nfinalTree:\n" + this.toString() +
		 * "\n" + this.threadMapToString()); this.pointersConsistent("Join END on\n");
		 */

	}

	// Assumes tc.root is not null
	// Returns true if copy was monotone
	public boolean monotoneCopy(TreeClock tc) {

		/*
		 * System.out.println("== MONOTONECOPY ====");
		 * System.out.println("== self ===="); System.out.println(toString());
		 * System.out.println("== tc ===="); System.out.println(tc.toString());
		 */

		if (this.root == null) {
			this.deepCopy(tc);
			return false;
		}

		/*
		 * String initTree = this.toString() + "\n" + this.threadMapToString();
		 * System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		 * + "\n\nInitial Tree:\n" + initTree);
		 */

		EfficientTreeNodeHardCodeWordTricks zprime = tc.root;
		long zprime_data = zprime.data;
		short zprime_tid = TripletHardCodeWordTricks.getTid(zprime_data);
		EfficientTreeNodeHardCodeWordTricks z = this.threadMap.get(zprime_tid);
		int z_clock = -1;

		if (z == null) {
			z = new EfficientTreeNodeHardCodeWordTricks();
			threadMap.put(zprime_tid, z);
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

		EfficientTreeNodeHardCodeWordTricks vprime = zprime.getHeadChild();
		while (vprime != null) {
			long vprime_data = vprime.getData();
			short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
			int vprime_clock = TripletHardCodeWordTricks.getClock(vprime_data);
			EfficientTreeNodeHardCodeWordTricks v = this.threadMap.get(vprime_tid);
			int v_clock = -1;

			if (v == null) {
				v = new EfficientTreeNodeHardCodeWordTricks();
				this.threadMap.put(vprime_tid, v);
				S.pushLatest(vprime);
				S.pushLatest(v);
				S.pushLatest(z);
			} else {
				v_clock = TripletHardCodeWordTricks.getClock(v.data);
				if (v_clock < vprime_clock) {
					if (vprime_tid != this.rootTid) {
						EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(v);
					}
					S.pushLatest(vprime);
					S.pushLatest(v);
					S.pushLatest(z);
				} else {
					if (vprime_tid == this.rootTid) {
						S.pushLatest(vprime);
						S.pushLatest(v);
						S.pushLatest(z);
					}
					int vprime_pclock = TripletHardCodeWordTricks.getPclock(vprime_data);
					if (vprime_pclock <= z_clock) {
						break;
					}
				}
			}
			vprime = vprime.getNext();
		}

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNodeHardCodeWordTricks u_parent = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks u = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks uprime = S.popLatest();

			u.data = uprime.data;

			// Attach back
			if (u_parent.getHeadChild() != null) {
				u_parent.getHeadChild().setPrev(u);
			}
			u.setNext(u_parent.getHeadChild());
			u.setParent(u_parent);
			u_parent.setHeadChild(u);

			// Iterate over the children of uprime
			u_parent = u;
			vprime = uprime.getHeadChild();
			while (vprime != null) {
				long vprime_data = vprime.getData();
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int vprime_clock = TripletHardCodeWordTricks.getClock(vprime_data);
				EfficientTreeNodeHardCodeWordTricks v = this.threadMap.get(vprime_tid);
				int v_clock = -1;

				if (v == null) {
					v = new EfficientTreeNodeHardCodeWordTricks();
					this.threadMap.put(vprime_tid, v);
					S.pushLatest(vprime);
					S.pushLatest(v);
					S.pushLatest(u);
				} else {
					v_clock = TripletHardCodeWordTricks.getClock(v.data);
					if (v_clock < vprime_clock) {
						if (vprime_tid != this.rootTid) {
							EfficientTreeNodeHardCodeWordTricks
									.detachFromNeighborsJoin(v);
						}
						S.pushLatest(vprime);
						S.pushLatest(v);
						S.pushLatest(u);
					} else {
						if (vprime_tid == this.rootTid) {
							S.pushLatest(vprime);
							S.pushLatest(v);
							S.pushLatest(u);
						}
						int vprime_pclock = TripletHardCodeWordTricks
								.getPclock(vprime_data);
						if (vprime_pclock <= z_clock) {
							break;
						}
					}
				}
				vprime = vprime.getNext();
			}
		}
		this.rootTid = zprime_tid;
		this.root = z;

		/*
		 * System.out.println("-------------MONOTONECOPY END--------------------------"
		 * ); System.out.println("From:\n" + tc.toString() + "\n" +
		 * tc.threadMapToString() + "\n\nInitial Tree:\n" + initTree +
		 * "\n\nfinalTree:\n" + this.toString() + "\n" + this.threadMapToString());
		 * this.pointersConsistent("MonotoneCopy END on\n");
		 */

		return true;

	}

	// This is essentially monotonecopy with a final step that attaches the current
	// root under the old root, if the old root is not known to tc
	public void joinReroot(TreeClock tc) {

		if (this.root == null) {
			this.deepCopy(tc);
			return;
		}

		/*
		 * String initTree = this.toString() + "\n" + this.threadMapToString();
		 * System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		 * + "\n\nInitial Tree:\n" + initTree);
		 */

		EfficientTreeNodeHardCodeWordTricks zprime = tc.root;
		long zprime_data = zprime.data;
		short zprime_tid = TripletHardCodeWordTricks.getTid(zprime_data);
		EfficientTreeNodeHardCodeWordTricks z = this.threadMap.get(zprime_tid);
		int z_clock = -1;

		if (z == null) {
			z = new EfficientTreeNodeHardCodeWordTricks();
			threadMap.put(zprime_tid, z);
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

		EfficientTreeNodeHardCodeWordTricks vprime = zprime.getHeadChild();
		while (vprime != null) {
			long vprime_data = vprime.getData();
			short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
			int vprime_clock = TripletHardCodeWordTricks.getClock(vprime_data);
			EfficientTreeNodeHardCodeWordTricks v = this.threadMap.get(vprime_tid);
			int v_clock = -1;

			if (v == null) {
				v = new EfficientTreeNodeHardCodeWordTricks();
				this.threadMap.put(vprime_tid, v);
				S.pushLatest(vprime);
				S.pushLatest(v);
				S.pushLatest(z);
			} else {
				v_clock = TripletHardCodeWordTricks.getClock(v.data);
				if (v_clock < vprime_clock) {
					if (vprime_tid != this.rootTid) {
						EfficientTreeNodeHardCodeWordTricks.detachFromNeighborsJoin(v);
					}
					S.pushLatest(vprime);
					S.pushLatest(v);
					S.pushLatest(z);
				} else {
					if (vprime_tid == this.rootTid) {
						S.pushLatest(vprime);
						S.pushLatest(v);
						S.pushLatest(z);
					}
					int vprime_pclock = TripletHardCodeWordTricks.getPclock(vprime_data);
					if (vprime_pclock <= z_clock) {
						break;
					}
				}
			}
			vprime = vprime.getNext();
		}

		// Handle the rest
		while (!S.isEmpty()) {
			EfficientTreeNodeHardCodeWordTricks u_parent = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks u = S.popLatest();
			EfficientTreeNodeHardCodeWordTricks uprime = S.popLatest();

			u.data = uprime.data;

			// Attach back
			if (u_parent.getHeadChild() != null) {
				u_parent.getHeadChild().setPrev(u);
			}
			u.setNext(u_parent.getHeadChild());
			u.setParent(u_parent);
			u_parent.setHeadChild(u);

			// Iterate over the children of uprime
			u_parent = u;
			vprime = uprime.getHeadChild();
			while (vprime != null) {
				long vprime_data = vprime.getData();
				short vprime_tid = TripletHardCodeWordTricks.getTid(vprime_data);
				int vprime_clock = TripletHardCodeWordTricks.getClock(vprime_data);
				EfficientTreeNodeHardCodeWordTricks v = this.threadMap.get(vprime_tid);
				int v_clock = -1;

				if (v == null) {
					v = new EfficientTreeNodeHardCodeWordTricks();
					this.threadMap.put(vprime_tid, v);
					S.pushLatest(vprime);
					S.pushLatest(v);
					S.pushLatest(u);
				} else {
					v_clock = TripletHardCodeWordTricks.getClock(v.data);
					if (v_clock < vprime_clock) {
						if (vprime_tid != this.rootTid) {
							EfficientTreeNodeHardCodeWordTricks
									.detachFromNeighborsJoin(v);
						}
						S.pushLatest(vprime);
						S.pushLatest(v);
						S.pushLatest(u);
					} else {
						if (vprime_tid == this.rootTid) {
							S.pushLatest(vprime);
							S.pushLatest(v);
							S.pushLatest(u);
						}
						int vprime_pclock = TripletHardCodeWordTricks
								.getPclock(vprime_data);
						if (vprime_pclock <= z_clock) {
							break;
						}
					}
				}
				vprime = vprime.getNext();
			}
		}

		// If this.root is not known to tc, attach the remaining subtree
		if (tc.getLocalClock(this.rootTid) < TripletHardCodeWordTricks
				.getClock(this.root.data)) {
			this.root.data = TripletHardCodeWordTricks.copyClockToPclock(z.data,
					this.root.data);
			if (z.getHeadChild() != null) {
				z.getHeadChild().setPrev(this.root);
			}
			this.root.setNext(z.getHeadChild());
			this.root.setParent(z);
			z.setHeadChild(this.root);
		}

		this.rootTid = zprime_tid;
		this.root = z;

		/*
		 * System.out.println("-------------JOINREROOT END--------------------------");
		 * System.out.println("From:\n" + tc.toString() + "\n" + tc.threadMapToString()
		 * + "\n\nInitial Tree:\n" + initTree + "\n\nfinalTree:\n" + this.toString() +
		 * "\n" + this.threadMapToString());
		 * this.pointersConsistent("MonotoneCopy END on\n");
		 */

	}

	@Override
	public String toString() {
		if (root == null)
			return "NULL";
		return this.root.toTreeString();
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