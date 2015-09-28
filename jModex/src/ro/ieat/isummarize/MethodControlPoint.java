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

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;

public class MethodControlPoint {

	private IdentityHashMap<MethodPath,MethodPath> outgoing = new IdentityHashMap<MethodPath,MethodPath>();
	private int id;
			
	private MethodControlPoint() {
		this.id = idCounter++;
	}
			
	public List<MethodPath> getOutgoingPaths() {
		List<MethodPath> res = new ArrayList<MethodPath>();
		res.addAll(outgoing.keySet());
		return Collections.unmodifiableList(res);
	}
	
	public String toString() {
		return id + "";
	}
	
	private static int idCounter = 0;

	void addOutgoing(MethodPath m) {
		outgoing.put(m,m);
	}

	void removeOutgoing(MethodPath m) {	
		outgoing.remove(m);
	}

	public static MethodControlPoint createMethodControlPoint() {
		return new MethodControlPoint();
	}

	private static IdentityHashMap<MethodControlPoint,MethodControlPoint> controlPoint2clone =
			new IdentityHashMap<MethodControlPoint,MethodControlPoint>();
	
	void clearCloneMapping() {
		controlPoint2clone.clear();
	}
	
	MethodControlPoint deepClone() {
		if(controlPoint2clone.get(this) != null) {
			return controlPoint2clone.get(this);
		}
		MethodControlPoint theControlPointClone = MethodControlPoint.createMethodControlPoint();
		controlPoint2clone.put(this, theControlPointClone);
		for(MethodPath mp : outgoing.keySet()) {
			MethodPath mpClone = (MethodPath) mp.clone();
			mpClone.addFromControlPoint(theControlPointClone);
			mpClone.addToControlPoint(mp.getTo().deepClone());
		}
		return theControlPointClone;
	}
	
}
