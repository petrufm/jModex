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
import java.util.Iterator;

public class AslanppTuple extends AslanppMetaEntity implements AslanppReference {

	private List<AslanppReference> elements = new ArrayList<AslanppReference>();
	
	AslanppTuple(AslanppReference ... elements) {
		for(AslanppReference anElement : elements) {
			this.elements.add(anElement);
			anElement.setParent(this);
		}
	}

	@Override
	public void print(int tabs, StringBuilder str) {
		str.append("(");
		Iterator<AslanppReference> it = elements.iterator();
		while(it.hasNext()){
			AslanppReference anElement = it.next();
			anElement.print(tabs, str);
			if(it.hasNext()) {
				str.append(",");
			}
		}
		str.append(")");
	}

	@Override
	public void setParent(AslanppMetaEntity entity) {
		this.parent = entity;
	}
	
	@Override
	public AslanppType getType() {
		ArrayList<AslanppType> result = new ArrayList<AslanppType>();
		Iterator<AslanppReference> it = elements.iterator();
		while(it.hasNext()){
			AslanppReference anElement = it.next();
			result.add(anElement.getType());
		}
		return AslanppType.getTupleType(result.toArray(new AslanppType[]{}));
	}
	
	@Override
	public boolean isTheSame(AslanppExpression ref) {
		if(ref instanceof AslanppTuple) {
			if(elements.size() != ((AslanppTuple)ref).elements.size()) {
				return false;
			}
			for(int i = 0; i < elements.size(); i++) {
				if(!elements.get(i).isTheSame(((AslanppTuple)ref).elements.get(i))) {
					return false;
				}
			}
			return true;
		}
		return false;
	}
	
	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppTuple(this);
	}


}
