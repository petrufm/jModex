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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

public class ModelFactory {

	private ModelFactory(){}
	private static ModelFactory instance = null;
	public static ModelFactory getInstance() {
		if(instance == null) {
			instance = new ModelFactory();
		}
		return instance;
	}
	
	//Configuration methods
	private HashMap<String,List<String>> abstractedLocalVariablesName = new HashMap<String,List<String>>();
	private HashMap<String,List<PredicateAbstractionPredicate>> localVariablesPredicates = new HashMap<String,List<PredicateAbstractionPredicate>>();
	private HashMap<String,HashMap<Integer, PredicateAbstractionPredicate>> localIndex2Predicate = new HashMap<String,HashMap<Integer, PredicateAbstractionPredicate>>();
	
	public ModelFactory reset() {
		abstractedLocalVariablesName.clear();
		localVariablesPredicates.clear();
		localIndex2Predicate.clear();
		return this;
	}
	
	public void loadAbstractedVariables(File fileDeclaringAbstractedLocals) {
		try {
			BufferedReader bf = new BufferedReader(new InputStreamReader(new FileInputStream(fileDeclaringAbstractedLocals)));
			String aLine;
			while((aLine = bf.readLine()) != null) {
				String[] args = aLine.split(":");
				if(args.length >=3) {
					if(!abstractedLocalVariablesName.containsKey(args[0])) {
						abstractedLocalVariablesName.put(args[0], new ArrayList<String>());
						localVariablesPredicates.put(args[0], new ArrayList<PredicateAbstractionPredicate>());
					}
					abstractedLocalVariablesName.get(args[0]).add(args[1]);
					Class<?> aPredicateClass = Class.forName(args[2].replace('/', '.'));
					Class<?> argsType[] = new Class[args.length - 3];
					Arrays.fill(argsType, String.class);
					String[] theArgs = new String[argsType.length];
					for(int i = 0; i < theArgs.length; i++) {
						theArgs[i] = args[i + 3].substring(1,args[i + 3].length()-1);
					}
					PredicateAbstractionPredicate thePredicate = (PredicateAbstractionPredicate) aPredicateClass.getConstructor(argsType).newInstance(theArgs);
					localVariablesPredicates.get(args[0]).add(thePredicate);
				}
			}
			bf.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e.getMessage());		
		}
	}

	//Creation methods
	public ProgramIndexExpression createProgramIndexExpression(IR currentRepresentation, TypeInference tInference, SymbolTable symTable, SSACFG ssacfg, int pc, int use) {
		if(symTable != null && symTable.isConstant(use)) {
			ProgramIndexExpressionConstant pix = null;
			if(symTable.isStringConstant(use)) {
				pix = new ProgramIndexExpressionConstant("\"" + symTable.getConstantValue(use).toString() + "\"",use);
			} else if(symTable.isNullConstant(use)) {
				pix = new ProgramIndexExpressionConstant(use);				
			} else if(symTable.isNumberConstant(use)) {
				if(symTable.isDoubleConstant(use)) pix = new ProgramIndexExpressionConstant(symTable.getDoubleValue(use),use);
				else if(symTable.isFloatConstant(use)) pix = new ProgramIndexExpressionConstant(symTable.getFloatValue(use),use);
				else if(symTable.isIntegerConstant(use)) pix = new ProgramIndexExpressionConstant(symTable.getIntValue(use),use);
				else if(symTable.isLongConstant(use)) pix = new ProgramIndexExpressionConstant(symTable.getLongValue(use),use);
			}
			return pix;
		} else {
			ProgramIndexExpression pix = new ProgramIndexExpressionVariable(tInference, use, ssacfg, pc, symTable);
			String methodName = ssacfg.getMethod().getDeclaringClass().getName().toString() + "." + ssacfg.getMethod().getName().toString() + ssacfg.getMethod().getDescriptor().toString();
			if(abstractedLocalVariablesName.containsKey(methodName)) { 
				if(!localIndex2Predicate.containsKey(methodName)) {
					localIndex2Predicate.put(methodName, new HashMap<Integer, PredicateAbstractionPredicate>());
					SSAInstruction[] instrs = ssacfg.getInstructions();
					//Create a cache for index values (for local variables) that must be abstracted
					for(int i = 0; i < instrs.length; i++) {
						if(instrs[i] == null) {
							continue;
						}
						int theLocalNumber = instrs[i].getDef();
						String localNames[] = currentRepresentation.getLocalNames(i, theLocalNumber);
						if(localNames != null) {
							List<String> nameList = abstractedLocalVariablesName.get(methodName);
							for(String aLocalName : localNames) {
								int pos = nameList.indexOf(aLocalName);
								if(pos != -1) {
									localIndex2Predicate.get(methodName).put(theLocalNumber, localVariablesPredicates.get(methodName).get(pos));
								}
							}
						}
					}
					for(int i = 0; i < ssacfg.getNumberOfNodes(); i++) {
						Iterator<SSAPhiInstruction> phiIt = ssacfg.getNode(i).iteratePhis();
						while(phiIt.hasNext()) {
							int theLocalNumber = phiIt.next().getDef();
							String localNames[] = currentRepresentation.getLocalNames(ssacfg.getNode(i).getFirstInstructionIndex(), theLocalNumber);
							if(localNames != null) {
								List<String> nameList = abstractedLocalVariablesName.get(methodName);
								for(String aLocalName : localNames) {
									int pos = nameList.indexOf(aLocalName);
									if(pos != -1) {
										localIndex2Predicate.get(methodName).put(theLocalNumber, localVariablesPredicates.get(methodName).get(pos));
									}
								}
							}
						}
					}
				}
				PredicateAbstractionPredicate tmp =localIndex2Predicate.get(methodName).get(use);
				if(tmp != null) {
					pix = createPredicateAbstractionAbstractVariable(pix, tmp);
				}
			}
			return pix;
		}		
	}

	public ProgramIndexExpression createProgramIndexExpression(String constString) {
		return new ProgramIndexExpressionConstant("\"" + constString + "\"");
	}

	public ProgramIndexExpression createProgramIndexExpression(int intConst) {
		return new ProgramIndexExpressionConstant(new Integer(intConst));
	}

	public ProgramIndexExpression createProgramIndexExpression(long longConst) {
		return new ProgramIndexExpressionConstant(new Long(longConst));
	}

	public ProgramIndexExpression createProgramIndexExpressionNull() {
		return new ProgramIndexExpressionConstant();
	}
	
	public PredicateAbstractionAbstractVariable createPredicateAbstractionAbstractVariable(TracedVariable tv, PredicateAbstractionPredicate thePredicate) {
		return new PredicateAbstractionAbstractVariable(tv, thePredicate);
	}

	public ProgramExpression createPredicateAbstractionAbstractization(ProgramExpression toAssign, PredicateAbstractionPredicate thePredicate) {
		if(toAssign instanceof PredicateAbstractionConcretization) {
			if(((PredicateAbstractionConcretization) toAssign).getExpression() instanceof PredicateAbstractionAbstractization) {
				return ((PredicateAbstractionConcretization) toAssign).getExpression();
			}
		}
		return new PredicateAbstractionAbstractization(toAssign, thePredicate);
	}

	public PredicateAbstractionConcretization createPredicateAbstractionConcretization(ProgramExpression toAssign, PredicateAbstractionPredicate thePredicate) {
		return new PredicateAbstractionConcretization(toAssign, thePredicate);
	}

	public ProgramConversionExpression createProgramConversionExpressionINT2STRING(ProgramExpression converted) {
		return new ProgramConversionExpression(ProgramConversionExpression.ConversionOperation.INT2STRING, converted);
	}

	public ProgramConversionExpression createProgramConversionExpressionSTRING2INT(ProgramExpression converted) {
		return new ProgramConversionExpression(ProgramConversionExpression.ConversionOperation.STRING2INT, converted);
	}

	public ProgramConversionExpression createProgramConversionExpressionCAST2INT(ProgramExpression converted) {
		return new ProgramConversionExpression(ProgramConversionExpression.ConversionOperation.CAST2INT, converted);
	}

	public ProgramRelationalExpression createProgramRelationalExpression(IConditionalBranchInstruction.IOperator operator, ProgramExpression left, ProgramExpression right) {
		return new ProgramRelationalExpression(operator, left, right);
	}

	public ProgramRequestParameter createProgramRequestParameter(ProgramExpression arg) {
		return new ProgramRequestParameter(arg);
	}

	public ProgramSessionAttribute createProgramSessionAttribute(ProgramExpression arg) {
		return new ProgramSessionAttribute(arg);
	}

	public ProgramStringComparison createProgramStringComparison(ProgramExpression left, ProgramExpression right) {
		return new ProgramStringComparison(left,right);
	}

	public ProgramBinaryExpression createProgramBinaryExpression(IBinaryOpInstruction.IOperator op, ProgramExpression left, ProgramExpression right) {
		return new ProgramBinaryExpression(op, left, right);
	}

	public ProgramGetObject createProgramGetObject(IClass theInstantietedClass) {
		return new ProgramGetObject(createProgramUniqueInstanceCounter(), theInstantietedClass);
	}

	public ProgramUniqueInstanceCounter createProgramUniqueInstanceCounter() {
		return new ProgramUniqueInstanceCounter();
	}

	public ProgramUniqueInstanceCounterAction createProgramUniqueInstanceCounterAction(ProgramUniqueInstanceCounterAction.Action theAction, ProgramUniqueInstanceCounter uic) {
		return new ProgramUniqueInstanceCounterAction(theAction, uic);
	}

}
