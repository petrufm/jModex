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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProgramConversionExpression extends ProgramExpression {

	public enum ConversionOperation {STRING2INT, INT2STRING, CAST2INT};
	
	private ProgramExpression convertedExpression;
	private ConversionOperation op;
	
	public ProgramConversionExpression(ConversionOperation op, ProgramExpression converted) {
		this.convertedExpression = converted;
		this.op = op;
	}
	
	public ProgramExpression getConvertedExpression() {
		return convertedExpression;
	}
	
	public ConversionOperation getOperation() {
		return op;
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression pe = convertedExpression.substitute(sub, newExpression);
		if(pe != convertedExpression) {
			if(pe instanceof ProgramConversionExpression) {
				if(this.op == ConversionOperation.INT2STRING && ((ProgramConversionExpression)pe).op == ConversionOperation.STRING2INT) {
					return ((ProgramConversionExpression)pe).convertedExpression;
				}
				if(this.op == ConversionOperation.STRING2INT && ((ProgramConversionExpression)pe).op == ConversionOperation.INT2STRING) {
					return ((ProgramConversionExpression)pe).convertedExpression;
				}
			}
			return new ProgramConversionExpression(op,pe);
		}
		return this;
	}

	@Override
	public String toString() {
		return op.name() + "(" + convertedExpression + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramConversionExpression) {
			return op == ((ProgramConversionExpression)o).op && convertedExpression.structuralEquals(((ProgramConversionExpression)o).convertedExpression);
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> res = new HashSet<ProgramExpression>();
		res.addAll(convertedExpression.getVariables());
		return res;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramConversionExpression(this);
	}

	@Override
	protected int getHashCode() {
		return 47 * op.hashCode() + convertedExpression.hashCode();
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return convertedExpression.getProxies();
	}

}
