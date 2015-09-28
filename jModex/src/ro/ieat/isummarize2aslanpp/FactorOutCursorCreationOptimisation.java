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
import java.util.HashMap;
import java.util.List;
import java.util.Stack;

import ro.ieat.isummarize.ProgramExpression;

public class FactorOutCursorCreationOptimisation {

	public static class FactorOutCursorCreationOptimisationData {
		public AslanppWhile loop;
		public List<AslanppStatement> before_loop = new ArrayList<AslanppStatement>();	
		public List<AslanppIf> selectors = new ArrayList<AslanppIf>();
		public List<AslanppStatement> after_loop = new ArrayList<AslanppStatement>();
	}
	
	public static Stack<List<ProgramExpression>> allCursorCreationStack = new Stack<List<ProgramExpression>>();
	public static Stack<HashMap<String,FactorOutCursorCreationOptimisationData>> allCursorCreationCacheStack = new Stack<HashMap<String,FactorOutCursorCreationOptimisationData>>();
	public static ProgramExpression currentCursorCreation;

	public static void tryCombineCursorCreationStart(Edge ... allEdges) {
		List<ProgramExpression> allCursorCreation = new ArrayList<ProgramExpression>();
		for(Edge anEdge : allEdges) {
			allCursorCreation.addAll(Util.collectCursorCreation(anEdge.getGuard()));
		}
		for(int i = 0; i < allCursorCreation.size(); i++) {
			for(int j = i+1; j < allCursorCreation.size(); j++) {
				if(allCursorCreation.get(i).structuralEquals(allCursorCreation.get(i))) {
					allCursorCreation.remove(j);
					j--;
				}
			}			
		}
		allCursorCreationStack.push(allCursorCreation);
		allCursorCreationCacheStack.push(new HashMap<String,FactorOutCursorCreationOptimisationData>());
	}

	public static void tryCombineCursorCreationStop(List<AslanppStatement> code) {
		allCursorCreationStack.pop();
		HashMap<String,FactorOutCursorCreationOptimisationData> data = allCursorCreationCacheStack.pop();
		for(String aLoop : data.keySet()) {
			FactorOutCursorCreationOptimisationData anOptimisation = data.get(aLoop);
			for(int i = 0; i < code.size(); i++) {
				if(code.get(i) == anOptimisation.loop) {
					code.addAll(i+1,anOptimisation.after_loop);
					code.addAll(i,anOptimisation.before_loop);
					break;
				}
			}
		}
	}
	
	public static void reset() {
		allCursorCreationStack.clear();
		allCursorCreationCacheStack.clear();
		currentCursorCreation = null;	
	}

}
