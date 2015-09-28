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

import ro.ieat.isummarize.GuardTrueExpression;
import ro.ieat.isummarize2aslanpp.interval.Fact;

public class GuardedSimpleSequenceRegion extends Node {

	private Edge theEdge;
	private Node node1, node2;
	
	public GuardedSimpleSequenceRegion(Node node1, Edge theEdge, Node node2) {
		this.theEdge = theEdge;
		this.node1 = node1;
		this.node2 = node2;
	}

	static GuardedSimpleSequenceRegion isGuardedSimpleSequenceRegion(Node theNode) {
		if(theNode.getOutgoing().size() == 1 && theNode.getOutgoing().get(0).getTo().getIncomming().size() == 1
		   ) {
			GuardedSimpleSequenceRegion result = new GuardedSimpleSequenceRegion(theNode, theNode.getOutgoing().get(0), theNode.getOutgoing().get(0).getTo());
			ArrayList<Edge> tmp = new ArrayList<Edge>();
			tmp.addAll(theNode.getIncomming());
			for(Edge anEdge : tmp) {
				anEdge.setTo(result);
			}
			tmp.clear();
			tmp.addAll(theNode.getOutgoing().get(0).getTo().getOutgoing());
			for(Edge anEdge : tmp) {
				anEdge.setFrom(result);
			}
			return result;
		}
		return null;	
	}
	
	@Override
	public List<AslanppStatement> generateCode(AslanppGenerator.ConversionVisitor conversionVisitor, Set<? extends Fact> fatcs) {
		List<AslanppStatement> code = new ArrayList<AslanppStatement>();
		code.addAll(node1.generateCode(conversionVisitor,null));
		if(theEdge instanceof SelectRegion) {
			code.addAll(theEdge.generateCode(conversionVisitor,null));
		} else {
			 AslanppSelect theSelect = new AslanppSelect();
			 FactorOutCursorCreationOptimisation.tryCombineCursorCreationStart(theEdge);
			 TranslatedExpression transG = ((TranslatedExpression) theEdge.getGuard().accept(conversionVisitor));
			 code.addAll(transG.getRValueCode(true, null, null));
			 AslanppReference theGuard = transG.getReferenceForRValueCode(true, null, null);
			 AslanppOn theOn = new AslanppOn(theGuard);
			 theSelect.addOnStatement(theOn);
			 List<AslanppStatement> inEdge = theEdge.generateCode(conversionVisitor, null);
			 for(AslanppStatement aStm : inEdge) {
				 theOn.addStatement(aStm);
			 }
			 if(theEdge.getGuard().equals(new GuardTrueExpression())) {
				 code.addAll(theSelect.getOns().get(0).getBody());
			 } else {
				 code.add(theSelect);
			 }
			 FactorOutCursorCreationOptimisation.tryCombineCursorCreationStop(code);
		}
		code.addAll(node2.generateCode(conversionVisitor,null));
		return code;
	}

	public Node duplicateNode() {
		Node res = new GuardedSimpleSequenceRegion(node1,theEdge,node2);
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
