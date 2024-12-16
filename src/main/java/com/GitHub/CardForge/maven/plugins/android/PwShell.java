/*
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
package com.github.cardforge.maven.plugins.android;

import org.codehaus.plexus.util.Os;
import org.codehaus.plexus.util.cli.shell.Shell;

import java.util.Arrays;
import java.util.List;

/**
 * This class is a wrapper for the Windows PowerShell command line. It is used to execute commands on Windows
 * avoiding the cmd.exe character limit.
 * <p>
 * If run from within linux, it will set the shell to use {@code pwsh}. This would require the installation of
 * PowerShell Core on linux to work.
 * </p>
 *
 * @author neoFuzz
 */
public class PwShell extends Shell {
    /**
     * Default constructor. It will set the shell command to {@code powershell} or {@code pwsh} depending on the
     * operating system.
     */
    public PwShell() {
        if (Os.isFamily("windows")) {
            this.setShellCommand("powershell");
        } else {
            this.setShellCommand("pwsh");
        }

        this.setQuotedExecutableEnabled(true);
        this.setShellArgs(new String[]{"-Command"});
    }

    /**
     * Returns the command line as a list of strings.
     *
     * @param executable the executing program
     * @param arguments  the program arguments
     * @return the command line as a list of strings
     */
    public List<String> getCommandLine(String executable, String[] arguments) {
        StringBuilder sb = new StringBuilder();
        sb.append("'");
        sb.append(super.getCommandLine(executable, arguments).get(0));
        sb.append("'");
        return Arrays.asList(sb.toString());
    }

}
