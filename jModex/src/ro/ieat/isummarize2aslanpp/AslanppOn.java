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

import java.util.ArrayList;
import java.util.List;

class AslanppOn extends AslanppMetaEntity {

	private List<AslanppStatement> body = new ArrayList<AslanppStatement>();
	private AslanppExpression guard;
	
	public AslanppOn(AslanppExpression guard) {
		this.guard = Simplifier.simplifyLogic(VirtualAslanppSpecialContains.eliminateVirtualInstruction(guard));
		this.guard.setParent(this);
	}

	public void print(int tabs, StringBuilder str) {
		super.print(tabs, str);
		str.append("on (");
		guard.print(0, str);
		if(body.size() == 0) {
			str.append("): {}\n");
		} else {
			str.append("): {\n");
			for(AslanppStatement stm : body) {
				stm.print(tabs + 1, str);
			}
			super.print(tabs, str);
			str.append("}\n");
		}
	}

	public void addStatement(AslanppStatement stm) {
		body.add(stm);
	}
	
	AslanppExpression getGuard() {
		return guard;
	}

	boolean isEmpty() {
		return body.isEmpty();
	}

	public void addAllStatements(List<AslanppStatement> code) {
		body.addAll(code);
	}

	public boolean isTheSame(AslanppOn stm) {
		if(guard.isTheSame(stm.guard)) {
			if(body.size() == stm.body.size()) {
				for(int i = 0; i < body.size(); i++) {
					if(!body.get(i).isTheSame(stm.body.get(i))) {
						return false;
					}
				}
			} else {
				return false;
			}
			return true;
		}
		return false;
	}
	List<AslanppStatement> getBody() {
		return body;
	}
}
