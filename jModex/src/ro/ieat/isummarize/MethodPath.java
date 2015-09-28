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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;


public class MethodPath {
	
	private GuardExpression guard;
	private MethodControlPoint from, to;
	
	public GuardExpression getGuard() {
		return guard;
	}

	public void setGuard(GuardExpression guard) {
		this.guard = guard;
	}
	
	public void addFromControlPoint(MethodControlPoint from) {
		this.from = from;
		from.addOutgoing(this);
	}

	public void addToControlPoint(MethodControlPoint to) {
		this.to = to;
	}

	private Map<TracedVariable,ProgramExpression> variable2pathExpression;
	
	public Map<TracedVariable,ProgramExpression> getVariableExpressions() {
		return Collections.unmodifiableMap(variable2pathExpression);
	}
	
	public MethodPath(GuardExpression guard, MethodControlPoint to) {
		this.guard = guard;
		this.variable2pathExpression = new HashMap<TracedVariable,ProgramExpression>();
		this.to = to;
	}
	
	public void add(TracedVariable pv, ProgramExpression pe) {
		if(variable2pathExpression.get(pv) == null) {
			variable2pathExpression.put(pv, pv.preparedForUpdateAssignment(pe));
		}
	}

	public void remove(TracedVariable pv) {
		variable2pathExpression.remove(pv);
	}

	public void substitute(ProgramSubstitutableVariable toSub, ProgramExpression newExp) {
		newExp = toSub.preparedForUpdateAssignment(newExp);
		guard = guard.substitute(toSub, newExp);
		HashMap<TracedVariable,ProgramExpression> tmp = new HashMap<TracedVariable,ProgramExpression>();
		for(TracedVariable sv : variable2pathExpression.keySet()) {
			TracedVariable svNew = sv.substituteSubExpression(toSub, newExp);
			ProgramExpression expNew = variable2pathExpression.get(sv).substitute(toSub, newExp);
			tmp.put(svNew, expNew);
		}
		variable2pathExpression = tmp;
	}
	
	public Object clone() {
		MethodPath mp = new MethodPath(guard,to);
		mp.variable2pathExpression = new HashMap<TracedVariable,ProgramExpression>();
		mp.variable2pathExpression.putAll(variable2pathExpression);
		mp.from = from;
		return mp;
	}
	
	public String toString() {
		return "Guard:" + guard + "\tState:" + variable2pathExpression;
	}

	public MethodControlPoint getTo() {
		return to;
	}

	public MethodControlPoint getFrom() {
		return from;
	}

	public boolean equals(Object o) {
		if(o instanceof MethodPath) {
			MethodPath oo = (MethodPath)o;
			if(guard == oo.guard || guard.equals(oo.guard)) {
				if(variable2pathExpression.keySet().size() != oo.variable2pathExpression.keySet().size()) {
					return false;
				}
				for(TracedVariable tv : variable2pathExpression.keySet()) {
					if(!oo.variable2pathExpression.containsKey(tv) ||
							!(variable2pathExpression.get(tv) == oo.variable2pathExpression.get(tv) || variable2pathExpression.get(tv).structuralEquals(oo.variable2pathExpression.get(tv)))) {
						return false;
					}
				}
				return true;
			}
			return false;
		}
		return false;
	}
	
	public int hashCode() {
		return 32 * guard.hashCode();
	}

	public MethodPath concatPath(MethodPath with, ProgramIndexExpression returnValue) {
		MethodPath combined = (MethodPath) with.clone();
		List<Entry<TracedVariable, ProgramExpression>> orderedUpdates = this.getOrderedUpdates();
		/*if(returnValue != null) {
			combined.substitute(
					returnValue, 
					this.variable2pathExpression.get(new ProgramReturnVariable())
			);
		}
		for(TracedVariable aTrancedVariable : this.variable2pathExpression.keySet()) {
			if(aTrancedVariable.isObservedGlobally() && aTrancedVariable instanceof ProgramSubstitutableVariable) {
				combined.substitute((ProgramSubstitutableVariable) aTrancedVariable, 
						this.variable2pathExpression.get(aTrancedVariable));
			}
		}*/
		//Substitution considerring the ordering of updates (e.g., a) ret = x; b) x = x + 1; 
		//should be b and after that a)
		for(int i = orderedUpdates.size() - 1; i >= 0; i--) {
			TracedVariable aTrancedVariable = orderedUpdates.get(i).getKey();			
			if(aTrancedVariable.isObservedGlobally() && aTrancedVariable instanceof ProgramSubstitutableVariable) {
				combined.substitute((ProgramSubstitutableVariable) aTrancedVariable, 
						this.variable2pathExpression.get(aTrancedVariable));
			} else if(aTrancedVariable instanceof ProgramReturnVariable && returnValue != null) {
				combined.substitute(
						returnValue, 
						this.variable2pathExpression.get(new ProgramReturnVariable()));				
			}
		}
		//End
		for(TracedVariable aTrancedVariable : this.variable2pathExpression.keySet()) {
			if(aTrancedVariable.isObservedGlobally() && 
					!combined.variable2pathExpression.keySet().contains(aTrancedVariable)) {
						combined.variable2pathExpression.put(aTrancedVariable, 
									this.variable2pathExpression.get(aTrancedVariable));
			}
		}
		combined.guard = new GuardAndExpression(this.guard, combined.guard);
		return combined;
	}
	
