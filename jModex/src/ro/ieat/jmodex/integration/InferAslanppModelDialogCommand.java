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

import java.util.Collections;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.handlers.HandlerUtil;

import ro.ieat.jmodex.api.jModexFacade;

public class InferAslanppModelDialogCommand extends AbstractHandler {

	private class InferModelDialog extends Dialog {
		
		private boolean run = false;
		private IJavaProject selectedProject = null;
		
		public InferModelDialog(Shell parent, int style) {
			super(parent, style);
		}

		public boolean open() {
			//Create the content
			final Shell myShell = new Shell(getParent(), SWT.DIALOG_TRIM | SWT.MIN | SWT.PRIMARY_MODAL);
			myShell.setText("Infer Aslan++ Model Dialog");
			
			Label projectLabel = new Label(myShell, SWT.NONE);
			projectLabel.setText("Select the project");
			
			final Combo project = new Combo(myShell, SWT.DROP_DOWN | SWT.BORDER | SWT.READ_ONLY);
			
			final Button preferences = new Button(myShell, SWT.PUSH);
			preferences.setText("Properties");
			preferences.setEnabled(false);
			
			Composite comp = new Composite(myShell,SWT.NONE);
			
			final Button okButton = new Button(myShell, SWT.PUSH);
			okButton.setText("Run");
			okButton.setEnabled(false);
			okButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					run = true;
					myShell.dispose();
				}
			});

			Button cancelButton = new Button(myShell, SWT.PUSH);
			cancelButton.setText("Cancel");
			cancelButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					run = false;
					myShell.dispose();
				}
			});
			
		    myShell.setLayout(new GridLayout(2, true));

		    GridData dataLabel = new GridData(150,20);
		    dataLabel.horizontalSpan = 2;
		    projectLabel.setLayoutData(dataLabel);

		    GridData dataProject = new GridData(150,20);
		    project.setLayoutData(dataProject);

		    GridData dataPreference = new GridData(150,20);
		    preferences.setLayoutData(dataPreference);
 
		    GridData compositeData = new GridData(150,20);
		    compositeData.horizontalSpan = 2;
		    compositeData.verticalSpan = 2;
			comp.setLayoutData(compositeData);			

		    GridData dataCancel = new GridData(150,20);
			cancelButton.setLayoutData(dataCancel);
			
		    GridData dataOK = new GridData(150,20);
			okButton.setLayoutData(dataOK);
		    
		    IProject[] allProjects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		    final IJavaProject[] javaProjects = new IJavaProject[allProjects.length];
		    int j = 0;
		    for (int i = 0; i < allProjects.length; i++) {
		    	try {
					if(allProjects[i].hasNature("org.eclipse.jdt.core.javanature")) {
						project.add(allProjects[i].getName(),j);
						javaProjects[j] = JavaCore.create(allProjects[i]);
						j++;
					}
				} catch (CoreException e1) {}
		    }

			preferences.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					PreferenceDialog diag = PreferencesUtil.createPropertyDialogOn(
							PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 
							javaProjects[project.getSelectionIndex()], 
							"ro.ieat.jmodex.propertypage", 
							new String[] { "ro.ieat.jmodex.propertypage" }, 
							Collections.EMPTY_MAP);
					diag.open();
				}				
			});
			
			project.addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					if(project.getSelectionIndex() != -1) {
						preferences.setEnabled(true);
						okButton.setEnabled(true);
						selectedProject = javaProjects[project.getSelectionIndex()];
					} else {
						preferences.setEnabled(false);	
						okButton.setEnabled(false);
						selectedProject = null;
					}
				}				
			});


			//Open
			myShell.pack();
			myShell.setLocation(getParent().getLocation().x + getParent().getSize().x/2 - myShell.getSize().x/2,getParent().getLocation().y + getParent().getSize().y/2 - myShell.getSize().y/2);
			myShell.open();
			Display display = getParent().getDisplay();
			while (!myShell.isDisposed()) {
				if (!display.readAndDispatch()) {
					display.sleep();
				}
			}
			return run;
		}

		public IJavaProject getProject() {
			return selectedProject;
		}
		
	}
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		InferModelDialog dialog = new InferModelDialog(window.getShell(),SWT.NONE);
		boolean toRun = dialog.open();
		if(toRun) {
			Job task = jModexFacade.getInstance().inferModel(dialog.getProject());
			task.setUser(true);
			task.schedule();
			return task;
		} else {
			return null;
		}
	}

}
