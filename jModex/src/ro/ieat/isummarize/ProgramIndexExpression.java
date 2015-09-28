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

import java.util.Set;

public abstract class ProgramIndexExpression extends ProgramSubstitutableVariable implements TracedVariable {

	protected int use;
	
	ProgramIndexExpression(int use) {
		this.use = use;
	}
	
	public abstract boolean equals(Object o);
	
	protected abstract int getHashCode();
	
	public abstract String toString();
	
	public abstract Set<ProgramExpression> getVariables();
	
	public abstract boolean isStringConstant();
	
	public abstract String getStringConstant();
	
	public abstract boolean isNullConstant();
	
	public abstract boolean isNumberConstant();
	
	public abstract Number getNumberConstant();
	
	public abstract boolean isIntConstant();
	
	public abstract Integer getIntConstant();
		
	public abstract boolean isLongConstant();
	
	public abstract Long getLongConstant();

	public abstract boolean isFloatConstant();

	public abstract Float getFloatConstant();

	public abstract boolean isDoubleConstant();
	
	public abstract Double getDoubleConstant();
	
	public abstract String getDeclaredTypeName();
	
	int getUse() {
		return use;
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

	//Non-State dependent
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		if(this.equals(sub)) {
			return newExpression;
		}
		return this;
	}
	
	@Override
	public TracedVariable substituteSubExpression(ProgramSubstitutableVariable toSub, ProgramExpression newExp) {
		return this;
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		return this.equals(o);
	}

	@Override
	public boolean isObservedGlobally() {
		return false;
	}
	
	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramIndexExpression(this);
	}
	
}
