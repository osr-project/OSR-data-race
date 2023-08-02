package engine.deadlock.goodlock2;

import engine.deadlock.goodlock2_rw.Goodlock2State;

public class Goodlock2Event extends engine.deadlock.goodlock2_rw.Goodlock2Event{
	@Override
	public boolean HandleSubRead(Goodlock2State state) {
		return false;
	}

	@Override
	public boolean HandleSubWrite(Goodlock2State state) {
		return false;
	}
}
