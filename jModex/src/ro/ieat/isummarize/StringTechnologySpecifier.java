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
import java.util.Collection;
import java.util.List;
import java.util.Set;

import ro.ieat.isummarize.ProgramStringBinaryExpression.StringBinaryOperator;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;

public class StringTechnologySpecifier extends TechnologySpecifier {

	public StringTechnologySpecifier(TechnologySpecifier next) {
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
		//Capture call to String.equals(Object):boolean
		if(called.getName().toString().equals("equals") &&
    			called.getDeclaringClass().getName().toString().equals("Ljava/lang/String") &&
    			called.getDescriptor().toString().equals("(Ljava/lang/Object;)Z")) {
	    		ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
    			ProgramStringComparison newExp = new ProgramStringComparison(
    					theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)),
    					theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to String.concat(String):String
		if(called.getName().toString().equals("concat") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/String") &&
			called.getDescriptor().toString().equals("(Ljava/lang/String;)Ljava/lang/String;")) {
				ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramStringBinaryExpression newExp = new ProgramStringBinaryExpression(
						ProgramStringBinaryExpression.StringBinaryOperator.CONCAT,
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)),
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to StringBuilder.StringBuilder(String)
		if(called.isInit() && 
				called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuilder") && 
				called.getDescriptor().toString().equals("(Ljava/lang/String;)V") ) {
			ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
			ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1));
	    	for(MethodPath mp : res) {
				mp.substitute(toSub, newExp);
	    	}			
			return true;
		}
		//Capture call to StringBuilder.StringBuilder()
		if(called.isInit() && 
				called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuilder") && 
				called.getDescriptor().toString().equals("()V") ) {
			ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
			ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression("");
	    	for(MethodPath mp : res) {
				mp.substitute(toSub, newExp);
	    	}			
			return true;
		}
		//Capture call to StringBuilder.append(String)
		if(called.getName().toString().equals("append") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuilder") &&
			called.getDescriptor().toString().equals("(Ljava/lang/String;)Ljava/lang/StringBuilder;")) {
				ProgramIndexExpression toSub1 = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramIndexExpression toSub2 = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
				ProgramStringBinaryExpression newExp = new ProgramStringBinaryExpression(
						ProgramStringBinaryExpression.StringBinaryOperator.CONCAT,
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)),
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)));
			   	for(MethodPath mp : res) {
					mp.substitute(toSub1, theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)));
					mp.substitute(toSub2, newExp);
			   	}
			   	return true;
		}
		//Capture call to StringBuilder.append(int)
		if(called.getName().toString().equals("append") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuilder") &&
			called.getDescriptor().toString().equals("(I)Ljava/lang/StringBuilder;")) {
				ProgramIndexExpression toSub1 = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramIndexExpression toSub2 = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
				ProgramStringBinaryExpression newExp = new ProgramStringBinaryExpression(
						ProgramStringBinaryExpression.StringBinaryOperator.CONCAT,
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)),
						theModelFactory.createProgramConversionExpressionINT2STRING(
								theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)))
						);
				for(MethodPath mp : res) {
					mp.substitute(toSub1, theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)));
					mp.substitute(toSub2, newExp);
			   	}
			   	return true;
		}
		//Capture call to StringBuilder.toString()
		if(called.getName().toString().equals("toString") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuilder")) {
				ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
			   	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
			   	}
			   	return true;
		}
		//Capture call to String.valueOf(Object):String
		if(called.getName().toString().equals("valueOf") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/String") &&
			called.getDescriptor().toString().equals("(Ljava/lang/Object;)Ljava/lang/String;")
			) {
				ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to String.toString():String
		if(called.getName().toString().equals("toString") &&
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/String") &&
			called.getDescriptor().toString().equals("()Ljava/lang/String;")
			) {
				ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to StringBuffer.StringBuffer(int)
		if(called.isInit() && 
				called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuffer") && 
				called.getDescriptor().toString().equals("(I)V") ) {
			ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
			ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1));
	    	for(MethodPath mp : res) {
				mp.substitute(toSub, newExp);
	    	}			
			return true;
		}
		//Capture call to StringBuffer.StringBuffer()
		if(called.isInit() && 
				called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuffer") && 
				called.getDescriptor().toString().equals("()V") ) {
			ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
			ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression("");
	    	for(MethodPath mp : res) {
				mp.substitute(toSub, newExp);
	    	}			
			return true;
		}
		//Capture call to StringBuffer.append(String)
		if(called.getName().toString().equals("append") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuffer") &&
			called.getDescriptor().toString().equals("(Ljava/lang/String;)Ljava/lang/StringBuffer;")) {
				ProgramIndexExpression toSub1 = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramIndexExpression toSub2 = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
				ProgramStringBinaryExpression newExp = new ProgramStringBinaryExpression(
						ProgramStringBinaryExpression.StringBinaryOperator.CONCAT,
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)),
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)));
			   	for(MethodPath mp : res) {
					mp.substitute(toSub1, newExp);
					mp.substitute(toSub2, newExp);
			   	}
			   	return true;
		}
		//Capture call to StringBuffer.append(int)
		if(called.getName().toString().equals("append") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuffer") &&
			called.getDescriptor().toString().equals("(I)Ljava/lang/StringBuffer;")) {
				ProgramIndexExpression toSub1 = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramIndexExpression toSub2 = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
				ProgramStringBinaryExpression newExp = new ProgramStringBinaryExpression(
						ProgramStringBinaryExpression.StringBinaryOperator.CONCAT,
						theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)),
						theModelFactory.createProgramConversionExpressionINT2STRING(
								theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)))
						);
			   	for(MethodPath mp : res) {
					mp.substitute(toSub1, newExp);
					mp.substitute(toSub2, newExp);
			   	}
			   	return true;
		}
		//Capture call to StringBuffer.toString()
		if(called.getName().toString().equals("toString") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/StringBuffer") &&
			called.getDescriptor().toString().equals("()Ljava/lang/String;")) {
			ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
			ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
		   	for(MethodPath mp : res) {
				mp.substitute(toSub, newExp);
		   	}
		   	return true;
		}
		//Capture call to String.length()
		if(called.getName().toString().equals("length") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/String") &&
			called.getDescriptor().toString().equals("()I")) {
			ProgramIndexExpression[] theArgs = new ProgramIndexExpression[1];
			theArgs[0] = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
			ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
			Collection<IMethod> theLengthMethods = currentCFG.getMethod().getClassHierarchy().getPossibleTargets(called);	
			ProgramFunction newExp = new ProgramFunction(theLengthMethods.iterator().next(), theArgs) {			
				public TernaryResult tryEvaluateComparison(final IOperator operator, ProgramExpression exp) {
						if(exp instanceof ProgramIndexExpression && ((ProgramIndexExpression) exp).isNumberConstant() && ((ProgramIndexExpression) exp).getNumberConstant().doubleValue() == 0) {
							Object result = args.get(0).accept(
									new ExpressionAcyclicVisitor() {						
										
										public Object visitPredicateConcretization(PredicateAbstractionConcretization expression) {
											ProgramExpression abstractExpression = expression.getExpression();
											return abstractExpression.accept(this);
										}
										
										public Object visitPredicateAbstractization(PredicateAbstractionAbstractization expression) {
											if(expression.hasKnownValue()) {
												String value = (String) expression.getKnownValue();
												if(operator == Operator.LE) {
													return value.length() <= 0 ? 1 : 0;
												}
												if(operator == Operator.NE) {
													return value.length() != 0 ? 1 : 0;
												}
											}
											return null;
										}

										public Object visitProgramIndexExpression(ProgramIndexExpression expression) {
											if(expression.isStringConstant()) {
												String theConstant = (String)expression.getStringConstant().substring(1,expression.getStringConstant().length()-1);
												if(operator == Operator.LE) {
													return theConstant.length() <= 0 ? 1 : 0;
												}
												if(operator == Operator.NE) {
													return theConstant.length() != 0 ? 1 : 0;
												}
											}
											return null;
										}
										
										public Object visitProgramStringBinaryExpression(ProgramStringBinaryExpression expression) {
											if(expression.getOperator() == StringBinaryOperator.CONCAT) {
												Object tmp1 = expression.getRight().accept(this);
												Object tmp2 = expression.getLeft().accept(this);
												if(tmp1 != null) {		
													return tmp1;
												}
												if(tmp2 != null) {
													return tmp2;
												}
											}
											return null;
										}
									});
							if(result != null) {
								return (Integer)result == 1 ? TernaryResult.TRUE : TernaryResult.FALSE;
							}
						}
					return TernaryResult.DONOTKNOW;
				}				
			};
		   	for(MethodPath mp : res) {
				mp.substitute(toSub, newExp);
		   	}
		   	return true;
		}
		//Capture call to Object.toString()
		if(called.getName().toString().equals("toString") && 
			called.getDeclaringClass().getName().toString().equals("Ljava/lang/Object") &&
			called.getDescriptor().toString().equals("()Ljava/lang/String;")) {
				ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
				ProgramExpression[] args = new ProgramExpression[instruction.getNumberOfParameters()];
				for(int i = 0; i < instruction.getNumberOfParameters(); i++) {
					args[i] = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(i));
				}
				ProgramExpressionProxy newExp = new ProgramExpressionProxy(instruction, args) {
					@Override
					public ProgramExpression resolve() {
						Set<TypeAbstraction> concreteTypes = (Set<TypeAbstraction>) getArguments()[0].getData("CONCRETE_TYPES");
						if(concreteTypes != null) {
							for(TypeAbstraction ta : concreteTypes) {
								if(!ta.getType().getName().toString().equals("Ljava/lang/String")) {
									return null;
								}
							}
						}
						return this.getArguments()[0];
					}
				};
			   	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
			   	}
			   	return true;
		}
		return false;
	}

}
