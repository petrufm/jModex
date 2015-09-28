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
package ro.ieat.isummarize2aslanpp.interval;

import java.util.Set;

import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.javasql.ProgramSQLCursorAction;
import ro.ieat.isummarize2aslanpp.TranslatedExpression;

public class AvailableCursorFact extends Fact {

	private ProgramExpression pe;
	private TranslatedExpression te;
	private TranslatedExpression has_next_applied_on;
	
	public AvailableCursorFact(ProgramExpression pe, TranslatedExpression te) {
		this.pe = pe;
		this.te = te;
	}
	
	public ProgramExpression getProgramExpression() {
		return pe;	
	}
	
	public TranslatedExpression getTranslatedExpression() {
		return te;	
	}

	public void setHasNextAppliedOn(TranslatedExpression theExp) {
		has_next_applied_on = theExp;	
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(this);
	}
		
	@Override
	protected void propagatesFromGuardToUpdate(Set<Fact> result) {
		if(pe instanceof ProgramSQLCursorAction && ((ProgramSQLCursorAction) pe).getFunction() == ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_HAS_NEXT) {
			result.add(new AvailableCursorFact(((ProgramSQLCursorAction) pe).getArgument(),has_next_applied_on)); 
		} else {
			result.add(this);
		}
	}

}
