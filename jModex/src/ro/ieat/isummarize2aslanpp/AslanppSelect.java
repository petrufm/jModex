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

class AslanppSelect extends AslanppMetaEntity implements AslanppStatement {

	private List<AslanppOn> body = new ArrayList<AslanppOn>();
	
	public void print(int tabs, StringBuilder str) {
		if(!isEmpty()) {
			super.print(tabs, str);
			str.append("select {\n");
			for(AslanppOn stm : body) {
				stm.print(tabs + 1, str);
			}
			super.print(tabs, str);
			str.append("}\n");
		}
	}

	void addOnStatement(AslanppOn stm) {
		body.add(stm);
	}
	
	boolean isEmpty() {
		boolean result = true;
		for(AslanppOn stm : body) {
			result = result && stm.isEmpty();
		}
		return result;
	}
	
	List<AslanppOn> getOns() {
		return body;
	}

	@Override
	public boolean isTheSame(AslanppStatement stm) {
		if(stm instanceof AslanppSelect) {
			if(body.size() == ((AslanppSelect)stm).body.size()) {
				List<AslanppOn> tmp = new ArrayList<AslanppOn>();
				tmp.addAll(((AslanppSelect)stm).body);
				for(int i = 0; i < body.size(); i++) {
					for(int j = 0; j < tmp.size(); j++) {
						if(body.get(i).isTheSame(tmp.get(j))) {
							tmp.remove(j);
							break;
						}						
					}
				}
			}
		}
		return false;
	}

	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppSelect(this);
	}	

}
