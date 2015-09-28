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
import java.io.IOException;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import com.ibm.wala.cast.java.ipa.callgraph.JavaSourceAnalysisScope;
import com.ibm.wala.ide.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.config.FileOfClasses;

public class WalaSSARepresentationBuilder {

	private static WalaSSARepresentationBuilder instance = null;

	public static WalaSSARepresentationBuilder getWalaBuilder() {
		return instance == null ? instance = new WalaSSARepresentationBuilder() : instance;
	}

	private File theFile;	

	private HashMap<IJavaProject,ClassHierarchy> cache = new HashMap<IJavaProject,ClassHierarchy>();
		
	public synchronized void init(String path) {
		this.theFile = new File(path + File.separator + "Java60RegressionExclusions.txt");
	}
	
	public synchronized ClassHierarchy parseProject(IJavaProject proj) throws IOException, CoreException, ClassHierarchyException {
		if(cache.get(proj) == null) {
			EclipseProjectPath eprj = EclipseProjectPath.make(proj);
			AnalysisScope scope = eprj.toAnalysisScope(new JavaSourceAnalysisScope());
			scope.setExclusions(FileOfClasses.createFileOfClasses(theFile));
			ClassHierarchy ch = ClassHierarchy.make(scope);
			cache.put(proj, ch);
			return ch;
		} else {
			return cache.get(proj);
		}
	}

	public void clearProject(IJavaProject prj) {
		cache.remove(prj);
	}

}
