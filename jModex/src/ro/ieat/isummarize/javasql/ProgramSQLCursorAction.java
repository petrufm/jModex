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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramSubstitutableVariable;

public class ProgramSQLCursorAction extends ProgramSubstitutableVariable {
	
	public interface ProgramSQLCursorActionVisitor {
		public Object visitProgramSQLCursorAction(ProgramSQLCursorAction expression);
	}

	public enum SQL_CURSOR_ACTION {
		CURSOR_CREATE, CURSOR_NEXT, CURSOR_HAS_NEXT
	};
	
	private SQL_CURSOR_ACTION function;
	private ProgramExpression argument;
	private SQLDataBaseModel dbBuilder;

	public ProgramSQLCursorAction(SQLDataBaseModel dbBuilder, SQL_CURSOR_ACTION function, ProgramExpression cursor) {
		this.function = function;
		this.argument = cursor;
		this.dbBuilder = dbBuilder;
	}
	
	public SQL_CURSOR_ACTION getFunction() {
		return function;
	}

	public ProgramExpression getArgument() {
		return argument;
	}

	public SQLDataBaseModel getModel() {
		return dbBuilder;
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		if(this.structuralEquals(sub)) {
			ProgramSQLCursorAction newExp = (ProgramSQLCursorAction) newExpression;
			nextArg:for(int i = 0; i < this.argsPositions.size(); i++) {
				for(int j = 0; j < newExp.argsPositions.size(); j++) {
					if(newExp.argsPositions.get(j) == argsPositions.get(i)) continue nextArg;
				}
				newExp.argsPositions.add(i,this.argsPositions.get(i));
				newExp.argsValues.add(i,this.argsValues.get(i));
			}
			return newExp;
		} else {
			ProgramExpression result = argument.substitute(sub, newExpression);
			boolean changed = false;
			List<ProgramExpression> tmpArgsPositions = new ArrayList<ProgramExpression>();
			List<ProgramExpression> tmpArgsValues = new ArrayList<ProgramExpression>();
			for(int i = 0; i < this.argsPositions.size(); i++) {
				ProgramExpression tmp = this.argsPositions.get(i).substitute(sub, newExpression);
				if(tmp != this.argsPositions.get(i)) {
					changed = true;
				}
				tmpArgsPositions.add(tmp);
				tmp = this.argsValues.get(i).substitute(sub, newExpression);
				if(tmp != this.argsValues.get(i)) {
					changed = true;
				}
				tmpArgsValues.add(tmp);
			}
			if(argument == result && !changed) {
				return this;
			} else {
				ProgramSQLCursorAction theRet = new ProgramSQLCursorAction(dbBuilder, function, result);
				theRet.argsPositions = tmpArgsPositions;
				theRet.argsValues = tmpArgsValues;
				return theRet;
			}
		}
	}

	@Override
	public String toString() {
		String arguments = "";
		for(int i = 0; i < argsPositions.size(); i++) {
			arguments += "[" + argsPositions.get(i) + "=" + argsValues.get(i) + "]";
		}
		return function + "(" + argument + ")" + arguments;
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramSQLCursorAction) {
			ProgramSQLCursorAction oAction = (ProgramSQLCursorAction) o;
			return this.function == oAction.function && this.argument.structuralEquals(oAction.argument);
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		result.addAll(argument.getVariables());
		return result;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramSQLCursorActionVisitor) {
			return ((ProgramSQLCursorActionVisitor) visitor).visitProgramSQLCursorAction(this);
		}
		return null;
	}

	@Override
	protected int getHashCode() {
		return 37 * function.hashCode() + argument.hashCode();
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return this.argument.getProxies();
	}

	@Override
	public ProgramExpression preparedForUpdateAssignment(ProgramExpression toAssign) {
		return toAssign;
	}

	private List<ProgramExpression> argsPositions = new ArrayList<ProgramExpression>();
	private List<ProgramExpression> argsValues = new ArrayList<ProgramExpression>();
	public void addCursorCreationParameter(ProgramExpression position, ProgramExpression value) {
		argsPositions.add(position);
		argsValues.add(value);
	}

	public List<ProgramExpression> getArgsPositions() {
		return Collections.unmodifiableList(argsPositions);
	}
	
	public List<ProgramExpression> getArgsValues() {
		return Collections.unmodifiableList(argsValues);		
	}
}
