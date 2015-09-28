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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator;

public class ProgramFunction extends ProgramExpression implements Cloneable {

	protected List<ProgramExpression> args;
	protected IMethod theMethod;
	
	public ProgramFunction(IMethod theMethod, ProgramExpression[] args) {
		this.args = new ArrayList<ProgramExpression>();
		this.theMethod = theMethod;
		for(ProgramExpression exp : args) {
			this.args.add(exp);
		}
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		boolean changed = false;
		List<ProgramExpression> newArgs = new ArrayList<ProgramExpression>();
		for(ProgramExpression exp : args) {
			ProgramExpression tmp = exp.substitute(sub, newExpression);
			newArgs.add(tmp);
			if(tmp != exp) {
				changed = true;
			}
		}
		if(changed) {
			ProgramFunction clone;
			try {
				clone = (ProgramFunction) this.clone();
				clone.args = new ArrayList<ProgramExpression>(newArgs);
				return clone;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return null;
			} 
		} else {
			return this;
		}
	}

	@Override
	public String toString() {
		String res =  "ProgramFunction(" + theMethod.getSignature();
		for(ProgramExpression exp: args) {
			res+=";" + exp.toString();
		}
		res+=";)";
		return res;
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramFunction) {
			ProgramFunction oo = (ProgramFunction)o;
			return this.args.equals(oo.args) && theMethod.getDeclaringClass().equals(oo.theMethod.getDeclaringClass()) && theMethod.getSelector().equals(oo.theMethod.getSelector());
		} else {
			return false;
		}
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> res = new HashSet<ProgramExpression>();
		for(ProgramExpression exp : args) {
			res.addAll(exp.getVariables());
		}
		return res;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramFunction(this);
	}
	
	public List<ProgramExpression> getArguments() {
		return Collections.unmodifiableList(args);
	}
		
	public String getFunctionName() {
		return theMethod.getName().toString();
	}
	
	public String getDeclaringTypeName() {
		return Utils.getFullQualifiedTypeNameFromBinaryName(theMethod.getDeclaringClass().getName().toString());		
	}
	
	public String getReturnTypeName() {
		return Utils.getFullQualifiedTypeNameFromBinaryName(theMethod.getReturnType().getName().toString());
	}
	
	public String getFunctionSignature() {
		return Utils.getFullQualifiedSignaruteFromBinarySignature(theMethod.getSignature());
	}
	
	@Override
	protected int getHashCode() {
		return 23 * args.hashCode() + 7 * theMethod.hashCode();
	}

	public TernaryResult tryEvaluateComparison(IOperator operator, ProgramExpression exp) {
		return TernaryResult.DONOTKNOW;
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		if(args.size() == 0) {
			return new ArrayList<ProgramExpressionProxy>();
		}
		List<ProgramExpressionProxy> res = this.args.get(0).getProxies();
		for(int i = 1; i < args.size(); i++) {
			res.addAll(this.args.get(i).getProxies());
		}
		return res;
	}
	
	public IMethod getMethod(){
		return this.theMethod;
	}

}
