/*******************************************************************************
 * Copyright (c) 2008, 2011 Sonatype Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Sonatype Inc. - initial API and implementation
 *******************************************************************************/
package com.github.cardforge.maven.plugins.android.phase_prebuild;

import com.github.cardforge.maven.plugins.android.common.AndroidExtension;
import com.github.cardforge.maven.plugins.android.common.ArtifactResolverHelper;
import com.github.cardforge.maven.plugins.android.common.DependencyResolver;
import com.github.cardforge.maven.plugins.android.common.PomConfigurationHelper;
import com.github.cardforge.maven.plugins.android.common.UnpackedLibHelper;
import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;


/**
 * Adds classes from AAR and APK dependencies to the project compile classpath.
 * 
 * @author William Ferguson
 * @author Benoit Billington
 * @author Manfred Moser
 */
@Component( role = AbstractMavenLifecycleParticipant.class, hint = "default" )
public final class ClasspathModifierLifecycleParticipant extends AbstractMavenLifecycleParticipant
{
    /** 
     * Mojo configuration parameter to determine if jar files found inside an apklib are 
     * pulled onto the classpath and into the resulting apk, defaults to false
     * @see INCLUDE_FROM_APKLIB_DEFAULT
     */
    private static final String INCLUDE_FROM_APKLIB_PARAM = "includeLibsJarsFromApklib";
    /** 
     * Mojo configuration parameter to determine if jar files found inside an aar are 
     * pulled onto the classpath and into the resulting apk, defaults to false
     * @see INCLUDE_FROM_AAR_DEFAULT
     */
    private static final String INCLUDE_FROM_AAR_PARAM = "includeLibsJarsFromAar";
    /**
     * Mojo configuration parameter to determine if we should warn about dependency conflicts with the provided
     * dependencies.
     * 
     * @see DISABLE_CONFLICTING_DEPENDENCIES_WARNING_DEFAULT
     */
    private static final String DISABLE_CONFLICTING_DEPENDENCIES_WARNING_PARAM
        = "disableConflictingDependenciesWarning";
    private static final boolean INCLUDE_FROM_APKLIB_DEFAULT = false;
    private static final boolean INCLUDE_FROM_AAR_DEFAULT = true;
    private static final boolean DISABLE_CONFLICTING_DEPENDENCIES_WARNING_DEFAULT = false;

    /**
     * Mojo configuration parameter that defines where AAR files should be unpacked.
     * Default is /target/unpacked-libs
     */
    private static final String UNPACKED_LIBS_FOLDER_PARAM = "unpackedLibsFolder";

    @Requirement
    private ArtifactResolver artifactResolver;

