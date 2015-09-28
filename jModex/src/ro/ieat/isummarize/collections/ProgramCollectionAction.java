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
package ro.ieat.isummarize.collections;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.GuardAndExpression;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramSubstitutableVariable;

public class ProgramCollectionAction extends ProgramExpression {

	public enum CollectionActions { ADD };
	
	public interface ProgramCollectionActionVisitor {
		public Object visitProgramCollectionAction(ProgramCollectionAction cg);
	}

	private CollectionActions theAction;
	private ProgramExpression theCollection;
	private ProgramExpression theValue;
	
	public ProgramCollectionAction(CollectionActions ca, ProgramExpression theCollection, ProgramExpression value) {
		this.theAction = ca;
		this.theCollection = theCollection;
		this.theValue = value;
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression t1 = theCollection.substitute(sub, newExpression);
		ProgramExpression t2 = theValue.substitute(sub, newExpression);
		if(t1 != theCollection || t2 != theValue) {
			return new ProgramCollectionAction(theAction, t1, t2);
		}
		return this;
	}

	@Override
	public String toString() {
		return "CollectionAction(" + theAction + "," + theCollection + "," + theValue + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramCollectionAction) {
			return theAction == ((ProgramCollectionAction)o).theAction 
					&& theCollection.structuralEquals(((ProgramCollectionAction)o).theCollection)
					&& theValue.structuralEquals(((ProgramCollectionAction)o).theValue);
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> res = new HashSet<ProgramExpression>();
		res.addAll(theCollection.getVariables());
		res.addAll(theValue.getVariables());
		return res;
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		ArrayList<ProgramExpressionProxy> res = new ArrayList<ProgramExpressionProxy>();
		res.addAll(theCollection.getProxies());
		res.addAll(theValue.getProxies());
		return res;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramCollectionActionVisitor) {
			return ((ProgramCollectionActionVisitor)visitor).visitProgramCollectionAction(this);
		}
		return null;
	}

	@Override
	protected int getHashCode() {
		return 39 * theCollection.hashCode() + theAction.hashCode() + theValue.hashCode();
	}

	public ProgramExpression getCollectionIdExpression() {
		return theCollection;
	}
	
	public CollectionActions getAction() {
		return theAction;
	}

	public ProgramExpression getValue() {
		return theValue;
	}

}
