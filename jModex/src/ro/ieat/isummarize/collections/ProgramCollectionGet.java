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
package ro.ieat.isummarize.collections;

import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramSubstitutableVariable;

public class ProgramCollectionGet extends ProgramExpression {

	public interface ProgramCollectionGetVisitor {
		public Object visitProgramCollectionGet(ProgramCollectionGet cg);
	}

	private ProgramExpression objectId;
	
	public ProgramCollectionGet(ProgramExpression uic) {
		this.objectId = uic;
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression tmp = objectId.substitute(sub, newExpression);
		if(tmp != objectId) {
			return new ProgramCollectionGet(tmp);
		}
		return this;
	}

	@Override
	public String toString() {
		return "getCollection(" + objectId + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramCollectionGet) {
			return objectId == ((ProgramCollectionGet)o).objectId;
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		return objectId.getVariables();
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return objectId.getProxies();
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramCollectionGetVisitor) {
			return ((ProgramCollectionGetVisitor) visitor).visitProgramCollectionGet(this);
		} else {
			return null;
		}
	}

	@Override
	protected int getHashCode() {
		return 37 * objectId.hashCode();
	}

	public ProgramExpression getCollectionIdExpression() {
		return objectId;
	}
	
}