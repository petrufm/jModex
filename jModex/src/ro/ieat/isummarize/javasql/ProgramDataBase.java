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
package ro.ieat.isummarize.javasql;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.PredicateAbstractionAbstractVariable;
import ro.ieat.isummarize.PredicateAbstractionAbstractization;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramSubstitutableVariable;
import ro.ieat.isummarize.TracedVariable;

public class ProgramDataBase extends ProgramSubstitutableVariable implements TracedVariable {

	public interface ProgramDataBaseVisitor {
		public Object visitProgramDataBase(ProgramDataBase db);
	}
	
	@Override
	public boolean isObservedGlobally() {
		return true;
	}

	@Override
	public TracedVariable substituteSubExpression(ProgramSubstitutableVariable toSub, ProgramExpression newExp) {
		return this;
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof ProgramDataBase;
	}

	@Override
	protected int getHashCode() {
		return 31;
	}
	
	@Override
	public String toString() {
		return "DB";
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		if(sub.equals(this)) {
			return newExpression;
		}
		return this;
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		return equals(o);
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> res = new HashSet<ProgramExpression>();
		res.add(this);
		return res;
	}

	@Override
	public Set<ProgramExpression> getSubVariables() {
		HashSet<ProgramExpression> res = new HashSet<ProgramExpression>();
		return res;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramDataBaseVisitor) {
			return ((ProgramDataBaseVisitor) visitor).visitProgramDataBase(this);
		}
		return null;
	}
	
	public ProgramExpression preparedForUpdateAssignment(ProgramExpression toAssign) {
		if(toAssign instanceof PredicateAbstractionAbstractization) {
			return ModelFactory.getInstance().createPredicateAbstractionConcretization(toAssign, ((PredicateAbstractionAbstractization) toAssign).getPredicate());
		}
		if(toAssign instanceof PredicateAbstractionAbstractVariable) {
			return ModelFactory.getInstance().createPredicateAbstractionConcretization(toAssign, ((PredicateAbstractionAbstractVariable) toAssign).getPredicate());
		}	
		return toAssign;
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return new ArrayList<ProgramExpressionProxy>();
	}
	
}
