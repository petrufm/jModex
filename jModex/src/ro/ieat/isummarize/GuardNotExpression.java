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


public class GuardNotExpression extends GuardExpression {

	private GuardExpression operand;
	
	public GuardNotExpression(GuardExpression operand) {
		this.operand = operand;
	}
	
	@Override
	public GuardExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		GuardExpression tmp1;
		tmp1 = operand.substitute(sub, newExpression);
		if(tmp1 != operand) {
			return new GuardNotExpression(tmp1);
		} else {
			return this;
		}
	}

	@Override
	public String toString() {
		return "NOT(" + operand + ")";
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof GuardNotExpression) {
			return operand == ((GuardNotExpression) o).operand  || operand.equals(((GuardNotExpression) o).operand);
		}
		return false;
	}

	@Override
	protected int getHashCode() {
		return 31 * operand.hashCode();
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		return operand.getVariables();
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitGuardNotExpression(this);
	}
	
	public GuardExpression getOperand() {
		return operand;
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return operand.getProxies();
	}

}
