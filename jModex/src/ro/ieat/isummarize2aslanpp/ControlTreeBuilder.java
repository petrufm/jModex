/*
 * Copyright (c) 2013-2014 Institute eAustria Timisoara
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ro.ieat.isummarize2aslanpp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Stack;

import ro.ieat.isummarize.MethodControlPoint;
import ro.ieat.isummarize.MethodPath;
import ro.ieat.isummarize.MethodSummary;

public class ControlTreeBuilder {
	
	private static HashSet<Node> visitedForPrinting = new HashSet<Node>();
	
	private static void generateDOT(Node state, PrintStream fos) {
		if(!visitedForPrinting.contains(state)) {
			visitedForPrinting.add(state);
			for(Edge e : state.getOutgoing()) {
				String label = "";
				fos.println("id_" + state.hashCode() + "->" + "id_" + e.getTo().hashCode() + "[label=\""+ label + "\"];");
				generateDOT(e.getTo(), fos);
			}
		}
	}

	private static IdentityHashMap<Object, Object> visited = new IdentityHashMap<Object, Object>();
	private static IdentityHashMap<Object,Object> eliminatedEntities = new IdentityHashMap<Object,Object>();
	public static Region buildTree(MethodSummary summary) {
		visited.clear();
		eliminatedEntities.clear();
		markEliminatedEntities(summary);	
		visited.clear();
		Node result = translateGraph(summary.getEntryControPoint());
		boolean changed;
		do {	
			visited.clear();
			revisitStack.clear();
			revisitStack.push(new IdentityHashMap<Object, Object>());
			result =  depthFirst(result);
			visited.clear();
			revisitStack.clear();
			changed = false;
			if(result.getIncomming().size() != 0 || result.getOutgoing().size() != 0) {
				visited.clear();
				changed = checkIreductible(result);				
				visited.clear();
				if(!changed) {
					//Prepare a view to see the problem
					try {
						/*String value = System.currentTimeMillis() + ".dot";
						File file = new File("/Users/petrum/" + value);
						PrintStream fos = new PrintStream(new FileOutputStream(file));
						fos.println("digraph " + "test" + " {");
						visitedForPrinting.clear();
						generateDOT(result, fos);
						fos.println("}");
						fos.close();*/
					} catch(Throwable e) {}
					// end
					throw new RuntimeException("The automaton cannot be reduced!");					
				}
			}
		} while(changed);
		return result;
	}
	
	private static boolean checkIreductible(Node aNode) {
		if(!visited.containsKey(aNode)) {
			visited.put(aNode, aNode);
			for(Edge anEdge: aNode.getOutgoing()) {
				boolean changed = checkIreductible(anEdge.getTo());
				if(changed) {
					return true;
				}
			}
			return IreductibleRegionOne.spliIreductibleRegionOne(aNode);
		}
		return false;
	}

	private static Node translateGraph(MethodControlPoint controlPoint) {
		if(!visited.containsKey(controlPoint)) {
			Node theDummyNode = new Node();
			visited.put(controlPoint, theDummyNode);
			for(MethodPath mp: controlPoint.getOutgoingPaths()) {
				Node toDummyNode = translateGraph(mp.getTo());
				if(eliminatedEntities.containsKey(mp)) {
					continue;
				}
				DummyEdge theDummyEdge = new DummyEdge(mp);
				theDummyEdge.setTo(toDummyNode);
				theDummyEdge.setFrom(theDummyNode);
			}
		}
		return (Node) visited.get(controlPoint);
	}
 	
	private static void markEliminatedEntities(MethodSummary summary) {
		//Eliminate the paths going from entry to exit and having no state updates
		MethodControlPoint entry = summary.getEntryControPoint();
		MethodControlPoint exit = summary.getExitControlPoint();
		for(MethodPath aPath : entry.getOutgoingPaths()) {
			if(aPath.getTo() == exit && aPath.getVariableExpressions().isEmpty() && entry.getOutgoingPaths().size() != 1) {
				eliminatedEntities.put(aPath, aPath);
			}
		}
	}
	
	private static Stack<IdentityHashMap<Object, Object>> revisitStack = new Stack<IdentityHashMap<Object, Object>>();
 	private static Node depthFirst(Node currentNode) {
		if(!revisitStack.peek().containsKey(currentNode)) {
			revisitStack.peek().put(currentNode, null);
			for(Edge edge: currentNode.getOutgoing()) {
				depthFirst(edge.getTo());
			}
			Node revisitFromNode = null;
			if((revisitFromNode = SelectRegion.isSelectHead(currentNode)) != null || 
			   (revisitFromNode = SelfLoopRegion.isSelfLoop(currentNode)) != null ||
			   (revisitFromNode = GuardedSequenceRegion.hasSuccessorGuardedSequenceRegion(currentNode)) != null ||
			   (revisitFromNode = GuardedSimpleSequenceRegion.isGuardedSimpleSequenceRegion(currentNode)) != null) {
				IdentityHashMap<Object, Object> toRevisit = new IdentityHashMap<Object, Object>();
				for(Object o : revisitStack.peek().keySet()) {
					if(revisitStack.peek().get(o) == null) {
						toRevisit.put(o,null);
					}
				}
				toRevisit.remove(revisitFromNode);
				revisitStack.push(toRevisit);
				Node returnedNode = depthFirst(revisitFromNode);
				toRevisit = revisitStack.pop();
				for(Object o : toRevisit.keySet()) {
					if(toRevisit.get(o) != null) {
						revisitStack.peek().put(o, o);					
					}
				}
				return returnedNode;
			} else {
				revisitStack.peek().put(currentNode, currentNode);
				return currentNode;
			}
		}
		return currentNode;
 	}
	
 	
}
