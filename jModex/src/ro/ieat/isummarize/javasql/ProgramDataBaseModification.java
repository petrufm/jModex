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
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramSubstitutableVariable;

public class ProgramDataBaseModification extends ProgramExpression {

	public interface ProgramDataBaseModificationVisitor {
		public Object visitProgramDataBaseModification(ProgramDataBaseModification expr);
	}
	
	private SQLDataBaseModel dbBuilder;
	private List<ProgramExpression> arguments = new ArrayList<ProgramExpression>();
	
	ProgramDataBaseModification(SQLDataBaseModel dbBuilder, ProgramDataBase db, ProgramExpression arg) {
		arguments.add(arg);
		arguments.add(db);
		this.dbBuilder = dbBuilder;
	}

	private ProgramDataBaseModification(SQLDataBaseModel dbBuilder, List<ProgramExpression> args) {
		arguments.addAll(args);
		this.dbBuilder = dbBuilder;
	}

	public SQLDataBaseModel getModel() {
		return dbBuilder;
	}

	public ProgramExpression getQuery() {
		return arguments.get(0);
	}

	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ArrayList<ProgramExpression> tmp = null;
		for(int i = 0; i < arguments.size(); i++) {
			ProgramExpression anArg = arguments.get(i);
			ProgramExpression res = anArg.substitute(sub, newExpression);
			if(anArg != res) {
				if(tmp == null) {
					tmp = new ArrayList<ProgramExpression>(arguments);
				}
				tmp.remove(i);
				tmp.add(i,res);
			}
		}
		return tmp == null ? this : new ProgramDataBaseModification(dbBuilder, tmp);
	}

	@Override
	public String toString() {
		String res = "";
		for(int i = 0; i < arguments.size(); i++) {
			res += arguments.get(i);
			if(i != arguments.size()-1) {
				res += ",";
			}
		}
		return "ProgramDataBaseModification(" + res + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramDataBaseModification) {
			ProgramDataBaseModification second = (ProgramDataBaseModification) o;
			if(arguments.size() == second.arguments.size()) {
				for(int i = 0; i < arguments.size(); i++) {
					if(!arguments.get(i).structuralEquals(second.arguments.get(i))) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> res = new HashSet<ProgramExpression>();
		for(int i = 0; i < arguments.size(); i++) {
			res.addAll(arguments.get(i).getVariables());
		}
		return res;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramDataBaseModificationVisitor) {
			return ((ProgramDataBaseModificationVisitor) visitor).visitProgramDataBaseModification(this);
		}
		return null;
	}

	@Override
	protected int getHashCode() {
		int code = 0;
		for(int i = 0; i < arguments.size(); i++) {
			code += 37 * arguments.get(i).hashCode();
		}
		return code;
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		if(arguments.size() == 0) {
			return new ArrayList<ProgramExpressionProxy>();
		}
		List<ProgramExpressionProxy> res = this.arguments.get(0).getProxies();
		for(int i = 1; i < arguments.size(); i++) {
			res.addAll(this.arguments.get(i).getProxies());
		}
		return res;
	}

}
