package util.vectorclock;

import java.util.Vector;

public class FlexibleVC extends GenericVectorClock<Long> {
	
	public FlexibleVC(int d) {
		super(d, 0L);
	}
	
	public FlexibleVC(FlexibleVC fromVectorClock) {
		super(fromVectorClock);
	}
	
	public void expandDim(int d) {
		if(d <= this.dim){
			throw new IllegalArgumentException("Cannot shrink size from " + this.dim + " to " + d); 
		}
		for (int ind = this.dim; ind < d; ind++) {
			this.clock.addElement(0L);
		}
		this.dim = d;
	}

	@Override
	public boolean isEqual(GenericVectorClock<Long> vc) {
		boolean itIsEqual = true;
		Vector<Long> vcClock = vc.getClock();
		int ind = 0;
		for (; ind < this.dim && ind < vc.dim; ind++) {
			if (this.clock.get(ind).longValue() != vcClock.get(ind).longValue()) {
				itIsEqual = false;
				break;
			}
		}
		if(this.dim != vc.dim) {
			Vector<Long> nonZeroClock = (this.dim > vc.dim) ? this.clock : vc.getClock();
			int maxDim = (this.dim > vc.dim) ? this.dim : vc.dim;
			for (; ind < maxDim; ind++) {
				if (nonZeroClock.get(ind).longValue() != 0) {
					itIsEqual = false;
					break;
				}
			}
		}
		return itIsEqual;
	}

	@Override
	public boolean isLessThanOrEqual(GenericVectorClock<Long> vc) {
		boolean itIsLessThanOrEqual = true;
		Vector<Long> vcClock = vc.getClock();
		int ind = 0;
		for ( ; ind < this.dim && ind < vc.dim; ind++) {
			if (!(this.clock.get(ind).longValue() <= vcClock.get(ind).longValue())) {
				itIsLessThanOrEqual = false;
				break;
			}
		}
		if(this.dim > vc.dim) {
			for (; ind < this.dim; ind++) {
				if (this.clock.get(ind).longValue() > 0) {
					itIsLessThanOrEqual = false;
					break;
				}
			}
		}
		return itIsLessThanOrEqual;
	}
	
	@Override
	public void copyFrom(GenericVectorClock<Long> vc) {
		this.dim = vc.dim;
		this.clock = new Vector<Long> (vc.clock);
	}	
	
	private void updateMax2(GenericVectorClock<Long> vc) {
		int ind = 0;
		for (; ind < this.dim && ind < vc.dim; ind++) {
			long this_c = this.clock.get(ind);
			long vc_c = vc.clock.get(ind);
			long max_c = this_c > vc_c ? this_c : vc_c;
			this.clock.set(ind, (Long) max_c);
		}
		if(this.dim < vc.dim) {
			for( ; ind < vc.dim; ind ++) {
				this.clock.addElement(vc.clock.get(ind));
			}
		}
		this.dim = this.clock.size();
	}
	
	//The following function update this as : this := \lambda t . if t == tIndex then this[tIndex] else max(this[t], vc[t])
	@Override
	public void updateMax2WithoutLocal(GenericVectorClock<Long> vc, int tIndex) {
		/*
		if(tIndex >= this.dim && tIndex >= vc.dim){
			throw new IllegalArgumentException("Index out of bounds in updateMax2WithoutLocal : this.dim = " + dim + ", vc.dim = " + vc.dim + ", tIndex = " + tIndex); 
		}
		*/
		long tIndexOrigVal = (tIndex < this.dim) ? this.clock.get(tIndex) : 0;
		this.updateMax2(vc);
		this.clock.set(tIndex, tIndexOrigVal);
	}
	
	@Override
	@SafeVarargs
	public final void updateWithMax(GenericVectorClock<Long>... vcList) {
		if(! (vcList.length >= 1) ){
			throw new IllegalArgumentException("Insuffiecient number of arguments provided"); 
		}
		for (int i = 1; i < vcList.length; i++) {
			if (vcList[i].equals(this)) throw new IllegalArgumentException("If \'this\' is one of the arguments, then it must be the first");
		}
		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {			
			this.updateMax2(vcList[i]);
		}
	}
	
	private void updateMin2(GenericVectorClock<Long> vc) {
		int ind = 0;
		for (; ind < this.dim && ind < vc.dim; ind++) {
			long this_c = this.clock.get(ind);
			long vc_c = vc.clock.get(ind);
			long min_c = this_c < vc_c ? this_c : vc_c;
			this.clock.set(ind, (Long) min_c);
		}
		if(this.dim > vc.dim) {
			for (; ind < this.dim ; ind++) {
				this.clock.set(ind, 0L);
			}
		}
	}
	
	@Override
	@SafeVarargs
	public final void updateWithMin(GenericVectorClock<Long>... vcList) {
		if(! (vcList.length >= 1) ){
			throw new IllegalArgumentException("Insuffiecient number of arguments provided"); 
		}
		for (int i = 1; i < vcList.length; i++) {
			if (vcList[i].equals(this)) throw new IllegalArgumentException("If \'this\' is one of the arguments, then it must be the first");
		}

		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {
			this.updateMin2(vcList[i]);
		}
	}
	
	@Override
	public Long getClockIndex(int tIndex){
		return (tIndex < this.dim) ? this.clock.get(tIndex) : 0L;
	}
	
	@Override
	public void setClockIndex(int tIndex, Long tValue){
		if(tIndex >= this.dim) {
			this.expandDim(tIndex + 1);
		}
		this.clock.set(tIndex, (Long) tValue);
	}
	
}