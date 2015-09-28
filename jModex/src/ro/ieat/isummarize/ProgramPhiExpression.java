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
import java.util.HashSet;
import java.util.List;
import java.util.Set;


import com.ibm.wala.ssa.SSAPhiInstruction;

public class ProgramPhiExpression extends ProgramExpression {

	private SSAPhiInstruction instruction;
	private List<ProgramIndexExpression> allUses;
	
	public ProgramPhiExpression(SSAPhiInstruction instruction, List<ProgramIndexExpression> allUses) {
		this.instruction = instruction;
		this.allUses = new ArrayList<ProgramIndexExpression>();
		this.allUses.addAll(allUses);
	}
	
	@Override
	public ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression) {
		if(sub instanceof ProgramIndexExpression) {
			for(int i = 0; i < instruction.getNumberOfUses(); i++) {
				if(((ProgramIndexExpression)sub).getUse() == instruction.getUse(i)) {
					return newExpression;		
				}
			}
		}
		return this;
	}

	@Override
	public String toString() {
		return instruction + "";
	}

	@Override
	public boolean structuralEquals(ProgramExpression o) {
		if(o instanceof ProgramPhiExpression) {
			return this.getAllUses().equals(((ProgramPhiExpression) o).getAllUses());
		}
		return false;
	}
	
	private Set<Integer> getAllUses() {
		Set<Integer> res = new HashSet<Integer>();
		for(int i = 0; i < instruction.getNumberOfUses(); i++) {
			res.add(instruction.getUse(i));
		}
		return res;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		for(ProgramExpression aUse : allUses) {
			result.addAll(aUse.getVariables());
		}
		return result;
	}

	@Override
	public Object accept(ExpressionAcyclicVisitor visitor) {
		return visitor.visitProgramPhiExpression(this);
	}

	@Override
	protected int getHashCode() {
		return 37 * getAllUses().hashCode();
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return new ArrayList<ProgramExpressionProxy>();
	}

}
