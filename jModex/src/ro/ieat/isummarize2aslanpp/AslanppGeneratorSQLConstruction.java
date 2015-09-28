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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.AllComparisonExpression;
import net.sf.jsqlparser.expression.AnyComparisonExpression;
import net.sf.jsqlparser.expression.CaseExpression;
import net.sf.jsqlparser.expression.DateValue;
import net.sf.jsqlparser.expression.DoubleValue;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.InverseExpression;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.NullValue;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.TimeValue;
import net.sf.jsqlparser.expression.TimestampValue;
import net.sf.jsqlparser.expression.WhenClause;
import net.sf.jsqlparser.expression.operators.arithmetic.Addition;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseAnd;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseOr;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseXor;
import net.sf.jsqlparser.expression.operators.arithmetic.Concat;
import net.sf.jsqlparser.expression.operators.arithmetic.Division;
import net.sf.jsqlparser.expression.operators.arithmetic.Multiplication;
import net.sf.jsqlparser.expression.operators.arithmetic.Subtraction;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.Between;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExistsExpression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.ItemsListVisitor;
import net.sf.jsqlparser.expression.operators.relational.LikeExpression;
import net.sf.jsqlparser.expression.operators.relational.Matches;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserManager;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.AllTableColumns;
import net.sf.jsqlparser.statement.select.FromItemVisitor;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitor;
import net.sf.jsqlparser.statement.select.SelectVisitor;
import net.sf.jsqlparser.statement.select.SubJoin;
import net.sf.jsqlparser.statement.select.SubSelect;
import net.sf.jsqlparser.statement.select.Union;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.truncate.Truncate;
import net.sf.jsqlparser.statement.update.Update;
import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.ProgramConversionExpression;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramField;
import ro.ieat.isummarize.ProgramFunction;
import ro.ieat.isummarize.ProgramIndexExpression;
import ro.ieat.isummarize.ProgramIndexExpressionConstant;
import ro.ieat.isummarize.ProgramStringBinaryExpression;
import ro.ieat.isummarize.ProgramStringBinaryExpression.StringBinaryOperator;
import ro.ieat.isummarize.javasql.ProgramDataBase;
import ro.ieat.isummarize.javasql.ProgramDataBaseModification;
import ro.ieat.isummarize.javasql.ProgramDbRValue;
import ro.ieat.isummarize.javasql.ProgramDbRValue.ProgramDbRValueVisitor;
import ro.ieat.isummarize.javasql.ProgramSQLCursorAction;
import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter;
import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter.ProgramRequestParameterVisitor;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute.ProgramSessionAttributeVisitor;
import ro.ieat.isummarize2aslanpp.FactorOutCursorCreationOptimisation.FactorOutCursorCreationOptimisationData;

public class AslanppGeneratorSQLConstruction implements StatementVisitor, SelectVisitor, FromItemVisitor, SelectItemVisitor, ExpressionVisitor, ItemsListVisitor {

	private int cursorCounter = 0;
	private List<ProgramIndexExpression> symbolList = new ArrayList<ProgramIndexExpression>();
	private List<AslanppMultipleReference> assocList = new ArrayList<AslanppMultipleReference>();
	private void propagatedAdd(ProgramIndexExpression sym, AslanppMultipleReference multiRef) {
		for(int i = 0; i < symbolList.size(); i++) {
			if(symbolList.get(i).equals(sym)) {
				return;
			}
		}
		symbolList.add(sym);
		assocList.add(multiRef);
	}
	private AslanppMultipleReference propagatedLookUp(ProgramIndexExpression sym) {
		for(int i = 0; i < symbolList.size(); i++) {
			if(symbolList.get(i).equals(sym)) {
				return assocList.get(i);
			}
		}	
		return null;
	}
	
	private AslanppGenerator.ConversionVisitor rootVisitor;
	private Map<String, List<String>> db;
	
	AslanppGeneratorSQLConstruction(AslanppGenerator.ConversionVisitor rootVisitor) {
		this.rootVisitor = rootVisitor;
	}
	
	class AslanppMultipleReference implements AslanppReference {

		private ArrayList<String> columnName = new ArrayList<String>();
		private ArrayList<AslanppSymbol> columnVariable = new ArrayList<AslanppSymbol>();
		private ArrayList<Integer> projection = null;
		private AslanppSymbol tableSet, cursorSet;
		private String tableName;
		private AslanppType cursorType;
		
		public AslanppMultipleReference(AslanppSymbol tableSet, String tableName, AslanppType cursorType) {
			this.tableSet = tableSet;
			this.tableName = tableName;
			this.cursorType = cursorType;
		}
		
		public String getTableName() {
			return tableName;
		}

		public AslanppType getCursorType() {
			return cursorType;
		}

		public void setCursorSetSymbol(AslanppSymbol x) {
			cursorSet = x;
		}
		
		public AslanppSymbol getCursorSetSymbol() {
			return cursorSet;
		}

		public AslanppSymbol getTableSetSymbol() {
			return tableSet;
		}

		public void add(String columnName, AslanppSymbol columnVariable) {
			this.columnName.add(columnName);
			this.columnVariable.add(columnVariable);
		}

		public void recordProjection(String columnName) {
			if(projection == null) {
				projection = new ArrayList<Integer>();
			}
			for(int i = 0; i < this.columnName.size(); i++) {
				if(this.columnName.get(i).equals(columnName)) {
					projection.add(i);
				}
			}
		}
		
		public int getNumberOfColumns() {
			return columnName.size(); 
		}
		
		public String getColumnSimpleName(int i) {
			return columnName.get(i);
		}

		public AslanppSymbol getColumnSymbol(int i) {
			return columnVariable.get(i);
		}

		public AslanppSymbol getColumnSymbolConsideringProjection(int i) {
			if(projection == null) {
				return getColumnSymbol(i);
			} else {
				return getColumnSymbol(projection.get(i-1));
			}
		}

		public ArrayList<Integer> getProjectedColumns() {
			return projection;
		}

		public AslanppSymbol getColumnSymbolConsideringProjection(String colName) {
			int i;
			for(i = 0; i < columnName.size(); i++) {
				if(columnName.get(i).equals(colName)) {
					break;
				}
			}
			if(i == columnName.size()) {
				throw new RuntimeException("Unknown column name " + colName);
			}
			if(projection == null) {
				return getColumnSymbol(i);
			} else {
				for(int j = 0; j < projection.size(); j++) {
					if(i == projection.get(j)) {
						return getColumnSymbolConsideringProjection(j + 1);						
					}
				}
			}
			throw new RuntimeException("Unknown column name " + colName + " in cursor!");
		}
		
		public int lookupColumnSymbol4SimpleColumnName(String simpleColName) {
			int i;
			for(i = 0; i < columnName.size(); i++) {
				if(columnName.get(i).equals(simpleColName)) {
					for(int j = i + 1; j < columnName.size(); j++) {
						if(columnName.get(j).equals(simpleColName))
							throw new RuntimeException("Duplicated column simple name " + simpleColName);						
					}
					return i;
				}
			}
			throw new RuntimeException("Unknown column name " + simpleColName);
		}

		@Override
		public void print(int tabs, StringBuilder str) {
			throw new RuntimeException("Should not appear in generated code");
		}

		@Override
		public void setParent(AslanppMetaEntity entity) {
			throw new RuntimeException("Should not appear in generated code");			
		}

		@Override
		public AslanppType getType() {
			throw new RuntimeException("Should not appear in generated code");
		}

		@Override
		public boolean isTheSame(AslanppExpression ref) {
			throw new RuntimeException("Should not appear in generated code");
		}

		@Override
		public Object accept(AslanppVisitor visitor) {
			throw new RuntimeException("Should not appear in generated code");
		}
		
	}
	
