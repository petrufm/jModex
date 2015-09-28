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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Set;

import ro.ieat.isummarize.collections.CollectionTechnologySpecifier;
import ro.ieat.isummarize.collections.ProgramCollectionGet;
import ro.ieat.isummarize.utils.GuardCreateDisjunctionSimplifiedIfFundamentalConjunctions;
import ro.ieat.isummarize.utils.GuardSimplifier;
import ro.ieat.jmodex.utils.CancelException;
import ro.ieat.jmodex.utils.NullProgressMonitor;
import ro.ieat.jmodex.utils.jModexProgressMonitor;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAInstruction.Visitor;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

public final class MethodSummaryAlgorithm extends Observable {

	public enum EVENT {
		BEFORESTART
	}
	
	private SatisfiabilityChecker satChecker;
	private TechnologySpecifier technologySpecifier;
	private CallGraphBuilder callGraphBuilder;
	private File fileDeclaringAbstractedLocals;
	
	private ModelFactory modelFactory;
	public ModelFactory getModelFactory() {
		return modelFactory;
	}
	
	final static boolean DEBUG = true;
	int debug_tabs = 0;

	public MethodSummaryAlgorithm(CallGraphBuilder callGraphBuilder) {
		satChecker = new AtomicConstantContraditionBasedChecker();
		this.callGraphBuilder = callGraphBuilder;
		this.technologySpecifier = new CollectionTechnologySpecifier(new StringTechnologySpecifier(new NumericObjectsTechnologySpecifier(callGraphBuilder.getTechnologySpecifier())));
		this.technologySpecifier.init(this);
		modelFactory = ModelFactory.getInstance();
	}

	public MethodSummaryAlgorithm(CallGraphBuilder callGraphBuilder, File abstractedLocalVariables) {
		this(callGraphBuilder);
		fileDeclaringAbstractedLocals = abstractedLocalVariables;
	}

	public MethodSummaryAlgorithm(SatisfiabilityChecker satChecker, CallGraphBuilder callGraphBuilder) {
		this.satChecker = satChecker;
		this.callGraphBuilder = callGraphBuilder;
		this.technologySpecifier = new CollectionTechnologySpecifier(new StringTechnologySpecifier(new NumericObjectsTechnologySpecifier(callGraphBuilder.getTechnologySpecifier())));
		this.technologySpecifier.init(this);
		modelFactory = ModelFactory.getInstance();
	}
	
	private CallGraph lastCallGraph;

	public CallGraph getLastCallGraph() {
		return lastCallGraph;
	}
	
	private HashMap<IMethod,MethodSummary> method2summary;
	private Map<IMethod,MethodSummary> algorithResult;
	private Map<IMethod, CallGraph> callgraph4Entrypoints;
	private jModexProgressMonitor monitor;
	private Set<IMethod> forcedEntriesSet;
	
	public synchronized Map<IMethod,MethodSummary> getAlgorithmResults() {
		return Collections.unmodifiableMap(algorithResult);
	}

	public synchronized SatisfiabilityChecker getSATChecker() {
		return satChecker;
	}
	
	private boolean hasObjects;
	
	public synchronized Map<IMethod,MethodSummary> analyzeIsolated(jModexProgressMonitor monitor) throws RecursionDetected, CallGraphBuildingException, CancelException {
		hasObjects = false;
		this.setChanged();
		this.notifyObservers(EVENT.BEFORESTART);
		debug_tabs = 0;
		algorithResult = new HashMap<IMethod,MethodSummary>();
		forcedEntriesSet = new HashSet<IMethod>();
		this.monitor = monitor;
		callgraph4Entrypoints = callGraphBuilder.getCallGraph(true);
		modelFactory.reset();
		if(fileDeclaringAbstractedLocals != null) {
			modelFactory.loadAbstractedVariables(fileDeclaringAbstractedLocals);
		}
		monitor.beginTask("Generate automaton for each entry point", callgraph4Entrypoints.entrySet().size());
		for(IMethod anEntry : callgraph4Entrypoints.keySet()) {
			if(algorithResult.containsKey(anEntry)) {
				continue;
			}
			method2summary = new HashMap<IMethod, MethodSummary>();
			depthFirstVisit(callgraph4Entrypoints.get(anEntry),callgraph4Entrypoints.get(anEntry).getFakeRootNode(), callGraphBuilder.getPointsTo(true).get(anEntry), monitor);
			if(anEntry == null) {
				algorithResult.put(null,method2summary.get(callgraph4Entrypoints.get(anEntry).getFakeRootNode().getMethod()));
			} else {
				algorithResult.put(anEntry,method2summary.get(anEntry));
			}
			monitor.worked(1);
		}
		for(IMethod tmp : algorithResult.keySet()) {
			algorithResult.get(tmp).finalizeConstruction(satChecker);
		}
		if(hasObjects) {
			MethodSummary msInit = algorithResult.get(null);
			for(MethodPath mp : msInit.getEntryControPoint().getOutgoingPaths()) {
				mp.substitute(modelFactory.createProgramUniqueInstanceCounter(), modelFactory.createProgramUniqueInstanceCounterAction(ProgramUniqueInstanceCounterAction.Action.INIT,null));
				mp.add(modelFactory.createProgramUniqueInstanceCounter(), modelFactory.createProgramUniqueInstanceCounterAction(ProgramUniqueInstanceCounterAction.Action.INIT,null));
			}
		}
		Map<IMethod, MethodSummary> unmodifiableMap = Collections.unmodifiableMap(algorithResult);
		monitor.done();
		return unmodifiableMap;
	}
	
	private MethodSummaryAlgorithm(MethodSummaryAlgorithm cloned) {
		algorithResult = new HashMap<IMethod,MethodSummary>();
		callgraph4Entrypoints = new HashMap<IMethod,CallGraph>();
		forcedEntriesSet = cloned.forcedEntriesSet;
		algorithResult.putAll(cloned.algorithResult);
		callgraph4Entrypoints.putAll(cloned.callgraph4Entrypoints);
		callGraphBuilder = cloned.callGraphBuilder;
		satChecker = cloned.satChecker;
		technologySpecifier = cloned.technologySpecifier;
		monitor = cloned.monitor;
		modelFactory = ModelFactory.getInstance();
		fileDeclaringAbstractedLocals = cloned.fileDeclaringAbstractedLocals;
	}

