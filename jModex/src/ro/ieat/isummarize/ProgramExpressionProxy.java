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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ibm.wala.ssa.SSAInvokeInstruction;

public class ProgramExpressionProxy extends ProgramSubstitutableVariable implements Cloneable {

	private SSAInvokeInstruction theCall;
	private ProgramExpression[] arguments;
	
	SSAInvokeInstruction getTheCall() {
		return theCall;
	}
	
	ProgramExpression[] getArguments() {
		return arguments;
	}
	
	public ProgramExpressionProxy(SSAInvokeInstruction theCall,ProgramExpression ... arguments) {
		this.theCall = theCall;
		this.arguments = arguments;
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		if(this.equals(sub)) {
			return newExpression;
		}
		ProgramExpression[] tmp = new ProgramExpression[arguments.length];
		boolean changed = false;
		int i = 0;
		for(ProgramExpression pe : arguments) {
			tmp[i] = arguments[i].substitute(sub, newExpression);
			if(tmp[i] != pe) {
				changed = true;
			}
			i++;
		}
		if(changed) {
			try {
				ProgramExpressionProxy aClone = (ProgramExpressionProxy) this.clone();
				aClone.arguments = tmp;
				return aClone;
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		return this;
	}

	@Override
	public String toString() {
		return "Proxy" + (theCall.getDeclaredTarget() + ":" + Arrays.toString(arguments));
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramExpressionProxy) {
			ProgramExpressionProxy cmp = (ProgramExpressionProxy)o;
			if(arguments.length != cmp.arguments.length) {
				return false;
			}
			for(int i = 0; i < arguments.length; i++) {
				if(!arguments[i].structuralEquals(cmp.arguments[i])) {
					return false;
				}
			}
			return theCall.equals(cmp.theCall);
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> res = new HashSet<ProgramExpression>();
		for(int i = 0; i < arguments.length; i++) {
			res.addAll(arguments[i].getVariables());
		}		
		return res;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramExpressionProxy(this);
	}

	@Override
	protected int getHashCode() {
		int res = theCall.hashCode();
		for(int i = 0; i < arguments.length; i++) {
			res += 37 * arguments[i].hashCode();
		}		
		return res;
	}

	public ProgramExpression resolve() {
		return null;
	}

	@Override
	public ProgramExpression preparedForUpdateAssignment(ProgramExpression toAssign) {
		return toAssign;
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		List<ProgramExpressionProxy> res = this.arguments[0].getProxies();
		for(int i = 1; i < arguments.length; i++) {
			res.addAll(this.arguments[i].getProxies());
		}
		res.add(this);
		return res;
	}
	
}
