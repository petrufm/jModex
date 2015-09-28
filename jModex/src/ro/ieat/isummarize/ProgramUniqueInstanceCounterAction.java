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

public class ProgramUniqueInstanceCounterAction extends ProgramExpression {

	public enum Action {INIT, INC};

	private ProgramExpression uic;
	private Action theAction;
	
	public ProgramUniqueInstanceCounterAction(Action act, ProgramExpression uic) {
		this.uic = uic;
		this.theAction = act;
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub,ProgramExpression newExpression) {
		if(theAction == Action.INIT) {
			return this;
		}
		ProgramExpression tmp = uic.substitute(sub, newExpression);
		if(tmp != uic) {
			return new ProgramUniqueInstanceCounterAction(theAction,tmp);
		}
		return this;
	}

	@Override
	public String toString() {
		if(theAction == Action.INIT) {
			return theAction.toString();
		}
		return theAction + "(" + uic + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramUniqueInstanceCounterAction) {
			if(theAction == Action.INIT) {
				return theAction == ((ProgramUniqueInstanceCounterAction)o).theAction;
			}
			return theAction == ((ProgramUniqueInstanceCounterAction)o).theAction 
					&& uic.structuralEquals(((ProgramUniqueInstanceCounterAction)o).uic);
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		if(theAction == Action.INIT) {
			return new HashSet<ProgramExpression>();
		}		
		return uic.getVariables();
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		if(theAction == Action.INIT) {
			return new ArrayList<ProgramExpressionProxy>();
		}		
		return uic.getProxies();
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramUniqueInstanceCounterAction(this);
	}

	@Override
	protected int getHashCode() {
		if(theAction == Action.INIT) {
			return 79 * theAction.hashCode();
		}		
		return 79 * theAction.hashCode() + uic.hashCode();
	}
	
	public Action getAction() {
		return theAction;
	}

	public ProgramExpression getCurrentCounter() {
		return uic;
	}
}
