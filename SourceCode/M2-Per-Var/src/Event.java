

public class Event {
	
	EventType type;
	EventKind kind;
	int location, inputTraceIndex, codeLine;
	
	
	
	public Event(EventType type, EventKind kind, int location, int inputTraceIndex, int codeLine) {
		this.type = type;
		this.kind = kind;
		this.location = location;
		this.inputTraceIndex = inputTraceIndex;
		this.codeLine = codeLine;
	}
	
	public boolean conflictsWith(Event e) {
		return this.location != e.location ? false :  this.type == EventType.WRITE || e.type == EventType.WRITE;
	}
	
	public boolean lockConflictsWith(Event e) {
		return this.location != e.location ? false :this.type == EventType.RELEASE || e.type == EventType.RELEASE;
	}
	
	public boolean equals(Object o) {
		Event other = (Event) o;
		return this.inputTraceIndex == other.inputTraceIndex;
	}
		
	public String toString() {
		return this.type.name() + " " + this.location + " " + this.inputTraceIndex + " " + this.codeLine;
	}

}

