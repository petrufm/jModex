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

public class ProgramIndexExpressionConstant extends ProgramIndexExpression {

	enum CONSTANT_TYPE {STRING_CONSTANT, NULL_CONSTANT, INT_CONSTANT, LONG_CONSTANT, FLOAT_CONSTANT, DOUBLE_CONSTANT};
		
	private CONSTANT_TYPE type;
	private String constString;
	private Number constNumeric;
		
	public ProgramIndexExpressionConstant(String constString, int use) {
		super(use);
		type = CONSTANT_TYPE.STRING_CONSTANT;
		this.constString = constString;
	}

	public ProgramIndexExpressionConstant(Double constNumeric, int use) {
		super(use);
		type = CONSTANT_TYPE.DOUBLE_CONSTANT;
		this.constNumeric = constNumeric;
	}

	public ProgramIndexExpressionConstant(Float constNumeric, int use) {
		super(use);
		type = CONSTANT_TYPE.FLOAT_CONSTANT;
		this.constNumeric = constNumeric;
	}

	public ProgramIndexExpressionConstant(Integer constNumeric, int use) {
		super(use);
		type = CONSTANT_TYPE.INT_CONSTANT;
		this.constNumeric = constNumeric;
	}

	public ProgramIndexExpressionConstant(Long constNumeric, int use) {
		super(use);
		type = CONSTANT_TYPE.LONG_CONSTANT;
		this.constNumeric = constNumeric;
	}

	public ProgramIndexExpressionConstant(int use) {
		super(use);
		type = CONSTANT_TYPE.NULL_CONSTANT;
	}

	public ProgramIndexExpressionConstant(String constString) {
		super(Integer.MIN_VALUE);
		type = CONSTANT_TYPE.STRING_CONSTANT;
		this.constString = constString;
	}

	public ProgramIndexExpressionConstant(Double constNumeric) {
		super(Integer.MIN_VALUE);
		type = CONSTANT_TYPE.DOUBLE_CONSTANT;
		this.constNumeric = constNumeric;
	}

	public ProgramIndexExpressionConstant(Integer constNumeric) {
		super(Integer.MIN_VALUE);
		type = CONSTANT_TYPE.INT_CONSTANT;
		this.constNumeric = constNumeric;
	}

	public ProgramIndexExpressionConstant(Long constNumeric) {
		super(Integer.MIN_VALUE);
		type = CONSTANT_TYPE.LONG_CONSTANT;
		this.constNumeric = constNumeric;
	}

	public ProgramIndexExpressionConstant() {
		super(Integer.MIN_VALUE);
		type = CONSTANT_TYPE.NULL_CONSTANT;
	}

	@Override
	protected int getHashCode() {
		if(type == CONSTANT_TYPE.STRING_CONSTANT) {
			return constString.hashCode();
		}
		if(type == CONSTANT_TYPE.NULL_CONSTANT) {
			return "null".hashCode();
		}
		return constNumeric.hashCode();			
	}

	@Override
	public String toString() {
			if(type == CONSTANT_TYPE.STRING_CONSTANT) {
				return constString;
			}
			if(type == CONSTANT_TYPE.NULL_CONSTANT) {
				return "null";
			}
			return constNumeric + "";
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof ProgramIndexExpressionConstant && ((ProgramIndexExpressionConstant)o).type == type) {
			if(type == CONSTANT_TYPE.NULL_CONSTANT) {
				return true;
			}
			if(type == CONSTANT_TYPE.STRING_CONSTANT) {
				return constString.equals(((ProgramIndexExpressionConstant)o).constString);
			}
			return constNumeric.equals(((ProgramIndexExpressionConstant)o).constNumeric);
		}
		return false;
	}

	@Override
	public Set<ProgramExpression> getVariables() {
		return new HashSet<ProgramExpression>();
	}

	@Override
	public Set<ProgramExpression> getSubVariables() {
		return new HashSet<ProgramExpression>();
	}

	@Override
	public  boolean isStringConstant() {
		return type == CONSTANT_TYPE.STRING_CONSTANT;
	}

	@Override
	public String getStringConstant() {
		return isStringConstant() ? constString : null;
	}

	@Override
	public boolean isNullConstant() {
		return type == CONSTANT_TYPE.NULL_CONSTANT;
	}

	@Override
	public boolean isIntConstant() {
		return type == CONSTANT_TYPE.INT_CONSTANT;
	}

	@Override
	public Integer getIntConstant() {
		return isIntConstant() ? (Integer)constNumeric : null;
	}

	@Override
	public boolean isLongConstant() {
		return type == CONSTANT_TYPE.LONG_CONSTANT;
	}

	@Override
	public Long getLongConstant() {
		return isLongConstant() ? (Long)constNumeric : null;
	}

	@Override
	public boolean isFloatConstant() {
		return type == CONSTANT_TYPE.FLOAT_CONSTANT;
	}

	@Override
	public Float getFloatConstant() {
		return isFloatConstant() ? (Float)constNumeric : null;
	}

	@Override
	public boolean isDoubleConstant() {
		return type == CONSTANT_TYPE.DOUBLE_CONSTANT;
	}

	@Override
	public Double getDoubleConstant() {
		return isDoubleConstant() ? (Double)constNumeric : null;
	}

	@Override
	public boolean isNumberConstant() {
		return isIntConstant() || isLongConstant() || isFloatConstant() || isDoubleConstant();
	}

	@Override
	public Number getNumberConstant() {
		return isNumberConstant() ? this.constNumeric : null;
	}

	@Override
	public String getDeclaredTypeName() {
		if(type == CONSTANT_TYPE.INT_CONSTANT) {
			return Utils.getFullQualifiedTypeNameFromBinaryName("I");
		} else if(type == CONSTANT_TYPE.DOUBLE_CONSTANT) {
			return Utils.getFullQualifiedTypeNameFromBinaryName("D");
		} else if(type == CONSTANT_TYPE.FLOAT_CONSTANT) {
			return Utils.getFullQualifiedTypeNameFromBinaryName("F");				
		} else if(type == CONSTANT_TYPE.LONG_CONSTANT) {
			return Utils.getFullQualifiedTypeNameFromBinaryName("J");				
		} else if(type == CONSTANT_TYPE.STRING_CONSTANT) {
			return Utils.getFullQualifiedTypeNameFromBinaryName("Ljava/lang/String;");
		} else {
			return "null";
		}
	}

	@Override
	public List<ProgramExpressionProxy> getProxies() {
		return new ArrayList<ProgramExpressionProxy>();
	}

}
