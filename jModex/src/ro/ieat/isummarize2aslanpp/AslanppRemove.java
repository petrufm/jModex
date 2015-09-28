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

class AslanppRemove extends AslanppMetaEntity implements AslanppStatement {

	private AslanppReference theThis;
	private AslanppReference param;
	
	AslanppRemove(AslanppSymbolReference theThis, AslanppReference param) {
		this.theThis = theThis;
		this.param = param;
	}
	
	public void print(int tabs, StringBuilder str) {
		super.print(tabs, str);
		theThis.print(tabs, str);
		str.append("->remove(");
		param.print(0, str);
		str.append(")");
		str.append(";\n");
	}

	@Override
	public boolean isTheSame(AslanppStatement stm) {
		if(stm instanceof AslanppRemove) {
			return theThis.isTheSame(((AslanppRemove) stm).theThis) && param.isTheSame(((AslanppRemove) stm).param);
		}
		return false;
	}

	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppRemove(this);
	}
	
}
