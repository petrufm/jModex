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
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize2aslanpp.interval.Fact;

public class SelfLoopRegion extends Node {

	private Edge theLoop;
	
	SelfLoopRegion(Edge theLoop) {
		this.theLoop = theLoop;
	}
		
 	static Node isSelfLoop(Node theNode) {
		SelfLoopRegion result = null;
 		Edge theLoopEdge = null;
		if(theNode.getIncomming().size() > 1) {
			for(Edge anEdge : theNode.getOutgoing()) {
				if(anEdge.getTo() == theNode) {
					if(theLoopEdge == null) {
						theLoopEdge = anEdge;
					} else {
						return null;
					}
				}
			}
			if(theLoopEdge == null) {
				return null;
			}
			result = new SelfLoopRegion(theLoopEdge);
			ArrayList<Edge> tmp = new ArrayList<Edge>();
			tmp.addAll(theNode.getIncomming());
			for(Edge anEdge : tmp) {
				if(anEdge != theLoopEdge) {
					anEdge.setTo(result);
				}
			}
			tmp.clear();
			tmp.addAll(theNode.getOutgoing());
			for(Edge anEdge : tmp) {
				if(anEdge != theLoopEdge) {
					anEdge.setFrom(result);
				}
			}
			return result;
		}
 	 	return null;
 	}
 	
	@Override
	public List<AslanppStatement> generateCode(AslanppGenerator.ConversionVisitor conversionVisitor, Set<? extends Fact> fatcs) {
		List<AslanppStatement> res = super.generateCode(conversionVisitor, null);
		TranslatedExpression transG = (TranslatedExpression) theLoop.getGuard().accept(conversionVisitor);
		res.addAll(transG.getRValueCode(true, null, null));
		AslanppReference theGuard = transG.getReferenceForRValueCode(true, null, null);
		AslanppWhile theWhile = new AslanppWhile(theGuard);
		List<AslanppStatement> inLoop = theLoop.generateCode(conversionVisitor, null);
		for(AslanppStatement aStm : inLoop) {
			theWhile.addStatement(aStm);
		}
		res.add(theWhile);
		return res;
	}

	public Node duplicateNode() {
		Node res = new SelfLoopRegion(theLoop);
		for(Edge anOutEdge : getOutgoing()) {
			Edge tmp = anOutEdge.duplicateEdge();
			tmp.setFrom(res);
			tmp.setTo(anOutEdge.getTo());
		}
		for(Edge anInEdge : getIncomming()) {
			if(!getOutgoing().contains(anInEdge)) {
				Edge tmp = anInEdge.duplicateEdge();
				tmp.setFrom(anInEdge.getFrom());
				tmp.setTo(res);
			}
		}		
		return res;
	}


}