    @Requirement( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Requirement
    private Logger log;
    
    private boolean addedJarFromLibs = false;
    
    @Override
    public void afterProjectsRead( MavenSession session ) throws MavenExecutionException
    {
        log.debug( "" );
        log.debug( "ClasspathModifierLifecycleParticipant#afterProjectsRead - start" );
        log.debug( "" );

        log.debug( "CurrentProject=" + session.getCurrentProject() );
        final List<MavenProject> projects = session.getProjects();
        final DependencyResolver dependencyResolver = new DependencyResolver( log, dependencyGraphBuilder );
        final ArtifactResolverHelper artifactResolverHelper = new ArtifactResolverHelper( artifactResolver, log );

        for ( MavenProject project : projects )
        {
            log.debug( "" );
            log.debug( "project=" + project.getArtifact() );

            if ( ! AndroidExtension.isAndroidPackaging( project.getPackaging() ) )
            {
                continue; // do not modify classpath if not an android project.
            }

            final String unpackedLibsFolder
                = getMojoConfigurationParameter( project, UNPACKED_LIBS_FOLDER_PARAM, null );
            final UnpackedLibHelper helper = new UnpackedLibHelper( artifactResolverHelper, project, log,
                    unpackedLibsFolder == null ? null : new File( unpackedLibsFolder )
            );

            final Set<Artifact> artifacts;

            // If there is an extension ClassRealm loaded for this project then use that
            // as the ContextClassLoader so that Wagon extensions can be used to resolves dependencies.
            final ClassLoader projectClassLoader = ( project.getClassRealm() != null )
                    ? project.getClassRealm()
                    : Thread.currentThread().getContextClassLoader();

            final ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
            try
            {
                Thread.currentThread().setContextClassLoader( projectClassLoader );
                artifacts = dependencyResolver.getProjectDependenciesFor( project, session );
            }
            catch ( DependencyGraphBuilderException e )
            {
                // Nothing to do. The resolution failure will be displayed by the standard resolution mechanism.
                continue;
            }
            finally
            {
                Thread.currentThread().setContextClassLoader( originalClassLoader );
            }

            boolean includeFromAar = getMojoConfigurationParameter( project, INCLUDE_FROM_AAR_PARAM,
                    INCLUDE_FROM_AAR_DEFAULT );
            boolean includeFromApklib = getMojoConfigurationParameter( project, INCLUDE_FROM_APKLIB_PARAM,
                    INCLUDE_FROM_APKLIB_DEFAULT );
            boolean disableConflictingDependenciesWarning = getMojoConfigurationParameter( project,
                    DISABLE_CONFLICTING_DEPENDENCIES_WARNING_PARAM, DISABLE_CONFLICTING_DEPENDENCIES_WARNING_DEFAULT );

            log.debug( "projects deps: : " + artifacts );
            
            if ( !disableConflictingDependenciesWarning )
            {
                ProvidedDependencyChecker checker = new ProvidedDependencyChecker();
                checker.checkProvidedDependencies( artifacts, log );
            }
            
            for ( Artifact artifact : artifacts )
            {
                final String type = artifact.getType();
                if ( type.equals( AndroidExtension.AAR ) )
                {
                    // An AAR lib contains a classes jar that needs to be added to the classpath.
                    // Create a placeholder classes.jar and add it to the compile classpath.
                    // It will replaced with the real classes.jar by GenerateSourcesMojo.
                    addClassesToClasspath( helper, project, artifact );

                    // An AAR may also contain zero or more internal libs in the libs folder.
                    // If 'includeLibsJarsFromAar' config param is true then include them too.
                    if ( includeFromAar )
                    {
                        // Add jar files in 'libs' into classpath.
                        addLibsJarsToClassPath( helper, project, artifact );
                    }
                }
                else if ( type.equals( AndroidExtension.APK ) )
                {
                    // The only time that an APK will likely be a dependency is when this an an APK test project.
                    // So add a placeholder (we cannot resolve the actual dep pre build) to the compile classpath.
                    // The placeholder will be replaced with the real APK jar later.
                    addClassesToClasspath( helper, project, artifact );
                }
                else if ( type.equals( AndroidExtension.APKLIB ) )
                {
                    if ( includeFromApklib ) 
                    {
                      // Add jar files in 'libs' into classpath.
                      addLibsJarsToClassPath( helper, project, artifact );
                    }
                }
            }
        }

        if ( addedJarFromLibs )
        {
            log.warn(
                    "Transitive dependencies should really be provided by Maven dependency management.\n"
        + "          We suggest you to ask the above providers to package their component properly.\n"
        + "          Things may break at compile and/or runtime due to multiple copies of incompatible libraries." );
        }
        log.debug( "" );
        log.debug( "ClasspathModifierLifecycleParticipant#afterProjectsRead - finish" );
    }

    private String getMojoConfigurationParameter( MavenProject project, String name, String defaultValue )
    {
        String value = PomConfigurationHelper.getPluginConfigParameter( project,
                name, defaultValue );
        log.debug( name + " set to " + value );
        return value;
    }
    
    private boolean getMojoConfigurationParameter( MavenProject project, String name, boolean defaultValue )
    {
        return Boolean.valueOf( getMojoConfigurationParameter( project, name, Boolean.toString( defaultValue ) ) );
    }

    /**
     * Add jar files in libs into the project classpath.
     */
    private void addLibsJarsToClassPath( UnpackedLibHelper helper, MavenProject project, Artifact artifact )
        throws MavenExecutionException
    {
         try
         {
             final File unpackLibFolder = helper.getUnpackedLibFolder( artifact );
             final File artifactFile = helper.getArtifactToFile( artifact );
             ZipFile zipFile = new ZipFile( artifactFile );
             Enumeration enumeration = zipFile.entries();
             while ( enumeration.hasMoreElements() )
             {
                 ZipEntry entry = ( ZipEntry )  enumeration.nextElement();
                 String entryName = entry.getName();

                 // Only jar files under 'libs' directory to be processed.
                 if ( Pattern.matches( "^libs/.+\\.jar$", entryName ) )
                 {
                     final File libsJarFile = new File( unpackLibFolder, entryName );
                     log.warn( "Adding jar from libs folder to classpath: " + libsJarFile );

                     // In order to satisfy the LifecycleDependencyResolver on execution up to a phase that
                     // has a Mojo requiring dependency resolution I need to create a dummy classesJar here.
                     if ( !libsJarFile.getParentFile().exists() )
                     {
                         libsJarFile.getParentFile().mkdirs();
                     }
                     libsJarFile.createNewFile();

                     // Add the jar to the classpath.
                     final Dependency dependency =
                            createSystemScopeDependency( artifact, libsJarFile, libsJarFile.getName() );

                     project.getModel().addDependency( dependency );
                     addedJarFromLibs = true;
                 }
             }
         }
         catch ( MojoExecutionException e )
         {
             log.debug( "Error extract jars" );
         }
         catch ( ZipException e )
         {
             log.debug( "Error" );
         }
         catch ( IOException e )
         {
             log.debug( "Error" );
         }
    }

    /**
     * Add the dependent library classes to the project classpath.
     */
    private void addClassesToClasspath( UnpackedLibHelper helper, MavenProject project, Artifact artifact )
        throws MavenExecutionException
    {
        // Work out where the dep will be extracted and calculate the file path to the classes jar.
        // This is location where the GenerateSourcesMojo will extract the classes.
        final File classesJar = helper.getUnpackedClassesJar( artifact );
        log.debug( "Adding to classpath : " + classesJar );

        // In order to satisfy the LifecycleDependencyResolver on execution up to a phase that
        // has a Mojo requiring dependency resolution I need to create a dummy classesJar here.
        if ( !classesJar.exists() )
        {
            classesJar.getParentFile().mkdirs();
            try
            {
                final ZipOutputStream zipOutputStream = new ZipOutputStream( new FileOutputStream( classesJar ) );
                zipOutputStream.putNextEntry( new ZipEntry( "dummy" ) );
                zipOutputStream.close();
                log.debug( "Created dummy " + classesJar.getName() + " exist=" + classesJar.exists() );
            }
            catch ( IOException e )
            {
                throw new MavenExecutionException( "Could not add " + classesJar.getName() + " as dependency", e );
            }
        }

        
        // Modify the classpath to use an extracted dex file.  This will overwrite
        // any exisiting dependencies with the same information.
        final Dependency dependency = createSystemScopeDependency( artifact, classesJar, null );
        final Dependency providedJar = findProvidedDependencies( dependency, project );
        if ( providedJar != null ) 
        {
            project.getModel().removeDependency( providedJar );
        }
        project.getModel().addDependency( dependency );
    }

    private Dependency createSystemScopeDependency( Artifact artifact, File location, String suffix )
    {
        String artifactId = artifact.getArtifactId();
        if ( suffix != null )
        {
            artifactId += "_" + suffix;
        }
        final Dependency dependency = new Dependency();
        dependency.setGroupId( artifact.getGroupId() );
        dependency.setArtifactId( artifactId );
        dependency.setVersion( artifact.getBaseVersion() );
        dependency.setScope( Artifact.SCOPE_SYSTEM );
        dependency.setSystemPath( location.getAbsolutePath() );
        return dependency;
    }
    
    private Dependency findProvidedDependencies( Dependency dexDependency, MavenProject project ) 
    {
        for ( Dependency dependency : project.getDependencies() ) 
        {
            if ( dependency.getScope().equals( Artifact.SCOPE_PROVIDED ) ) 
            {
                if ( dependency.getArtifactId().equals( dexDependency.getArtifactId() ) 
                        && dependency.getGroupId().equals( dexDependency.getGroupId() ) 
                        && dependency.getType().equals( dexDependency.getType() ) 
                        && dependency.getVersion().equals( dexDependency.getVersion() ) ) 
                {
                    return dependency;
                }
            }
        }
        return null;
        
    }
    
}
  
