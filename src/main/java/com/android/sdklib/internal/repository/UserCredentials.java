/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.sdklib.internal.repository;

/**
 * @deprecated com.android.sdklib.internal.repository has moved into Studio as
 * com.android.tools.idea.sdk.remote.internal.
 */
@Deprecated
public class UserCredentials {
    private final String mUserName;
    private final String mPassword;
    private final String mWorkstation;
    private final String mDomain;


    /**
     * Constructor for UserCredentials.
     *
     * @param userName    The username for authentication
     * @param password    The password for authentication
     * @param workstation The workstation name
     * @param domain      The domain name
     */
    public UserCredentials(String userName, String password, String workstation, String domain) {
        mUserName = userName;
        mPassword = password;
        mWorkstation = workstation;
        mDomain = domain;
    }

    /**
     * @return the username, or null if not set
     */
    public String getUserName() {
        return mUserName;
    }

    /**
     * @return the password, or null if not set
     */
    public String getPassword() {
        return mPassword;
    }

    /**
     * @return the workstation, or null if not set
     */
    public String getWorkstation() {
        return mWorkstation;
    }

    /**
     * @return the domain, or null if not set
     */
    public String getDomain() {
        return mDomain;
    }
}
