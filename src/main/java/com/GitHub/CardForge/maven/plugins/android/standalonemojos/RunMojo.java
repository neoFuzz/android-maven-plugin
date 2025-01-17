/*
 * Copyright (C) 2011 Lorenzo Villani
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.cardforge.maven.plugins.android.standalonemojos;

import com.android.ddmlib.AdbCommandRejectedException;
import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.ShellCommandUnresponsiveException;
import com.android.ddmlib.TimeoutException;
import com.github.cardforge.maven.plugins.android.AbstractAndroidMojo;
import com.github.cardforge.maven.plugins.android.DeviceCallback;
import com.github.cardforge.maven.plugins.android.common.DeviceHelper;
import com.github.cardforge.maven.plugins.android.config.ConfigHandler;
import com.github.cardforge.maven.plugins.android.config.ConfigPojo;
import com.github.cardforge.maven.plugins.android.config.PullParameter;
import com.github.cardforge.maven.plugins.android.configuration.Run;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import java.io.IOException;

import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.APK;

import java.io.BufferedReader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;

/**
 * Runs the first Activity shown in the top-level launcher as determined by its Intent filters.
 * <p>
 * Android provides a component-based architecture, which means that there is no "main" function which serves as an
 * entry point to the APK. There's an homogeneous collection of Activity(es), Service(s), Receiver(s), etc.
 * </p>
 * <p>
 * The Android top-level launcher (whose purpose is to allow users to launch other applications) uses the Intent
 * resolution mechanism to determine which Activity(es) to show to the end user. Such activities are identified by at
 * least:
 * <ul>
 * <li>Action type: <code>android.intent.action.MAIN</code></li>
 * <li>Category: <code>android.intent.category.LAUNCHER</code></li>
 * </ul>
 * </p>
 * <p>And are declared in <code>AndroidManifest.xml</code> as such:</p>
 * <pre>
 * &lt;activity android:name=".ExampleActivity"&gt;
 *     &lt;intent-filter&gt;
 *         &lt;action android:name="android.intent.action.MAIN" /&gt;
 *         &lt;category android:name="android.intent.category.LAUNCHER" /&gt;
 *     &lt;/intent-filter&gt;
 * &lt;/activity&gt;
 * </pre>
 * <p>
 * This {@link Mojo} will try to to launch the first activity of this kind found in <code>AndroidManifest.xml</code>. In
 * case multiple activities satisfy the requirements listed above only the first declared one is run. In case there are
 * no "Launcher activities" declared in the manifest or no activities declared at all, this goal aborts throwing an
 * error.
 * </p>
 * <p>
 * The device parameter is taken into consideration so potentially the Activity found is started on all attached
 * devices. The application will NOT be deployed and running will silently fail if the application is not deployed.
 * </p>
 *
 * @author Lorenzo Villani - lorenzo@villani.me
 * @author Manfred Moser - manfred@simpligility.com
 * @see "http://developer.android.com/guide/topics/fundamentals.html"
 * @see "http://developer.android.com/guide/topics/intents/intents-filters.html"
 */
@Mojo( name = "run" )
public class RunMojo extends AbstractAndroidMojo
{

    /**
     * <p>The configuration for the run goal can be set up in the plugin configuration in the pom file as:</p>
     * <pre>
     * &lt;run&gt;
     *     &lt;debug&gt;true|false|portnumber&lt;/debug&gt;
     * &lt;/run&gt;
     * </pre>
     * <p>The <code>&lt;debug&gt;</code> parameter is optional and defaults to false. Numeric values like 5432 are 
     * parsed as port number. 
     * <p>The debug parameter can also be configured as property in the pom or settings file
     * <pre>
     * &lt;properties&gt;
     *     &lt;android.run.debug&gt;true&lt;/android.run.debug&gt;
     * &lt;/properties&gt;
     * </pre>
     * or from command-line with parameter <code>-Dandroid.run.debug=true</code>.</p>
     */
    @Parameter
    @ConfigPojo
    private Run run;

