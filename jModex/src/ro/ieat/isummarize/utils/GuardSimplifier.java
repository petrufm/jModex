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
package ro.ieat.isummarize.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.GuardAndExpression;
import ro.ieat.isummarize.GuardExpression;
import ro.ieat.isummarize.GuardNotExpression;
import ro.ieat.isummarize.GuardOrExpression;
import ro.ieat.isummarize.GuardProgramExpression;
import ro.ieat.isummarize.GuardTrueExpression;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramFunction;
import ro.ieat.isummarize.ProgramIndexExpression;
import ro.ieat.isummarize.ProgramRelationalExpression;
import ro.ieat.isummarize.ProgramStringBinaryExpression;
import ro.ieat.isummarize.ProgramStringComparison;
import ro.ieat.isummarize.TernaryResult;
import ro.ieat.isummarize.javasql.ProgramSQLCursorAction;
/**
 * 
 * @author petrum
 * 
 * Other logical simplifications that could be added:
 * 
 * 1. add constraints for numerical values
 * 
 * 2. Simplifications like a = null || a = "" || (a != null && a != "")
 *
 */
public class GuardSimplifier {

	private static IdentityHashMap<GuardExpression, Set<Constraint>> cache = new IdentityHashMap<GuardExpression, Set<Constraint>>();
	private static void addToCache(GuardExpression node, Set<Constraint> constr) {
		Set<Constraint> clone = new HashSet<Constraint>();
		for(Constraint c : constr) {
			clone.add((Constraint) c.clone());
		}
		cache.put(node,Collections.unmodifiableSet(clone));
	}
	public static void reset() {
		cache.clear();
	}
	
	private static class Constraint implements Cloneable {
		public Object clone() {
			try {
				return super.clone();
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
				return null;
			}
		}
		public String toString() {
			return exp + (shouldBeEqual ? "=" : "!=") + value;
		}
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
	
		private static GuardExpression inferValueBasedOnAxioms(Constraint antecedent, Constraint consequent) {
			//x=a -> x!=null [or !(x=null)]
			if(antecedent.shouldBeEqual && antecedent.value != null && 
					antecedent.exp.structuralEquals(consequent.exp) && consequent.value == null) {
				return consequent.shouldBeEqual ? FALSE : TRUE;
			}
			//x=a -> x!=b [or !(x=b)]
			if(antecedent.shouldBeEqual && antecedent.value != null && 
					antecedent.exp.structuralEquals(consequent.exp) && consequent.value != null && !antecedent.value.equals(consequent.value)) {
				return consequent.shouldBeEqual ? FALSE : TRUE;
			}
			/*//x=/!=a -> x!=null [or !(x=b)]
			if(antecedent.value != null && !antecedent.shouldBeEqual && antecedent.exp.structuralEquals(consequent.exp)
					&& consequent.value == null) {
				return consequent.shouldBeEqual ? FALSE : TRUE;
			}*/
			return null;
		}

	}
		
	private static Simplifier simplify = new Simplifier();
	private static GuardExpression TRUE = new GuardTrueExpression();
	private static GuardExpression FALSE = new GuardNotExpression(new GuardTrueExpression());
	
	public static GuardExpression simplify(GuardExpression exp) {
		return simplify.process(exp);
	}

	private static class Simplifier extends ExpressionAcyclicVisitor {

		private Stack<Set<Constraint>> stack = new Stack<Set<Constraint>>();
		private int phase;

		private Set<Constraint> getNeyEmptyContraints() {
			return new HashSet<Constraint>();
		}
		
		public GuardExpression process(GuardExpression exp) {
			stack.clear();
			cache.clear();
			phase = 1;
			GuardExpression tmp = (GuardExpression) exp.accept(this);
			if(stack.size() != 1) {
				throw new RuntimeException("Error in simplifier. Stack should contain just one element!");
			}
			phase = 2;
			GuardExpression result = (GuardExpression) tmp.accept(this);
			cache.clear();
			return result;
		}
		
		public Object visitGuardTrueExpression(GuardTrueExpression expression) {
			if(phase == 1) {
				stack.push(getNeyEmptyContraints());
			}
			return expression;
		}
		
		public Object visitGuardNotExpression(GuardNotExpression expression) {
			if(phase == 1) {
				if(cache.containsKey(expression)) {
					Set<Constraint> contrs = cache.get(expression);
					HashSet<Constraint> tmp = new HashSet<Constraint>();
					for(Constraint c : contrs) {
						tmp.add((Constraint) c.clone());
					}
					stack.push(tmp);
					return expression;
				}
			}
			GuardExpression tmp = (GuardExpression) expression.getOperand().accept(this);
			//Produce constraints for this negation (tmp may have been simplified)
			if(phase == 1) {
				Set<Constraint> constraints = stack.pop();
				for(Constraint aConstraint : constraints) {
					aConstraint.shouldBeEqual = !aConstraint.shouldBeEqual;
				}
				stack.push(constraints);
			}
			//Simplification !!a = a
			if(tmp instanceof GuardNotExpression) {
				return ((GuardNotExpression) tmp).getOperand();
			}
			//Prepare result (remember that expressions are immutable)
			GuardExpression result;
			if(tmp == expression.getOperand()) {
				result = expression;
			} else {
				result = new GuardNotExpression(tmp);
				if(phase == 2) {
					Set<Constraint> constraints = new HashSet<Constraint>(cache.get(tmp));
					for(Constraint aConstraint : constraints) {
						aConstraint.shouldBeEqual = !aConstraint.shouldBeEqual;
					}
					addToCache(result, constraints);
				}
			}
			if(phase == 1) {
				addToCache(result, stack.peek());
			}
			return result;
		}
		
