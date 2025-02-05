/*
 * Copyright (C) 2011 simpligility technologies inc.
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
package com.github.cardforge.maven.plugins.android.common;


import com.android.ddmlib.SyncService;
import org.apache.maven.plugin.logging.Log;

/**
 * LogSyncProgressMonitor is an implementation of the ISyncProgressMonitor
 * from the Android ddmlib that logs to the Maven Plugin log passed into it.
 *
 * @author Manfred Moser - manfred@simpligility.com
 */
public class LogSyncProgressMonitor implements SyncService.ISyncProgressMonitor {
    /**
     * The indent to use for sub-tasks.
     */
    private static final String INDENT = "  ";
    /**
     * The Maven Plugin logger to log to.
     */
    private final Log log;

    /**
     * @param log the Maven Plugin logger to log to.
     */
    public LogSyncProgressMonitor(Log log) {
        this.log = log;
    }

    /**
     * @param totalWork the total amount of work.
     */
    public void start(int totalWork) {
        log.info("Starting transfer of " + totalWork + ". See debug log for progress");
    }

    /**
     * Show the transfer has stopped
     */
    public void stop() {
        log.info("Stopped transfer");
    }

    /**
     * @return true if the operation was canceled.
     */
    public boolean isCanceled() {
        return false;
    }

    /**
     * @param name the name of the sub-task.
     */
    public void startSubTask(String name) {
        log.info(INDENT + "Started sub task " + name);
    }

    /**
     * @param work the amount of work done.
     */
    public void advance(int work) {
        log.debug(INDENT + "Transferred " + work);
    }
}