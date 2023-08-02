package util.tree;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class EfficientTreeNode<T> {
	private T data;
	private EfficientTreeNode<T> next;
	private EfficientTreeNode<T> prev;
	private EfficientTreeNode<T> parent;
	private EfficientTreeNode<T> headChild;

	public EfficientTreeNode() {
		this.data = null;
		this.next = null;
		this.prev = null;
		this.parent = null;
		this.headChild = null;
	}

	public EfficientTreeNode(T data) {
		this.data = data;
		this.next = null;
		this.prev = null;
		this.parent = null;
		this.headChild = null;
	}

	public T getData() {
		return this.data;
	}

	public void setData(T data) {
		this.data = data;
	}

	@SuppressWarnings("unchecked")
	public void deepCopyData(T data) {
//		this.data = data;
		Class<?> clazz = data.getClass();
		try {
			Constructor<?> copyConstructor = clazz.getConstructor(clazz);
			try {
				this.data = (T) copyConstructor.newInstance(data);
			} catch (InstantiationException | IllegalAccessException
					| IllegalArgumentException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} catch (NoSuchMethodException | SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public EfficientTreeNode<T> getNext() {
		return this.next;
	}

	public void setNext(EfficientTreeNode<T> n) {
		this.next = n;
	}

	public boolean hasNext() {
		return !(this.next == null);
	}

	//

	public EfficientTreeNode<T> getPrev() {
		return this.prev;
	}

	public void setPrev(EfficientTreeNode<T> n) {
		this.prev = n;
	}

	public boolean hasPrev() {
		return !(this.prev == null);
	}

	//

	public EfficientTreeNode<T> getParent() {
		return this.parent;
	}

	public void setParent(EfficientTreeNode<T> n) {
		this.parent = n;
	}

	public boolean hasParent() {
		return !(this.parent == null);
	}

	//

	public EfficientTreeNode<T> getHeadChild() {
		return this.headChild;
	}

	public void setHeadChild(EfficientTreeNode<T> n) {
		this.headChild = n;
	}

	public boolean hasChildren() {
		return !(this.headChild == null);
	}

	//

	public void addLatestChild(EfficientTreeNode<T> n) {
		n.prev = null;
		n.parent = this;
		n.next = this.headChild;
		if (!(this.headChild == null)) {
			this.headChild.prev = n;
		}
		this.headChild = n;
	}

	//

	public static <T> void detachFromNeighbors(EfficientTreeNode<T> n) {

		EfficientTreeNode<T> n_next = n.next;
		EfficientTreeNode<T> n_prev = n.prev;
		EfficientTreeNode<T> n_parent = n.parent;

		// if n is the headchild of its parent, set the headchild to n.next
		if (!(n_parent == null)) {
			if (n_parent.headChild == n) {
				n_parent.headChild = n_next;
			}
		}

		// Now update the neighbors
		if (!(n_next == null)) {
			n_next.prev = n_prev;
		}
		if (!(n_prev == null)) {
			n_prev.next = n_next;
		}

		n.next = null;
		n.prev = null;
		n.parent = null;
	}

	//

	public static <T> EfficientTreeNode<T> deepCopyTree(EfficientTreeNode<T> n) {
		if (n == null)
			return null;
		else {
			EfficientTreeNode<T> n_copy = new EfficientTreeNode<T>();
			n_copy.deepCopyData(n.getData());
			EfficientTreeNode<T> children_itr = n.getHeadChild();
			boolean isHead = true;
			EfficientTreeNode<T> n_copy_children_prev = null;
			while (!(children_itr == null)) {
				EfficientTreeNode<T> child_copy = deepCopyTree(children_itr);
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
		String str = pre + this.data.toString();
		str = str + "\n";
		EfficientTreeNode<T> child_itr = this.headChild;
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
		return data.toString();
	}
}
