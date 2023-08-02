import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EventMap {
	
	List<Integer> map;
	
	public EventMap() {
		this.map = new ArrayList<Integer>();
	}
	
	public void addEvent(int i) {
		this.map.add(i);
	}
	
	public int getNext(int k) {
		
		if(this.map.get(this.map.size()-1)<=k) {
			return -1;
		}
		
		int left=0, right=map.size()-1;
		//System.out.println("Map: " + Arrays.toString(map.toArray()));
		//System.out.println("Next Input: " + k);
		
		while(right>left) {
			int mid = (right+left)/2;
			if(this.map.get(mid) == k) {
				left = mid+1;
				right = mid+1;
			}
			else if(this.map.get(mid)<k) {
				left=mid+1;
				
			}
			else {
				right=mid;
			}
		}
		//System.out.println("Map: " + Arrays.toString(map.toArray()));
		//System.out.println("Next: on input " + k + " returning " + right);
		return this.map.get(right);
	}
	
	public int getPrevious(int k) {
		
		if(this.map.get(0)>=k) {
			return -1;
		}
		
		int left=0, right=map.size()-1;
		
		//System.out.println("Map: " + Arrays.toString(map.toArray()));
		//System.out.println("Previous Input: " + k);
		
		while(right>left) {
			int mid = (int) Math.ceil((right+left)/2.0);
			if(this.map.get(mid) == k) {
				left = mid-1;
				right = mid-1;
			}
			else if(this.map.get(mid)>k) {
				right=mid-1;
			}
			else {
				left = mid;
			}
		}
		//System.out.println("Map: " + Arrays.toString(map.toArray()));
		//System.out.println("Previous: on input " + k + " returning " + left);
		return this.map.get(left);
	}

}
