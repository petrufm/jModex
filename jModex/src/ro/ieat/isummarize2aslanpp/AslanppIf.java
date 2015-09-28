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

class AslanppIf extends AslanppMetaEntity implements AslanppStatement {

	private AslanppExpression guard;
	private List<AslanppStatement> thenBranch = new ArrayList<AslanppStatement>();
	private List<AslanppStatement> elseBranch = new ArrayList<AslanppStatement>();
	
	AslanppIf(AslanppExpression guard) {
		this.guard = guard;
	}
	
	public void print(int tabs, StringBuilder str) {
		super.print(tabs, str);
		str.append("if(");
		guard.print(0, str);
		if(thenBranch.size() == 0) {
			str.append(") {}");			
		} else {
			str.append(") {\n");
			for(AslanppStatement stm : thenBranch) {
				stm.print(tabs + 1, str);
			}
			super.print(tabs, str);
			str.append("}");
		}
		if(elseBranch.size() > 0) {
			str.append(" else {\n");
			for(AslanppStatement stm : elseBranch) {
				stm.print(tabs + 1, str);
			}
			super.print(tabs, str);
			str.append("}\n");
		} else {
			str.append("\n");			
		}
	}

	AslanppExpression getGuard() {
			return guard;
	}

	void addStatementThen(AslanppStatement stm) {
		thenBranch.add(stm);
	}

	void addStatementElse(AslanppStatement stm) {
		elseBranch.add(stm);
	}

	@Override
	public boolean isTheSame(AslanppStatement stm) {
		if(stm instanceof AslanppIf) {
			if(guard.isTheSame(((AslanppIf) stm).guard)) {
				if(thenBranch.size() == ((AslanppIf) stm).thenBranch.size()) {
					for(int i = 0; i < thenBranch.size(); i++) {
						if(!thenBranch.get(i).isTheSame(((AslanppIf) stm).thenBranch.get(i))) {
							return false;
						}
					}
				} else {
					return false;
				}
				if(elseBranch.size() == ((AslanppIf) stm).elseBranch.size()) {
					for(int i = 0; i < elseBranch.size(); i++) {
						if(!elseBranch.get(i).isTheSame(((AslanppIf) stm).elseBranch.get(i))) {
							return false;
						}
					}
				} else {
					return false;
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppIf(this);
	}

}
