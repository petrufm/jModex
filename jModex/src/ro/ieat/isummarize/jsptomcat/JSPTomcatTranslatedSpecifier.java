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
package ro.ieat.isummarize.jsptomcat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import ro.ieat.isummarize.GuardAndExpression;
import ro.ieat.isummarize.GuardNotExpression;
import ro.ieat.isummarize.GuardProgramExpression;
import ro.ieat.isummarize.MethodPath;
import ro.ieat.isummarize.MethodSummary;
import ro.ieat.isummarize.MethodSummaryAlgorithm;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.ProgramIndexExpression;
import ro.ieat.isummarize.TechnologySpecifier;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;

public class JSPTomcatTranslatedSpecifier extends TechnologySpecifier  implements Observer {

	private boolean captureOutput;
	private boolean considerRequestParametersNotNull;
	
	public JSPTomcatTranslatedSpecifier(TechnologySpecifier next) {
		super(next);
		this.captureOutput = false;
		this.considerRequestParametersNotNull = false;
	}

	public JSPTomcatTranslatedSpecifier(boolean captureOutput, TechnologySpecifier next) {
		super(next);
		this.captureOutput = captureOutput;
		this.considerRequestParametersNotNull = false;
	}

	private final static String JSPTomcat_MethodName = "_jspService";	
	private final static String JSPTomcat_MethodDescriptor = "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V";
	  
	private ArrayList<Entrypoint> res = new ArrayList<Entrypoint>();

	public List<Entrypoint> getEntrypoints(ClassHierarchy ch) {
		res.clear();
		Iterator<IClass> itClass = null;
		itClass = ch.getLoader(ClassLoaderReference.Application).iterateAllClasses();
		while(itClass.hasNext()) {
			IClass theWalaClass = itClass.next();
			Iterator<IMethod> itMethod = theWalaClass.getDeclaredMethods().iterator();
			while(itMethod.hasNext()) {
				IMethod theWalaMethod = itMethod.next();	
				if(theWalaMethod.getName().toString().equals(JSPTomcat_MethodName) && 
						theWalaMethod.getDescriptor().toString().equals(JSPTomcat_MethodDescriptor)) {
				res.add(new DefaultEntrypoint(theWalaMethod, ch));
				}
			}
		}
		return res;
	}