	private MethodSummary forceAnalysisBefore(Entrypoint analyzeThisFirst) throws CancelException, RecursionDetected {
		method2summary = new HashMap<IMethod, MethodSummary>();
		try {
			depthFirstVisit(callgraph4Entrypoints.get(analyzeThisFirst.getMethod()),callgraph4Entrypoints.get(analyzeThisFirst.getMethod()).getFakeRootNode(), callGraphBuilder.getPointsTo(true).get(analyzeThisFirst.getMethod()), monitor);
		} catch (CallGraphBuildingException e) {}	
		monitor.worked(1);
		return method2summary.get(analyzeThisFirst.getMethod());
	}

	public synchronized void mustBeAnalyzedBefore(Entrypoint analyzeThisFirst) throws RecursionDetected, CancelException {
		if(forcedEntriesSet.contains(analyzeThisFirst.getMethod())) {
			throw new RecursionDetected("Recursive dependency in the order in which entry points must be analyzed!");
		}
		forcedEntriesSet.add(analyzeThisFirst.getMethod());
		MethodSummaryAlgorithm newAlgorithm = new MethodSummaryAlgorithm(this);
		MethodSummary ms = newAlgorithm.forceAnalysisBefore(analyzeThisFirst);
		algorithResult.put(analyzeThisFirst.getMethod(), ms);
		forcedEntriesSet.remove(analyzeThisFirst.getMethod());
	}

	public synchronized Map<IMethod,MethodSummary> analyzeTogether() throws RecursionDetected, CallGraphBuildingException, CancelException {
		this.setChanged();
		this.notifyObservers(EVENT.BEFORESTART);
		debug_tabs = 0;
		algorithResult = method2summary = new HashMap<IMethod, MethodSummary>();
		callgraph4Entrypoints = callGraphBuilder.getCallGraph(false);
		modelFactory.reset();
		if(fileDeclaringAbstractedLocals != null) {
			modelFactory.loadAbstractedVariables(fileDeclaringAbstractedLocals);
		}
		lastCallGraph = callgraph4Entrypoints.get(null);
		depthFirstVisit(lastCallGraph,callgraph4Entrypoints.get(null).getFakeRootNode(),callGraphBuilder.getPointsTo(false).get(null), new NullProgressMonitor());
		for(IMethod tmp : algorithResult.keySet()) {
			algorithResult.get(tmp).finalizeConstruction(satChecker);
		}
		Map<IMethod, MethodSummary> unmodifiableMap = Collections.unmodifiableMap(method2summary);
		return unmodifiableMap;
	}
		
	private boolean isLibraryClass(com.ibm.wala.classLoader.IMethod iMethod) {
		Iterator<IClass> classIt = iMethod.getClassHierarchy().getLoader(ClassLoaderReference.Application).iterateAllClasses();
		while(classIt.hasNext()) {
			IClass theWalaClass = classIt.next();
			if(iMethod.getDeclaringClass().equals(theWalaClass)) {
				return false;
			}
		}
		return true;
	}

	private final MethodSummary NOT_COMPUTED_YET = new MethodSummary(null, null); 

	private void depthFirstVisit(CallGraph cg, CGNode node, PointerAnalysis pointsTo, jModexProgressMonitor monitor) throws RecursionDetected, CancelException {
		try {
			if(!cg.getFakeRootNode().getMethod().getDeclaringClass().equals(node.getMethod().getDeclaringClass()) && 
					isLibraryClass(node.getMethod())) {
				return;
			}
			MethodSummary methodSummary = method2summary.get(node.getMethod());
			currentCallGraph = cg;
			if(methodSummary == NOT_COMPUTED_YET) {
				throw new RecursionDetected();			
			}
			if(methodSummary != null) {
				return;
			}
			if(DEBUG) {
				String tabs = "";
				for(int i = 0; i < debug_tabs; i++) {
					tabs += '\t';
				}
				debug_tabs++;
				System.err.println(tabs + "Start analyzing:" + node.getMethod());
			}
			method2summary.put(node.getMethod(), NOT_COMPUTED_YET);		
			Iterator<CGNode> it = cg.getSuccNodes(node);
			while(it.hasNext()) {
				depthFirstVisit(cg, it.next(), pointsTo, monitor);
			}
			MethodSummary result = computeMethodSummary(node,pointsTo, monitor);
			if(DEBUG) {
				debug_tabs--;
				String tabs = "";
				for(int i = 0; i < debug_tabs; i++) {
					tabs += '\t';
				}
				System.err.println("\n" + tabs + "Finish analyzing:" + node.getMethod() + " " + (result.getEntryControPoint().getOutgoingPaths().size()));
			}
			method2summary.put(node.getMethod(), result);

		} catch(RecursionDetected e) {
			System.err.println(node.getMethod() + "\n");
			throw e;
		}
	}
	//Method analysis
	private HashMap<ISSABasicBlock,List<MethodPath>> results = new HashMap<ISSABasicBlock,List<MethodPath>>();
	private InstructionVisitor backwardInstructionVisitor = new InstructionVisitor();
	
	private CallGraph currentCallGraph;
	private CGNode currentCallGraphNode;
	private IR currentNodeCode;
	
	public IR getCurrentIR() {
		return currentNodeCode;
	}
	
	private MethodSummary computeMethodSummary(CGNode node, PointerAnalysis pointsTo, jModexProgressMonitor monitor) throws CancelException {
		monitor.subTask("Process method " + node.getMethod().getDeclaringClass().getName().toString() + "/" + node.getMethod().getName().toString());
		currentCallGraphNode = node;
		results.clear();
		currentNodeCode = node.getIR();
		computeLoops(currentNodeCode.getControlFlowGraph());
		TypeInference tInference = TypeInference.make(currentNodeCode, true);
		tInference.solve();
		backwardBasicBlockVisit(currentNodeCode.getSymbolTable(),currentNodeCode.getControlFlowGraph(),currentNodeCode.getControlFlowGraph().entry(), node, pointsTo, tInference, monitor);
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		for(MethodPath path : results.get(currentNodeCode.getControlFlowGraph().entry())) {
			path.addFromControlPoint(entryControlPoint);
		}
		MethodSummary res = new MethodSummary(node.getMethod(), entryControlPoint);
		results.clear();
		return res;
	}
	
