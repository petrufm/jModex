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
package ro.ieat.isummarize.utils;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.GuardAndExpression;
import ro.ieat.isummarize.GuardExpression;
import ro.ieat.isummarize.GuardNotExpression;
import ro.ieat.isummarize.GuardOrExpression;
import ro.ieat.isummarize.GuardProgramExpression;
import ro.ieat.isummarize.GuardTrueExpression;

public class GuardCreateDisjunctionSimplifiedIfFundamentalConjunctions {

	private static FundamentalConjunctionWithComplementsEliminationVisitor fcce = new FundamentalConjunctionWithComplementsEliminationVisitor();
	
	public static GuardExpression createOrSimplifiedIfFundamentalConjunctions(GuardExpression exp1, GuardExpression exp2) {
		GuardExpression result = fcce.process(exp1, exp2);
		if(result == null) {
				return new GuardOrExpression(exp1,exp2);
		} else {
				return result;
		}
	}
	
	private static class FundamentalConjunctionWithComplementsEliminationVisitor extends ExpressionAcyclicVisitor {
		
		private GuardExpression compareWith;
				
		public GuardExpression process(GuardExpression exp1, GuardExpression exp2) {
			compareWith = exp2;
			return (GuardExpression) exp1.accept(this);
		}
		
		public Object visitGuardAndExpression(GuardAndExpression expression) { 
			if(compareWith instanceof GuardAndExpression) {
				GuardAndExpression tmp = (GuardAndExpression) compareWith;				
				compareWith = tmp.getLeft();
				GuardExpression newLeft = (GuardExpression) expression.getLeft().accept(this);
				if(newLeft == null) {
					compareWith = tmp;
					return null;
				}
				compareWith = tmp.getRight();
				GuardExpression newRight = (GuardExpression) expression.getRight().accept(this);
				if(newRight == null) {
					compareWith = tmp;
					return null;
				}
				compareWith = tmp;
				if(newLeft == expression.getLeft() && newRight == expression.getRight()) {
					return expression;
				} else {
					return new GuardAndExpression(newLeft, newRight);
				}
			}
			return null;
		}
		
		public Object visitGuardOrExpression(GuardOrExpression expression) { 
			return null;
		}

		public Object visitGuardTrueExpression(GuardTrueExpression expression) { 
			if(compareWith instanceof GuardTrueExpression) {
				return expression;
			} else {
				return null;
			}
		}

		public Object visitGuardNotExpression(GuardNotExpression expression) {
			if(expression.getOperand() instanceof GuardProgramExpression) {
				if(compareWith.equals(expression)) {
					return expression;
				}
				if(compareWith instanceof GuardProgramExpression) {
					if(compareWith.equals(expression.getOperand())) {
						return new GuardTrueExpression();
					}
				}
			}
			return null;
		}
		
		public Object visitGuardProgramExpression(GuardProgramExpression expression) {
			if(compareWith.equals(expression)) {
				return expression;
			}
			if(compareWith instanceof GuardNotExpression) {
				if(((GuardNotExpression) compareWith).getOperand().equals(expression)) {
					return new GuardTrueExpression();
				}
			}
			return null;
		}
		
	}
	
}
