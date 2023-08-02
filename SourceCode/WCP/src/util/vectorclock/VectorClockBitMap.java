package util.vectorclock;

import java.util.Vector;

public class VectorClockBitMap {

	private int dim;
	private Vector<Integer> clock;
	private Vector<Boolean> bitmap;

	public VectorClockBitMap(int d) {
		this.dim = d;
		this.clock = new Vector<Integer>(dim);
		this.bitmap = new Vector<Boolean>(dim);
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.addElement(0);
			this.bitmap.addElement(false);
		}
	}

	public VectorClockBitMap(VectorClockBitMap fromVectorClock) {
		this.dim = fromVectorClock.getDim();
		this.clock = new Vector<Integer>(dim);
		this.bitmap = new Vector<Boolean>(dim);
		Vector<Integer> fromClock = fromVectorClock.getClock();
		Vector<Boolean> fromBitMap = fromVectorClock.getBitMap();
		for (int ind = 0; ind < fromVectorClock.getDim(); ind++) {
			this.clock.addElement((Integer) fromClock.get(ind));
			this.bitmap.addElement((Boolean) fromBitMap.get(ind));
		}
	}

	public int getDim() {
		if(!(this.dim == this.clock.size())){
			throw new IllegalArgumentException("Mismatch in dim and clock size"); 
		}
		return this.dim;
	}

	public Vector<Integer> getClock() {
		return this.clock;
	}
	
	public Vector<Boolean> getBitMap() {
		return this.bitmap;
	}

	public String toString() {
		return String.format("%s | %s", this.clock.toString(), this.bitmap.toString());
	}

	/*
	public boolean isClockZero() {
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
	*/
	
	public boolean isBitMapZero() {
		boolean itIsZero = true;
		for (int ind = 0; ind < this.dim; ind++) {
			boolean thisVal = this.bitmap.get(ind);
			if (thisVal) {
				itIsZero = false;
				break;
			}
		}
		return itIsZero;
	}

	public boolean isEqualClock(VectorClockBitMap vcb) {
		if(! (this.dim == vcb.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		boolean itIsEqual = true;
		Vector<Integer> vcbClock = vcb.getClock();
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind).intValue();
			int clockVal = vcbClock.get(ind).intValue();
			if (thisVal != clockVal) {
				itIsEqual = false;
				break;
			}
		}
		return itIsEqual;
	}
	
	public boolean isEqualBitMap(VectorClockBitMap vcb) {
		if(! (this.dim == vcb.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		boolean itIsEqual = true;
		Vector<Boolean> vcbBitMap = vcb.getBitMap();
		for (int ind = 0; ind < this.dim; ind++) {
			Boolean thisVal = this.bitmap.get(ind);
			Boolean bitVal = vcbBitMap.get(ind);
			if (thisVal != bitVal) {
				itIsEqual = false;
				break;
			}
		}
		return itIsEqual;
	}
	
	public boolean isEqual(VectorClockBitMap vcb) {
		return this.isEqualClock(vcb);
	}

	public boolean isLessThanOrEqual(VectorClockBitMap vcb) {
		if(! (this.dim == vcb.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		boolean itIsLessThanOrEqual = true;
		Vector<Integer> vcClock = vcb.getClock();
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
	
	public boolean isLessThanOrEqual(VectorClockBitMap vcb, int ind) {
		return this.clock.get(ind).intValue() <= vcb.getClock().get(ind).intValue();
	}

	/*
	public void setToZero() {
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.set(ind, (Integer) 0 );
		}
	}
	*/
	
	public void copyFrom(VectorClockBitMap vcb) {
		if(! (this.dim == vcb.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		for (int ind = 0; ind < this.dim; ind++) {
			this.clock.set(ind, (Integer) vcb.clock.get(ind));
			this.bitmap.set(ind, (Boolean) vcb.bitmap.get(ind));
		}
	}	
	
	private void updateMax2(VectorClockBitMap vcb) {
		if(! (this.dim == vcb.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		for (int ind = 0; ind < this.dim; ind++) {
			int this_c = this.clock.get(ind);
			int vc_c = vcb.clock.get(ind);
			int max_c = this_c > vc_c ? this_c : vc_c;
			this.clock.set(ind, (Integer) max_c);
			Boolean this_b = this.bitmap.get(ind);
			Boolean vc_b = vcb.bitmap.get(ind);
			Boolean bit_c = this_c > vc_c ? this_b : vc_b;
			this.bitmap.set(ind, bit_c);
		}
	}
	
	/*
	//The following function update this as : this := \lambda t . if t == tIndex then this[tIndex] else max(this[t], vc[t])
	public void updateMax2WithoutLocal(VectorClockBitMap vc, int tIndex) {
		if(! (this.dim == vc.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		for (int ind = 0; ind < this.dim; ind++) {
			if(ind != tIndex){
				int this_c = this.clock.get(ind);
				int vc_c = vc.clock.get(ind);
				int max_c = this_c > vc_c ? this_c : vc_c;
				this.clock.set(ind, (Integer) max_c);
			}
		}
	}
	*/
	
	public void updateWithMax(VectorClockBitMap... vcbList) {
		if(! (vcbList.length >= 1) ){
			throw new IllegalArgumentException("Insuffiecient number of arguments provided"); 
		}
		for (int i = 1; i < vcbList.length; i++) {
			if (vcbList[i].equals(this)) throw new IllegalArgumentException("If \'this\' is one of the arguments, then it must be the first");
		}

		//this.setToZero();
		this.copyFrom(vcbList[0]);
		for (int i = 1; i < vcbList.length; i++) {
			VectorClockBitMap vc = vcbList[i];
			if (! (this.dim == vc.getDim()) ) {
				throw new IllegalArgumentException("Mismatch in maxVC.dim and vc.dim");
			}
			this.updateMax2(vc);
		}
	}
	
	/*
	private void updateMin2(VectorClockBitMap vc) {
		if(! (this.dim == vc.getDim()) ){
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim"); 
		}
		for (int ind = 0; ind < this.dim; ind++) {
			int this_c = this.clock.get(ind);
			int vc_c = vc.clock.get(ind);
			int max_c = this_c < vc_c ? this_c : vc_c;
			this.clock.set(ind, (Integer) max_c);
		}
	}
	
	
	public void updateWithMin(VectorClockBitMap... vcList) {
		if(! (vcList.length >= 1) ){
			throw new IllegalArgumentException("Insuffiecient number of arguments provided"); 
		}
		for (int i = 1; i < vcList.length; i++) {
			if (vcList[i].equals(this)) throw new IllegalArgumentException("If \'this\' is one of the arguments, then it must be the first");
		}

		//this.setToZero();
		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {
			VectorClockBitMap vc = vcList[i];
			if (! (this.dim == vc.getDim()) ) {
				throw new IllegalArgumentException("Mismatch in maxVC.dim and vc.dim");
			}
			this.updateMin2(vc);
		}
	}
	*/
	
	public int getClockIndex(int tIndex){
		return this.clock.get(tIndex);
	}
	
	public void setClockIndex(int tIndex, int tValue){
		this.clock.set(tIndex, (Integer) tValue);
	}
	
	public Boolean getBitIndex(int tIndex){
		return this.bitmap.get(tIndex);
	}
	
	public void setBitIndex(int tIndex, Boolean tValue){
		this.bitmap.set(tIndex, tValue);
	}
	
}