	public Set<TracedVariable> getReadVariables() {
		HashSet<TracedVariable> result = new HashSet<TracedVariable>();
		for(TracedVariable tv : variable2pathExpression.keySet()) {
			for(ProgramExpression pe : variable2pathExpression.get(tv).getVariables()) {
				if(pe instanceof TracedVariable) {
					result.add((TracedVariable) pe);
				}
			}
		}
		for(ProgramExpression pe : this.guard.getVariables()) {
			if(pe instanceof TracedVariable) {
				result.add((TracedVariable) pe);
			}
		}
		return result;
	}
	
	public List<Entry<TracedVariable,ProgramExpression>> getOrderedUpdates() {
		Set<Entry<TracedVariable,ProgramExpression>> theUpdates = variable2pathExpression.entrySet();
		ArrayList<Entry<TracedVariable,ProgramExpression>> allUpdates = new ArrayList<Entry<TracedVariable,ProgramExpression>>(theUpdates);
		ArrayList<Set<ProgramExpression>> allUsedVars = new ArrayList<Set<ProgramExpression>>();
		for(Entry<TracedVariable,ProgramExpression> anEntry : allUpdates) {
			Set<ProgramExpression> usedVar = new HashSet<ProgramExpression>();
			usedVar.addAll(anEntry.getKey().getSubVariables());
			usedVar.addAll(anEntry.getValue().getVariables());
			allUsedVars.add(usedVar);
		}
		List<Set<Entry<TracedVariable,ProgramExpression>>> shouldBeAfter = new ArrayList<Set<Entry<TracedVariable,ProgramExpression>>>();
		for(int i = 0; i < allUpdates.size(); i++) {
			shouldBeAfter.add(new HashSet<Entry<TracedVariable,ProgramExpression>>());
		}
		for(int i = 0; i < allUpdates.size(); i++) {
			for(int j = 0; j < allUpdates.size(); j++) {
				if(i == j) {
					continue;
				}
				if(allUsedVars.get(i).contains(allUpdates.get(j).getKey())) {
					shouldBeAfter.get(j).add(allUpdates.get(i));
				}
			}
		}
		LinkedList<Entry<TracedVariable,ProgramExpression>> workList = new LinkedList<Entry<TracedVariable,ProgramExpression>>();
		for(int i = 0; i < allUpdates.size(); i++) {
			if(shouldBeAfter.get(i).size() == 0) {
				workList.add(allUpdates.get(i));
			}
		}
		ArrayList<Entry<TracedVariable,ProgramExpression>> result = new ArrayList<Entry<TracedVariable,ProgramExpression>>();
		while(!workList.isEmpty()) {
			Entry<TracedVariable,ProgramExpression> current = workList.removeFirst();
			result.add(current);
			for(int i = 0; i < shouldBeAfter.size(); i++) {
				if(shouldBeAfter.get(i).size() == 0) continue;
				Iterator<Entry<TracedVariable,ProgramExpression>> it = shouldBeAfter.get(i).iterator();
				while(it.hasNext()) {
					Entry<TracedVariable,ProgramExpression> anEntry = it.next();
					if(current == anEntry) {
						it.remove();
					}
				}
				if(shouldBeAfter.get(i).size() == 0) {
					workList.add(allUpdates.get(i));
				}
 			}
		}
		for(int i = 0; i < shouldBeAfter.size(); i++) {
			if(shouldBeAfter.get(i).size() != 0) {
				throw new RuntimeException("Cyclic dependences in updates!" + allUpdates);
			}
		}
 		return result;
	}
	
}