	private HashMap<ISSABasicBlock,List<ISSABasicBlock>> loopMap = new HashMap<ISSABasicBlock,List<ISSABasicBlock>>();
		
	private void computeLoops(SSACFG ssacfg) {
		loopMap.clear();
		Dominators<ISSABasicBlock> dominators = Dominators.make(ssacfg, ssacfg.entry());
		Iterator<ISSABasicBlock> treeNodeIterator = dominators.dominatorTree().iterator();
		while(treeNodeIterator.hasNext()) {
			ISSABasicBlock h = treeNodeIterator.next();
			Iterator<ISSABasicBlock> hPredIterator = ssacfg.getPredNodes(h);
			while(hPredIterator.hasNext()) {
				ISSABasicBlock n = hPredIterator.next();
				if(dominators.isDominatedBy(n, h)) {
					LinkedList<ISSABasicBlock> loop = new LinkedList<ISSABasicBlock>();
					if(h != n) {
						loop.add(n);
					}
					int index = 0;
					while(index < loop.size()) {
						ISSABasicBlock current = loop.get(index);
						Iterator<ISSABasicBlock> predIterator = ssacfg.getPredNodes(current);
						while(predIterator.hasNext()) {
							ISSABasicBlock inLoopNode = predIterator.next();
							if(inLoopNode != h && !loop.contains(inLoopNode)) {
								loop.add(inLoopNode);
							}
						}
						index++;
					}
					if(!loopMap.containsKey(h)) {
						loopMap.put(h, loop);
					} else {
						loopMap.get(h).addAll(loop);
					}
				}				
			}
		}
	}
	
	private boolean isInLoop(ISSABasicBlock bb) {
		for(ISSABasicBlock header : loopMap.keySet()) {
			if(bb == header) {
				return true;
			}
			if(loopMap.get(header).contains(bb)) {
				return true;
			}
		}
		return false;
	}
		
	private List<MethodPath> VISITED = Collections.unmodifiableList(new ArrayList<MethodPath>());
	private void backwardBasicBlockVisit(SymbolTable symbolTable, SSACFG ssacfg, ISSABasicBlock bb, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, jModexProgressMonitor monitor) throws CancelException {
		
		if(results.containsKey(bb)) {
			return;
		}
		
		results.put(bb, VISITED);
		
		if(!loopMap.containsKey(bb)) {

			//Not loop header
			Iterator<ISSABasicBlock> succIt = ssacfg.getSuccNodes(bb);
			while(succIt.hasNext()) {
				ISSABasicBlock aSucc = succIt.next();
				backwardBasicBlockVisit(symbolTable,ssacfg,aSucc, node, pointsTo, tInference, monitor);
			}
			results.put(bb,backwardInstructionVisitor.process(symbolTable, ssacfg, bb, false, node, pointsTo, tInference, monitor));								
		
		} else {
		
			//Loop header control point
			MethodControlPoint headerControlPoint = MethodControlPoint.createMethodControlPoint();

			//All the loop members
			Set<ISSABasicBlock> allBasicBlocksInLoop = new HashSet<ISSABasicBlock>();
			allBasicBlocksInLoop.add(bb);
			allBasicBlocksInLoop.addAll(loopMap.get(bb));

			//Visit the basic blocks outside the loop appearing after a basic block in the loop (including the header)
			for(ISSABasicBlock aBasicBlockInLoop : allBasicBlocksInLoop) {
				Iterator<ISSABasicBlock> succIt = ssacfg.getSuccNodes(aBasicBlockInLoop);
				while(succIt.hasNext()) {
					ISSABasicBlock aSucc = succIt.next();
					if(!allBasicBlocksInLoop.contains(aSucc)) {
						backwardBasicBlockVisit(symbolTable,ssacfg,aSucc,node,pointsTo,tInference, monitor);
					}
				}
			}
			
			//For relevant variable computation
			Set<ProgramExpression> relevantVariables = new HashSet<ProgramExpression>();
			Set<ProgramExpression> lastRelevantVariables = new HashSet<ProgramExpression>();
			
			boolean done;
			do {
				
				//Prepare a fake input of the header capturing all relevant variable
				MethodPath fakeHeaderInputPathsToLoop = new MethodPath(new GuardTrueExpression(), headerControlPoint);

				for(ProgramExpression exp : lastRelevantVariables) {
					fakeHeaderInputPathsToLoop.add((TracedVariable) exp, exp);
				}
				
				Iterator<SSAPhiInstruction> phiIterator = bb.iteratePhis();
				while(phiIterator.hasNext()) {
					SSAPhiInstruction phi = phiIterator.next();
					int pcIndex = 0;
					try {
						pcIndex = ssacfg.getProgramCounter(bb.getLastInstructionIndex());
					} catch(ArrayIndexOutOfBoundsException e) {
						if(DEBUG) {
							System.err.print(" Phi without pcIndex! ");
						}
					}
					ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symbolTable,ssacfg,pcIndex,phi.getDef());
					ArrayList<ProgramIndexExpression> allPhiUses = new ArrayList<ProgramIndexExpression>();
					for(int i = 0; i < phi.getNumberOfUses(); i++) {
						allPhiUses.add(modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symbolTable,ssacfg,pcIndex,phi.getUse(i)));
					}
					ProgramPhiExpression newExp = new ProgramPhiExpression(phi,allPhiUses);
					fakeHeaderInputPathsToLoop.substitute(toSub, newExp);
				}

				List<MethodPath> fakeHeaderInputPathsToLoopSet = new ArrayList<MethodPath>();
				fakeHeaderInputPathsToLoopSet.add(fakeHeaderInputPathsToLoop);				
				results.put(bb, fakeHeaderInputPathsToLoopSet);
								
				//Pass the fake path through loop
				done = true;
				Iterator<ISSABasicBlock> succIt = ssacfg.getSuccNodes(bb);
				while(succIt.hasNext()) {
					ISSABasicBlock aSucc = succIt.next();
					if(allBasicBlocksInLoop.contains(aSucc)) {
						for(IBasicBlock aBB : loopMap.get(bb)) {
							results.remove(aBB);
						}
						backwardBasicBlockVisit(symbolTable,ssacfg,aSucc,node,pointsTo,tInference, monitor);
					}
				}
				
				List<MethodPath> headerInputPathsToLoop = 
						backwardInstructionVisitor.process(symbolTable, ssacfg, bb, true, node, pointsTo,tInference,monitor);
				
				//Add the additional variables
				for(MethodPath mp : headerInputPathsToLoop) {
					Map<TracedVariable, ProgramExpression> tmp = mp.getVariableExpressions();
					for(TracedVariable tv: tmp.keySet()) {
						relevantVariables.addAll(tmp.get(tv).getVariables());
					}
					relevantVariables.addAll(mp.getGuard().getVariables());
				}
				if(!lastRelevantVariables.equals(relevantVariables)) {
					done = false;
					lastRelevantVariables.addAll(relevantVariables);
				} else {
					results.put(bb, headerInputPathsToLoop);
				}

			} while(!done);
			
