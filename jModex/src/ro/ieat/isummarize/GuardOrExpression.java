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

/**
 * @author alexandrugyori
 *
 */

public class GuardOrExpression extends GuardBinaryExpression {

	public GuardOrExpression(GuardExpression left, GuardExpression right) {
		super(left,right);
	}

	@Override
	public GuardExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		GuardExpression tmp1,tmp2;
		tmp1 = left.substitute(sub, newExpression);
		tmp2 = right.substitute(sub, newExpression);
		if(tmp1 != left || tmp2 != right) {
			return new GuardOrExpression(tmp1, tmp2);
		} else {
			return this;
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof GuardOrExpression) {
			return ((left == ((GuardOrExpression) o).left ||  left.equals(((GuardOrExpression) o).left)) && 
					(right == ((GuardOrExpression) o).right || right.equals(((GuardOrExpression) o).right))) ||
					((left == ((GuardOrExpression) o).right || left.equals(((GuardOrExpression) o).right)) && 
					 (right == ((GuardOrExpression) o).left || right.equals(((GuardOrExpression) o).left)));
		}
		return false;
	}

	@Override
	public String toString() {
		return left + " OR " + right;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitGuardOrExpression(this);
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		List<ProgramExpressionProxy> res = left.getProxies();
		res.addAll(right.getProxies());
		return res;
	}

}
