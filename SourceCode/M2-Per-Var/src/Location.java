import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;


public class Location {

	public int location;							//The location represented
	public EventKind kind;						//The kind of events accessing the location
	public Set<EventType> operation;				//The set of all types of operations performed in this location
	public int numWrites;						//The number of times the location is written (if kind=EventKind.Write_READ)
	public Map<Integer, Integer> counts;			//The number of times each thread accesses the location
	public List<Stack<Integer>> events;	//The events accessing the location


	public Location(int location, int numThreads, boolean trackEvents) {
		this.location = location;
		this.operation = new HashSet<EventType>();
		this.numWrites = 0;
		this.counts = new HashMap<Integer, Integer>();
		this.kind = EventKind.OTHER;
		if(trackEvents) {
			this.events = new ArrayList<Stack<Integer>>();
			for(int i=0; i<numThreads; i++) {
				this.events.add(new Stack<Integer>());
			}
		}
	}

	public void addOperation(Event ev, Pair<Integer, Integer> u, boolean trackEvents) {
		this.operation.add(ev.type);

		if(ev.type == EventType.WRITE) {
			this.numWrites++;
		}

		if(ev.kind == EventKind.WRITE_READ) {
			if(this.kind != EventKind.OTHER && this.kind != EventKind.WRITE_READ) {
				System.out.println("Error");
			}
			this.kind = EventKind.WRITE_READ;
		}
		if(ev.kind == EventKind.ACQUIRE_RELEASE) {
			if(this.kind != EventKind.OTHER && this.kind != EventKind.ACQUIRE_RELEASE) {
				System.out.println("Error");
			}
			this.kind = EventKind.ACQUIRE_RELEASE;
		}

		if(!this.counts.containsKey(u.x)) {
			this.counts.put(u.x, 0);
		}

		this.counts.put(u.x, this.counts.get(u.x) + 1);
		if(trackEvents) {
			this.events.get(u.x).push(u.y);
		}
	}


	public void popEvent(int tid) {
		this.events.get(tid).pop();
		if(this.counts.get(tid) == 1) {
			this.counts.remove(tid);
		}
		else {
			this.counts.put(tid, this.counts.get(tid)-1);
		}

	}

	public Set<Pair<Integer, Integer>> popAllEvents() {
		Set<Pair<Integer, Integer>> dropped = new HashSet<Pair<Integer, Integer>>();

		for(int i=0; i<this.events.size(); i++) {
			while(this.events.get(i).size() > 0) {
				dropped.add(new Pair<Integer, Integer>(i,this.events.get(i).pop()));
			}
			this.counts.remove(i);
		}
		return dropped;
	}

	public int getTotalEvents() {
		int t = 0;
		for(int i : this.counts.keySet()) {
			t += this.counts.get(i);
		}
		return t;
	}


}
