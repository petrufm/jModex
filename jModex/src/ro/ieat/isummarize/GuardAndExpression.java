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


public class GuardAndExpression extends GuardBinaryExpression {
	
	public GuardAndExpression(GuardExpression left, GuardExpression right) {
		super(left,right);
	}

	@Override
	public GuardExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		GuardExpression tmp1,tmp2;
		tmp1 = left.substitute(sub, newExpression);
		tmp2 = right.substitute(sub, newExpression);
		if(tmp1 != left || tmp2 != right) {
			return new GuardAndExpression(tmp1, tmp2);
		} else {
			return this;
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof GuardAndExpression) {
			return ((left == ((GuardAndExpression) o).left ||  left.equals(((GuardAndExpression) o).left)) && 
					(right == ((GuardAndExpression) o).right || right.equals(((GuardAndExpression) o).right))) ||
					((left == ((GuardAndExpression) o).right || left.equals(((GuardAndExpression) o).right)) && 
					 (right == ((GuardAndExpression) o).left || right.equals(((GuardAndExpression) o).left)));
		}
		return false;
	}

	@Override
	public String toString() {
		return (left instanceof GuardOrExpression ? "(" : "") + left + (left instanceof GuardOrExpression ? ")" : "") + 
				" AND " + 
			   (right instanceof GuardOrExpression ? "(" : "") + right + (right instanceof GuardOrExpression ? ")" : "");
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitGuardAndExpression(this);
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		List<ProgramExpressionProxy> res = left.getProxies();
		res.addAll(right.getProxies());
		return res;
	}

}
