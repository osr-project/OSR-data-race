package util.trace;

import java.util.HashSet;
import java.util.Stack;

public class Test {

	public Test() {
		// TODO Auto-generated constructor stub
	}
	
	public static <T> HashSet<T> getSetFromStack(Stack<T> stack){
		HashSet<T> set = new HashSet<T>();
		for(T t : stack){
			set.add(t);
		}
		return set;
	}

	public static void main(String[] args) {
		Stack<Integer> st = new Stack<Integer>();
		st.push(0);
		st.push(-1);
		st.push(2);
		st.push(100);
		st.push(-120);
		
		HashSet<Integer> set = getSetFromStack(st);
		System.out.println(st.toString());
		System.out.println(set.toString());

	}

}
