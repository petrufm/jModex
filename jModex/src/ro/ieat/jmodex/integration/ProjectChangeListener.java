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
package ro.ieat.jmodex.integration;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.jdt.core.ElementChangedEvent;
import org.eclipse.jdt.core.IElementChangedListener;
import org.eclipse.jdt.core.IJavaElementDelta;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import ro.ieat.isummarize.WalaSSARepresentationBuilder;
import ro.ieat.jmodex.Activator;

public class ProjectChangeListener implements IElementChangedListener {

	private static ProjectChangeListener instance = null;
	
	public static ProjectChangeListener getInstance() {
		if(instance == null) {
			instance = new ProjectChangeListener();
			JavaCore.addElementChangedListener(instance, ElementChangedEvent.POST_CHANGE);
			try {
				URL url = Activator.getDefault().getBundle().getEntry(File.separator);
				url = FileLocator.resolve(url);
				WalaSSARepresentationBuilder.getWalaBuilder().init(url.getPath());
			} catch (IOException e) {
				throw new RuntimeException("Init error: can not find the plugin path");
			}
		}
		return instance;
	}
	
	private ProjectChangeListener() {}
	
	private static boolean analyze(IJavaElementDelta delta) {
		boolean res; 
		if((delta.getFlags() & IJavaElementDelta.F_PRIMARY_WORKING_COPY) != 0) {
			res = false;
		} else {
			res = true;
		}
		for(IJavaElementDelta d : delta.getAffectedChildren()) {
			res = res & analyze(d);
		}
		return res;
	}
	
	private void clearProjects(IJavaElementDelta delta) {
		if(delta.getElement() instanceof IJavaProject) {
			boolean t = false;
			for(IJavaElementDelta d : delta.getAffectedChildren()) {
				t = t | analyze(d);
			}
			if(t) {
				WalaSSARepresentationBuilder.getWalaBuilder().clearProject((IJavaProject) delta.getElement());
			}
		}		
		for(IJavaElementDelta d : delta.getAffectedChildren()) {
			clearProjects(d);
		}
	}
	
	public synchronized void elementChanged(ElementChangedEvent event) {
		clearProjects(event.getDelta());
	}

}
