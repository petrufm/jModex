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

public class AslanppLogicalNegation extends AslanppMetaEntity implements AslanppReference {

	private AslanppReference exp;
	
	public AslanppReference getOperand() {
		return exp;
	}
	
	private AslanppLogicalNegation(AslanppReference exp) {
		this.exp = exp;
	}
	
	@Override
	public void print(int tabs, StringBuilder str) {
		str.append("!");
		str.append("(");
		exp.print(tabs, str);
		str.append(")");
	}

	@Override
	public AslanppType getType() {
		return AslanppType.factType;
	}

	public static AslanppReference createInstance(AslanppReference ref) {
		if(ref instanceof AslanppLogicalNegation) {
			return ((AslanppLogicalNegation)ref).exp;
		} else if(ref instanceof AslanppEquality) {
			return new AslanppInequality(((AslanppEquality)ref).getLeft(), ((AslanppEquality)ref).getRight());
		} else if(ref instanceof AslanppInequality){
			return new AslanppEquality(((AslanppInequality)ref).getLeft(), ((AslanppInequality)ref).getRight());
		} else if(ref instanceof VirtualAslanppSpecialContains) {
			return ((VirtualAslanppSpecialContains)ref).negate();
		} else {
			return new AslanppLogicalNegation(ref);
		} 
	}

	public static AslanppReference createInstanceForElse(AslanppReference ref) {
		if(ref instanceof AslanppLogicalNegation) {
			return ((AslanppLogicalNegation)ref).exp;
		} else if(ref instanceof AslanppEquality) {
			return new AslanppInequality(((AslanppEquality)ref).getLeft(), ((AslanppEquality)ref).getRight());
		} else if(ref instanceof AslanppInequality){
			return new AslanppEquality(((AslanppInequality)ref).getLeft(), ((AslanppInequality)ref).getRight());
		} else {
			return new AslanppLogicalNegation(ref);
		} 
	}

	@Override
	public boolean isTheSame(AslanppExpression ref) {
		if(ref instanceof AslanppLogicalNegation) {
			return exp.isTheSame(((AslanppLogicalNegation) ref).exp);
		}
		return false;
	}
	
	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppLogicalNegation(this);
	}


}
