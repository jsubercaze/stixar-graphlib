package stixar.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;

import stixar.graph.order.NodeOrder;
import stixar.util.CList;

/**
   An implementation of an editable Digraph with
   {@link BasicNode}s and {@link BasicEdge}s.
 */
public class BasicDigraph extends FilterGraph
implements MutableDigraph
{
	protected BasicNode[] nodes;
	protected int nodeCount;
	protected int edgeCount;
	// fail fast node and edge iterators.
	protected int nMods;
	protected int eMods;


	/**
       Construct an attribute-identical copy of an arbitrary digraph.
	 */
	public static BasicDigraph copy(final Digraph dg)
	{
		final int nasz = dg.nodeAttrSize();
		final BasicDigraph res = new BasicDigraph(nasz, dg.edgeAttrSize());

		for (int i=0; i<nasz; ++i) {
			final Node n = dg.node(i);
			if (n == null) {
				continue;
			}
			res.nodes[i] = new BasicNode(res, i);
			res.nSlots.set(i, true);
			res.nodeCount++;
		}

		for (final Node n : dg.nodes()) {
			final BasicNode bn = res.nodes[n.nodeId()];
			for (Edge e = n.out(); e != null; e = e.next()) {
				final Node t = e.target();
				final int eid = e.edgeId();
				final BasicEdge be = new BasicEdge(res, eid, bn, res.nodes[t.nodeId()]);
				res.eSlots.set(eid, true);
				res.edgeCount++;
				bn.add(be);
			}
		}
		return res;
	}

	/*
      Construct a new BasicDigraph with nodes specified.

      performed by the BasicDGFactory.
	 */
	public BasicDigraph(final BasicNode[] nodes)
	{
		super(nodes.length, 0);
		this.nodes = nodes;
		this.nodeTop = nodes.length;
		this.edgeTop = 0;
		this.nodeCount = 0;
		this.edgeCount = 0;
		this.nMods = 0;
		this.eMods = 0;
		for (int i=0; i<nodes.length; ++i) {
			if (nodes[i] != null) {
				nodeCount++;
				nSlots.set(i, true);
				for (Edge e = nodes[i].out(); e != null; e = e.next()) {
					eSlots.set(e.edgeId(), true);
					edgeTop = Math.max(edgeTop, e.edgeId());
					edgeCap = Math.max(edgeCap, edgeTop);
					edgeCount++;
				}
			}
		}
	}

	public BasicDigraph(final int n, final int m)
	{
		super(n, m);
		this.nodes = new BasicNode[n];
	}

	public BasicDigraph()
	{
		super(10,25);
		this.nodes = new BasicNode[10];
	}


	/*
      javadoc'd in digraph, more or less.
	 */
	@Override
	public BasicNode node(final int id)
	{
		return nodes[id];
	}

	@Override
	public BasicNode genNode()
	{
		if (nodeCap <= nodeTop + 1) {
			growNodes(nodeCap * 2);
		}
		final BasicNode n = new BasicNode(this, nodeTop);
		nodes[nodeTop] = n;
		nSlots.set(nodeTop++, true);
		nodeCount++;
		nMods++;
		return n;
	}

	@Override
	public void remove(final Node n)
	{
		if (!(n instanceof BasicNode)) {
			throw new IllegalArgumentException();
		}
		final BasicNode bn = (BasicNode) n;
		final CList<BasicEdge> eList = new CList<BasicEdge>();
		/*
          Because of possible self loops, we need to make sure
          that self loop edges are removed from both in and out
          lists, but that the non-topologic removal only occurs
          once.

          To do this, we mark each edge by setting its id to
          -1 (id + 1).  This way, we collect in eList each
          edge only once by testing for a positive id.  With
          a unique list, we can then apply topologic and non-topologic
          removal ops uniformly.
		 */
		for (BasicEdge e = bn.out(); e != null; e = e.next()) {
			e.edgeId(-(e.edgeId() + 1));
			eList.add(e);
		}
		for (BasicEdge e = bn.in(); e != null; e = e.nextIn()) {
			if (e.edgeId() >= 0) {
				e.edgeId(-(e.edgeId() + 1));
				eList.add(e);
			}
		}
		for (final BasicEdge e : eList) {
			e.edgeId(-1 * e.edgeId() - 1);
			final BasicNode u = e.source();
			u.remove(e);
			_remove(e);
		}
		nSlots.set(bn.nodeId(), false);
		nodes[bn.nodeId()] = null;
		super.remove(bn);
		bn.nodeId(-1);
		nMods++;
		nodeCount--;
	}

	@Override
	public void remove(final Edge e)
	{
		if (!(e instanceof BasicEdge)) {
			throw new IllegalArgumentException();
		}
		final BasicEdge be = (BasicEdge) e;
		be.source().remove(be);
		_remove(be);
	}

	@Override
	public void relink(final Edge e)
	{
		if (!(e instanceof BasicEdge)) {
			throw new IllegalArgumentException();
		}
		final BasicEdge be = (BasicEdge) e;
		final int eid = be.edgeId();
		if (eid >= 0) {
			throw new IllegalArgumentException();
		}
		be.source().add(be);
		be.edgeId(-eid - 1);
		eSlots.set(-eid - 1, true);
		edgeCount++;
		eMods++;
	}


	// "low level" remove, which does nothing with graph topology
	// but deals with all the rest (except shrinking).
	private void _remove(final BasicEdge e)
	{
		final int eid = e.edgeId();
		eSlots.set(eid, false);
		e.edgeId(-eid - 1);
		edgeCount--;
		eMods++;
		super.remove(e);
	}

	@Override
	public void sortEdges(final Comparator<Edge> cmp)
	{
		BasicEdge[] ea = new BasicEdge[nodes.length];
		for (final BasicNode bn : nodes) {
			ea = bn.sortEdges(cmp, ea);
		}
	}


	@Override
	public void trimToSize()
	{
		if (nodeCount < nodeTop || nodeTop < nodeCap) {
			_shrinkNodes(nodeCount);
		}
		if (edgeCount < edgeTop || edgeTop < edgeCap) {
			_shrinkEdges(edgeCount);
		}
	}

	@Override
	public void clear()
	{
		super.clear();
		Arrays.fill(nodes, null);
		edgeCount = nodeCount = 0;
		edgeTop = nodeTop = 0;
		eSlots.clear();
		nSlots.clear();
		eMods++;
		nMods++;
	}

	private void _shrinkEdges(final int newCap)
	{
		final int[] perm = super.shrinkEdges(newCap);
		eSlots.clear();
		for (int i=0; i<nodeTop; ++i) {
			final BasicNode n = nodes[i];
			if (n == null) {
				continue;
			}
			for (BasicEdge f = n.out(); f != null; f = f.next()) {
				final int oid = f.edgeId();
				final int nid = perm[oid];
				f.edgeId(nid);
				eSlots.set(nid, true);
			}
		}
	}

	private void _shrinkNodes(final int newCap)
	{
		final int[] perm = shrinkNodes(newCap);
		nSlots.clear();
		final BasicNode[] newNodes = new BasicNode[newCap];
		for (final BasicNode n : nodes) {
			if (n == null) {
				continue;
			}
			final int oid = n.nodeId();
			final int nid = perm[oid];
			n.nodeId(nid);
			nSlots.set(nid, true);
			newNodes[nid] = n;
		}
		nodes = newNodes;
	}


	@Override
	public List<Node> genNodes(final int n)
	{
		final CList<Node> result = new CList<Node>();
		for (int i=0; i<n; ++i) {
			result.add(genNode());
		}
		return result;
	}

	@Override
	public BasicEdge genEdge(final Node u, final Node v)
	{
		if (!(u instanceof BasicNode) || !(v instanceof BasicNode)) {
			throw new IllegalArgumentException();
		}
		final BasicNode bu = (BasicNode) u;
		final BasicNode bv = (BasicNode) v;
		final BasicEdge res = new BasicEdge(this, edgeTop, bu, bv);
		if (edgeCap <= edgeTop + 1) {
			growEdges(edgeCap * 2);
		}
		eSlots.set(edgeTop, true);
		res.edgeId(edgeTop++);
		edgeCount++;
		bu.add(res);
		super.newEdge(res);
		eMods++;
		return res;
	}

	@Override
	protected void growNodes(final int newCap)
	{
		final BasicNode[] newNodes = new BasicNode[newCap];
		System.arraycopy(nodes, 0, newNodes, 0, nodeTop);
		nodes = newNodes;
		super.growNodes(newCap);
	}

	@Override
	public void moveEdge(final Edge e, final Node u, final Node v)
	{
		if (!(e instanceof BasicEdge) || !(u instanceof BasicNode)
				|| !(v instanceof BasicNode)) {
			throw new IllegalArgumentException();
		}
		final BasicEdge be = (BasicEdge) e;
		final BasicNode bu = (BasicNode) u;
		final BasicNode bv = (BasicNode) v;
		be.source().remove(be);
		be.source(bu);
		be.target(bv);
		bu.add(be);
		eMods++;
	}

	/*
      Javadoc'd in digraph.
	 */
	 @Override
	 public int nodeSize()
	{
		return nodeCount;
	}

	/*
      Javadoc'd in digraph.
	 */
	 @Override
	 public int nodeAttrSize()
	{
		return nodeTop;
	}

	/**
       Returns an appropriate size for an edge attribute array.
	 */
	 @Override
	 public int edgeSize()
	{
    	   return edgeCount;
	}

	@Override
	public int edgeAttrSize()
	{
		return edgeTop;
	}

	/*
      Javadoc'd in Graph
	 */
	 @Override
	 public Iterable<Node> nodes()
	 {
		final BasicDigraph g = this;
		return new Iterable<Node>() {
			@Override
			public Iterator<Node> iterator() {
				return new BasicNodeIterator(nodes, nMods);
			}
		};
	 }

	 /*
      Javadoc'd in Graph
	  */
	 @Override
	 public Iterable<Node> nodes(final NodeOrder order)
	 {
		 final int[] perm = order.permutation();
		 final ArrayList<Node> res = new ArrayList<Node>(nSlots.cardinality());
		 if (order.isReversed()) {
			 for (int i=perm.length - 1; i>=0; --i) {
				 if (perm[i] != -1) {
					 final Node n = nodes[perm[i]];
					 assert n != null;
					 res.add(n);
				 }
			 }
		 } else {
			 for (int i=0; i<perm.length; ++i) {
				 if (perm[i] != -1) {
					 final Node n = nodes[perm[i]];
					 assert n != null;
					 res.add(n);
				 }
			 }
		 }
		 return res;
	 }

	 /*
      Javadoc'd in Graph
	  */
	 @Override
	 public Iterable<Edge> edges()
	 {
		 return new Iterable<Edge>() {
			 @Override
			 public Iterator<Edge> iterator() {
				 return new BasicEdgeIterator(nodes, eMods);
			 }
		 };
	 }

	 @Override
	 public void ensureCapacity(final int n, final int m)
	 {
		 if (nodes.length < n) {
			 growNodes(n);
		 }
		 if (edgeCap < m) {
			 growEdges(m);
		 }
	 }


	 /**
       Produces a string for the graph in a simple adjacency list
       representation.  Each node <tt>n</tt> is listed in order (by {@link Node#nodeId}),
       followed by  a colon ':' and a space separated list of the nodes
       to which <tt>n</tt> is linked by an edge.
	  */
	 @Override
	 public String toString()
	 {
		 final StringBuffer sb = new StringBuffer();
		 for (final Node n : nodes()) {
			 sb.append(n.nodeId() + ":");
			 for (Edge e = n.out(); e != null; e = e.next()) {
				 sb.append(" " + e.target().nodeId());
			 }
			 sb.append("\n");
		 }
		 return sb.toString();
	 }
	 /*
      Iterator classes from here down.
	  */

	 class BasicNodeIterator implements Iterator<Node>
	 {
		 protected BasicNode[] nodes;
		 protected int index;
		 protected int expectedMods;

		 BasicNodeIterator(final BasicNode[] nodes, final int nMods)
		 {
			 this.nodes = nodes;
			 this.index = nextIndex(0);
			 this.expectedMods = nMods;
		 }

		 @Override
		 public boolean hasNext()
		 {
			 if (expectedMods != nMods) {
				 throw new ConcurrentModificationException();
			 }
			 return index < nodes.length;
		 }

		 @Override
		 public Node next()
		 {
			 if (expectedMods != nMods) {
				 throw new ConcurrentModificationException();
			 }
			 final BasicNode result = nodes[index];
			 index = nextIndex(index + 1);
			 return result;
		 }

		 protected int nextIndex(final int start)
		 {
			 int res = start;
			 while(res < nodes.length && nodes[res] == null) {
				 res++;
			 }
			 return res;
		 }

		 @Override
		 public void remove()
		 {
			 throw new UnsupportedOperationException();
		 }
	 }

	 class BasicEdgeIterator implements Iterator<Edge>
	 {
		 protected BasicNode[] nodes;
		 protected int index;
		 protected Edge edge;
		 protected int expectedMods;

		 BasicEdgeIterator(final BasicNode[] nodes, final int eMods)
		 {
			 this.nodes = nodes;
			 index = -1;
			 this.index = -1;
			 this.edge = null;
			 this.expectedMods = eMods;
			 while(edge == null && index < nodes.length) {
				 index = nextNodeIndex(index + 1);
				 if (index < nodes.length) {
					 edge = nodes[index].out();
				 }
			 }

		 }

		 protected int nextNodeIndex(final int i)
		 {
			 int res = i;
			 while(res < nodes.length && nodes[res] == null) {
				 res++;
			 }
			 return res;
		 }

		 @Override
		 public boolean hasNext()
		 {
			 if (expectedMods != eMods) {
				 throw new ConcurrentModificationException();
			 }
			 return edge != null;
		 }

		 @Override
		 public Edge next()
		 {
			 if (expectedMods != eMods) {
				 throw new ConcurrentModificationException();
			 }
			 final Edge result = edge;
			 edge = edge.next();
			 while(edge == null && index < nodes.length) {
				 index = nextNodeIndex(index + 1);
				 if (index < nodes.length) {
					 edge = nodes[index].out();
				 }
			 }
			 return result;
		 }

		 @Override
		 public void remove()
		 {
			 throw new UnsupportedOperationException();
		 }
	 }

}



