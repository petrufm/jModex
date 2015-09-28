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

public abstract class ExpressionAcyclicVisitor {

	public Object visitGuardAndExpression(GuardAndExpression expression) { 
		return null; 
	}
	
	public Object visitGuardOrExpression(GuardOrExpression expression) { 
		return null; 
	}
	
	public Object visitGuardNotExpression(GuardNotExpression expression) {
		return null; 
	}
	
	public Object visitGuardProgramExpression(GuardProgramExpression expression) {
		return null; 
	}

	public Object visitGuardTrueExpression(GuardTrueExpression expression) {
		return null;
	}
	
	public Object visitProgramBinaryExpression(ProgramBinaryExpression expression) {
		return null;
	}

	public Object visitProgramRelationalExpression(ProgramRelationalExpression expression) {
		return null;
	}

	public Object visitProgramStringComparison(ProgramStringComparison expression) {
		return null;
	}

	public Object visitProgramIndexExpression(ProgramIndexExpression expression) {
		return null;
	}

	public Object visitProgramField(ProgramField expression) {
		return null;
	}

	public Object visitProgramPhiExpression(ProgramPhiExpression epression) {
		return null;
	}

	public Object visitProgramExpressionProxy(ProgramExpressionProxy expression) {
		return null;
	}
	
	public Object visitProgramReturnVariable(ProgramReturnVariable expression) {
		return null;
	}

	public Object visitProgramStringBinaryExpression(ProgramStringBinaryExpression expression) {
		return null;
	}

	public Object visitProgramFunction(ProgramFunction expression) {
		return null;
	}

	public Object visitPredicateAbstractization(PredicateAbstractionAbstractization expression) {
		return null;
	}

	public Object visitPredicateConcretization(PredicateAbstractionConcretization expression) {
		return null;
	}

	public Object visitProgramConversionExpression(ProgramConversionExpression expression) {
		return null;
	}

	public Object visitProgramGetObject(ProgramGetObject expression) {
		return null;
	}

	public Object visitProgramUniqueInstanceCounter(ProgramUniqueInstanceCounter expression) {
		return null;
	}

	public Object visitProgramUniqueInstanceCounterAction(ProgramUniqueInstanceCounterAction expression) {
		return null;
	}

}
