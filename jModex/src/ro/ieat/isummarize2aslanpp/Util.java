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
package ro.ieat.isummarize2aslanpp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.GuardAndExpression;
import ro.ieat.isummarize.GuardExpression;
import ro.ieat.isummarize.GuardNotExpression;
import ro.ieat.isummarize.GuardOrExpression;
import ro.ieat.isummarize.GuardProgramExpression;
import ro.ieat.isummarize.GuardTrueExpression;
import ro.ieat.isummarize.PredicateAbstractionAbstractization;
import ro.ieat.isummarize.PredicateAbstractionConcretization;
import ro.ieat.isummarize.ProgramBinaryExpression;
import ro.ieat.isummarize.ProgramConversionExpression;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramExpressionProxy;
import ro.ieat.isummarize.ProgramField;
import ro.ieat.isummarize.ProgramFunction;
import ro.ieat.isummarize.ProgramIndexExpression;
import ro.ieat.isummarize.ProgramPhiExpression;
import ro.ieat.isummarize.ProgramRelationalExpression;
import ro.ieat.isummarize.ProgramReturnVariable;
import ro.ieat.isummarize.ProgramStringBinaryExpression;
import ro.ieat.isummarize.ProgramStringComparison;
import ro.ieat.isummarize.javasql.ProgramDataBase;
import ro.ieat.isummarize.javasql.ProgramDataBaseModification;
import ro.ieat.isummarize.javasql.ProgramDbRValue;
import ro.ieat.isummarize.javasql.ProgramSQLCursorAction;
import ro.ieat.isummarize.javasql.ProgramDataBase.ProgramDataBaseVisitor;
import ro.ieat.isummarize.javasql.ProgramDataBaseModification.ProgramDataBaseModificationVisitor;
import ro.ieat.isummarize.javasql.ProgramDbRValue.ProgramDbRValueVisitor;
import ro.ieat.isummarize.javasql.ProgramSQLCursorAction.ProgramSQLCursorActionVisitor;
import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute;
import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter.ProgramRequestParameterVisitor;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute.ProgramSessionAttributeVisitor;

public class Util {

	public static List<ProgramExpression> collectCursorCreation(GuardExpression guard) {
		return cursorCreationDetector.collectCursorCreation(guard);
	}
	private static CollectCursorCreation cursorCreationDetector = new CollectCursorCreation();	
	private static class CollectCursorCreation extends ExpressionAcyclicVisitor implements ProgramRequestParameterVisitor, ProgramSessionAttributeVisitor, ProgramDbRValueVisitor, ProgramSQLCursorActionVisitor, ProgramDataBaseVisitor, ProgramDataBaseModificationVisitor {
		public List<ProgramExpression> collectCursorCreation(GuardExpression guardExpression) {
			return (List<ProgramExpression>) guardExpression.accept(this);
		}
		@Override
		public Object visitGuardAndExpression(GuardAndExpression expression) { 
			List<ProgramExpression> res = (List<ProgramExpression>) expression.getLeft().accept(this);
			res.addAll((List) expression.getRight().accept(this));		
			return res;
		}
		@Override
		public Object visitGuardOrExpression(GuardOrExpression expression) { 
			List<ProgramExpression> res = (List<ProgramExpression>) expression.getLeft().accept(this);
			res.addAll((List) expression.getRight().accept(this));		
			return res;
		}
		@Override
		public Object visitGuardNotExpression(GuardNotExpression expression) {
			return expression.getOperand().accept(this);	
		}
		@Override
		public Object visitGuardProgramExpression(GuardProgramExpression expression) {
			return expression.getProgramCondition().accept(this);
		}
		@Override
		public Object visitGuardTrueExpression(GuardTrueExpression expression) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramBinaryExpression(ProgramBinaryExpression expression) {
			List<ProgramExpression> res = (List<ProgramExpression>) expression.getLeft().accept(this);
			res.addAll((List) expression.getRight().accept(this));		
			return res;
		}
		@Override
		public Object visitProgramRelationalExpression(ProgramRelationalExpression expression) {
			List<ProgramExpression> res = (List<ProgramExpression>) expression.getLeft().accept(this);
			res.addAll((List) expression.getRight().accept(this));		
			return res;
		}
		@Override
		public Object visitProgramStringComparison(ProgramStringComparison expression) {
			List<ProgramExpression> res = (List<ProgramExpression>) expression.getLeft().accept(this);
			res.addAll((List) expression.getRight().accept(this));		
			return res;
		}
		@Override
		public Object visitProgramIndexExpression(ProgramIndexExpression expression) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramField(ProgramField expression) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramPhiExpression(ProgramPhiExpression epression) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramExpressionProxy(ProgramExpressionProxy expression) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramReturnVariable(ProgramReturnVariable expression) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramStringBinaryExpression(ProgramStringBinaryExpression expression) {
			List<ProgramExpression> res = (List<ProgramExpression>) expression.getLeft().accept(this);
			res.addAll((List) expression.getRight().accept(this));		
			return res;
		}
		@Override
		public Object visitProgramFunction(ProgramFunction expression) {
			ArrayList<ProgramExpression> res = new ArrayList<ProgramExpression>();
			for(ProgramExpression anArg : expression.getArguments()) {
				res.addAll((Collection<ProgramExpression>) anArg.accept(this));
			}
			return res;
		}
		@Override
		public Object visitPredicateAbstractization(PredicateAbstractionAbstractization expression) {
			return expression.getExpression().accept(this);
		}
		@Override
		public Object visitPredicateConcretization(PredicateAbstractionConcretization expression) {
			return expression.getExpression().accept(this);
		}
		@Override
		public Object visitProgramConversionExpression(ProgramConversionExpression expression) {
			return expression.getConvertedExpression().accept(this);
		}
		@Override
		public Object visitProgramDataBaseModification(ProgramDataBaseModification expr) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramDataBase(ProgramDataBase db) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramSQLCursorAction(ProgramSQLCursorAction expression) {
			if(expression.getFunction() == ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE) {
				ArrayList<ProgramExpression> res = new ArrayList<ProgramExpression>();
				res.add(expression);
				return res;
			}
			return expression.getArgument().accept(this);
		}
		@Override
		public Object visitProgramDbRValue(ProgramDbRValue expression) {
			return expression.getCursorExpression().accept(this);
		}
		@Override
		public Object visitProgramSessionAttribute(ProgramSessionAttribute expression) {
			return new ArrayList<ProgramExpression>();
		}
		@Override
		public Object visitProgramRequestParameter(ProgramRequestParameter expression) {
			return new ArrayList<ProgramExpression>();
		}
	}
	
