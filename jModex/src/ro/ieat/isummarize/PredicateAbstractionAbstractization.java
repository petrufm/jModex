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
package ro.ieat.isummarize;

import java.util.List;
import java.util.Set;

public class PredicateAbstractionAbstractization extends ProgramExpression {

	private ProgramExpression exp;
	private PredicateAbstractionPredicate thePredicate;
	
	PredicateAbstractionAbstractization(ProgramExpression exp, PredicateAbstractionPredicate thePredicate) {
		this.exp = exp;
		this.thePredicate = thePredicate;
	}
	
	public PredicateAbstractionPredicate getPredicate() {
		return thePredicate;
	}
	
	public ProgramExpression getExpression() {
		return exp;
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression res = exp.substitute(sub, newExpression);
		if(res == exp) {
			return this;
		} else {
			return ModelFactory.getInstance().createPredicateAbstractionAbstractization(res, thePredicate);
		}
	}

	@Override
	public String toString() {
		int v = thePredicate.evaluateConcreteToAbstract(exp);
		String value;
		if(v == PredicateAbstractionPredicate.FALSE) {
			value = "FALSE";
		} else if(v == PredicateAbstractionPredicate.TRUE) {			
			value = "TRUE";
		} else {
			value = "UNKNOWN";			
		}
		return "AbstractValue" + thePredicate + "(" + value + ":" + exp + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof PredicateAbstractionAbstractization) {
			if(exp.structuralEquals(((PredicateAbstractionAbstractization)o).exp)) {
				return true;
			}
			int v1 = thePredicate.evaluateConcreteToAbstract(exp);
			int v2 = thePredicate.evaluateConcreteToAbstract(((PredicateAbstractionAbstractization)o).exp);
			if(v1 == v2 && v1 != PredicateAbstractionPredicate.DONTKNOW) {
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		return exp.getVariables();
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitPredicateAbstractization(this);
	}

	@Override
	protected int getHashCode() {
		return 23 * exp.hashCode() + 7 * thePredicate.hashCode();
	}

	public boolean hasKnownValue() {
		return thePredicate.evaluateAbstractToConcrete(exp) != null;
	}
	
	public Object getKnownValue() {
		return thePredicate.evaluateAbstractToConcrete(exp);
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return exp.getProxies();
	}
	
}
