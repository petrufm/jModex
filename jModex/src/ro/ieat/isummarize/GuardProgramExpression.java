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


public class GuardProgramExpression extends GuardExpression {

	private ProgramExpression programCondition;

	public GuardProgramExpression(ProgramExpression programCondition) {
		this.programCondition = programCondition;
	}
	
	@Override
	public GuardExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression tmp1;
		tmp1 = programCondition.substitute(sub, newExpression);
		if(tmp1 != programCondition) {
			return new GuardProgramExpression(tmp1);
		} else {
			return this;
		}
	}

	@Override
	public String toString() {
		return programCondition + "";
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof GuardProgramExpression) {
			return (programCondition == ((GuardProgramExpression) o).programCondition || programCondition.structuralEquals(((GuardProgramExpression) o).programCondition));
		}
		return false;
	}

	@Override
	protected int getHashCode() {
		return 31 * programCondition.hashCode();
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		return programCondition.getVariables();
	}
	
	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitGuardProgramExpression(this);
	}

	public ProgramExpression getProgramCondition() {
		return programCondition;
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return programCondition.getProxies();
	}
	
}
