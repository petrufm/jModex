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

class AslanppStringStatement extends AslanppMetaEntity implements AslanppReference, AslanppStatement {

	private String stm;
	private boolean cr;

	AslanppStringStatement(String statement, boolean cr) {
		this.stm = statement;
		this.cr = cr;
	}

	private AslanppType type = null;
	
	AslanppStringStatement(String statement, boolean cr, AslanppType type) {
		this(statement,cr);
		this.type = type;
	}

	String getStatement() {
		return stm;
	}

	public void print(int tabs, StringBuilder str) {
		super.print(tabs, str);
		str.append(stm);
		if(cr) {
			str.append(";\n");
		}
	}

	@Override
	public AslanppType getType() {
		return type;
	}
		
	public String toString() {
		return stm;
	}
	
	@Override
	public boolean isTheSame(AslanppExpression ref) {
		if(ref instanceof AslanppStringStatement) {
			return stm.equals(((AslanppStringStatement) ref).stm) && cr == ((AslanppStringStatement)ref).cr;
		}
		return false;
	}

	@Override
	public boolean isTheSame(AslanppStatement stm) {
		if(stm instanceof AslanppStringStatement) {
			return stm.equals(((AslanppStringStatement) stm).stm);
		}
		return false;
	}

	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppStringStatement(this);
	}

}
