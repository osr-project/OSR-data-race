

public class NumArrayMax {

    class SegmentTreeNode {
        int start, end;
        SegmentTreeNode left, right;
        int max;

        public SegmentTreeNode(int start, int end) {
            this.start = start;
            this.end = end;
            this.left = null;
            this.right = null;
            this.max = Integer.MIN_VALUE;
        }
    }
      
    SegmentTreeNode root = null;
    public NumArrayMax(int[] nums) {
        root = buildTree(nums, 0, nums.length-1);
    }
    
    public NumArrayMax(NumArrayMax other) {
    		root = buildTree(other, other.root);
    }
    
    private SegmentTreeNode buildTree(NumArrayMax other, SegmentTreeNode otherCurrent) {
    		if(otherCurrent == null) {
    			return null;
    		}
    		else {
    			SegmentTreeNode thisCurrent = new SegmentTreeNode(otherCurrent.start, otherCurrent.end);
    			thisCurrent.max = otherCurrent.max;
    			
    			if(otherCurrent.left != null) {
    				thisCurrent.left = this.buildTree(other, otherCurrent.left);
    			}
    			
    			if(otherCurrent.right != null) {
    				thisCurrent.right = this.buildTree(other, otherCurrent.right);
    			}
    			
    			return thisCurrent;
    		}
    }

    private SegmentTreeNode buildTree(int[] nums, int start, int end) {
        if (start > end) {
            return null;
        } else {
            SegmentTreeNode ret = new SegmentTreeNode(start, end);
            if (start == end) {
                ret.max = nums[start];
            } else {
                int mid = start  + (end - start) / 2;             
                ret.left = buildTree(nums, start, mid);
                ret.right = buildTree(nums, mid + 1, end);
                ret.max = ret.left.max >  ret.right.max ? ret.left.max : ret.right.max;
            }         
            return ret;
        }
    }
   
    void update(int i, int val) {
        update(root, i, val);
    }
   
    void update(SegmentTreeNode root, int pos, int val) {
        if (root.start == root.end) {
           root.max = val;
        } else {
            int mid = root.start + (root.end - root.start) / 2;
            if (pos <= mid) {
                 update(root.left, pos, val);
            } else {
                 update(root.right, pos, val);
            }
            root.max = root.left.max > root.right.max? root.left.max : root.right.max;
        }
    }

    
    public int argMax(int x) {
		if(this.root.max >= x) {
			return this.argMax(this.root, x)-1;
		}
		else {
			return -1;
		}
    }

    public int argMax(SegmentTreeNode root, int x) {
		if(root.start == root.end) {
			return 1;
		}
		if(root.left.max >= x) {
			return this.argMax(root.left, x);
		}
		else {
			return this.argMax(root.right, x) + root.left.end - root.left.start + 1;
		}
    }
    
    public int sumRange(int i, int j) {
        return sumRange(root, i, j);
    }
    
    public int sumRange(SegmentTreeNode root, int start, int end) {
        if (root.end == end && root.start == start) {
            return root.max;
        } else {
            int mid = root.start + (root.end - root.start) / 2;
            if (end <= mid) {
                return sumRange(root.left, start, end);
            } else if (start >= mid+1) {
                return sumRange(root.right, start, end);
            }  else {
            		int l = sumRange(root.left, start, mid);
            		int r = sumRange(root.right, mid+1, end);
                return  l > r ? l : r ;
            }
        }
    }
}