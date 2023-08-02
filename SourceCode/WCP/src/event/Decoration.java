package event;

public abstract class Decoration {
	protected int id;
	protected String name;
	
	public int getId() {
		return this.id;
	}

	public String getName() {
		if(this.name == null) {
			return Integer.toString(this.id);
		}
		return this.name;
	}
	
	public String toString() {
		if(this.name == null) {
			return Integer.toString(this.id);
		}
		return getName();
	}

}
