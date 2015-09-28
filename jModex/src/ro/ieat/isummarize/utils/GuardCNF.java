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

public class GuardCNF {

	private static CNFVisitor toCNF = new CNFVisitor();
	
	public static GuardExpression toCNF(GuardExpression theExp) {
		return (GuardExpression) theExp.accept(toCNF);
	}
		
	private static class CNFVisitor extends ExpressionAcyclicVisitor {
		
		public Object visitGuardAndExpression(GuardAndExpression expression) { 
			GuardExpression l = (GuardExpression) expression.getLeft().accept(this);
			GuardExpression r = (GuardExpression) expression.getRight().accept(this);
			if(l == expression.getLeft() && r == (GuardExpression) expression.getRight()) {
				return expression;
			}
			return new GuardAndExpression(l, r);
		}
		
		public Object visitGuardOrExpression(GuardOrExpression expression) { 
			GuardExpression l = (GuardExpression) expression.getLeft().accept(this);
			GuardExpression r = (GuardExpression) expression.getRight().accept(this);
			if(r instanceof GuardAndExpression) {
				return new GuardAndExpression((GuardExpression)new GuardOrExpression(l,((GuardAndExpression)r).getLeft()).accept(this), (GuardExpression)new GuardOrExpression(l,((GuardAndExpression)r).getRight()).accept(this));
			} else if(l instanceof GuardAndExpression) {
				return new GuardAndExpression((GuardExpression)new GuardOrExpression(((GuardAndExpression)l).getLeft(),r).accept(this), (GuardExpression) new GuardOrExpression(((GuardAndExpression)l).getRight(),r).accept(this));				
			} else {
				if(l == expression.getLeft() && r == (GuardExpression) expression.getRight()) {
					return expression;
				} else {
					return new GuardOrExpression(l, r);
				}
			}
		}
		
		public Object visitGuardNotExpression(GuardNotExpression expression) {
			GuardExpression notOperand = expression.getOperand();
			if(notOperand instanceof GuardAndExpression) {
				return (new GuardOrExpression(new GuardNotExpression(((GuardAndExpression)notOperand).getLeft()), new GuardNotExpression(((GuardAndExpression)notOperand).getLeft())).accept(this));
			} else if(notOperand instanceof GuardOrExpression) {
				return (new GuardAndExpression(new GuardNotExpression(((GuardOrExpression)notOperand).getLeft()), new GuardNotExpression(((GuardOrExpression)notOperand).getLeft())).accept(this));				
			} else if(notOperand instanceof GuardNotExpression) {
				return ((GuardNotExpression)notOperand).getOperand().accept(this);
			} else {
				return expression;
			}
		}
		
		public Object visitGuardProgramExpression(GuardProgramExpression expression) {
			return expression; 
		}
		
	}
	
}
