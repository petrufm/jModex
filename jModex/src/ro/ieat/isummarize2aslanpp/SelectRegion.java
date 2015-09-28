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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.GuardExpression;
import ro.ieat.isummarize.GuardOrExpression;

import ro.ieat.isummarize.utils.GuardSimplifier;
import ro.ieat.isummarize2aslanpp.interval.Fact;

class SelectRegion extends Edge {
	
	private List<Edge> allEdges = new ArrayList<Edge>();
			
	private SelectRegion(List<Edge> all) {
		allEdges.addAll(all);
	}
	
 	static Node isSelectHead(Node theNode) {
 		if(theNode.getOutgoing().size() > 1) {
 			HashMap<Node,ArrayList<Edge>> mapping = new HashMap<Node,ArrayList<Edge>>();
 			for(Edge anEdge : theNode.getOutgoing()) {
 				if(!mapping.keySet().contains(anEdge.getTo())) {
 					mapping.put(anEdge.getTo(), new ArrayList<Edge>());
 				}
 				mapping.get(anEdge.getTo()).add(anEdge);
 			}
 			boolean changed = false;
 			for(Node to : mapping.keySet()) {
 				if(mapping.get(to).size() > 1) {
 					changed = true;
 		 			SelectRegion result = new SelectRegion(mapping.get(to));
 					for(Edge anOutEdge : mapping.get(to)) {
 		 				theNode.removeOutgoing(anOutEdge);
 		 				to.removeIncomming(anOutEdge);
 		 			}
 		 			result.setTo(to);
 		 			result.setFrom(theNode);
 				}
 			}
 			if(changed) {
 				return theNode;
 			}
 		}
 		return null;
 	}

	@Override
	public List<AslanppStatement> generateCode(AslanppGenerator.ConversionVisitor conversionVisitor, Set<? extends Fact> facts) {
		List<AslanppStatement> code = new ArrayList<AslanppStatement>();
		AslanppSelect theSelect = new AslanppSelect();
		FactorOutCursorCreationOptimisation.tryCombineCursorCreationStart(allEdges.toArray(new Edge[]{}));
		Set<Fact> factoredOut = new HashSet<Fact>();
		for(Edge anEdge : allEdges) {
			HashSet<Fact> duplicatedFacts = new HashSet<Fact>();
			duplicatedFacts.addAll(factoredOut);
			if(facts != null) {
				duplicatedFacts.addAll(facts);
			}
			conversionVisitor.setFacts(duplicatedFacts);
			TranslatedExpression transG = (TranslatedExpression) anEdge.getGuard().accept(conversionVisitor);
			List<AslanppStatement> codeBeforeSelect = transG.getRValueCode(true, null, null);
			if(!Util.isTheSame(codeBeforeSelect, code)) {
				code.addAll(codeBeforeSelect);				
			}
			AslanppReference theGuard = transG.getReferenceForRValueCode(true, null, null);
			AslanppOn theOn = new AslanppOn(theGuard);
			
			factoredOut.addAll(conversionVisitor.getFacts());
			conversionVisitor.setFacts(Fact.fromGuardToUpdatePropagation(conversionVisitor.getFacts()));
			
			List<AslanppStatement> theUpdates = (List<AslanppStatement>) anEdge.generateCode(conversionVisitor, conversionVisitor.getFacts());
			for(AslanppStatement as : theUpdates) {
				theOn.addStatement(as);
			}
			theSelect.addOnStatement(theOn);
		}
		code.add(theSelect);
		FactorOutCursorCreationOptimisation.tryCombineCursorCreationStop(code);
		return code;
	}

	@Override
	public GuardExpression getGuard() {
		GuardExpression result = allEdges.get(0).getGuard();
		for(int i = 1; i < allEdges.size(); i++) {
			result = new GuardOrExpression(result,allEdges.get(i).getGuard());
		}
		result = GuardSimplifier.simplify(result);
		return result;
	}

	@Override
	public Edge duplicateEdge() {
		return new SelectRegion(allEdges);
	}

}