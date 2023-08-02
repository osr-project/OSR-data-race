package util.dll;

public class DoublyLinkedList<T>{
	private int sz;
	private EfficientDLNode<T> headNode;

	public DoublyLinkedList() {
		this.sz = 0;
		this.headNode = null;
	}

	public int getLength() {
		return this.sz;
	}

	public boolean isEmpty(){
		return (sz <= 0);
	}

	// Latest inserted element
	public T latest() {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot get latest: Store is empty");
		}
		return this.headNode.getData();
	}

	public void pushLatest(T data) {
		EfficientDLNode<T> newNode = new EfficientDLNode<T>(data);
		newNode.setNext(this.headNode);
		if(this.headNode != null) {
			this.headNode.setPrev(newNode);
		}
		this.headNode = newNode;
		
		this.sz = this.sz + 1;
	}
	
	public T popLatest() {
		if(sz <= 0) {
			throw new IllegalArgumentException("Cannot pop: Store is empty");
		}
		EfficientDLNode<T> headNode_old = this.headNode;
		this.headNode = this.headNode.getNext();
		if(this.headNode != null) {
			this.headNode.setPrev(null);		
		}
		this.sz = this.sz - 1;
		
		return headNode_old.getData();
	}

	public void setLatest(T data) {
		if (this.isEmpty()) {
			throw new IllegalArgumentException("Cannot get top: Store is empty");
		}
		this.headNode.setData(data);
	}

	public EfficientDLNode<T> getHeadNode(){
		return this.headNode;
	}

	public String toString(){
		String strPre = "[";
		String strPost = "]";
		String strMid = "";
		if(sz >= 1){
			strMid = strMid + this.headNode.getData().toString();
			EfficientDLNode<T> itrNode = this.headNode.getNext();
			for(int i = 1; i < sz; i ++){
				strMid = strMid + ", " + itrNode.getData().toString();
				itrNode = itrNode.getNext();
			}
		}
		return strPre + strMid + strPost;
	}
}

