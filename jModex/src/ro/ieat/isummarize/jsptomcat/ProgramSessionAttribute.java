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
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.PredicateAbstractionAbstractVariable;
import ro.ieat.isummarize.PredicateAbstractionAbstractization;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramSubstitutableVariable;
import ro.ieat.isummarize.TracedVariable;

public class ProgramSessionAttribute extends ProgramSubstitutableVariable implements TracedVariable {

	public interface ProgramSessionAttributeVisitor {
		public Object visitProgramSessionAttribute(ProgramSessionAttribute expression);
	}

	private ProgramExpression theAttribute;
	
	public ProgramSessionAttribute(ProgramExpression theAttribute) {
		this.theAttribute = theAttribute; 
	}

	private ProgramSessionAttribute(ProgramExpression theAttribute, Map<ProgramSessionAttribute,Set<TypeAbstraction>> typeCache, Set<TypeAbstraction> propagated, IClassHierarchy ch) {
		this.theAttribute = theAttribute;
		this.ch = ch;
		this.cache = typeCache;
		if(propagated != null) {
			for(TypeAbstraction ta : propagated) {
				this.recordType(typeCache, ta);
			}
		}
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression result = theAttribute.substitute(sub, newExpression);
		if(result == theAttribute) {
			if(this.equals(sub)) {
				return newExpression;
			} else {
				return this;
			}
		} else {
			return new ProgramSessionAttribute(result,cache,cache.get(this),ch);
		}
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof ProgramSessionAttribute) {
			return theAttribute == ((ProgramSessionAttribute) o).theAttribute || theAttribute.structuralEquals(((ProgramSessionAttribute) o).theAttribute);
		}
		return false;
	}
	
	@Override
	protected int getHashCode() {
		return theAttribute.hashCode();
	}

	@Override
	public String toString() {
		return "SessionAttribute(" + theAttribute + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		return this.equals(o);
	}
	
	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		result.add(this);
		result.addAll(theAttribute.getVariables());
		return result;
	}

	@Override
	public Set<ProgramExpression> getSubVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		result.addAll(theAttribute.getVariables());
		return result;
	}

	@Override
	public boolean isObservedGlobally() {
		return true;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramSessionAttributeVisitor) {
			return ((ProgramSessionAttributeVisitor)visitor).visitProgramSessionAttribute(this);
		}
		return null;
	}
	
	public ProgramExpression getAttribute() {
		return theAttribute;
	}

	@Override
	public TracedVariable substituteSubExpression(ProgramSubstitutableVariable toSub, ProgramExpression newExp) {
		ProgramExpression result = theAttribute.substitute(toSub, newExp);
		if(result == theAttribute) {
			return this;
		} else {
			return new ProgramSessionAttribute(result,cache,cache.get(this),ch);
		}
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
		return theAttribute.getProxies();
	}
	
	private Map<ProgramSessionAttribute,Set<TypeAbstraction>> cache;
	public void recordType(Map<ProgramSessionAttribute,Set<TypeAbstraction>> cache, TypeAbstraction ta) {
		this.cache = cache;
		if(ta != null) {
			Set<TypeAbstraction> alreadyRecorded = cache.get(this);
			if(alreadyRecorded == null) {
				HashSet<TypeAbstraction> typeSet = new HashSet<TypeAbstraction>();
				typeSet.add(ta);
				cache.put(this,typeSet);
			} else {
				alreadyRecorded.add(ta);
			}
		}
	}
	
	public Set<TypeReference> getPossibleTypes() {
		HashSet<TypeReference> res = new HashSet<TypeReference>();
		Set<TypeAbstraction> allTypes = cache.get(this);
		for(TypeAbstraction ta : allTypes) {
			res.add(ta.getTypeReference());
		}
		return res;
	}
	
	public Set<IClass> getPossibleClasses() {
		HashSet<IClass> res = new HashSet<IClass>();
		Set<TypeAbstraction> allTypes = cache.get(this);
		for(TypeAbstraction ta : allTypes) {
			res.add(ch.lookupClass(ta.getTypeReference()));
		}
		return res;
	}

	private IClassHierarchy ch;
	public void setClassHierarchy(IClassHierarchy ch) {
		this.ch = ch;
	}
	
}
