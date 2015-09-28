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
package ro.ieat.jmodex.api;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;

import ro.ieat.isummarize.CallGraphBuilder;
import ro.ieat.isummarize.CallGraphForEclipseJavaProject;
import ro.ieat.isummarize.MethodSummary;
import ro.ieat.isummarize.MethodSummaryAlgorithm;
import ro.ieat.isummarize.TechnologySpecifier;
import ro.ieat.isummarize.javasql.JavaSqlTechnologySpecifier;
import ro.ieat.isummarize.jsptomcat.JSPTomcatTranslatedSpecifier;
import ro.ieat.isummarize2aslanpp.AslanppGenerator;
import ro.ieat.isummarize2aslanpp.AslanppModel;
import ro.ieat.jmodex.Activator;
import ro.ieat.jmodex.integration.EclipseProgressMonitor;
import ro.ieat.jmodex.integration.ProjectPropertyPage;
import ro.ieat.jmodex.utils.CancelException;

import com.ibm.wala.classLoader.IMethod;

/**
 * A facade representing the API of the jModex plug-in.
 * 
 */
public class jModexFacade {

	/**
	 * Singleton variable
	 */
	private static jModexFacade instance;

	/*
	 * Preference name constants
	 */
	public final static String OUTPUTFILE = "aspan++file";
	public final static String PREDEFINEDSPECIFIERSLIST = "predefinedspecifierlist";	
	public final static String SPECIFIERSLIST = "specifierlist";
	public final static String LOCALSABSTRACTION = "localsabstraction";
	public final static String DATABASEDATABASEDESCRIPTION = "databasespecification";
	public final static String TOMCATSPECIFIEROPTIONS = "notNullRequestParametyers";

	private jModexFacade() {}
	
	/**
	 * Returns the singleton object of this facade. The singleton is created
	 * if required.
	 *    
	 * @return - the singleton object
	 * 
	 */
	public static jModexFacade getInstance() {
		if(instance == null) {
			instance = new jModexFacade();
		}
		return instance;
	}
	
	/**
	 * Sets programmatically a preference for a project that is going to be analyzed. The preference names are 
	 * constant strings specified at the beginning of this class.
	 * 
	 * @param theProject - the project for which we set some property
	 * @param preferenceName - the name of the set property
	 * @param preferenceValue - the value of the set property
	 * @return - true in case of success, otherwise false
	 */
	public boolean setProjectPreference(IJavaProject theProject, String preferenceName, String preferenceValue) {
		try {
			if(preferenceName.equals(OUTPUTFILE) || 
					preferenceName.equals(PREDEFINEDSPECIFIERSLIST) || 
					preferenceName.equals(SPECIFIERSLIST) || 
					preferenceName.equals(LOCALSABSTRACTION) ||
					preferenceName.equals(DATABASEDATABASEDESCRIPTION) ||
					preferenceName.equals(TOMCATSPECIFIEROPTIONS)) {
				theProject.getResource().setPersistentProperty(new QualifiedName(Activator.PLUGIN_ID,preferenceName), preferenceValue);
				return true;
			}
		} catch (CoreException e) {}
		return false;
	}
	
	/**
	 * The class representing the status of the job returned by 
	 * the {@link ro.ieat.jmodex.api.jModexFacade#visualizeOutputModel(IJavaProject) 
	 * visualizeOutputModel} method.
	 * 
	 */
	public class VisualizeOutputModelStatus implements IStatus {

		@Override
		public IStatus[] getChildren() {
			return null;
		}

		@Override
		public int getCode() {
			return 0;
		}

		@Override
		public Throwable getException() {
			return null;
		}

		@Override
		public String getMessage() {
			return null;
		}

		@Override
		public String getPlugin() {
			return null;
		}

		@Override
		public int getSeverity() {
			return 0;
		}

		@Override
		public boolean isMultiStatus() {
			return false;
		}

		@Override
		public boolean isOK() {
			return false;
		}

		@Override
		public boolean matches(int severityMask) {
			return false;
		}
				
	}
	