		public Object visitGuardAndExpression(GuardAndExpression expression) {
			if(phase == 1) {
				if(cache.containsKey(expression)) {
					Set<Constraint> contrs = cache.get(expression);
					HashSet<Constraint> tmp = new HashSet<Constraint>();
					for(Constraint c : contrs) {
						tmp.add((Constraint) c.clone());
					}
					stack.push(tmp);
					return expression;
				}
			}
			if(phase == 2) {
				HashSet<Constraint> propagate = new HashSet<Constraint>(stack.peek());
				propagate.addAll(cache.get(expression.getRight()));
				stack.push(propagate);
			}
			GuardExpression tmp1 = (GuardExpression) expression.getLeft().accept(this);
			if(phase == 2) {
				stack.pop();
			}
			Set<Constraint> cTmp1 = null;
			if(phase == 1) {
				cTmp1 = stack.pop();
			}
			if(tmp1 instanceof GuardNotExpression && ((GuardNotExpression) tmp1).getOperand() instanceof GuardTrueExpression) {
				if(phase == 1) {
					stack.push(getNeyEmptyContraints());
				}
				return FALSE;
			}
			if(phase == 2) {
				HashSet<Constraint> propagate = new HashSet<Constraint>(stack.peek());
				propagate.addAll(cache.get(expression.getLeft()));
				stack.push(propagate);
			}
			GuardExpression tmp2 = (GuardExpression) expression.getRight().accept(this);
			if(phase == 2) {
				stack.pop();
			}
			Set<Constraint> cTmp2 = null;
			if(phase == 1) {
				cTmp2 = stack.pop();
			}
			if(tmp2 instanceof GuardNotExpression && ((GuardNotExpression) tmp2).getOperand() instanceof GuardTrueExpression) {
				if(phase == 1) {
					stack.push(getNeyEmptyContraints());
				}
				return FALSE;
			}
			if(tmp1 instanceof GuardTrueExpression) {
				if(phase == 1) {
					stack.push(cTmp2);
				}
				return tmp2;
			}
			if(tmp2 instanceof GuardTrueExpression) {
				if(phase == 1) {
					stack.push(cTmp1);
				}
				return tmp1;
			}
			if(phase == 1) {
				for(Constraint constraint1 : cTmp1) {
					for(Constraint constraint2 : cTmp2) {
						if(constraint1.isInContradictionWith(constraint2)) {
							stack.push(getNeyEmptyContraints());
							return FALSE;
						}
					}
				}
				cTmp1.addAll(cTmp2);
				stack.push(cTmp1);
			}
			GuardAndExpression result;
			if(tmp1 == expression.getLeft() && tmp2 == expression.getRight()) {
				result = expression;
			} else {
				result = new GuardAndExpression(tmp1, tmp2);
				if(phase == 2) {
					HashSet<Constraint> tmp = new HashSet<Constraint>(cache.get(tmp1));
					tmp.addAll(cache.get(tmp2));
					addToCache(result, tmp);
				}
			}
			//a & a = a
			GuardExpression tmp = trySimplificationAnd1(result);
			if(tmp != null) {
				if(phase == 1) {
					stack.pop();
				}
				return tmp.accept(this);
			}
			//
			if(phase == 1) {
				addToCache(result, stack.peek());
			}
			return result;
		}

