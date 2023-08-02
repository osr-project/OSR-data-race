package engine.deadlock;

public abstract class State {
	
	//parameter flags
	public int verbosity;

	public abstract void printMemory();
}
