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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import junit.framework.Assert;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier;
import ro.ieat.isummarize.jsptomcat.ProgramOutput;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute;
import ro.ieat.tests.utils.TestUtil;

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

public class SubsumeTests {

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
	public void testSubsumation1() {
		IR code = findWalaMethod("Ltestsubsume/test1/Entry","testsubsume1", "(Ljavax/servlet/http/HttpServletRequest;)V");
		MethodReference libraryMethodRef = MethodReference.findOrCreate(TypeReference.JavaLangSystem, Selector.make("currentTimeMillis()J"));
		IMethod theLibraryMethod = code.getMethod().getClassHierarchy().resolveMethod(libraryMethodRef);
		IMethod theMethod = code.getMethod();
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path1.addFromControlPoint(entryControlPoint);		
		path1.setGuard(
						new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LE,
												new ProgramFunction(theLibraryMethod, new ProgramExpression[]{}),
												modelFactory.createProgramIndexExpression(0l)
										)
								)
		);
		path1.add(new ProgramSessionAttribute(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("cucu"))), 
				  modelFactory.createProgramIndexExpression("mumu2")
		);

		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.addFromControlPoint(entryControlPoint);		
		path2.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LE,
												new ProgramFunction(theLibraryMethod, new ProgramExpression[]{}),
												modelFactory.createProgramIndexExpression(0l)
										)
									)
								)
		);
		path2.add(new ProgramSessionAttribute(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("cucu"))), 
				  modelFactory.createProgramIndexExpression("mumu1")
		);


		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void testSubsumation2() {
		IR code = findWalaMethod("Ltestsubsume/test2/Entry","testsubsume2", "(Ljavax/servlet/http/HttpServletRequest;)V");
		IMethod theMethod = code.getMethod();
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path1.addFromControlPoint(entryControlPoint);		
		path1.add(new ProgramSessionAttribute(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("cucu"))), 
			modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("mumu"))
		);

		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void testSubsumation3() {
		IR code = findWalaMethod("Ltestsubsume/test3/Entry","testsubsume3", "(Ljavax/servlet/http/HttpServletRequest;)Ljava/lang/String;");
		MethodReference libraryMethodRef = MethodReference.findOrCreate(TypeReference.JavaLangSystem, Selector.make("currentTimeMillis()J"));
		IMethod theLibraryMethod = code.getMethod().getClassHierarchy().resolveMethod(libraryMethodRef);
		IMethod theMethod = code.getMethod();
		//Get actual value
		MethodSummary actual = results.get(theMethod);
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodPath path1 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path1.addFromControlPoint(entryControlPoint);		
		path1.setGuard(
						new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LE,
												new ProgramFunction(theLibraryMethod, new ProgramExpression[]{}),
												modelFactory.createProgramIndexExpression(0l)
										)
								)
		);
		path1.add(new ProgramReturnVariable(), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("2"))
		);
		path1.add(new ProgramSessionAttribute(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("cucu"))), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("mumu"))
		);


		MethodPath path2 = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path2.addFromControlPoint(entryControlPoint);		
		path2.setGuard(
						new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.LE,
												new ProgramFunction(theLibraryMethod, new ProgramExpression[]{}),
												modelFactory.createProgramIndexExpression(0l)
										)
									)
								)
		);
		path2.add(new ProgramReturnVariable(), 
			modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("1"))
		);
		path2.add(new ProgramSessionAttribute(modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("cucu"))), 
				modelFactory.createProgramIndexExpression(null,null,code.getSymbolTable(), code.getControlFlowGraph(), 0, code.getSymbolTable().getConstant("mumu"))
		);

		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
	
	@Test
	public void testSubsumation4() {
		IR code = findWalaMethod("Ltestsubsume/test4/EntrySubsume4","_jspService", "(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)V");
		
		//Get actual value
		IMethod theMethod = code.getMethod();
		MethodSummary actual = results.get(theMethod);
		
		//Build expected value 
		MethodControlPoint entryControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodControlPoint exitControlPoint = MethodControlPoint.createMethodControlPoint();
		MethodPath path = new MethodPath(new GuardTrueExpression(), exitControlPoint);
		path.addFromControlPoint(entryControlPoint);	
		List<ProgramExpression> out = new ArrayList<ProgramExpression>();
		out.add(modelFactory.createProgramIndexExpression("Out"));
		path.add(new ProgramOutput(), new ProgramOutput(out)); 

		MethodSummary expected = new MethodSummary(theMethod,entryControlPoint); 
		System.out.println(expected);
		System.out.println(actual);
		Assert.assertTrue("Expected:" + expected + " Actual:" + actual, expected.structuralEquals(actual));
	}
}
