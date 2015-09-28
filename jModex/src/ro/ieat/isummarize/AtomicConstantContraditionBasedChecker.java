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

public class AtomicConstantContraditionBasedChecker implements SatisfiabilityChecker {

	
	@Override
	public boolean mayBeSatisfiable(GuardExpression expression) {
		if(expression instanceof GuardNotExpression) {
			if(((GuardNotExpression) expression).getOperand() instanceof GuardTrueExpression) {
				return false;
			}
		}
		return true;
	} 

	/*private SimpleCheckerVisitor checker = new SimpleCheckerVisitor();

	private static class SimpleCheckerVisitor extends ExpressionAcyclicVisitor {
		
		private enum Result {FALSE, TRUE, DONOTKNOW};
		
		private Stack<Set<Constraint>> stack = new Stack<Set<Constraint>>();
		
		private static class Constraint {
			Constraint(ProgramExpression exp, boolean shouldBeEqual, String value) {
				this.exp = exp;
				this.shouldBeEqual = shouldBeEqual;
				this.value = value;
			}
			ProgramExpression exp;
			boolean shouldBeEqual;
			String value;
			public boolean isInContradictionWith(Constraint c) {
				if(exp.structuralEquals(c.exp)) {
					if(shouldBeEqual == c.shouldBeEqual && shouldBeEqual == true) {
						if(value == null && c.value != null) {
							return true;
						}
						if(c.value == null && value != null) {
							return true;
						}
						if(c.value == null && value == null) {
							return false;
						}
						return !c.value.equals(value);
					}
					if(shouldBeEqual && !c.shouldBeEqual) {
						if(value == null) {
							if(c.value == null) {
								return true;
							}
						} else {
							if(c.value != null) {
								return value.equals(c.value);
							}
						}
					}
					if(!shouldBeEqual && c.shouldBeEqual) {
						if(c.value == null) {
							if(value == null) {
								return true;
							}
						} else {
							if(value != null) {
								return value.equals(c.value);								
							}
						}
					}
				}
				return false;
			}
			
			public boolean isTheSame(Constraint c) {
				if(exp.structuralEquals(c.exp) && shouldBeEqual == c.shouldBeEqual && (value == null ? c.value == null : (c.value != null && value.equals(c.value)))) {
					return true;
				}
				return false;
			}
			
		}
		
		public boolean check(GuardExpression expression) {
			stack.clear();
			boolean value = expression.accept(this) == Result.FALSE ? false : true;
			return value;
		}
		
		public Object visitGuardTrueExpression(GuardTrueExpression expression) {
			stack.push(new HashSet<Constraint>());
			return Result.TRUE;
		}
		
		public Object visitGuardNotExpression(GuardNotExpression expression) {
			Result tmp = (Result) expression.getOperand().accept(this);
			Set<Constraint> constraints = stack.pop();
			for(Constraint aConstraint : constraints) {
				aConstraint.shouldBeEqual = !aConstraint.shouldBeEqual;
			}
			stack.push(constraints);
			return tmp == Result.TRUE ? Result.FALSE : (tmp == Result.FALSE ? Result.TRUE : Result.DONOTKNOW);  
		}

		public Object visitGuardOrExpression(GuardOrExpression expression) {
			Result tmp1 = (Result)expression.getLeft().accept(this);
			Set<Constraint> constraintsLeft =  stack.pop();
			Result tmp2 = (Result)expression.getRight().accept(this);
			Set<Constraint> constraintsRight =  stack.pop();
			if(tmp1 == Result.TRUE || tmp2 == Result.TRUE) {
				stack.push(new HashSet<Constraint>());
				return Result.TRUE;				
			}
			if(tmp1 == Result.FALSE && tmp2 == Result.FALSE) {
				stack.push(new HashSet<Constraint>());
				return Result.FALSE;				
			}
			if(tmp1 == Result.FALSE) {
				stack.push(constraintsRight);
				return tmp2;
			}
			if(tmp2 == Result.FALSE) {
				stack.push(constraintsLeft);
				return tmp1;				
			}
			Set<Constraint> constraints =  new HashSet<Constraint>();
			for(Constraint constraint1 : constraintsLeft) {
				for(Constraint constraint2 : constraintsRight) {
					if(constraint1.isTheSame(constraint2)) {
						constraints.add(constraint1);
					}
				}
			}
			stack.push(constraints);
			return Result.DONOTKNOW;
		}

		public Object visitGuardAndExpression(GuardAndExpression expression) {
			Result tmp1 = (Result)expression.getLeft().accept(this);
			Set<Constraint> constraintsLeft =  stack.pop();
			if(tmp1 == Result.FALSE) {
				stack.push(new HashSet<Constraint>());
				return Result.FALSE;
			}
			Result tmp2 = (Result)expression.getRight().accept(this);
			Set<Constraint> constraintsRight =  stack.pop();
			if(tmp2 == Result.FALSE) {
				stack.push(new HashSet<Constraint>());
				return Result.FALSE;
			}
			if(tmp1 == Result.TRUE && tmp2 == Result.TRUE) {
				stack.push(new HashSet<Constraint>());
				return Result.TRUE;
			}
			for(Constraint constraint1 : constraintsLeft) {
				for(Constraint constraint2 : constraintsRight) {
					if(constraint1.isInContradictionWith(constraint2)) {
						stack.push(new HashSet<Constraint>());
						return Result.FALSE;
					}
				}
			}
			constraintsLeft.addAll(constraintsRight);
			stack.push(constraintsLeft);
			return Result.DONOTKNOW;
		}

		public Object visitGuardProgramExpression(GuardProgramExpression expression) {
			return expression.getProgramCondition().accept(this);
		}

		public Object visitProgramRelationalExpression(ProgramRelationalExpression expression) {
			ProgramExpression l = expression.getLeft();
			ProgramExpression r = expression.getRight();
			IConditionalBranchInstruction.IOperator op = expression.getOperator();
			Set<Constraint> constraints =  new HashSet<Constraint>();
			if(l instanceof ProgramIndexExpression && r instanceof ProgramIndexExpression) {
				ProgramIndexExpression il = (ProgramIndexExpression) l;
				ProgramIndexExpression ir = (ProgramIndexExpression) r;
				if(op == IConditionalBranchInstruction.Operator.EQ) {
					if((il.isStringConstant() && ir.isNullConstant()) || (ir.isStringConstant() && il.isNullConstant())) {
						stack.push(constraints);
						return Result.FALSE;
					}
					if(il.isNumberConstant() && ir.isNumberConstant()) {
						if(il.getNumberConstant().doubleValue() == ir.getNumberConstant().doubleValue()) {
							stack.push(constraints);
							return Result.TRUE;
						} else {
							stack.push(constraints);
							return Result.FALSE;							
						}
					}
				} else if(op == IConditionalBranchInstruction.Operator.NE) {
					if((il.isStringConstant() && ir.isNullConstant()) || (ir.isStringConstant() && il.isNullConstant())) {
						stack.push(constraints);
						return Result.TRUE;
					}
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() == ir.getNumberConstant().doubleValue()) {
							stack.push(constraints);
							return Result.FALSE;
						} else {
							stack.push(constraints);
							return Result.TRUE;
						}
					}
				} else if(op == IConditionalBranchInstruction.Operator.LT) {
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() < ir.getNumberConstant().doubleValue()) {
							stack.push(constraints);
							return Result.TRUE;
						} else {
							stack.push(constraints);
							return Result.FALSE;
						}
					}					
				} else if(op == IConditionalBranchInstruction.Operator.GE) {
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() >= ir.getNumberConstant().doubleValue()) {
							stack.push(constraints);
							return Result.TRUE;
						} else {
							stack.push(constraints);
							return Result.FALSE;
						}
					}					
				} else if(op == IConditionalBranchInstruction.Operator.GT) {
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() > ir.getNumberConstant().doubleValue()) {
							stack.push(constraints);
							return Result.TRUE;
						} else {
							stack.push(constraints);
							return Result.FALSE;
						}
					}					
				} else if(op == IConditionalBranchInstruction.Operator.LE) {
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() <= ir.getNumberConstant().doubleValue()) {
							stack.push(constraints);
							return Result.TRUE;
						} else {
							stack.push(constraints);
							return Result.FALSE;
						}
					}					
				}
			} else if(l instanceof ProgramIndexExpression && r == null) {
				ProgramIndexExpression il = (ProgramIndexExpression) l;
				if(op == IConditionalBranchInstruction.Operator.EQ) {
					if(il.isNumberConstant()) {
						if(il.getNumberConstant().doubleValue() == expression.getConstant().doubleValue()) {
							stack.push(constraints);
							return Result.TRUE;
						} else {
							stack.push(constraints);
							return Result.FALSE;							
						}
					}
				} else if(op == IConditionalBranchInstruction.Operator.NE) {
					if(il.isNumberConstant()) {
						if(il.getNumberConstant().doubleValue() == expression.getConstant().doubleValue()) {
							stack.push(constraints);
							return Result.FALSE;
						} else {
							stack.push(constraints);
							return Result.TRUE;							
						}
					}
				} 
			} else if(!(l instanceof ProgramFunction) && r instanceof ProgramIndexExpression && ((ProgramIndexExpression)r).isNullConstant()) {
				if(op == IConditionalBranchInstruction.Operator.EQ) {
					constraints.add(new Constraint(l, true, null));					
				}
				if(op == IConditionalBranchInstruction.Operator.NE) {
					constraints.add(new Constraint(l, false, null));					
				}				
			} else if(!(r instanceof ProgramFunction) && l instanceof ProgramIndexExpression && ((ProgramIndexExpression)l).isNullConstant()) {
				if(op == IConditionalBranchInstruction.Operator.EQ) {
					constraints.add(new Constraint(r, true, null));					
				}
				if(op == IConditionalBranchInstruction.Operator.NE) {
					constraints.add(new Constraint(r, false, null));					
				}				
			} else if((l instanceof ProgramStringComparison) && (r instanceof ProgramIndexExpression)) {
				Result tmp = (Result) l.accept(this);
				Set<Constraint> expConstr = stack.pop();
				if(tmp != Result.DONOTKNOW && ((ProgramIndexExpression)r).isNumberConstant()) {
					if(op == IConditionalBranchInstruction.Operator.EQ) {
						if(tmp == Result.TRUE) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								stack.push(constraints);
								return Result.TRUE;
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								stack.push(constraints);
								return Result.FALSE;
							}
						}
						if(tmp == Result.FALSE) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								stack.push(constraints);
								return Result.FALSE;
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								stack.push(constraints);
								return Result.TRUE;
							}
						}
					} else if(op == IConditionalBranchInstruction.Operator.NE) {
						if(tmp == Result.TRUE) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								stack.push(constraints);
								return Result.FALSE;
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								stack.push(constraints);
								return Result.TRUE;
							}
						}
						if(tmp == Result.FALSE) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								stack.push(constraints);
								return Result.TRUE;
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								stack.push(constraints);
								return Result.FALSE;
							}
						}						
					}
				}
				if(tmp == Result.DONOTKNOW && ((ProgramIndexExpression)r).isNumberConstant()) {
					if(op == IConditionalBranchInstruction.Operator.EQ) {
						if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
							for(Constraint aConstraint : expConstr) {
								constraints.add(aConstraint);
							}
						}
						if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
							for(Constraint aConstraint : expConstr) {
								aConstraint.shouldBeEqual = !aConstraint.shouldBeEqual;
								constraints.add(aConstraint);
							}
						}
					} else if(op == IConditionalBranchInstruction.Operator.NE) {
						if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
							for(Constraint aConstraint : expConstr) {
								aConstraint.shouldBeEqual = !aConstraint.shouldBeEqual;
								constraints.add(aConstraint);
							}
						}
						if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
							for(Constraint aConstraint : expConstr) {
								constraints.add(aConstraint);
							}
						}
					}				
				}
			} else if(l instanceof ProgramFunction) {
				ProgramFunction left = (ProgramFunction)l;
				TernaryResult res = left.tryEvaluateComparison(op, r);
				if(res == TernaryResult.TRUE) {
					stack.push(constraints);
					return Result.TRUE;
				}
				if(res == TernaryResult.FALSE) {
					stack.push(constraints);
					return Result.FALSE;					
				}
			} else if(r instanceof ProgramFunction) {
				ProgramFunction right = (ProgramFunction)r;
				TernaryResult res = right.tryEvaluateComparison(op, l);
				if(res == TernaryResult.TRUE) {
					stack.push(constraints);
					return Result.TRUE;
				}
				if(res == TernaryResult.FALSE) {
					stack.push(constraints);
					return Result.FALSE;					
				}
			}
			stack.push(constraints);
			return Result.DONOTKNOW;
		}

		public Object visitProgramStringComparison(ProgramStringComparison expression) {
			ProgramExpression l = expression.getLeft();
			ProgramExpression r = expression.getRight();
			Set<Constraint> constraints =  new HashSet<Constraint>();
			if(l instanceof ProgramIndexExpression && r instanceof ProgramIndexExpression) {
				ProgramIndexExpression il = (ProgramIndexExpression) l;
				ProgramIndexExpression ir = (ProgramIndexExpression) r;
				if(il.isStringConstant() && ir.isStringConstant()) {
					if(il.getStringConstant().equals(ir.getStringConstant())) {
						stack.push(constraints);
						return Result.TRUE;
					} else {
						stack.push(constraints);
						return Result.FALSE;
					}
				}
			}
			if(!(l instanceof ProgramFunction) && r instanceof ProgramIndexExpression && ((ProgramIndexExpression)r).isStringConstant()) {
				constraints.add(new Constraint(l, true, ((ProgramIndexExpression)r).getStringConstant()));
			} else if(!(r instanceof ProgramFunction) && l instanceof ProgramIndexExpression && ((ProgramIndexExpression)l).isStringConstant()) {
				constraints.add(new Constraint(r, true, ((ProgramIndexExpression)l).getStringConstant()));
			}
			stack.push(constraints);
			return Result.DONOTKNOW;
		}

	}*/
	
}
