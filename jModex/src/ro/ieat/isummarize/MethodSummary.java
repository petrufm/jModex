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
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ro.ieat.isummarize.utils.GuardSimplifier;

import com.ibm.wala.classLoader.IMethod;

public class MethodSummary {
	
	private MethodControlPoint entry;
	private IMethod theMethod;

	public MethodSummary(IMethod theMethod, MethodControlPoint entry) {
		this.entry = entry;
		this.theMethod = theMethod;
	}

	public MethodControlPoint getEntryControPoint() {
		return entry;
	}
	
	public MethodControlPoint getExitControlPoint() {
		MethodControlPoint res = getExit(entry);
		visited.clear();
		return res;
	}
	
	private static HashSet<Integer> visited = new HashSet<Integer>();
	private MethodControlPoint getExit(MethodControlPoint cp) {
		if(cp.getOutgoingPaths().size() == 0) {
			return cp;
		}
		for(MethodPath mp : cp.getOutgoingPaths()) {
			if(visited.contains(System.identityHashCode(mp.getTo()))) continue;
			visited.add(System.identityHashCode(mp.getTo()));
			MethodControlPoint result = getExit(mp.getTo());
			if(result != null) {
				return result;
			}
		}
		return null;
	}
	
	public List<MethodPath> getIncommingPaths(MethodControlPoint cp) {
		if(entry==cp) {
			return new ArrayList<MethodPath>();
		}
		List<MethodPath> res = getIncomming(entry,cp);
		visited.clear();
		return res;
	}
	
	private List<MethodPath> getIncomming(MethodControlPoint current, MethodControlPoint cp) {
		List<MethodPath> result = new ArrayList<MethodPath>();
		for(MethodPath mp : current.getOutgoingPaths()) {
			if(mp.getTo() == cp) {
				result.add(mp);
			}
			if(visited.contains(System.identityHashCode(mp.getTo()))) {
				continue;
			}
			visited.add(System.identityHashCode(mp.getTo()));
			result.addAll(getIncomming(mp.getTo(),cp));
		}
		return result;
	}		
	
	public MethodSummary deepClone() {
		MethodSummary theClone = new MethodSummary(theMethod, entry.deepClone());
		entry.clearCloneMapping();
		return theClone;
	}
	
	public String toString() {
		LinkedList<MethodControlPoint> workList = new LinkedList<MethodControlPoint>();
		workList.add(entry);
		int index = 0;
		StringBuilder res = new StringBuilder("");
		int controlPoints = 0;
		int transitions = 0;
		while(index < workList.size()) {
			controlPoints++;
			MethodControlPoint current = workList.get(index);
			index++;
			res.append(current);
			res.append("->\n");
			for(MethodPath outPath : current.getOutgoingPaths()) {
				transitions++;
				res.append("\t");
				res.append(outPath.getTo());
				res.append("\t");
				res.append(outPath);
				res.append("\n");
				if(!workList.contains(outPath.getTo())) {
					workList.add(outPath.getTo());
				}
			}
		}
		res.insert(0,"\n" + "Control Points: " + controlPoints + "\nTransitions:" + transitions + "\n" + theMethod + "\n");
		return res.toString();
	}
	
	void finalizeConstruction(SatisfiabilityChecker satChecker) {
		LinkedList<MethodControlPoint> workList = new LinkedList<MethodControlPoint>();
		LinkedList<MethodPath> toRemove = new LinkedList<MethodPath>();
		workList.add(entry);
		int index = 0;
		while(index < workList.size()) {
			MethodControlPoint current = workList.get(index);
			index++;
			toRemove.clear();
			for(MethodPath outPath : current.getOutgoingPaths()) {
				if(!workList.contains(outPath.getTo())) {
					workList.add(outPath.getTo());
				}
				//Eliminate proxies from guards
				GuardExpression newGuard = outPath.getGuard();
				List<ProgramExpressionProxy> proxies = newGuard.getProxies();
				for(ProgramExpressionProxy proxy : proxies) {
					ProgramExpression newExp = proxy.resolve();
					if(newExp != null) {
						newGuard = newGuard.substitute(proxy, newExp);
					}
				}
				//Eliminate proxies from updates
				//TODO:Not done yet
				//End
				newGuard = GuardSimplifier.simplify(newGuard);
				outPath.setGuard(newGuard);
				if(satChecker != null && !satChecker.mayBeSatisfiable(newGuard)) {
					toRemove.add(outPath);
				}
			}
			for(MethodPath mp : toRemove) {
				current.removeOutgoing(mp);
			}
		}
		eliminateSomeIntermediateVariables(satChecker);
		//interpointsSimplification(satChecker);
	}
	
