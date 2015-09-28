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


import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator;

public class ProgramRelationalExpression extends ProgramExpression {

	private ProgramExpression left;
	private ProgramExpression right;
	private IConditionalBranchInstruction.IOperator operator;
	private Number constant;

	public ProgramRelationalExpression(IConditionalBranchInstruction.IOperator operator, ProgramExpression left, ProgramExpression right) {
		this.left = left;
		this.right = right;
		this.operator = operator;
	}

	public ProgramRelationalExpression(ProgramExpression left, Number constant) {
		this.left = left;
		this.operator = IConditionalBranchInstruction.Operator.EQ;
		this.constant = constant;
	}
	
	public ProgramRelationalExpression(IConditionalBranchInstruction.IOperator operator, ProgramExpression left, Number constant) {
		this.left = left;
		this.operator = operator;
		this.constant = constant;
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression tmp1, tmp2 = null;
		
		//Eliminate comparison instruction from the bytecode level
		if(newExpression instanceof ProgramRelationalExpression && left instanceof ProgramIndexExpressionVariable && left.structuralEquals(sub)) {
			if(right instanceof ProgramIndexExpressionConstant && ((ProgramIndexExpressionConstant)right).isIntConstant()) {
				int comparingConstant = ((ProgramIndexExpressionConstant)right).getIntConstant();
				ProgramRelationalExpression theNewExpression = (ProgramRelationalExpression) newExpression;
				//compare(a,b) is (1 <-> a > b), (0 <-> a = b), (-1 <-> a < b) 
				if(comparingConstant == 0) {
					if(operator.equals(Operator.LE)) {
						//compare(a,b) <= 0
						return new ProgramRelationalExpression(Operator.LE, theNewExpression.left, theNewExpression.right);
					}
					if(operator.equals(Operator.NE)) {
						//compare(a,b) != 0
						return new ProgramRelationalExpression(Operator.NE, theNewExpression.left, theNewExpression.right);
					}
					if(operator.equals(Operator.EQ)) {
						//compare(a,b) = 0
						return new ProgramRelationalExpression(Operator.EQ, theNewExpression.left, theNewExpression.right);
					}					
				}
				throw new RuntimeException("Cannot eliminate comparison instruction: do not know how to convert operator " + operator + " with constant " + comparingConstant);
			}
		}
		
		tmp1 = left.substitute(sub, newExpression);
		if(right != null) {
			tmp2 = right.substitute(sub, newExpression);
		}
		if(tmp1 != left || (right != null && tmp2 != right)) {
			if(right != null) {
				return new ProgramRelationalExpression(operator, tmp1, tmp2);
			} else {
				return new ProgramRelationalExpression(tmp1, constant);
			}
		} else {
			return this;
		}
	}
	
	@Override
	public String toString() {
		return left.toString() 
				+ " " + operator + " "
				+ (right != null ? right.toString() : constant);
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramRelationalExpression) {
			if(right != null) {
				return operator.equals(((ProgramRelationalExpression)o).operator) &&
					(left == ((ProgramRelationalExpression) o).left || left.structuralEquals(((ProgramRelationalExpression) o).left)) && 
					(right == ((ProgramRelationalExpression) o).right || right.structuralEquals(((ProgramRelationalExpression) o).right));
			} else {
				return operator.equals(((ProgramRelationalExpression)o).operator) &&
						(left == ((ProgramRelationalExpression) o).left || left.structuralEquals(((ProgramRelationalExpression) o).left)) && 
						constant.equals(((ProgramRelationalExpression) o).constant);				
			}
		}
		return false;
	}
	
	@Override
	public Set<ProgramExpression> getVariables() {
		Set<ProgramExpression> leaves = left.getVariables();
		if(right != null) {
			leaves.addAll(right.getVariables());
		}
		return leaves;
	}
	
	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramRelationalExpression(this);
	}

	public ProgramExpression getLeft() {
		return left;
	}
	
	public ProgramExpression getRight() {
		return right;
	}

	public Number getConstant() {
		return constant;
	}

	public IConditionalBranchInstruction.IOperator getOperator() {
		return operator;
	}
	
	@Override
	protected int getHashCode() {
		return 23 * left.hashCode() + 17 * operator.hashCode() + (right == null ? constant.hashCode() : right.hashCode());
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		List<ProgramExpressionProxy> res = this.left.getProxies();
		if(right!=null) {
			res.addAll(this.right.getProxies());
		}
		return res;
	}

}