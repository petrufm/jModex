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
package ro.ieat.isummarize2aslanpp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ro.ieat.isummarize.ExpressionAcyclicVisitor;
import ro.ieat.isummarize.GuardAndExpression;
import ro.ieat.isummarize.GuardNotExpression;
import ro.ieat.isummarize.GuardOrExpression;
import ro.ieat.isummarize.GuardProgramExpression;
import ro.ieat.isummarize.GuardTrueExpression;
import ro.ieat.isummarize.MethodSummary;
import ro.ieat.isummarize.ModelFactory;
import ro.ieat.isummarize.ProgramBinaryExpression;
import ro.ieat.isummarize.ProgramConversionExpression;
import ro.ieat.isummarize.ProgramGetObject;
import ro.ieat.isummarize.ProgramIndexExpressionVariable;
import ro.ieat.isummarize.ProgramUniqueInstanceCounter;
import ro.ieat.isummarize.ProgramUniqueInstanceCounterAction;
import ro.ieat.isummarize.Utils;
import ro.ieat.isummarize.ProgramConversionExpression.ConversionOperation;
import ro.ieat.isummarize.ProgramExpression;
import ro.ieat.isummarize.ProgramField;
import ro.ieat.isummarize.ProgramFunction;
import ro.ieat.isummarize.ProgramIndexExpression;
import ro.ieat.isummarize.ProgramIndexExpressionConstant;
import ro.ieat.isummarize.ProgramRelationalExpression;
import ro.ieat.isummarize.ProgramStringBinaryExpression;
import ro.ieat.isummarize.ProgramStringComparison;
import ro.ieat.isummarize.collections.ProgramCollectionAction;
import ro.ieat.isummarize.collections.ProgramCollectionAction.CollectionActions;
import ro.ieat.isummarize.collections.ProgramCollectionAction.ProgramCollectionActionVisitor;
import ro.ieat.isummarize.collections.ProgramCollectionGet;
import ro.ieat.isummarize.collections.ProgramCollectionGet.ProgramCollectionGetVisitor;
import ro.ieat.isummarize.collections.ProgramCollections;
import ro.ieat.isummarize.collections.ProgramCollections.ProgramCollectionVisitor;
import ro.ieat.isummarize.javasql.ProgramDataBase;
import ro.ieat.isummarize.javasql.ProgramDataBase.ProgramDataBaseVisitor;
import ro.ieat.isummarize.javasql.ProgramDataBaseModification;
import ro.ieat.isummarize.javasql.ProgramDataBaseModification.ProgramDataBaseModificationVisitor;
import ro.ieat.isummarize.javasql.ProgramDbRValue;
import ro.ieat.isummarize.javasql.ProgramDbRValue.ProgramDbRValueVisitor;
import ro.ieat.isummarize.javasql.ProgramSQLCursorAction;
import ro.ieat.isummarize.javasql.ProgramSQLCursorAction.ProgramSQLCursorActionVisitor;
import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute;
import ro.ieat.isummarize.jsptomcat.ProgramRequestParameter.ProgramRequestParameterVisitor;
import ro.ieat.isummarize.jsptomcat.ProgramSessionAttribute.ProgramSessionAttributeVisitor;
import ro.ieat.isummarize2aslanpp.interval.AvailableCursorFact;
import ro.ieat.isummarize2aslanpp.interval.AvailableRequestParameter;
import ro.ieat.isummarize2aslanpp.interval.Fact;
import ro.ieat.jmodex.utils.jModexProgressMonitor;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.IOperator;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator;
import com.ibm.wala.types.TypeReference;

public class AslanppGenerator {

	enum ReqParRepresentation {USING_SETS, USING_FUNCTION_ARGUMENTS}
	enum SessionRepresentation {USING_SETS, USING_VARIABLES}
	
	static ReqParRepresentation REQUEST_PARAMETER_AS = ReqParRepresentation.USING_FUNCTION_ARGUMENTS;
	static boolean REQUEST_PARAMETER_FILTERING = true;
	
	static SessionRepresentation SESSION_ATTR_AS = SessionRepresentation.USING_VARIABLES;
	
	static final String PREFIX = "r";
	static final String CONSTANT_STRING_PREFIX = "s";
	static final String REQUEST_PARAMETER_PREFIX = "RQP_";
	static final String SESSION_ATTRIBUTE_PREFIX = "SA_";
	static final String EMPTY_STRING_NAME = "sEmpty";
	static final String NULL_REFERENCE_NAME = "oNull";
	static final String INT_NULL_REFERENCE_NAME = "intNull";
	static final String TMP_PARAM_PREFIX = "TMP";
	static final String TMP_APARAMA_NAME = "aParamName";
	static final String TMP_APARAMA_VALUE = "aParamValue";

	static final AslanppType JAVA_REFERENCE_ASLANPP = AslanppType.messageType;
	
	static AslanppEntity environmentEntity;

	private static HashMap<String,AslanppSymbol> saNames = new HashMap<String,AslanppSymbol>();

	public static AslanppModel generate(String name, Map<IMethod, MethodSummary> behaviour, jModexProgressMonitor monitor) {
		initIntegerTheory();
		FactorOutCursorCreationOptimisation.reset();
		saNames.clear();
		//Initialization
		AslanppModel model = new AslanppModel(name);
		environmentEntity = model.getEnvironment();
		environmentEntity.addStatement(new AslanppStringStatement("any A. Session(A) where A!=i",true));
		AslanppEntity sessionEntity = environmentEntity.searchOrAddSubEntity("Session");
		sessionEntity.searchOrAddParameter(PREFIX+"S","",AslanppType.agentType);
		AslanppEntity applicationEntity = sessionEntity.searchOrAddSubEntity("Application");	
		AslanppParameter theActor = applicationEntity.searchOrAddParameter("Actor","",AslanppType.agentType);
		AslanppParameter theUser = applicationEntity.searchOrAddParameter(PREFIX+"U","",AslanppType.agentType);
		AslanppParameter theSession = null;
		if(SESSION_ATTR_AS == SessionRepresentation.USING_SETS) {
			theSession = applicationEntity.searchOrAddParameter(PREFIX+"Sess","",AslanppType.getSetType(AslanppType.getTupleType(JAVA_REFERENCE_ASLANPP,JAVA_REFERENCE_ASLANPP)));
			sessionEntity.addStatement(new AslanppStringStatement("new Application("+PREFIX.toUpperCase()+"S,i,{})",true));
		} else {
			sessionEntity.addStatement(new AslanppStringStatement("new Application("+PREFIX.toUpperCase()+"S,i)",true));
		}
		// Convert the summaries
		monitor.beginTask("Translate each method summary", behaviour.entrySet().size());
		ConversionVisitor cv = new ConversionVisitor();
		if(behaviour.get(null) != null) {
			List<AslanppStatement> res = (List<AslanppStatement>)cv.process(applicationEntity, environmentEntity, theActor, theUser, theSession, behaviour.get(null), false);
			monitor.worked(1);
			for(AslanppStatement aStm : res) {
				applicationEntity.addStatement(aStm);
			}
		}
		AslanppWhile theWhile = new AslanppWhile(new AslanppStringStatement("true",false));
		AslanppSelect theSelect = new AslanppSelect();
		for(IMethod aMethod: behaviour.keySet()) {
			if(aMethod != null) {
				AslanppOn theOn = (AslanppOn)cv.process(applicationEntity, environmentEntity, theActor, theUser, theSession, behaviour.get(aMethod), true);
				if(theOn != null) {
					theSelect.addOnStatement(theOn);
				}
				monitor.worked(1);
			}
		}
		if(!theSelect.isEmpty()) {
			theWhile.addStatement(theSelect);
		}
		completeIntegerTheory(environmentEntity);
		if(SESSION_ATTR_AS == SessionRepresentation.USING_VARIABLES) {
			for(String keyString : saNames.keySet()) {
				applicationEntity.addStatement(new AslanppAssignment(saNames.get(keyString).getReference(false, null), cv.getNullReference4Type(saNames.get(keyString).getType())));
			}
		}
		applicationEntity.addStatement(theWhile);
		monitor.done();
		return model;
	}
		
	public static class ConversionVisitor extends ExpressionAcyclicVisitor implements ProgramRequestParameterVisitor, ProgramSessionAttributeVisitor, ProgramDbRValueVisitor, ProgramSQLCursorActionVisitor, ProgramDataBaseVisitor, ProgramDataBaseModificationVisitor, ProgramCollectionActionVisitor, ProgramCollectionGetVisitor, ProgramCollectionVisitor {
		
		private Set<Fact> facts;
		
		public void setFacts(Set<Fact> facts) {
			this.facts = facts;
		}

		public Set<Fact> getFacts() {
			return facts;
		}

		private AslanppGeneratorSQLConstruction sqlConstrunctionGenerator = new AslanppGeneratorSQLConstruction(this);  
				
		private AslanppEntity theApp,theEnv;
		private AslanppParameter theSession;
		private AslanppParameter theActor;
		private AslanppParameter theUser;
		private AslanppSymbol thePars;
		private MethodSummary summary;
		private List<String> rqpNames = new ArrayList<String>();
		
		AslanppReference getNullReference() {
			return theApp.searchOrAddSymbol(true, false, NULL_REFERENCE_NAME, "", JAVA_REFERENCE_ASLANPP).getReference(false, null);
		}

		AslanppReference getNullReference4Type(AslanppType at) {
			if(at == AslanppType.natType) {
				return new AslanppStringStatement("0",false);
			} else if(at == javaType2AslanppType(theEnv, "int")) {
				return theApp.searchOrAddSymbol(true, false, INT_NULL_REFERENCE_NAME, "", javaType2AslanppType(theEnv, "int")).getReference(false, null);
			} else {
				return getNullReference();
			}
		}

		AslanppEntity getEnvironmentEntity() {
			return theEnv;
		}

		AslanppEntity getApplicationEntity() {
			return theApp;
		}

