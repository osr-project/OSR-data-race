package util.tree;

import java.util.ArrayList;
import java.util.Iterator;

import util.TripletHardCodeWordTricks;

public class EfficientTreeNodeHardCodeWordTricks {
	public long data;
	private EfficientTreeNodeHardCodeWordTricks next;
	private EfficientTreeNodeHardCodeWordTricks prev;
	private EfficientTreeNodeHardCodeWordTricks parent;
	private EfficientTreeNodeHardCodeWordTricks headChild;

	public EfficientTreeNodeHardCodeWordTricks() {
		this.next = null;
		this.prev = null;
		this.parent = null;
		this.headChild = null;
	}

	public EfficientTreeNodeHardCodeWordTricks(long data) {
		this.data = data;
		this.next = null;
		this.prev = null;
		this.parent = null;
		this.headChild = null;
	}

	public long getData() {
		return this.data;
	}

	public void setData(long data) {
		this.data = data;
	}

	public EfficientTreeNodeHardCodeWordTricks getNext() {
		return this.next;
	}

	public void setNext(EfficientTreeNodeHardCodeWordTricks n) {
		this.next = n;
	}

	public boolean hasNext() {
		return !(this.next == null);
	}

	//

	public EfficientTreeNodeHardCodeWordTricks getPrev() {
		return this.prev;
	}

	public void setPrev(EfficientTreeNodeHardCodeWordTricks n) {
		this.prev = n;
	}

	public boolean hasPrev() {
		return !(this.prev == null);
	}

	//

	public EfficientTreeNodeHardCodeWordTricks getParent() {
		return this.parent;
	}

	public void setParent(EfficientTreeNodeHardCodeWordTricks n) {
		this.parent = n;
	}

	public boolean hasParent() {
		return !(this.parent == null);
	}

	//

	public EfficientTreeNodeHardCodeWordTricks getHeadChild() {
		return this.headChild;
	}

	public void setHeadChild(EfficientTreeNodeHardCodeWordTricks n) {
		this.headChild = n;
	}

	public boolean hasChildren() {
		return !(this.headChild == null);
	}

	//

	public void addLatestChild(EfficientTreeNodeHardCodeWordTricks n) {
		// n.prev = null;
		n.parent = this;
		n.next = this.headChild;
		if (!(this.headChild == null)) {
			this.headChild.prev = n;
		}
		this.headChild = n;
	}

	public void addChildren(ArrayList<EfficientTreeNodeHardCodeWordTricks> children) {

		Iterator<EfficientTreeNodeHardCodeWordTricks> children_it = children.iterator();
		EfficientTreeNodeHardCodeWordTricks u = children_it.next();
		this.headChild = u;
		u.parent = this;

		EfficientTreeNodeHardCodeWordTricks prevChild = u;

		while (children_it.hasNext()) {
			u = children_it.next();
			u.parent = this;
			u.prev = prevChild;
			prevChild.next = u;
			prevChild = u;
		}
	}

	//

	public static void detachFromNeighbors(EfficientTreeNodeHardCodeWordTricks n) {

		// System.out.println("Detaching " +
		// TripletHardCodeWordTricks.toString(n.data));

		EfficientTreeNodeHardCodeWordTricks n_next = n.next;
		EfficientTreeNodeHardCodeWordTricks n_prev = n.prev;
		EfficientTreeNodeHardCodeWordTricks n_parent = n.parent;

		// if n is the headchild of its parent, set the headchild to n.next
		if (!(n_parent == null)) {
			if (n_parent.headChild == n) {
				n_parent.headChild = n_next;
			} else {
				// n has a previous
				n_prev.next = n_next;
			}
		}

		// Now next
		if (!(n_next == null)) {
			n_next.prev = n_prev;
		}

		// n.next = null;
		// n.prev = null;
		// n.parent = null;

	}

	// We are guaranteed that n has a parent
	public static void detachFromNeighborsJoin(EfficientTreeNodeHardCodeWordTricks n) {

		EfficientTreeNodeHardCodeWordTricks n_next = n.next;
		EfficientTreeNodeHardCodeWordTricks n_prev = n.prev;
		EfficientTreeNodeHardCodeWordTricks n_parent = n.parent;

		if (n_parent.headChild == n) {
			n_parent.headChild = n_next;
		} else {
			// n has a previous
			n_prev.next = n_next;
		}

		// Now update the neighbors
		if (!(n_next == null)) {
			n_next.prev = n_prev;
		}

	}

	//

	public static EfficientTreeNodeHardCodeWordTricks deepCopyTree(
			EfficientTreeNodeHardCodeWordTricks n) {
		if (n == null)
			return null;
		else {
			EfficientTreeNodeHardCodeWordTricks n_copy = new EfficientTreeNodeHardCodeWordTricks();
			n_copy.data = n.data;
			EfficientTreeNodeHardCodeWordTricks children_itr = n.getHeadChild();
			boolean isHead = true;
			EfficientTreeNodeHardCodeWordTricks n_copy_children_prev = null;
			while (!(children_itr == null)) {
				EfficientTreeNodeHardCodeWordTricks child_copy = deepCopyTree(
						children_itr);
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

	//

	private String toTreeStringHelper(String pre, String delimiter) {
		String str = pre + TripletHardCodeWordTricks.toString(this.data);
		str = str + "\n";
		EfficientTreeNodeHardCodeWordTricks child_itr = this.headChild;
		while (child_itr != null) {
			str = str + child_itr.toTreeStringHelper(pre + delimiter, delimiter);
			child_itr = child_itr.next;
		}
		return str;
	}

	public String toTreeString() {
		return toTreeStringHelper("", "-");
	}

	@Override
	public String toString() {
		return TripletHardCodeWordTricks.toString(this.data);
	}
}