	@Override
	public boolean replaceWithAbstractVariable(SSAInvokeInstruction instruction, int instructionIndex, SymbolTable symTable, SSACFG currentCFG, List<MethodPath> res, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, MethodSummaryAlgorithm runningAlgorithm) {
		  MethodReference called = instruction.getDeclaredTarget();
		  ModelFactory theModelFactory = runningAlgorithm.getModelFactory();
		  IR theIR = runningAlgorithm.getCurrentIR();		
		  if(called.getName().toString().equals("getParameter") && 
	    	 called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/http/HttpServletRequest") &&
	    	 called.getDescriptor().toString().equals("(Ljava/lang/String;)Ljava/lang/String;")) {
				ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
	    		ProgramRequestParameter newExp = new ProgramRequestParameter(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));	    			
	    		GuardNotExpression notNullParam = null;
	    		if(considerRequestParametersNotNull) {
	    			notNullParam = new GuardNotExpression(
			    			new GuardProgramExpression(
			    					theModelFactory.createProgramRelationalExpression(
			    							IConditionalBranchInstruction.Operator.EQ, 
			    							newExp, 
			    							theModelFactory.createProgramIndexExpressionNull()))
	    					);
	    		}
	    		for(MethodPath mp : res) {
	    			if(considerRequestParametersNotNull) {
	    				mp.setGuard(new GuardAndExpression(notNullParam,mp.getGuard()));
	    			}
					mp.substitute(toSub, newExp);
		    	}
		    	return true;
		  }	    	    
		  if(called.getName().toString().equals("getAttribute") &&
	    	 called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/http/HttpSession") &&
	    	 called.getDescriptor().toString().equals("(Ljava/lang/String;)Ljava/lang/Object;")) {
	    		ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
	    		ProgramSessionAttribute newExp = new ProgramSessionAttribute(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)));
	    		newExp.addData("CONCRETE_TYPES", typesInState);
	    		for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
				}
		    	//Identify the type of session attributes
		    	newExp.recordType(attr2Types,null);
		    	newExp.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
		    	//End
		    	return true;
		  }
		  if(called.getName().toString().equals("getAttribute") &&
			    	 called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/jsp/PageContext") &&
			    	 called.getDescriptor().toString().equals("(Ljava/lang/String;)Ljava/lang/Object;")) {
			    ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
			    ProgramSessionAttribute newExp = new ProgramSessionAttribute(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)));
			    newExp.addData("CONCRETE_TYPES", typesInState);
			    for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
				}
		    	//Identify the type of session attributes
		    	newExp.recordType(attr2Types,null);
		    	newExp.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
		    	//End
				return true;
		  }
		  if(called.getName().toString().equals("getAttribute") &&
			    	 called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/jsp/PageContext") &&
			    	 called.getDescriptor().toString().equals("(Ljava/lang/String;I)Ljava/lang/Object;")) {
			    ProgramIndexExpression toSub = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getDef());
			    ProgramSessionAttribute newExp = new ProgramSessionAttribute(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)));
			    newExp.addData("CONCRETE_TYPES", typesInState);
			    for(MethodPath mp : res) {
					mp.substitute(toSub, newExp);
				}
		    	//Identify the type of session attributes
		    	newExp.recordType(attr2Types,null);
		    	newExp.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
		    	//End
				return true;
		  }
		  if (called.getName().toString().equals("removeAttribute") &&
				  called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/http/HttpSession") &&
		    	  called.getDescriptor().toString().equals("(Ljava/lang/String;)V")) {			  	
		    		ProgramIndexExpression newExp = theModelFactory.createProgramIndexExpressionNull();
		    		ProgramSessionAttribute toSub = new ProgramSessionAttribute(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));
			    	for(MethodPath mp : res) {
						mp.add(toSub,newExp);
						mp.substitute(toSub, newExp);
					}
			    	//Identify the type of session attributes
			    	toSub.recordType(attr2Types,null);
			    	toSub.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
			    	//End
			    	return true;
		   }
		  if (called.getName().toString().equals("setAttribute") &&
			  called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/http/HttpSession") &&
	    	  called.getDescriptor().toString().equals("(Ljava/lang/String;Ljava/lang/Object;)V")) {			  	
	    		ProgramIndexExpression newExp =theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(2));
	    		ProgramSessionAttribute toSub = new ProgramSessionAttribute(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));
		    	for(MethodPath mp : res) {
					mp.add(toSub,newExp);
					mp.substitute(toSub, newExp);
				}
		    	//Identify the type of session attributes
		    	typesInState.add(tInference.getType(instruction.getUse(2)));
		    	toSub.recordType(attr2Types,tInference.getType(instruction.getUse(2)));
		    	toSub.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
		    	//End
		    	return true;
	    	}
		  if (called.getName().toString().equals("setAttribute") &&
				  called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/jsp/PageContext") &&
		    	  called.getDescriptor().toString().equals("(Ljava/lang/String;Ljava/lang/Object;I)V")) {			  	
		    		ProgramIndexExpression newExp =theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(2));
		    		ProgramSessionAttribute toSub = new ProgramSessionAttribute(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));
			    	for(MethodPath mp : res) {
						mp.add(toSub,newExp);
						mp.substitute(toSub, newExp);
					}
			    	//Identify the type of session attributes
			    	typesInState.add(tInference.getType(instruction.getUse(2)));
			    	toSub.recordType(attr2Types,tInference.getType(instruction.getUse(2)));
			    	toSub.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
			    	//End
			    	return true;
		    	}
		  if(captureOutput) {
				  if (
			    	  ((called.getName().toString().equals("println") || called.getName().toString().equals("print")) &&
			    	   (called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/jsp/JspWriter") ||
			    	   (called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/ServletOutputStream"))))
			    	   ||
			    	   (called.getName().toString().equals("write") && 
			    		(called.getDeclaringClass().getName().toString().equals("Ljava/io/Writer") ||
			    		 called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/jsp/JspWriter")))
			    		) {
			    		ProgramOutput toSub = new ProgramOutput();
			    		ProgramOutput newExp = new ProgramOutput(theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1)));
				    	for(MethodPath mp : res) {
							mp.substitute(toSub, newExp);
							mp.add(toSub,newExp); 
						} 		
				    	return true;
				  }
				  if (called.getName().toString().equals("sendRedirect") &&
						  called.getDeclaringClass().getName().toString().equals("Ljavax/servlet/http/HttpServletResponse") &&
						  called.getDescriptor().toString().equals("(Ljava/lang/String;)V")) {
					  		ProgramRedirect newVariable = new ProgramRedirect();
					    	ProgramIndexExpression newVariableExpression = theModelFactory.createProgramIndexExpression(theIR, tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(1));
					  		for(MethodPath mp : res) {
								mp.add(newVariable, newVariableExpression);
							}
					    	return true;
				  }
		  }
		  if(called.getName().toString().equals("include") && 
				  called.getDeclaringClass().getName().toString().equals("Lorg/apache/jasper/runtime/JspRuntimeLibrary")) {
			  if(symTable.isConstant(instruction.getUse(2))) {
				  Entrypoint theIncludedEntry = getEntryPointFor(symTable.getStringValue(instruction.getUse(2)));				  
				  if(theIncludedEntry != null) {
					  if(!runningAlgorithm.getAlgorithmResults().containsKey(theIncludedEntry.getMethod())) {
						  runningAlgorithm.mustBeAnalyzedBefore(theIncludedEntry);
					  }
					  MethodSummary includedMethodSummary = runningAlgorithm.getAlgorithmResults().get(theIncludedEntry.getMethod());
					  //TODO : it is not exactly like that (e.g. the arguments should point to the same objects, etc.)
					  //Process it like a method invocation
					  MethodSummary includedMethodSummaryCloned = includedMethodSummary.deepClone();
					  if(includedMethodSummaryCloned.combineExitsWith(runningAlgorithm.getSATChecker(), res, null)) {
						  res.clear();
						  res.addAll(includedMethodSummaryCloned.getEntryControPoint().getOutgoingPaths());
					  }
					  //Done
					  return true;
				  }
			  } 
			  System.err.println("\n A JSP include has not been solved! \n");				  
			  return false;
		  }
		  return false;
	}
	
	private Entrypoint getEntryPointFor(String fullName) {
		if(fullName.endsWith(".jsp")) {
			String name = fullName.substring(0,fullName.lastIndexOf('.'));
			name = "Lorg/apache/jsp/" + name + "_jsp";
			for(Entrypoint entry : res) {
				if(entry.getMethod().getDeclaringClass().getName().toString().equals(name)) {
					return entry;
				}
			}
		} 
		return null;
	}
	
	protected void handleInit(MethodSummaryAlgorithm rootRunningAlgorithm) {
		rootRunningAlgorithm.addObserver(this);
	}

	private Set<TypeAbstraction> typesInState;
	private Map<ProgramSessionAttribute,Set<TypeAbstraction>> attr2Types;

	@Override
	public void update(Observable o, Object arg) {
		if(arg == MethodSummaryAlgorithm.EVENT.BEFORESTART) {
			typesInState = new HashSet<TypeAbstraction>();
			attr2Types = new HashMap<ProgramSessionAttribute,Set<TypeAbstraction>>();
		}
	}

	public void setNotNullRequestParameters(boolean propertyValue) {
		considerRequestParametersNotNull = propertyValue;
	}
	
	public Set<TypeAbstraction> getTypesInStateSetObject() {
		return typesInState;
	}
	
	public Map<ProgramSessionAttribute,Set<TypeAbstraction>> getAttr2TypesObject() {
		return attr2Types;
	}
}
