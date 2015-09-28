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
package ro.ieat.isummarize.webgoattechnology;

import java.util.ArrayList;
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
import ro.ieat.isummarize.MethodSummaryAlgorithm;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramIndexExpression;
import ro.ieat.isummarize.TechnologySpecifier;
import ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier;
import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute;

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
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;

public class WebgoatSpecifier extends TechnologySpecifier implements Observer {
	
	private final static String CSS_CLASSNAMES[] = {
		"Lorg/owasp/webgoat/lessons/GoatHillsFinancial/Login",		
		"Lorg/owasp/webgoat/lessons/RoleBasedAccessControl/DeleteProfile"
		//"Lorg/owasp/webgoat/lessons/GoatHillsFinancial/ListStaff",		
		//"Lorg/owasp/webgoat/lessons/GoatHillsFinancial/SearchStaff",
		//"Lorg/owasp/webgoat/lessons/RoleBasedAccessControl/ViewProfile",
		//"Lorg/owasp/webgoat/lessons/RoleBasedAccessControl/EditProfile",		
		//"Lorg/owasp/webgoat/lessons/GoatHillsFinancial/Logout",				
		//"Lorg/owasp/webgoat/lessons/GoatHillsFinancial/FindProfile",
		//"Lorg/owasp/webgoat/lessons/RoleBasedAccessControl/UpdateProfile",
	};
	
	private final static String CSS_METHODNAMES[] = {
		"handleRequest"
	};
	
	private final static String CSS_METHODDESCRIPTORS[] = {
		"(Lorg/owasp/webgoat/session/WebSession;)V"
	};//must have the same number of elements as CSS_METHODNAMES[]
	
	private JSPTomcatTranslatedSpecifier tts;
	private boolean captureOutput;
	
	public WebgoatSpecifier(TechnologySpecifier next) {
		super(next);
		tts = new JSPTomcatTranslatedSpecifier(null);
		this.captureOutput = false;
	}

	public WebgoatSpecifier(boolean captureOutput, TechnologySpecifier next) {
		super(next);
		tts = new JSPTomcatTranslatedSpecifier(null);
		this.captureOutput = captureOutput;
	}
	  
	private ArrayList<Entrypoint> res = new ArrayList<Entrypoint>();

	public List<Entrypoint> getEntrypoints(ClassHierarchy ch) {
		res.clear();
		Iterator<IClass> itClass = null;
		itClass = ch.getLoader(ClassLoaderReference.Application).iterateAllClasses();
		while(itClass.hasNext()) {
			IClass theWalaClass = itClass.next();
			Iterator<IMethod> itMethod = theWalaClass.getDeclaredMethods().iterator();
			for(int i=0; i<CSS_CLASSNAMES.length; i++)
				if(theWalaClass.getName().toString().equals(CSS_CLASSNAMES[i]))
					while(itMethod.hasNext()) {
						IMethod theWalaMethod = itMethod.next();	
						for(int j=0; j<CSS_METHODNAMES.length; j++)
						{
							if(theWalaMethod.getName().toString().equals(CSS_METHODNAMES[j]) && 
								theWalaMethod.getDescriptor().toString().equals(CSS_METHODDESCRIPTORS[j])) {
								res.add(new DefaultEntrypoint(theWalaMethod, ch));
							}
						}
					}
				
		}
		System.out.println("Entry points:"+res);
		return res;		
	}

	@Override
	public boolean replaceWithAbstractVariable(SSAInvokeInstruction instruction, int instructionIndex, SymbolTable symTable, SSACFG currentCFG, List<MethodPath> res, CGNode node, PointerAnalysis pointsTo, TypeInference tInference, MethodSummaryAlgorithm runningAlgorithm) {
		boolean result = tts.handleReplaceWithAbstractVariable(instruction, instructionIndex, symTable, currentCFG, res, node, pointsTo, tInference, runningAlgorithm);
		if(result) {
			return true;
		}
		ModelFactory modelFactory = runningAlgorithm.getModelFactory();
		MethodReference theInvokedMethod = instruction.getDeclaredTarget();
			
		if(theInvokedMethod.getName().toString().equals("getBooleanSessionAttribute")){
				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(),  tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
				ProgramSessionAttribute psExpr = new ProgramSessionAttribute(modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(2)));
				ProgramExpression newExpr = modelFactory.createProgramConversionExpressionCAST2INT(psExpr);		
				GuardNotExpression notNullParam = new GuardNotExpression(
		    			new GuardProgramExpression(
		    					modelFactory.createProgramRelationalExpression(
		    							IConditionalBranchInstruction.Operator.EQ, 
		    							psExpr, 
		    							modelFactory.createProgramIndexExpressionNull()))
    					);
				for(MethodPath mp : res) {
    				mp.setGuard(new GuardAndExpression(notNullParam,mp.getGuard()));
					mp.substitute(toSub, newExpr);
				}
				//Record information about the type of attribute
				psExpr.recordType(attr2Types,null);
				psExpr.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
		    	//End
				return true;
			}
			
