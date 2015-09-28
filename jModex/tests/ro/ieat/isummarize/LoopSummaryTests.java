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

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ro.ieat.isummarize.CallGraphBuilder;
import ro.ieat.isummarize.CallGraphBuildingException;
import ro.ieat.isummarize.CallGraphForEclipseJavaProject;
import ro.ieat.isummarize.GuardAndExpression;
import ro.ieat.isummarize.GuardNotExpression;
import ro.ieat.isummarize.GuardProgramExpression;
import ro.ieat.isummarize.GuardTrueExpression;
import ro.ieat.isummarize.MethodControlPoint;
import ro.ieat.isummarize.MethodPath;
import ro.ieat.isummarize.MethodSummary;
import ro.ieat.isummarize.MethodSummaryAlgorithm;
import ro.ieat.isummarize.ProgramBinaryExpression;
import ro.ieat.isummarize.ProgramRelationalExpression;
import ro.ieat.isummarize.ProgramReturnVariable;
import ro.ieat.isummarize.RecursionDetected;
import ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier;
import ro.ieat.tests.utils.TestUtil;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.ssa.IR;

public class LoopSummaryTests {

	@BeforeClass
	public static void setUp() throws ClassHierarchyException, IOException, CoreException, IllegalArgumentException, CallGraphBuilderCancelException, RecursionDetected, CallGraphBuildingException {
		TestUtil.importProject("SummaryTestsProject", "SummaryTestsProject.zip");
		IJavaProject theProject = TestUtil.getProject("SummaryTestsProject");
		CallGraphBuilder cBuilder = new CallGraphForEclipseJavaProject(theProject, new JSPTomcatTranslatedSpecifier(true,null));
		MethodSummaryAlgorithm alg = new MethodSummaryAlgorithm(cBuilder);
		results = alg.analyzeTogether();
		callGraph = alg.getLastCallGraph();
		modelFactory = alg.getModelFactory();
	}
	
	@AfterClass
	public static void tearDown() {
		TestUtil.deleteProject("SummaryTestsProject");
	}
	
	private static Map<IMethod,MethodSummary> results;
	private static CallGraph callGraph;
	private static ModelFactory modelFactory;
	
	private IR findWalaMethod(String theClass, String methodName, String methodDesc) {
		for(IMethod meth : results.keySet()) {	
			if(meth.getName().toString().equals(methodName) && meth.getDescriptor().toString().equals(methodDesc)) {
				Iterator<CGNode> it = callGraph.iterator();
				while(it.hasNext()) {
					CGNode c = it.next();
					if(c.getMethod().equals(meth) && c.getMethod().getDeclaringClass().getName().toString().equals(theClass)) {
						return c.getIR();
					}
				}
			}
		}
		Assert.fail();
		return null;
	}
			
