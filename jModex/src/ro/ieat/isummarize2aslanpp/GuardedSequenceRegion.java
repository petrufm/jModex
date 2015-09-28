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

import ro.ieat.isummarize.GuardExpression;
import ro.ieat.isummarize2aslanpp.interval.Fact;

public class GuardedSequenceRegion extends Edge {

	private Edge in, out;
	private Node theNode;
	
	GuardedSequenceRegion(Edge in, Node theNode, Edge out) {
		this.in = in;
		this.out = out;
		this.theNode = theNode;
	}

	private static Edge isGuardedSequenceRegion(Node aSuccessor) {
		if(aSuccessor.getIncomming().size() == 1 && aSuccessor.getOutgoing().size() == 1) {
			GuardedSequenceRegion result = new GuardedSequenceRegion(aSuccessor.getIncomming().get(0),aSuccessor,aSuccessor.getOutgoing().get(0));
			aSuccessor.getIncomming().get(0).getFrom().removeOutgoing(aSuccessor.getIncomming().get(0));
			aSuccessor.getOutgoing().get(0).getTo().removeIncomming(aSuccessor.getOutgoing().get(0));		
			result.setTo(aSuccessor.getOutgoing().get(0).getTo());
			result.setFrom(aSuccessor.getIncomming().get(0).getFrom());
			return result;	
		}
		return null;
	}
	
	static Node hasSuccessorGuardedSequenceRegion(Node theNode) {
		for(Edge anEdge : theNode.getOutgoing()) {
			Edge tmp = isGuardedSequenceRegion(anEdge.getTo());
			if(tmp != null) {
				return theNode;
			}
		}
		return null;
	}

	@Override
	public List<AslanppStatement> generateCode(AslanppGenerator.ConversionVisitor conversionVisitor, Set<? extends Fact> fatcs) {
		List<AslanppStatement> code = new ArrayList<AslanppStatement>();
		code.addAll(in.generateCode(conversionVisitor,null));
		code.addAll(theNode.generateCode(conversionVisitor,null));
		if(out instanceof SelectRegion) {
			code.addAll(out.generateCode(conversionVisitor,null));
		} else {
			AslanppSelect theSelect = new AslanppSelect();
			TranslatedExpression transG = (TranslatedExpression) out.getGuard().accept(conversionVisitor);
			code.addAll(transG.getRValueCode(true, null, null));
			AslanppReference theGuard = transG.getReferenceForRValueCode(true, null, null);
			AslanppOn theOn = new AslanppOn(theGuard);
			for(AslanppStatement aStm : out.generateCode(conversionVisitor,null)) {
				theOn.addStatement(aStm);
			}
			theSelect.addOnStatement(theOn);
			code.add(theSelect);
		}
		return code;
	}

	@Override
	public GuardExpression getGuard() {
		return in.getGuard();
	}

	@Override
	public Edge duplicateEdge() {
		return new GuardedSequenceRegion(in, theNode, out);
	}
	
}
