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

import java.util.HashSet;
import java.util.Set;

public class ProgramReturnVariable implements TracedVariable {

	public boolean equals(Object o) {
		if(o instanceof ProgramReturnVariable) {
			return true;
		}
		return false;
	}

	public int hashCode() {
		return 0;
	}

	@Override
	public String toString() {
		return "ReturnVariable";
	}

	@Override
	public boolean isObservedGlobally() {
		return false;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramReturnVariable(this);
	}

	@Override
	public TracedVariable substituteSubExpression(ProgramSubstitutableVariable toSub, ProgramExpression newExp) {
		return this;
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