	/**
	 * Creates a job object that will visualize the output model of 
	 * the code of the specified project.
	 * 
	 * @param theProject - the project that it is going to be analyzed
	 * @return - the created job object
	 * 
	 */
	public Job visualizeOutputModel(final IJavaProject theProject) {
		
		return new Job("Visualize Output Model") {
			
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				monitor.beginTask("Visualize Output Model", 100);		
								
				monitor.done();
				return new VisualizeOutputModelStatus();
			}
			
		};
	}
	
	/**
	 * The class representing the status of the job returned by 
	 * the {@link ro.ieat.jmodex.api.jModexFacade#inferModel(IJavaProject) inferModel} 
	 * method.
	 * 
	 */
	public class InferModelStatus implements IStatus {

		private File theFile = null;
		private Throwable exception = null;
		
		private InferModelStatus(File theFile) {
			this.theFile = theFile;
		}
		
		public InferModelStatus(Throwable e) {
			this.exception = e;
		}

		@Override
		public IStatus[] getChildren() {
			return null;
		}

		@Override
		public int getCode() {
			return 0;
		}

		@Override
		public Throwable getException() {
			return exception;
		}

		@Override
		public String getMessage() {
			return "Error during the inference process";
		}

		@Override
		public String getPlugin() {
			return Activator.PLUGIN_ID;
		}

		@Override
		public int getSeverity() {
			if(exception instanceof CancelException) {
				return IStatus.CANCEL;
			} else if(exception == null) {
				return IStatus.OK;
			} else {
				return IStatus.ERROR;
			}
		}

		@Override
		public boolean isMultiStatus() {
			return false;
		}

		@Override
		public boolean isOK() {
			return exception == null;
		}

		@Override
		public boolean matches(int severityMask) {
			return false;
		}
		
		/**
		 * Returns the result of the model inference job.
		 * 
		 * @return - the file containing the ASLAN(++) model
		 * 
		 */
		public File getResult() {
			return theFile;
		}
		
	}
	
	/**
	 * Creates a job object that will infer an ASLAN(++) model from 
	 * the code of the specified project.
	 * 
	 * @param theProject - the project that is going to be analyzed
	 * @return - the created job object
	 * 
	 */
	public Job inferModel(final IJavaProject theProject) {
			
		return new Job("Infer Aslan++ Model") {
				
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(this.getName(), 3);
					monitor.subTask("Build beharioral automaton");
					Job j1 = buildBehavioralAutomaton(theProject);
					j1.schedule();
					j1.join();
					if(j1.getResult().getException() != null) {
						throw j1.getResult().getException();
					}
					if(monitor.isCanceled()) {
						throw new CancelException();
					}
					Map<IMethod, MethodSummary> behaviour = ((BehavioralAutomatonStatus)j1.getResult()).getResult();
					monitor.worked(1);
					monitor.subTask("Convert automaton to Aslan++");
					String modelName = ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.OUTPUTFILE);
					modelName = modelName.substring(modelName.lastIndexOf(File.separator) + 1,modelName.lastIndexOf("."));
					Job j2 = translateIntoAslanpp(modelName, behaviour);
					j2.schedule();
					j2.join();
					if(j2.getResult().getException() != null) {
						throw j2.getResult().getException();
					}
					if(monitor.isCanceled()) {
						throw new CancelException();
					}
					AslanppModel am = ((TranslationStatus)j2.getResult()).getResult();
					monitor.worked(1);
					monitor.subTask("Write the model into file");
					File file = new File(ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.OUTPUTFILE));
					PrintStream fos = new PrintStream(new FileOutputStream(file));
					StringBuilder text = new StringBuilder();
					am.print(0, text);
					fos.print(text);
					fos.close();
					theProject.getResource().refreshLocal(IResource.DEPTH_INFINITE, null);
					monitor.worked(1);
					return new InferModelStatus(file);
				} catch (Throwable e) {
					return new InferModelStatus(e);
				} finally {
					monitor.done();					
				}
			}
			
		};
		
	}
	
	/**
	 * Creates a job object that will infer an ASLAN(++) model from 
	 * the code of the specified project.
	 * 
	 * @param theProject - the project that is going to be analyzed
	 * @param theFile - the file containing the resulting model
	 * @return - the created job object
	 */
	public Job inferModel(final IJavaProject theProject, final IFile theFile) {
		
		return new Job("Infer Aslan++ Model") {
				
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					monitor.beginTask(this.getName(), 3);
					monitor.subTask("Build beharioral automaton");
					Job j1 = buildBehavioralAutomaton(theProject);
					j1.schedule();
					j1.join();
					if(j1.getResult().getException() != null) {
						throw j1.getResult().getException();
					}
					if(monitor.isCanceled()) {
						throw new CancelException();
					}
					Map<IMethod, MethodSummary> behaviour = ((BehavioralAutomatonStatus)j1.getResult()).getResult();
					monitor.worked(1);
					monitor.subTask("Convert automaton to Aslan++");
					String modelName = theFile.getName();
					modelName = modelName.substring(modelName.lastIndexOf(File.separator) + 1,modelName.lastIndexOf("."));
					Job j2 = translateIntoAslanpp(modelName, behaviour);
					j2.schedule();
					j2.join();
					if(j2.getResult().getException() != null) {
						throw j2.getResult().getException();
					}
					if(monitor.isCanceled()) {
						throw new CancelException();
					}
					AslanppModel am = ((TranslationStatus)j2.getResult()).getResult();
					monitor.worked(1);
					monitor.subTask("Write the model into file");
					ByteArrayOutputStream bais = new ByteArrayOutputStream();
					PrintStream fos = new PrintStream(bais);
					StringBuilder text = new StringBuilder();
					am.print(0, text);
					fos.print(text);
					fos.close();
					monitor.worked(1);
					if(theFile.exists()) {
						theFile.setContents(new ByteArrayInputStream(bais.toByteArray()), true, true, null);
					} else {
						theFile.create(new ByteArrayInputStream(bais.toByteArray()), true, null);
					}
					return new InferModelStatus(theFile.getRawLocation().makeAbsolute().toFile());
				} catch (Throwable e) {
					return new InferModelStatus(e);
				} finally {
					monitor.done();					
				}
			}
			
		};
		
	}
	
	public class BehavioralAutomatonStatus implements IStatus {

		private Map<IMethod, MethodSummary> behavior = null;
		private Throwable exception = null;
		
		private BehavioralAutomatonStatus(Map<IMethod, MethodSummary> behaviour) {
			this.behavior = behaviour;
		}
		
		public BehavioralAutomatonStatus(Throwable e) {
			this.exception = e;
		}

		@Override
		public IStatus[] getChildren() {
			return null;
		}

		@Override
		public int getCode() {
			return 0;
		}

		@Override
		public Throwable getException() {
			return exception;
		}

		@Override
		public String getMessage() {
			return "Error during behavioral model building";
		}

		@Override
		public String getPlugin() {
			return Activator.PLUGIN_ID;
		}

		@Override
		public int getSeverity() {
			if(exception instanceof CancelException) {
				return IStatus.CANCEL;
			} else if(exception == null) {
				return IStatus.OK;
			} else {
				return IStatus.ERROR;
			}
		}

		@Override
		public boolean isMultiStatus() {
			return false;
		}

		@Override
		public boolean isOK() {
			return exception == null;
		}

		@Override
		public boolean matches(int severityMask) {
			return false;
		}
		
		public Map<IMethod, MethodSummary> getResult() {
			return behavior;
		}	
	}
	
	public Job buildBehavioralAutomaton(final IJavaProject theProject) {
		return new Job("Build behavioral automaton") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
					monitor.beginTask(this.getName(), 3);
					try {
						String aList = ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.SPECIFIERSLIST);
						String[] specPaths = aList.split(File.pathSeparator);
						List<URL> urls = new ArrayList<URL>();
						Set<String> allClasses = new HashSet<String>();
						for(int i = 0; i < specPaths.length; i++) {
							URL currentURL = new File(specPaths[i]).toURI().toURL();
							urls.add(currentURL);
							getAllClassFiles(currentURL.getPath(), "", allClasses);
						}
						URLClassLoader cl = new URLClassLoader(urls.toArray(new URL[]{}),this.getClass().getClassLoader());	
						TechnologySpecifier userChain = null;
						for(String aClass : allClasses) {
							if(aList.equals("")) continue;
							Class<?> aClassObject = cl.loadClass(aClass);
							if(TechnologySpecifier.class.isAssignableFrom(aClassObject)) {
								userChain = (TechnologySpecifier) aClassObject.getConstructor(TechnologySpecifier.class).newInstance(userChain);
							}
						}
						if(!ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.PREDEFINEDSPECIFIERSLIST).isEmpty()) {
							for(String aPredefSpec : ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.PREDEFINEDSPECIFIERSLIST).split(ProjectPropertyPage.SEPARATOR)) {
								Class<?> aClassObject = cl.loadClass(aPredefSpec);
								userChain = (TechnologySpecifier) aClassObject.getConstructor(TechnologySpecifier.class).newInstance(userChain);							
								if(userChain instanceof JavaSqlTechnologySpecifier) {
									((JavaSqlTechnologySpecifier)userChain).setDescriptionFile(new File(ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.DATABASEDESCRIPTION)));
								} else if(userChain instanceof JSPTomcatTranslatedSpecifier) {
									((JSPTomcatTranslatedSpecifier)userChain).setNotNullRequestParameters(Boolean.parseBoolean(ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.TOMCATSPECIFIEROPTIONS)));
								}
							}
						}
						if(userChain == null) {
							throw new RuntimeException("The system cannot be analyzed since there are no entry points!");
						}
						
						CallGraphBuilder cBuilder = new CallGraphForEclipseJavaProject(theProject, userChain);
						MethodSummaryAlgorithm alg;
						if(ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.LOCALSABSTRACTION).isEmpty()) {
							alg = new MethodSummaryAlgorithm(cBuilder);
						} else {
							alg = new MethodSummaryAlgorithm(cBuilder, new File(ProjectPropertyPage.getProperty(theProject, ProjectPropertyPage.LOCALSABSTRACTION)));								
						}
						Map<IMethod, MethodSummary> behaviour = alg.analyzeIsolated(new EclipseProgressMonitor(monitor));
						return new BehavioralAutomatonStatus(behaviour);
					} catch (Throwable e) {
						return new BehavioralAutomatonStatus(e);
					} finally {
						monitor.done();
					}
			}			
		};
	}
	
	private class TranslationStatus implements IStatus {

		private AslanppModel am = null;
		private Throwable exception = null;
		
		private TranslationStatus(AslanppModel am) {
			this.am = am;
		}
		
		public TranslationStatus(Throwable e) {
			this.exception = e;
		}

		@Override
		public IStatus[] getChildren() {
			return null;
		}

		@Override
		public int getCode() {
			return 0;
		}

		@Override
		public Throwable getException() {
			return exception;
		}

		@Override
		public String getMessage() {
			return "Error during translation into Aslan++";
		}

		@Override
		public String getPlugin() {
			return Activator.PLUGIN_ID;
		}

		@Override
		public int getSeverity() {
			if(exception == null) {
				return IStatus.OK;
			} else if(exception instanceof CancelException) {
				return IStatus.CANCEL;
			} else {
				return IStatus.ERROR;
			}
		}

		@Override
		public boolean isMultiStatus() {
			return false;
		}

		@Override
		public boolean isOK() {
			return exception == null;
		}

		@Override
		public boolean matches(int severityMask) {
			return false;
		}
		
		public AslanppModel getResult() {
			return am;
		}	
	}

	private Job translateIntoAslanpp(final String name, final Map<IMethod, MethodSummary> behaviour) {
		return new Job("Translate behavioral automaton into Aslan++") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
					try {
						AslanppModel am = AslanppGenerator.generate(name,behaviour,new EclipseProgressMonitor(monitor));
						return new TranslationStatus(am);
					} catch (Throwable e) {
						return new TranslationStatus(e);
					}
			}			
		};
	}
	
    private void getAllClassFiles(String path, String packageName, Set<String> allClasses) {
        File thisPath = new File(path);
        File[] files = thisPath.listFiles();
        if (files == null) return;
        for (int i = 0; i < files.length; i++) {
            if (files[i].getName().endsWith(".class") && files[i].isFile())
                allClasses.add(files[i].getName().substring(0, files[i].getName().lastIndexOf(".")));
            if (files[i].isDirectory())
            	getAllClassFiles(path + File.separator + files[i].getName(), packageName + files[i].getName() + ".", allClasses);
        }
    }

	
}
