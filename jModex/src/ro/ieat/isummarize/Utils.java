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
package ro.ieat.isummarize;

public class Utils {

	public static String getFullQualifiedTypeNameFromBinaryName(String binaryName) {
		String type = binaryName;
		if(type.equals("V")) {
			return "void";
		}
		if(type.equals("Z")) {
			return "boolean";
		}
		if(type.equals("C")) {
			return "char";
		}
		if(type.equals("B")) {
			return "byte";
		}
		if(type.equals("S")) {
			return "short";
		}
		if(type.equals("I")) {
			return "int";
		}
		if(type.equals("F")) {
			return "float";
		}
		if(type.equals("J")) {
			return "long";
		}
		if(type.equals("D")) {
			return "double";
		} 
		if(type.charAt(type.length() - 1) == ';') {
				type = type.substring(0,type.length()-1);
		}
		return type.substring(1,type.length()).replace('/', '.');
	}
	
	static String getFullQualifiedSignaruteFromBinarySignature(String binarySignature) {
		String classMethodNames = binarySignature.substring(0, binarySignature.indexOf('('));
		String returnType = binarySignature.substring(binarySignature.lastIndexOf(')') + 1);
		returnType = getFullQualifiedTypeNameFromBinaryName(returnType);
		String remained = binarySignature.substring(binarySignature.indexOf('(') + 1, binarySignature.lastIndexOf(')'));
		String theArgTypes = "";
		while(!remained.equals("")) {
			if(remained.charAt(0) == 'L') {
				String aType = remained.substring(0,remained.indexOf(';'));
				theArgTypes += getFullQualifiedTypeNameFromBinaryName(aType);
				remained = remained.substring(remained.indexOf(';') + 1);
			} else {
				theArgTypes += getFullQualifiedTypeNameFromBinaryName(new Character(remained.charAt(0)).toString());
				remained = remained.substring(1);
			}
			if(!remained.equals("")) {
				theArgTypes += ";";
			}
		}
		return classMethodNames + "(" + theArgTypes + ")" + returnType;
	}
}
