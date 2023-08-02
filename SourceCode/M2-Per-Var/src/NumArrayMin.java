import java.util.Arrays;

public class NumArrayMin {

//	public int[] nums;
	
	class SegmentTreeNode {
		public int start, end;
		public SegmentTreeNode left, right;
		public int min;

		public SegmentTreeNode(int start, int end) {
			this.start = start;
			this.end = end;
			this.left = null;
			this.right = null;
			this.min = Integer.MAX_VALUE;
		}

		public SegmentTreeNode(SegmentTreeNode copy) {
			this.start = copy.start;
			this.end = copy.end;
			this.min = copy.min;
		}
	}

	public SegmentTreeNode root = null;

	public NumArrayMin(int[] nums) {
		this.root = buildTree(nums, 0, nums.length-1);
	}

	public NumArrayMin(NumArrayMin other) {
		this.root = copyTree(other.root);
	}

	//Return the value at i
	public int get(int i) {
		return this.sumRange(root, i, i);
	}

	public SegmentTreeNode copyTree(SegmentTreeNode curNode){
		if(curNode == null) return null;

		SegmentTreeNode curCopy = new SegmentTreeNode(curNode);
		curCopy.left = copyTree(curNode.left);
		curCopy.right = copyTree(curNode.right);

		return curCopy;
	}

	private SegmentTreeNode buildTree(int[] nums, int start, int end) {
		if (start > end) {
			return null;
		} else {
			SegmentTreeNode ret = new SegmentTreeNode(start, end);
			if (start == end) {
				ret.min = nums[start];
			} else {
				int mid = start  + (end - start) / 2;             
				ret.left = buildTree(nums, start, mid);
				ret.right = buildTree(nums, mid + 1, end);
				ret.min = ret.left.min <  ret.right.min ? ret.left.min : ret.right.min;
			}         
			return ret;
		}
	}

	void update(int i, int val) {
		update(root, i, val);
	}

	void update(SegmentTreeNode root, int pos, int val) {
		if (root.start == root.end) {
			root.min = val;
		} else {
			int mid = root.start + (root.end - root.start) / 2;
			if (pos <= mid) {
				update(root.left, pos, val);
			} else {
				update(root.right, pos, val);
			}
			root.min = root.left.min < root.right.min? root.left.min : root.right.min;
		}
	}

	public int argMin(int x) {
		if(this.root.min <= x) {
			return this.argMin(this.root, x)-1;
		}
		else {
			return -1;
		}
	}

	public int argMin(SegmentTreeNode root, int x) {
		if(root.left == root.right) {
			return 1;
		}
		if(root.right.min <= x) {
			return this.argMin(root.right, x) + root.left.end - root.left.start + 1;
		}
		else {
			return this.argMin(root.left, x);
		}
	}



	public int sumRange(int i, int j) {
		return sumRange(root, i, j);
	}

	public int sumRange(SegmentTreeNode root, int start, int end) {
		if (root.end == end && root.start == start) {
			return root.min;
		} else {
			int mid = root.start + (root.end - root.start) / 2;
			if (end <= mid) {
				return sumRange(root.left, start, end);
			} else if (start >= mid+1) {
				return sumRange(root.right, start, end);
			}  else {
				int l = sumRange(root.left, start, mid);
				int r = sumRange(root.right, mid+1, end);
				return  l < r ? l : r ;
			}
		}
	}
	
	
//	public String numArray2Str() {
//		String s="";
//		for(int i : nums) {
//			s += i == Integer.MAX_VALUE ? "-" : i;
//			s += ", ";
//		}
//		return s;
//	}
}