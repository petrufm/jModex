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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;

public class ProgramGetObject extends ProgramExpression {

	private ProgramExpression objectId;
	private IClass theInstantietedClass;
	
	public ProgramGetObject(ProgramExpression uic, IClass theInstantietedClass) {
		this.objectId = uic;
		this.theInstantietedClass = theInstantietedClass;
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression tmp = objectId.substitute(sub, newExpression);
		if(tmp != objectId) {
			return new ProgramGetObject(tmp, theInstantietedClass);
		}
		return this;
	}

	@Override
	public String toString() {
		return "getObject(" + objectId + "," + theInstantietedClass.getName().toString() + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramGetObject) {
			return objectId == ((ProgramGetObject)o).objectId;
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
		return visitor.visitProgramGetObject(this);
	}

	@Override
	protected int getHashCode() {
		return 37 * objectId.hashCode() + theInstantietedClass.hashCode();
	}

	public ProgramExpression getObjectIDExpression() {
		return objectId;
	}

	public static List<IField> getApplicationObjectFields(IClass aClass) {
		Collection<IField> allInstanceFields = aClass.getAllInstanceFields();
		ArrayList<IField> res = new ArrayList<IField>();
		for(IField afield : allInstanceFields) {
			if(afield.getDeclaringClass().getClassLoader().equals(aClass.getClassLoader())) {
				res.add(afield);
			}
		}
		Collections.sort(res,new Comparator<IField>(){
			@Override
			public int compare(IField arg0, IField arg1) {
				String s0 = arg0.getDeclaringClass().getName().toString() + "." + arg0.getName().toString();
				String s1 = arg1.getDeclaringClass().getName().toString() + "." + arg1.getName().toString();
				return s0.compareTo(s1);
			}
		});
		return Collections.unmodifiableList(res);		
	}

	public List<IField> getApplicationObjectFields() {
		return getApplicationObjectFields(theInstantietedClass);
	}

	public IClass getDeclaredClass() {
		return theInstantietedClass;
	}

}
