package engine.racedetection.onlinewcp;

import java.util.LinkedList;
import java.util.ListIterator;

import util.vectorclock.ClockPair;
import util.vectorclock.VectorClock;

public class Store {
	private int dim;
	private LinkedList<ClockPair> store;

	public Store(int n) {
		this.dim = n;
		this.store = new LinkedList<ClockPair>();
	}

	public int getLength() {
		return this.store.size();
	}

	public ClockPair bottom() {
		if (this.store.isEmpty()) {
			throw new IllegalArgumentException("Cannot get bottom: Store is empty");
		}
		return this.store.getFirst();
	}

	public ClockPair top() {
		if (this.store.isEmpty()) {
			throw new IllegalArgumentException("Cannot get top: Store is empty");
		}
		return this.store.getLast();

	}

	public ClockPair removeBottom() {
		if (this.store.isEmpty()) {
			throw new IllegalArgumentException("Cannot remove first: Store is empty");
		}
		return this.store.removeFirst();
	}

	public void removeBottomPrefix(int i) {
		if (i > this.store.size()) {
			throw new IllegalArgumentException("Array out of bound: removePrefix : i =" + Integer.toString(i)
					+ ", size = " + Integer.toString(this.store.size()));
		}
		for (int k = 0; k < i; k++) {
			this.removeBottom();
		}
	}

	public void pushTop(ClockPair clockPair) {
		if (clockPair.getDim() != this.dim) {
			throw new IllegalArgumentException("Dimension mismatch b/w store and clockpair");
		}
		this.store.add(clockPair);
	}

	public void removeTop() {
		this.store.removeLast();
	}
	
	public int getIndexOfAcquire(VectorClock vc){
		int index = -1;
		for(int i = 0; i < this.getLength(); i ++){
			if(vc.isEqual(this.store.get(i).getAcquire())){
				index = i;
				break;
			}
		}
		return index;
	}

	public ListIterator<ClockPair> getIterator(int index){
		if (this.getLength() < index + 1) {
			throw new IllegalArgumentException("Cannot get element at index " + Integer.toString(index) + " : Size of Store is " + Integer.toString(this.getLength()));
		}
		return this.store.listIterator(index);
	}
	
	public String toString(){
		return this.store.toString();
	}
}

