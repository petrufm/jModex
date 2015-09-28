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
package ro.ieat.isummarize.collections;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

import ro.ieat.isummarize.MethodPath;
import ro.ieat.isummarize.MethodSummaryAlgorithm;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.TechnologySpecifier;
import ro.ieat.isummarize.collections.ProgramCollectionAction.CollectionActions;

public class CollectionTechnologySpecifier extends TechnologySpecifier{

	public CollectionTechnologySpecifier(TechnologySpecifier next) {
		super(next);
	}

	@Override
	protected List<Entrypoint> getEntrypoints(ClassHierarchy ch) {
		return new ArrayList<Entrypoint>();
	}

	@Override
	protected boolean replaceWithAbstractVariable(SSAInvokeInstruction instruction, int instructionIndex, SymbolTable symTable, SSACFG currentCFG, List<MethodPath> res, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, MethodSummaryAlgorithm runningAlgorithm) {
		MethodReference called = instruction.getDeclaredTarget();
		TypeReference declaringType = called.getDeclaringClass();
		boolean isCollectionMethod = false;
		if(declaringType.isClassType()) {
			IClass theClass = node.getClassHierarchy().lookupClass(declaringType);
			if(theClass != null) {
				for(IClass aClass : theClass.getAllImplementedInterfaces()) {
					if(aClass.isInterface() && aClass.getName().toString().equals("Ljava/util/Collection")) {
						isCollectionMethod = true;
					}
				}
			}
		}
		if(!isCollectionMethod) {
			return false;
		}
		ModelFactory theModelFactory = runningAlgorithm.getModelFactory();
		IR theIR = runningAlgorithm.getCurrentIR();		
		if (called.getName().toString().equals("add")) {
			ProgramCollectionAction newExp = 
					new ProgramCollectionAction(
							CollectionActions.ADD,
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)),
							theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1))							
					);
			ProgramCollections pc = new ProgramCollections();
	  		for(MethodPath mp : res) {
	  			mp.add(pc, newExp);
			}
	    	return true;
		}
		return false;
	}


}