		public Object visitGuardOrExpression(GuardOrExpression expression) {
			if(phase == 1) {
				if(cache.containsKey(expression)) {
					Set<Constraint> contrs = cache.get(expression);
					HashSet<Constraint> tmp = new HashSet<Constraint>();
					for(Constraint c : contrs) {
						tmp.add((Constraint) c.clone());
					}
					stack.push(tmp);
					return expression;
				}
			}
			if(phase == 2) {
				boolean leftHasContradiction = false, rightHasContradiction = false;
				for(Constraint constraint1 : stack.peek()) {
					for(Constraint constraint2 : cache.get(expression.getLeft())) {
						if(constraint1.isInContradictionWith(constraint2)) {
							leftHasContradiction = true;
							break;
						}
					}
					for(Constraint constraint2 : cache.get(expression.getRight())) {
						if(constraint1.isInContradictionWith(constraint2)) {
							rightHasContradiction = true;
							break;
						}
					}
				}
				if(leftHasContradiction && rightHasContradiction) {
					return FALSE;
				} else if(leftHasContradiction) {
					return expression.getRight().accept(this);
				} else if(rightHasContradiction) {
					return expression.getLeft().accept(this);					
				}
			}
			GuardExpression tmp1 = (GuardExpression) expression.getLeft().accept(this);
			Set<Constraint> cTmp1 = null;
			if(phase == 1) {
				cTmp1 = stack.pop();
			}
			if(tmp1 instanceof GuardTrueExpression) {
				if(phase == 1) {
					stack.push(getNeyEmptyContraints());
				}
				return TRUE;
			}
			GuardExpression tmp2 = (GuardExpression) expression.getRight().accept(this);
			Set<Constraint> cTmp2 = null;
			if(phase == 1) {
				cTmp2 = stack.pop();
			}
			if(tmp2 instanceof GuardTrueExpression) {
				if(phase == 1) {
					stack.push(getNeyEmptyContraints());
				}
				return TRUE;
			}
			if(tmp1 instanceof GuardNotExpression && ((GuardNotExpression) tmp1).getOperand() instanceof GuardTrueExpression) {
				if(phase == 1) {
					stack.push(cTmp2);
				}
				return tmp2;
			}
			if(tmp2 instanceof GuardNotExpression && ((GuardNotExpression) tmp2).getOperand() instanceof GuardTrueExpression) {
				if(phase == 1) {
					stack.push(cTmp1);
				}
				return tmp1;
			}
			//TODO: a | !a = true (something more general would be useful; eliminate complements from two fundamental 
			//conjunctions with same literals) 
			if(trySimplificationOr2(expression) != null) {
				if(phase == 1) {
					stack.push(getNeyEmptyContraints());
				}
				return TRUE;				
			}
			if(phase == 1) {
				Set<Constraint> constraints =  getNeyEmptyContraints();
				for(Constraint constraint1 : cTmp1) {
					for(Constraint constraint2 : cTmp2) {
						if(constraint1.isTheSame(constraint2)) {
							constraints.add(constraint1);
							break;
						}
					}
				}
				stack.push(constraints);
			}
			GuardOrExpression result;
			if(tmp1 == expression.getLeft() && tmp2 == expression.getRight()) {
				result = expression;
			} else {
				result = new GuardOrExpression(tmp1, tmp2);
				if(phase == 2) {
					Set<Constraint> constraints =  getNeyEmptyContraints();
					for(Constraint constraint1 : cache.get(tmp1)) {
						for(Constraint constraint2 : cache.get(tmp2)) {
							if(constraint1.isTheSame(constraint2)) {
								constraints.add(constraint1);
								break;
							}
						}
					}
					addToCache(result, constraints);
				}
			}
			//x | ... | a | y | ... | (b & !a) = x | y | ... | a | b 
			GuardExpression resSimplified = trySimplificationOr1(result);
			if(resSimplified != null) {
				if(phase == 1) {
					stack.pop();
				}
				return resSimplified.accept(this);
			}
			//
			// a & b | a & c = a & (b | c) 
			resSimplified = trySimplificationCommonFactor1(result);
			if(resSimplified != null) {
				if(phase == 1) {
					stack.pop();
				}
				return resSimplified.accept(this);
			}
			//
			if(phase == 1) {
				addToCache(result, stack.peek());
			}
			return result;
		}
		
		public Object visitGuardProgramExpression(GuardProgramExpression expression) {
			if(phase == 1) {
				if(cache.containsKey(expression)) {
					Set<Constraint> contrs = cache.get(expression);
					HashSet<Constraint> tmp = new HashSet<Constraint>();
					for(Constraint c : contrs) {
						tmp.add((Constraint) c.clone());
					}
					stack.push(tmp);
					return expression;
				}
			}
			Object tmp = expression.getProgramCondition().accept(this);
			//Simplification v1 ne v2 = !(v1 eq v2)
			if(tmp instanceof ProgramRelationalExpression && ((ProgramRelationalExpression)tmp).getOperator() == IConditionalBranchInstruction.Operator.NE) {
				ProgramRelationalExpression pre = (ProgramRelationalExpression)tmp;
				GuardNotExpression newpre;
				if(pre.getRight() == null)
					newpre = new GuardNotExpression(new GuardProgramExpression(new ProgramRelationalExpression(IConditionalBranchInstruction.Operator.EQ, pre.getLeft(), pre.getConstant())));
				else
					newpre = new GuardNotExpression(new GuardProgramExpression(new ProgramRelationalExpression(IConditionalBranchInstruction.Operator.EQ, pre.getLeft(), pre.getRight())));
				stack.pop();
				return newpre.accept(this);
			}
			if(tmp instanceof ProgramExpression) {
				if(phase == 1) {
					addToCache(expression, stack.peek());
				}
				return expression;
			}
			return tmp;
		}

