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


public class ProgramStringComparison extends ProgramExpression {

	private ProgramExpression left;
	private ProgramExpression right;

	public ProgramStringComparison(ProgramExpression left, ProgramExpression right) {
		this.left = left;
		this.right = right;
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression tmp1,tmp2;
		tmp1 = left.substitute(sub, newExpression);
		tmp2 = right.substitute(sub, newExpression);
		if(tmp1 != left || tmp2 != right) {
			return new ProgramStringComparison(tmp1, tmp2);
		} else {
			return this;			
		}
	}

	@Override
	public String toString() {
		return "equals(" + left + "," + right + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramStringComparison) {
			return (left == ((ProgramStringComparison) o).left || left.structuralEquals(((ProgramStringComparison) o).left)) && 
				   (right == ((ProgramStringComparison) o).right || right.structuralEquals(((ProgramStringComparison) o).right));
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		Set<ProgramExpression> leaves = left.getVariables();
		leaves.addAll(right.getVariables());
		return leaves;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramStringComparison(this);
	}
	
	public ProgramExpression getLeft() {
		return left;
	}

	public ProgramExpression getRight() {
		return right;
	}

	@Override
	protected int getHashCode() {
		return 23 * left.hashCode() + right.hashCode();
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		List<ProgramExpressionProxy> res = this.left.getProxies();
		res.addAll(this.right.getProxies());
		return res;
	}

}
