package engine.atomicity.conflictserializability.velodome_basic;

import org.jgrapht.*;
import org.jgrapht.graph.*;
import org.jgrapht.alg.cycle.CycleDetector;

public class JGraphTest {
	
	public static void main(String[] args) {
		Graph<Integer, DefaultEdge> g = new DefaultDirectedGraph<>(DefaultEdge.class);
		CycleDetector<Integer, DefaultEdge> cd_g = new CycleDetector<Integer, DefaultEdge>(g);
		
		g.addVertex(1);
		g.addVertex(2);
		g.addEdge(1, 2);
		System.out.println("Does g have an edge? " + cd_g.detectCycles());
		
		g.addVertex(3);
		g.addEdge(2, 3);
		System.out.println("Does g have an edge? " + cd_g.detectCycles());
		
		g.addEdge(3, 1);
		System.out.println("Does g have an edge? " + cd_g.detectCycles());
	}

}