		public Object process(AslanppEntity theApp, AslanppEntity theEnv, AslanppParameter theActor, AslanppParameter theUser, AslanppParameter theSession, MethodSummary summary, boolean isWithMessage) {
			this.theApp = theApp;
			this.theEnv = theEnv;
			this.theSession = theSession;
			this.theActor = theActor;
			this.theUser = theUser;
			this.summary = summary;
			Region controlTree = ControlTreeBuilder.buildTree(summary);
			if(isWithMessage) {
				rqpNames.clear();
				List<AslanppStatement> code = controlTree.generateCode(this,null);
				if(code.isEmpty() || (code.size() == 1 && code.get(0) instanceof AslanppSelect && ((AslanppSelect)code.get(0)).isEmpty())) {
					return null;
				}
				String messageName = PREFIX + summary.getFullName().replace('.', '_');
				AslanppMessage theMessage;
				if(REQUEST_PARAMETER_AS == ReqParRepresentation.USING_SETS) {
					thePars = theApp.searchOrAddSymbol(false, true, PREFIX+"Params","",AslanppType.getSetType(AslanppType.getTupleType(JAVA_REFERENCE_ASLANPP,JAVA_REFERENCE_ASLANPP)));
					if(REQUEST_PARAMETER_FILTERING) {
						AslanppSymbol tmpPars = theApp.searchOrAddSymbol(false, true, TMP_PARAM_PREFIX + thePars.getName(), "", AslanppType.getSetType(AslanppType.getTupleType(JAVA_REFERENCE_ASLANPP,JAVA_REFERENCE_ASLANPP)));
						AslanppSymbol aParamName = theApp.searchOrAddSymbol(false, true, TMP_APARAMA_NAME, "", AslanppType.messageType);
						AslanppSymbol aParamValue = theApp.searchOrAddSymbol(false, true, TMP_APARAMA_VALUE, "", AslanppType.messageType);
						AslanppTuple messageArgs = new AslanppTuple(tmpPars.getReference(true, null));
						theMessage = new AslanppMessage(theUser.getReference(true, null), theActor.getReference(false, null), theApp.searchOrAddSymbol(true, false, messageName, AslanppType.getSetType(AslanppType.getTupleType(JAVA_REFERENCE_ASLANPP,JAVA_REFERENCE_ASLANPP)).getName(), AslanppType.messageType).getReference(false, messageArgs), true);
						code.add(0,new AslanppAssignment(thePars.getReference(false, null),new AslanppEmptySetInstantiation()));
						AslanppReference thePair[] = new AslanppReference[2];
						thePair[0] = aParamName.getReference(true, null);
						thePair[1] = aParamValue.getReference(true, null);
						AslanppWhile filteringWhile = new AslanppWhile(new AslanppContains(tmpPars.getReference(false, null),new AslanppTuple(thePair), true));
						thePair = new AslanppReference[2];
						thePair[0] = aParamName.getReference(false, null);
						thePair[1] = new AslanppStringStatement("?", false);
						AslanppIf filteringIf = new AslanppIf(AslanppLogicalNegation.createInstance(new AslanppContains(thePars.getReference(false, null),new AslanppTuple(thePair),true)));
						thePair = new AslanppReference[2];
						thePair[0] = aParamName.getReference(false, null);
						thePair[1] = aParamValue.getReference(false, null);					
						filteringIf.addStatementThen(new AslanppContains(thePars.getReference(false, null),new AslanppTuple(thePair),false));
						filteringWhile.addStatement(filteringIf);
						thePair = new AslanppReference[2];
						thePair[0] = aParamName.getReference(false, null);
						thePair[1] = aParamValue.getReference(false, null);				
						filteringWhile.addStatement(new AslanppRemove(tmpPars.getReference(false, null),new AslanppTuple(thePair)));
						code.add(1,filteringWhile);
					} else {
						AslanppTuple messageArgs = new AslanppTuple(thePars.getReference(true, null));
						theMessage = new AslanppMessage(theUser.getReference(true, null), theActor.getReference(false, null), theApp.searchOrAddSymbol(true, false, messageName, AslanppType.getSetType(AslanppType.getTupleType(JAVA_REFERENCE_ASLANPP,JAVA_REFERENCE_ASLANPP)).getName(), AslanppType.messageType).getReference(false, messageArgs), true);
					}
				} else {
					Collections.sort(rqpNames);
					List<AslanppType> rqpTypes  = new ArrayList<AslanppType>();
					List<AslanppReference> rqpReferences  = new ArrayList<AslanppReference>();
					String structure = "";
					for(String anRQP : rqpNames) {
						rqpTypes.add(AslanppType.messageType);
						structure+=rqpTypes.get(rqpTypes.size()-1).getName() + ",";
						rqpReferences.add(theApp.searchOrAddSymbol(false, true, anRQP, "", AslanppType.messageType).getReference(true, null));
					}
					if(!structure.equals("")) {
						structure = structure.substring(0,structure.length()-1);
					}
					AslanppTuple messageArgs = new AslanppTuple(rqpReferences.toArray(new AslanppReference[] {}));
					theMessage = new AslanppMessage(theUser.getReference(true, null), theActor.getReference(false, null), theApp.searchOrAddSymbol(true, false, messageName, structure, AslanppType.messageType).getReference(false, messageArgs), true);					
				}
				AslanppOn theOn = new AslanppOn(theMessage);
				for(AslanppStatement st : code) {
					theOn.addStatement(st);
				}
				return theOn;
			} else {
				return controlTree.generateCode(this,null);
			}
		}
				
