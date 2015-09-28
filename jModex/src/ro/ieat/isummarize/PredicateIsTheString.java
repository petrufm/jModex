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

import ro.ieat.isummarize.ProgramStringBinaryExpression.StringBinaryOperator;

public class PredicateIsTheString extends ExpressionAcyclicVisitor implements PredicateAbstractionPredicate {
				
	private String theString;
	
	public PredicateIsTheString(String theString) {
		this.theString = theString;
	}

	@Override
	public int evaluateConcreteToAbstract(ProgramExpression exp) {
		Object o = exp.accept(this);
		if(o == null) {
			return DONTKNOW;
		} else {
			return theString.equals(o) ? TRUE : FALSE;
		}
	}

	public String toString() {
		return "IsString_" + theString;
	}
	
	@Override
	public Object visitProgramIndexExpression(ProgramIndexExpression expression) {
		if(expression.isStringConstant()) {
			String tmp = expression.getStringConstant();
			return tmp.substring(1, tmp.length()-1);
		} else {
			return null;
		}
	}

	@Override
	public Object visitProgramStringBinaryExpression(ProgramStringBinaryExpression expression) {
		if(expression.getOperator() == StringBinaryOperator.CONCAT) {
			Object r1 = expression.getLeft().accept(this);
			Object r2 = expression.getRight().accept(this);
			if(r1 instanceof Boolean || r2 instanceof Boolean) {
				return false;
			}
			if(r1 != null && r2 != null) {
				return ((String)r1) + ((String)r2);
			}
			if(r1 != null) {
				if(theString.indexOf((String) r1) == -1) {
					return false;
				}
			}
			if(r2 != null) {
				if(theString.indexOf((String) r2) == -1) {
					return false;
				}
			}
		}
		return null;
	}
		
	public Object visitPredicateConcretization(PredicateAbstractionConcretization expression) {
		PredicateAbstractionPredicate thePredicate = expression.getPredicate();
		if(thePredicate.equals(this)) {
			Object o = expression.getExpression().accept(this);
			if(o != null && o instanceof Integer) {
				if(((Integer)o).intValue() == PredicateAbstractionPredicate.TRUE) {
					return theString;
				}
			}
		}
		return null;
	}

	public Object visitPredicateAbstractization(PredicateAbstractionAbstractization expression) {
		PredicateAbstractionPredicate thePredicate = expression.getPredicate();
		if(thePredicate.equals(this)) {
			PredicateIsTheString tmp = new PredicateIsTheString(theString);
			return tmp.evaluateConcreteToAbstract(expression.getExpression());
		}
		return null;
	}

	@Override
	public Object evaluateAbstractToConcrete(ProgramExpression exp) {
		Object o = exp.accept(this);
		if(o != null && theString.equals(o)) {
			return o;
		} else {
			return null;
		}
	}

}
