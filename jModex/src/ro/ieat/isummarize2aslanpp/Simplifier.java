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
import java.util.List;
import java.util.Set;

public class Simplifier {
		
	private static AslanppVisitor visitor1 = new AllTermsInConjunction();
	public static AslanppExpression simplifyLogic(AslanppExpression exp) {
		if(exp instanceof AslanppLogicalConjunction) {
			boolean changed = false;
			List<AslanppExpression> listOfExpressionInConjunction = (List<AslanppExpression>) exp.accept(visitor1);
			for(int i = 0; i < listOfExpressionInConjunction.size(); i++) {
				for(int j = i + 1; j < listOfExpressionInConjunction.size(); j++) {
					if(listOfExpressionInConjunction.get(i).isTheSame(listOfExpressionInConjunction.get(j))) {
						changed = true;
						listOfExpressionInConjunction.remove(j);
						j--;
					}
				}
			}
			if(changed) {
				if(listOfExpressionInConjunction.size() == 1) {
					return listOfExpressionInConjunction.get(0);
				}
				AslanppLogicalConjunction cumulative = AslanppLogicalConjunction.createInstance((AslanppReference)listOfExpressionInConjunction.get(0), (AslanppReference)listOfExpressionInConjunction.get(1));
				for(int i = 2; i < listOfExpressionInConjunction.size(); i++) {
					cumulative = AslanppLogicalConjunction.createInstance(cumulative,(AslanppReference)listOfExpressionInConjunction.get(i));
				}
				return cumulative;
			}
		}
		return exp;
	}
	
	private static class AllTermsInConjunction extends AslanppVisitor {		
		public Object visitAslanppLogicalConjunction(AslanppLogicalConjunction node) {
			List<AslanppExpression> resultingExpressions = new ArrayList<AslanppExpression>();
			Object expl = node.getLeft().accept(this);
			if(!(expl instanceof List)) {
				resultingExpressions.add(node.getLeft());
			} else {
				resultingExpressions.addAll((List<AslanppExpression>)expl);	
			}
			Object expr = node.getRight().accept(this);
			if(!(expr instanceof List)) {
				resultingExpressions.add(node.getRight());
			} else {
				resultingExpressions.addAll((List<AslanppExpression>)expr);	
			}
			return resultingExpressions;
		}
	}

	public static abstract class AslanppConstraint {
		public abstract boolean equals(Object o);
		public abstract int hashCode();		
	}

	public static Set<AslanppConstraint> getConstraintsForSatisfiability(AslanppExpression exp) {
		return new HashSet<AslanppConstraint>();
	}
	
}
