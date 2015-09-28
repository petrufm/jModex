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

public class ProgramStringBinaryExpression extends ProgramExpression {

	public enum StringBinaryOperator {
		CONCAT
	}
	
	private ProgramExpression left,right;
	private StringBinaryOperator operator;
	
	public ProgramStringBinaryExpression(StringBinaryOperator op,ProgramExpression left, ProgramExpression right) {
		this.operator = op;
		this.left = left;
		this.right = right;
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression tmp1,tmp2;
		tmp1 = left.substitute(sub, newExpression);
		tmp2 = right.substitute(sub, newExpression);
		if(tmp1 != left || tmp2 != right) {
			if(tmp1 instanceof ProgramIndexExpressionConstant && tmp2 instanceof ProgramIndexExpressionConstant) {
				if(((ProgramIndexExpressionConstant)tmp1).isStringConstant() && ((ProgramIndexExpressionConstant)tmp2).isStringConstant()) {
					String s1 = ((ProgramIndexExpressionConstant)tmp1).getStringConstant();
					String s2 = ((ProgramIndexExpressionConstant)tmp2).getStringConstant();	
					String res = "\"" + s1.substring(1,s1.length()-1) + s2.substring(1,s2.length()-1) + "\"";
					return new ProgramIndexExpressionConstant(res);
				}
			}
			if(tmp1 instanceof ProgramIndexExpressionConstant && ((ProgramIndexExpressionConstant)tmp1).isStringConstant() && tmp2 instanceof ProgramStringBinaryExpression && ((ProgramStringBinaryExpression)tmp2).operator == StringBinaryOperator.CONCAT) {
				ProgramStringBinaryExpression t2 = (ProgramStringBinaryExpression)tmp2;
				if(t2.left instanceof ProgramIndexExpressionConstant && ((ProgramIndexExpressionConstant)t2.left).isStringConstant()) {
					String s1 = ((ProgramIndexExpressionConstant)tmp1).getStringConstant();
					String s2 = ((ProgramIndexExpressionConstant)t2.left).getStringConstant();				
					String res = "\"" + s1.substring(1,s1.length()-1) + s2.substring(1,s2.length()-1) + "\"";
					return new ProgramStringBinaryExpression(StringBinaryOperator.CONCAT, new ProgramIndexExpressionConstant(res), t2.right);
				}
			}
			if(tmp2 instanceof ProgramIndexExpressionConstant && ((ProgramIndexExpressionConstant)tmp2).isStringConstant() && tmp1 instanceof ProgramStringBinaryExpression && ((ProgramStringBinaryExpression)tmp1).operator == StringBinaryOperator.CONCAT) {
				ProgramStringBinaryExpression t1 = (ProgramStringBinaryExpression)tmp1;
				if(t1.right instanceof ProgramIndexExpressionConstant && ((ProgramIndexExpressionConstant)t1.right).isStringConstant()) {
					String s1 = ((ProgramIndexExpressionConstant)tmp2).getStringConstant();
					String s2 = ((ProgramIndexExpressionConstant)t1.right).getStringConstant();	
					String res = "\"" + s2.substring(1,s2.length()-1) + s1.substring(1,s1.length()-1) + "\"";
					return new ProgramStringBinaryExpression(StringBinaryOperator.CONCAT, t1.left, new ProgramIndexExpressionConstant(res));
				}
			}
			return new ProgramStringBinaryExpression(operator, tmp1, tmp2);
		} else {
			return this;			
		}
	}

	@Override
	public String toString() {
		return left.toString() + " " + operator.toString() + " " + right.toString(); 
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramStringBinaryExpression) {
			return operator.equals(((ProgramStringBinaryExpression)o).operator) && 
					(left == ((ProgramStringBinaryExpression) o).left || left.structuralEquals(((ProgramStringBinaryExpression) o).left)) && 
					(right == ((ProgramStringBinaryExpression) o).right || right.structuralEquals(((ProgramStringBinaryExpression) o).right));
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
		return visitor.visitProgramStringBinaryExpression(this);
	}

	public ProgramExpression getLeft() {
		return left;
	}
	
	public ProgramExpression getRight() {
		return right;
	}

	public StringBinaryOperator getOperator() {
		return operator;
	}

	@Override
	protected int getHashCode() {
		return 23 * left.hashCode() + 7 * right.hashCode() + operator.hashCode();
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		List<ProgramExpressionProxy> res = this.left.getProxies();
		res.addAll(this.right.getProxies());
		return res;
	}

}