	@Test
	public void loopTest1() {
		IR code = findWalaMethod("Ltestloops/test1/Entry","foo", "(II)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControl = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path1.addFromControlPoint(entryControlPoint);	
		path1.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.GE, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 1), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
										)
								)
						)
		);
		path1.add(new ProgramReturnVariable(), modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2));

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), loopControl);
		path2.addFromControlPoint(entryControlPoint);	
		path2.setGuard(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.GE, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 1), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
								)
						)
		);
		path2.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 1));
		path2.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8), modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2));
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path3.addFromControlPoint(loopControl);	
		path3.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.GT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
										)
								)
				)
		);
		path3.add(new ProgramReturnVariable(), modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8));

		MethodPath path4 = new MethodPath(new GuardTrueExpression(), loopControl);
		path4.addFromControlPoint(loopControl);	
		path4.setGuard(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.GT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
								)
						)
		);
		path4.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);
		path4.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);

		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void loopTest2() {
		IR code = findWalaMethod("Ltestloops/test2/Entry","foo", "(II)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControlPoint);
		path1.addFromControlPoint(entryControlPoint);	
		path1.setGuard(new GuardTrueExpression());
		path1.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
					modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);
		path1.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 1)
		);

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), loopControlPoint);
		path2.addFromControlPoint(loopControlPoint);	
		path2.setGuard(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.GT, 
										new ProgramBinaryExpression(
												IBinaryOpInstruction.Operator.ADD, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
										), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
								)
						)
		);
		path2.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9),
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);
		path2.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8),
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path3.addFromControlPoint(loopControlPoint);	
		path3.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.GT, 
												new ProgramBinaryExpression(
														IBinaryOpInstruction.Operator.ADD, 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 8), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
												),
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
										)
								)
						)
		);
		path3.add(new ProgramReturnVariable(), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);

		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void loopTest3() {
		IR code = findWalaMethod("Ltestloops/test3/Entry","foo", "(II)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControl = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControl);
		path1.addFromControlPoint(entryControlPoint);	
		path1.setGuard(
				new GuardTrueExpression()
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 1)
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), loopControl);
		path2.addFromControlPoint(loopControl);	
		path2.setGuard(
				new GuardAndExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.GT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
										)
								)
						,
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.GE, 
										new ProgramBinaryExpression(
											IBinaryOpInstruction.Operator.ADD,
											modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
											modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
										),
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
								)
						)								
				)
		);
		/*path2.add(modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9)
		);*/
		path2.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
				new ProgramBinaryExpression(
					IBinaryOpInstruction.Operator.ADD,	
					modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10),
					modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), loopControl);
		path3.addFromControlPoint(loopControl);	
		path3.setGuard(
				new GuardAndExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.GT, 
												new ProgramBinaryExpression(
														IBinaryOpInstruction.Operator.ADD, 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
												),
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
										)
								)
						,
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.GE, 
												new ProgramBinaryExpression(
														IBinaryOpInstruction.Operator.ADD,
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
												),
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
										)
								)
						)								
				)
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD,	
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9),
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
					)
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
				new ProgramBinaryExpression(
					IBinaryOpInstruction.Operator.ADD,	
					modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10),
					modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);

		MethodPath path4 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path4.addFromControlPoint(loopControl);	
		path4.setGuard(
			new GuardOrExpression(
				new GuardAndExpression(
								new GuardNotExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														IConditionalBranchInstruction.Operator.GT, 
														new ProgramBinaryExpression(
																IBinaryOpInstruction.Operator.ADD, 
																modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
																modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
														),
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
												)
										)
						),
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.GE, 
												new ProgramBinaryExpression(
														IBinaryOpInstruction.Operator.ADD,
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
												),
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
										)
								)
						)
				),
				new GuardAndExpression(
								new GuardNotExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														IConditionalBranchInstruction.Operator.GT, 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
												)
										)
						),
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.GE, 
											new ProgramBinaryExpression(
													IBinaryOpInstruction.Operator.ADD,
													modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
													modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
											),
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
								)
						)						
				)
			)
		);
		path4.add(new ProgramReturnVariable(), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);

		/*MethodPath path5 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path5.addFromControlPoint(loopControl);	
		path5.setGuard(
				new GuardAndExpression(
						new GuardAndExpression(
								new GuardTrueExpression(),
								new GuardNotExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														IConditionalBranchInstruction.Operator.GT, 
														modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 9), 
														modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
												)
										)
								)
						),
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.GE, 
											new ProgramBinaryExpression(
													IBinaryOpInstruction.Operator.ADD,
													modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
													modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
											),
										modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
								)
						)						
				)		
		);
		path5.add(new ProgramReturnVariable(), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
						modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(-1))
				)
		);*/

		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void loopReturnInNestedLoop1() {
		IR code = findWalaMethod("Ltestloops/test4/Entry","testLoopReturnInNestedLoop1", "(I)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlI = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlJ = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path1.addFromControlPoint(entryControlPoint);
		//i = 0
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);
		//IntermediateA = a
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 14),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.addFromControlPoint(loopControlI);	
		path2.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
										)
								)
						)
		);
		path2.add(new ProgramReturnVariable(), 
				  modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 14)
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), loopControlJ);
		path3.addFromControlPoint(loopControlI);	
		path3.setGuard(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
								)
						)
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				  modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 14)
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);
		//path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16), 
		//		modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15)
		//);

		MethodPath path4 = new MethodPath(new GuardTrueExpression(), loopControlJ);
		path4.addFromControlPoint(loopControlJ);	
		path4.setGuard(
				new GuardAndExpression(
						new GuardNotExpression(new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.EQ, 
												new ProgramBinaryExpression(
														IBinaryOpInstruction.Operator.ADD, 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
												),
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(10))
										)
								)
						),
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
								)
						)
				)
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		/*path4.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16) 
		);*/

		MethodPath path5 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path5.addFromControlPoint(loopControlJ);	
		path5.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
										)
								)
						)
		);
		path5.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 14), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
		);
		path5.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);

		MethodPath path6 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path6.addFromControlPoint(loopControlJ);	
		path6.setGuard(
				new GuardAndExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														IConditionalBranchInstruction.Operator.EQ, 
														new ProgramBinaryExpression(
																IBinaryOpInstruction.Operator.ADD, 
																modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
																modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
														),
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(10))
												)
										)
						,
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
								)
						)
				)
		);
		path6.add(
				new ProgramReturnVariable(), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void loopLoopReturnBreakInNestedLoop() {
		IR code = findWalaMethod("Ltestloops/test5/Entry","testLoopReturnBreakInNestedLoop", "(I)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlI = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlJ = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path1.addFromControlPoint(entryControlPoint);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.addFromControlPoint(loopControlI);	
		path2.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
										)
								)
						)
		);
		path2.add(new ProgramReturnVariable(), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15)
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), loopControlJ);
		path3.addFromControlPoint(loopControlI);	
		path3.setGuard(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
								)
						)
		);
		//path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
		//		modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16)
		//);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15)
		);

		MethodPath path4 = new MethodPath(new GuardTrueExpression(), loopControlJ);
		path4.addFromControlPoint(loopControlJ);	
		path4.setGuard(
				new GuardAndExpression(
								new GuardNotExpression(new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.EQ, 
												new ProgramBinaryExpression(
														IBinaryOpInstruction.Operator.ADD, 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
												),
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(10))
										)
								))
						,
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
								)
						)
				)
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		/*path4.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17) 
		);*/

		MethodPath path5 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path5.addFromControlPoint(loopControlJ);	
		path5.setGuard(
				new GuardAndExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LE, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(500))
										)
								)
						,
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
										)
								)
						)
				)
		);
		path5.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 15), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
		);
		path5.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);

		MethodPath path6 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path6.addFromControlPoint(loopControlJ);	
		path6.setGuard(
				new GuardAndExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														IConditionalBranchInstruction.Operator.EQ, 
														new ProgramBinaryExpression(
																IBinaryOpInstruction.Operator.ADD, 
																modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
																modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
														),
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(10))
												)
										)
						,
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
								)
						)
				)
		);
		path6.add(
				new ProgramReturnVariable(), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodPath path7 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path7.addFromControlPoint(loopControlJ);	
		path7.setGuard(
				new GuardAndExpression(
								new GuardNotExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														IConditionalBranchInstruction.Operator.LE, 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(500))
												)
										)
								)
						,
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
										)
								)
						)
				)
		);
		path7.add(
				new ProgramReturnVariable(), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
		);
		
		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void testLoopBreak() {
		IR code = findWalaMethod("Ltestloops/test7/Entry","testLoopBreak", "(I)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControl = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControl);
		path1.addFromControlPoint(entryControlPoint);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.addFromControlPoint(loopControl);	
		path2.setGuard(
				new GuardAndExpression(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ, 
										new ProgramBinaryExpression(
												IBinaryOpInstruction.Operator.ADD, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
										),
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(10))
								)
						)
						,
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
								)
						)
				)
		);
		path2.add(new ProgramReturnVariable(), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), loopControl);
		path3.addFromControlPoint(loopControl);	
		path3.setGuard(
				new GuardAndExpression(
						new GuardNotExpression(new GuardProgramExpression(
											new ProgramRelationalExpression(
													IConditionalBranchInstruction.Operator.EQ, 
													new ProgramBinaryExpression(
															IBinaryOpInstruction.Operator.ADD, 
															modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
															modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
													),
													modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(10))
											)
									)
						),
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
								)
						)
				)
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);

		MethodPath path4 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path4.addFromControlPoint(loopControl);	
		path4.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
										)
								)
						)
		);
		path4.add(
				new ProgramReturnVariable(), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10) 
		);
		
		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void testLoopReturn() {
		IR code = findWalaMethod("Ltestloops/test6/Entry","testLoopReturn", "(I)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControl = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControl);
		path1.addFromControlPoint(entryControlPoint);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11),
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.addFromControlPoint(loopControl);	
		path2.setGuard(
				new GuardAndExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														IConditionalBranchInstruction.Operator.EQ, 
														new ProgramBinaryExpression(
																IBinaryOpInstruction.Operator.ADD, 
																modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
																modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
														),
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(10))
												)
										)
						,
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
								)
						)
				)
		);
		path2.add(new ProgramReturnVariable(), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), loopControl);
		path3.addFromControlPoint(loopControl);	
		path3.setGuard(
				new GuardAndExpression(
						new GuardNotExpression(new GuardProgramExpression(
											new ProgramRelationalExpression(
													IConditionalBranchInstruction.Operator.EQ, 
													new ProgramBinaryExpression(
															IBinaryOpInstruction.Operator.ADD, 
															modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
															modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
													),
													modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(10))
											)
									)
						),
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT, 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
								)
						)
				)
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		path3.add(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);

		MethodPath path4 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path4.addFromControlPoint(loopControl);	
		path4.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
										)
								)
				)
		);
		path4.add(
				new ProgramReturnVariable(), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 10) 
		);
		
		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void testInfiniteLoop() {
		IR code = findWalaMethod("Ltestloops/test8/Entry","testInfinite", "(I)V");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControl = MethodControlPoint.createMethodControlPoint();

		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControl);
		path1.addFromControlPoint(entryControlPoint);	

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), loopControl);
		path2.addFromControlPoint(loopControl);	
		
		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}

	@Test
	public void testNestedLoopBreakSpecial() {
		IR code = findWalaMethod("Ltestloops/test9/Entry","testNestedLoopBreakSpecial1", "(II)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlI = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlJ = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();

		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path1.addFromControlPoint(entryControlPoint);	
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 3)
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);
		
		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.addFromControlPoint(loopControlI);
		path2.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT,
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
										)
								)
						)
		);
		path2.add(
				new ProgramReturnVariable(), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16)
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), loopControlJ);
		path3.addFromControlPoint(loopControlI);
		path3.setGuard(
						new GuardProgramExpression(
							new ProgramRelationalExpression(
								IConditionalBranchInstruction.Operator.LT,
								modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
								modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
							)
				)
		);
		/*path3.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17)
		);
		path3.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);*/
		path3.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16)
		);
		path3.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);
		
		MethodPath path4 = new MethodPath(new GuardTrueExpression(), loopControlJ);
		path4.addFromControlPoint(loopControlJ);
		path4.setGuard(
				new GuardAndExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LE, 
												new ProgramBinaryExpression(
														IBinaryOpInstruction.Operator.ADD, 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12)
												),
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
										)
								)
						, 
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT,
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12),
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
								)
						)
				)
		);
		/*path4.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18)
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);*/
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodPath path5 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path5.addFromControlPoint(loopControlJ);
		path5.setGuard(
				new GuardAndExpression(
								new GuardNotExpression(
									new GuardProgramExpression(
											new ProgramRelationalExpression(
													IConditionalBranchInstruction.Operator.LE, 
													new ProgramBinaryExpression(
															IBinaryOpInstruction.Operator.ADD, 
															modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
															modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12)
													),
													modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
											)
									)
								)
						, 
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT,
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12),
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
								)
						)
				)
		);
		path5.add(
				new ProgramReturnVariable(), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
		);
		
		MethodPath path6 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path6.addFromControlPoint(loopControlJ);
		path6.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT,
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
										)
								)
						)
		);
		/*path6.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);*/
		path6.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 16), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
				)
		);
		path6.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void testNestedLoopContinueSpecial() {
		IR code = findWalaMethod("Ltestloops/test10/Entry","testNestedLoopContinueSpecial1", "(II)I");
		IMethod theMethod = code.getMethod();
		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlI = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loopControlJ = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();

		MethodPath path1 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path1.addFromControlPoint(entryControlPoint);	
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 3)
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);
		path1.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);
		
		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.addFromControlPoint(loopControlI);
		path2.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT,
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
										)
								)
						)
		);
		path2.add(
				new ProgramReturnVariable(), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17)
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), loopControlJ);
		path3.addFromControlPoint(loopControlI);
		path3.setGuard(
						new GuardProgramExpression(
							new ProgramRelationalExpression(
								IConditionalBranchInstruction.Operator.LT,
								modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
								modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
							)
						)
		);
		/*path3.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 19), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18)
		);
		path3.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);*/
		path3.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17)
		);
		path3.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(0))
		);
		
		MethodPath path4 = new MethodPath(new GuardTrueExpression(), loopControlJ);
		path4.addFromControlPoint(loopControlJ);
		path4.setGuard(
				new GuardAndExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LE, 
												new ProgramBinaryExpression(
														IBinaryOpInstruction.Operator.ADD, 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
														modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12)
												),
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
										)
								)
						, 
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT,
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12),
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
								)
						)
				)
		);
		/*path4.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 19), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 19)
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);*/
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodPath path5 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path5.addFromControlPoint(loopControlJ);
		path5.setGuard(
				new GuardAndExpression(
								new GuardNotExpression(
									new GuardProgramExpression(
											new ProgramRelationalExpression(
													IConditionalBranchInstruction.Operator.LE, 
													new ProgramBinaryExpression(
															IBinaryOpInstruction.Operator.ADD, 
															modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
															modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12)
													),
													modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(100))
											)
									)
								)
						, 
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.LT,
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12),
										modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
								)
						)
				)
		);
		/*path5.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);*/
		path5.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11) 
		);
		path5.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodPath path6 = new MethodPath(new GuardTrueExpression(), loopControlI);
		path6.addFromControlPoint(loopControlJ);
		path6.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT,
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 12), 
												modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11)
										)
								)
						)
		);
		/*path6.add(
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2), 
				modelFactory.createProgramIndexExpression(null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 2)
		);*/
		path6.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 17), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 11), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1000))
				)
		);
		path6.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
				new ProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 18), 
						modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant(1))
				)
		);
		
		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	
	@Test
	public void testCallerLocalsBackwardPropagationInLoopFromCalledMethod() {
		IR code = findWalaMethod("Ltestloops/test11/Entry","_jspService", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V");
		IMethod theMethod = code.getMethod();
		IR calledCode = findWalaMethod("Ltestloops/test11/Entry","test", "(Ljavax/servlet/http/HttpServletRequest;)V");

		
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint loop = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();

		MethodPath path1 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path1.setGuard(
				new GuardAndExpression(
						new GuardProgramExpression(
								modelFactory.createProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ, 
										modelFactory.createProgramStringComparison(
												modelFactory.createProgramRequestParameter(modelFactory.createProgramIndexExpression("action")), 
												modelFactory.createProgramIndexExpression("test2")
												),
										modelFactory.createProgramIndexExpression(0))
						), 
						new GuardProgramExpression(
								modelFactory.createProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ, 
										modelFactory.createProgramStringComparison(
												modelFactory.createProgramRequestParameter(modelFactory.createProgramIndexExpression("action")), 
												modelFactory.createProgramIndexExpression("test1")
										),
										modelFactory.createProgramIndexExpression(0))
						)
				)
		);
		path1.addFromControlPoint(entryControlPoint);	
		
		
		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.setGuard(
				new GuardNotExpression(
						new GuardProgramExpression(
								modelFactory.createProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ, 
										modelFactory.createProgramStringComparison(
												modelFactory.createProgramRequestParameter(modelFactory.createProgramIndexExpression("action")), 
												modelFactory.createProgramIndexExpression("test2")
												),
										modelFactory.createProgramIndexExpression(0))
						)
				)
		);
		path2.addFromControlPoint(entryControlPoint);
		path2.add(
				modelFactory.createProgramSessionAttribute(modelFactory.createProgramIndexExpression("attr2")),
				modelFactory.createProgramIndexExpression("da")
		);
		
		MethodPath path3 = new MethodPath(new GuardTrueExpression(), loop);
		path3.setGuard(
				new GuardNotExpression(
						new GuardProgramExpression(
								modelFactory.createProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ, 
										modelFactory.createProgramStringComparison(
												modelFactory.createProgramRequestParameter(modelFactory.createProgramIndexExpression("action")), 
												modelFactory.createProgramIndexExpression("test1")
												),
										modelFactory.createProgramIndexExpression(0))
						)
				)
		);
		path3.addFromControlPoint(entryControlPoint);
		path3.add(
				modelFactory.createProgramIndexExpression(null,null,calledCode.getSymbolTable(), calledCode.getControlFlowGraph(), 0, 12),
				modelFactory.createProgramIndexExpression(0)
		);	
		/*path3.add(
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, 7),
				modelFactory.createProgramRequestParameter(modelFactory.createProgramIndexExpression("action"))
		);*/

		MethodPath path4 = new MethodPath(new GuardTrueExpression(), loop);
		path4.setGuard(
				new GuardProgramExpression(
						modelFactory.createProgramRelationalExpression(
								IConditionalBranchInstruction.Operator.LT, 
								modelFactory.createProgramIndexExpression(null,null,calledCode.getSymbolTable(), calledCode.getControlFlowGraph(), 0, 12),
								modelFactory.createProgramIndexExpression(1)
						)
				)
		);
		path4.addFromControlPoint(loop);
		path4.add(
				modelFactory.createProgramSessionAttribute(modelFactory.createProgramIndexExpression("attr1")),
				modelFactory.createProgramIndexExpression("da")
		);
		path4.add(
				modelFactory.createProgramIndexExpression(null,null,calledCode.getSymbolTable(), calledCode.getControlFlowGraph(), 0, 12),
				modelFactory.createProgramBinaryExpression(
						IBinaryOpInstruction.Operator.ADD, 
						modelFactory.createProgramIndexExpression(null,null,calledCode.getSymbolTable(), calledCode.getControlFlowGraph(), 0, 12), 
						modelFactory.createProgramIndexExpression(1)						
				)
		);

		MethodPath path5 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path5.setGuard(
				new GuardAndExpression(
						new GuardNotExpression(
								new GuardProgramExpression(
										modelFactory.createProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,calledCode.getSymbolTable(), calledCode.getControlFlowGraph(), 0, 12),
												modelFactory.createProgramIndexExpression(1)
										)
								)
						),
						new GuardProgramExpression(
								modelFactory.createProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ, 
										modelFactory.createProgramStringComparison(
												modelFactory.createProgramRequestParameter(modelFactory.createProgramIndexExpression("action")), 
												modelFactory.createProgramIndexExpression("test2")
												),
										modelFactory.createProgramIndexExpression(0))
						)
				)
		);
		path5.addFromControlPoint(loop);
		
		MethodPath path6 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path6.setGuard(
				new GuardAndExpression(
						new GuardNotExpression(
								new GuardProgramExpression(
										modelFactory.createProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LT, 
												modelFactory.createProgramIndexExpression(null,null,calledCode.getSymbolTable(), calledCode.getControlFlowGraph(), 0, 12),
												modelFactory.createProgramIndexExpression(1)
										)
								)
						),
						new GuardNotExpression(
								new GuardProgramExpression(
								modelFactory.createProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ, 
										modelFactory.createProgramStringComparison(
												modelFactory.createProgramRequestParameter(modelFactory.createProgramIndexExpression("action")), 
												modelFactory.createProgramIndexExpression("test2")
												),
										modelFactory.createProgramIndexExpression(0))
								)
						)
				)
		);
		path6.addFromControlPoint(loop);
		path6.add(
				modelFactory.createProgramSessionAttribute(modelFactory.createProgramIndexExpression("attr2")),
				modelFactory.createProgramIndexExpression("da")
		);
	
		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		System.out.println(actual.getAllRelevantVariables());
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
}
