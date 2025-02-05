/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Eclipse Public License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.eclipse.org/org/documents/epl-v10.php
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.sdklib.repository;


/**
 * Public constants used by the repository when saving {@code source.properties}
 * files in local packages.
 * <p>
 * These constants are public and part of the SDK Manager public API.
 * Once published we can't change them arbitrarily since various parts
 * of our build process depend on them.
 * </p>
 */
public class PkgProps {
    /**
     * Private constructor to prevent instantiation
     */
    private PkgProps() {
        // Not instantiable
    }

    /**
     * The string for revision
     */
    // Base Package
    public static final String PKG_REVISION = "Pkg.Revision";           //$NON-NLS-1$
    /**
     * The license of the package
     */
    public static final String PKG_LICENSE = "Pkg.License";            //$NON-NLS-1$
    /**
     * The license reference of the package
     */
    public static final String PKG_LICENSE_REF = "Pkg.LicenseRef";         //$NON-NLS-1$
    /**
     * The description of the package
     */
    public static final String PKG_DESC = "Pkg.Desc";               //$NON-NLS-1$
    /**
     * The URL of the package description
     */
    public static final String PKG_DESC_URL = "Pkg.DescUrl";            //$NON-NLS-1$
    /**
     * The release note of the package
     */
    public static final String PKG_RELEASE_NOTE = "Pkg.RelNote";            //$NON-NLS-1$
    /**
     * The URL of the release note of the package
     */
    public static final String PKG_RELEASE_URL = "Pkg.RelNoteUrl";         //$NON-NLS-1$
    /**
     * The source URL of the package
     */
    public static final String PKG_SOURCE_URL = "Pkg.SourceUrl";          //$NON-NLS-1$
    /**
     * Whether the package is obsolete
     */
    public static final String PKG_OBSOLETE = "Pkg.Obsolete";           //$NON-NLS-1$
    /**
     * Display name of the package, in a list
     */
    public static final String PKG_LIST_DISPLAY = "Pkg.ListDisplay";        //$NON-NLS-1$

    // AndroidVersion

    /**
     * The API level of the platform, if the platform is final
     */
    public static final String VERSION_API_LEVEL = "AndroidVersion.ApiLevel";//$NON-NLS-1$
    /**
     * Code name of the platform if the platform is not final
     */
    public static final String VERSION_CODENAME = "AndroidVersion.CodeName";//$NON-NLS-1$


    // AddonPackage

    /**
     * Static string for the addon name, in a list
     */
    public static final String ADDON_NAME = "Addon.Name";             //$NON-NLS-1$
    /**
     * Static string for the addon name, in a list
     */
    public static final String ADDON_NAME_ID = "Addon.NameId";           //$NON-NLS-1$
    /**
     * Display name of the addon, in a list
     */
    public static final String ADDON_NAME_DISPLAY = "Addon.NameDisplay";      //$NON-NLS-1$

    /**
     * Static string for the addon vendor, in a list
     */
    public static final String ADDON_VENDOR = "Addon.Vendor";           //$NON-NLS-1$
    /**
     * The vendor of the addon, in a list
     */
    public static final String ADDON_VENDOR_ID = "Addon.VendorId";         //$NON-NLS-1$
    /**
     * The vendor of the addon, in a list
     */
    public static final String ADDON_VENDOR_DISPLAY = "Addon.VendorDisplay";    //$NON-NLS-1$

    // DocPackage

    // ExtraPackage

    /**
     * String for extra path
     */
    public static final String EXTRA_PATH = "Extra.Path";             //$NON-NLS-1$
    /**
     * String for extra old paths
     */
    public static final String EXTRA_OLD_PATHS = "Extra.OldPaths";         //$NON-NLS-1$
    /**
     * String for extra min API level
     */
    public static final String EXTRA_MIN_API_LEVEL = "Extra.MinApiLevel";      //$NON-NLS-1$
    /**
     * String for extra project files
     */
    public static final String EXTRA_PROJECT_FILES = "Extra.ProjectFiles";     //$NON-NLS-1$
    /**
     * String for extra vendor
     */
    public static final String EXTRA_VENDOR = "Extra.Vendor";           //$NON-NLS-1$
    /**
     * The vendor of the extra, in a list
     */
    public static final String EXTRA_VENDOR_ID = "Extra.VendorId";         //$NON-NLS-1$
    /**
     * Display name of the extra, in a list
     */
    public static final String EXTRA_VENDOR_DISPLAY = "Extra.VendorDisplay";    //$NON-NLS-1$
    /**
     * String for extra display name
     */
    public static final String EXTRA_NAME_DISPLAY = "Extra.NameDisplay";      //$NON-NLS-1$

    // ILayoutlibVersion

    /**
     * Static string for the layoutlib version, in a list
     */
    public static final String LAYOUTLIB_API = "Layoutlib.Api";          //$NON-NLS-1$
    /**
     * Static string for the layoutlib revision, in a list
     */
    public static final String LAYOUTLIB_REV = "Layoutlib.Revision";     //$NON-NLS-1$

    // MinToolsPackage

    /**
     * Static string for the minimum tools revision of the platform, in a list
     */
    public static final String MIN_TOOLS_REV = "Platform.MinToolsRev";   //$NON-NLS-1$

    // PlatformPackage

    /**
     * Static string for the platform version, in a list
     */
    public static final String PLATFORM_VERSION = "Platform.Version";       //$NON-NLS-1$
    /**
     * Code name of the platform. This has no bearing on the package being a preview or not.
     */
    public static final String PLATFORM_CODENAME = "Platform.CodeName";      //$NON-NLS-1$
    /**
     * Static string for the platform version, in a list
     */
    public static final String PLATFORM_INCLUDED_ABI = "Platform.Included.Abi";  //$NON-NLS-1$

    // ToolPackage

    /**
     * Static string for the minimum platform tools revision of the platform, in a list
     */
    public static final String MIN_PLATFORM_TOOLS_REV = "Platform.MinPlatformToolsRev";//$NON-NLS-1$
    /**
     * Static string for the minimum build tools revision of the platform, in a list
     */
    public static final String MIN_BUILD_TOOLS_REV = "Platform.MinBuildToolsRev"; //$NON-NLS-1$


    // SamplePackage

    /**
     * Static string for the minimum API level of the sample package, in a list
     */
    public static final String SAMPLE_MIN_API_LEVEL = "Sample.MinApiLevel";     //$NON-NLS-1$

    // SystemImagePackage

    /**
     * Static string for the system-image tag, in a list
     */
    public static final String SYS_IMG_ABI = "SystemImage.Abi";        //$NON-NLS-1$
    /**
     * Static string for the system-image tag, in a list
     */
    public static final String SYS_IMG_TAG_ID = "SystemImage.TagId";      //$NON-NLS-1$
    /**
     * Static string for the system-image tag, in a list
     */
    public static final String SYS_IMG_TAG_DISPLAY = "SystemImage.TagDisplay"; //$NON-NLS-1$
}
