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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;

import ro.ieat.isummarize.MethodPath;
import ro.ieat.isummarize.MethodSummaryAlgorithm;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.ProgramConversionExpression;
import ro.ieat.isummarize.ProgramIndexExpression;
import ro.ieat.isummarize.TechnologySpecifier;

public class JavaSqlTechnologySpecifier extends TechnologySpecifier implements Observer {

	public JavaSqlTechnologySpecifier(TechnologySpecifier next) {
		super(next);
	}

	@Override
	protected List<Entrypoint> getEntrypoints(ClassHierarchy ch) {
		return new ArrayList<Entrypoint>();
	}

	@Override
	protected boolean replaceWithAbstractVariable(SSAInvokeInstruction instruction, int instructionIndex, SymbolTable symTable, SSACFG currentCFG, List<MethodPath> res, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, MethodSummaryAlgorithm runningAlgorithm) {
		MethodReference called = instruction.getDeclaredTarget();
		ModelFactory theModelFactory = runningAlgorithm.getModelFactory();
		IR theIR = runningAlgorithm.getCurrentIR();		
		if (called.getName().toString().equals("getString") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/ResultSet") &&
				called.getDescriptor().toString().equals("(I)Ljava/lang/String;")) {
					ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramDbRValue newExp = new ProgramDbRValue(dbBuilder,
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)),
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0))
					);
			  		for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("getString") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/ResultSet") &&
				called.getDescriptor().toString().equals("(Ljava/lang/String;)Ljava/lang/String;")) {
					ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramDbRValue newExp = new ProgramDbRValue(dbBuilder,
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)),
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0))
					);
			  		for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("getInt") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/ResultSet") &&
				called.getDescriptor().toString().equals("(Ljava/lang/String;)I")) {
					ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramConversionExpression newExp = theModelFactory.createProgramConversionExpressionSTRING2INT(
							new ProgramDbRValue(dbBuilder,
									theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)),
									theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0))
									)
							);
			  		for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("getLong") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/ResultSet") &&
				(called.getDescriptor().toString().equals("(I)J") || called.getDescriptor().toString().equals("(Ljava/lang/String;)J"))) {
					ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramConversionExpression newExp = theModelFactory.createProgramConversionExpressionSTRING2INT(
							new ProgramDbRValue(dbBuilder,
									theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)),
									theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0))
									)
							);
			  		for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("executeQuery") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/Statement") &&
				called.getDescriptor().toString().equals("(Ljava/lang/String;)Ljava/sql/ResultSet;")) {
					ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramSQLCursorAction newExp = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE, theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));
			  		for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("executeQuery") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/PreparedStatement") &&
				called.getDescriptor().toString().equals("()Ljava/sql/ResultSet;")) {
					ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramSQLCursorAction newExp = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE, theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
			  		for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("setInt") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/PreparedStatement") &&
				called.getDescriptor().toString().equals("(II)V")) {
					ProgramSQLCursorAction toSub = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE, theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
					ProgramSQLCursorAction newExp = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE, theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
					newExp.addCursorCreationParameter(
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)), 
						theModelFactory.createProgramConversionExpressionINT2STRING(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(2)))
					);
					for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("setString") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/PreparedStatement") &&
				called.getDescriptor().toString().equals("(ILjava/lang/String;)V")) {
					ProgramSQLCursorAction toSub = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE, theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
					ProgramSQLCursorAction newExp = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE, theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
					newExp.addCursorCreationParameter(
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)), 
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(2))
					);
					for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("prepareStatement") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/Connection") &&
				called.getDescriptor().toString().equals("(Ljava/lang/String;II)Ljava/sql/PreparedStatement;")) {
					ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1));
			  		for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
					}
			    	return true;
		}
		if (called.getName().toString().equals("next") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/ResultSet") &&
				called.getDescriptor().toString().equals("()Z")) {
					ProgramIndexExpression toSubOne = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
					ProgramSQLCursorAction newExpOne = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_NEXT,
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
					ProgramIndexExpression toSubTwo = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramSQLCursorAction newExpTwo = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_HAS_NEXT,
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
			  		for(MethodPath mp : res) {
						mp.substitute(toSubOne, newExpOne);
						mp.substitute(toSubTwo, newExpTwo);
					}
			    	return true;
		}
		if (called.getName().toString().equals("first") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/ResultSet") &&
				called.getDescriptor().toString().equals("()Z")) {
					//TODO:What if the cursor has been consumed ?
					ProgramIndexExpression toSubTwo = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
					ProgramSQLCursorAction newExpTwo = new ProgramSQLCursorAction(dbBuilder, ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_HAS_NEXT,
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
			  		for(MethodPath mp : res) {
						mp.substitute(toSubTwo, newExpTwo);
					}
			    	return true;
		}
		if (called.getName().toString().equals("executeUpdate") &&
				called.getDeclaringClass().getName().toString().equals("Ljava/sql/Statement") &&
				called.getDescriptor().toString().equals("(Ljava/lang/String;)I")) {
					ProgramDataBase toSub = new ProgramDataBase();
					ProgramDataBaseModification newExp = new ProgramDataBaseModification(dbBuilder, new ProgramDataBase(), theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));
			  		for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
			  			mp.add(toSub,newExp);
					}
			    	return true;
		}

		return false;
	}
	
	private SQLDataBaseModel dbBuilder;
	
	protected void handleInit(MethodSummaryAlgorithm rootRunningAlgorithm) {
		rootRunningAlgorithm.addObserver(this);
	}

	@Override
	public void update(Observable o, Object arg) {
		if(arg == MethodSummaryAlgorithm.EVENT.BEFORESTART) {
			dbBuilder = new SQLDataBaseModel(dbStructure);
		}
	}
	
	private HashMap<String,List<String>> dbStructure;
	
	public void setDescriptionFile(File file) {
		try {
			this.dbStructure = new HashMap<String,List<String>>();
			BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(file.getAbsolutePath())));
			String line;
			while((line = bf.readLine()) != null) {
				line = line.trim();
				if(line.startsWith("%") || line.equals("")) continue;
				String tokens[] = line.trim().split(":");
				if(tokens.length < 2) {
					bf.close();
					throw new RuntimeException("Incorect file containing the DB structure!");
				}
				List<String> columns = new ArrayList<String>();
				for(int i = 1; i < tokens.length; i++) {
					columns.add(tokens[i]);
				}
				dbStructure.put(tokens[0], columns);
			}
			bf.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