		public Object visitGuardAndExpression(GuardAndExpression expression) {
			final TranslatedExpression transL = (TranslatedExpression) expression.getLeft().accept(this);
			final TranslatedExpression transR = (TranslatedExpression) expression.getRight().accept(this);
			return new TranslatedExpression<GuardAndExpression>(expression) {				

				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) { return null; }
					List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
					code.addAll(transL.getRValueCode(inGuard, null, null));
					code.addAll(transR.getRValueCode(inGuard, null, null));
					return code;
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) { return null; }
					AslanppReference refL = transL.getReferenceForRValueCode(inGuard, null, null);
					AslanppReference refR = transR.getReferenceForRValueCode(inGuard, null, null);
					return AslanppLogicalConjunction.createInstance(refL,refR);		
				}
				
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to an and expression!");
				}

			};
		}
		
		@Override
		public Object visitGuardOrExpression(GuardOrExpression expression) {
			final TranslatedExpression transL = (TranslatedExpression) expression.getLeft().accept(this);
			final TranslatedExpression transR = (TranslatedExpression) expression.getRight().accept(this);
			return new TranslatedExpression<GuardOrExpression>(expression) {				

				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) { return null; }
					List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
					code.addAll(transL.getRValueCode(inGuard, null, null));
					code.addAll(transR.getRValueCode(inGuard, null, null));
					return code;
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) { return null; }
					AslanppReference refL = transL.getReferenceForRValueCode(inGuard, null, null);
					AslanppReference refR = transR.getReferenceForRValueCode(inGuard, null, null);
					return AslanppLogicalDisjunction.createInstance(refL,refR);
				}
				
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to an or expression!");
				}

			};
		}
		
		public Object visitGuardNotExpression(GuardNotExpression expression) {
			final TranslatedExpression trans = (TranslatedExpression) expression.getOperand().accept(this);
			return new TranslatedExpression<GuardNotExpression>(expression) {

				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) { return null; }
					List<AslanppStatement> code = super.getRValueCode(inGuard,null,null);
					code.addAll(trans.getRValueCode(inGuard,null,null));
					return code;
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) { return null; }
					AslanppReference ref = trans.getReferenceForRValueCode(inGuard, null, null);  
					return AslanppLogicalNegation.createInstance(ref);					
				}
				
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to a not expression!");
				}
				
			};
		}
		
		public Object visitGuardTrueExpression(GuardTrueExpression expression) {
			return new TranslatedExpression<GuardTrueExpression>(expression) {
				
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) { return null; }
					return super.getRValueCode(inGuard, null, null);
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) { return null; }
					return new AslanppStringStatement("true",false);
				}

				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to the true symbol!");
				}
				
			};
		}

		public Object visitProgramSessionAttribute(ProgramSessionAttribute expression) {
			if(SESSION_ATTR_AS == SessionRepresentation.USING_SETS) {
				//Session attributes are represented using sets
				final TranslatedExpression translatedAttribute = (TranslatedExpression) expression.getAttribute().accept(this);
				return new TranslatedExpression<ProgramSessionAttribute>(expression) {
	
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						List<AslanppStatement> theCode = super.getRValueCode(inGuard, null, null);
						theCode.addAll(translatedAttribute.getRValueCode(inGuard, null, null));
						if(operator == null) {
							AslanppSymbolReference theAttributeSymbol = (AslanppSymbolReference) translatedAttribute.getReferenceForRValueCode(inGuard, null, null);
							AslanppSymbolReference theAttributeValue = theApp.searchOrAddSymbol(false, true, theAttributeSymbol.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(true, null);
							AslanppReference theTest = AslanppLogicalNegation.createInstance(new AslanppContains(theSession.getReference(false, null), new AslanppTuple(theAttributeSymbol,theAttributeValue), true));
							AslanppIf theIf = new AslanppIf(theTest);
							theIf.addStatementThen(new AslanppAssignment(theApp.searchOrAddSymbol(false, true, theAttributeSymbol.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(false, null),getNullReference()));
							theCode.add(theIf);
						} else {
							theCode.addAll(comparingValue.getRValueCode(inGuard, null, null));
						}
						return theCode;
					}
	
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {					
						AslanppSymbolReference theAttributeNameSymbolReference = (AslanppSymbolReference) translatedAttribute.getReferenceForRValueCode(inGuard, null, null);
						if(operator != null) {
							if(operator == Operator.EQ) {	
								if(comparingValue.originalExpression instanceof ProgramIndexExpression && ((ProgramIndexExpression)comparingValue.originalExpression).isNullConstant()) {
									return AslanppLogicalNegation.createInstance(new AslanppContains(theSession.getReference(false, null), new AslanppTuple(theAttributeNameSymbolReference,new AslanppStringStatement("?",false)), true));	
								} else {
									return new VirtualAslanppSpecialContains(theSession.getReference(false, null), theAttributeNameSymbolReference, comparingValue.getReferenceForRValueCode(inGuard, null, null), true, null);
								}
							} else if(operator == Operator.NE){
								if(comparingValue.originalExpression instanceof ProgramIndexExpression && ((ProgramIndexExpression)comparingValue.originalExpression).isNullConstant()) {
									return new AslanppContains(theSession.getReference(false, null), new AslanppTuple(theAttributeNameSymbolReference,new AslanppStringStatement("?",false)), true);	
								} else {	
									return new VirtualAslanppSpecialContains(theSession.getReference(false, null), theAttributeNameSymbolReference, comparingValue.getReferenceForRValueCode(inGuard, null, null), false, null);
								}
							}
							throw new RuntimeException("Cannot handle " + operator + " for state attributes!");
						} else {
							AslanppSymbolReference theAttributeValue = theApp.searchOrAddSymbol(false, true, theAttributeNameSymbolReference.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(false, null);
							return theAttributeValue;
						}
					}
					
					@Override
					public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
						List<AslanppStatement> theCode = super.getLValueCode(null);
						AslanppSymbolReference theAttributeSymbol = (AslanppSymbolReference) translatedAttribute.getReferenceForRValueCode(false,null,null);
						AslanppSymbolReference theAttributeValueQuestion = theApp.searchOrAddSymbol(false, true, theAttributeSymbol.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(true, null);
						AslanppIf toDeleteIf = new AslanppIf(new AslanppContains(theSession.getReference(false, null), new AslanppTuple(theAttributeSymbol,theAttributeValueQuestion), true));
						AslanppSymbolReference theAttributeValue = theApp.searchOrAddSymbol(false, true, theAttributeSymbol.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(false, null);
						toDeleteIf.addStatementThen(new AslanppRemove(theSession.getReference(false, null), new AslanppTuple(theAttributeSymbol,theAttributeValue)));
						theCode.addAll(assignedExpression.getRValueCode(false,null,null));
						theCode.add(toDeleteIf);
						AslanppReference theNullSymbol = getNullReference();
						AslanppReference assignedReference = assignedExpression.getReferenceForRValueCode(true,null,null);
						if(assignedReference instanceof AslanppSymbolReference && ((AslanppSymbolReference)assignedReference).getSymbol().isConstant()) {
							if(!theNullSymbol.isTheSame(assignedReference)) {
								theCode.add(new AslanppContains(theSession.getReference(false, null), new AslanppTuple(theAttributeSymbol,assignedExpression.getReferenceForRValueCode(false,null,null)), false));							
							}
							return theCode;						
						} else {
							AslanppIf shouldBeAddedIf = new AslanppIf(new AslanppInequality(assignedReference, theNullSymbol)); 
							shouldBeAddedIf.addStatementThen(new AslanppContains(theSession.getReference(false, null), new AslanppTuple(theAttributeSymbol,assignedExpression.getReferenceForRValueCode(false,null,null)), false));
							theCode.add(shouldBeAddedIf);
						}
						return theCode;
					}
	
				};
			} else {
				//Session attributes are represented using distinct variables
				if(!(expression.getAttribute() instanceof ProgramIndexExpressionConstant)) {
					throw new RuntimeException("Cannot represent session attributes as variables because some of them are accessed using values knows only at runtime!");
				}
				String attrNameConstant = ((ProgramIndexExpressionConstant)expression.getAttribute()).getStringConstant();
				final String attrName = eliminateAnslanppTokens(SESSION_ATTRIBUTE_PREFIX + attrNameConstant.substring(1,attrNameConstant.length()-1));
				return new TranslatedExpression<ProgramSessionAttribute>(expression) {				
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) {
							if((operator == Operator.EQ || operator == Operator.NE) 
									&& comparingValue.originalExpression instanceof ProgramIndexExpressionConstant 
									&& ((ProgramIndexExpressionConstant)comparingValue.originalExpression).isNullConstant()) {
								return new ArrayList<AslanppStatement>();
							}
							return null;
						}
						return new ArrayList<AslanppStatement>();
					}
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) { 
							if((operator == Operator.EQ || operator == Operator.NE) 
									&& comparingValue.originalExpression instanceof ProgramIndexExpressionConstant 
									&& ((ProgramIndexExpressionConstant)comparingValue.originalExpression).isNullConstant()) {
								Set<TypeReference> possibleTypes = originalExpression.getPossibleTypes();
								if(possibleTypes.size() != 1) {
									throw new RuntimeException("Cannot handle casses in which a session attributes can have 0 or more than one runtime types!");
								}
								String theTypeName = possibleTypes.iterator().next().getName().toString();
								AslanppSymbol saSymbol = theApp.searchOrAddSymbol(false, true, attrName, "", javaType2AslanppType(theApp, Utils.getFullQualifiedTypeNameFromBinaryName(theTypeName)));
								saNames.put(attrName, saSymbol);
								if(operator == Operator.EQ) {
									return new AslanppEquality(saSymbol.getReference(false, null), getNullReference4Type(saSymbol.getType()));
								} else {
									return new AslanppInequality(saSymbol.getReference(false, null), getNullReference4Type(saSymbol.getType()));
								}
							}
							return null;
						}
						Set<TypeReference> possibleTypes = originalExpression.getPossibleTypes();
						if(possibleTypes.size() != 1) {
							throw new RuntimeException("Cannot handle casses in which a session attributes can have 0 or more than one runtime types!");
						}
						String theTypeName = possibleTypes.iterator().next().getName().toString();
						AslanppSymbol saSymbol = theApp.searchOrAddSymbol(false, true, attrName, "", javaType2AslanppType(theApp, Utils.getFullQualifiedTypeNameFromBinaryName(theTypeName)));
						saNames.put(attrName, saSymbol);
						return saSymbol.getReference(false, null);
					}					
					@Override
					public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
						List<AslanppStatement> theCode = super.getLValueCode(null);
						theCode.addAll(assignedExpression.getRValueCode(false,null,null));						
						Set<TypeReference> possibleTypes = originalExpression.getPossibleTypes();
						if(possibleTypes.size() != 1) {
							throw new RuntimeException("Cannot handle casses in which a session attributes can have 0 or more than one runtime types!");
						}
						String theTypeName = possibleTypes.iterator().next().getName().toString();
						AslanppSymbol saSymbol = theApp.searchOrAddSymbol(false, true, attrName, "", javaType2AslanppType(theApp, Utils.getFullQualifiedTypeNameFromBinaryName(theTypeName)));
						AslanppSymbolReference theAttributeVariable = saSymbol.getReference(false, null);
						AslanppReference assignedReference = assignedExpression.getReferenceForRValueCode(false,null,null);
						AslanppAssignment assignemnt = new AslanppAssignment(theAttributeVariable, assignedReference);
						theCode.add(assignemnt);
						saNames.put(attrName, saSymbol);
						return theCode;
					}
				};
			}
		}

		public Object visitProgramRequestParameter(ProgramRequestParameter expression) {
			if(REQUEST_PARAMETER_AS == ReqParRepresentation.USING_SETS) {
				//Request parameters are represented using sets
				final TranslatedExpression translatedParameter = (TranslatedExpression) expression.getParam().accept(this);
				return new TranslatedExpression<ProgramRequestParameter>(expression) {				
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						List<AslanppStatement> theCode = super.getRValueCode(inGuard, null, null);
						theCode.addAll(translatedParameter.getRValueCode(inGuard, null, null));
						if(operator == null) {
							AslanppSymbolReference theParameterNameSymbolReference = (AslanppSymbolReference) translatedParameter.getReferenceForRValueCode(inGuard, null, null);
							//Optimization:maybe the value is available
							for(Fact f : facts) {
								if(f instanceof AvailableRequestParameter && ((AvailableRequestParameter) f).getProgramRequestParameter().structuralEquals(originalExpression)) {
									return theCode;
								}
							}
							//
							AslanppSymbolReference theParameterValue = theApp.searchOrAddSymbol(false, true, theParameterNameSymbolReference.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(true, null);
							AslanppReference theTest = AslanppLogicalNegation.createInstance(new AslanppContains(thePars.getReference(false, null), new AslanppTuple(theParameterNameSymbolReference,theParameterValue), true));
							AslanppIf theIf = new AslanppIf(theTest);
							theIf.addStatementThen(new AslanppAssignment(theApp.searchOrAddSymbol(false, true, theParameterNameSymbolReference.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(false, null),getNullReference()));
							theCode.add(theIf);
							AslanppSymbolReference theParameterValueUnbound = theApp.searchOrAddSymbol(false, true, theParameterNameSymbolReference.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(false, null);
							facts.add(new AvailableRequestParameter(originalExpression, theParameterValueUnbound));
						} else {
							theCode.addAll(comparingValue.getRValueCode(inGuard, null, null));
						}
						return theCode;
					}
					
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						AslanppSymbolReference theParameterNameSymbolReference = (AslanppSymbolReference) translatedParameter.getReferenceForRValueCode(inGuard, null, null);
						if(operator != null) {
							AslanppSymbol theParamSymbol = theApp.searchOrAddSymbol(false, true, theParameterNameSymbolReference.getName(), "", JAVA_REFERENCE_ASLANPP);
							if(operator == Operator.EQ) {
								if(comparingValue.originalExpression instanceof ProgramIndexExpression && ((ProgramIndexExpression)comparingValue.originalExpression).isNullConstant()) {
									facts.add(new AvailableRequestParameter(originalExpression, getNullReference()));
									return AslanppLogicalNegation.createInstance(new AslanppContains(thePars.getReference(false, null), new AslanppTuple(theParameterNameSymbolReference,theParamSymbol.getReference(true,null)), true));
								} else {
									AslanppReference comparingReference = comparingValue.getReferenceForRValueCode(inGuard, null, null);
									facts.add(new AvailableRequestParameter(originalExpression,comparingReference));
									return new VirtualAslanppSpecialContains(thePars.getReference(false, null), theParameterNameSymbolReference, comparingReference, true, theParamSymbol);
								}
							} else if(operator == Operator.NE) {
								AslanppSymbolReference theParameterValueBound = theParamSymbol.getReference(true, null);
								AslanppSymbolReference theParameterValueNotBound = theParamSymbol.getReference(false, null);
								if(comparingValue.originalExpression instanceof ProgramIndexExpression && ((ProgramIndexExpression)comparingValue.originalExpression).isNullConstant()) {			
									facts.add(new AvailableRequestParameter(originalExpression,theParameterValueNotBound));
									return new AslanppContains(thePars.getReference(false, null), new AslanppTuple(theParameterNameSymbolReference,theParameterValueBound), true);
								} else {
									facts.add(new AvailableRequestParameter(originalExpression,theParameterValueNotBound));								
									return new VirtualAslanppSpecialContains(thePars.getReference(false, null), theParameterNameSymbolReference, comparingValue.getReferenceForRValueCode(inGuard, null, null), false, theParamSymbol);
								}
							}
							throw new RuntimeException("Cannot handle " + operator + " for request parameter!");
						} else {
							//Optimization:maybe the value is available
							for(Fact f : facts) {
								if(f instanceof AvailableRequestParameter && ((AvailableRequestParameter) f).getProgramRequestParameter().structuralEquals(originalExpression)) {
									return ((AvailableRequestParameter) f).getReference();
								}
							}
							//
							AslanppSymbolReference theParameterValue = theApp.searchOrAddSymbol(false, true, theParameterNameSymbolReference.getName(), "", JAVA_REFERENCE_ASLANPP).getReference(false, null);
							return theParameterValue;
						}
					}
					
					@Override
					public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
						throw new RuntimeException("Cannot assign to a request attribute!");
					}
	
				};
			} else {
				//Request parameters are represented as function arguments
				if(!(expression.getParam() instanceof ProgramIndexExpressionConstant)) {
					throw new RuntimeException("Cannot represent request parameters as arguments because some of them are accessed using values knows only at runtime!");
				}
				String paramNameConstant = ((ProgramIndexExpressionConstant)expression.getParam()).getStringConstant();
				final String paramName = eliminateAnslanppTokens(REQUEST_PARAMETER_PREFIX + paramNameConstant.substring(1,paramNameConstant.length()-1));
				if(!rqpNames.contains(paramName)) {
					rqpNames.add(paramName);
				}
				return new TranslatedExpression<ProgramRequestParameter>(expression) {				
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) { return null;}
						return theApp.searchOrAddSymbol(false, true, paramName, "", JAVA_REFERENCE_ASLANPP).getReference(false, null);
					}					
					@Override
					public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
						throw new RuntimeException("Cannot assign to a request attribute!");
					}
				};
			}
		}
				
		public Object visitGuardProgramExpression(GuardProgramExpression expression) {
			return expression.getProgramCondition().accept(this);
		}
		
		public Object visitProgramBinaryExpression(ProgramBinaryExpression expression) {
			final TranslatedExpression transL = (TranslatedExpression) expression.getLeft().accept(this);
			final TranslatedExpression transR = (TranslatedExpression) expression.getRight().accept(this);
			return new TranslatedExpression<ProgramBinaryExpression>(expression) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> code = super.getRValueCode(inGuard, operator, comparingValue);
					code.addAll(transL.getRValueCode(inGuard, operator, comparingValue));
					code.addAll(transR.getRValueCode(inGuard, operator, comparingValue));
					AslanppReference refL = transL.getReferenceForRValueCode(inGuard, operator, comparingValue);
					return code;
				}
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;					
					AslanppReference expL = transL.getReferenceForRValueCode(inGuard, operator, comparingValue);
					AslanppReference expR = transR.getReferenceForRValueCode(inGuard, operator, comparingValue);
					if(originalExpression.getOperator() == IBinaryOpInstruction.Operator.ADD) {
						if(expL.getType().equals(javaType2AslanppType(theApp, "int"))) {
							return getIntAdd().getReference(false, new AslanppTuple(expL,expR));
						}
					}
					throw new RuntimeException("Do not know to to convert binary operation " + originalExpression);
				}
			};
		}

		public Object visitProgramRelationalExpression(ProgramRelationalExpression expression) {
			final TranslatedExpression transL = (TranslatedExpression) expression.getLeft().accept(this);
			final TranslatedExpression transR;
			if(expression.getRight() != null) {
				transR = (TranslatedExpression) expression.getRight().accept(this);	
			} else {
				if(expression.getConstant() instanceof Double || expression.getConstant() instanceof Float) {
					throw new RuntimeException("Cannot process real constants!");												
				} else if(expression.getConstant().longValue() < 0) {
					throw new RuntimeException("Cannot process negative constants!");					
				} else { 
					transR = (TranslatedExpression) ModelFactory.getInstance().reset().createProgramIndexExpression(expression.getConstant().longValue()).accept(this);
				} 
			}
			return new TranslatedExpression<ProgramRelationalExpression>(expression) {
				
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					List<AslanppStatement> code = transL.getRValueCode(inGuard, originalExpression.getOperator(), transR);
					if(code != null) {
						return code;
					}
					code = super.getRValueCode(inGuard, null, null);
					code.addAll(transL.getRValueCode(inGuard, null, null));
					code.addAll(transR.getRValueCode(inGuard, null, null));
					return code;
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					AslanppReference refL = transL.getReferenceForRValueCode(inGuard, originalExpression.getOperator(), transR);
					if(refL != null) {
						return refL;
					}
					refL = transL.getReferenceForRValueCode(inGuard, null, null);
					if(originalExpression.getOperator() == IConditionalBranchInstruction.Operator.EQ) {
							if(refL.getType().equals(AslanppType.factType) && originalExpression.getRight() instanceof ProgramIndexExpression) {
								if(((ProgramIndexExpression)originalExpression.getRight()).isNumberConstant() && ((ProgramIndexExpression)originalExpression.getRight()).getNumberConstant().doubleValue() == 0) {
									AslanppReference res = AslanppLogicalNegation.createInstance(refL);
									return res;
								} else {
									AslanppStringStatement res = new AslanppStringStatement(refL.toString(), false, AslanppType.factType);					
									return res;
								}
							} else if(refL.getType().getName().equals("int")) {
								AslanppReference refR = transR.getReferenceForRValueCode(inGuard,null,null);							
								AslanppSymbol intEQFact = getIntEQ();
								AslanppTuple contained = new AslanppTuple(refL,refR);
								AslanppSymbolReference factSymbolReference = intEQFact.getReference(false, contained);
								return factSymbolReference;
							} else {
								AslanppReference refR = transR.getReferenceForRValueCode(inGuard, null, null);
								AslanppEquality res = new AslanppEquality(refL,refR);
								return res;								
							}
					} else if(originalExpression.getOperator() == IConditionalBranchInstruction.Operator.NE) {
							if(refL.getType().equals(AslanppType.factType) && originalExpression.getRight() instanceof ProgramIndexExpression) {
								if(((ProgramIndexExpression)originalExpression.getRight()).isNumberConstant() && ((ProgramIndexExpression)originalExpression.getRight()).getNumberConstant().doubleValue() == 0) {
									AslanppStringStatement res = new AslanppStringStatement(refL.toString(), false, AslanppType.factType);
									return res;
								} else {
									AslanppReference res = AslanppLogicalNegation.createInstance(refL);					
									return res;
								}
							} else if(refL.getType().getName().equals("int")) {
								AslanppReference refR = transR.getReferenceForRValueCode(inGuard,null,null);							
								AslanppSymbol intEQFact = getIntEQ();
								AslanppTuple contained = new AslanppTuple(refL,refR);
								AslanppReference factSymbolReference = AslanppLogicalNegation.createInstance(intEQFact.getReference(false, contained));
								return factSymbolReference;
							} else {
								AslanppReference refR = transR.getReferenceForRValueCode(inGuard,null,null);
								AslanppInequality res = new AslanppInequality(refL, refR);
								return res;								
							}
					} else if(originalExpression.getOperator() == IConditionalBranchInstruction.Operator.GE) {
							AslanppReference refR = transR.getReferenceForRValueCode(inGuard,null,null);							
							if(refL.getType().getName().equals("int") && refR.getType().getName().equals("int")) {				
								AslanppSymbol intGEFact = getIntGE();
								AslanppTuple contained = new AslanppTuple(refL,refR);
								AslanppSymbolReference factSymbolReference = intGEFact.getReference(false, contained);
								return factSymbolReference;
							}
					} else if(originalExpression.getOperator() == IConditionalBranchInstruction.Operator.LE) {
						AslanppReference refR = transR.getReferenceForRValueCode(inGuard,null,null);							
						if(refL.getType().getName().equals("int") && refR.getType().getName().equals("int")) {				
							AslanppSymbol intGEFact = getIntGE();
							AslanppSymbol intEQFact = getIntEQ();
							AslanppTuple contained = new AslanppTuple(refL,refR);
							AslanppReference intSmaller = AslanppLogicalNegation.createInstance(intGEFact.getReference(false, contained));
							contained = new AslanppTuple(refL,refR);
							AslanppReference intEqual = intEQFact.getReference(false, contained);
							return AslanppLogicalDisjunction.createInstance(intSmaller, intEqual);
						}
					} else if(originalExpression.getOperator() == IConditionalBranchInstruction.Operator.GT) {
						AslanppReference refR = transR.getReferenceForRValueCode(inGuard,null,null);							
						if(refL.getType().getName().equals("int") && refR.getType().getName().equals("int")) {				
							AslanppSymbol intGEFact = getIntGE();
							AslanppSymbol intEQFact = getIntEQ();
							AslanppTuple contained = new AslanppTuple(refL,refR);
							AslanppReference greaterOrEqual = intGEFact.getReference(false, contained);
							contained = new AslanppTuple(refL,refR);
							AslanppReference notEqual = AslanppLogicalNegation.createInstance(intEQFact.getReference(false, contained));
							return AslanppLogicalConjunction.createInstance(greaterOrEqual, notEqual);
						}					
					} else if(originalExpression.getOperator() == IConditionalBranchInstruction.Operator.LT) {
						AslanppReference refR = transR.getReferenceForRValueCode(inGuard,null,null);					
						if(refL.getType().getName().equals("int") && refR.getType().getName().equals("int")) {				
							AslanppSymbol intGEFact = getIntGE();
							AslanppTuple contained = new AslanppTuple(refL,refR);
							AslanppReference greaterOrEqual = intGEFact.getReference(false, contained);
							return AslanppLogicalNegation.createInstance(greaterOrEqual);
						}
					}
					throw new RuntimeException("Cannot handle expressions like " + originalExpression);
				}
				 
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to a relational expression!");
				}
				
			};
		}

		public Object visitProgramStringComparison(ProgramStringComparison expression) {
			final TranslatedExpression transL = (TranslatedExpression) expression.getLeft().accept(this);
			final TranslatedExpression transR = (TranslatedExpression) expression.getRight().accept(this);	
			return new TranslatedExpression<ProgramStringComparison>(expression) {
				
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) {
						if(operator == Operator.EQ) {
							if(comparingValue.originalExpression instanceof ProgramIndexExpression) {
								if(((ProgramIndexExpression)comparingValue.originalExpression).isNumberConstant() && ((ProgramIndexExpression)comparingValue.originalExpression).getNumberConstant().doubleValue() == 0) {
									List<AslanppStatement> res =  transL.getRValueCode(inGuard, Operator.NE, transR);
									if(res == null) {
										List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
										code.addAll(transL.getRValueCode(inGuard, null, null));
										code.addAll(transR.getRValueCode(inGuard, null, null));
										return code;
									}
									return res;
								}
							}
						} else if(operator == Operator.NE) {
							if(comparingValue.originalExpression instanceof ProgramIndexExpression) {
								if(((ProgramIndexExpression)comparingValue.originalExpression).isNumberConstant() && ((ProgramIndexExpression)comparingValue.originalExpression).getNumberConstant().doubleValue() == 0) {
									List<AslanppStatement> res = transL.getRValueCode(inGuard, Operator.EQ, transR);
									if(res == null) {
										List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
										code.addAll(transL.getRValueCode(inGuard, null, null));
										code.addAll(transR.getRValueCode(inGuard, null, null));		
										return code;
									}
									return res;
								}
							}
						}
						return null;
					} else {
						List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
						code.addAll(transL.getRValueCode(inGuard, null, null));
						code.addAll(transR.getRValueCode(inGuard, null, null));
						return code;
					}
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) {
						if(operator == Operator.EQ) {
							if(comparingValue.originalExpression instanceof ProgramIndexExpression) {
								if(((ProgramIndexExpression)comparingValue.originalExpression).isNumberConstant() && ((ProgramIndexExpression)comparingValue.originalExpression).getNumberConstant().doubleValue() == 0) {
									AslanppReference res = transL.getReferenceForRValueCode(inGuard, Operator.NE, transR);
									if(res == null) {
										res = new AslanppInequality(transL.getReferenceForRValueCode(inGuard, null, null),transR.getReferenceForRValueCode(inGuard, null, null));
									}
									return res;
								}
							}
						} else if(operator == Operator.NE) {
							if(comparingValue.originalExpression instanceof ProgramIndexExpression) {
								if(((ProgramIndexExpression)comparingValue.originalExpression).isNumberConstant() && ((ProgramIndexExpression)comparingValue.originalExpression).getNumberConstant().doubleValue() == 0) {
									AslanppReference res = transL.getReferenceForRValueCode(inGuard, Operator.EQ , transR);
									if(res == null) {
										res = new AslanppEquality(transL.getReferenceForRValueCode(inGuard, null, null),transR.getReferenceForRValueCode(inGuard, null, null));										
									}
									return res;
								}
							}
						}
						throw new RuntimeException("Unknows string comparison " + originalExpression);
					} else {
						throw new RuntimeException("Unknows string comparison " + originalExpression);
					}
				}

				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to a String operator!");
				}

			};
		}

		public Object visitProgramIndexExpression(ProgramIndexExpression expression) {
			return new TranslatedExpression<ProgramIndexExpression>(expression) {
	
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					return new ArrayList<AslanppStatement>();
				}

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					if(originalExpression.isNumberConstant()) {
						if(originalExpression.isIntConstant() || originalExpression.isLongConstant()) {
							AslanppType intType = theApp.searchOrAddType("int", AslanppType.messageType, false);
							AslanppReference theIntConstant = theApp.searchOrAddSymbol(true, false, eliminateAnslanppTokens("i" + (originalExpression.isIntConstant() ? originalExpression.getIntConstant() : originalExpression.getLongConstant())), "",intType).getReference(false,null);
							long theConstantValue = originalExpression.isIntConstant() ? originalExpression.getIntConstant() : originalExpression.getLongConstant();
							addIntegerConstant(theConstantValue);
							return theIntConstant;
						} else {
							throw new RuntimeException("Cannot process real constants!");							
						}
					}
					if(originalExpression.isStringConstant()) {
						if(originalExpression.getStringConstant().equals("\"\"")) {
							return theApp.searchOrAddSymbol(true, false, EMPTY_STRING_NAME, "", JAVA_REFERENCE_ASLANPP).getReference(false, null);							
						} else {
							return theApp.searchOrAddSymbol(true, false, eliminateAnslanppTokens(CONSTANT_STRING_PREFIX + originalExpression.getStringConstant().substring(1, originalExpression.getStringConstant().length() - 1)), "", JAVA_REFERENCE_ASLANPP).getReference(false, null);
						}
					}
					if(originalExpression.isNullConstant()) {
						return getNullReference();
					}
					AslanppType theType = javaType2AslanppType(theApp, originalExpression.getDeclaredTypeName());
					if(theType.equals("fact")) {
						//Variables cannot be facts
						theType = AslanppType.natType;
					}
					return theApp.searchOrAddSymbol(false, true, eliminateAnslanppTokens(PREFIX + originalExpression.toString()), "", theType).getReference(false, null);
				}
				
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					if(originalExpression.isNullConstant() || originalExpression.isStringConstant() || originalExpression.isNumberConstant()) {
						throw new RuntimeException("Cannot assign to a constant!");
					} else {
						List<AslanppStatement> theCode = assignedExpression.collectAssignmentData(originalExpression);
						if(theCode == null) {
							AslanppReference assignedReference = assignedExpression.getReferenceForRValueCode(false, null, null);
							AslanppType theType = assignedReference.getType();
							theCode = super.getRValueCode(false, null, null);
							theCode.addAll(assignedExpression.getRValueCode(false, null, null));
							theCode.add(new AslanppAssignment(theApp.searchOrAddSymbol(false, true, eliminateAnslanppTokens(PREFIX + originalExpression.toString()), "", theType).getReference(false, null), assignedReference));
						}
						return theCode;
					}
				}
				
			};
		}

		public Object visitProgramField(ProgramField expression) {

			if(expression.isStatic()) {

				return new TranslatedExpression<ProgramField>(expression) {
					
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						return super.getRValueCode(inGuard, null, null);
					}
					
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						String name = eliminateAnslanppTokens(PREFIX + originalExpression.getDeclaringTypeName() + "/" + originalExpression.getFieldName());
						AslanppSymbolReference fieldSymbol = theApp.searchOrAddSymbol(false, false, name, "", javaType2AslanppType(theApp, originalExpression.getDeclaredTypeName())).getReference(false, null);
						return fieldSymbol;
					}
										
					@Override
					public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
						String name = eliminateAnslanppTokens(PREFIX + originalExpression.getDeclaringTypeName() + "/" + originalExpression.getFieldName());
						AslanppSymbolReference fieldSymbol = theApp.searchOrAddSymbol(false, false, name, "", javaType2AslanppType(theApp, originalExpression.getDeclaredTypeName())).getReference(false, null);
						List<AslanppStatement> theCode = super.getRValueCode(false, null, null);
						theCode.addAll(assignedExpression.getRValueCode(false, null, null));
						theCode.add(new AslanppAssignment(fieldSymbol, assignedExpression.getReferenceForRValueCode(false, null, null)));
						return theCode;					
					}
					
				};
			} else {
				final TranslatedExpression theObjRef = (TranslatedExpression) expression.getObjectExpression().accept(this);
				return new TranslatedExpression<ProgramField>(expression) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
						code.addAll(theObjRef.getRValueCode(inGuard, operator, comparingValue));
						if(originalExpression.getObjectExpression() instanceof ProgramGetObject) {
							return code;							
						} else if(originalExpression.getObjectExpression() instanceof ProgramSessionAttribute) {
							ProgramSessionAttribute theObjExpression = (ProgramSessionAttribute) originalExpression.getObjectExpression();
							Set<IClass> possibleClasses = theObjExpression.getPossibleClasses();
							if(possibleClasses.size() != 1) {
								throw new RuntimeException("Cannot handle casses in which a session attributes can have 0 or more than one runtime types!");
							}
							IClass theClass = possibleClasses.iterator().next();
							String theClassName = theClass.getName().toString();
							List<IField> fieldList = ProgramGetObject.getApplicationObjectFields(theClass);
							AslanppReference fieldsTuple[] = new AslanppReference[fieldList.size() + 1];
							AslanppType[] fieldsTypes = new AslanppType[fieldList.size() + 1];
							fieldsTypes[0] = AslanppType.natType;
							fieldsTuple[0] = theObjRef.getReferenceForRValueCode(true, null, null);
 							for(int i = 0; i < fieldsTuple.length - 1; i++) {
								String aFieldName = eliminateAnslanppTokens(theClassName + "." + fieldList.get(i).getName().toString());
								String aFieldTypeName = Utils.getFullQualifiedTypeNameFromBinaryName(fieldList.get(i).getFieldTypeReference().getName().toString());
								fieldsTuple[i+1] = theApp.searchOrAddSymbol(false, true, aFieldName, "", javaType2AslanppType(theApp, aFieldTypeName)).getReference(true, null);
								fieldsTypes[i+1] = javaType2AslanppType(theApp, aFieldTypeName);
							}
							AslanppSymbol theTypeHeap = 
									theApp.searchOrAddSymbol(false, false, eliminateAnslanppTokens(theClassName + "Objects"), 
									"", AslanppType.getSetType(AslanppType.getTupleType(fieldsTypes)));
							AslanppIf existanceIf = new AslanppIf(new AslanppContains(theTypeHeap.getReference(false, null), new AslanppTuple(fieldsTuple), true));
							code.add(existanceIf);
							return code;
						} else if(originalExpression.getObjectExpression() instanceof ProgramIndexExpressionVariable) {
							ProgramIndexExpressionVariable theObjExp = (ProgramIndexExpressionVariable) originalExpression.getObjectExpression();
							IClass theClass = theObjExp.getDeclaredClass();
							String theClassName = theClass.getName().toString();
							List<IField> fieldList = ProgramGetObject.getApplicationObjectFields(theClass);
							AslanppReference fieldsTuple[] = new AslanppReference[fieldList.size() + 1];
							AslanppType[] fieldsTypes = new AslanppType[fieldList.size() + 1];
							fieldsTypes[0] = AslanppType.natType;
							fieldsTuple[0] = theObjRef.getReferenceForRValueCode(true, null, null);
 							for(int i = 0; i < fieldsTuple.length - 1; i++) {
								String aFieldName = eliminateAnslanppTokens(theClassName + "." + fieldList.get(i).getName().toString());
								String aFieldTypeName = Utils.getFullQualifiedTypeNameFromBinaryName(fieldList.get(i).getFieldTypeReference().getName().toString());
								fieldsTuple[i+1] = theApp.searchOrAddSymbol(false, true, aFieldName, "", javaType2AslanppType(theApp, aFieldTypeName)).getReference(true, null);
								fieldsTypes[i+1] = javaType2AslanppType(theApp, aFieldTypeName);
							}
							AslanppSymbol theTypeHeap = 
									theApp.searchOrAddSymbol(false, false, eliminateAnslanppTokens(theClassName + "Objects"), 
									"", AslanppType.getSetType(AslanppType.getTupleType(fieldsTypes)));
							AslanppIf existanceIf = new AslanppIf(new AslanppContains(theTypeHeap.getReference(false, null), new AslanppTuple(fieldsTuple), true));
							code.add(existanceIf);
							return code;
						}
						return null;
					}	
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						if(originalExpression.getObjectExpression() instanceof ProgramGetObject) {
							String fieldName = eliminateAnslanppTokens(((ProgramGetObject)originalExpression.getObjectExpression()).getDeclaredClass().getName().toString() + "." + originalExpression.getFieldName());
							String typeName = Utils.getFullQualifiedTypeNameFromBinaryName(originalExpression.getField().getFieldTypeReference().getName().toString());
							return theApp.searchOrAddSymbol(false, true, fieldName, "", javaType2AslanppType(theApp, typeName)).getReference(false, null);
						} else if(originalExpression.getObjectExpression() instanceof ProgramSessionAttribute) {
							ProgramSessionAttribute theObjExpression = (ProgramSessionAttribute) originalExpression.getObjectExpression();
							Set<IClass> possibleClasses = theObjExpression.getPossibleClasses();
							if(possibleClasses.size() != 1) {
								throw new RuntimeException("Cannot handle casses in which a session attributes can have 0 or more than one runtime types!");
							}
							String theClassName = possibleClasses.iterator().next().getName().toString();
							String fieldName = eliminateAnslanppTokens(theClassName + "." + originalExpression.getFieldName());
							String typeName = Utils.getFullQualifiedTypeNameFromBinaryName(originalExpression.getField().getFieldTypeReference().getName().toString());											
							return theApp.searchOrAddSymbol(false, true, fieldName, "", javaType2AslanppType(theApp, typeName)).getReference(false, null);
						} else if(originalExpression.getObjectExpression() instanceof ProgramIndexExpressionVariable) {
							ProgramIndexExpressionVariable theObjExp = (ProgramIndexExpressionVariable) originalExpression.getObjectExpression();
							IClass theClass = theObjExp.getDeclaredClass();
							String theClassName = theClass.getName().toString();
							String fieldName = eliminateAnslanppTokens(theClassName + "." + originalExpression.getFieldName());
							String typeName = Utils.getFullQualifiedTypeNameFromBinaryName(originalExpression.getField().getFieldTypeReference().getName().toString());											
							return theApp.searchOrAddSymbol(false, true, fieldName, "", javaType2AslanppType(theApp, typeName)).getReference(false, null);
						}
						return null;
					}
					@Override
					public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
						List<AslanppStatement> code = super.getLValueCode(assignedExpression);
						code.addAll(theObjRef.getRValueCode(false, null, null));
						code.addAll(assignedExpression.getRValueCode(false, null, null));
						if(originalExpression.getObjectExpression() instanceof ProgramGetObject) {
							String name = eliminateAnslanppTokens(((ProgramGetObject)originalExpression.getObjectExpression()).getDeclaredClass().getName().toString() + "." + originalExpression.getFieldName());
							String typeName = Utils.getFullQualifiedTypeNameFromBinaryName(originalExpression.getField().getFieldTypeReference().getName().toString());
							code.add(new AslanppAssignment(
									theApp.searchOrAddSymbol(false, true, name, "", javaType2AslanppType(theApp, typeName)).getReference(false, null),
									assignedExpression.getReferenceForRValueCode(false, null, null)
							));
							List<IField> fieldList = ((ProgramGetObject)originalExpression.getObjectExpression()).getApplicationObjectFields();
							AslanppReference removeTuple[] = new AslanppReference[fieldList.size() + 1];
							AslanppReference addedTuple[] = new AslanppReference[fieldList.size() + 1];
							AslanppType[] fTypes = new AslanppType[fieldList.size() + 1];
							removeTuple[0] = addedTuple[0] = theObjRef.getReferenceForRValueCode(false, null, null);
							fTypes[0] = AslanppType.natType;
							for(int i = 0; i < removeTuple.length - 1; i++) {
								name = eliminateAnslanppTokens(((ProgramGetObject)originalExpression.getObjectExpression()).getDeclaredClass().getName().toString() + "." + fieldList.get(i).getName().toString());
								typeName = Utils.getFullQualifiedTypeNameFromBinaryName(fieldList.get(i).getFieldTypeReference().getName().toString());
								addedTuple[i+1] = theApp.searchOrAddSymbol(false, true, name, "", javaType2AslanppType(theApp, typeName)).getReference(false, null);
								removeTuple[i+1] = new AslanppStringStatement("?",false);
								fTypes[i+1] = javaType2AslanppType(theApp, typeName);
							}
							AslanppSymbol theTypeHeap = 
									theApp.searchOrAddSymbol(false, false, eliminateAnslanppTokens(((ProgramGetObject)originalExpression.getObjectExpression()).getDeclaredClass().getName().toString()+"Objects"), 
									"", AslanppType.getSetType(AslanppType.getTupleType(fTypes)));
							code.add(new AslanppRemove(theTypeHeap.getReference(false, null),new AslanppTuple(removeTuple)));
							code.add(new AslanppContains(theTypeHeap.getReference(false, null),new AslanppTuple(addedTuple),false));
						}
						return code;					
					}

				};
			}
		}
		
		public Object visitProgramGetObject(ProgramGetObject expression) {
			final TranslatedExpression objExp = (TranslatedExpression) expression.getObjectIDExpression().accept(this);
			final List<IField> fieldList = expression.getApplicationObjectFields();
			final List<AslanppSymbol> var4Fields = new ArrayList<AslanppSymbol>();
			AslanppType[] fTypes = new AslanppType[fieldList.size()];
			int i = 0;
			for(IField aField : fieldList) {
				String typeName = Utils.getFullQualifiedTypeNameFromBinaryName(aField.getFieldTypeReference().getName().toString());
				fTypes[i++] = javaType2AslanppType(theApp, typeName);
				var4Fields.add(theApp.searchOrAddSymbol(false, true, eliminateAnslanppTokens(expression.getDeclaredClass().getName().toString() + "." + aField.getName()), "", fTypes[i-1]));
			}
			AslanppType finalTuple[] = new AslanppType[2];
			finalTuple[0] = AslanppType.natType;
			finalTuple[1] = AslanppType.getTupleType(fTypes);
			final AslanppSymbol theTypeHeap = theApp.searchOrAddSymbol(
					false, false, eliminateAnslanppTokens(expression.getDeclaredClass().getName().toString()+"Objects"), 
					"", AslanppType.getSetType(AslanppType.getTupleType(finalTuple)));
			return new TranslatedExpression<ProgramGetObject>(expression) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator!=null) return null;
					List<AslanppStatement> code = super.getRValueCode(inGuard, operator, comparingValue);
					code.addAll(objExp.getRValueCode(inGuard, operator, comparingValue));
					AslanppReference existanceTupleArray[] = new AslanppReference[fieldList.size() + 1];
					existanceTupleArray[0] = objExp.getReferenceForRValueCode(inGuard, operator, comparingValue);
					for(int i = 1; i < existanceTupleArray.length; i++) {
						existanceTupleArray[i] = var4Fields.get(i-1).getReference(true, null);
					}
					AslanppTuple existanceTuple = new AslanppTuple(existanceTupleArray);
					AslanppContains checkExistance = new AslanppContains(
							theTypeHeap.getReference(false, null),
							existanceTuple,
							true
					);
					AslanppIf theExistanceIf = new AslanppIf(AslanppLogicalNegation.createInstance(checkExistance));
					AslanppReference addedTupleArray[] = new AslanppReference[fieldList.size() + 1];
					addedTupleArray[0] = objExp.getReferenceForRValueCode(inGuard, operator, comparingValue);
					for(int i = 1; i < addedTupleArray.length; i++) {
						addedTupleArray[i] = new AslanppStringStatement("?",false);
					}
					AslanppTuple addedTuple = new AslanppTuple(addedTupleArray);
					AslanppContains objCreation = new AslanppContains(
							theTypeHeap.getReference(false, null),
							addedTuple,
							false
					);
					theExistanceIf.addStatementThen(objCreation);
					code.add(theExistanceIf);
					return code;
				}
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator!=null) return null;
					return objExp.getReferenceForRValueCode(inGuard, operator, comparingValue);
				}
			};
		}
				
		public Object visitProgramFunction(ProgramFunction expression) {
			final List<TranslatedExpression> tArgs = new ArrayList<TranslatedExpression>();
			for(ProgramExpression anArg : expression.getArguments()) {
				tArgs.add((TranslatedExpression)anArg.accept(this));
			}
			return new TranslatedExpression<ProgramFunction>(expression) {
				
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
					for(TranslatedExpression anArg : tArgs) {
						code.addAll(anArg.getRValueCode(inGuard, null, null));
					}
					return code;
				}	

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					String name = PREFIX + originalExpression.getFunctionSignature();
					name = eliminateAnslanppTokens(name);
					List<AslanppReference> argRefs = new ArrayList<AslanppReference>();
					String structure = "";
					for(TranslatedExpression anArg : tArgs) {
						AslanppReference aRef = anArg.getReferenceForRValueCode(inGuard, null, null);
						argRefs.add(aRef);
						structure += aRef.getType().getName() + ",";
					}
					if(!structure.equals("")) {
						structure = structure.substring(0, structure.length() - 1);
					}
					AslanppType theType = javaType2AslanppType(theApp, originalExpression.getReturnTypeName());
					AslanppTuple contained = new AslanppTuple(argRefs.toArray(new AslanppReference[] {}));
					AslanppSymbolReference functionSymbol = theApp.searchOrAddSymbol(true, false, name, structure, theType).getReference(false, contained);
					return functionSymbol;
				}
				
				
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to a function!");
				}

			};
		}

		public Object visitProgramStringBinaryExpression(ProgramStringBinaryExpression expression) {
			final TranslatedExpression transL = (TranslatedExpression) expression.getLeft().accept(this);
			final TranslatedExpression transR = (TranslatedExpression) expression.getRight().accept(this);	
			return new TranslatedExpression<ProgramStringBinaryExpression>(expression) {
				
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
					code.addAll(transL.getRValueCode(inGuard, null, null));
					code.addAll(transR.getRValueCode(inGuard, null, null));					
					return code;
				}	

				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					return new AslanppStringStatement(transL.getReferenceForRValueCode(inGuard, null, null) + "." + transR.getReferenceForRValueCode(inGuard, null, null), false, JAVA_REFERENCE_ASLANPP);
				}
								
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to a string operation!");
				}

			};
		}

		public Object visitProgramConversionExpression(ProgramConversionExpression expression) {
			final TranslatedExpression trans = (TranslatedExpression) expression.getConvertedExpression().accept(this);
			return new TranslatedExpression<ProgramConversionExpression>(expression) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> code = super.getRValueCode(inGuard, null, null);
					code.addAll(trans.getRValueCode(inGuard, null, null));					
					return code;
				}	
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					String name = PREFIX + originalExpression.getOperation().name().toLowerCase();
					name = eliminateAnslanppTokens(name);
					List<AslanppReference> argRefs = new ArrayList<AslanppReference>();
					argRefs.add(trans.getReferenceForRValueCode(inGuard, null, null));
					String structure;
					AslanppType theType;
					if(originalExpression.getOperation() == ConversionOperation.INT2STRING) {
						structure= javaType2AslanppType(theApp, "int").getName();
						theType = javaType2AslanppType(theApp, "java.lang.String");
						int2string = true;
					} else if(originalExpression.getOperation() == ConversionOperation.STRING2INT) {
						structure= javaType2AslanppType(theApp, "java.lang.String").getName();
						theType = javaType2AslanppType(theApp, "int");
						string2int = true;
					} else if(originalExpression.getOperation() == ConversionOperation.CAST2INT) {
						return trans.getReferenceForRValueCode(inGuard, operator, comparingValue);
					} else {
						throw new RuntimeException("Cannot handle expressions like " + originalExpression);
					}
					AslanppTuple contained = new AslanppTuple(argRefs.toArray(new AslanppReference[] {}));
					AslanppSymbolReference functionSymbol = theApp.searchOrAddSymbol(true, false, name, structure, theType).getReference(false, contained);
					return functionSymbol;
				}
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					throw new RuntimeException("Cannot assign to a string operation!");
				}
			};
		}
		
		@Override
		public Object visitProgramUniqueInstanceCounter(ProgramUniqueInstanceCounter expression) {
			return new TranslatedExpression<ProgramUniqueInstanceCounter>(expression) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					return super.getRValueCode(inGuard, null, null);
				}	
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					return theApp.searchOrAddSymbol(false, false, "uniqueInstanceCounter","", AslanppType.natType).getReference(false, null);
				}
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					List<AslanppStatement> code = super.getLValueCode(assignedExpression);
					code.addAll(assignedExpression.getRValueCode(false, null, null));
					AslanppReference assignTo = theApp.searchOrAddSymbol(false, false, "uniqueInstanceCounter","", AslanppType.natType).getReference(false, null);
					code.add(new AslanppAssignment(assignTo, assignedExpression.getReferenceForRValueCode(false, null, null)));
					return code;
				}
				
			};
		}
		
		public Object visitProgramUniqueInstanceCounterAction(ProgramUniqueInstanceCounterAction expression) {
			if(expression.getAction() == ProgramUniqueInstanceCounterAction.Action.INIT) {
				return new TranslatedExpression<ProgramUniqueInstanceCounterAction>(expression) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						return super.getRValueCode(inGuard, null, null);
					}	
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						return new AslanppStringStatement("1",false, AslanppType.natType);
					}
					
				};
			} else {
				final TranslatedExpression cc = (TranslatedExpression) expression.getCurrentCounter().accept(this);
				return new TranslatedExpression<ProgramUniqueInstanceCounterAction>(expression) {
					@Override
					public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						if(operator != null) return null;
						return cc.getRValueCode(inGuard, operator, comparingValue);
					}	
					@Override
					public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
						return new AslanppStringStatement("succ("+ cc.getReferenceForRValueCode(inGuard, operator, comparingValue) + ")",false, AslanppType.natType);
					}
				};				
			}
		}

		@Override
		public Object visitProgramDbRValue(ProgramDbRValue expression) {
			if(facts != null) {
				for(Fact f : facts) {
					if(f instanceof AvailableCursorFact) {
						if(((AvailableCursorFact) f).getProgramExpression().structuralEquals(expression)) {
							final AvailableCursorFact ff = (AvailableCursorFact) f;
							return new TranslatedExpression<ProgramDbRValue>(expression) {
								@Override
								public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
									return ff.getTranslatedExpression().getReferenceForRValueCode(inGuard, operator, comparingValue);
								}
							};
						}
					}
				}
			}
			TranslatedExpression te = sqlConstrunctionGenerator.translate(expression);
			return te;
		}
		
		@Override
		public Object visitProgramSQLCursorAction(ProgramSQLCursorAction expression) {
			//Check for available cursor
			if(facts != null) {
				for(Fact f : facts) {
					if(f instanceof AvailableCursorFact) {
						if(((AvailableCursorFact) f).getProgramExpression().structuralEquals(expression)) {
							final AvailableCursorFact ff = (AvailableCursorFact) f;
							return new TranslatedExpression<ProgramSQLCursorAction>(expression) {
								@Override
								public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
									return ff.getTranslatedExpression().getReferenceForRValueCode(inGuard, operator, comparingValue);
								}
								@Override
								public List<AslanppStatement> collectAssignmentData(ProgramIndexExpression exp) {
									return new ArrayList<AslanppStatement>();
								}
							};
						}
					}
				}
			}
			TranslatedExpression res = sqlConstrunctionGenerator.translate(expression);
			if(facts != null) {
				AvailableCursorFact gen = new AvailableCursorFact(expression, res);			
				if(expression.getFunction() != ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_CREATE) {
					HashSet<Fact> killed = new HashSet<Fact>();
					for(Fact f : facts) {
						if(f instanceof AvailableCursorFact) {
							if(((AvailableCursorFact) f).getProgramExpression().structuralEquals(expression.getArgument())) {
								if(expression.getFunction() == ProgramSQLCursorAction.SQL_CURSOR_ACTION.CURSOR_HAS_NEXT) {
									gen.setHasNextAppliedOn(((AvailableCursorFact) f).getTranslatedExpression());
								}
								killed.add(f);
							}
						}
					}
					facts.removeAll(killed);
				}
				facts.add(gen);
			}
			return res;
		}

		@Override
		public Object visitProgramDataBaseModification(ProgramDataBaseModification expr) {
			return sqlConstrunctionGenerator.translate(expr);
		}

		@Override
		public Object visitProgramDataBase(ProgramDataBase db) {
			return sqlConstrunctionGenerator.translate(db);
		}

		private static String COLLECTION_HEAP_SYMBOL_NAME = "theCollectionHeap";
		private static String A_COLLECTION_SYMBOL_NAME = "aCollectionReference";

		@Override
		public Object visitProgramCollections(ProgramCollections c) {
			return new TranslatedExpression<ProgramCollections>(c) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					return super.getRValueCode(inGuard, operator, comparingValue);
				}
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					throw new RuntimeException("Should not happen: Reference to a ProgramCollections");
				}
				@Override
				public List<AslanppStatement> getLValueCode(TranslatedExpression assignedExpression) {
					return assignedExpression.getRValueCode(false, null, null);
				}
			};
		}
		
		@Override
		public Object visitProgramCollectionGet(ProgramCollectionGet cg) {
			final TranslatedExpression id = (TranslatedExpression) cg.getCollectionIdExpression().accept(this);
			return new TranslatedExpression<ProgramCollectionGet>(cg) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> code = super.getRValueCode(inGuard, operator, comparingValue);
					code.addAll(id.getRValueCode(inGuard, operator, comparingValue));
					AslanppSymbol theCollectionHeap = theApp.searchOrAddSymbol(false, false, COLLECTION_HEAP_SYMBOL_NAME, "", AslanppType.getSetType(AslanppType.getTupleType(AslanppType.natType,AslanppType.getSetType(AslanppType.messageType))));
					AslanppContains checkExistance = new AslanppContains(theCollectionHeap.getReference(false, null), new AslanppTuple(id.getReferenceForRValueCode(inGuard, operator, comparingValue), new AslanppStringStatement("?",false)), true);
					AslanppIf theExistanceIf = new AslanppIf(AslanppLogicalNegation.createInstance(checkExistance));					
					AslanppContains collectionCreation 
						= new AslanppContains(theCollectionHeap.getReference(false, null),
								new AslanppTuple(id.getReferenceForRValueCode(inGuard, operator, comparingValue), 
										new AslanppEmptySetInstantiation()),false);
					theExistanceIf.addStatementThen(collectionCreation);
					code.add(theExistanceIf);
					return code;
				}
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					return id.getReferenceForRValueCode(inGuard, operator, comparingValue);
				}
			};
		}

		@Override
		public Object visitProgramCollectionAction(ProgramCollectionAction cg) {
			final TranslatedExpression id = (TranslatedExpression) cg.getCollectionIdExpression().accept(this);
			final TranslatedExpression value = (TranslatedExpression) cg.getValue().accept(this);
			return new TranslatedExpression<ProgramCollectionAction>(cg) {
				@Override
				public List<AslanppStatement> getRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					List<AslanppStatement> code = super.getRValueCode(inGuard, operator, comparingValue);
					if(originalExpression.getAction() == CollectionActions.ADD) {
						code.addAll(id.getRValueCode(inGuard, operator, comparingValue));
						code.addAll(value.getRValueCode(inGuard, operator, comparingValue));
						AslanppSymbol theCollectionHeap = theApp.searchOrAddSymbol(false, false, COLLECTION_HEAP_SYMBOL_NAME, "", AslanppType.getSetType(AslanppType.getTupleType(AslanppType.natType,AslanppType.getSetType(AslanppType.messageType))));
						AslanppSymbol aCollectionSymbol = theApp.searchOrAddSymbol(false, true, A_COLLECTION_SYMBOL_NAME, "", AslanppType.getSetType(AslanppType.messageType));
						AslanppContains extractionContains = new AslanppContains(theCollectionHeap.getReference(false, null), new AslanppTuple(id.getReferenceForRValueCode(inGuard, operator, comparingValue),aCollectionSymbol.getReference(true, null)),true);						
						AslanppIf theExtractionIf = new AslanppIf(extractionContains);			
						AslanppContains add2Collection = new AslanppContains(aCollectionSymbol.getReference(false, null),value.getReferenceForRValueCode(inGuard, operator, comparingValue),false);
						code.add(theExtractionIf);
						code.add(add2Collection);
					} else {
						throw new RuntimeException("Do not know how to manage " + originalExpression.getAction() + " on collections!");
					}
					return code;
				}
				@Override
				public AslanppReference getReferenceForRValueCode(boolean inGuard, IOperator operator, TranslatedExpression comparingValue) {
					if(operator != null) return null;
					throw new RuntimeException("Should not happen: Reference to a ProgramCollectionAction");				
				}
			};
		}
		
	}
	
	static AslanppType javaType2AslanppType(AslanppEntity entity, String type) {
		if(type.equals("int") || type.equals("long") || type.equals("boolean") || type.equals("java.lang.Boolean")) {
			return entity.searchOrAddType("int", AslanppType.messageType, false);
		}
		if(type.equals("java.lang.String")) {
			return AslanppType.messageType;
		}
		if(type.equals("double") || type.equals("float")) {
			throw new RuntimeException("Unhandled type " + type);
		}
		return AslanppType.natType;
	}

	static String eliminateAnslanppTokens(String token) {
		StringBuilder s = new StringBuilder();
		for(int i = 0; i < token.length(); i++) {
			char ch = token.charAt(i);
			if(Character.isLetterOrDigit(ch) || ch == '_') {
				s=s.append(ch);
			} else {
				s=s.append('_');
			}
		}
		return s.toString();
	}
	
	private static boolean intGEPredicate = false;
	private static boolean intEQPredicate = false;
	private static boolean string2int = false;
	private static boolean int2string = false;
	private static boolean cast2int = false;
	private static boolean intAdd = false;
	private static ArrayList<Long> constantIngetegers = new ArrayList<Long>();
	public static AslanppSymbol addIntegerConstant(Long constant) {
		if(!constantIngetegers.contains(constant)) {
			constantIngetegers.add(constant);
		}
		AslanppType theType = javaType2AslanppType(environmentEntity, "int");
		return environmentEntity.searchOrAddSymbol(true, false, eliminateAnslanppTokens("i"+constant), "", theType);
	}
	public static AslanppType getIntType() {
		return  javaType2AslanppType(environmentEntity, "int");
	}
	public static AslanppSymbol getString2Int() {
		string2int = true;
		String name = PREFIX + "string2int";
		name = eliminateAnslanppTokens(name);
		String structure = javaType2AslanppType(environmentEntity, "java.lang.String").getName();
		AslanppType theType = javaType2AslanppType(environmentEntity, "int");
		return environmentEntity.searchOrAddSymbol(true, false, name, structure, theType);
	}
	public static AslanppSymbol getInt2String() {
		int2string = true;
		String name = PREFIX + "int2string";
		name = eliminateAnslanppTokens(name);
		String structure = javaType2AslanppType(environmentEntity, "int").getName();
		AslanppType theType = javaType2AslanppType(environmentEntity, "java.lang.String");
		return environmentEntity.searchOrAddSymbol(true, false, name, structure, theType);
	}
	public static AslanppSymbol getCast2Int() {
		cast2int = true;
		String name = PREFIX + "cast2string";
		name = eliminateAnslanppTokens(name);
		String structure= AslanppType.messageType.getName();
		AslanppType theType = javaType2AslanppType(environmentEntity, "int");
		return environmentEntity.searchOrAddSymbol(true, false, name, structure, theType);
	}	
	public static AslanppSymbol getIntAdd() {
		intAdd = true;
		String name = PREFIX + "intAdd";
		name = eliminateAnslanppTokens(name);
		String structure= "int,int";
		AslanppType theType = javaType2AslanppType(environmentEntity, "int");
		return environmentEntity.searchOrAddSymbol(true, false, name, structure, theType);
	}
	public static AslanppSymbol getIntGE() {
		intGEPredicate = true;
		String name = PREFIX + "intGE";
		name = eliminateAnslanppTokens(name);
		String structure= "int,int";
		return environmentEntity.searchOrAddSymbol(true, false, name, structure, AslanppType.factType);
	}
	public static AslanppSymbol getIntEQ() {
		intEQPredicate = true;
		String name = PREFIX + "intEQ";
		name = eliminateAnslanppTokens(name);
		String structure= "int,int";
		AslanppType theType = javaType2AslanppType(environmentEntity, "int");
		return environmentEntity.searchOrAddSymbol(true, false, name, structure, AslanppType.factType);
	}
	private static void initIntegerTheory() {
		intGEPredicate = false;
		intEQPredicate = false;
		string2int = false;
		int2string = false;
		cast2int = false;
		intAdd = false;
		constantIngetegers.clear();
	}
	private static void completeIntegerTheory(AslanppEntity env) {
		int clauseCounter = 0;
		if(intGEPredicate || intEQPredicate) {
			//int comparisons are used then check for special function usage
			String specialEqualityName = PREFIX + "specialEQ";
			if(string2int || int2string) {
				//Since conversion functions are used we need to define some special equalities
				//At this moment we cannot declare them reflexive :(
				String structure = AslanppType.messageType.getName() + "," + AslanppType.messageType.getName();
				AslanppSymbol functionSymbol = env.searchOrAddSymbol(true, false, specialEqualityName, structure, AslanppType.factType);				
				//Ensure addition of both conversion function
				AslanppSymbol f1 = getString2Int();
				AslanppSymbol f2 = getInt2String();
				env.addClause(new AslanppStringStatement(
						specialEqualityName+"_"+clauseCounter+"(X) : "+specialEqualityName+"("+f1.getName()+"("+f2.getName()+"(X))"+",X)"
						,true));				
				clauseCounter++;				
				if(intGEPredicate) {
					env.addClause(new AslanppStringStatement(
						"rintGE_"+clauseCounter+"(X,Y,X1) : rintGE(X,Y) :- rintGE(X1,Y) & " + specialEqualityName + "(X,X1)"
						,true));
					clauseCounter++;				
				}
				if(intEQPredicate) {
					env.addClause(new AslanppStringStatement(
							"rintEQ_"+clauseCounter+"(X,Y) : rintEQ(X,Y) :- " + specialEqualityName + "(X,Y)"
							,true));
						clauseCounter++;					
				}
			}
			if(cast2int) {
				//Ensure addition of conversion function
				AslanppSymbol f = getCast2Int();
				env.addClause(new AslanppStringStatement(
					specialEqualityName+"_"+clauseCounter+"(X) : " + specialEqualityName + "(" + f.getName() + "(X)"+",X)",
					true));
				clauseCounter++;				
			}
			if(intAdd) {
				if(intGEPredicate) {
					env.addClause(new AslanppStringStatement(
						"rintGE_"+clauseCounter+"(X,Y,Z) : rintGE(Z,X) :- rintAdd(X,Y) = Z",true));
					clauseCounter++;
					env.addClause(new AslanppStringStatement(
						"rintGE_"+clauseCounter+"(X,Y,Z) : rintGE(Z,Y) :- rintAdd(X,Y) = Z",true));
					clauseCounter++;
				}
				if(intEQPredicate) {
					addIntegerConstant(1l);
					env.addClause(new AslanppStringStatement(
						"rintEQ_"+clauseCounter+"(X,Y,Z) : rintEQ(X,Z) :- rintAdd(X,Y) = Z & rintEQ(Y,i0)",true));
					clauseCounter++;
					env.addClause(new AslanppStringStatement(
						"rintEQ_"+clauseCounter+"(X,Y,Z) : rintEQ(Y,Z) :- rintAdd(X,Y) = Z & rintEQ(X,i0)",true));
					clauseCounter++;
				}
			}
			if(intGEPredicate) {
				//Add clauses for intGE
				for(int i = 0; i < constantIngetegers.size(); i++) {
					for(int j = i + 1; j < constantIngetegers.size(); j++) {
						if(constantIngetegers.get(i) > constantIngetegers.get(j)) {
							env.addClause(
									new AslanppStringStatement(
											"rintGE_"+clauseCounter+":"+"rintGE("+eliminateAnslanppTokens("i"+constantIngetegers.get(i))+","+eliminateAnslanppTokens("i"+constantIngetegers.get(j))+")",true));
							clauseCounter++;
						}
					}
				}
				env.addClause(new AslanppStringStatement("rintGE_"+clauseCounter+"(X) : rintGE(X,X)",true));
				clauseCounter++;
			}
			if(intEQPredicate) {
				//Add clauses for intEQ
				env.addClause(new AslanppStringStatement("rintEQ_"+clauseCounter+"(X) : rintEQ(X,X)",true));
				clauseCounter++;
			}
		}
	}

}
