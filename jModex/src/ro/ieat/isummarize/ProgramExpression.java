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

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public abstract class ProgramExpression {
		
	public abstract ProgramExpression substitute(ProgramSubstitutableVariable sub, ProgramExpression newExpression);
	
	public abstract String toString();
	
	public abstract boolean structuralEquals(ProgramExpression o);

	public abstract Set<ProgramExpression> getVariables();

	public abstract List<ProgramExpressionProxy> getProxies();

	public abstract Object accept(ExpressionAcyclicVisitor visitor);
	
	@Override
	public boolean equals(Object o)
	{
		if(o != null && o instanceof ProgramExpression)
			return this.structuralEquals((ProgramExpression)o);
		return false;
	}
	
	private Integer cachedHashCode = null;

	@Override
	public final int hashCode() {
		if(cachedHashCode == null) {
			cachedHashCode = getHashCode();
		}
		return cachedHashCode;
	}
	
	protected abstract int getHashCode();
	
	private HashMap<String,Object> metadata;
	
	public void addData(String key, Object data) {
		if(metadata == null) {
			metadata = new HashMap<String,Object>();
		}
		metadata.put(key,data);
	}
	
	public Object getData(String key) {
		if(metadata == null) {
			return null;
		}
		return metadata.get(key);
	}
	
}
