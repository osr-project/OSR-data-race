package util.vectorclock;

import java.util.Vector;

public class GenericVectorClock<N extends Number & Comparable<N>> {

	protected int dim;
	protected Vector<N> clock;

	public GenericVectorClock(int d, N sentinel) {
		this.dim = d;
		this.clock = new Vector<N>(dim);
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.addElement(sentinel);
		}
	}

	public GenericVectorClock(GenericVectorClock<N> fromVectorClock) {
		this.dim = fromVectorClock.getDim();
		this.clock = new Vector<N>(dim);
		Vector<N> fromClock = fromVectorClock.getClock();
		for (int ind = 0; ind < fromVectorClock.getDim(); ind++) {
			this.clock.addElement(fromClock.get(ind));
		}
	}

	public int getDim() {
		if(!(this.dim == this.clock.size())){
			throw new IllegalArgumentException("Mismatch in dim and clock size"); 
		}
		return this.dim;
	}

	public Vector<N> getClock() {
		return this.clock;
	}

	public String toString() {
		return this.clock.toString();
	}

	/* public void inc(int ind) {
		if(! ((ind < this.dim) && (ind >= 0)) ){
			throw new IllegalArgumentException("You are attempting to access the vector clock with an illegal index");
		}
		int new_clock_val = this.clock.get(ind) + 1;
		this.clock.set(ind, (Integer) new_clock_val);
	} */

	public boolean isZero() {
		boolean itIsZero = true;
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind).intValue();
			if (thisVal != 0) {
				itIsZero = false;
				break;
			}
		}
		return itIsZero;
	}

	public boolean isEqual(GenericVectorClock<N> vc) {
		if(! (this.dim == vc.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		boolean itIsEqual = true;
		Vector<N> vcClock = vc.getClock();
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind).intValue();
			int vcVal = vcClock.get(ind).intValue();
			// System.out.println("Comparing: " + thisVal + " | " + vcVal);
			if (thisVal != vcVal) {
				itIsEqual = false;
				break;
			}
		}
		return itIsEqual;
	}

	public boolean isLessThanOrEqual(GenericVectorClock<N> vc) {
		if(! (this.dim == vc.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		boolean itIsLessThanOrEqual = true;
		Vector<N> vcClock = vc.getClock();
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind).intValue();
			int vcVal = vcClock.get(ind).intValue();
			if (!(thisVal <= vcVal)) {
				itIsLessThanOrEqual = false;
				break;
			}
		}
		return itIsLessThanOrEqual;
	}
	
	public void setToSentinel(N sentinel) {
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.set(ind, sentinel );
		}
	}
	
	public void copyFrom(GenericVectorClock<N> vc) {
		if(! (this.dim == vc.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.set(ind, vc.clock.get(ind));
		}
	}	
	
	private void updateMax2(GenericVectorClock<N> vc) {
		if(! (this.dim == vc.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		for (int ind = 0; ind < this.dim; ind++) {
			N this_c = this.clock.get(ind);
			N vc_c = vc.clock.get(ind);
			N max_c = (this_c.compareTo(vc_c)) >= 0 ? this_c : vc_c;
			this.clock.set(ind,max_c);
		}
	}
	
	//The following function update this as : this := \lambda t . if t == tIndex then this[tIndex] else max(this[t], vc[t])
	public void updateMax2WithoutLocal(GenericVectorClock<N> vc, int tIndex) {
		if(! (this.dim == vc.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		for (int ind = 0; ind < this.dim; ind++) {
			if(ind != tIndex){
				N this_c = this.clock.get(ind);
				N vc_c = vc.clock.get(ind);
				N max_c = (this_c.compareTo(vc_c)) < 0 ? this_c : vc_c;
				this.clock.set(ind, max_c);
			}
		}
	}
	
	public void updateWithMax(GenericVectorClock<N>... vcList) {
		if(! (vcList.length >= 1) ){
			throw new IllegalArgumentException("Insuffiecient number of arguments provided"); 
		}
		for (int i = 1; i < vcList.length; i++) {
			if (vcList[i].equals(this)) throw new IllegalArgumentException("If \'this\' is one of the arguments, then it must be the first");
		}

		//this.setToZero();
		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {
			GenericVectorClock<N> vc = vcList[i];
			if (! (this.dim == vc.getDim()) ) {
				throw new IllegalArgumentException("Mismatch in maxVC.dim and vc.dim");
			}
			this.updateMax2(vc);
		}
	}
	
	private void updateMin2(GenericVectorClock<N> vc) {
		if(! (this.dim == vc.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		for (int ind = 0; ind < this.dim; ind++) {
			N this_c = this.clock.get(ind);
			N vc_c = vc.clock.get(ind);
			N min_c = (this_c.compareTo(vc_c)) < 0 ? this_c : vc_c;
			this.clock.set(ind, min_c);
		}
	}
	
	public void updateWithMin(GenericVectorClock<N>... vcList) {
		if(! (vcList.length >= 1) ){
			throw new IllegalArgumentException("Insuffiecient number of arguments provided"); 
		}
		for (int i = 1; i < vcList.length; i++) {
			if (vcList[i].equals(this)) throw new IllegalArgumentException("If \'this\' is one of the arguments, then it must be the first");
		}

		//this.setToZero();
		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {
			GenericVectorClock<N> vc = vcList[i];
			if (! (this.dim == vc.getDim()) ) {
				throw new IllegalArgumentException("Mismatch in maxVC.dim and vc.dim");
			}
			this.updateMin2(vc);
		}
	}
	
	public N getClockIndex(int tIndex){
		return this.clock.get(tIndex);
	}
	
	public void setClockIndex(int tIndex, N tValue){
		this.clock.set(tIndex, tValue);
	}
	
}