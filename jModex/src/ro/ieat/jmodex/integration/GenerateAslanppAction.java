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

import java.util.Arrays;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import ro.ieat.jmodex.api.jModexFacade;

public class GenerateAslanppAction implements IObjectActionDelegate {

	public GenerateAslanppAction() {}

	@Override
	public void run(IAction action) {
		if (theSelection instanceof IStructuredSelection) {
			if(((IStructuredSelection) theSelection).getFirstElement() instanceof IJavaProject) {
				IJavaProject theProject = (IJavaProject) ((IStructuredSelection) theSelection).getFirstElement();
				final Job jb = jModexFacade.getInstance().inferModel(theProject);
				final Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				final Display disp = Display.getCurrent();
				jb.setUser(true);
				jb.addJobChangeListener(new JobChangeAdapter() {
					public void done(IJobChangeEvent event) {
						if(!event.getResult().isOK()) {
							if(event.getResult().getSeverity() != IStatus.CANCEL) {
								disp.asyncExec(new Runnable() {
									@Override
									public void run() {
										MessageDialog md = new MessageDialog(theShell,"jModex Error", null, jb.getResult().getMessage() + "\n" + jb.getResult().getException().toString() + Arrays.toString(jb.getResult().getException().getStackTrace()), MessageDialog.ERROR, new String[] {"Ok"}, 0);
										md.open();
									}
								});
							}
						}
					}
				});
				jb.schedule();
			}
		}
	}
	
	private ISelection theSelection;
	
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {}

	public void selectionChanged(IAction action, ISelection selection) {
		theSelection = selection;
	}

}