	public boolean structuralEquals(MethodSummary o) {
		controlPointSet.clear();
		return checkEquality(entry, o.entry);
	}

	private static IdentityHashMap<MethodControlPoint,MethodControlPoint> controlPointSet =
			new IdentityHashMap<MethodControlPoint,MethodControlPoint>();
	private static boolean checkEquality(MethodControlPoint one, MethodControlPoint two) {	
		if(controlPointSet.containsKey(one)) {
			return true;
		}
		controlPointSet.put(one,one);
		List<MethodPath> tmp = new ArrayList<MethodPath>();
		tmp.addAll(two.getOutgoingPaths());
		if(tmp.size() != one.getOutgoingPaths().size()) {
			controlPointSet.remove(one);
			return false;
		}
		repeat:for(MethodPath mp : one.getOutgoingPaths()) {
				for(int i = 0; i < tmp.size(); i++) {
					if(mp.equals(tmp.get(i))) {
						if(checkEquality(mp.getTo(), tmp.get(i).getTo())) {
							tmp.remove(i);
							continue repeat;
						}
					}
				}
				controlPointSet.remove(one);
				return false;
		}
		controlPointSet.remove(one);
		return tmp.size() == 0;
	}

	private boolean areThereUpdatesToGlobals(MethodControlPoint aState, HashSet<Integer> markVisited) {
		if(!markVisited.contains(System.identityHashCode(aState))) {
			markVisited.add(System.identityHashCode(aState));
			for(MethodPath mp : aState.getOutgoingPaths()) {
				Map<TracedVariable, ProgramExpression> mapping = mp.getVariableExpressions();
				for(TracedVariable tv : mapping.keySet()) {
					if(tv.isObservedGlobally()) return true;
				}
				if(areThereUpdatesToGlobals(mp.getTo(), markVisited)) {
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean combinationNedded(List<MethodPath> pathsOut, List<MethodPath> pathsAterCall, ProgramIndexExpression retValue) {
		HashSet<Integer> markVisited = new HashSet<Integer>();
		if(areThereUpdatesToGlobals(this.entry, markVisited)) {
			return true;
		}		
		for(MethodPath aPathAfterInvocation : pathsAterCall) {
			for(MethodPath anOutPathFromInvokedMethod : pathsOut) {
				for(TracedVariable tv: anOutPathFromInvokedMethod.getVariableExpressions().keySet()) {
					ProgramExpression aVar;
					if(tv instanceof ProgramReturnVariable) {
						if(retValue == null) {
							continue;
						}
						aVar = retValue;
					}  else {
						aVar = (ProgramExpression)tv;
					}
					if(aPathAfterInvocation.getGuard().getVariables().contains(aVar)) {
						return true;
					}
					Map<TracedVariable, ProgramExpression> updates = aPathAfterInvocation.getVariableExpressions();
					for(TracedVariable observedVar : updates.keySet()) {
						if(observedVar.getSubVariables().contains(aVar)) {
							return true;
						}
						if(updates.get(observedVar).getVariables().contains(aVar)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	public boolean combineExitsWith(SatisfiabilityChecker checker, List<MethodPath> res, ProgramIndexExpression returnValue) {
		List<MethodPath> allBefore = getIncommingPaths(getExitControlPoint());
		if(!combinationNedded(allBefore, res, returnValue)) {
			return false;
		}
		//Check for local variables in the paths after the invocation points
		Set<TracedVariable> allUsedLocalsOfInvoker = new HashSet<TracedVariable>(); ;
		for(MethodPath after : res) {
			for(TracedVariable aVariable : after.getReadVariables()) {
				if(!aVariable.isObservedGlobally()) {
					allUsedLocalsOfInvoker.add(aVariable);
				}
			}			
		}
		if(!allUsedLocalsOfInvoker.isEmpty()) {
			//Add locals as state variables to every state immediately following the entry node
			//They are not added at the exit node.
			MethodControlPoint exit = getExitControlPoint();
			for(MethodPath inPath : entry.getOutgoingPaths()) {
				if(inPath.getTo() != exit) {
					for(TracedVariable tv : allUsedLocalsOfInvoker) {
						inPath.add(tv, (ProgramExpression)tv);
					}					
				}
			}
		}
		//Concatenate paths
		for(MethodPath before : allBefore) {
			for(MethodPath after : res) {
				MethodPath combined = before.concatPath(after,returnValue);
				combined.setGuard(GuardSimplifier.simplify(combined.getGuard()));
				if(checker == null || this.entry == before.getFrom() || checker.mayBeSatisfiable(combined.getGuard())) {
					combined.addFromControlPoint(before.getFrom());
				}
			}
			before.getFrom().removeOutgoing(before);
		}
		return true;
	}

	public void substitute(ProgramIndexExpression toSub, ProgramIndexExpression newExp) {
		for(MethodPath mp : entry.getOutgoingPaths()) {
			mp.substitute(toSub, newExp);
		}
	}

	public String getFullName() {
		return theMethod.getDeclaringClass().getName().toString().replace('/','.').substring(1); 
	}
	
	/**
	 * Identifies all the variables that are used in the outgoing transitions of the
	 * entry control point, or in the outgoing transitions of control points reachable from
	 * the entry control point
	 * 
	 * @return - the set of variables
	 */
	public Set<TracedVariable> getAllRelevantVariables() {
		HashSet<TracedVariable> result = new HashSet<TracedVariable>();
		HashSet<Integer> markVisited = new HashSet<Integer>();
		getAllFurtherRelevantVariables(this.entry, result, markVisited);
		return Collections.unmodifiableSet(result);
	}
	
	private void getAllFurtherRelevantVariables(MethodControlPoint aState, Set<TracedVariable> result, HashSet<Integer> markVisited) {
		if(!markVisited.contains(System.identityHashCode(aState))) {
			markVisited.add(System.identityHashCode(aState));
			for(MethodPath mp : aState.getOutgoingPaths()) {
				for(ProgramExpression pe : mp.getGuard().getVariables()) {
					result.add((TracedVariable)pe);
				}
				Map<TracedVariable, ProgramExpression> mapping = mp.getVariableExpressions();
				for(TracedVariable tv : mapping.keySet()) {
					for(ProgramExpression pe : mapping.get(tv).getVariables()) {
						result.add((TracedVariable)pe);
					}
				}
				getAllFurtherRelevantVariables(mp.getTo(),result, markVisited);
			}
		}
	}
	
	private Map<MethodControlPoint, Set<MethodControlPoint>> computeDominance(MethodControlPoint graphEntry) {
		//Compute all nodes of the graph and the predecessor relation
		LinkedList<MethodControlPoint> allNodes = new LinkedList<MethodControlPoint>();
		Map<MethodControlPoint, Set<MethodControlPoint>> predecessors = new HashMap<MethodControlPoint, Set<MethodControlPoint>>();
		allNodes.add(graphEntry);
		predecessors.put(graphEntry, new HashSet<MethodControlPoint>());
		int index = 0;
		while(index < allNodes.size()) {
			MethodControlPoint current = allNodes.get(index);
			index++;
			for(MethodPath outPath : current.getOutgoingPaths()) {
				if(!allNodes.contains(outPath.getTo())) {
					allNodes.add(outPath.getTo());
					HashSet<MethodControlPoint> preds = new HashSet<MethodControlPoint>();
					for(MethodPath aPath : this.getIncommingPaths(outPath.getTo())) {
						preds.add(aPath.getFrom());
					}
					predecessors.put(outPath.getTo(), preds);
				}
			}
		}
		//Initialize algorithm
		Map<MethodControlPoint, Set<MethodControlPoint>> dominanceRelation = new HashMap<MethodControlPoint, Set<MethodControlPoint>>();
		for(MethodControlPoint cp : allNodes) {
			Set<MethodControlPoint> dominators = new HashSet<MethodControlPoint>();
			dominanceRelation.put(cp, dominators);
			if(cp == graphEntry) {
				dominators.add(cp);
			} else {
				dominators.addAll(allNodes);
			}
		}
		//Execute algorithm
		boolean changes = true;
		while(changes) {
			changes = false;
			for(MethodControlPoint cp : allNodes) {
				if(cp != graphEntry) {
					HashSet<MethodControlPoint> newDominators = null;
					for(MethodControlPoint p : predecessors.get(cp)) {
						if(newDominators == null) {
							newDominators = new HashSet<MethodControlPoint>();
							newDominators.addAll(dominanceRelation.get(p));
						} else {
							newDominators.retainAll(dominanceRelation.get(p));
						}
					}
					newDominators.add(cp);
					if(!dominanceRelation.get(cp).equals(newDominators)) {
						changes = true;
						dominanceRelation.put(cp, newDominators);
					}
				}
			}
		}
		return dominanceRelation;
	}

	private void eliminateSomeIntermediateVariables(SatisfiabilityChecker satChecker) {
		Map<MethodControlPoint, Set<MethodControlPoint>> dominatorsOf = computeDominance(entry);
		MethodControlPoint mcp = getExitControlPoint();
		if(mcp != null) {
			//TODO - in case of multiple infinite loops we do not have an exit (?)
			eliminateSomeIntermediateVariablesProcess(dominatorsOf, satChecker, new HashSet<MethodControlPoint>(), mcp);
		}
	}
	
	private void eliminateSomeIntermediateVariablesProcess(Map<MethodControlPoint, Set<MethodControlPoint>> dominatorsOf, SatisfiabilityChecker satChecker, Set<MethodControlPoint> visited, MethodControlPoint current) {
		if(visited.contains(current)) {
			return;
		}
		visited.add(current);
		for(MethodPath mp : this.getIncommingPaths(current)) {
			eliminateSomeIntermediateVariablesProcess(dominatorsOf, satChecker, visited, mp.getFrom());
		}
		//For the current control-point search all incoming (non-back) edges 
		//for common updates; if the right hand side of the update does not 
		//depend on variables updated in the loop propagate the update
		if(current.getOutgoingPaths().size() == 0) {
			return;
		}
		Iterator<MethodPath> incommingIt = this.getIncommingPaths(current).iterator();
		List<MethodPath> allNonBackEdges = new ArrayList<MethodPath>();
		List<MethodPath> allBackEdges = new ArrayList<MethodPath>();
		while(incommingIt.hasNext()) {
			MethodPath anInPath = incommingIt.next();
			boolean backEdge = false;
			for(MethodControlPoint aPoint : dominatorsOf.get(anInPath.getFrom())) {
				if(aPoint == anInPath.getTo()) {
					backEdge = true;
				}
			}
			if(!backEdge) {
				allNonBackEdges.add(anInPath);
			} else {
				allBackEdges.add(anInPath);
			}
		}
		if(!allNonBackEdges.isEmpty()) {
			//Compute the set of all updated variables with exactly the same expression in the right side 
			Map<TracedVariable, ProgramExpression> updates = allNonBackEdges.get(0).getVariableExpressions();
			List<TracedVariable> removedVar = new ArrayList<TracedVariable>();
			List<ProgramExpression> removedVarExp = new ArrayList<ProgramExpression>();
			nextVar:for(TracedVariable tv : updates.keySet()) {
				if(tv instanceof ProgramSubstitutableVariable == false || tv.isObservedGlobally()) continue;
				ProgramExpression pe = updates.get(tv);
				for(int i = 1; i < allNonBackEdges.size(); i++) {
					Map<TracedVariable, ProgramExpression> myUpdates = allNonBackEdges.get(i).getVariableExpressions();
					if(!myUpdates.keySet().contains(tv) || !myUpdates.get(tv).structuralEquals(pe)) {
						continue nextVar;
					}
				}
				removedVar.add(tv);
				removedVarExp.add(pe);
			}
			//Eliminate tv whose right value depends on variables updated in the loop
			//At this moment we only simplify for non-nested loop
			for(MethodPath mp : allBackEdges) {
				if(mp.getTo() != mp.getFrom()) {
					removedVar.clear();
					removedVarExp.clear();
					break;
				}
			}
			nextVar:for(int i = 0; i < removedVar.size(); i++) {
				TracedVariable tv = removedVar.get(i);
				ProgramExpression pe = removedVarExp.get(i);
				Set<ProgramExpression> variablesInRHS = pe.getVariables();
				//Check the loop
				for(MethodPath mp : allBackEdges) {
					for(TracedVariable atv : mp.getVariableExpressions().keySet()) {
						if(atv.equals(tv) || variablesInRHS.contains(atv) || atv.getSubVariables().contains(tv)) {
							continue nextVar;
						}
					}
				}
				//Remove variables and perform the substitution
				for(MethodPath mp : allNonBackEdges) {
					mp.remove(tv);
				}
				//Propagate the update
				for(MethodPath outPath : current.getOutgoingPaths()) {
					outPath.substitute((ProgramSubstitutableVariable) tv, pe);
				}	
			}
		}
	}
	
	private void interpointsSimplification(SatisfiabilityChecker satChecker) {
		LinkedList<MethodControlPoint> workList = new LinkedList<MethodControlPoint>();
		LinkedList<MethodPath> toRemove = new LinkedList<MethodPath>();
		Map<MethodControlPoint, Set<MethodControlPoint>> dominatorsOf = computeDominance(entry);
		workList.add(entry);
		int index = 0;
		while(index < workList.size()) {
			MethodControlPoint current = workList.get(index);
			index++;
			toRemove.clear();
			for(MethodPath outPath : current.getOutgoingPaths()) {
				if(!workList.contains(outPath.getTo())) {
					workList.add(outPath.getTo());
				}
			}
			//Build the disjunction of all incoming, non-loop guards
			GuardExpression incommingDisjunction = null;
			Iterator<MethodPath> incommingIt = this.getIncommingPaths(current).iterator();
			while(incommingIt.hasNext()) {
				MethodPath aMP = incommingIt.next();
				boolean backEdge = false;
				for(MethodControlPoint aPoint : dominatorsOf.get(aMP.getFrom())) {
					if(aPoint == aMP.getTo()) {
						backEdge = true;
					}
				}
				if(!backEdge) {
					GuardExpression theGuard = aMP.getGuard();
					for(TracedVariable tv : aMP.getVariableExpressions().keySet()) {
						if(tv instanceof ProgramSubstitutableVariable) {
							theGuard = theGuard.substitute((ProgramSubstitutableVariable)tv, aMP.getVariableExpressions().get(tv));
						}
					}
					if(incommingDisjunction == null) {
						incommingDisjunction = theGuard;
					} else {
						incommingDisjunction = new GuardOrExpression(incommingDisjunction, theGuard);
					}
				}
			}
			//Outgoing paths should not be in contradiction with incoming non-loop edges
			if(incommingDisjunction != null) {
				for(MethodPath outPath : current.getOutgoingPaths()) {
					GuardExpression toCheck = new GuardAndExpression(incommingDisjunction, outPath.getGuard());
					toCheck = GuardSimplifier.simplify(toCheck);
					if(satChecker != null && !satChecker.mayBeSatisfiable(toCheck)) {
						System.err.println("Infeasible transition found:");
						System.err.println(incommingDisjunction);
						System.err.println(outPath.getGuard());
						System.err.println("");
						toRemove.add(outPath);
					}
				}
			}
			for(MethodPath mp : toRemove) {
				current.removeOutgoing(mp);
			}
		}
	}
}
