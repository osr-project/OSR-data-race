package util.treeclock.recursive.monitor;

import java.util.HashMap;

import util.Triplet;
import util.tree.EfficientTreeNode;

public class TreeClockTest {
	
	public static TreeClockRecursiveMonitor tc1() {
		HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> threadMap = 
				new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		
		Triplet<Short, Integer, Integer> d1 = 
				new Triplet<Short, Integer, Integer> ((short)1, 16, 0);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n1 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d1);
		threadMap.put((short)1, n1);
		
		Triplet<Short, Integer, Integer> d2 = 
				new Triplet<Short, Integer, Integer> ((short)2, 20, 9);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n2 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d2);
		threadMap.put((short)2, n2);
		
		Triplet<Short, Integer, Integer> d3 = 
				new Triplet<Short, Integer, Integer> ((short)3, 17, 7);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n3 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d3);
		threadMap.put((short)3, n3);
		
		Triplet<Short, Integer, Integer> d4 = 
				new Triplet<Short, Integer, Integer> ((short)4, 23, 18);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n4 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d4);
		threadMap.put((short)4, n4);
		
		Triplet<Short, Integer, Integer> d5 = 
				new Triplet<Short, Integer, Integer> ((short)5, 4, 14);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n5 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d5);
		threadMap.put((short)5, n5);
		
		Triplet<Short, Integer, Integer> d6 = 
				new Triplet<Short, Integer, Integer> ((short)6, 15, 8);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n6 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d6);
		threadMap.put((short)6, n6);
		
		Triplet<Short, Integer, Integer> d7 = 
				new Triplet<Short, Integer, Integer> ((short)7, 11, 2);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n7 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d7);
		threadMap.put((short)7, n7);
		
		Triplet<Short, Integer, Integer> d8 = 
				new Triplet<Short, Integer, Integer> ((short)8, 2, 19);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n8 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d8);
		threadMap.put((short)8, n8);
		
		Triplet<Short, Integer, Integer> d9 = 
				new Triplet<Short, Integer, Integer> ((short)9, 10, 4);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n9 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d9);
		threadMap.put((short)9, n9);
		
		Triplet<Short, Integer, Integer> d10 = 
				new Triplet<Short, Integer, Integer> ((short)10, 2, 15);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n10 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d10);
		threadMap.put((short)10, n10);
		
		Triplet<Short, Integer, Integer> d11 = 
				new Triplet<Short, Integer, Integer> ((short)11, 8, 7);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n11 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d11);
		threadMap.put((short)11, n11);
		
		Triplet<Short, Integer, Integer> d12 = 
				new Triplet<Short, Integer, Integer> ((short)12, 2, 4);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n12 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d12);
		threadMap.put((short)12, n12);
		
		n4.addLatestChild(n8);
		n9.addLatestChild(n12);
		n9.addLatestChild(n11);
		n5.addLatestChild(n9);
		n6.addLatestChild(n10);
		n2.addLatestChild(n7);
		n2.addLatestChild(n6);
		n2.addLatestChild(n5);
		n2.addLatestChild(n4);
		n1.addLatestChild(n3);
		n1.addLatestChild(n2);
		
		TreeClockRecursiveMonitor tc = new TreeClockRecursiveMonitor((short)1);
		tc.root = n1;
		tc.threadMap = threadMap;
		return tc;
	}
	
	public static TreeClockRecursiveMonitor tc2() {
		HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> threadMap = 
				new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		
		Triplet<Short, Integer, Integer> d1 = 
				new Triplet<Short, Integer, Integer> ((short)1, 4, 4);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n1 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d1);
		threadMap.put((short)1, n1);
		
		Triplet<Short, Integer, Integer> d2 = 
				new Triplet<Short, Integer, Integer> ((short)2, 14, 9);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n2 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d2);
		threadMap.put((short)2, n2);
		
		Triplet<Short, Integer, Integer> d3 = 
				new Triplet<Short, Integer, Integer> ((short)3, 10, 4);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n3 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d3);
		threadMap.put((short)3, n3);
		
		Triplet<Short, Integer, Integer> d4 = 
				new Triplet<Short, Integer, Integer> ((short)4, 31, 20);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n4 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d4);
		threadMap.put((short)4, n4);
		
		Triplet<Short, Integer, Integer> d5 = 
				new Triplet<Short, Integer, Integer> ((short)5, 8, 20);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n5 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d5);
		threadMap.put((short)5, n5);
		
		Triplet<Short, Integer, Integer> d6 = 
				new Triplet<Short, Integer, Integer> ((short)6, 15, 8);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n6 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d6);
		threadMap.put((short)6, n6);
		
		Triplet<Short, Integer, Integer> d7 = 
				new Triplet<Short, Integer, Integer> ((short)7, 24, 16);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n7 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d7);
		threadMap.put((short)7, n7);
		
		Triplet<Short, Integer, Integer> d8 = 
				new Triplet<Short, Integer, Integer> ((short)8, 10, 8);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n8 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d8);
		threadMap.put((short)8, n8);
		
		Triplet<Short, Integer, Integer> d9 = 
				new Triplet<Short, Integer, Integer> ((short)9, 16, 5);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n9 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d9);
		threadMap.put((short)9, n9);
		
		Triplet<Short, Integer, Integer> d10 = 
				new Triplet<Short, Integer, Integer> ((short)10, 6, 12);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n10 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d10);
		threadMap.put((short)10, n10);
		
		Triplet<Short, Integer, Integer> d11 = 
				new Triplet<Short, Integer, Integer> ((short)11, 15, 7);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n11 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d11);
		threadMap.put((short)11, n11);
		
		Triplet<Short, Integer, Integer> d12 = 
				new Triplet<Short, Integer, Integer> ((short)12, 25, 0);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n12 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d12);
		threadMap.put((short)12, n12);
		
		n9.addLatestChild(n10);
		n8.addLatestChild(n9);
		n1.addLatestChild(n3);
		n5.addLatestChild(n1);
		n5.addLatestChild(n8);
		n2.addLatestChild(n6);
		n11.addLatestChild(n2);
		n7.addLatestChild(n11);
		n7.addLatestChild(n4);
		n12.addLatestChild(n7);
		n12.addLatestChild(n5);
		
		TreeClockRecursiveMonitor tc = new TreeClockRecursiveMonitor((short)1);
		tc.root = n12;
		tc.threadMap = threadMap;
		return tc;
	}
	
	public static void demo1() {
		TreeClockRecursiveMonitor tc1 = tc1();
		TreeClockRecursiveMonitor tc2 = tc2();
//		System.out.println(tc2.root.toTreeString());
		tc2.join(tc1);
		System.out.println(tc2.root.toTreeString());
//		System.out.println("ThreadMap checks");
		
//		EfficientTreeNode<Triplet<Short, Integer, Integer>> n5 = tc2.threadMap.get((short)5);
//		System.out.println("node for t=5 = " + n5.getHeadChild());
//		EfficientTreeNode<Triplet<Short, Integer, Integer>> tc2_child = tc2.root.getHeadChild().getNext().getNext();
//		while(tc2_child != null) {
//			System.out.println(tc2_child);
//			tc2_child = tc2_child.getHeadChild();
//		}
//		
//		while(tc2_child != null) {
//			System.out.println(tc2_child.toString());
//			tc2_child = tc2_child.getNext();
//		}
//		System.out.println(tc2_child);
//		System.out.println(tc2_child.getHeadChild());
//		System.out.println(tc2_child.getHeadChild().getNext());

	}
	
	public static TreeClockRecursiveMonitor tc3() {
		HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> threadMap = 
				new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		
		Triplet<Short, Integer, Integer> d1 = 
				new Triplet<Short, Integer, Integer> ((short)0, 1, 0);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n1 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d1);
		threadMap.put((short)1, n1);
		
		TreeClockRecursiveMonitor tc = new TreeClockRecursiveMonitor();
		tc.root = n1;
		tc.threadMap = threadMap;
		return tc;
	}
	
	public static TreeClockRecursiveMonitor tc4() {
		HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> threadMap = 
				new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		
		Triplet<Short, Integer, Integer> d1 = 
				new Triplet<Short, Integer, Integer> ((short)0, 0, 0);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n1 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d1);
		threadMap.put((short)1, n1);
		
		TreeClockRecursiveMonitor tc = new TreeClockRecursiveMonitor();
		tc.root = n1;
		tc.threadMap = threadMap;
		return tc;
	}
	
	public static void demo2() {
		TreeClockRecursiveMonitor tc3 = tc3();
		TreeClockRecursiveMonitor tc4 = tc4();
		System.out.println(tc4.toString());
		tc4.join(tc3);
		System.out.println(tc4.toString());

	}
	
	public static void demo3() {
		TreeClockRecursiveMonitor tc1 = tc1();
		TreeClockRecursiveMonitor tc2 = tc2();
		System.out.println(tc1.root.toTreeString());
		tc2.join(tc1);
		tc1.monotoneCopy(tc2);
		System.out.println(tc1.root.toTreeString());
//		System.out.println("ThreadMap checks");
		
//		EfficientTreeNode<Triplet<Short, Integer, Integer>> n5 = tc2.threadMap.get((short)5);
//		System.out.println("node for t=5 = " + n5.getHeadChild());
//		EfficientTreeNode<Triplet<Short, Integer, Integer>> tc2_child = tc2.root.getHeadChild().getNext().getNext();
//		while(tc2_child != null) {
//			System.out.println(tc2_child);
//			tc2_child = tc2_child.getHeadChild();
//		}
//		
//		while(tc2_child != null) {
//			System.out.println(tc2_child.toString());
//			tc2_child = tc2_child.getNext();
//		}
//		System.out.println(tc2_child);
//		System.out.println(tc2_child.getHeadChild());
//		System.out.println(tc2_child.getHeadChild().getNext());

	}
	
	public static TreeClockRecursiveMonitor tc5() {
		HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> threadMap = 
				new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		
		Triplet<Short, Integer, Integer> d1 = 
				new Triplet<Short, Integer, Integer> ((short)1, 2, 0);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n1 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d1);
		threadMap.put((short)1, n1);
		
		TreeClockRecursiveMonitor tc = new TreeClockRecursiveMonitor();
		tc.root = n1;
		tc.threadMap = threadMap;
		return tc;
	}
	
	public static TreeClockRecursiveMonitor tc6() {
		HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> threadMap = 
				new HashMap<Short, EfficientTreeNode<Triplet<Short, Integer, Integer>>> ();
		
		Triplet<Short, Integer, Integer> d1 = 
				new Triplet<Short, Integer, Integer> ((short)3, 4, 0);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n1 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d1);
		threadMap.put((short)3, n1);
		
		Triplet<Short, Integer, Integer> d2 = 
				new Triplet<Short, Integer, Integer> ((short)1, 2, 1);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n2 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d2);
		threadMap.put((short)3, n1);
		
		Triplet<Short, Integer, Integer> d3 = 
				new Triplet<Short, Integer, Integer> ((short)5, 2, 2);
		EfficientTreeNode<Triplet<Short, Integer, Integer>> n3 = 
				new EfficientTreeNode<Triplet<Short, Integer, Integer>> (d3);
		threadMap.put((short)3, n1);
		
		n1.addLatestChild(n2);
		n1.addLatestChild(n3);
		
		TreeClockRecursiveMonitor tc = new TreeClockRecursiveMonitor();
		tc.root = n1;
		tc.threadMap = threadMap;
		return tc;
	}
	
	public static void demo4() {
		TreeClockRecursiveMonitor tc5 = tc5();
		TreeClockRecursiveMonitor tc6 = tc6();
		System.out.println(tc5.toString());
		System.out.println(tc6.toString());
		tc5.monotoneCopy(tc6);
		System.out.println(tc5.toString());

	}
	
	public static void main(String[] args) {
		demo4();
	}
}
