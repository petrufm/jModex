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

public class AslanppType extends AslanppMetaEntity{

	public static final AslanppType messageType = new AslanppType("message",null);
	public static final AslanppType factType = new AslanppType("fact",null);
	public static final AslanppType textType = new AslanppType("text",messageType);
	public static final AslanppType natType = new AslanppType("nat",messageType);
	public static final AslanppType agentType = new AslanppType("agent",messageType);
	public static final AslanppType setType = new AslanppType("set",null);

	public static final AslanppType getSetType(AslanppType elements) {
		return new AslanppType(elements.getName() + " set", null) {};
	}

	public static final AslanppType getTupleType(AslanppType ... elements) {
		String name = "";
		for(AslanppType aType : elements) {
			if(aType.getName().endsWith(" set")) {
				name += "(" + aType.getName() + ")*";				
			} else {
				name += aType.getName() + "*";
			}
		}
		name=name.substring(0,name.length()-1);
		return new AslanppType(name, null) {};
	}
	
	private String name;
	private AslanppType superType;
	
	public AslanppType(String name, AslanppType superType) {
		this.name = name;
		this.superType = superType;
	}
	
	public void print(int tabs, StringBuilder str) {
		super.print(tabs,str);
		if(superType == null) {
			str.append(name);
		} else {
			str.append(name + " < " + superType.getName() + ";");
		}
		str.append("\n");
	}

	public String getName() {
		return name;
	}
	
}
