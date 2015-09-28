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
package ro.ieat.tests.utils;

import java.io.File;
import java.net.URL;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.dialogs.IOverwriteQuery;
import org.eclipse.ui.wizards.datatransfer.ImportOperation;
import org.eclipse.ui.wizards.datatransfer.ZipFileStructureProvider;


public class TestUtil {
	
	
	public static void importProject(String projectName, String fileName) {
		
		try {
		
			URL url = Platform.getBundle(ro.ieat.jmodex.Activator.PLUGIN_ID).getEntry("/");
			url = FileLocator.resolve(url);
			String path = url.getPath() + "res/testdata/";
			ZipFile theFile = new ZipFile(new File(path + fileName));
			ZipFileStructureProvider zp = new ZipFileStructureProvider(theFile);
			
			IWorkspaceRoot workSpaceRoot =  ResourcesPlugin.getWorkspace().getRoot();
			IProject project = workSpaceRoot.getProject(projectName);
			project.create(null);
			project.open(null);
			
			IPath container = workSpaceRoot.getProject(projectName).getFullPath();
		    ImportOperation importOp = new ImportOperation(container, zp.getRoot(), zp, new IOverwriteQuery() {
		      public String queryOverwrite(String pathString) {
		        return IOverwriteQuery.ALL;
		      }
		    });

		    importOp.setCreateContainerStructure(true);
		    importOp.setOverwriteResources(true);
		    importOp.run(null);
		    try {
		    	Platform.getJobManager().join(ResourcesPlugin.FAMILY_AUTO_BUILD,null);
		    } catch(InterruptedException e) {}
		    theFile.close();		
		    
		} catch(Exception e) {
			e.printStackTrace();
	    	throw new RuntimeException(e.getMessage());			
		}
	}
	
	public static void deleteProject(String projectName) {
		try {
			IWorkspaceRoot workSpaceRoot =  ResourcesPlugin.getWorkspace().getRoot();
			IProject project = workSpaceRoot.getProject(projectName);
			project.close(null);
			project.delete(true, null);
	    } catch (CoreException e) {
	    	throw new RuntimeException(e.getMessage());
	    }
	}

	public static IJavaProject getProject(String projectName) {
		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
	    IJavaModel javaModel = JavaCore.create(workspaceRoot);
	    IJavaProject theProject = javaModel.getJavaProject(projectName);
	    return theProject;
	}
	
}
