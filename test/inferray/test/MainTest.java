package inferray.test;

import junit.framework.TestCase;

import org.junit.Test;

import stixar.graph.BasicNode;
import stixar.graph.Digraph;
import stixar.graph.attr.NodeMatrix;
import stixar.graph.check.ForestChecker;
import stixar.graph.conn.Transitivity;
import stixar.graph.gen.BasicDGFactory;

public class MainTest extends TestCase {
	// Example value whose square is over Integer.MAX_VALUE
	final static int SIZE = 48000;

	@Test
	public void test() {

		final BasicNode[] nodes = new BasicNode[SIZE];
		final BasicDGFactory factory = new BasicDGFactory();
		for (int i = 0; i < SIZE; ++i) {
			nodes[i] = factory.node();
		}
		for (int i = 0; i < SIZE - 1; ++i) {
			factory.edge(nodes[i], nodes[i + 1]);
		}

		final Digraph digraph = factory.digraph();
		final long t1 = System.currentTimeMillis();
		final NodeMatrix<Boolean> reachMat = Transitivity
				.compactClosure(digraph);
		assertNotNull(reachMat);
		final long t2 = System.currentTimeMillis();

		System.out.println(SIZE + " Nodes closure in " + (t2 - t1) + " ms");
		final ForestChecker fcheker = new ForestChecker();
		assertTrue(fcheker.check(digraph));
	}
}
