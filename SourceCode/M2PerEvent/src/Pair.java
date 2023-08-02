
public class Pair<X, Y> { 
	  public X x; 
	  public Y y; 
	  public Pair(X x, Y y) { 
	    this.x = x; 
	    this.y = y; 
	  }
	  
	  public X getX(){
	  		return this.x;
	  }
	  
	  public Y getY() {
	  		return this.y;
	  }
	  
	  public boolean equals(Object o) {
		  Pair<Integer, Integer> other = (Pair<Integer, Integer>) o;
		  return this.x.equals(other.x) && this.y.equals(other.y);  
	  }
	  
	  public String toString() {
		  return "(" + this.x.toString() + "," + this.y.toString() + ")";
	  }
	  
	  public int hashCode() {
		  return y.hashCode()*100 + x.hashCode();
	  }
}
