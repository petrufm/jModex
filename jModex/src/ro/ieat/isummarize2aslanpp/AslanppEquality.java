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

public class AslanppEquality extends AslanppMetaEntity implements AslanppReference {

	private AslanppReference l,r;
	
	AslanppEquality(AslanppReference l, AslanppReference r) {
		this.l = l;
		this.r = r;
	}
		
	@Override
	public AslanppType getType() {
		return AslanppType.factType;
	}

	@Override
	public boolean isTheSame(AslanppExpression ref) {
		if(ref instanceof AslanppEquality) {
			return l.isTheSame(((AslanppEquality)ref).l) && r.isTheSame(((AslanppEquality)ref).r);
		}
		return false;
	}
	
	@Override
	public void print(int tabs, StringBuilder str) {
		l.print(0, str);
		str.append(" = ");
		r.print(0, str);
	}

	public AslanppReference getLeft() {
		return l;
	}

	public AslanppReference getRight() {
		return r;
	}

	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppEquality(this);
	}

}
