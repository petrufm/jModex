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
package ro.ieat.isummarize.jsptomcat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramSubstitutableVariable;


public class ProgramRequestParameter extends ProgramExpression {

	public interface ProgramRequestParameterVisitor {
		public Object visitProgramRequestParameter(ProgramRequestParameter expression);
	}

	private ProgramExpression theParam;
	
	public ProgramRequestParameter(ProgramExpression theParam) {
		this.theParam = theParam;
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression result = theParam.substitute(sub, newExpression);
		if(theParam == result) {
			return this;
		} else {
			return new ProgramRequestParameter(result);
		}
	}

	@Override
	public String toString() {
		return "RequestParameter(" + theParam + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramRequestParameter) {
			return theParam == ((ProgramRequestParameter) o).theParam || theParam.structuralEquals(((ProgramRequestParameter) o).theParam);
		}
		return false;
	}
	
	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		result.addAll(theParam.getVariables());
		return result;
	}
	
	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramRequestParameterVisitor) {
			return ((ProgramRequestParameterVisitor)visitor).visitProgramRequestParameter(this);
		}
		return null;
	}
	
	public ProgramExpression getParam() {
		return theParam;
	}

	@Override
	protected int getHashCode() {
		return 37 * theParam.hashCode();
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return theParam.getProxies();
	}

}
