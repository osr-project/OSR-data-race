package util.treeclock.word_tricks_map;

import util.vectorclock.Epoch;

public class ClockEpochPair {

	private Epoch acquireClock;
	private TreeClock releaseClock;

	public ClockEpochPair(Epoch acquire) {
		this.acquireClock = new Epoch(acquire);
	}

	public Epoch getAcquire() {
		return this.acquireClock;
	}

	public TreeClock getRelease() {
		return this.releaseClock;
	}

	public void setAcquire(Epoch acquire) {
		this.acquireClock.copyFrom(acquire);
	}

	public void setRelease(TreeClock release) {
		this.releaseClock = new TreeClock(release);
	}

	public String toString() {
		String str = "(" + this.acquireClock.toString() + " , ";
		str = str + ((this.releaseClock != null) ? this.releaseClock.toString() : "null");
		str = str + ")";
		return str;
	}
}