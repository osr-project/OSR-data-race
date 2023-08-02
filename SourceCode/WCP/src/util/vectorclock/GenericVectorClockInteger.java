package util.vectorclock;

public class GenericVectorClockInteger extends GenericVectorClock<Integer> {

	public GenericVectorClockInteger(int d) {
		super(d, 0);
	}
	
	public GenericVectorClockInteger(GenericVectorClockInteger vc) {
		super(vc);
	}
	
	public void setToZero() {
		setToSentinel(0);
	}
}