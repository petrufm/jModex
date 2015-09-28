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


import com.ibm.wala.shrikeBT.IBinaryOpInstruction;

public class ProgramBinaryExpression extends ProgramExpression {

	private ProgramExpression left;
	private ProgramExpression right;
	private IBinaryOpInstruction.IOperator operator;
	
	public ProgramBinaryExpression(IBinaryOpInstruction.IOperator operator, ProgramExpression left, ProgramExpression right) {
		this.left = left;
		this.right = right;
		this.operator = operator;
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression tmp1,tmp2;
		tmp1 = left.substitute(sub, newExpression);
		tmp2 = right.substitute(sub, newExpression);
		if(tmp1 != left || tmp2 != right) {
			return new ProgramBinaryExpression(operator, tmp1, tmp2);
		} else {
			return this;			
		}
	}

	@Override
	public String toString() {
		return left.toString() + " " + operator + " " + right.toString();
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramBinaryExpression) {
			return operator.equals(((ProgramBinaryExpression)o).operator) && 
					(left == ((ProgramBinaryExpression) o).left || left.structuralEquals(((ProgramBinaryExpression) o).left)) && 
					(right == ((ProgramBinaryExpression) o).right || right.structuralEquals(((ProgramBinaryExpression) o).right));
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
		return visitor.visitProgramBinaryExpression(this);
	}
	
	public ProgramExpression getLeft() {
		return left;
	}

	public ProgramExpression getRight() {
		return right;
	}
	
	public IBinaryOpInstruction.Operator getOperator() {
		return (IBinaryOpInstruction.Operator) operator;
	}

	@Override
	protected int getHashCode() {
		return 23 * left.hashCode() + 7 * right.hashCode() + operator.hashCode();
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		List<ProgramExpressionProxy> res = left.getProxies();
		res.addAll(right.getProxies());
		return res;
	}

}
