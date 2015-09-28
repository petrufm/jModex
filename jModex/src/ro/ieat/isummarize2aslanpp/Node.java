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
import java.util.Set;

import ro.ieat.isummarize2aslanpp.interval.Fact;

class Node implements Region {

	private List<Edge> out = new ArrayList<Edge>();
	private List<Edge> in = new ArrayList<Edge>();
	
	void addIncommig(Edge in) {
		this.in.add(in);
	}
	
	void addOutgoing(Edge out) {
		this.out.add(out);
	}
	
	List<Edge> getIncomming() {
		return in;
	}

	List<Edge> getOutgoing() {
		return out;
	}

	void removeIncomming(Edge edge) {
		in.remove(edge);
	}

	void removeOutgoing(Edge edge) {
		out.remove(edge);
	}

	@Override
	public List<AslanppStatement> generateCode(AslanppGenerator.ConversionVisitor conversionVisitor, Set<? extends Fact> fatcs) {
		return new ArrayList<AslanppStatement>();
	}

	public Node duplicateNode() {
		Node res = new Node();
		for(Edge anOutEdge : out) {
			Edge tmp = anOutEdge.duplicateEdge();
			tmp.setFrom(res);
			tmp.setTo(anOutEdge.getTo());
		}
		for(Edge anInEdge : in) {
			if(!out.contains(anInEdge)) {
				Edge tmp = anInEdge.duplicateEdge();
				tmp.setFrom(anInEdge.getFrom());
				tmp.setTo(res);
			}
		}		
		return res;
	}

}