	public TranslatedExpression translate(ProgramDbRValue expression) {
		this.db = expression.getModel().getTablesWithColumns();	
		final List<TranslatedExpression> tArgs = new ArrayList<TranslatedExpression>();
		tArgs.add((TranslatedExpression)expression.getCursorExpression().accept(rootVisitor));
		tArgs.add((TranslatedExpression)expression.getColumnAccessExpression().accept(rootVisitor));		
		return new TranslatedExpression<ProgramDbRValue>(expression) {
			
			@Override
			public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
				if(operator != null) {return null;}
				List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
				for(TranslatedExpression anArg : tArgs) {
					code.addAll(anArg.getRValueCode(inGuard, null, null));
				}
				return code;
			}	

			@Override
			public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
				if(operator != null) {return null;}
				AslanppReference cursorReference = tArgs.get(0).getReferenceForRValueCode(inGuard, null, null);
				if(cursorReference instanceof AslanppMultipleReference) {
					ProgramExpression columnExp = originalExpression.getColumnAccessExpression();
					if(columnExp instanceof ProgramIndexExpression && ((ProgramIndexExpression) columnExp).isIntConstant()) {
						return ((AslanppMultipleReference) cursorReference).getColumnSymbolConsideringProjection(((ProgramIndexExpression) columnExp).getNumberConstant().intValue()).getReference(false, null);
					} else if(columnExp instanceof ProgramIndexExpression && ((ProgramIndexExpression) columnExp).isStringConstant()) {
						String tmp = ((ProgramIndexExpression) columnExp).getStringConstant();
						return ((AslanppMultipleReference) cursorReference).getColumnSymbolConsideringProjection(tmp.substring(1, tmp.length() - 1)).getReference(false, null);						
					}
					return null;
				} else if(originalExpression.getCursorExpression() instanceof ProgramIndexExpression && propagatedLookUp((ProgramIndexExpression)originalExpression.getCursorExpression()) != null) {
					AslanppMultipleReference theCursorRef = propagatedLookUp((ProgramIndexExpression)originalExpression.getCursorExpression());
					ProgramExpression columnExp = originalExpression.getColumnAccessExpression();
					if(columnExp instanceof ProgramIndexExpression && ((ProgramIndexExpression) columnExp).isIntConstant()) {
						return ((AslanppMultipleReference) theCursorRef).getColumnSymbolConsideringProjection(((ProgramIndexExpression) columnExp).getNumberConstant().intValue()).getReference(false, null);
					}
				}
				String name = AslanppGenerator.PREFIX + "DBAccessFunction";
				name = AslanppGenerator.eliminateAnslanppTokens(name);
				List<AslanppReference> argRefs = new ArrayList<AslanppReference>();
				String structure = "";
				for(TranslatedExpression anArg : tArgs) {
					AslanppReference aRef = anArg.getReferenceForRValueCode(inGuard, null, null);
					argRefs.add(aRef);
					structure += aRef.getType().getName() + ",";
				}
				if(!structure.equals("")) {
					structure = structure.substring(0, structure.length() - 1);
				}
				AslanppType theType = AslanppGenerator.JAVA_REFERENCE_ASLANPP;
				AslanppTuple contained = new AslanppTuple(argRefs.toArray(new AslanppReference[] {}));
				AslanppSymbolReference functionSymbol = rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, name, structure, theType).getReference(false, contained);
				return functionSymbol;
			}
			
			@Override
			public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
				throw new RuntimeException("Cannot assign to a db column!");
			}
			
		};
	}

	private static int this_translate_in_progress = 0;
	public TranslatedExpression translate(ProgramSQLCursorAction stm) {
		FactorOutCursorCreationOptimisation.currentCursorCreation = stm;
		final int counter = this_translate_in_progress++;
		try{
			this.db = stm.getModel().getTablesWithColumns();
			if(stm.getFunction() == ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE) {
				String theStatement = null;
				final Pair theTop;
				try {
					theStatement = toStringVisitor.process(stm.getArgument(),stm.getArgsPositions(),stm.getArgsValues());
					CCJSqlParserManager pm = new CCJSqlParserManager();
					Statement sqlStm = pm.parse(new StringReader(theStatement));
					stack.clear();
					stackCurrentTable.clear();
					sqlStm.accept(this);
					theTop = stack.pop();
				} catch (JSQLParserException e) {
					System.err.println("Cannot process SQL statement: " + theStatement);
					System.err.println("Use uninterpreted functions");
					final TranslatedExpression te = (TranslatedExpression) stm.getArgument().accept(rootVisitor);
					return new TranslatedExpression<ProgramSQLCursorAction>(stm) {
						@Override
						public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
							return te.getRValueCode(inGuard, operator, comparingValue);
						}
						@Override
						public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
							if(operator != null) return null;
							String functionName = AslanppGenerator.eliminateAnslanppTokens(AslanppGenerator.PREFIX + "DBCursorCreation");
							String stucture = AslanppType.messageType.getName();
							AslanppTuple args = new AslanppTuple(te.getReferenceForRValueCode(inGuard, operator, comparingValue));
							AslanppSymbolReference functionSymbol = rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, functionName, stucture, AslanppGenerator.JAVA_REFERENCE_ASLANPP).getReference(false, args);
							return functionSymbol;
						}						
					};					
				}
				return new TranslatedExpression<ProgramSQLCursorAction>(stm) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
						res.addAll(theTop.code);
						return res;
					}
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						if(counter == 0) {
							return theTop.ref.getCursorSetSymbol().getReference(false, null);
						}
						return theTop.ref;							
					}
					@Override
					public List<AslanppStatement> collectAssignmentData(ProgramIndexExpression exp) {
						propagatedAdd(exp,theTop.ref);
						return this.getRValueCode(false, null, null);
					}
				};
			} else if(stm.getFunction() == ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_HAS_NEXT) {
				final TranslatedExpression theCursor = (TranslatedExpression) stm.getArgument().accept(rootVisitor);
				return new TranslatedExpression<ProgramSQLCursorAction>(stm) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						if(inGuard) {
							List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
							res.addAll(theCursor.getRValueCode(inGuard, null, null));
							return res;
						} else {
							//TODO : Not done
							return null;
						}
					}
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						if(inGuard) {
							if(originalExpression.getArgument() instanceof ProgramIndexExpression && propagatedLookUp((ProgramIndexExpression)originalExpression.getArgument()) != null) {
								AslanppMultipleReference theCursorRef = propagatedLookUp((ProgramIndexExpression)originalExpression.getArgument());
								AslanppContains theTest = new AslanppContains(theCursorRef.getCursorSetSymbol().getReference(false, null), new AslanppStringStatement("?",false), true);
								return theTest;
							} else {
								AslanppReference reference = theCursor.getReferenceForRValueCode(inGuard, null, null);
								if(reference instanceof AslanppMultipleReference) {
									AslanppContains theTest = new AslanppContains(((AslanppMultipleReference) reference).getCursorSetSymbol().getReference(false, null), new AslanppStringStatement("?",false), true);
									return theTest;							
								} else {
									String functionName = AslanppGenerator.eliminateAnslanppTokens(AslanppGenerator.PREFIX + "DBCursorHasNext");
									String stucture = AslanppType.messageType.getName();
									AslanppTuple args = new AslanppTuple(theCursor.getReferenceForRValueCode(inGuard, operator, comparingValue));
									AslanppSymbolReference functionSymbol = rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, functionName, stucture, AslanppType.factType).getReference(false, args);
									return functionSymbol;
								}
							}
						} else {
							//TODO : Not done
							return null;
						}
					}
				};
			} else if(stm.getFunction() == ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_NEXT) {
				final TranslatedExpression theCursor = (TranslatedExpression) stm.getArgument().accept(rootVisitor);
				return new TranslatedExpression<ProgramSQLCursorAction>(stm) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
						AslanppMultipleReference cursorReference = null;
						if(originalExpression.getArgument() instanceof ProgramIndexExpression && propagatedLookUp((ProgramIndexExpression)originalExpression.getArgument()) != null) {
							cursorReference = propagatedLookUp((ProgramIndexExpression)originalExpression.getArgument());
						} else {
							res.addAll(theCursor.getRValueCode(inGuard, null, null));
							AslanppReference ref = theCursor.getReferenceForRValueCode(inGuard, null, null);
							if(ref instanceof AslanppMultipleReference) {
								cursorReference = (AslanppMultipleReference)ref;
							}
						}
						if(cursorReference != null) {
							AslanppReference args[] = new AslanppReference[cursorReference.getNumberOfColumns()];
							for(int i = 0; i < cursorReference.getNumberOfColumns(); i++) {
								args[i] = cursorReference.getColumnSymbol(i).getReference(true, null);
							}
							AslanppTuple toExtract = new AslanppTuple(args);
							AslanppContains theContains = new AslanppContains(cursorReference.getCursorSetSymbol().getReference(false, null),toExtract,true);
							AslanppIf theIf = new AslanppIf(theContains);
							args = new AslanppReference[cursorReference.getNumberOfColumns()];
							for(int i = 0; i < cursorReference.getNumberOfColumns(); i++) {
								args[i] = cursorReference.getColumnSymbol(i).getReference(false, null);								
							}
							AslanppTuple toRemove = new AslanppTuple(args);
							AslanppRemove theRemove = new AslanppRemove(cursorReference.getCursorSetSymbol().getReference(false, null),toRemove);
							theIf.addStatementThen(theRemove);
							res.add(theIf);				
						}
						return res;
					}
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						AslanppReference theCursorRef = null;
						if(originalExpression.getArgument() instanceof ProgramIndexExpression && propagatedLookUp((ProgramIndexExpression)originalExpression.getArgument()) != null) {
							theCursorRef = propagatedLookUp((ProgramIndexExpression)originalExpression.getArgument());
						} else { 
							theCursorRef = theCursor.getReferenceForRValueCode(inGuard, null, null);
							if(!(theCursorRef instanceof AslanppMultipleReference)) {
								String functionName = AslanppGenerator.eliminateAnslanppTokens(AslanppGenerator.PREFIX + "DBCursorNext");
								String stucture = AslanppType.messageType.getName();
								AslanppTuple args = new AslanppTuple(theCursorRef);
								AslanppSymbolReference functionSymbol = rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, functionName, stucture, AslanppType.messageType).getReference(false, args);
								return functionSymbol;
							}
						}
						return theCursorRef;
					}
					@Override
					public List<AslanppStatement> collectAssignmentData(ProgramIndexExpression exp) {
						if(propagatedLookUp(exp) != null) {
							AslanppMultipleReference theCursor = propagatedLookUp(exp);
							AslanppReference args[] = new AslanppReference[theCursor.getNumberOfColumns()];
							AslanppReference argsNotBound[] = new AslanppReference[theCursor.getNumberOfColumns()];
							for(int i = 0; i < theCursor.getNumberOfColumns(); i++) {
								args[i] = theCursor.getColumnSymbol(i).getReference(true, null);
								argsNotBound[i] = theCursor.getColumnSymbol(i).getReference(false, null);
							}
							AslanppTuple toExtract = new AslanppTuple(args);
							AslanppContains theContains = new AslanppContains(theCursor.getCursorSetSymbol().getReference(false, null),toExtract,true);
							AslanppIf theIf = new AslanppIf(theContains);
							AslanppTuple toRemove = new AslanppTuple(argsNotBound);
							AslanppRemove theRemove = new AslanppRemove(theCursor.getCursorSetSymbol().getReference(false, null), toRemove);
							List<AslanppStatement> theCode = new ArrayList<AslanppStatement>();
							theCode.add(theIf);
							theCode.add(theRemove);
							return theCode;
						} else {
							return null;
						}
					}
				};
			}
		} finally {
			this_translate_in_progress--;
		}
		return null;
	}
	
	public TranslatedExpression translate(ProgramDataBase expression) {
		return new TranslatedExpression<ProgramDataBase>(expression) {

			public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
				throw new RuntimeException("DB expression should not be refered in the Aslan++ model");
			}

			@Override
			public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
				throw new RuntimeException("DB expression should not be refered in the Aslan++ model");
			}

			@Override
			public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
				return assignedExpression.getRValueCode(false, null, null);
			}

		};
	}
	
	public TranslatedExpression translate(ProgramDataBaseModification expression) {
		this.db = expression.getModel().getTablesWithColumns();
		String theStatement = null;
		final Pair theTop;
		try {
			theStatement = (String) expression.getQuery().accept(toStringVisitor);
			CCJSqlParserManager pm = new CCJSqlParserManager();
			Statement sqlStm = pm.parse(new StringReader(theStatement));
			stack.clear();
			stackCurrentTable.clear();
			sqlStm.accept(this);
			theTop = stack.pop();
		} catch (JSQLParserException e) {
			System.err.println("Cannot process SQL statement: " + theStatement);
			return (TranslatedExpression) expression.getQuery().accept(rootVisitor);
		}
		return new TranslatedExpression<ProgramDataBaseModification>(expression) {
			@Override
			public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
				if(operator != null) return null;
				List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
				res.addAll(theTop.code);
				return res;
			}
			@Override
			public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
				return null;
			}
		};
	}

	//SQL statements translator part of the object
	private Stack<Pair> stack = new Stack<Pair>();
	private Stack<AslanppMultipleReference> stackCurrentTable = new Stack<AslanppMultipleReference>();

	private final String TABLE_PREFIX = "dbTable_";
	private final String COLUMN_PREFIX = "dbColumn_";
	private final String TEMPORARY_PREFIX = "Tmp";
	private final String RESULTSET_PREFIX = "dbResultSet_";
		
	private class Pair {
		AslanppMultipleReference ref;
		TranslatedExpression expRef;
		List<AslanppStatement> code;
		Pair() {
			code = new ArrayList<AslanppStatement>();
		}
	}
						
	@Override
	public void visit(Select arg0) {
		arg0.getSelectBody().accept(this);
	}

	@Override
	public void visit(Delete arg0) {
		Pair res = new Pair();

		//Take the table
		arg0.getTable().accept(this);
		Pair tablePair = stack.pop();
		res.code.addAll(tablePair.code);
		stackCurrentTable.push(tablePair.ref);
				
		//Process condition
		arg0.getWhere().accept(this);
		Pair wherePair = stack.pop();
		res.code.addAll(wherePair.code);
	
		//Generate update code
		AslanppSymbol tempSet = rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, TEMPORARY_PREFIX + tablePair.ref.getTableSetSymbol().getName(), "", tablePair.ref.getTableSetSymbol().getType());			
		res.code.add(new AslanppAssignment(tempSet.getReference(false, null), new AslanppEmptySetInstantiation()));
		
		//Select the items, remove them from table and add them to TempSet (the deleted ones will not be inserted in TempSet)
		List<AslanppReference> removedTupleComponentsBoundSelectingWhile = new ArrayList<AslanppReference>();
		List<AslanppReference> removedTupleComponentsUnboundSelectingWhile = new ArrayList<AslanppReference>();
		for(int i = 0; i < tablePair.ref.getNumberOfColumns(); i++) {
			AslanppSymbol aSymbol = tablePair.ref.getColumnSymbol(i);
			removedTupleComponentsBoundSelectingWhile.add(aSymbol.getReference(true, null));
			removedTupleComponentsUnboundSelectingWhile.add(aSymbol.getReference(false, null));
		}
		AslanppWhile theSelectionWhile = new AslanppWhile(
				new AslanppContains(
					tablePair.ref.getTableSetSymbol().getReference(false, null), 
					new AslanppTuple(removedTupleComponentsBoundSelectingWhile.toArray(new AslanppReference[] {})), 
					true));
		
		AslanppTuple removedUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
		theSelectionWhile.addStatement(new AslanppRemove(tablePair.ref.getTableSetSymbol().getReference(false, null), removedUnboundSelectingWhileTuple));

		AslanppReference theGuard = wherePair.expRef.getReferenceForRValueCode(true, null, null);
		AslanppIf theIf = new AslanppIf(AslanppLogicalNegation.createInstance(theGuard));
		AslanppTuple addToResUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
		theIf.addStatementThen(new AslanppContains(tempSet.getReference(false, null), addToResUnboundSelectingWhileTuple, false));

		theSelectionWhile.addStatement(theIf);		
	
		//Database (the table) becomes the new set
		res.code.add(theSelectionWhile);
		res.code.add(new AslanppAssignment(tablePair.ref.getTableSetSymbol().getReference(false, null), tempSet.getReference(false, null)));

		stackCurrentTable.pop();
		stack.push(res);
	}

	@Override
	public void visit(Update arg0) {
		Pair res = new Pair();

		//Take the table
		arg0.getTable().accept(this);
		Pair tablePair = stack.pop();
		res.code.addAll(tablePair.code);
		stackCurrentTable.push(tablePair.ref);
				
		//Process condition
		arg0.getWhere().accept(this);
		Pair wherePair = stack.pop();
		res.code.addAll(wherePair.code);
	
		//Generate update code
		AslanppSymbol tempSet = rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, TEMPORARY_PREFIX + tablePair.ref.getTableSetSymbol().getName(), "", tablePair.ref.getTableSetSymbol().getType());			
		res.code.add(new AslanppAssignment(tempSet.getReference(false, null), new AslanppEmptySetInstantiation()));
		
		//Select the items, remove them from table and add them to TempSet (with modification and without based on where)
		List<AslanppReference> removedTupleComponentsBoundSelectingWhile = new ArrayList<AslanppReference>();
		List<AslanppReference> removedTupleComponentsUnboundSelectingWhile = new ArrayList<AslanppReference>();
		for(int i = 0; i < tablePair.ref.getNumberOfColumns(); i++) {
			AslanppSymbol aSymbol = tablePair.ref.getColumnSymbol(i);
			removedTupleComponentsBoundSelectingWhile.add(aSymbol.getReference(true, null));
			removedTupleComponentsUnboundSelectingWhile.add(aSymbol.getReference(false, null));
		}
		AslanppWhile theSelectionWhile = new AslanppWhile(
				new AslanppContains(
					tablePair.ref.getTableSetSymbol().getReference(false, null), 
					new AslanppTuple(removedTupleComponentsBoundSelectingWhile.toArray(new AslanppReference[] {})), 
					true));
		AslanppTuple removedUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
		theSelectionWhile.addStatement(new AslanppRemove(tablePair.ref.getTableSetSymbol().getReference(false, null), removedUnboundSelectingWhileTuple));

		AslanppReference theGuard = wherePair.expRef.getReferenceForRValueCode(true, null, null);
		AslanppIf theIfSelector = new AslanppIf(theGuard);	
		AslanppTuple addToResUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
		//Modify column data
		for(int i = 0; i < arg0.getColumns().size(); i++) {
			Column aColumn = (Column) arg0.getColumns().get(i);
			aColumn.accept(this);
			Pair columnPair = stack.pop();
			res.code.addAll(columnPair.code);
			Expression aExp = (Expression) arg0.getExpressions().get(i);
			aExp.accept(this);
			Pair exprPair = stack.pop();
			res.code.addAll(exprPair.code);
			for(AslanppStatement aStm : ((List<AslanppStatement>)columnPair.expRef.getLValueCode(exprPair.expRef))) {
				theIfSelector.addStatementThen(aStm);
			}
		}
		theIfSelector.addStatementThen(new AslanppContains(tempSet.getReference(false, null), addToResUnboundSelectingWhileTuple, false));

		theIfSelector.addStatementElse(new AslanppContains(tempSet.getReference(false, null), addToResUnboundSelectingWhileTuple, false));
		theSelectionWhile.addStatement(theIfSelector);		
	
		//Database becomes the new set
		res.code.add(theSelectionWhile);
		res.code.add(new AslanppAssignment(tablePair.ref.getTableSetSymbol().getReference(false, null), tempSet.getReference(false, null)));

		stackCurrentTable.pop();
		stack.push(res);
	}

	@Override
	public void visit(Insert arg0) {
		Pair res = new Pair();

		//Take the table
		arg0.getTable().accept(this);
		Pair tablePair = stack.pop();
		res.code.addAll(tablePair.code);
		
		List<AslanppReference> allValues = new ArrayList<AslanppReference>();
		for(int i = 0; i < ((ExpressionList)arg0.getItemsList()).getExpressions().size(); i++) {
			Expression anExp = (Expression) ((ExpressionList)arg0.getItemsList()).getExpressions().get(i);
			anExp.accept(this);
			Pair anExpResult = stack.pop();
			res.code.addAll(anExpResult.expRef.getRValueCode(false, null, null));
			allValues.add(anExpResult.expRef.getReferenceForRValueCode(false, null, null));
		}

		AslanppReference[] theColumnValues = new AslanppReference[tablePair.ref.getNumberOfColumns()];
		for(int i = 1; i < tablePair.ref.getNumberOfColumns(); i++) {
			theColumnValues[i] = rootVisitor.getNullReference();
		}
		res.code.add(new AslanppAssignment(
					tablePair.ref.getColumnSymbol(0).getReference(false, null),
						new AslanppStringStatement("fresh()",false)));
		theColumnValues[0] = tablePair.ref.getColumnSymbol(0).getReference(false, null);
		if(arg0.getColumns() != null) {
			for(int i = 0; i < arg0.getColumns().size(); i++) {
				String aColumnName = ((Column)arg0.getColumns().get(i)).getColumnName();
				int j = tablePair.ref.lookupColumnSymbol4SimpleColumnName(aColumnName);
				theColumnValues[j] = allValues.get(i);
			}
		} else {
			for(int i = 0; i < allValues.size(); i++) {
				theColumnValues[i+1] = allValues.get(i);
			}
		}
		AslanppTuple addedTuple = new AslanppTuple(theColumnValues);
		res.code.add(new AslanppContains(tablePair.ref.tableSet.getReference(false,null),addedTuple,false));
		stack.push(res);
	}

	@Override
	public void visit(Replace arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(Drop arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(Truncate arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(CreateTable arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(PlainSelect arg0) {
		Pair res = new Pair();
		res.code = new ArrayList<AslanppStatement>();

		String selectedTableName = null;
		Pair selectedTable = null;
		if(arg0.getJoins() == null || arg0.getJoins().size() == 0) {
			//Take the table
			arg0.getFromItem().accept(this);
			selectedTable = stack.pop();
			res.code.addAll(selectedTable.code);
			res.ref = selectedTable.ref;					
			//The name of the table being iterated
			selectedTableName = res.ref.tableName;	
			stackCurrentTable.push(res.ref);
		} else {
			List<Pair> allPairs = new ArrayList<Pair>();
			arg0.getFromItem().accept(this);
			allPairs.add(stack.pop());
			for(int i = 0; i < arg0.getJoins().size(); i++) {
				((Join)arg0.getJoins().get(i)).getRightItem().accept(this);
				allPairs.add(stack.pop());
			}
			ArrayList<AslanppType> allColumnTypes = new ArrayList<AslanppType>();
			selectedTableName = "Join";
			for(Pair aPair : allPairs) {
				for(int j = 0; j < aPair.ref.getNumberOfColumns(); j++) {
					allColumnTypes.add(aPair.ref.getColumnSymbol(j).getType());
				}
				selectedTableName = selectedTableName + "_" + aPair.ref.getTableName();
			}
			AslanppSymbol joinedTableSymbol = rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, TEMPORARY_PREFIX + selectedTableName, "", AslanppType.getSetType(AslanppType.getTupleType(allColumnTypes.toArray(new AslanppType[]{}))));
			selectedTable = new Pair();
			res.ref = selectedTable.ref = new AslanppMultipleReference(joinedTableSymbol, selectedTableName, AslanppType.getSetType(AslanppType.getTupleType(allColumnTypes.toArray(new AslanppType[]{}))));
			for(Pair aPair : allPairs) {
				for(int i = 0; i < aPair.ref.getNumberOfColumns(); i++) {
					selectedTable.ref.add(aPair.ref.getColumnSimpleName(i), aPair.ref.getColumnSymbol(i));					
				}
			}
			//The join code
			res.code.add(new AslanppAssignment(joinedTableSymbol.getReference(false, null), new AslanppEmptySetInstantiation()));
			ArrayList<AslanppSymbol> backupTableSymbols = new ArrayList<AslanppSymbol>();
			for(int i = 0; i < allPairs.size(); i++) {
				AslanppSymbol resultTemp = rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, TEMPORARY_PREFIX + allPairs.get(i).ref.getTableSetSymbol().getName(), "", allPairs.get(i).ref.getTableSetSymbol().getType());			
				backupTableSymbols.add(resultTemp);
			}
			AslanppWhile joiningWhile = null;
			for(int i = allPairs.size() - 1; i >= 0; i--) {
				Pair currentTable = allPairs.get(i);
				AslanppReference[] extractedValues = new AslanppReference[currentTable.ref.getNumberOfColumns()];
				AslanppReference[] removedValues = new AslanppReference[currentTable.ref.getNumberOfColumns()];
				AslanppReference[] tmpValues = new AslanppReference[currentTable.ref.getNumberOfColumns()];
				for(int j = 0; j < currentTable.ref.getNumberOfColumns(); j++) {
					extractedValues[j] = currentTable.ref.getColumnSymbol(j).getReference(true, null);
					removedValues[j] = currentTable.ref.getColumnSymbol(j).getReference(false, null);
					tmpValues[j] = currentTable.ref.getColumnSymbol(j).getReference(false, null);
				}
				AslanppWhile aWhile = new AslanppWhile(
						new AslanppContains(currentTable.ref.getTableSetSymbol().getReference(false, null), 
								new AslanppTuple(extractedValues),true));
				aWhile.addStatement(new AslanppRemove(currentTable.ref.getTableSetSymbol().getReference(false, null), new AslanppTuple(removedValues)));
				aWhile.addStatement(new AslanppContains(backupTableSymbols.get(i).getReference(false, null), new AslanppTuple(tmpValues),false));
				if(joiningWhile != null) {
					aWhile.addStatement(new AslanppAssignment(backupTableSymbols.get(i+1).getReference(false, null), new AslanppEmptySetInstantiation()));
					aWhile.addStatement(joiningWhile);
					aWhile.addStatement(new AslanppAssignment(allPairs.get(i+1).ref.getTableSetSymbol().getReference(false, null), backupTableSymbols.get(i+1).getReference(false, null)));
				} else {
					//Put result in joied tmp table
					AslanppReference[] joinedValues = new AslanppReference[res.ref.getNumberOfColumns()];
					for(int j = 0; j < res.ref.getNumberOfColumns(); j++) {
						joinedValues[j] = res.ref.getColumnSymbol(j).getReference(false, null);
					}
					aWhile.addStatement(new AslanppContains(joinedTableSymbol.getReference(false, null), new AslanppTuple(joinedValues),false));
				}
				joiningWhile = aWhile;
			}
			res.code.add(new AslanppAssignment(backupTableSymbols.get(0).getReference(false, null), new AslanppEmptySetInstantiation()));
			res.code.add(joiningWhile);
			res.code.add(new AslanppAssignment(allPairs.get(0).ref.getTableSetSymbol().getReference(false, null), backupTableSymbols.get(0).getReference(false, null)));
			stackCurrentTable.push(res.ref);
		}
				
		//Process condition
		TranslatedExpression whereTrans = null;
		if(arg0.getWhere() != null) {
			arg0.getWhere().accept(this);
			Pair wherePair = stack.pop();
			res.code.addAll(wherePair.code);
			res.code.addAll(wherePair.expRef.getRValueCode(true, null, null));
			whereTrans = wherePair.expRef;
		}

		//Selected columns only
		boolean projection = true;
		boolean selectFunctions = false;
		if(arg0.getSelectItems().size() == 1 && arg0.getSelectItems().get(0) instanceof AllColumns) {
			projection = false;
			selectFunctions = false;
		} else {
			for(int i = 0; i < arg0.getSelectItems().size(); i++) {
				projection = projection & ((SelectExpressionItem)arg0.getSelectItems().get(i)).getExpression() instanceof Column;
				selectFunctions |= ((SelectExpressionItem)arg0.getSelectItems().get(i)).getExpression() instanceof Function;
			}
		}
		
		if(projection) {
			for(int i = 0; i < arg0.getSelectItems().size(); i++) {
				selectedTable.ref.recordProjection(((Column)((SelectExpressionItem)arg0.getSelectItems().get(i)).getExpression()).getColumnName());
			}
		}	
				
		//Generate select code
		//Initialize cursor with the empty set
		AslanppSymbol resultSetSymbol;
		ArrayList<AslanppSymbol> functionsVariablesSymbols = new ArrayList<AslanppSymbol>();
		if(selectFunctions) {
			AslanppType elements[] = new AslanppType[arg0.getSelectItems().size()];
			for(int i = 0; i < arg0.getSelectItems().size(); i++) {
				Function aFunction = (Function) ((SelectExpressionItem)arg0.getSelectItems().get(i)).getExpression();
				if(!aFunction.isAllColumns() || !aFunction.getName().equalsIgnoreCase("count")) {
					throw new RuntimeException("Do not know yet how to handle " + aFunction);
				}
				elements[i] = AslanppType.messageType;
				functionsVariablesSymbols.add(rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, AslanppGenerator.eliminateAnslanppTokens(aFunction.getName() + this.cursorCounter), "", AslanppGenerator.getIntType()));
				AslanppSymbol zeroInteger = rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, "i0", "", AslanppGenerator.getIntType());
				AslanppGenerator.addIntegerConstant(0l);
				res.code.add(new AslanppAssignment(functionsVariablesSymbols.get(i).getReference(false, null), zeroInteger.getReference(false, null)));
			}
			resultSetSymbol = rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, RESULTSET_PREFIX + selectedTable.ref.getTableName() + this.cursorCounter++, "", AslanppType.getSetType(AslanppType.getTupleType(elements)));
		} else {
			resultSetSymbol = rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, RESULTSET_PREFIX + selectedTable.ref.getTableName() + this.cursorCounter++, "", selectedTable.ref.getCursorType());
		}
		selectedTable.ref.setCursorSetSymbol(resultSetSymbol);
		res.code.add(new AslanppAssignment(resultSetSymbol.getReference(false, null), new AslanppEmptySetInstantiation()));
		
		//Optimization: try to combine the while of this select with another one
		if(FactorOutCursorCreationOptimisation.allCursorCreationCacheStack.size() > 0) {
			for(ProgramExpression aCursorCreation : FactorOutCursorCreationOptimisation.allCursorCreationStack.peek()) {
				if(aCursorCreation == FactorOutCursorCreationOptimisation.currentCursorCreation) {
					//It is in the list of cursor creation operations that we try to compress
					FactorOutCursorCreationOptimisationData optimizationData = FactorOutCursorCreationOptimisation.allCursorCreationCacheStack.peek().get(selectedTable.ref.getTableName());
					if(optimizationData != null) {

						//There is a while for the same column thus we can introduce within that loop
						
						//If the selector is the same, then use the same selector
						AslanppIf theIfSelector = null;
						if(whereTrans != null) {
							AslanppReference theGuard = whereTrans.getReferenceForRValueCode(true, null, null);
							boolean found = false;
							for(AslanppIf aSelect : optimizationData.selectors) {
								if(aSelect.getGuard().isTheSame(theGuard)) {
									theIfSelector = aSelect;
									found = true;
									break;
								}
							}
							if(!found) {
								theIfSelector = new AslanppIf(theGuard);
								theGuard = whereTrans.getReferenceForRValueCode(true, null, null);
								optimizationData.loop.addFirstStatement(theIfSelector);
								optimizationData.selectors.add(theIfSelector);
							}
						}
						if(selectFunctions) {
							AslanppSymbol oneInteger = rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, "i1", "", AslanppGenerator.getIntType());
							AslanppGenerator.addIntegerConstant(1l);
							for(int i = 0; i < arg0.getSelectItems().size(); i++) {
								AslanppTuple args = new AslanppTuple(functionsVariablesSymbols.get(i).getReference(false, null), oneInteger.getReference(false, null));
								if(theIfSelector != null) {
									theIfSelector.addStatementThen(new AslanppAssignment(functionsVariablesSymbols.get(i).getReference(false, null),
										AslanppGenerator.getIntAdd().getReference(false,args)));
								} else {
									new AslanppAssignment(functionsVariablesSymbols.get(i).getReference(false, null),
											AslanppGenerator.getIntAdd().getReference(false,args));
								}
							}
							AslanppReference[] theAgrsRefs = new AslanppReference[functionsVariablesSymbols.size()];
							for(int i = 0; i < arg0.getSelectItems().size(); i++) {
								AslanppSymbol toStringFunction = AslanppGenerator.getInt2String();
								theAgrsRefs[i] = toStringFunction.getReference(false, new AslanppTuple(functionsVariablesSymbols.get(i).getReference(false, null)));
							}			
							AslanppTuple args = new AslanppTuple(theAgrsRefs);
							optimizationData.after_loop.add(new AslanppContains(resultSetSymbol.getReference(false, null),args,false));
							res.ref.columnVariable = new ArrayList<AslanppSymbol>();
							res.ref.columnName = new ArrayList<String>();
							for(AslanppSymbol aSymbol : functionsVariablesSymbols) {	
								res.ref.columnVariable.add(rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, this.COLUMN_PREFIX + aSymbol.getName(), "", AslanppType.messageType));
								res.ref.columnName.add(aSymbol.getName());
							}
							res.ref.projection = new ArrayList<Integer>();
							res.ref.projection.add(0);
							res.ref.tableName = null;
						} else {
							List<AslanppReference> removedTupleComponentsUnboundSelectingWhile = new ArrayList<AslanppReference>();
							for(int i = 0; i < selectedTable.ref.getNumberOfColumns(); i++) {
								AslanppSymbol aSymbol = selectedTable.ref.getColumnSymbol(i);
								removedTupleComponentsUnboundSelectingWhile.add(aSymbol.getReference(false, null));
							}
							AslanppTuple addToResUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
							if(theIfSelector != null) {
								theIfSelector.addStatementThen(new AslanppContains(resultSetSymbol.getReference(false, null), addToResUnboundSelectingWhileTuple, false));
							} else {
								optimizationData.loop.addFirstStatement(new AslanppContains(resultSetSymbol.getReference(false, null), addToResUnboundSelectingWhileTuple, false));								
							}
						}				
						
						optimizationData.before_loop.addAll(res.code);
						
						res.code.clear();
						stack.push(res);
						stackCurrentTable.pop();
						return;
					}
				}
			}
		}
			
		//Initialize a temporary set to record the database when iterating on it
		AslanppSymbol resultTemp = rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, TEMPORARY_PREFIX + selectedTable.ref.getTableSetSymbol().getName(), "", selectedTable.ref.getTableSetSymbol().getType());			
		res.code.add(new AslanppAssignment(resultTemp.getReference(false, null), new AslanppEmptySetInstantiation()));
				
		//Select the items, remove them from table and add them to ResultSet and TempSet
		List<AslanppReference> removedTupleComponentsBoundSelectingWhile = new ArrayList<AslanppReference>();
		List<AslanppReference> removedTupleComponentsUnboundSelectingWhile = new ArrayList<AslanppReference>();
		for(int i = 0; i < selectedTable.ref.getNumberOfColumns(); i++) {
			AslanppSymbol aSymbol = selectedTable.ref.getColumnSymbol(i);
			removedTupleComponentsBoundSelectingWhile.add(aSymbol.getReference(true, null));
			removedTupleComponentsUnboundSelectingWhile.add(aSymbol.getReference(false, null));
		}
		AslanppWhile theSelectionWhile = new AslanppWhile(
				new AslanppContains(
					selectedTable.ref.getTableSetSymbol().getReference(false, null), 
					new AslanppTuple(removedTupleComponentsBoundSelectingWhile.toArray(new AslanppReference[] {})), 
					true)
			);
		//Generate the code for filtering 
		AslanppReference theGuard;
		AslanppIf theIfSelector = null;
		if(whereTrans != null) {
			theGuard = whereTrans.getReferenceForRValueCode(true, null, null);
			theIfSelector = new AslanppIf(theGuard);
		}
		if(selectFunctions) {
			AslanppSymbol oneInteger = rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, "i1", "", AslanppGenerator.getIntType());
			AslanppGenerator.addIntegerConstant(1l);
			for(int i = 0; i < arg0.getSelectItems().size(); i++) {
				AslanppTuple args = new AslanppTuple(functionsVariablesSymbols.get(i).getReference(false, null), oneInteger.getReference(false, null));
				if(theIfSelector != null) {
					theIfSelector.addStatementThen(new AslanppAssignment(functionsVariablesSymbols.get(i).getReference(false, null),
							AslanppGenerator.getIntAdd().getReference(false,args)));
				} else {
					theSelectionWhile.addStatement(new AslanppAssignment(functionsVariablesSymbols.get(i).getReference(false, null),
						AslanppGenerator.getIntAdd().getReference(false,args)));
				}
			}
			theSelectionWhile.addStatement(theIfSelector);
		} else {
			if(theIfSelector != null) {
				AslanppTuple addToResUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
				theIfSelector.addStatementThen(new AslanppContains(resultSetSymbol.getReference(false, null), addToResUnboundSelectingWhileTuple, false));
				theSelectionWhile.addStatement(theIfSelector);
			} else {
				AslanppTuple addToResUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
				theSelectionWhile.addStatement(new AslanppContains(resultSetSymbol.getReference(false, null), addToResUnboundSelectingWhileTuple, false));
			}
		}
		AslanppTuple removedUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
		theSelectionWhile.addStatement(new AslanppRemove(selectedTable.ref.getTableSetSymbol().getReference(false, null), removedUnboundSelectingWhileTuple));
		AslanppTuple addToTempUnboundSelectingWhileTuple = new AslanppTuple(removedTupleComponentsUnboundSelectingWhile.toArray(new AslanppReference[]{}));
		theSelectionWhile.addStatement(new AslanppContains(resultTemp.getReference(false, null), addToTempUnboundSelectingWhileTuple, false));			
		res.code.add(theSelectionWhile);
		if(selectFunctions) {
			AslanppReference[] theAgrsRefs = new AslanppReference[functionsVariablesSymbols.size()];
			for(int i = 0; i < arg0.getSelectItems().size(); i++) {
				AslanppSymbol toStringFunction = AslanppGenerator.getInt2String();
				theAgrsRefs[i] = toStringFunction.getReference(false, new AslanppTuple(functionsVariablesSymbols.get(i).getReference(false, null)));
			}			
			AslanppTuple args = new AslanppTuple(theAgrsRefs);
			res.code.add(new AslanppContains(resultSetSymbol.getReference(false, null),args,false));
			res.ref.columnVariable = new ArrayList<AslanppSymbol>();
			res.ref.columnName = new ArrayList<String>();
			for(AslanppSymbol aSymbol : functionsVariablesSymbols) {	
				res.ref.columnVariable.add(rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, this.COLUMN_PREFIX + aSymbol.getName(), "", AslanppType.messageType));
				res.ref.columnName.add(aSymbol.getName());
			}
			res.ref.projection = new ArrayList<Integer>();
			res.ref.projection.add(0);
			res.ref.tableName = null;
		}
		res.code.add(new AslanppAssignment(selectedTable.ref.getTableSetSymbol().getReference(false, null), resultTemp.getReference(false, null)));
		
		//Optimization: remember this select -> it might be combined with another one
		if(FactorOutCursorCreationOptimisation.allCursorCreationCacheStack.size() > 0) {
			for(ProgramExpression aCursorCreation : FactorOutCursorCreationOptimisation.allCursorCreationStack.peek()) {
				if(aCursorCreation == FactorOutCursorCreationOptimisation.currentCursorCreation) {
					FactorOutCursorCreationOptimisationData optimisationData = new FactorOutCursorCreationOptimisationData();
					optimisationData.loop = theSelectionWhile;
					optimisationData.selectors.add(theIfSelector);
					FactorOutCursorCreationOptimisation.allCursorCreationCacheStack.peek().put(selectedTableName, optimisationData);
					break;
				}
			}
		}
		stackCurrentTable.pop();
		stack.push(res);
	}

	@Override
	public void visit(AllColumns arg0) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void visit(AllTableColumns arg0) {
		// TODO Auto-generated method stub			
	}

	@Override
	public void visit(SelectExpressionItem arg0) {
		arg0.getExpression().accept(this);
	}

	@Override
	public void visit(Table arg0) {
		ArrayList<AslanppType> typeComp = new ArrayList<AslanppType>();
		typeComp.add(AslanppType.natType);
		if(db.get(arg0.getName()) == null) {
			throw new RuntimeException("Unknown table " + arg0.getName());
		}
		for(int i = 0; i < db.get(arg0.getName()).size(); i++) {
			typeComp.add(AslanppType.messageType);
		}
		AslanppType type = AslanppType.getSetType(AslanppType.getTupleType(typeComp.toArray(new AslanppType[]{})));
		AslanppSymbol tableSymbol = rootVisitor.getEnvironmentEntity().searchOrAddSymbol(false, true, TABLE_PREFIX + arg0.getName(), "", type);
		Pair res = new Pair();
		res.code = new ArrayList<AslanppStatement>();
		res.ref = new AslanppMultipleReference(tableSymbol, arg0.getName(), type);
		res.ref.add("jModexID", rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, COLUMN_PREFIX + arg0.getName() + "_jModexID" , "", AslanppType.natType));
		for(String aColumnName : db.get(arg0.getName())) {
			res.ref.add(aColumnName, rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, COLUMN_PREFIX + arg0.getName() + "_" + aColumnName, "", AslanppType.messageType));
		}
		stack.push(res);
	}

	@Override
	public void visit(SubSelect arg0) {
		arg0.getSelectBody().accept(this);
	}

	@Override
	public void visit(SubJoin arg0) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(Union arg0) {
		// TODO Auto-generated method stub		
	}

	@Override
	public void visit(NullValue arg0) {
		Pair res = new Pair();
		res.expRef = new TranslatedExpression<NullValue>(arg0) {

			@Override
			public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
				if(operator != null) return null;
				return new ArrayList<AslanppStatement>();
			}
			
			@Override
			public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
				if(operator != null) return null;
				return rootVisitor.getNullReference();
			}
			
		};
		stack.push(res);
	}

	@Override
	public void visit(Function arg0) {
		// TODO Auto-generated method stub
	}

		@Override
		public void visit(InverseExpression arg0) {
			// TODO Auto-generated method stub			
		}

		@Override
		public void visit(JdbcParameter arg0) {
			// TODO Auto-generated method stub			
		}

		@Override
		public void visit(DoubleValue arg0) {
			// TODO Auto-generated method stub	
		}

		@Override
		public void visit(LongValue arg0) {
			Pair res = new Pair();
			res.expRef = new TranslatedExpression<LongValue>(arg0) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					return new ArrayList<AslanppStatement>();
				}
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					AslanppType intType = rootVisitor.getApplicationEntity().searchOrAddType("int", AslanppType.messageType, false);
					AslanppReference theIntConstant = rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, AslanppGenerator.eliminateAnslanppTokens("i" + originalExpression.getValue()), "",intType).getReference(false,null);
					long theConstantValue = originalExpression.getValue();
					AslanppGenerator.addIntegerConstant(theConstantValue);
					return theIntConstant;
				}				
			};
			stack.push(res);
		}

		@Override
		public void visit(DateValue arg0) {
			// TODO Auto-generated method stub		
		}

		@Override
		public void visit(TimeValue arg0) {
			// TODO Auto-generated method stub		
		}

		@Override
		public void visit(TimestampValue arg0) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void visit(Parenthesis arg0) {
			if(!arg0.isNot()) {
				arg0.getExpression().accept(this);	
			} else {
				Pair res = new Pair();
				arg0.getExpression().accept(this);	
				final Pair theExp = stack.pop();
				res.expRef = new TranslatedExpression<Parenthesis>(arg0) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
						res.addAll(theExp.expRef.getRValueCode(inGuard, null, null));
						return res;
					}
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						return AslanppLogicalNegation.createInstance(theExp.expRef.getReferenceForRValueCode(inGuard, null, null));
					}
				};
				stack.push(res);
			}
		}

		@Override
		public void visit(StringValue arg0) {
			if(toStringVisitor.stringVar2leaf.containsKey(arg0.getValue())) {
				Pair res = new Pair();
				ProgramExpression pe = toStringVisitor.stringVar2leaf.get(arg0.getValue());
				TranslatedExpression te = (TranslatedExpression) pe.accept(this.rootVisitor);
				res.expRef = te;
				stack.push(res);
			} else {
				Pair res = new Pair();
				res.expRef = new TranslatedExpression<StringValue>(arg0) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						return new ArrayList<AslanppStatement>();
					}
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						if(originalExpression.equals("")) {
							return rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, AslanppGenerator.EMPTY_STRING_NAME, "", AslanppGenerator.JAVA_REFERENCE_ASLANPP).getReference(false, null);							
						} else {
							return rootVisitor.getApplicationEntity().searchOrAddSymbol(true, false, AslanppGenerator.eliminateAnslanppTokens(AslanppGenerator.CONSTANT_STRING_PREFIX + originalExpression.getValue()), "", AslanppGenerator.JAVA_REFERENCE_ASLANPP).getReference(false, null);
						}
					}
				};
				stack.push(res);
			}
		}

		@Override
		public void visit(Addition arg0) {
			Pair res = new Pair();
			arg0.getLeftExpression().accept(this);
			final TranslatedExpression translatedLeft = stack.pop().expRef;
			arg0.getRightExpression().accept(this);
			final TranslatedExpression translatedRight = stack.pop().expRef;
			res.expRef = new TranslatedExpression<Addition>(arg0) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
					res.addAll(translatedLeft.getRValueCode(inGuard, null, null));
					res.addAll(translatedRight.getRValueCode(inGuard, null, null));
					return res;
				}
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					AslanppReference l = translatedLeft.getReferenceForRValueCode(false, null, null);
					AslanppReference r = translatedRight.getReferenceForRValueCode(false, null, null);
					String ltype = l.getType().getName();
					String rtype = r.getType().getName();
					AslanppSymbol addFunction = AslanppGenerator.getIntAdd();						
					List<AslanppReference> argRefs = new ArrayList<AslanppReference>();
					if(ltype.equals("int")) {
						argRefs.add(l);
					} else {
						AslanppTuple theArgs = new AslanppTuple(l);
						argRefs.add(AslanppGenerator.getString2Int().getReference(false, theArgs));	
					}
					if(rtype.equals("int")) {
						argRefs.add(r);		
					} else {
						AslanppTuple theArgs = new AslanppTuple(r);
						argRefs.add(AslanppGenerator.getString2Int().getReference(false, theArgs));							
					}
					AslanppTuple contained = new AslanppTuple(argRefs.toArray(new AslanppReference[] {}));
					AslanppSymbolReference functionSymbolReference = addFunction.getReference(false, contained);
					return functionSymbolReference;
				}			
			};
			stack.push(res);
		}

		@Override
		public void visit(Division arg0) {
			throw new RuntimeException("visit(Division arg0) not implemented");												
		}

		@Override
		public void visit(Multiplication arg0) {
			throw new RuntimeException("visit(Multiplication arg0) not implemented");									
		}

		@Override
		public void visit(Subtraction arg0) {
			throw new RuntimeException("visit(Subtraction arg0) not implemented");						
		}

		@Override
		public void visit(AndExpression arg0) {
			Pair res = new Pair();
			arg0.getLeftExpression().accept(this);
			final Pair l = stack.pop();
			arg0.getRightExpression().accept(this);
			final Pair r = stack.pop();
			res.expRef = new TranslatedExpression<AndExpression>(arg0) {

				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
					res.addAll(l.expRef.getRValueCode(inGuard, null, null));
					res.addAll(r.expRef.getRValueCode(inGuard, null, null));
					return res;
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					return AslanppLogicalConjunction.createInstance(l.expRef.getReferenceForRValueCode(inGuard, null, null), r.expRef.getReferenceForRValueCode(inGuard, null, null));
				}

			};
			stack.push(res);
		}

		@Override
		public void visit(OrExpression arg0) {
			Pair res = new Pair();
			arg0.getLeftExpression().accept(this);
			final Pair l = stack.pop();
			arg0.getRightExpression().accept(this);
			final Pair r = stack.pop();
			res.expRef = new TranslatedExpression<OrExpression>(arg0) {

				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
					res.addAll(l.expRef.getRValueCode(inGuard, null, null));
					res.addAll(r.expRef.getRValueCode(inGuard, null, null));
					return res;
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					AslanppReference refL = l.expRef.getReferenceForRValueCode(inGuard, null, null);
					AslanppReference refR = r.expRef.getReferenceForRValueCode(inGuard, null, null);
					return AslanppLogicalDisjunction.createInstance(refL,refR);
				}

			};
			stack.push(res);			
		}

		@Override
		public void visit(Between arg0) {
			throw new RuntimeException("visit(Between arg0) not implemented");			
		}

		@Override
		public void visit(EqualsTo arg0) {
			Pair res = new Pair();
			arg0.getLeftExpression().accept(this);
			final Pair l = stack.pop();
			arg0.getRightExpression().accept(this);
			final Pair r = stack.pop();
			res.expRef = new TranslatedExpression<EqualsTo>(arg0) {

				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					List<AslanppStatement> code = l.expRef.getRValueCode(inGuard, Operator.EQ, r.expRef);
					if(code != null) {
						return code;
					}
					code = r.expRef.getRValueCode(inGuard, Operator.EQ, l.expRef);
					if(code != null) {
						return code;
					}
					List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
					res.addAll(l.expRef.getRValueCode(inGuard, null, null));			
					res.addAll(r.expRef.getRValueCode(inGuard, null, null));
					return res;
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					AslanppReference res = l.expRef.getReferenceForRValueCode(inGuard, Operator.EQ, r.expRef);
					if(res != null) {
						return res;
					}
					res = r.expRef.getReferenceForRValueCode(inGuard, Operator.EQ, l.expRef);
					if(res != null) {
						return res;
					}
					return new AslanppEquality(l.expRef.getReferenceForRValueCode(inGuard, null, null), r.expRef.getReferenceForRValueCode(inGuard, null, null));
				}

			};
			stack.push(res);
		}

		@Override
		public void visit(GreaterThan arg0) {
			throw new RuntimeException("visit(GreaterThan arg0) not implemented");
		}

		@Override
		public void visit(GreaterThanEquals arg0) {
			throw new RuntimeException("visit(GreaterThanEquals arg0) not implemented");
		}

		@Override
		public void visit(final InExpression arg0) {
			Pair res = new Pair();
			arg0.getLeftExpression().accept(this);
			final Pair l = stack.pop();
			arg0.getItemsList().accept(this);
			final Pair r = stack.pop();
			res.expRef = new TranslatedExpression<InExpression>(arg0) {				
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
					res.addAll(l.expRef.getRValueCode(inGuard, null, null));
					res.addAll(r.code);
					return res;
				}
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					AslanppReference[] theTestedValue = new AslanppReference[r.ref.getNumberOfColumns()];
					ArrayList<Integer> projectedColumns = r.ref.getProjectedColumns();
					for(int i = 0; i < r.ref.getNumberOfColumns(); i++) {
						theTestedValue[i] = new AslanppStringStatement("?",false);
					}
					if(projectedColumns.size() == 1) {
						theTestedValue[projectedColumns.get(0)] = l.expRef.getReferenceForRValueCode(inGuard, null, null);
					} else {
						throw new RuntimeException("SQL IN expression is curently handled only for single values");
					}
					if(!arg0.isNot()) {
						return new AslanppContains(
							r.ref.cursorSet.getReference(false,null),
							new AslanppTuple(theTestedValue), true);
					} else {
						return AslanppLogicalNegation.createInstance(new AslanppContains(
								r.ref.cursorSet.getReference(false,null),
								new AslanppTuple(theTestedValue), true));
					}
				}
			};
			stack.push(res);
		}

		@Override
		public void visit(ExpressionList arg0) {
			throw new RuntimeException("visit(ExpressionList arg0) not implemented");
		}

		@Override
		public void visit(IsNullExpression arg0) {
			throw new RuntimeException("visit(IsNullExpression arg0) not implemented");
		}

		@Override
		public void visit(LikeExpression arg0) {
			throw new RuntimeException("visit(LikeExpression arg0) not implemented");
		}

		@Override
		public void visit(MinorThan arg0) {
			throw new RuntimeException("visit(MinorThan arg0) not implemented");			
		}

		@Override
		public void visit(MinorThanEquals arg0) {
			throw new RuntimeException("visit(MinorThanEquals arg0) not implemented");			
		}

		@Override
		public void visit(NotEqualsTo arg0) {
			Pair res = new Pair();
			arg0.getLeftExpression().accept(this);
			final Pair l = stack.pop();
			arg0.getRightExpression().accept(this);
			final Pair r = stack.pop();
			res.expRef = new TranslatedExpression<NotEqualsTo>(arg0) {

				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					List<AslanppStatement> code = l.expRef.getRValueCode(inGuard, Operator.NE, r.expRef);
					if(code != null) {
						return code;
					}
					code = r.expRef.getRValueCode(inGuard, Operator.NE, l.expRef);
					if(code != null) {
						return code;
					}
					List<AslanppStatement> res = super.getRValueCode(inGuard, null, null);
					res.addAll(l.expRef.getRValueCode(inGuard, null, null));
					res.addAll(r.expRef.getRValueCode(inGuard, null, null));
					return res;
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					AslanppReference res = l.expRef.getReferenceForRValueCode(inGuard, Operator.NE, r.expRef);
					if(res != null) {
						return res;
					}
					res = r.expRef.getReferenceForRValueCode(inGuard, Operator.NE, l.expRef);
					if(res != null) {
						return res;
					}
					return new AslanppInequality(l.expRef.getReferenceForRValueCode(inGuard, null, null),r.expRef.getReferenceForRValueCode(inGuard, null, null));
				}

			};
			stack.push(res);
		}

		@Override
		public void visit(Column arg0) {
			if(toStringVisitor.stringVar2leaf.containsKey(arg0.getColumnName())) {
				Pair res = new Pair();
				ProgramExpression pe = toStringVisitor.stringVar2leaf.get(arg0.getColumnName());
				TranslatedExpression te = (TranslatedExpression) pe.accept(this.rootVisitor);
				res.expRef = te;
				stack.push(res);
			} else {
				Pair res = new Pair();
				res.expRef = new TranslatedExpression<Column>(arg0) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) {return null;}
						return new ArrayList<AslanppStatement>();
					}
					
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) {return null;}
						String name = originalExpression.getColumnName();
						if(originalExpression.getTable().getName() != null) {
							name = originalExpression.getTable().getName() + "_" + name;
							return rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, COLUMN_PREFIX + name, "", AslanppType.messageType).getReference(false, null);
						} else {
							return stackCurrentTable.peek().getColumnSymbol(stackCurrentTable.peek().lookupColumnSymbol4SimpleColumnName(name)).getReference(false, null);
						}
					}
					
					@Override
					public List<AslanppStatement> getLValueCode(TranslatedExpression exp) {
						List<AslanppStatement> res = super.getLValueCode(exp);
						res.addAll(exp.getRValueCode(false, null, null));
						String name = originalExpression.getColumnName();
						AslanppReference theSymbolColumnReference = null;
						if(originalExpression.getTable().getName() != null) {
							name = originalExpression.getTable().getName() + "_" + name;
							theSymbolColumnReference = rootVisitor.getApplicationEntity().searchOrAddSymbol(false, true, COLUMN_PREFIX + name, "", AslanppType.messageType).getReference(false, null);
						} else {
							theSymbolColumnReference = stackCurrentTable.peek().getColumnSymbol(stackCurrentTable.peek().lookupColumnSymbol4SimpleColumnName(name)).getReference(false, null);
							if(theSymbolColumnReference == null) {
								throw new RuntimeException("Cannot identify the table for column reference " + name);
							}
						}
						res.add(new AslanppAssignment(theSymbolColumnReference,exp.getReferenceForRValueCode(false, null, null)));
						return res;
					}
					
				};
				stack.push(res);				
			}
		}

		@Override
		public void visit(CaseExpression arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(WhenClause arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(ExistsExpression arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(AllComparisonExpression arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(AnyComparisonExpression arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(Concat arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(Matches arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(BitwiseAnd arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(BitwiseOr arg0) {
			// TODO Auto-generated method stub
		}

		@Override
		public void visit(BitwiseXor arg0) {
			// TODO Auto-generated method stub
		}
	
	private iSummarize2String toStringVisitor = new iSummarize2String();

	private class iSummarize2String extends ExpressionAcyclicVisitor implements ProgramRequestParameterVisitor, ProgramSessionAttributeVisitor, ProgramDbRValueVisitor {

		private String introduceArguments(ProgramExpression theStatement,List<ProgramExpression> argsPositions, List<ProgramExpression> argsValues) {
			if(argsPositions.size() == 0) {
				return (String)theStatement.accept(this);
			}
			String firstString = (String)theStatement.accept(this);
			if(firstString.indexOf('?') == -1) {
				return firstString;
			}
			ProgramExpression myExpression = null;
			StringBuilder stm = new StringBuilder();
			int counter = 0;
			for(int i = 0; i < firstString.length(); i++) {
				if(firstString.charAt(i) == '?') {
					counter++;
					boolean found = false;
					for(int j = 0; j < argsPositions.size(); j++) {
						ProgramExpression positionExpr = argsPositions.get(j);
						if(positionExpr instanceof ProgramIndexExpressionConstant && 
								((ProgramIndexExpressionConstant) positionExpr).getIntConstant() == counter) {
							if(myExpression == null) {
								myExpression = new ProgramStringBinaryExpression(ProgramStringBinaryExpression.StringBinaryOperator.CONCAT, 
										new ProgramIndexExpressionConstant("\"" + stm.toString() + "\""), 
										argsValues.get(j));
							} else {
								myExpression = new ProgramStringBinaryExpression(ProgramStringBinaryExpression.StringBinaryOperator.CONCAT, 
										myExpression, 
										new ProgramStringBinaryExpression(ProgramStringBinaryExpression.StringBinaryOperator.CONCAT,
												new ProgramIndexExpressionConstant("\"" + stm.toString() + "\""),
												argsValues.get(j)));
							}
							stm = new StringBuilder();
							found = true;
							break;
						}
					}
					if(!found) {
						throw new RuntimeException("Cannot model SQL statements with ? arguments when they are set unsing non-constant values!");
					}
				} else {
					stm.append(firstString.charAt(i));
				}
			}
			if(stm.length() != 0) {
				myExpression = new ProgramStringBinaryExpression(ProgramStringBinaryExpression.StringBinaryOperator.CONCAT, 
						myExpression, 
						new ProgramIndexExpressionConstant("\"" + stm.toString() + "\""));				
			}
			return (String)myExpression.accept(this);
		}

		public String process(ProgramExpression statement, List<ProgramExpression> argsPositions, List<ProgramExpression> argsValues) {
			return introduceArguments(statement, argsPositions, argsValues);
		}

		private int counter = 0;
		private final String VARIABLE = "inferredVariableInSQLStatement";
		private HashMap<ProgramExpression, String> leaft2StringVar = new HashMap<ProgramExpression, String>();
		private HashMap<String,ProgramExpression> stringVar2leaf = new HashMap<String,ProgramExpression>();
		
		public void reset() {
			counter = 0;
			leaft2StringVar.clear();
		}
		
		@Override
		public Object visitProgramIndexExpression(ProgramIndexExpression expression) {
			if(expression.isStringConstant()) {
				return expression.getStringConstant().substring(1,expression.getStringConstant().length() - 1);
			}
			return null;
		}

		@Override
		public Object visitProgramStringBinaryExpression(ProgramStringBinaryExpression expression) {
			if(expression.getOperator() == StringBinaryOperator.CONCAT) {
				Object l = expression.getLeft().accept(this);
				Object r = expression.getRight().accept(this);
				if(l instanceof String && r instanceof String) {
					return l.toString() + r.toString(); 
				}
			}
			return null;
		}

		@Override
		public Object visitProgramRequestParameter(ProgramRequestParameter expression) {
			if(!leaft2StringVar.containsKey(expression)) {
				leaft2StringVar.put(expression, VARIABLE+counter);
				stringVar2leaf.put(VARIABLE+counter, expression);
				counter++;
			}
			return leaft2StringVar.get(expression);
		}

		@Override
		public Object visitProgramSessionAttribute(ProgramSessionAttribute expression) {
			if(!leaft2StringVar.containsKey(expression)) {
				leaft2StringVar.put(expression, VARIABLE+counter);
				stringVar2leaf.put(VARIABLE+counter, expression);
				counter++;
			}
			return leaft2StringVar.get(expression);
		}

		@Override
		public Object visitProgramFunction(ProgramFunction expression) {
			if(!leaft2StringVar.containsKey(expression)) {
				leaft2StringVar.put(expression, VARIABLE+counter);
				stringVar2leaf.put(VARIABLE+counter, expression);
				counter++;
			}
			return leaft2StringVar.get(expression);
		}

		@Override
		public Object visitProgramDbRValue(ProgramDbRValue expression) {
			if(!leaft2StringVar.containsKey(expression)) {
				leaft2StringVar.put(expression, VARIABLE+counter);
				stringVar2leaf.put(VARIABLE+counter, expression);
				counter++;
			}
			return leaft2StringVar.get(expression);
		}

		@Override
		public Object visitProgramField(ProgramField expression) {
			if(!leaft2StringVar.containsKey(expression)) {
				leaft2StringVar.put(expression, VARIABLE+counter);
				stringVar2leaf.put(VARIABLE+counter, expression);
				counter++;
			}
			return leaft2StringVar.get(expression);
		}

		@Override
		public Object visitProgramConversionExpression(ProgramConversionExpression expression) {
			if(!leaft2StringVar.containsKey(expression)) {
				leaft2StringVar.put(expression, VARIABLE+counter);
				stringVar2leaf.put(VARIABLE+counter, expression);
				counter++;
			}
			return leaft2StringVar.get(expression);
		}

	}

}