    /**
     * Debug parameter for the the run goal. If true, the device or emulator will pause execution of the process at
     * startup to wait for a debugger to connect. Also see the "run" parameter documentation. Default value is false.
     * If the value is numeric, it is treated as a port number to forward the JDWP protocol
     * of the launched process to.
     */
    @Parameter( property = "android.run.debug" )
    protected String runDebug;

    /* the value for the debug flag after parsing pom and parameter */
    @PullParameter( defaultValue = "false" )
    private String parsedDebug;

    /**
     * Holds information about the "Launcher" activity.
     *
     * @author Lorenzo Villani
     */
    private static class LauncherInfo
    {
        private String packageName;

        private String activity;

        public String getPackageName()
        {
            return packageName;
        }

        public void setPackageName( String packageName )
        {
            this.packageName = packageName;
        }

        public String getActivity()
        {
            return activity;
        }

        public void setActivity( String activity )
        {
            this.activity = activity;
        }
    }

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException
    {
        if ( project.getPackaging().equals( APK ) )
        {
            try
            {
                LauncherInfo launcherInfo;

                launcherInfo = getLauncherActivity();

                ConfigHandler configHandler = new ConfigHandler( this, this.session, this.execution );
                configHandler.parseConfiguration();

                launch( launcherInfo );
            }
            catch ( Exception ex )
            {
                getLog().info( "Unable to run launcher Activity" );
                getLog().debug( ex );
            }
        }
        else
        {
            getLog().info( "Project packaging is not apk, skipping run action." );
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Gets the first "Launcher" Activity by running an XPath query on <code>AndroidManifest.xml</code>.
     *
     * @return A {@link LauncherInfo}
     * @throws MojoFailureException
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws XPathExpressionException
     */
    private LauncherInfo getLauncherActivity()
            throws ParserConfigurationException, SAXException, IOException, XPathExpressionException,
            MojoFailureException
    {
        Document document;
        DocumentBuilder documentBuilder;
        DocumentBuilderFactory documentBuilderFactory;
        Object result;
        XPath xPath;
        XPathExpression xPathExpression;
        XPathFactory xPathFactory;

        //
        // Setup JAXP stuff
        //
        documentBuilderFactory = DocumentBuilderFactory.newInstance();

        documentBuilder = documentBuilderFactory.newDocumentBuilder();

        document = documentBuilder.parse( destinationManifestFile );

        xPathFactory = XPathFactory.newInstance();

        xPath = xPathFactory.newXPath();

        xPathExpression = xPath.compile(
                "//manifest/application/activity/intent-filter[action[@name=\"android.intent.action.MAIN\"] "
                + "and category[@name=\"android.intent.category.LAUNCHER\"]]/.." );

        //
        // Run XPath query
        //
        result = xPathExpression.evaluate( document, XPathConstants.NODESET );

        if ( result instanceof NodeList )
        {
            NodeList activities;

            activities = ( NodeList ) result;

            if ( activities.getLength() > 0 )
            {
                // Grab the first declared Activity
                LauncherInfo launcherInfo;

                launcherInfo = new LauncherInfo();
                String activityName = activities.item( 0 ).getAttributes().getNamedItem( "android:name" )
                        .getNodeValue();

                if ( ! activityName.contains( "." ) )
                {
                    activityName = "." + activityName;
                }

                if ( activityName.startsWith( "." ) )
                {
                    String packageName = document.getElementsByTagName( "manifest" ).item( 0 ).getAttributes()
                            .getNamedItem( "package" ).getNodeValue();
                    activityName = packageName + activityName;
                }

                launcherInfo.activity = activityName;

                launcherInfo.packageName = renameManifestPackage != null
                    ? renameManifestPackage
                    : document.getDocumentElement().getAttribute( "package" ).toString();

                return launcherInfo;
            }
            else
            {
                // If we get here, we couldn't find a launcher activity.
                throw new MojoFailureException( "Could not find a launcher activity in manifest" );
            }
        }
        else
        {
            // If we get here we couldn't find any Activity
            throw new MojoFailureException( "Could not find any activity in manifest" );
        }
    }

    /**
     * Executes the "Launcher activity".
     *
     * @param info A {@link LauncherInfo}.
     * @throws MojoFailureException
     * @throws MojoExecutionException
     */
    private void launch( final LauncherInfo info ) throws MojoExecutionException, MojoFailureException
    {
        final String command;
        
        final int debugPort = findDebugPort();

        command = String.format( "am start %s-n %s/%s", debugPort >= 0 ? "-D " : "", info.packageName, info.activity );

        doWithDevices( new DeviceCallback()
        {
            @Override
            public void doWithDevice( IDevice device ) throws MojoExecutionException, MojoFailureException
            {
                String deviceLogLinePrefix = DeviceHelper.getDeviceLogLinePrefix( device );

                try
                {
                    getLog().info( deviceLogLinePrefix + "Attempting to start " + info.packageName + "/" 
                            + info.activity );

                    CollectingOutputReceiver shellOutput = new CollectingOutputReceiver();
                    device.executeShellCommand( command, shellOutput );
                    if ( shellOutput.getOutput().contains( "Error" ) )
                    {
                        throw new MojoFailureException( shellOutput.getOutput() );
                    }
                    if ( debugPort > 0 ) 
                    {
                        int pid = findPid( device, "ps" );
                        if ( pid == -1 )
                        {
                            pid = findPid( device, "ps -Af" );
                        }
                        if ( pid == -1 )
                        {
                            throw new MojoFailureException( "Cannot find stated process " + info.packageName );
                        }
                        getLog().info(
                            deviceLogLinePrefix + "Process " + debugPort + " launched"
                        );
                        try 
                        {
                            createForward( device, debugPort, pid );
                            getLog().info(
                                deviceLogLinePrefix + "Debugger listening on " + debugPort
                            );
                        } 
                        catch ( Exception ex ) 
                        {
                            throw new MojoFailureException( 
                                "Cannot create forward tcp: " + debugPort 
                                    + " jdwp: " + pid, ex 
                            );
                        }
                    }
                }
                catch ( IOException ex )
                {
                    throw new MojoFailureException( deviceLogLinePrefix + "Input/Output error", ex );
                }
                catch ( TimeoutException ex )
                {
                    throw new MojoFailureException( deviceLogLinePrefix + "Command timeout", ex );
                }
                catch ( AdbCommandRejectedException ex )
                {
                    throw new MojoFailureException( deviceLogLinePrefix + "ADB rejected the command", ex );
                }
                catch ( ShellCommandUnresponsiveException ex )
                {
                    throw new MojoFailureException( deviceLogLinePrefix + "Unresponsive command", ex );
                }
            }

            private int findPid( IDevice device, final String cmd )
            throws IOException, TimeoutException, AdbCommandRejectedException, ShellCommandUnresponsiveException
            {
                CollectingOutputReceiver processOutput = new CollectingOutputReceiver();
                device.executeShellCommand( cmd, processOutput );
                BufferedReader r = new BufferedReader( new StringReader( processOutput.getOutput() ) );
                int pid = -1;
                for ( ;; )
                {
                    String line = r.readLine();
                    if ( line == null )
                    {
                        break;
                    }
                    if ( line.endsWith( info.packageName ) )
                    {
                        String[] values = line.split( " +" );
                        if ( values.length > 2 )
                        {
                            pid = Integer.valueOf( values[1] );
                            break;
                        }
                    }
                }
                r.close();
                return pid;
            }
        } );
    }
    
    private static void createForward( IDevice device, int debugPort, int pid ) 
    throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException
    {
        Method m = Class.forName( "com.android.ddmlib.AdbHelper" ).
            getDeclaredMethod(
                "createForward", InetSocketAddress.class, 
                device.getClass(), String.class, String.class
            );
        m.setAccessible( true );
        m.invoke(
            null, AndroidDebugBridge.getSocketAddress(), device,
            String.format( "tcp:%d", debugPort ), String.format( "jdwp:%d", pid )
        );
    }

    private int findDebugPort()
    {
        int debugPort;
        if ( "true".equals( parsedDebug ) )
        {
            debugPort = 0;
        }
        else
        {
            try
            {
                debugPort = Integer.parseInt( parsedDebug );
            }
            catch ( NumberFormatException ex )
            {
                debugPort = -1;
            }
        }
        return debugPort;
    }
}
