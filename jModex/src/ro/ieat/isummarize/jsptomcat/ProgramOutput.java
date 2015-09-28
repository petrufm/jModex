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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.PredicateAbstractionAbstractVariable;
import ro.ieat.isummarize.PredicateAbstractionAbstractization;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramIndexExpression;
import ro.ieat.isummarize.ProgramSubstitutableVariable;
import ro.ieat.isummarize.TracedVariable;

public class ProgramOutput extends ProgramSubstitutableVariable implements TracedVariable  {

	public interface ProgramOutputVisitor {
		public Object visitProgramOutput(ProgramOutput expression);
	}
	
	private List<ProgramExpression> uses = new ArrayList<ProgramExpression>();
	
	public ProgramOutput(ProgramIndexExpression use) {
		uses.add(use);
	}

	public ProgramOutput() {}

	public ProgramOutput(List<ProgramExpression> l) {
		uses.addAll(l);
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		List<ProgramExpression> newUses = new ArrayList<ProgramExpression>();
		if(this.equals(sub)) {
			if(newExpression instanceof ProgramOutput) {
				newUses.addAll(((ProgramOutput) newExpression).uses);
			} else {
				newUses.add(newExpression);				
			}
			newUses.addAll(uses);
			return new ProgramOutput(newUses);
		}	
		for(ProgramExpression pie : uses) {
			newUses.add(pie.substitute(sub, newExpression));
		}
		return new ProgramOutput(newUses);
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof ProgramOutput) {
			return true;
		}
		return false;
	}
	
	@Override
	protected int getHashCode() {
		return 17;
	}

	@Override
	public String toString() {
		if(uses.isEmpty())
			return "ProgramOutput";
		else
			return uses.toString().replace("\n", "\\n");
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramOutput) {
			if(this.uses.size() == ((ProgramOutput)o).uses.size()) {
				for(int i = 0; i < this.uses.size(); i++) {
					if(!(this.uses.get(i) == ((ProgramOutput)o).uses.get(i) || this.uses.get(i).structuralEquals(((ProgramOutput)o).uses.get(i)))) {
						return false;
					}
				}
			} else {
				return false;
			}
		}
		return true;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		for(ProgramExpression aUse : uses) {
			result.addAll(aUse.getVariables());
		}
		result.add(new ProgramOutput());
		return result;
	}

	@Override
	public Set<ProgramExpression> getSubVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		for(ProgramExpression aUse : uses) {
			result.addAll(aUse.getVariables());
		}
		return result;
	}

	@Override
	public boolean isObservedGlobally() {
		return true;
	}
	
	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramOutputVisitor) {
			return ((ProgramOutputVisitor)visitor).visitProgramOutput(this);
		}
		return null;
	}

	@Override
	public TracedVariable substituteSubExpression(ProgramSubstitutableVariable toSub, ProgramExpression newExp) {
		return this;
	}
	
	public List<ProgramExpression> getOutputExpressions() {
		return Collections.unmodifiableList(uses);
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
		if(uses.size() == 0) {
			return new ArrayList<ProgramExpressionProxy>();
		}
		List<ProgramExpressionProxy> res = this.uses.get(0).getProxies();
		for(int i = 1; i < uses.size(); i++) {
			res.addAll(this.uses.get(i).getProxies());
		}
		return res;
	}
	
}
