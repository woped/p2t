package de.dhbw.woped.process2text.bpmnProcessing.bpt.graph.algo.tctree;

import de.dhbw.woped.process2text.bpmnProcessing.bpt.hypergraph.abs.IVertex;

import java.util.ArrayList;

/**
 * This NodeList is an abstraction of the underlying list type, which stores vertices.
 * 
 * @author Christian Wiggert
 *
 * @param Implementation of IVertex
 */
public class NodeList<V extends IVertex> extends ArrayList<V> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -748791916008781735L;

}