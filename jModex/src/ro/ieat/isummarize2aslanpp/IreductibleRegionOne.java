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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class IreductibleRegionOne {

	public static boolean spliIreductibleRegionOne(Node currentNode) {
		for(Edge edge1 : currentNode.getOutgoing()) {
			for(Edge edge2 : currentNode.getOutgoing()) {
				if(edge1 == edge2 || edge1.getTo() == edge2.getTo()) {
					continue;
				}
				Node split = null, exit = null;
				Node succ1 = edge1.getTo();
				Node succ2 = edge2.getTo();
				if(succ1.getOutgoing().size() == 1 && succ1.getOutgoing().get(0).getTo() == succ2) {
					split = succ1;
					exit = succ2;
				}
				if(succ2.getOutgoing().size() == 1 && succ2.getOutgoing().get(0).getTo() == succ1) {
					split = succ2;
					exit = succ1;
				}
				if(split == null) {
					continue;
				}
				Set<Node> predOfcurrent = getDirectPredecessors(split);
				addIndirectPredecessors(predOfcurrent);
			
				Set<Node> predOfSplit = getDirectPredecessors(split);
				predOfSplit.remove(currentNode);
				addIndirectPredecessors(predOfSplit);
				predOfSplit.retainAll(predOfcurrent);
				if(predOfSplit.size() == 0) {
					continue;
				}
				Set<Node> predOfExit = getDirectPredecessors(exit);
				predOfExit.remove(currentNode);
				predOfExit.remove(split);
				addIndirectPredecessors(predOfExit);
				predOfExit.retainAll(predOfcurrent);
				if(predOfExit.size() == 0) {
					continue;				
				}
			
				//Perform node splitting
				Set<Node> splitPred = getDirectPredecessors(split);
				splitPred.remove(currentNode);
				for(Node aPred: splitPred) {
					Node aCopy = split.duplicateNode();
					List<Edge> tmp = new ArrayList<Edge>();
					tmp.addAll(aCopy.getIncomming());
					for(Edge anEdge : tmp) {
						if(anEdge.getFrom() != aPred) {
							anEdge.getFrom().removeOutgoing(anEdge);
							aCopy.removeIncomming(anEdge);
						}
					}
				}
				
				//The original node remains connected only with the current node
				List<Edge> tmp = new ArrayList<Edge>();
				tmp.addAll(split.getIncomming());
				for(Edge anEdge : tmp) {
					if(anEdge.getFrom() != currentNode) {
						anEdge.getFrom().removeOutgoing(anEdge);
						split.removeIncomming(anEdge);
					}
				}
				return true;
			}
		}
		return false;
	}
	
	private static void addIndirectPredecessors(Set<Node> directPredOfNode) {
		LinkedList<Node> workList = new LinkedList<Node>();
		HashSet<Node> visited = new HashSet<Node>();
		workList.addAll(directPredOfNode);
		while(!workList.isEmpty()) {
			Node aNode = workList.removeFirst();
			if(!visited.contains(aNode)) {
				for(Edge anEdge : aNode.getIncomming()) {
					workList.add(anEdge.getFrom());
				}
			}
			visited.add(aNode);
		}
		directPredOfNode.addAll(visited);
	}

	private static HashSet<Node> getDirectPredecessors(Node x) {
		HashSet<Node> res = new HashSet<Node>();
		for(Edge anEdge : x.getIncomming()) {
			res.add(anEdge.getFrom());
		}
		return res;
	}

}
