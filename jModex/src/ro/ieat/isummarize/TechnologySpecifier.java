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

import java.util.List;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;

public abstract class TechnologySpecifier {

	private TechnologySpecifier next = null;

	protected TechnologySpecifier(TechnologySpecifier next) {
		this.next = next;
	}

	public final List<Entrypoint> handleGetEntrypoints(ClassHierarchy ch) {
		List<Entrypoint> res = getEntrypoints(ch);
		if(next != null) {
			res.addAll(next.handleGetEntrypoints(ch));
		}
		return res;
	}

	public final boolean handleReplaceWithAbstractVariable (SSAInvokeInstruction instruction, int instructionIndex, SymbolTable symTable, SSACFG currentCFG, List<MethodPath> res, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, MethodSummaryAlgorithm runningAlgorithm) {
		boolean result = replaceWithAbstractVariable(instruction, instructionIndex, symTable, currentCFG, res, node, pointsTo, tInference, runningAlgorithm);
		if(!result) {
			if(next != null) {
				result = next.handleReplaceWithAbstractVariable(instruction, instructionIndex, symTable, currentCFG, res, node, pointsTo, tInference, runningAlgorithm);
			} else {
				result = false;
			}
		}
		return result;
	}

	protected abstract List<Entrypoint> getEntrypoints(ClassHierarchy ch);

	protected abstract boolean replaceWithAbstractVariable(SSAInvokeInstruction instruction, int instructionIndex, SymbolTable symTable, SSACFG currentCFG, List<MethodPath> res, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, MethodSummaryAlgorithm runningAlgorithm);

	protected final void init(MethodSummaryAlgorithm rootRunningAlgorithm) {
		handleInit(rootRunningAlgorithm);
		if(next != null) {
			next.init(rootRunningAlgorithm);
		}
	}
	
	protected void handleInit(MethodSummaryAlgorithm rootRunningAlgorithm) {}

}