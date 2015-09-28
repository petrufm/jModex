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
import java.util.List;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.intset.OrdinalSet;

public class NumericObjectsTechnologySpecifier extends TechnologySpecifier {

	protected NumericObjectsTechnologySpecifier(TechnologySpecifier next) {
		super(next);
	}

	@Override
	protected List<Entrypoint> getEntrypoints(ClassHierarchy ch) {
		return new ArrayList<Entrypoint>();
	}

	@Override
	protected boolean replaceWithAbstractVariable(SSAInvokeInstruction instruction, int instructionIndex, SymbolTable symTable, SSACFG currentCFG, List<MethodPath> res, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, MethodSummaryAlgorithm runningAlgorithm) {
		MethodReference called = instruction.getDeclaredTarget();
		ModelFactory theModel = runningAlgorithm.getModelFactory();
		IR theIR = runningAlgorithm.getCurrentIR();		
		//Capture call to Integer.intValue():int
		if(called.getName().toString().equals("intValue") &&
    			called.getDeclaringClass().getName().toString().equals("Ljava/lang/Integer") &&
    			called.getDescriptor().toString().equals("()I")) {
	    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
	    		ProgramIndexExpression newExp = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to Integer.valueOf(int):Integer
		if(called.getName().toString().equals("valueOf") &&
    			called.getDeclaringClass().getName().toString().equals("Ljava/lang/Integer") &&
    			called.getDescriptor().toString().equals("(I)Ljava/lang/Integer;")) {
	    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
	    		ProgramIndexExpression newExp = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to Integer.Integer(String)
		if(called.isInit() &&
    			called.getDeclaringClass().getName().toString().equals("Ljava/lang/Integer") &&
    			called.getDescriptor().toString().equals("(Ljava/lang/String;)V")) {
	    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
	    		ProgramExpression newExp = 
	    			theModel.createProgramConversionExpressionSTRING2INT(theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to Integer.parseInt(String)
		if(called.getName().toString().equals("parseInt") &&
    			called.getDeclaringClass().getName().toString().equals("Ljava/lang/Integer") &&
    			called.getDescriptor().toString().equals("(Ljava/lang/String;)I")) {
	    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
	    		ProgramExpression newExp = 
	    			theModel.createProgramConversionExpressionSTRING2INT(theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to Integer.Integer(int)
		if(called.isInit() &&
    			called.getDeclaringClass().getName().toString().equals("Ljava/lang/Integer") &&
    			called.getDescriptor().toString().equals("(I)V")) {
	    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
	    		ProgramIndexExpression newExp = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to Integer.toString()
		if(called.getName().toString().equals("toString") && called.getDescriptor().toString().equals("()Ljava/lang/String;")) {
    			if(called.getDeclaringClass().getName().toString().equals("Ljava/lang/Integer")) {
		    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
		    		ProgramExpression newExp = theModel.createProgramConversionExpressionINT2STRING(theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
			    	for(MethodPath mp : res) {
						mp.substitute(toSub, newExp);
			    	}
			    	return true;
    			} else {
    				PointerKey pk = pointsTo.getHeapModel().getPointerKeyForLocal(node, instruction.getUse(0));
    				OrdinalSet<InstanceKey> ikSet = pointsTo.getPointsToSet(pk);
    				int count = 0;
    				for(InstanceKey ik : ikSet) {
    					if(ik.getConcreteType().getName().toString().equals("Ljava/lang/Integer")) {
    						count++;
    					}
    				}
    				if(count == ikSet.size() && count != 0) {
    		    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
    		    		ProgramExpression newExp = theModel.createProgramConversionExpressionINT2STRING(theModel.createProgramConversionExpressionCAST2INT(theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0))));
    			    	for(MethodPath mp : res) {
    						mp.substitute(toSub, newExp);
    			    	}
    				}
    			}
		}
		//Capture call to Integer.toString(int):String
		if(called.getName().toString().equals("toString") && called.getDescriptor().toString().equals("(I)Ljava/lang/String;")) {
			if(called.getDeclaringClass().getName().toString().equals("Ljava/lang/Integer")) {
				ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
		    	ProgramExpression newExp = theModel.createProgramConversionExpressionINT2STRING(theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)));
			    for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
			    }
			    return true;
    		}
		}
		//Capture call to Double.doubleValue():double
		if(called.getName().toString().equals("doubleValue") &&
    			called.getDeclaringClass().getName().toString().equals("Ljava/lang/Double") &&
    			called.getDescriptor().toString().equals("()D")) {
	    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
	    		ProgramIndexExpression newExp = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		//Capture call to Double.valueOf(double):Double
		if(called.getName().toString().equals("valueOf") &&
    			called.getDeclaringClass().getName().toString().equals("Ljava/lang/Double") &&
    			called.getDescriptor().toString().equals("(D)Ljava/lang/Double;")) {
	    		ProgramIndexExpression toSub = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
	    		ProgramIndexExpression newExp = theModel.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0));
		    	for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		}
		return false;
	}

}
