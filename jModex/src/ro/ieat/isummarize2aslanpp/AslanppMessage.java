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

class AslanppMessage extends AslanppMetaEntity implements AslanppExpression, AslanppStatement {

	private AslanppSymbolReference from, to, message;
	private boolean asExpression;
	
	AslanppMessage(AslanppSymbolReference from, AslanppSymbolReference to, AslanppSymbolReference message, boolean asExpression) {
		this.from = from;
		this.to = to;
		this.message = message;
		this.asExpression = asExpression;
	}

	public void print(int tabs, StringBuilder str) {
		if(!asExpression) {
			super.print(tabs, str);
		}
		from.print(tabs, str);
		str.append("*->*");
		to.print(0, str);
		str.append(":");
		message.print(0, str);
		if(!asExpression) {
			str.append(";\n");
		}
	}

	@Override
	public AslanppType getType() {
		return AslanppType.factType;
	}

	@Override
	public boolean isTheSame(AslanppExpression exp) {
		if(exp instanceof AslanppMessage) {
			return from.isTheSame(((AslanppMessage) exp).from) && to.isTheSame(((AslanppMessage) exp).to) && message.isTheSame(((AslanppMessage) exp).message);
		}
		return false;
	}

	@Override
	public boolean isTheSame(AslanppStatement stm) {
		if(stm instanceof AslanppMessage) {
			return from.isTheSame(((AslanppMessage) stm).from) && to.isTheSame(((AslanppMessage) stm).to) && message.isTheSame(((AslanppMessage) stm).message);
		}
		return false;
	}

	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppMessage(this);
	}
	
}