	static AslanppReference listOfReferences2FundamentalConjunction(List<AslanppReference> list) {
		if(list.size() == 0) {
			return null;
		} else if(list.size() == 1) {
			return list.get(0);
		} else {
			AslanppLogicalConjunction alc = AslanppLogicalConjunction.createInstance(list.get(0),list.get(1));
			for(int i = 2; i < list.size(); i++) {
				alc = AslanppLogicalConjunction.createInstance(alc,list.get(i));
			}
			return alc;
		}
 	}
	
	static List<AslanppReference> addIfNotContained(List<AslanppReference> list, AslanppReference theElement) {
		for(AslanppReference aRef : list) {
			if(aRef.isTheSame(theElement)) return list;
		}
		list.add(theElement);
		return list;
	}

	static List<AslanppReference> addAllNotContained(List<AslanppReference> list, List<AslanppReference> theElements) {
		for(AslanppReference anElement : theElements) {
			addIfNotContained(list, anElement);
		}
		return list;
	}

	static void split(List<AslanppReference> one, List<AslanppReference> two, List<AslanppReference> intersection, List<AslanppReference> dif1, List<AslanppReference> dif2) {
		boolean mark[] = new boolean[two.size()];
		Arrays.fill(mark, false);
		for(AslanppReference eOne : one) {
			boolean found = false;
			int i = 0;
			for(AslanppReference eTwo : two) {
				if(eOne.isTheSame(eTwo)) {
					if(intersection != null) {
						addIfNotContained(intersection,eTwo);
						found = true;
						mark[i] = true;
					}
					i++;
				}
			}
			if(!found && dif1 != null) {
				addIfNotContained(dif1,eOne);
			}
		}
		if(dif2 != null) {
			int i = 0;
			for(AslanppReference eTwo : two) {
				if(!mark[i]) {
					addIfNotContained(dif2,eTwo);
				}
				i++;
			}
		}
	}

	public static boolean isTheSame(List<AslanppStatement> codeBeforeSelect, List<AslanppStatement> code) {
		if(codeBeforeSelect.size() == code.size()) {
			for(int i = 0; i < codeBeforeSelect.size(); i++) {
				if(!codeBeforeSelect.get(i).isTheSame(code.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

}
