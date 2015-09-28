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

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;

import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter;
import ro.ieat.isummarize.utils.GuardSimplifier;

public class SimplificationTests {

	@Test
	public void testSimplification1() {
		GuardExpression theGuard = new GuardAndExpression(
			new GuardProgramExpression(
				new ProgramRelationalExpression(
						IConditionalBranchInstruction.Operator.EQ,
						new ProgramStringComparison(
								new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Login\"")),
								new ProgramIndexExpressionConstant("\"\"")),
						new ProgramIndexExpressionConstant(new Integer(0))
				)
			),
			new GuardOrExpression(
					new GuardNotExpression(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ,
										new ProgramStringComparison(
												new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Login\"")),
												new ProgramIndexExpressionConstant("\"\"")),
										new ProgramIndexExpressionConstant(new Integer(0))
								)							
						)
					),
					new GuardProgramExpression(
							new ProgramRelationalExpression(
									IConditionalBranchInstruction.Operator.EQ,
									new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Login\"")),
									new ProgramIndexExpressionConstant()
							)
					)			
			)
		);
		System.out.println(theGuard);
		System.out.println(GuardSimplifier.simplify(theGuard));
	}
	
	@Test
	public void testSimplification2() {
		GuardExpression theGuard = new GuardOrExpression(
			new GuardProgramExpression(
				new ProgramRelationalExpression(
						IConditionalBranchInstruction.Operator.EQ,
						new ProgramStringComparison(
								new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
								new ProgramIndexExpressionConstant("\"\"")),
						new ProgramIndexExpressionConstant(new Integer(0))
				)
			),
			new GuardAndExpression(
					new GuardNotExpression(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ,
										new ProgramStringComparison(
												new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Login\"")),
												new ProgramIndexExpressionConstant("\"\"")),
										new ProgramIndexExpressionConstant(new Integer(0))
								)							
						)
					),
					new GuardProgramExpression(
							new ProgramRelationalExpression(
									IConditionalBranchInstruction.Operator.EQ,
									new ProgramStringComparison(
											new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Login\"")),
											new ProgramIndexExpressionConstant("\"\"")),
									new ProgramIndexExpressionConstant(new Integer(0))
							)							
					)
			)
		);
		System.out.println(theGuard);
		System.out.println(GuardSimplifier.simplify(theGuard));
	}

	@Test
	public void testSimplification3() {
		GuardExpression theGuard = new GuardOrExpression(
			new GuardProgramExpression(
				new ProgramRelationalExpression(
						IConditionalBranchInstruction.Operator.EQ,
						new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
						new ProgramIndexExpressionConstant()
				)
			),
			new GuardOrExpression(
					new GuardNotExpression(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ,
										new ProgramStringComparison(
												new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
												new ProgramIndexExpressionConstant("\"\"")),
										new ProgramIndexExpressionConstant(new Integer(0))
								)							
						)
					),
					new GuardAndExpression(
							new GuardProgramExpression(
									new ProgramRelationalExpression(
											IConditionalBranchInstruction.Operator.EQ,
											new ProgramStringComparison(
													new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
													new ProgramIndexExpressionConstant("\"\"")),
											new ProgramIndexExpressionConstant(new Integer(0))
									)							
							),
							new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.EQ,
												new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
												new ProgramIndexExpressionConstant()
										)
								)
							)
					)
			)
		);				
		GuardExpression simplif = GuardSimplifier.simplify(theGuard);
		System.out.println(theGuard);
		System.out.println(GuardSimplifier.simplify(simplif));
		Assert.assertTrue(new GuardTrueExpression().equals(simplif));
	}

	@Test
	public void testSimplification4() {
		GuardExpression theGuard = new GuardOrExpression(
				new GuardNotExpression(
						new GuardProgramExpression(
								new ProgramRelationalExpression(
										IConditionalBranchInstruction.Operator.EQ,
										new ProgramStringComparison(
												new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
												new ProgramIndexExpressionConstant("\"\"")),
										new ProgramIndexExpressionConstant(new Integer(0))
								)							
						)
			),
			new GuardOrExpression(
					new GuardProgramExpression(
							new ProgramRelationalExpression(
									IConditionalBranchInstruction.Operator.EQ,
									new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
									new ProgramIndexExpressionConstant()
							)
					),
					new GuardAndExpression(
							new GuardProgramExpression(
									new ProgramRelationalExpression(
											IConditionalBranchInstruction.Operator.EQ,
											new ProgramStringComparison(
													new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
													new ProgramIndexExpressionConstant("\"\"")),
											new ProgramIndexExpressionConstant(new Integer(0))
									)							
							),
							new GuardNotExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.EQ,
												new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
												new ProgramIndexExpressionConstant()
										)
								)
							)
					)
			)
		);	
		GuardExpression simplif = GuardSimplifier.simplify(theGuard);
		System.out.println(theGuard);
		System.out.println(simplif);
		Assert.assertTrue(new GuardTrueExpression().equals(simplif));
	}
	
	@Test
	public void testSimplification5() {
		GuardExpression theGuard = 
				new GuardOrExpression(
						new GuardAndExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.EQ,
												new ProgramStringComparison(
														new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
														new ProgramIndexExpressionConstant("\"\"")),
												new ProgramIndexExpressionConstant(new Integer(0))
										)							
								),
								new GuardNotExpression(
									new GuardProgramExpression(
											new ProgramRelationalExpression(
													IConditionalBranchInstruction.Operator.EQ,
													new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
													new ProgramIndexExpressionConstant()
											)
									)
								)
						),
						new GuardOrExpression(
								new GuardProgramExpression(
										new ProgramRelationalExpression(
												IConditionalBranchInstruction.Operator.EQ,
												new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
												new ProgramIndexExpressionConstant()
										)
								),
								new GuardNotExpression(
										new GuardProgramExpression(
												new ProgramRelationalExpression(
														IConditionalBranchInstruction.Operator.EQ,
														new ProgramStringComparison(
																new ProgramRequestParameter(new ProgramIndexExpressionConstant("\"Pass\"")),
																new ProgramIndexExpressionConstant("\"\"")),
														new ProgramIndexExpressionConstant(new Integer(0))
												)							
										)
								)
						)	
		);
		GuardExpression simplif = GuardSimplifier.simplify(theGuard);
		System.out.println(theGuard);
		System.out.println(GuardSimplifier.simplify(simplif));
		Assert.assertTrue(new GuardTrueExpression().equals(simplif));
	}
}