		public Object visitProgramRelationalExpression(ProgramRelationalExpression expression) {
			ProgramExpression l = expression.getLeft();
			ProgramExpression r = expression.getRight();
			IConditionalBranchInstruction.IOperator op = expression.getOperator();
			Set<Constraint> newConstraints = null;
			if(phase == 1) {
				newConstraints = getNeyEmptyContraints();
			}
			if(l instanceof ProgramIndexExpression && r instanceof ProgramIndexExpression) {
				ProgramIndexExpression il = (ProgramIndexExpression) l;
				ProgramIndexExpression ir = (ProgramIndexExpression) r;
				if(op == IConditionalBranchInstruction.Operator.EQ) {
					if((il.isStringConstant() && ir.isNullConstant()) || (ir.isStringConstant() && il.isNullConstant())) {
						if(phase == 1) {
							stack.push(getNeyEmptyContraints());
						}
						return FALSE;
					}
					if(il.isNumberConstant() && ir.isNumberConstant()) {
						if(il.getNumberConstant().doubleValue() == ir.getNumberConstant().doubleValue()) {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return TRUE;
						} else {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return FALSE;		
						}
					}
				} else if(op == IConditionalBranchInstruction.Operator.NE) {
					if((il.isStringConstant() && ir.isNullConstant()) || (ir.isStringConstant() && il.isNullConstant())) {
						if(phase == 1) {
							stack.push(getNeyEmptyContraints());
						}
						return TRUE;
					}
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() == ir.getNumberConstant().doubleValue()) {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return FALSE;
						} else {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return TRUE;
						}
					}
				} else if(op == IConditionalBranchInstruction.Operator.LT) {
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() < ir.getNumberConstant().doubleValue()) {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return TRUE;
						} else {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return FALSE;
						}
					}					
				} else if(op == IConditionalBranchInstruction.Operator.GE) {
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() >= ir.getNumberConstant().doubleValue()) {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return TRUE;
						} else {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return FALSE;
						}
					}					
				} else if(op == IConditionalBranchInstruction.Operator.GT) {
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() > ir.getNumberConstant().doubleValue()) {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return TRUE;
						} else {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return FALSE;
						}
					}					
				} else if(op == IConditionalBranchInstruction.Operator.LE) {
					if(il.isNumberConstant() && (ir.isNumberConstant())) {
						if(il.getNumberConstant().doubleValue() <= ir.getNumberConstant().doubleValue()) {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return TRUE;
						} else {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return FALSE;
						}
					}					
				}
			} else if(l instanceof ProgramIndexExpression && r == null) {
				ProgramIndexExpression il = (ProgramIndexExpression) l;
				if(op == IConditionalBranchInstruction.Operator.EQ) {
					if(il.isNumberConstant()) {
						if(il.getNumberConstant().doubleValue() == expression.getConstant().doubleValue()) {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return TRUE;
						} else {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return FALSE;							
						}
					}
				} else if(op == IConditionalBranchInstruction.Operator.NE) {
					if(il.isNumberConstant()) {
						if(il.getNumberConstant().doubleValue() == expression.getConstant().doubleValue()) {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return FALSE;
						} else {
							if(phase == 1) {
								stack.push(getNeyEmptyContraints());
							}
							return TRUE;			
						}
					}
				} 
			} else if(r instanceof ProgramIndexExpression && ((ProgramIndexExpression)r).isNullConstant()) {
				if(op == IConditionalBranchInstruction.Operator.EQ) {
					if(l instanceof ProgramSQLCursorAction) {
						if(phase == 1) {
							stack.push(getNeyEmptyContraints());
						}
						return FALSE;
					}
					if(phase == 1) {
						newConstraints.add(new Constraint(l, true, null));											
					} else {
						Constraint myConstraint = new Constraint(l, true, null);
						for(Constraint aConstraint : stack.peek()) {
							GuardExpression result;
							if((result = Constraint.inferValueBasedOnAxioms(aConstraint, myConstraint)) != null) {
								return result;
							}
						}
					}
				}
				if(op == IConditionalBranchInstruction.Operator.NE) {
					if(phase == 1) {
						newConstraints.add(new Constraint(l, false, null));					
					} else {
						Constraint myConstraint = new Constraint(l, false, null);
						for(Constraint aConstraint : stack.peek()) {
							GuardExpression result;
							if((result = Constraint.inferValueBasedOnAxioms(aConstraint, myConstraint)) != null) {
								return result;
							}
						}
					}
				}				
			} else if(l instanceof ProgramIndexExpression && ((ProgramIndexExpression)l).isNullConstant()) {
				if(op == IConditionalBranchInstruction.Operator.EQ) {
					if(phase == 1) {
						newConstraints.add(new Constraint(r, true, null));	
					} else {
						Constraint myConstraint = new Constraint(r, true, null);
						for(Constraint aConstraint : stack.peek()) {
							GuardExpression result;
							if((result = Constraint.inferValueBasedOnAxioms(aConstraint, myConstraint)) != null) {
								return result;
							}
						}						
					}
				}
				if(op == IConditionalBranchInstruction.Operator.NE) {
					if(phase == 1) {
						newConstraints.add(new Constraint(r, false, null));					
					} else {
						Constraint myConstraint = new Constraint(r, false, null);
						for(Constraint aConstraint : stack.peek()) {
							GuardExpression result;
							if((result = Constraint.inferValueBasedOnAxioms(aConstraint, myConstraint)) != null) {
								return result;
							}
						}												
					}
				}
			} else if((l instanceof ProgramStringComparison) && (r instanceof ProgramIndexExpression)) {
				Object tmp = l.accept(this);
				Set<Constraint> constraintsLeft = null;
				if(phase == 1) {
					constraintsLeft = stack.pop();
				}
				if((tmp.equals(TRUE) || tmp.equals(FALSE)) && ((ProgramIndexExpression)r).isNumberConstant()) {
					if(op == IConditionalBranchInstruction.Operator.EQ) {
						if(tmp.equals(TRUE)) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								if(phase == 1) {
									stack.push(getNeyEmptyContraints());
								}
								return TRUE;
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								if(phase == 1) {
									stack.push(getNeyEmptyContraints());
								}
								return FALSE;
							}
						}
						if(tmp.equals(FALSE)) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								if(phase == 1) {
									stack.push(getNeyEmptyContraints());
								}
								return FALSE;
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								if(phase == 1) {
									stack.push(getNeyEmptyContraints());
								}
								return TRUE;
							}
						}
					} else if(op == IConditionalBranchInstruction.Operator.NE) {
						if(tmp.equals(TRUE)) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								if(phase == 1) {
									stack.push(getNeyEmptyContraints());
								}
								return FALSE;
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								if(phase == 1) {
									stack.push(getNeyEmptyContraints());
								}
								return TRUE;
							}
						}
						if(tmp.equals(FALSE)) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								if(phase == 1) {
									stack.push(getNeyEmptyContraints());
								}
								return TRUE;
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								if(phase == 1) {
									stack.push(getNeyEmptyContraints());
								}
								return FALSE;
							}
						}						
					}
				}
				if(phase == 1) {
					if(!(tmp.equals(TRUE) || tmp.equals(FALSE)) && ((ProgramIndexExpression)r).isNumberConstant()) {
						if(op == IConditionalBranchInstruction.Operator.EQ) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								for(Constraint aConstraint : constraintsLeft) {
									newConstraints.add(aConstraint);
								}
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								for(Constraint aConstraint : constraintsLeft) {
									aConstraint.shouldBeEqual = !aConstraint.shouldBeEqual;
									newConstraints.add(aConstraint);
								}
							}
						} else if(op == IConditionalBranchInstruction.Operator.NE) {
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 1) {
								for(Constraint aConstraint : constraintsLeft) {
									aConstraint.shouldBeEqual = !aConstraint.shouldBeEqual;
									newConstraints.add(aConstraint);
								}
							}
							if(((ProgramIndexExpression)r).getNumberConstant().doubleValue() == 0) {
								for(Constraint aConstraint : constraintsLeft) {
									newConstraints.add(aConstraint);
								}
							}
						}
					}				
				}	
			} else if(l instanceof ProgramFunction) {
				ProgramFunction left = (ProgramFunction)l;
				TernaryResult res = left.tryEvaluateComparison(op, r);
				if(res == TernaryResult.TRUE) {
					if(phase == 1) {
						stack.push(getNeyEmptyContraints());
					}
					return TRUE;
				}
				if(res == TernaryResult.FALSE) {
					if(phase == 1) {
						stack.push(getNeyEmptyContraints());
					}
					return FALSE;					
				}
			} else if(r instanceof ProgramFunction) {
				ProgramFunction right = (ProgramFunction)r;
				TernaryResult res = right.tryEvaluateComparison(op, l);
				if(res == TernaryResult.TRUE) {
					if(phase == 1) {
						stack.push(getNeyEmptyContraints());
					}
					return TRUE;
				}
				if(res == TernaryResult.FALSE) {
					if(phase == 1) {
						stack.push(getNeyEmptyContraints());
					}
					return FALSE;					
				}
			}
			if(phase == 1) {
				stack.push(newConstraints);
			}
			return expression;
		}

		public Object visitProgramStringComparison(ProgramStringComparison expression) {
			Set<Constraint> newConstraints = null;
			if(phase == 1) {
				newConstraints = getNeyEmptyContraints();
			}
			ProgramExpression l = expression.getLeft();
			ProgramExpression r = expression.getRight();
			if(l instanceof ProgramIndexExpression && r instanceof ProgramIndexExpression) {
				ProgramIndexExpression il = (ProgramIndexExpression) l;
				ProgramIndexExpression ir = (ProgramIndexExpression) r;
				if(il.isStringConstant() && ir.isStringConstant()) {
					if(il.getStringConstant().equals(ir.getStringConstant())) {
						if(phase == 1) {
							stack.push(getNeyEmptyContraints());
						}
						return TRUE;
					} else {
						if(phase == 1) {
							stack.push(getNeyEmptyContraints());
						}
						return FALSE;
					}
				}
			} 
			if(r instanceof ProgramIndexExpression && ((ProgramIndexExpression)r).isStringConstant()) {
				if(phase == 1) {
					newConstraints.add(new Constraint(l, true, ((ProgramIndexExpression)r).getStringConstant()));
				} else {
					Constraint myConstraint = new Constraint(l, true, ((ProgramIndexExpression)r).getStringConstant());
					for(Constraint aConstraint : stack.peek()) {
						GuardExpression result;
						if((result = Constraint.inferValueBasedOnAxioms(aConstraint, myConstraint)) != null) {
							return result;
						}
					}
				}
			}
			if(l instanceof ProgramIndexExpression && ((ProgramIndexExpression)l).isStringConstant()) {
				if(phase == 1) {
					newConstraints.add(new Constraint(r, true, ((ProgramIndexExpression)l).getStringConstant()));
				} else {
					Constraint myConstraint = new Constraint(r, true, ((ProgramIndexExpression)l).getStringConstant());
					for(Constraint aConstraint : stack.peek()) {
						GuardExpression result;
						if((result = Constraint.inferValueBasedOnAxioms(aConstraint, myConstraint)) != null) {
							return result;
						}
					}
				}
			}
			if(impossibleToBeEqual(l,r)) {
				if(phase == 1) {
					stack.push(getNeyEmptyContraints());
				}
				return FALSE;
			}
			if(phase == 1) {
				stack.push(newConstraints);
			}
			return expression;
		}

	}
		
	//x | ... | a | y | ... | (b & !a) = x | y | ... | a | b 
	private static GuardExpression trySimplificationOr1(GuardOrExpression theRoot) {
		Object termsInDisjuctions = theRoot.accept(
				new ExpressionAcyclicVisitor() {
					public Object visitGuardOrExpression(GuardOrExpression expression) { 
						List<GuardExpression> res = new ArrayList<GuardExpression>();
						Object resL = expression.getLeft().accept(this);
						if(resL == null) {
							res.add(expression.getLeft());
						} else {
							res.addAll((List<GuardExpression>) resL);
						}
						Object resR = expression.getRight().accept(this);
						if(resR == null) {
							res.add(expression.getRight());
						} else {
							res.addAll((List<GuardExpression>) resR);
						}
						return res;
					}
			});		
		List<GuardExpression> allTermsInDisjunctions = (List<GuardExpression>) termsInDisjuctions;
		for(int i = 0; i < allTermsInDisjunctions.size(); i++) {
			GuardExpression t1 = allTermsInDisjunctions.get(i);
			Object termsInConjunctions = t1.accept(
						new ExpressionAcyclicVisitor() {
							public Object visitGuardAndExpression(GuardAndExpression expression) { 
								List<GuardExpression> res = new ArrayList<GuardExpression>();
								Object resL = expression.getLeft().accept(this);
								if(resL == null) {
									res.add(expression.getLeft());
								} else {
									res.addAll((List<GuardExpression>) resL);
								}
								Object resR = expression.getRight().accept(this);
								if(resR == null) {
									res.add(expression.getRight());
								} else {
									res.addAll((List<GuardExpression>) resR);
								}
								return res;
							}
			});
			List<GuardExpression> allTermsInConjunctions;
			if(termsInConjunctions == null) {
				allTermsInConjunctions = new ArrayList<GuardExpression>();
				allTermsInConjunctions.add(t1);
			} else {
				allTermsInConjunctions = (List<GuardExpression>) termsInConjunctions;
			}
			for(int j = 0; j < allTermsInDisjunctions.size(); j++) {
				if(i == j) {
					continue;
				}
				GuardExpression a = allTermsInDisjunctions.get(j);
				boolean mustBeNegated = true;
				if(a instanceof GuardNotExpression) {
					a = ((GuardNotExpression)a).getOperand();
					mustBeNegated = false;
				}
				boolean found = false;
				for(int k = 0; k < allTermsInConjunctions.size(); k++) {
					if(mustBeNegated) {
						if(allTermsInConjunctions.get(k) instanceof GuardNotExpression && a.equals(((GuardNotExpression)allTermsInConjunctions.get(k)).getOperand())) {
							allTermsInConjunctions.remove(k);
							k--;
							found = true;
						}
					} else {
						if(a.equals(allTermsInConjunctions.get(k))) {
							allTermsInConjunctions.remove(k);
							k--;
							found = true;
						}				
					}
				}
				if(found) {
					GuardExpression cumulative;
					if(allTermsInConjunctions.size() == 0) {
						return TRUE;
					} else if(allTermsInConjunctions.size() == 1) {
						cumulative = allTermsInConjunctions.get(0);
					} else {
						cumulative = new GuardAndExpression(allTermsInConjunctions.get(0), allTermsInConjunctions.get(1));
						Set<Constraint> constraints =  new HashSet<Constraint>(cache.get(allTermsInConjunctions.get(0)));
						constraints.addAll(cache.get(allTermsInConjunctions.get(1)));
						addToCache(cumulative, constraints);
						for(int k = 2; k < allTermsInConjunctions.size(); k++) {
							constraints =  new HashSet<Constraint>(cache.get(cumulative));
							cumulative = new GuardAndExpression(cumulative, allTermsInConjunctions.get(k));
							constraints.addAll(cache.get(allTermsInConjunctions.get(k)));
							addToCache(cumulative, constraints);
						}
					}
					allTermsInDisjunctions.set(i, cumulative);
					cumulative = new GuardOrExpression(allTermsInDisjunctions.get(0), allTermsInDisjunctions.get(1));
					Set<Constraint> constraints =  new HashSet<Constraint>();
					for(Constraint c1 : cache.get(allTermsInDisjunctions.get(0))) {
						for(Constraint c2 : cache.get(allTermsInDisjunctions.get(1))) {
							if(c1.isTheSame(c2)) {
								constraints.add(c1);
								break;
							}
						}						
					}
					addToCache(cumulative, constraints);
					for(int k = 2; k < allTermsInDisjunctions.size(); k++) {
						GuardExpression old = cumulative;
						cumulative = new GuardOrExpression(cumulative, allTermsInDisjunctions.get(k));
						constraints = new HashSet<Constraint>();
						for(Constraint c1 : cache.get(old)) {
							for(Constraint c2 : cache.get(allTermsInDisjunctions.get(k))) {
								if(c1.isTheSame(c2)) {
									constraints.add(c1);
									break;
								}
							}						
						}
						addToCache(cumulative, constraints);
					}
					return cumulative;
				}
			}
		}
		return null;
	}
	
	//a | b | ... | !a | ... = true
	private static GuardExpression trySimplificationOr2(GuardOrExpression theRoot) {
		Object termsInDisjuctions = theRoot.accept(
				new ExpressionAcyclicVisitor() {
					public Object visitGuardOrExpression(GuardOrExpression expression) { 
						List<GuardExpression> res = new ArrayList<GuardExpression>();
						Object resL = expression.getLeft().accept(this);
						if(resL == null) {
							res.add(expression.getLeft());
						} else {
							res.addAll((List<GuardExpression>) resL);
						}
						Object resR = expression.getRight().accept(this);
						if(resR == null) {
							res.add(expression.getRight());
						} else {
							res.addAll((List<GuardExpression>) resR);
						}
						return res;
					}
			});
		for(int i = 0; i < ((List<GuardExpression>)termsInDisjuctions).size(); i++) {
			GuardExpression exp1 = ((List<GuardExpression>)termsInDisjuctions).get(i);
			for(int j = i + 1; j < ((List<GuardExpression>)termsInDisjuctions).size(); j++) {
				GuardExpression exp2 = ((List<GuardExpression>)termsInDisjuctions).get(j);
				if(exp1 instanceof GuardNotExpression && 
						((GuardNotExpression) exp1).getOperand().equals(exp2)) {
					return new GuardTrueExpression();
				}
				if(exp2 instanceof GuardNotExpression &&
						((GuardNotExpression) exp2).getOperand().equals(exp1)) {
					return new GuardTrueExpression();					
				}
			}
		}
		return null;
	}

	//a & a ... & ... =  a & ... & ...
	private static GuardExpression trySimplificationAnd1(GuardAndExpression theRoot) {
		Object termsInConjunctions = theRoot.accept(
				new ExpressionAcyclicVisitor() {
					public Object visitGuardAndExpression(GuardAndExpression expression) { 
						List<GuardExpression> res = new ArrayList<GuardExpression>();
						Object resL = expression.getLeft().accept(this);
						if(resL == null) {
							res.add(expression.getLeft());
						} else {
							res.addAll((List<GuardExpression>) resL);
						}
						Object resR = expression.getRight().accept(this);
						if(resR == null) {
							res.add(expression.getRight());
						} else {
							res.addAll((List<GuardExpression>) resR);
						}
						return res;
					}
			});
		boolean found = false; 
		for(int i = 0; i < ((List<GuardExpression>)termsInConjunctions).size(); i++) {
			GuardExpression exp1 = ((List<GuardExpression>)termsInConjunctions).get(i);
			for(int j = i + 1; j < ((List<GuardExpression>)termsInConjunctions).size(); j++) {
				GuardExpression exp2 = ((List<GuardExpression>)termsInConjunctions).get(j);
				if(exp1.equals(exp2)) {
					((List<GuardExpression>)termsInConjunctions).remove(j);
					j--;
					found = true;
				}
			}
		}
		if(found) {
			List<GuardExpression> terms = ((List<GuardExpression>)termsInConjunctions);
			if(terms.size() == 1) {
				return terms.get(0);
			}
			GuardAndExpression cumulative = new GuardAndExpression(terms.get(0), terms.get(1));			
			HashSet<Constraint> constr = new HashSet<Constraint>(cache.get(terms.get(0)));
			constr.addAll(cache.get(terms.get(1)));
			addToCache(cumulative, constr);			
			for(int i = 2; i < terms.size(); i++) {
				constr = new HashSet<Constraint>(cache.get(terms.get(i)));
				constr.addAll(cache.get(cumulative));
				cumulative = new GuardAndExpression(cumulative,terms.get(i));
				addToCache(cumulative, constr);
			}
			return cumulative;
		}
		return null;
	}
	
	//a & b | a & c = a & (b | c)
	private static GuardExpression trySimplificationCommonFactor1(GuardOrExpression theRoot) {
		Object termsInConjunctionsLeft = theRoot.getLeft().accept(
				new ExpressionAcyclicVisitor() {
					public Object visitGuardAndExpression(GuardAndExpression expression) { 
						List<GuardExpression> res = new ArrayList<GuardExpression>();
						Object resL = expression.getLeft().accept(this);
						if(resL == null) {
							res.add(expression.getLeft());
						} else {
							res.addAll((List<GuardExpression>) resL);
						}
						Object resR = expression.getRight().accept(this);
						if(resR == null) {
							res.add(expression.getRight());
						} else {
							res.addAll((List<GuardExpression>) resR);
						}
						return res;
					}
			});
		List<GuardExpression> allTermsInConjunctionsLeft;
		if(termsInConjunctionsLeft == null) {
			allTermsInConjunctionsLeft = new ArrayList<GuardExpression>();
			allTermsInConjunctionsLeft.add(theRoot.getLeft());
		} else {
			allTermsInConjunctionsLeft = (List<GuardExpression>) termsInConjunctionsLeft;
		}
		Object termsInConjunctionsRight = theRoot.getRight().accept(
				new ExpressionAcyclicVisitor() {
					public Object visitGuardAndExpression(GuardAndExpression expression) { 
						List<GuardExpression> res = new ArrayList<GuardExpression>();
						Object resL = expression.getLeft().accept(this);
						if(resL == null) {
							res.add(expression.getLeft());
						} else {
							res.addAll((List<GuardExpression>) resL);
						}
						Object resR = expression.getRight().accept(this);
						if(resR == null) {
							res.add(expression.getRight());
						} else {
							res.addAll((List<GuardExpression>) resR);
						}
						return res;
					}
			});
		List<GuardExpression> allTermsInConjunctionsRight;
		if(termsInConjunctionsRight == null) {
			allTermsInConjunctionsRight = new ArrayList<GuardExpression>();
			allTermsInConjunctionsRight.add(theRoot.getRight());
		} else {
			allTermsInConjunctionsRight = (List<GuardExpression>) termsInConjunctionsRight;
		}
		GuardExpression common = null;
		for(int i = 0; i < allTermsInConjunctionsLeft.size(); i++) {
			boolean extracted = false;
			GuardExpression exp1 = allTermsInConjunctionsLeft.get(i);
			for(int j = 0; j < allTermsInConjunctionsRight.size(); j++) {
				GuardExpression exp2 = allTermsInConjunctionsRight.get(j);
				if(exp1.equals(exp2)) {
					if(common == null) {
						common = exp1;
					} else {
						HashSet<Constraint> constr = new HashSet<Constraint>(cache.get(common));
						common = new GuardAndExpression(common, exp1);
						constr.addAll(cache.get(exp1));
						addToCache(common, constr);	
					}
					allTermsInConjunctionsRight.remove(j);
					j--;
					extracted = true;
				}
			}
			if(extracted) {
				allTermsInConjunctionsLeft.remove(i);
				i--;
			}
		}
		if(common != null) {
			GuardExpression remainedLeft = reconstructConjunction(allTermsInConjunctionsLeft);
			GuardExpression remainedRight = reconstructConjunction(allTermsInConjunctionsRight);
			if(remainedLeft == null || remainedRight == null) {
				return common;
			} else {
				List<GuardExpression> ored = new ArrayList<GuardExpression>();
				ored.add(remainedLeft);
				ored.add(remainedRight);
				List<GuardExpression> anded = new ArrayList<GuardExpression>();
				anded.add(reconstructDisjunction(ored));
				anded.add(common);
				return reconstructConjunction(anded);
			}
		}
		return null;
	}
	
	private static GuardExpression reconstructConjunction(List<GuardExpression> list) {
		if(list.size() == 0) {
			return null;
		} else if(list.size() == 1) {
			return list.get(0);
		} else {
			GuardAndExpression cumulative = new GuardAndExpression(list.get(0), list.get(1));			
			HashSet<Constraint> constr = new HashSet<Constraint>(cache.get(list.get(0)));
			constr.addAll(cache.get(list.get(1)));
			addToCache(cumulative, constr);			
			for(int i = 2; i < list.size(); i++) {
				constr = new HashSet<Constraint>(cache.get(list.get(i)));
				constr.addAll(cache.get(cumulative));
				cumulative = new GuardAndExpression(cumulative,list.get(i));
				addToCache(cumulative, constr);
			}
			return cumulative;
		}
	}
	
	private static GuardExpression reconstructDisjunction(List<GuardExpression> list) {
		if(list.size() == 0) {
			return null;
		} else if(list.size() == 1) {
			return list.get(0);
		} else {
			GuardOrExpression cumulative = new GuardOrExpression(list.get(0), list.get(1));			
			HashSet<Constraint> constr = new HashSet<Constraint>();
			for(Constraint c1 : cache.get(list.get(0))) {
				for(Constraint c2 : cache.get(list.get(1))) {
					if(c1.isTheSame(c2)) {
						constr.add(c1);
						break;
					}
				}
			}
			addToCache(cumulative, constr);			
			for(int i = 2; i < list.size(); i++) {
				constr = new HashSet<Constraint>();
				for(Constraint c1 : cache.get(cumulative)) {
					for(Constraint c2 : cache.get(list.get(i))) {
						if(c1.isTheSame(c2)) {
							constr.add(c1);
							break;
						}
					}
				}
				cumulative = new GuardOrExpression(cumulative,list.get(i));
				addToCache(cumulative, constr);
			}
			return cumulative;
		}
	}
	
	private static boolean impossibleToBeEqual(ProgramExpression l, ProgramExpression r) {
		ExpressionAcyclicVisitor visitor = new ExpressionAcyclicVisitor() {			
			@Override
			public Object visitProgramStringBinaryExpression(ProgramStringBinaryExpression exp) {
				if(exp.getOperator() ==  ProgramStringBinaryExpression.StringBinaryOperator.CONCAT) {
					Object o1 = exp.getLeft().accept(this);
					Object o2 = exp.getRight().accept(this);
					if(o1 != null && o1 instanceof Boolean) {
						return true;
					}
					if(o2 != null && o2 instanceof Boolean) {
						return true;
					}
				}
				return null;
			}
			@Override
			public Object visitProgramIndexExpression(ProgramIndexExpression exp) {
				if(exp.isStringConstant() && !exp.getStringConstant().equals("\"\"")) {
					return true;
				}
				return null;
			}
		};
		//L is the empty string while R is a concatenation containing a constant String != ""
		if(l instanceof ProgramIndexExpression && ((ProgramIndexExpression)l).isStringConstant() && ((ProgramIndexExpression)l).getStringConstant().equals("\"\"") && r instanceof ProgramStringBinaryExpression && ((ProgramStringBinaryExpression)r).getOperator() == ProgramStringBinaryExpression.StringBinaryOperator.CONCAT) {
			Object res = r.accept(visitor);
			if(res instanceof Boolean) {
				return (Boolean)res;
			}
		}
		//R is the empty string while L is a concatenation containing a constant String != ""
		if(r instanceof ProgramIndexExpression && ((ProgramIndexExpression)r).isStringConstant() && ((ProgramIndexExpression)r).getStringConstant().equals("\"\"") && l instanceof ProgramStringBinaryExpression && ((ProgramStringBinaryExpression)l).getOperator() == ProgramStringBinaryExpression.StringBinaryOperator.CONCAT) {
			Object res = l.accept(visitor);			
			if(res instanceof Boolean) {
				return (Boolean)res;
			}
		}
		return false;
	}


}
