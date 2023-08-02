package util.dll;

public class EfficientDLNode<T> {
	private T data;
	private EfficientDLNode<T> next;
	private EfficientDLNode<T> prev;
	
	public EfficientDLNode() {
		this.data = null;
		this.next = null;
		this.prev = null;
	}
	
	public EfficientDLNode(T data){
		this.data = data;
		this.next = null;
		this.prev = null;
	}
	
	public T getData(){
		return this.data;
	}
	
	public void setData(T data){
		this.data = data;
	}
	
	public EfficientDLNode<T> getNext(){
		return this.next;
	}
	
	public void setNext(EfficientDLNode<T> n){
		this.next = n;
	}
	
	public boolean hasNext(){
		return !(this.next == null);
	}
	
	//
	
	public EfficientDLNode<T> getPrev(){
		return this.prev;
	}
	
	public void setPrev(EfficientDLNode<T> n){
		this.prev = n;
	}
	
	public boolean hasPrev(){
		return !(this.prev == null);
	}
	
	//
	
	public static<T> void detachFromNeighbors(EfficientDLNode<T> n) {
		if(n.hasPrev()) {
			n.getPrev().setNext(n.getNext());
		}
		if(n.hasNext()) {
			n.getNext().setPrev(n.getPrev());
		}
	}

}	