			//Check for a:=a state updates and exclude them
			for(MethodPath mp : results.get(bb)) {
				Map<TracedVariable, ProgramExpression> var2exp = new HashMap<TracedVariable, ProgramExpression>(mp.getVariableExpressions());
				for(TracedVariable tv : var2exp.keySet()) {
					if(tv instanceof ProgramExpression && var2exp.get(tv).structuralEquals((ProgramExpression) tv)) {
						mp.remove(tv);
					}
				}
			}
			
			//Link loop paths to the header control point
			for(MethodPath mp : results.get(bb)) {
				mp.addFromControlPoint(headerControlPoint);
			}

			//Put the path going to the header
			MethodPath headerInputPath = new MethodPath(new GuardTrueExpression(), headerControlPoint);
			for(ProgramExpression exp : relevantVariables) {
				headerInputPath.add((TracedVariable) exp, exp);
			}
			Iterator<SSAPhiInstruction> phiIterator = bb.iteratePhis();
			while(phiIterator.hasNext()) {
				SSAPhiInstruction phi = phiIterator.next();
				int pcIndex = 0;
				try {
					pcIndex = ssacfg.getProgramCounter(bb.getLastInstructionIndex());
				} catch(ArrayIndexOutOfBoundsException e) {
					if(DEBUG) {
						System.err.print(" Phi without pcIndex! ");
					}
				}
				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symbolTable,ssacfg,pcIndex,phi.getDef());
				ArrayList<ProgramIndexExpression> allPhiUses = new ArrayList<ProgramIndexExpression>();
				for(int i = 0; i < phi.getNumberOfUses(); i++) {
					allPhiUses.add(modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symbolTable,ssacfg,pcIndex,phi.getUse(i)));
				}
				ProgramPhiExpression newExp = new ProgramPhiExpression(phi,allPhiUses);
				headerInputPath.substitute(toSub, newExp);
			}
			List<MethodPath> headerInputPathSet = new ArrayList<MethodPath>();
			headerInputPathSet.add(headerInputPath);
			results.put(bb, headerInputPathSet);
		}		

	}
	
	private class InstructionVisitor extends Visitor {
		
		private SymbolTable symTable;
		private SSACFG currentCFG;
		private ISSABasicBlock currentBasicBlock;
		private int instructionIndex;
		private List<MethodPath> res;
		private PointerAnalysis pointsTo;
		private CGNode node;
		private TypeInference tInference;
		
		public List<MethodPath> process(SymbolTable symbolTable, SSACFG ssacfg, ISSABasicBlock bb, boolean skipStartingPhis, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, jModexProgressMonitor monitor) throws CancelException {
			this.symTable = symbolTable;
			this.currentCFG = ssacfg;
			this.currentBasicBlock = bb;
			this.res = new ArrayList<MethodPath>();
			this.pointsTo = pointsTo;
			this.node = node;
			this.tInference = tInference;
			if(DEBUG) {
				if(bb == ssacfg.exit()) {
					String tabs = "";
					for(int i = 0; i < debug_tabs - 1; i++) {
						tabs += '\t';
					}
					System.err.print(tabs);
				}
				System.err.print(bb.getNumber() + " ");
			}
			if(currentCFG.getSuccNodeCount(currentBasicBlock) == 0) {
				res.add(new MethodPath(new GuardTrueExpression(),MethodControlPoint.createMethodControlPoint()));
			}
			if(currentCFG.getSuccNodeCount(currentBasicBlock) == 1) {
				Iterator<ISSABasicBlock> it = ssacfg.getSuccNodes(bb);
				ISSABasicBlock singleSucc = it.next();
				if(currentCFG.getPredNodeCount(singleSucc) > 1 || isInLoop(singleSucc)) {
					for(MethodPath mp : results.get(singleSucc)) {
						MethodPath mpCloned = (MethodPath) mp.clone();
						this.res.add(mpCloned);
						//Solve phi expression substitution according to the pred. basic block 
						it = ssacfg.getPredNodes(singleSucc);
						int usePosition = 0;
						while(it.hasNext() && !it.next().equals(currentBasicBlock)) {
							usePosition++;
						}
						Iterator<SSAPhiInstruction> phiIterator = singleSucc.iteratePhis();
						while(phiIterator.hasNext()) {
							SSAPhiInstruction phi = phiIterator.next();
							int pcIndex = 0;
							try {
								pcIndex = currentCFG.getProgramCounter(instructionIndex);
							} catch(ArrayIndexOutOfBoundsException e) {
								if(DEBUG) {
									System.err.print(" Phi without pcIndex! ");
								}
							}
							ProgramIndexExpression newExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable,currentCFG,pcIndex,phi.getUse(usePosition));
							mpCloned.substitute(newExp, newExp);
						}
					}
				} else {
					this.res.addAll(results.get(singleSucc));
				}
			}
			
			//The case of exceptional paths
			//TODO - this block must be completed (generalized)
			List<ISSABasicBlock> exceptionalSucc = currentCFG.getExceptionalSuccessors(currentBasicBlock);
			if(currentCFG.getSuccNodeCount(currentBasicBlock) > 1 && exceptionalSucc.size() > 0) {
				//Process only the normal successor
				Iterator<ISSABasicBlock> it = ssacfg.getNormalSuccessors(bb).iterator();
				if(it.hasNext()) {
					ISSABasicBlock normalSucc = it.next();
					if(currentCFG.getPredNodeCount(normalSucc) > 1 || isInLoop(normalSucc)) {
						for(MethodPath mp : results.get(normalSucc)) {
							MethodPath mpCloned = (MethodPath) mp.clone();
							this.res.add(mpCloned);
							//Solve phi expression substitution according to the pred. basic block 
							it = ssacfg.getPredNodes(normalSucc);
							int usePosition = 0;
							while(it.hasNext() && !it.next().equals(currentBasicBlock)) {
								usePosition++;
							}
							Iterator<SSAPhiInstruction> phiIterator = normalSucc.iteratePhis();
							while(phiIterator.hasNext()) {
								SSAPhiInstruction phi = phiIterator.next();
								int pcIndex = 0;
								try {
									pcIndex = currentCFG.getProgramCounter(instructionIndex);
								} catch(ArrayIndexOutOfBoundsException e) {
									if(DEBUG) {
										System.err.print(" Phi without pcIndex! ");
									}
								}
								ProgramIndexExpression newExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference,symTable,currentCFG,pcIndex,phi.getUse(usePosition));
								mpCloned.substitute(newExp, newExp);
							}
						}
					} else {
						this.res.addAll(results.get(normalSucc));
					}		
				}
			}
			/*for debug purposes*/int firstIndex = -1;			
			for(instructionIndex = bb.getLastInstructionIndex(); instructionIndex >= bb.getFirstInstructionIndex(); instructionIndex--) {
				if(ssacfg.getInstructions()[instructionIndex] == null) {
					continue;
				}
				/*for debug purposes*/if(firstIndex == -1) firstIndex = instructionIndex;
				ssacfg.getInstructions()[instructionIndex].visit(backwardInstructionVisitor);
			}
			Iterator<SSAPhiInstruction> it = currentBasicBlock.iteratePhis();
			while(!skipStartingPhis && it.hasNext()) {
				SSAPhiInstruction instr = it.next();
				instr.visit(this);
			}
			
			//Check user cancel action
			if(monitor.isCanceled()) {
				throw new CancelException();
			}
			//Compress paths with OR
			LinkedList<MethodPath> workList = new LinkedList<MethodPath>(res);
			res.clear();
			redo:while(!workList.isEmpty()) {
				MethodPath aPath = workList.removeFirst();
				Map<TracedVariable,ProgramExpression> aPathEffect = aPath.getVariableExpressions();
				next_another_path:for(int i = 0; i < workList.size(); i++) {
					MethodPath anotherPath = workList.get(i);
					if(aPath.getTo() != anotherPath.getTo()) {
						continue;
					}
					Map<TracedVariable,ProgramExpression> anotherPathEffect = workList.get(i).getVariableExpressions();
					if(aPathEffect.keySet().size() != anotherPathEffect.keySet().size()) {
						continue;
					}
					for(TracedVariable tv : aPathEffect.keySet()) {
						if(!anotherPathEffect.containsKey(tv) || !aPathEffect.get(tv).structuralEquals(anotherPathEffect.get(tv))) {
							 continue next_another_path;
						}
					}
					workList.remove(i);
					aPath.setGuard(GuardCreateDisjunctionSimplifiedIfFundamentalConjunctions.createOrSimplifiedIfFundamentalConjunctions(aPath.getGuard(),anotherPath.getGuard()));
					workList.add(aPath);
					continue redo;
				}
				res.add(aPath);
			}
			//Simplifications & satisfiability
			for(int i = 0; i < res.size(); i++) {
				MethodPath tmp = res.get(i);
				//Simplify the guard
				tmp.setGuard(GuardSimplifier.simplify(tmp.getGuard()));
				//Eliminate paths with unsatisfiable guards
				if(!satChecker.mayBeSatisfiable(tmp.getGuard())) {
					res.remove(i);
					i--;
				}
			}
			/*for debug purposes*/if(DEBUG) System.err.print("("+res.size()+(firstIndex>0 ? "@"+currentCFG.getMethod().getLineNumber(currentCFG.getProgramCounter(firstIndex)) : "") + ") ");
			return Collections.unmodifiableList(res);
		}
		
		public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
			Iterator<ISSABasicBlock> it = currentCFG.getSuccNodes(currentBasicBlock);
			ISSABasicBlock trueSucc = it.next();
			ISSABasicBlock falseSucc = it.next();
			//TODO: I am not absolutely sure about correctness
			//The subsumed paths probably should contain exactly the same program condition (the
			//same not structurally equals). Additionally, we must do the same thing in other places (e.g. switch)
			List<MethodPath> truePaths = new ArrayList<MethodPath>();
			truePaths.addAll(results.get(trueSucc));
			HashMap<Integer,List<MethodPath>> falsePathsMap = new HashMap<Integer,List<MethodPath>>();
			for(MethodPath mp : results.get(falseSucc)) {
				Integer code = mp.hashCode();
				if(!falsePathsMap.containsKey(code)) {
					falsePathsMap.put(code,new ArrayList<MethodPath>());
				}
				falsePathsMap.get(code).add(mp);
			}
			Iterator<MethodPath> tIt = truePaths.iterator();
			while(tIt.hasNext()) {
				MethodPath a_t_path = tIt.next();
				List<MethodPath> toCompareOnFalse = falsePathsMap.get(a_t_path.hashCode());
				if(toCompareOnFalse != null) {
					another_path:for(int i = 0; i < toCompareOnFalse.size(); i++) {
						MethodPath a_f_path = toCompareOnFalse.get(i);
						if(a_t_path.getTo() == a_f_path.getTo()) {
							if(a_t_path.getGuard() == a_f_path.getGuard()) {
								Map<TracedVariable,ProgramExpression> t = a_t_path.getVariableExpressions();
								Map<TracedVariable,ProgramExpression> f = a_f_path.getVariableExpressions();
								if(t.keySet().size() != f.keySet().size()) {
									continue;
								}
								for(TracedVariable tv : t.keySet()) {
									if(!f.containsKey(tv) || t.get(tv) != f.get(tv)) {
										 continue another_path;
									}
								}
								tIt.remove();
								toCompareOnFalse.remove(i);
								res.add(a_t_path);
								break;
							}
						}
					}
				}
			}
			List<MethodPath> falsePaths = new ArrayList<MethodPath>();
			for(List<MethodPath> aList : falsePathsMap.values()) {
				falsePaths.addAll(aList);
			}
			//end subsume checker
			boolean mustClone = (currentCFG.getPredNodeCount(trueSucc) > 1) || isInLoop(currentBasicBlock);
			GuardProgramExpression trueGuard = new GuardProgramExpression(
					new ProgramRelationalExpression(
							instruction.getOperator(),
							modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)),
							modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1))));
			for(MethodPath mp : truePaths) {
				if(mustClone) {
					mp = (MethodPath) mp.clone();
				}
				mp.setGuard(new GuardAndExpression(mp.getGuard(),trueGuard));
				//Solve phi expressions 
				Iterator<ISSABasicBlock> predIt = currentCFG.getPredNodes(trueSucc);
				int usePosition = 0;
				while(predIt.hasNext() && !predIt.next().equals(currentBasicBlock)) {
					usePosition++;
				}
				Iterator<SSAPhiInstruction> phiIterator = trueSucc.iteratePhis();
				while(phiIterator.hasNext()) {
					SSAPhiInstruction phi = phiIterator.next();
					ProgramIndexExpression newExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference,symTable,currentCFG,currentCFG.getProgramCounter(instructionIndex),phi.getUse(usePosition));
					mp.substitute(newExp, newExp);
				}
				res.add(mp);
			}
			mustClone = (currentCFG.getPredNodeCount(falseSucc) > 1) || isInLoop(currentBasicBlock);
			GuardNotExpression falseGuard = new GuardNotExpression(
					new GuardProgramExpression(
							new ProgramRelationalExpression(
									instruction.getOperator(),
									modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)),
									modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)))));
			for(MethodPath mp : falsePaths) {
				if(mustClone) {
					mp = (MethodPath) mp.clone();
				}
				mp.setGuard(new GuardAndExpression(mp.getGuard(), falseGuard));
				//Solve phi expressions
				Iterator<ISSABasicBlock> predIt = currentCFG.getPredNodes(falseSucc);
				int usePosition = 0;
				while(predIt.hasNext() && !predIt.next().equals(currentBasicBlock)) {
					usePosition++;
				}
				Iterator<SSAPhiInstruction> phiIterator = falseSucc.iteratePhis();
				while(phiIterator.hasNext()) {
					SSAPhiInstruction phi = phiIterator.next();
					ProgramIndexExpression newExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable,currentCFG,currentCFG.getProgramCounter(instructionIndex),phi.getUse(usePosition));
					mp.substitute(newExp, newExp);
				}
				res.add(mp);
    		}
		}

		public void visitBinaryOp(SSABinaryOpInstruction instruction) {
			ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
			ProgramBinaryExpression newExp = new ProgramBinaryExpression(instruction.getOperator(),modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)),modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));
			for(MethodPath mp : res) {
				mp.substitute(toSub, newExp);
			}
		}
		
	    public void visitReturn(SSAReturnInstruction instruction) {
	    	if(!instruction.returnsVoid()) {
	    		ProgramIndexExpression pExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0));
	    		for(MethodPath mp : res) {
	    			mp.add(new ProgramReturnVariable(), pExp);
	    		}
	    	}
	    }

	    public void visitSwitch(SSASwitchInstruction instruction) {
			Iterator<ISSABasicBlock> it = currentCFG.getSuccNodes(currentBasicBlock);
			while(it.hasNext()) {
				ISSABasicBlock aSucc = it.next();
				boolean mustClone = (currentCFG.getPredNodeCount(aSucc) > 1) || isInLoop(currentBasicBlock);
				for(MethodPath mp : results.get(aSucc)) {
					if(mustClone) {
						mp = (MethodPath) mp.clone();
					}
					IntIterator labelsIt = instruction.iterateLabels();
					List<Integer> allLabelToSucc = new ArrayList<Integer>();
					while(labelsIt.hasNext()) {
						int label = labelsIt.next();
						int target = instruction.getTarget(label);
						if(aSucc.getFirstInstructionIndex() <= target && target <= aSucc.getLastInstructionIndex()) {
							allLabelToSucc.add(label);
						}
					}
					if(allLabelToSucc.size() == 1) {
						mp.setGuard(new GuardAndExpression(
							mp.getGuard(),
							new GuardProgramExpression(
									new ProgramRelationalExpression(
											modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)),
											allLabelToSucc.get(0)))));	
					} else if(allLabelToSucc.size() > 1) {
						GuardExpression tmp = 
								new GuardNotExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)),
														allLabelToSucc.get(0))));
						for(int i = 1; i < allLabelToSucc.size(); i++) {
							tmp = new GuardAndExpression( 
									new GuardNotExpression(
											new GuardProgramExpression(
													new ProgramRelationalExpression(
															modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)),
															allLabelToSucc.get(i)))),
									tmp);
						}
						mp.setGuard(new GuardAndExpression(
								mp.getGuard(),
								new GuardNotExpression(tmp)));	
					} else {
						//Default path
						//The guard is a conjunctions of negations of each case of equality
						labelsIt = instruction.iterateLabels();
						GuardExpression guard = mp.getGuard();
						while(labelsIt.hasNext()) {
							int label = labelsIt.next();
							guard = new GuardAndExpression(
									guard, 
									new GuardNotExpression(
											new GuardProgramExpression(
											new ProgramRelationalExpression(
													modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(0)),
													label))));
							}
						mp.setGuard(guard);
					}

					//Solve phi expression substitution according to the pred. basic block 
					Iterator<ISSABasicBlock> predIt = currentCFG.getPredNodes(aSucc);
					int usePosition = 0;
					while(predIt.hasNext() && !predIt.next().equals(currentBasicBlock)) {
						usePosition++;
					}
					Iterator<SSAPhiInstruction> phiIterator = aSucc.iteratePhis();
					while(phiIterator.hasNext()) {
						SSAPhiInstruction phi = phiIterator.next();
						ProgramIndexExpression newExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable,currentCFG,currentCFG.getProgramCounter(instructionIndex),phi.getUse(usePosition));
						mp.substitute(newExp, newExp);
					}
					res.add(mp);
				}

			}
	    }

	    public void visitPhi(SSAPhiInstruction instruction) {
			int pcIndex = 0;
			try {
				pcIndex = currentCFG.getProgramCounter(instructionIndex);
			} catch(ArrayIndexOutOfBoundsException e) {
				if(DEBUG) {
					System.err.print(" Phi without pcIndex! ");
				}
			}
	    	ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, pcIndex,instruction.getDef());
			ArrayList<ProgramIndexExpression> allPhiUses = new ArrayList<ProgramIndexExpression>();
			for(int i = 0; i < instruction.getNumberOfUses(); i++) {
				allPhiUses.add(modelFactory.createProgramIndexExpression(currentNodeCode, tInference, currentNodeCode.getSymbolTable(), currentCFG, pcIndex,instruction.getUse(i)));
			}
	    	ProgramPhiExpression newExp = new ProgramPhiExpression(instruction,allPhiUses);
	    	for(MethodPath mp : res) {
				mp.substitute(toSub, newExp);
			}	    	
	    }
	    
	    public void visitInvoke(SSAInvokeInstruction instruction) {	    	
	    	boolean changed = technologySpecifier.handleReplaceWithAbstractVariable(instruction, instructionIndex, symTable, currentCFG, res, node, pointsTo, tInference, MethodSummaryAlgorithm.this);
	    	if(!changed) {
		    	//Inter-procedurally if possible
		    	Iterator<CallSiteReference> callSiteIt = currentCallGraphNode.iterateCallSites();
		    	CallSiteReference theCallSite = null;
		    	while(callSiteIt.hasNext()) {
		    		//Due to inlining a callsite might be duplicated at IR level
			    	IntSet indexes = currentNodeCode.getCallInstructionIndices(theCallSite = callSiteIt.next());
		    		if(indexes.contains(instructionIndex)) {
		    			break;
		    		}
		    		theCallSite = null;
		    	}
		    	if(theCallSite == null) {
		    		return;
		    	}
		    	//We detected the corresponding call site
		    	Set<CGNode> possibleTargetMethods = currentCallGraph.getPossibleTargets(currentCallGraphNode, theCallSite);
		    	if(possibleTargetMethods.size() == 0 && instruction.hasDef()) {
    				ProgramExpression args[] = new ProgramExpression[instruction.getNumberOfUses()];
    				for(int i = 0; i < instruction.getNumberOfUses(); i++) {
    					args[i] = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(i));
    				}
    				ProgramFunction prgFun = new ProgramFunction(currentCFG.getMethod().getClassHierarchy().resolveMethod(instruction.getDeclaredTarget()), args);
    				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
    				for(MethodPath mp : res) {
    					mp.substitute(toSub, prgFun);
    				}
    				return;
		    	}
		    	for(CGNode aTarget : possibleTargetMethods) {
		    		MethodSummary ms = method2summary.get(aTarget.getMethod());
		    		if(ms == null) {
		    			//The method is not abstracted by a specific behavior and is not analyzed in depth
		    			if(instruction.hasDef()) {
		    				ProgramExpression args[] = new ProgramExpression[instruction.getNumberOfUses()];
		    				for(int i = 0; i < instruction.getNumberOfUses(); i++) {
		    					args[i] = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(i));
		    				}
		    				ProgramFunction prgFun = new ProgramFunction(aTarget.getMethod(), args);
		    				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
		    				for(MethodPath mp : res) {
		    					mp.substitute(toSub, prgFun);
		    				}
		    			}
		    			break;
		    		}
	
		    		//We found a possible invoked implementation, summarized in ms (we should remove irelevant paths)
		    		MethodSummary theSummaryClone = ms.deepClone();
		    		
		    		//Substitute parameters in the summary
	    			TypeInference ti = TypeInference.make(aTarget.getIR(),true);
	    			ti.solve();
		    		for(int i = 0; i < instruction.getNumberOfUses(); i++) {
		    			theSummaryClone.substitute(
		    					modelFactory.createProgramIndexExpression(currentNodeCode, ti, aTarget.getIR().getSymbolTable(), aTarget.getIR().getControlFlowGraph(), aTarget.getIR().getControlFlowGraph().getProgramCounter(0),i + 1),
		    					modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(i))  						
		    					);
		    		}
		    		
		    		//Cartesian product of called exit paths and caller paths (after the call) 
		    		boolean relevant;
		    		if(instruction.hasDef()) {
		    			relevant = theSummaryClone.combineExitsWith(satChecker, res,
		    					modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef()));
		    		} else {
		    			relevant = theSummaryClone.combineExitsWith(satChecker, res,null);	    				
		    		}
		    		
		    		//The caller paths before the call are the called paths (from the beginning)
		    		if(relevant) {
		    			res.clear();
		    			res.addAll(theSummaryClone.getEntryControPoint().getOutgoingPaths());
		    		}
		    			    			
		    		//Analyze subsume
		    		//TODO - Not sure if correct
					/*LinkedList<MethodPath> workList = new LinkedList<MethodPath>(res);
					res.clear();
					while(!workList.isEmpty()) {
						MethodPath aPath = workList.removeFirst();
						Map<TracedVariable,ProgramExpression> aPathEffect = aPath.getVariableExpressions();
						next_another_path:for(int i = 0; i < workList.size(); i++) {
							MethodPath anotherPath = workList.get(i);
							if(aPath.getTo() != anotherPath.getTo()) {
								continue;
							}
							Map<TracedVariable,ProgramExpression> anotherPathEffect = workList.get(i).getVariableExpressions();
							for(TracedVariable tv : aPathEffect.keySet()) {
								if(!anotherPathEffect.containsKey(tv) || !aPathEffect.get(tv).structuralEquals(anotherPathEffect.get(tv))) {
									 continue next_another_path;
								}
							}
							workList.remove(i);
							i--;
						}
						res.add(aPath);
					}*/  		
		    		//TODO - Do not consider polymorphic invocations (ie. multiple targets)
		    		break;
		    	}
	    	}
	    }

	    public void visitGet(SSAGetInstruction instruction) {
	    	if(instruction.isStatic()) {
	    		if(instruction.getDeclaredField().getDeclaringClass().getName().toString().equals("Ljava/lang/Boolean") && instruction.getDeclaredField().getName().toString().equals("TRUE")) {
		    		ProgramIndexExpression valueExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
		    		ProgramIndexExpression newExpr = modelFactory.createProgramIndexExpression(1);
		    		for(MethodPath mp : res) {
						mp.substitute(valueExp, newExpr);
					}	    			
	    			return;
	    		}
	    		if(instruction.getDeclaredField().getDeclaringClass().getName().toString().equals("Ljava/lang/Boolean") && instruction.getDeclaredField().getName().toString().equals("FALSE")) {
		    		ProgramIndexExpression valueExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
		    		ProgramIndexExpression newExpr = modelFactory.createProgramIndexExpression(0);
		    		for(MethodPath mp : res) {
						mp.substitute(valueExp, newExpr);
					}	    			
	    			return;
	    		}
	    		IField aField = currentCFG.getMethod().getClassHierarchy().resolveField(instruction.getDeclaredField());
	    		ProgramField fieldExp = new ProgramField(aField);
	    		ProgramIndexExpression valueExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
				for(MethodPath mp : res) {
					mp.substitute(valueExp, fieldExp);
				}
	    	} else {
	    		IField aField = currentCFG.getMethod().getClassHierarchy().resolveField(instruction.getDeclaredField());
	    		ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
	    		ProgramExpression theObject = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0));
	    		ProgramField fieldExp = new ProgramField(theObject,aField);
	    		for(MethodPath mp : res) {
					mp.substitute(toSub,fieldExp);
	    		}
	    	}
	    }

	    public void visitPut(SSAPutInstruction instruction) {
	    	if(instruction.isStatic()) {
	    		IField aField = currentCFG.getMethod().getClassHierarchy().resolveField(instruction.getDeclaredField());
	    		ProgramField fieldExp = new ProgramField(aField);
	    		ProgramIndexExpression theExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0));
	    		for(MethodPath mp : res) {
					mp.substitute(fieldExp, theExp);
	    			mp.add(fieldExp, theExp);
	    		}
	    	} else {
	    		IField aField = currentCFG.getMethod().getClassHierarchy().resolveField(instruction.getDeclaredField());
	    		ProgramExpression theObject = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0));
	    		ProgramField fieldExp = new ProgramField(theObject,aField);
	    		ProgramIndexExpression theNewExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1));
	    		for(MethodPath mp : res) {
					mp.substitute(fieldExp, theNewExp);
	    			mp.add(fieldExp, theNewExp);
	    		}
	    	}
	    }
	    
	    public void visitCheckCast(SSACheckCastInstruction instruction) {
	    	ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
    		ProgramExpression newExp = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1));
	    	for(MethodPath mp : res) {
	    		mp.substitute(toSub,newExp);
	    	}
	    }
	
	    public void visitThrow(SSAThrowInstruction instruction) {
	    	//TODO - Exceptional paths are neglected
	    	res.clear();
	    }

	    public void visitNew(SSANewInstruction instruction) {
	    	if(instruction.getConcreteType().isClassType()) {
	    		TypeReference theTypeRef = instruction.getConcreteType();
	    		IClass theClass = node.getMethod().getClassHierarchy().lookupClass(theTypeRef);
	    		if(theClass.getClassLoader().equals(theClass.getClassHierarchy().getLoader(ClassLoaderReference.Application))) {
		    		ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
			    	ProgramGetObject newExp = modelFactory.createProgramGetObject(theClass);
			    	ProgramUniqueInstanceCounter uic = modelFactory.createProgramUniqueInstanceCounter();
			    	ProgramUniqueInstanceCounterAction uicAction = modelFactory.createProgramUniqueInstanceCounterAction(ProgramUniqueInstanceCounterAction.Action.INC, modelFactory.createProgramUniqueInstanceCounter());
			    	for(MethodPath mp : res) {
			    		mp.substitute(uic, uicAction);
			    		mp.add(uic,uicAction);
			    		mp.substitute(toSub,newExp);
			    	}
			    	hasObjects = true;
	    		} else {
	    			for(IClass aClass : theClass.getAllImplementedInterfaces()) {
	    				if(aClass.isInterface() && aClass.getName().toString().equals("Ljava/util/Collection")) {
	    					//This is a collection
	    		    		ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
	    			    	ProgramCollectionGet newExp = new ProgramCollectionGet(modelFactory.createProgramUniqueInstanceCounter());
	    			    	ProgramUniqueInstanceCounter uic = modelFactory.createProgramUniqueInstanceCounter();
	    			    	ProgramUniqueInstanceCounterAction uicAction = modelFactory.createProgramUniqueInstanceCounterAction(ProgramUniqueInstanceCounterAction.Action.INC, modelFactory.createProgramUniqueInstanceCounter());
	    			    	for(MethodPath mp : res) {
	    			    		mp.substitute(uic, uicAction);
	    			    		mp.add(uic,uicAction);
	    			    		mp.substitute(toSub,newExp);
	    			    	}
	    			    	hasObjects = true;
	    					break;
	    				}
	    			}
	    		}
	    	}
	    }

	    public void visitComparison(SSAComparisonInstruction instruction) {
	    	ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
    		ProgramExpression newExp = modelFactory.createProgramRelationalExpression(
    			IConditionalBranchInstruction.Operator.EQ,
    			modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(0)),
    		    modelFactory.createProgramIndexExpression(currentNodeCode, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1))
    		);	
 	    	for(MethodPath mp : res) {
	    		mp.substitute(toSub,newExp);
	    	}
	    }

	}	
}
