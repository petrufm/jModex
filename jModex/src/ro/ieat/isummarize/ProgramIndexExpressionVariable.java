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

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SymbolTable;

public class ProgramIndexExpressionVariable extends ProgramIndexExpression {

	private SSACFG ssacfg;
	private int pc;
	private SymbolTable symTable;
	private TypeInference ti;

	public ProgramIndexExpressionVariable(TypeInference ti, int use, SSACFG ssacfg, int pc, SymbolTable symTable) {
		super(use);
		this.ssacfg = ssacfg;
		this.pc = pc;
		this.symTable = symTable;
		this.ti = ti;
	}
		
	@Override
	public boolean equals(Object o) {
		if(o instanceof ProgramIndexExpressionVariable) {
			return ssacfg==((ProgramIndexExpressionVariable)o).ssacfg && use == ((ProgramIndexExpressionVariable)o).use;
		}
		return false;
	}
		
	@Override
	protected int getHashCode() {
		return 66 * use;			
	}

	@Override
	public String toString() {
		String name;
		if(symTable != null) {
			if(symTable.isParameter(use)) {
				name = ssacfg.getMethod().getLocalVariableName(pc, use-1);
			} else {
				name = symTable.getValueString(use) + "";
			}
			return Utils.getFullQualifiedSignaruteFromBinarySignature(ssacfg.getMethod().getSignature()) + "_" + name; 
		} else {
			return "_V" + use;
		}
	}
		
	@Override
	public Set<ProgramExpression> getVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		result.add(this);
		return result;
	}

	@Override
	public Set<ProgramExpression> getSubVariables() {
		HashSet<ProgramExpression> result = new HashSet<ProgramExpression>();
		return result;
	}

	@Override
	public  boolean isStringConstant() {
		return false;
	}

	@Override
	public String getStringConstant() {
		return null;
	}

	@Override
	public boolean isNullConstant() {
		return false;
	}

	@Override
	public boolean isIntConstant() {
		return false;
	}

	@Override
	public Integer getIntConstant() {
		return null;
	}

	@Override
	public boolean isLongConstant() {
		return false;
	}

	@Override
	public Long getLongConstant() {
		return null;
	}

	@Override
	public boolean isFloatConstant() {
		return false;
	}

	@Override
	public Float getFloatConstant() {
		return null;
	}

	@Override
	public boolean isDoubleConstant() {
		return false;
	}

	@Override
	public Double getDoubleConstant() {
		return null;
	}

	@Override
	public boolean isNumberConstant() {
		return false;
	}

	@Override
	public Number getNumberConstant() {
		return null;
	}

	@Override
	public String getDeclaredTypeName() {
		return Utils.getFullQualifiedTypeNameFromBinaryName(ti.getType(use).getTypeReference().getName().toString());
	}
	
	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return new ArrayList<ProgramExpressionProxy>();
	}

	public IClass getDeclaredClass() {
		if(ti.getType(use).getTypeReference().isClassType()) {
			return ssacfg.getMethod().getClassHierarchy().lookupClass(ti.getType(use).getTypeReference());
		} else {
			return null;
		}
	}

}
