package inferray.test;

import junit.framework.TestCase;

import org.junit.Test;

import stixar.graph.BasicNode;
import stixar.graph.Digraph;
import stixar.graph.Node;
import stixar.graph.attr.NodeMatrix;
import stixar.graph.conn.ConnectedComponents;
import stixar.graph.conn.Transitivity;
import stixar.graph.gen.BasicDGFactory;

/**
 * Real world case for inferray
 * 
 * 
 * Adds a forest, separate using spanning trees, compute closures
 * 
 * @author Julien
 * 
 */
public class ForestTest extends TestCase {
	final static int SIZE = 11;

	@Test
	public void test() {
		final long[] values = new long[SIZE];
		final BasicNode[] nodes = new BasicNode[SIZE];
		final BasicDGFactory factory = new BasicDGFactory();

		for (int i = 0; i < SIZE; ++i) {
			nodes[i] = factory.node();
			nodes[i].setLong(values, i);
		}

		// First forest
		factory.edge(nodes[1], nodes[0]);
		factory.edge(nodes[2], nodes[1]);
		factory.edge(nodes[3], nodes[2]);
		// Second forest
		factory.edge(nodes[4], nodes[5]);
		factory.edge(nodes[5], nodes[6]);
		// Third forest
		factory.edge(nodes[7], nodes[8]);
		factory.edge(nodes[8], nodes[9]);
		factory.edge(nodes[9], nodes[10]);

		final Digraph digraph = factory.digraph();
		final NodeMatrix<Boolean> matrix = Transitivity.compactClosure(digraph);
		assertNotNull(matrix);
		// Compute spanning trees
		final ConnectedComponents cc = new ConnectedComponents(digraph);
		cc.run();
		final Node[] components = cc.components();
		assertNotNull(components);
		assertEquals(11, components.length);

	}
}
