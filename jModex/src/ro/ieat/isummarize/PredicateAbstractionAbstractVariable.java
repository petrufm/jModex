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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PredicateAbstractionAbstractVariable extends ProgramIndexExpression {

	private TracedVariable abstractedVariable;
	private PredicateAbstractionPredicate thePredicate;
	
	PredicateAbstractionAbstractVariable(TracedVariable abstractedVariable, PredicateAbstractionPredicate thePredicate) {
		super((abstractedVariable instanceof ProgramIndexExpressionVariable) ? ((ProgramIndexExpression) abstractedVariable).getUse() : Integer.MIN_VALUE);
		this.abstractedVariable = abstractedVariable;
		this.thePredicate = thePredicate;
	}
	
	public PredicateAbstractionPredicate getPredicate() {
		return thePredicate;
	}
	
	@Override
	public boolean isObservedGlobally() {
		return abstractedVariable.isObservedGlobally();
	}

	@Override
	public String toString() {
		return "AbstractVariableOf(" + abstractedVariable + ")";
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		if(abstractedVariable instanceof ProgramSubstitutableVariable) {
			return ((PredicateAbstractionAbstractVariable) abstractedVariable).getVariables();
		} else {
			return new HashSet<ProgramExpression>();
		}
	}

	@Override
	public Set<ProgramExpression> getSubVariables() {
		if(abstractedVariable instanceof ProgramSubstitutableVariable) {
			return ((PredicateAbstractionAbstractVariable) abstractedVariable).getVariables();
		} else {
			return new HashSet<ProgramExpression>();
		}
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof PredicateAbstractionAbstractVariable) {
			return abstractedVariable.equals(((PredicateAbstractionAbstractVariable) o).abstractedVariable);
		}
		return false;
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		if(this.equals(sub)) {
			return ModelFactory.getInstance().createPredicateAbstractionAbstractization(newExpression, thePredicate);
		}
		return this;
	}

	@Override
	protected int getHashCode() {
		return abstractedVariable.hashCode();
	}

	public ProgramExpression preparedForUpdate(ProgramExpression toAssign) {
		return ModelFactory.getInstance().createPredicateAbstractionAbstractization(toAssign, thePredicate);
	}

	@Override
	public boolean isStringConstant() {
		return false;
	}

	@Override
	public String getStringConstant() {
		return null;
	}

	@Override
	public boolean isNullConstant() {
		return false;
	}

	@Override
	public boolean isNumberConstant() {
		return false;
	}

	@Override
	public Number getNumberConstant() {
		return null;
	}

	@Override
	public boolean isIntConstant() {
		return false;
	}

	@Override
	public Integer getIntConstant() {
		return null;
	}

	@Override
	public boolean isLongConstant() {
		return false;
	}

	@Override
	public Long getLongConstant() {
		return null;
	}

	@Override
	public boolean isFloatConstant() {
		return false;
	}

	@Override
	public Float getFloatConstant() {
		return null;
	}

	@Override
	public boolean isDoubleConstant() {
		return false;
	}

	@Override
	public Double getDoubleConstant() {
		return null;
	}

	@Override
	public String getDeclaredTypeName() {
		return "boolean";
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return new ArrayList<ProgramExpressionProxy>();
	}

}
