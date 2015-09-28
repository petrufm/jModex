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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.java.client.impl.ZeroCFABuilderFactory;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchy;

public class CallGraphForEclipseJavaProject implements CallGraphBuilder {

	private IJavaProject theProject;
	private TechnologySpecifier specifier;
	private Map<IMethod,PointerAnalysis> pointerResults = null;
	private boolean pointerResultsFor = false;
	
	public CallGraphForEclipseJavaProject(IJavaProject theProject, TechnologySpecifier entryDetector) {
		this.theProject = theProject;
		this.specifier = entryDetector;
	}
	
	@Override
	public Map<IMethod, CallGraph> getCallGraph(boolean inIsolation) throws CallGraphBuildingException {
		ClassHierarchy ch;
		try {
			ch = WalaSSARepresentationBuilder.getWalaBuilder().parseProject(theProject);
			Map<IMethod, CallGraph> result = new HashMap<IMethod, CallGraph>();
			pointerResults = new HashMap<IMethod,PointerAnalysis>();
			if(inIsolation) {
				//Static initializers
				AnalysisCache cache = new AnalysisCache();
				AnalysisOptions options = new AnalysisOptions(ch.getScope(),new ArrayList<Entrypoint>());
				options.setHandleStaticInit(true);
				com.ibm.wala.ipa.callgraph.CallGraphBuilder theBuilder = getCallGraphBuilder(ch, cache, options);
				CallGraph cg = theBuilder.makeCallGraph(options, null);
				result.put(null, cg);
				//For each entry point
				List<Entrypoint> res = specifier.handleGetEntrypoints(ch);
				List<Entrypoint> tmp = new ArrayList<Entrypoint>();
				for(Entrypoint anEntry : res) {
					tmp.clear();
					tmp.add(anEntry);
					cache = new AnalysisCache();
					options = new AnalysisOptions(ch.getScope(),tmp);
					options.setHandleStaticInit(false);
					theBuilder = getCallGraphBuilder(ch, cache, options);
					cg = theBuilder.makeCallGraph(options, null);
					result.put(anEntry.getMethod(), cg);
					pointerResults.put(anEntry.getMethod(),theBuilder.getPointerAnalysis());
				}
			} else {
				List<Entrypoint> res = specifier.handleGetEntrypoints(ch);
				AnalysisCache cache = new AnalysisCache();
				AnalysisOptions options = new AnalysisOptions(ch.getScope(),res);
				options.setHandleStaticInit(true);
				com.ibm.wala.ipa.callgraph.CallGraphBuilder theBuilder = getCallGraphBuilder(ch, cache, options);
				CallGraph cg = theBuilder.makeCallGraph(options, null);
				result.put(null, cg);				
				pointerResults.put(null,theBuilder.getPointerAnalysis());
			}
			pointerResultsFor = inIsolation;
			return result;
		} catch (Exception e) {
			pointerResults = null;
			throw new CallGraphBuildingException(e);
		}
	}

	private com.ibm.wala.ipa.callgraph.CallGraphBuilder getCallGraphBuilder(ClassHierarchy ch, AnalysisCache cache, AnalysisOptions options) {
		return Util.makeRTABuilder(options , cache, ch, ch.getScope());
	}
	
	@Override
	public Map<IMethod, PointerAnalysis> getPointsTo(boolean inIsolation) throws CallGraphBuildingException {
		if(pointerResults == null || inIsolation != pointerResultsFor) {
			getCallGraph(inIsolation);
		}
		return pointerResults;
	}

	@Override
	public TechnologySpecifier getTechnologySpecifier() {
		return specifier;
	}

}
