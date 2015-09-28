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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramSubstitutableVariable;

public class ProgramDbRValue extends ProgramExpression {

	public interface ProgramDbRValueVisitor {
		public Object visitProgramDbRValue(ProgramDbRValue expression);
	}

	private SQLDataBaseModel dbBuilder;
	private ProgramExpression column, cursor;
	
	public ProgramDbRValue(SQLDataBaseModel dbBuilder, ProgramExpression column, ProgramExpression cursor) {
		this.column = column;
		this.cursor = cursor;
		this.dbBuilder = dbBuilder;
	}
	
	public ProgramExpression getColumnAccessExpression() {
		return column;
	}
	
	public ProgramExpression getCursorExpression() {
		return cursor;
	}
	
	public SQLDataBaseModel getModel() {
		return dbBuilder;
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		ProgramExpression resultColumn = column.substitute(sub, newExpression);
		ProgramExpression resultQuery = cursor.substitute(sub, newExpression);
		if(column == resultColumn && cursor == resultQuery) {
			return this;
		} else
			return new ProgramDbRValue(dbBuilder, resultColumn, resultQuery);
	}

	@Override
	public String toString() {
		return "DbGetValue(Column " + column + " of Query " + cursor + ")";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramDbRValue) {
			return (column == ((ProgramDbRValue)o).column ||  column.structuralEquals(((ProgramDbRValue)o).column)) && 
				   (cursor == ((ProgramDbRValue)o).cursor || cursor.structuralEquals(((ProgramDbRValue)o).cursor));
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		result.addAll(column.getVariables());
		result.addAll(cursor.getVariables());
		return result;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		if(visitor instanceof ProgramDbRValueVisitor) {
			return ((ProgramDbRValueVisitor)visitor).visitProgramDbRValue(this);
		} else {
			return null;
		}
	}

	@Override
	protected int getHashCode() {
		return 37 * column.hashCode() + cursor.hashCode();
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		List<ProgramExpressionProxy> res = column.getProxies();
		res.addAll(cursor.getProxies());
		return res;
	}
	

}
