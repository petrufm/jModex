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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ro.ieat.isummarize.GuardExpression;
import ro.ieat.isummarize.MethodPath;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.TracedVariable;
import ro.ieat.isummarize2aslanpp.interval.Fact;

class DummyEdge extends Edge {
	
	private MethodPath mp;

	DummyEdge(MethodPath mp) {
		this.mp = mp;
	}

	@Override
	public List<AslanppStatement> generateCode(AslanppGenerator.ConversionVisitor conversionVisitor, Set<? extends Fact> facts) {
		List<AslanppStatement> code = new ArrayList<AslanppStatement>();
		Set<Fact> localFacts = facts != null ? 
				new HashSet<Fact>(facts) : 
					new HashSet<Fact>();
		conversionVisitor.setFacts(localFacts);
		List<Entry<TracedVariable, ProgramExpression>> updates = mp.getOrderedUpdates();
		for(Entry<TracedVariable, ProgramExpression> anUpdate : updates) {
			TracedVariable tv = anUpdate.getKey();
			TranslatedExpression leftOfAssignment = (TranslatedExpression) tv.accept(conversionVisitor);
			TranslatedExpression rightOfAssingnment = (TranslatedExpression) anUpdate.getValue().accept(conversionVisitor);
			code.addAll(leftOfAssignment.getLValueCode(rightOfAssingnment));
		}
		return code;
	}

	@Override
	public GuardExpression getGuard() {
		return mp.getGuard();
	}

	@Override
	public Edge duplicateEdge() {
		return new DummyEdge(mp);
	}
	
}