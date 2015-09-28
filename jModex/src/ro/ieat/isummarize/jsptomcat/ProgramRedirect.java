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
package ro.ieat.isummarize.jsptomcat;

import java.util.HashSet;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.PredicateAbstractionAbstractVariable;
import ro.ieat.isummarize.PredicateAbstractionAbstractization;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramSubstitutableVariable;
import ro.ieat.isummarize.TracedVariable;

public class ProgramRedirect implements TracedVariable {

	public interface ProgramRedirectVisitor {
		public Object visitProgramRedirectParameter(ProgramRedirect expression);
	}

	@Override
	public boolean isObservedGlobally() {
		return true;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramRedirectVisitor) {
			return ((ProgramRedirectVisitor)visitor).visitProgramRedirectParameter(this);
		}
		return null;
	}

	@Override
	public TracedVariable substituteSubExpression(ProgramSubstitutableVariable toSub, ProgramExpression newExp) {
		return this;
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
	
	public int hashCode() {
		return super.hashCode();
	}
	
	public String toString() {
		return "Redirect";
	}
	
	public ProgramExpression preparedForUpdateAssignment(ProgramExpression toAssign) {
		if(toAssign instanceof PredicateAbstractionAbstractization) {
			return ModelFactory.getInstance().createPredicateAbstractionConcretization(toAssign, ((PredicateAbstractionAbstractization) toAssign).getPredicate());
		}
		if(toAssign instanceof PredicateAbstractionAbstractVariable) {
			return ModelFactory.getInstance().createPredicateAbstractionConcretization(toAssign, ((PredicateAbstractionAbstractVariable) toAssign).getPredicate());
		}	
		return toAssign;
	}
	
	@Override
	public Set<ProgramExpression> getSubVariables() {
		return new HashSet<ProgramExpression>();
	}

}
