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

public class AslanppSymbolReference extends AslanppMetaEntity implements AslanppReference {

	private AslanppSymbol theSymbol;
	private boolean toBeBound;
	private AslanppExpression contained;
	
	AslanppSymbolReference(AslanppSymbol theSymbole, boolean toBeBound, AslanppReference contained) {
		super(null);
		this.theSymbol = theSymbole;
		this.toBeBound = toBeBound;
		this.contained = contained;
	}
	
	public void print(int tabs, StringBuilder str) {
		String result = "";
		for(AslanppExpression stm : code) {
			result += stm + " & ";
		}
		str.append(result);
		str.append(this.toString());
	}
	
	public String toString() {
		if(theSymbol.structure.equals("")) {
			if(toBeBound) {
				return "?" + theSymbol.name;
			} else {
				return theSymbol.name;				
			}
		} else {
			if(toBeBound) {
				return "?" + theSymbol.name + contained;				
			} else {
				return theSymbol.name + contained;				
			}
		}
	}

	private List<AslanppExpression> code = new ArrayList<AslanppExpression>(); 
		
	@Override
	public AslanppType getType() {
		return theSymbol.getType();
	}
	
	String getName() {
		return theSymbol.getName();
	}
	
	AslanppSymbol getSymbol() {
		return theSymbol;
	}

	@Override
	public boolean isTheSame(AslanppExpression ref) {
		if(ref instanceof AslanppSymbolReference) {
			return theSymbol.isTheSame(((AslanppSymbolReference) ref).theSymbol) && 
					toBeBound == ((AslanppSymbolReference) ref).toBeBound &&
					(contained == ((AslanppSymbolReference) ref).contained || contained.isTheSame(((AslanppSymbolReference) ref).contained));
		}
		return false;
	}
	
	@Override
	public Object accept(AslanppVisitor visitor) {
		return visitor.visitAslanppSymbolReference(this);
	}

	
}
