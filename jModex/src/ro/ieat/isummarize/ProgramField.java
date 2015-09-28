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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IField;

public class ProgramField extends ProgramSubstitutableVariable implements TracedVariable {

	private ProgramExpression theObject;
	private IField theField;
	
	public ProgramField(IField theField) {
		this.theField = theField;
		this.theObject = null;
	}

	public ProgramField(ProgramExpression theObject, IField theField) {
		this.theField = theField;
		this.theObject = theObject;
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		if(this.equals(sub)) {
			return newExpression;
		}
		if(!theField.isStatic()) {
			ProgramExpression tmp = theObject.substitute(sub, newExpression);
			if(tmp != theObject) {
				return new ProgramField(tmp,theField);
			}
		}
		return this;
	}

	@Override
	public String toString() {
		if(theField.isStatic()) {
			return theField.getDeclaringClass().getName() + "." + theField.getName().toString();
		} else {
			return "Field(" + theField.getName().toString() + "," + theObject + ")"; 
		}
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		return this.equals(o);
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		Set<ProgramExpression> res = new HashSet<ProgramExpression>();
		res.add(this);
		if(!theField.isStatic()) {
			res.addAll(theObject.getVariables());
		}
		return res;
	}

	@Override
	public Set<ProgramExpression> getSubVariables() {
		Set<ProgramExpression> res = new HashSet<ProgramExpression>();
		if(!theField.isStatic()) {
			res.addAll(theObject.getVariables());
		}
		return res;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof ProgramField) {
			if(theField.isStatic()) {
				return ((ProgramField) o).theField.equals(theField);
			} else if(!((ProgramField) o).theField.isStatic()) {
				return ((ProgramField) o).theObject.equals(theObject) && ((ProgramField) o).theField.equals(theField);
			}
		}
		return false;		
	}
	
	@Override
	protected int getHashCode() {
		return 32 * theField.hashCode();
	}

	@Override
	public boolean isObservedGlobally() {
		return true;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramField(this);
	}

	@Override
	public TracedVariable substituteSubExpression(ProgramSubstitutableVariable toSub, ProgramExpression newExp) {
		if(!theField.isStatic()) {
			ProgramExpression tmp = theObject.substitute(toSub, newExp);
			if(tmp != theObject) {
				return new ProgramField(tmp,theField);
			}
		}
		return this;
	}
	
	public boolean isStatic() {
		return theField.isStatic();
	}

	public String getFieldName() {
		return theField.getName().toString();
	}
	
	public String getDeclaringTypeName() {
		return Utils.getFullQualifiedTypeNameFromBinaryName(theField.getDeclaringClass().getName().toString());
	}
	
	public String getDeclaredTypeName() {
		return Utils.getFullQualifiedTypeNameFromBinaryName(theField.getFieldTypeReference().getName().toString());
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
		ArrayList<ProgramExpressionProxy> tmp = new ArrayList<ProgramExpressionProxy>();
		if(!theField.isStatic()) {
			tmp.addAll(theObject.getProxies());
		}
		return tmp;
	}
	
	public ProgramExpression getObjectExpression() {
		return theObject;
	}

	public IField getField() {
		return theField;
	}

}
