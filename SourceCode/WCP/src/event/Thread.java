package event;

public class Thread extends Decoration {

	public static int threadCountTracker = 0;

	public Thread() {
		this.id = threadCountTracker;
		threadCountTracker++;
		this.name = "__thread::" + Integer.toString(this.id) + "__";
	}

	public Thread(String sname) {
		this.id = threadCountTracker;
		threadCountTracker++;
		this.name = sname;
	}
	
	public Thread(int tid) {
		this.id = tid;
	}

	public boolean equals(Thread other){
		return this.id == other.id;
	}

	public int hashCode(){
		return id;
	}
}
