
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PrefixMaxima {
	
	boolean modified;
	NumArrayMax numArray;
	public int length;
	
	public PrefixMaxima(int[] nums) {
		//this.nums = new int[nums.length];
		//System.arraycopy(nums, 0, this.nums, 0, nums.length);
		this.numArray = new NumArrayMax(nums);
		this.modified = false;
	}
	
	
	
	public PrefixMaxima(PrefixMaxima other) {
		//this.nums = new int[other.nums.length];
		//System.arraycopy(other.nums, 0, this.nums, 0, other.nums.length);
		//this.numArray = new NumArrayMax(this.nums);
		this.length = other.length;
		this.numArray = new NumArrayMax(other.numArray);
		this.modified = false;
	}
	
	public int getArgMax(int x) {
		//System.out.println("getArgMax from PrefixMaxima of numArray that has size " + nums.length + "\t" + Arrays.toString(nums));
		return this.numArray.argMax(x);
	}

	public int getMax(int i) {
		return this.numArray.sumRange(0, i);
	}
	
	public boolean update(int i, int val) {
		this.modified = true;
		this.numArray.update(i, val);
		return true;
	}
	
}
