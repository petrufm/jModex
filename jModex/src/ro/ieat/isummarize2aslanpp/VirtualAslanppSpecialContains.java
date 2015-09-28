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

public class VirtualAslanppSpecialContains extends AslanppMetaEntity implements AslanppReference {

	public static AslanppExpression eliminateVirtualInstruction(AslanppExpression exp) {
		return (AslanppExpression) exp.accept(new EliminationVisitor());
	}
	
	private static class EliminationVisitor extends AslanppVisitor implements VirtualAslanppSpecialContainsVisitor {
		public Object visitAslanppLogicalConjunction(AslanppLogicalConjunction node) {
			AslanppReference l,r;
			l = (AslanppReference) node.getLeft().accept(this);
			r = (AslanppReference) node.getRight().accept(this);
			if(node.getLeft() == l && node.getRight() == r) {
				return node;
			} else {
				return AslanppLogicalConjunction.createInstance(l, r);
			}
		}
		public Object visitAslanppLogicalDisjunction(AslanppLogicalDisjunction node) {
			AslanppReference l,r;
			l = (AslanppReference) node.getLeft().accept(this);
			r = (AslanppReference) node.getRight().accept(this);
			if(node.getLeft() == l && node.getRight() == r) {
				return node;
			} else {
				return AslanppLogicalDisjunction.createInstance(l, r);
			}
		}
		public Object visitAslanppLogicalNegation(AslanppLogicalNegation node) {
			AslanppReference r = (AslanppReference) node.getOperand().accept(this);
			if(r == node.getOperand()) {
				return node;
			} else {
				return AslanppLogicalNegation.createInstance(r);
			}
		}
		public Object visitVirtualAslanppSpecialContains(VirtualAslanppSpecialContains node) {
			return node.getNormalExpression();
		}
	}
	
	public interface VirtualAslanppSpecialContainsVisitor {
		public Object visitVirtualAslanppSpecialContains(VirtualAslanppSpecialContains node);
	}
	
	private AslanppReference theSet,attribute,value;
	private boolean isPositive;
	private AslanppContains positive;
	private AslanppLogicalConjunction negative;
	private AslanppSymbol theSymbol;
	
	VirtualAslanppSpecialContains(AslanppSymbolReference theSet, AslanppReference attribute, AslanppReference value, boolean isPositive, AslanppSymbol theSymbol) {
		this.theSet = theSet;
		this.attribute = attribute;
		this.value = value;
		this.isPositive = isPositive;
		this.theSymbol = theSymbol;
		positive = new AslanppContains(theSet, new AslanppTuple(attribute,value),true);
		if(theSymbol != null) 
			negative = AslanppLogicalConjunction.createInstance(
				new AslanppContains(theSet, new AslanppTuple(attribute,this.theSymbol.getReference(true, null)),true), 
				AslanppLogicalNegation.createInstance(new AslanppContains(theSet, new AslanppTuple(attribute,value),true))
					);
		else 
			negative = AslanppLogicalConjunction.createInstance(
					new AslanppContains(theSet, new AslanppTuple(attribute,new AslanppStringStatement("?",false)),true), 
					AslanppLogicalNegation.createInstance(new AslanppContains(theSet, new AslanppTuple(attribute,value),true))
						);

	}
	
	public AslanppReference getNormalExpression() {
		if(isPositive) {
			return positive;
		} else {
			return negative;
		}
	}
	
	VirtualAslanppSpecialContains negate() {
		this.isPositive = !this.isPositive;
		return this;
	}
	
	public void print(int tabs, StringBuilder str) {
		if(isPositive) {
			positive.print(tabs, str);
		} else {
			negative.print(tabs, str);
		}
	}

	@Override
	public AslanppType getType() {
		return AslanppType.factType;
	}
	
	@Override
	public boolean isTheSame(AslanppExpression ref) {
		if(ref instanceof VirtualAslanppSpecialContains) {
			return theSet.isTheSame(((VirtualAslanppSpecialContains) ref).theSet) 
					&& attribute.isTheSame(((VirtualAslanppSpecialContains) ref).attribute)
					&& value.isTheSame(((VirtualAslanppSpecialContains) ref).value);
		}
		return false;
	}

	@Override
	public Object accept(AslanppVisitor visitor) {
		if(visitor instanceof VirtualAslanppSpecialContainsVisitor) {
			return ((VirtualAslanppSpecialContainsVisitor) visitor).visitVirtualAslanppSpecialContains(this);		
		}
		return null;
	}

}