			if(theInvokedMethod.getName().toString().equals("getIntSessionAttribute")) {
				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
				ProgramSessionAttribute psExpr = new ProgramSessionAttribute(
						modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(2))
					);
				ProgramExpression newExpression = modelFactory.createProgramConversionExpressionSTRING2INT(psExpr);
				for(MethodPath mp : res) {
					mp.substitute(toSub, newExpression);
				}
				//Record information about the type of attribute
				psExpr.recordType(attr2Types,null);
				psExpr.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
		    	//End
				return true;
			}
			
			if(theInvokedMethod.getName().toString().equals("getLessonName")) {
				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
				ProgramExpression newExpression = modelFactory.createProgramIndexExpression("CrossSiteScripting");
				for(MethodPath mp : res) {
					mp.substitute(toSub, newExpression);
				}
				return true;
			}
			
			if(theInvokedMethod.getName().toString().equals("getIntParameter")) {
				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
				ProgramExpression newExpression = 
						modelFactory.createProgramConversionExpressionSTRING2INT(
							new ProgramRequestParameter(
										modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1))
									)
						);
				for(MethodPath mp : res) {
					mp.substitute(toSub, newExpression);
				}
				return true;
			}

			if(theInvokedMethod.getName().toString().equals("getStringParameter")) {
				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
				ProgramExpression newExpression = new ProgramRequestParameter(modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1)));
				for(MethodPath mp : res) {
					mp.substitute(toSub, newExpression);
				}
				return true;
			}

			if(theInvokedMethod.getName().toString().equals("getRawParameter")) {
				ProgramIndexExpression toSub = modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getDef());
				ProgramExpression newExpression = 
						new ProgramRequestParameter(
								modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex), instruction.getUse(1))
						);
				for(MethodPath mp : res) {
					mp.substitute(toSub, newExpression);
				}
				return true;
			}

			if(theInvokedMethod.getName().toString().equals("setSessionAttribute")){// || theInvokedMethod.getName().toString().equals("setRequestAttribute")) {
	    		ProgramIndexExpression newExp = modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(3));
	    		ProgramSessionAttribute toSub = new ProgramSessionAttribute(modelFactory.createProgramIndexExpression(runningAlgorithm.getCurrentIR(), tInference, symTable, currentCFG, currentCFG.getProgramCounter(instructionIndex),instruction.getUse(2)));
		    	for(MethodPath mp : res) {
					mp.add(toSub,newExp);
					mp.substitute(toSub, newExp);
				}
		    	//Identify the type of session attributes
		    	toSub.recordType(attr2Types,tInference.getType(instruction.getUse(3)));
		    	toSub.setClassHierarchy(currentCFG.getMethod().getClassHierarchy());
		    	//End
				return true;
			}

			if(theInvokedMethod.getName().toString().equals("getConnection") 
					|| theInvokedMethod.getName().toString().equals("getParser") 
					|| (theInvokedMethod.getName().toString().equals("handleRequest") && theInvokedMethod.getDeclaringClass().getName().toString().equals("Lorg/owasp/webgoat/lessons/GoatHillsFinancial/ListStaff"))) {
				return true;
			}
			
		return false;
	}
	
	private Map<ProgramSessionAttribute,Set<TypeAbstraction>> attr2Types;

	@Override
	public void update(Observable o, Object arg) {
		tts.update(o, arg);
		if(arg == MethodSummaryAlgorithm.EVENT.BEFORESTART) {
			attr2Types = tts.getAttr2TypesObject();
		}
	}
	
	protected void handleInit(MethodSummaryAlgorithm rootRunningAlgorithm) {
		rootRunningAlgorithm.addObserver(this);
	}
	
}
