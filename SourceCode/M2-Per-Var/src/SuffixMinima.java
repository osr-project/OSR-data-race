

public class SuffixMinima {
	
	boolean modified = false;
	NumArrayMin numArray;
	int length;
	public SuffixMinima(int[] nums) {

		length = nums.length;
		this.numArray = new NumArrayMin(nums);
		this.modified = false;

	}
	
	public SuffixMinima(SuffixMinima other) {

		this.length = other.length;
		this.numArray = new NumArrayMin(other.numArray);
		this.modified = false;
		
	}
	
	//Return the largest i such that getMin(i) <= x
	//This gives the last predecessor of the x-th node of the current thread from the other thread
	public int getArgMin(int x) {
		return this.numArray.argMin(x);
	}
	
	//Return the minimum value found at the suffix starting in i
	//This gives the the first successor of the i-th node of the current thread to the other thread
	public int getMin(int i) {
		return this.numArray.sumRange(i, this.length-1);
	}
	
	//Return the value stored at position i
	public int get(int i) {
			return this.numArray.get(i);
	}
	
	// Insert an edge from node i to node val
	public boolean update(int i, int val) {
		//this.numArray.nums[i] = val;
		this.modified = true;
		this.numArray.update(i, val);
		return true;
	}
	
	
}
