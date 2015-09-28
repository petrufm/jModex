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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PathEditor;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.preference.StringButtonFieldEditor;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbenchPropertyPage;

import ro.ieat.jmodex.Activator;

public class ProjectPropertyPage extends FieldEditorPreferencePage implements IWorkbenchPropertyPage {
	
	public final static String OUTPUTFILE = "aspan++file";
	public final static String PREDEFINEDSPECIFIERSLIST = "predefinedspecifierlist";	
	public final static String SPECIFIERSLIST = "specifierlist";
	public final static String LOCALSABSTRACTION = "localsabstraction";
	public final static String DATABASEDESCRIPTION = "databasespecification";
	public final static String TOMCATSPECIFIEROPTIONS = "notNullRequestParametyers";
	public final static String SEPARATOR = ":";
		
	public static String getProperty(IJavaProject prj, String propName) {
		try {
			String value = prj.getResource().getPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,propName));
			if(value == null) {
				if(propName.equals(OUTPUTFILE))
					value = prj.getResource().getLocation().toOSString() + File.separator + prj.getElementName() + ".aslan++";
				else if(propName.equals(SPECIFIERSLIST)) {
					value = "";
				} else if(propName.equals(PREDEFINEDSPECIFIERSLIST)) {
					value = "";
				} else if(propName.equals(LOCALSABSTRACTION)) {
					value = "";
				} else if(propName.equals(DATABASEDESCRIPTION)) {
					value = "";
				} else if(propName.equals(TOMCATSPECIFIEROPTIONS)) {
					value = "";
				}
			}
			return value;
		} catch (CoreException e) {
			return null;
		}
	}
	
    private IAdaptable element;
    private PreferenceStore ps; 
    
	public ProjectPropertyPage() {}

	@Override
	public IAdaptable getElement() {
		return element;
	}

	public void performApply() {
		super.performOk();
		try {
			String theFileName = ps.getString(OUTPUTFILE);
			String tmp = theFileName.toLowerCase();
			if(!tmp.endsWith(".aslan++")) {
				theFileName += ".aslan++";
			}
			if(new File(theFileName).isAbsolute()) {
				((IJavaProject)element).getResource().setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,OUTPUTFILE), theFileName);
			} else {
				theFileName = ((IJavaProject)element).getResource().getLocation().toOSString() + File.separator + theFileName;
				((IJavaProject)element).getResource().setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,OUTPUTFILE), theFileName);				
			}
			((IJavaProject)element).getResource().setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,SPECIFIERSLIST), ps.getString(SPECIFIERSLIST));
			String predefList = "";
			if(ps.getBoolean("ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier")) {
				predefList+="ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier:";
			}
			if(ps.getBoolean("ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier")) {
				predefList+="ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier:";
			}
			if(ps.getBoolean("ro.ieat.isummarize.webgoattechnology.WebgoatSpecifier")) {
				predefList+="ro.ieat.isummarize.webgoattechnology.WebgoatSpecifier:";
			}
			if(!predefList.equals("")) {
				predefList = predefList.substring(0,predefList.length() - 1);
			}
			((IJavaProject)element).getResource().setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,PREDEFINEDSPECIFIERSLIST), predefList);
			//Abstraction file for local variables
			String theLocalsAbstractionFile = ps.getString(LOCALSABSTRACTION);
			if(!theLocalsAbstractionFile.equals("")) {
				if(!new File(theLocalsAbstractionFile).isAbsolute()) {
					theLocalsAbstractionFile = ((IJavaProject)element).getResource().getLocation().toOSString() + File.separator + theLocalsAbstractionFile;
				}
			}
			((IJavaProject)element).getResource().setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,LOCALSABSTRACTION), theLocalsAbstractionFile);
			//Description file for database
			String theDBDescriptionFile = ps.getString(DATABASEDESCRIPTION);
			if(!theDBDescriptionFile.equals("")) {
				if(!new File(theDBDescriptionFile).isAbsolute()) {
					theDBDescriptionFile = ((IJavaProject)element).getResource().getLocation().toOSString() + File.separator + theDBDescriptionFile;
				}
			}
			((IJavaProject)element).getResource().setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,DATABASEDESCRIPTION), theDBDescriptionFile);
			
			//Not null request parameters for Tomcat Specifier
			String tomcatSpecifiersOptions = ps.getString(TOMCATSPECIFIEROPTIONS);
			((IJavaProject)element).getResource().setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,TOMCATSPECIFIEROPTIONS), tomcatSpecifiersOptions);
			
		} catch (CoreException e) {}		
	}
	
	public boolean performOk() {
		performApply();
		return true;
	}
	
	@Override
	public void setElement(IAdaptable element) {
		this.element = element;
		ps = new PreferenceStore() {
			public void save() {}
		};
		ps.setDefault(OUTPUTFILE, ((IJavaProject)element).getResource().getLocation().toOSString() + File.separator + ((IJavaProject)element).getElementName() + ".aslan++");
		ps.setDefault(SPECIFIERSLIST, "");
		ps.setDefault("ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier", "true");
		ps.setDefault("ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier","false"); 
		ps.setDefault("ro.ieat.isummarize.webgoattechnology.WebgoatSpecifier","false"); 
		ps.setDefault(LOCALSABSTRACTION, "");
		ps.setDefault(DATABASEDESCRIPTION, "");
		ps.setDefault(TOMCATSPECIFIEROPTIONS, "false");
		ps.setValue(OUTPUTFILE, getProperty((IJavaProject) element,OUTPUTFILE));
		ps.setValue(SPECIFIERSLIST, getProperty((IJavaProject) element,SPECIFIERSLIST));
		String predefList = getProperty((IJavaProject)element, PREDEFINEDSPECIFIERSLIST);
		ps.setValue("ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier", predefList.contains("ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier"));
		ps.setValue("ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier", predefList.contains("ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier"));
		ps.setValue("ro.ieat.isummarize.webgoattechnology.WebgoatSpecifier", predefList.contains("ro.ieat.isummarize.webgoattechnology.WebgoatSpecifier"));
		ps.setValue(LOCALSABSTRACTION, getProperty((IJavaProject) element,LOCALSABSTRACTION));
		ps.setValue(DATABASEDESCRIPTION, getProperty((IJavaProject) element,DATABASEDESCRIPTION));
		ps.setValue(TOMCATSPECIFIEROPTIONS, getProperty((IJavaProject) element,TOMCATSPECIFIEROPTIONS));
	}
	
	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return ps;
	}
	
	@Override
	protected void createFieldEditors() {
		FileFieldEditor fileEditor = new FileFieldEditor(OUTPUTFILE, "Output Aslan++ File:", false, StringButtonFieldEditor.VALIDATE_ON_KEY_STROKE, this.getFieldEditorParent()) {	
			protected boolean checkState() {
		        String path = getTextControl().getText();
		        if (path != null) {
					path = path.trim();
				} else {
					path = "";
				}
		        if(path.equals("")) {
		        	showErrorMessage("You must specify a file name !");
		        	return false;
		        }
		        if(new File(path).isDirectory()) {
		        	showErrorMessage("The specified file is a directory !");
		        	return false;		        	
		        }
				return true;
			}			
		};
		fileEditor.setEmptyStringAllowed(false);
		this.addField(fileEditor);
		PathEditor specifierEditor = new PathEditor(SPECIFIERSLIST,"Paths to User Defined Specifiers:", "", this.getFieldEditorParent());
        this.addField(specifierEditor);        
        FileFieldEditor localsAbstractionFile = new FileFieldEditor(LOCALSABSTRACTION, "(Experimental) Path to file declaring local variables predicate abstractions:", false, StringButtonFieldEditor.VALIDATE_ON_KEY_STROKE, this.getFieldEditorParent()) {	
			protected boolean checkState() {
		        String path = getTextControl().getText();
		        if (path != null) {
					path = path.trim();
				} else {
					path = "";
				}
		        if(!path.equals("")) {
			        if(new File(path).isDirectory()) {
			        	showErrorMessage("The specified file is a directory !");
			        	return false;		        	
			        }
			        if(!new File(path).exists()) {
			        	showErrorMessage("The specified file must exist !");
			        	return false;		        	
			        }
		        }
				return true;
			}			
		};
        this.addField(localsAbstractionFile);
        final Composite dbDescriptionFileParent = this.getFieldEditorParent();
        final FileFieldEditor dbDescriptionFile = new FileFieldEditor(DATABASEDESCRIPTION, "Path to file describing the tables in the database:", false, StringButtonFieldEditor.VALIDATE_ON_KEY_STROKE, dbDescriptionFileParent) {	
 			protected boolean checkState() {
 		        String path = getTextControl().getText();
 		        if (path != null) {
 					path = path.trim();
 				} else {
 					path = "";
 				}
 		        if(!path.equals("")) {
 			        if(new File(path).isDirectory()) {
 			        	showErrorMessage("The specified file is a directory !");
 			        	return false;		        	
 			        }
 			        if(!new File(path).exists()) {
 			        	showErrorMessage("The specified file must exist !");
 			        	return false;
 			        }
 		        } else if(this.getPreferenceStore().getString("ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier").equals("true")){
			        showErrorMessage("You must provide the database structure!");
			        return false; 		        	
 		        }
				return true;
 			}	
 		}; 
        this.addField(new BooleanFieldEditor("ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier","Use Predefined Tomcat Servlet Specifier",this.getFieldEditorParent()));

        this.addField(new BooleanFieldEditor(TOMCATSPECIFIEROPTIONS,"Option for Tomcat Servlet Specifier : consider not null request parameters", this.getFieldEditorParent()));

        final BooleanFieldEditor sqlSpecifierCheck = new BooleanFieldEditor("ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier","Use Predefined Java Sql Specifier",this.getFieldEditorParent()) {
        	protected Button getChangeControl(Composite parent) {
        		Button bt = super.getChangeControl(parent);
        		final BooleanFieldEditor enclosing = this;
        		bt.addSelectionListener(new SelectionListener() {					
					@Override
					public void widgetSelected(SelectionEvent e) {
       					dbDescriptionFile.setEnabled(enclosing.getBooleanValue(),dbDescriptionFileParent);
       					dbDescriptionFile.load();
					}
					@Override
					public void widgetDefaultSelected(SelectionEvent e) {}
				});
        		return bt;
        	}
        };
 	    dbDescriptionFile.setEnabled(this.getPreferenceStore().getString("ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier").equals("true"),dbDescriptionFileParent);
 	    this.addField(sqlSpecifierCheck);
        this.addField(dbDescriptionFile);
        final BooleanFieldEditor webgoatSpecifier = new BooleanFieldEditor("ro.ieat.isummarize.webgoattechnology.WebgoatSpecifier"," (Experimental) Use Predefined Webgoat Specifier",this.getFieldEditorParent());
        this.addField(webgoatSpecifier);
	}	
}
