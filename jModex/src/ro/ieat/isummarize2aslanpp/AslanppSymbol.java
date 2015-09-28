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

public class AslanppSymbol extends AslanppMetaEntity {

	protected String name, structure;
	protected AslanppType type;

	AslanppSymbol(boolean isConstant, String name, String structure, AslanppType type, AslanppMetaEntity parent) {
		super(parent);
		if(isConstant) {
			this.name = new Character(name.charAt(0)).toString().toLowerCase() + name.substring(1);
		} else {
			this.name = new Character(name.charAt(0)).toString().toUpperCase() + name.substring(1);			
		}
		this.structure = structure;
		this.type = type;
	}
		
	public void print(int tabs, StringBuilder str) {
		super.print(tabs, str);
		if(structure.equals("")) {
			str.append(name + ":" + type.getName() + ";\n");			
		} else {
			str.append(name + "(" + structure + "):" + type.getName() + ";\n");
		}
	}
	
	AslanppSymbolReference getReference(boolean toBeBound, AslanppReference contained) {
		return new AslanppSymbolReference(this, toBeBound, contained);
	}

	String getName() {
		return name;
	}

	AslanppType getType() {
		return type;
	}
	
	boolean isTheSame(AslanppSymbol symbol) {
		return name.equals(symbol.name) && structure.equals(symbol.structure) && type.equals(symbol.type);
	}

	public boolean isConstant() {
		return Character.isLowerCase(name.charAt(0));
	}

}
