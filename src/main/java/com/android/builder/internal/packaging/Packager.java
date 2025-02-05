/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.builder.internal.packaging;

import com.android.SdkConstants;
import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.builder.internal.packaging.JavaResourceProcessor.IArchiveBuilder;
import com.android.builder.model.PackagingOptions;
import com.android.builder.packaging.DuplicateFileException;
import com.android.builder.packaging.PackagerException;
import com.android.builder.packaging.SealedPackageException;
import com.android.builder.signing.SignedJarBuilder;
import com.android.builder.signing.SignedJarBuilder.IZipEntryFilter;
import com.android.ide.common.signing.CertificateInfo;
import com.android.utils.FileUtils;
import com.android.utils.ILogger;
import com.google.common.io.Closeables;
import com.google.common.io.Files;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.regex.Pattern;

import static com.android.SdkConstants.FN_APK_CLASSES_N_DEX;

/**
 * Class making the final app package.
 * The inputs are:
 * - packaged resources (output of aapt)
 * - code file (output of dx)
 * - Java resources coming from the project, its libraries, and its jar files
 * - Native libraries from the project or its library.
 */
public final class Packager implements IArchiveBuilder {

    /**
     * Static failed message
     */
    public static final String FAILED_TO_ADD_S = "Failed to add %s";
    /**
     * Static sealed message
     */
    public static final String ALREADY_SEALED = "APK is already sealed";
    /**
     * Static pattern used for nativelibs
     */
    private static final Pattern PATTERN_NATIVELIB_EXT = Pattern.compile("^.+\\.so$",
            Pattern.CASE_INSENSITIVE);
    /**
     * Logger instance
     */
    private final ILogger mLogger;
    /**
     * internal Zip filter
     */
    private final DuplicateZipFilter mNoDuplicateFilter = new DuplicateZipFilter();
    /**
     * internal Zip filter
     */
    private final NoBinaryZipFilter mNoBinaryZipFilter = new NoBinaryZipFilter(mNoDuplicateFilter);
    /**
     * internal packaging filter
     */
    @Nullable
    private final SignedJarBuilder.IZipEntryFilter mPackagingOptionsFilter;
    /**
     * Files to add
     */
    private final HashMap<String, File> mAddedFiles = new HashMap<>();
    /**
     * Files to merge
     */
    private final HashMap<String, File> mMergeFiles = new HashMap<>();
    /**
     * The manifest file to create. If null, no manifest will be created.
     */
    private SignedJarBuilder mBuilder = null;
    /**
     * JNI debug mode. If true, the APK will be built with JNI debug symbols.
     */
    private boolean mJniDebugMode = false;
    /**
     * Whether the APK has already been sealed
     */
    private boolean mIsSealed = false;

    /**
     * Creates a new instance.
     * <p>
     * This creates a new builder that will create the specified output file, using the two
     * mandatory given input files.
     * <p>
     * An optional debug keystore can be provided. If set, it is expected that the store password
     * is {@code android} and the key alias and password are {@code androiddebugkey} and {@code android}.
     * <p>
     * An optional {@link ILogger} can also be provided for verbose output. If null, there will
     * be no output.
     *
     * @param apkLocation            the file to create
     * @param resLocation            the file representing the packaged resource file.
     * @param mergingFolder          the folder to store files that are being merged.
     * @param certificateInfo        the signing information used to sign the package. Optional the OS path to the debug keystore, if needed or null.
     * @param createdBy              the creator of the APK, or null
     * @param packagingOptions       the packaging options, or null
     * @param packagingOptionsFilter the packaging options filter, or null
     * @param logger                 the logger.
     * @throws com.android.builder.packaging.PackagerException if an error occurred
     */
    public Packager(
            @NonNull String apkLocation,
            @NonNull String resLocation,
            @NonNull File mergingFolder,
            CertificateInfo certificateInfo,
            @Nullable String createdBy,
            @Nullable PackagingOptions packagingOptions,
            @Nullable SignedJarBuilder.IZipEntryFilter packagingOptionsFilter,
            ILogger logger) throws PackagerException {

        try {
            File apkFile = new File(apkLocation);
            checkOutputFile(apkFile);

            File resFile = new File(resLocation);
            checkInputFile(resFile);

            checkMergingFolder(mergingFolder);
            if (packagingOptions != null) {
                for (String merge : packagingOptions.getMerges()) {
                    mMergeFiles.put(merge, new File(mergingFolder, merge));
                }
            }

            mPackagingOptionsFilter = packagingOptionsFilter;
            mLogger = logger;

            mBuilder = new SignedJarBuilder(
                    new FileOutputStream(apkFile, false /* append */),
                    certificateInfo != null ? certificateInfo.getKey() : null,
                    certificateInfo != null ? certificateInfo.getCertificate() : null,
                    getLocalVersion(),
                    createdBy);

            mLogger.verbose("Packaging %s", apkFile.getName());

            // add the resources
            addZipFile(resFile);

        } catch (PackagerException e) {
            if (mBuilder != null) {
                mBuilder.cleanUp();
            }
            throw e;
        } catch (Exception e) {
            if (mBuilder != null) {
                mBuilder.cleanUp();
            }
            throw new PackagerException(e);
        }
    }

    /**
     * Checks an output {@link File} object.
     * This checks the following:
     * - the file is not an existing directory.
     * - if the file exists, that it can be modified.
     * - if it doesn't exist, that a new file can be created.
     *
     * @param file the File to check
     * @throws PackagerException If the check fails
     */
    private static void checkOutputFile(@NonNull File file) throws PackagerException {
        if (file.isDirectory()) {
            throw new PackagerException("%s is a directory!", file);
        }

        if (file.exists()) { // will be a file in this case.
            if (!file.canWrite()) {
                throw new PackagerException("Cannot write %s", file);
            }
        } else {
            try {
                if (!file.createNewFile()) {
                    throw new PackagerException("Failed to create %s", file);
                }
            } catch (IOException e) {
                throw new PackagerException(
                        "Failed to create '%1$ss': %2$s", file, e.getMessage());
            }
        }
    }

    /**
     * Checks the merger folder is:
     * - a directory.
     * - if the folder exists, that it can be modified.
     * - if it doesn't exist, that a new folder can be created.
     *
     * @param file the File to check
     * @throws PackagerException If the check fails
     */
    private static void checkMergingFolder(@NonNull File file) throws PackagerException {
        if (file.isFile()) {
            throw new PackagerException("%s is a file!", file);
        }

        if (file.exists() && !file.canWrite()) {
            throw new PackagerException("Cannot write %s", file);
        }


        try {
            FileUtils.emptyFolder(file);
        } catch (IOException e) {
            throw new PackagerException(e);
        }
    }

    /**
     * Checks an input {@link File} object.
     * This checks the following:
     * - the file is not an existing directory.
     * - that the file exists (if <var>throwIfDoesntExist</var> is <code>false</code>) and can
     * be read.
     *
     * @param file the File to check
     * @throws FileNotFoundException if the file is not here.
     * @throws PackagerException     If the file is a folder or a file that cannot be read.
     */
    private static void checkInputFile(@NonNull File file) throws FileNotFoundException, PackagerException {
        if (file.isDirectory()) {
            throw new PackagerException("%s is a directory!", file);
        }

        if (file.exists()) {
            if (!file.canRead()) {
                throw new PackagerException("Cannot read %s", file);
            }
        } else {
            throw new FileNotFoundException(String.format("%s does not exist", file));
        }
    }

    /**
     * @return the local version of the builder, or null if it cannot be determined.
     */
    @Nullable
    public static String getLocalVersion() {
        Class clazz = Packager.class;
        String className = clazz.getSimpleName() + ".class";
        String classPath = clazz.getResource(className).toString();
        if (!classPath.startsWith("jar")) {
            // Class not from JAR, unlikely
            return null;
        }
        try {
            String manifestPath = classPath.substring(0, classPath.lastIndexOf('!') + 1) +
                    "/META-INF/MANIFEST.MF";

            URLConnection jarConnection = new URL(manifestPath).openConnection();
            jarConnection.setUseCaches(false);
            InputStream jarInputStream = jarConnection.getInputStream();
            Attributes attr = new Manifest(jarInputStream).getMainAttributes();
            jarInputStream.close();
            return attr.getValue("Builder-Version");
        } catch (IOException ignored) {
            // Ignored
        }

        return null;
    }

    /**
     * @param mainDexFolder the folder containing the main dex files.
     * @param extraDexFiles the extra dex files to add.
     * @throws DuplicateFileException if a file conflicts with another already added to the APK
     * @throws SealedPackageException if the APK is already sealed
     * @throws PackagerException      if an error occurred
     */
    public void addDexFiles(@NonNull File mainDexFolder, @NonNull Collection<File> extraDexFiles)
            throws DuplicateFileException, SealedPackageException, PackagerException {

        File[] mainDexFiles = mainDexFolder.listFiles((file, name) -> name.endsWith(SdkConstants.DOT_DEX));

        if (mainDexFiles != null && mainDexFiles.length > 0) {
            // Never rename the dex files in the main dex folder, in case we are in legacy mode
            // we require the main dex files to not be renamed.
            for (File dexFile : mainDexFiles) {
                addFile(dexFile, dexFile.getName());
            }

            // prepare the index for the next files.
            int dexIndex = mainDexFiles.length + 1;

            for (File dexFile : extraDexFiles) {
                addFile(dexFile, String.format(FN_APK_CLASSES_N_DEX, dexIndex++));
            }
        }
    }

    /**
     * Sets the JNI debug mode. In debug mode, when native libraries are present, the packaging
     * will also include one or more copies of gdbserver in the final APK file.
     * <p>
     * These are used for debugging native code, to ensure that gdbserver is accessible to the
     * application.
     * <p>
     * There will be one version of gdbserver for each ABI supported by the application.
     * <p>
     * the gbdserver files are placed in the libs/abi/ folders automatically by the NDK.
     *
     * @param jniDebugMode the jni-debug mode flag.
     */
    public void setJniDebugMode(boolean jniDebugMode) {
        mJniDebugMode = jniDebugMode;
    }

    /**
     * Adds a file to the APK at a given path
     *
     * @param file        the file to add
     * @param archivePath the path of the file inside the APK archive.
     * @throws PackagerException                                    if an error occurred
     * @throws com.android.builder.packaging.SealedPackageException if the APK is already sealed.
     * @throws DuplicateFileException                               if a file conflicts with another already added to the APK
     *                                                              at the same location inside the APK archive.
     */
    @Override
    public void addFile(File file, String archivePath) throws PackagerException,
            SealedPackageException, DuplicateFileException {
        if (mIsSealed) {
            throw new SealedPackageException(ALREADY_SEALED);
        }

        try {
            doAddFile(file, archivePath);
        } catch (DuplicateFileException e) {
            mBuilder.cleanUp();
            throw e;
        } catch (Exception e) {
            mBuilder.cleanUp();
            throw new PackagerException(e, FAILED_TO_ADD_S, file);
        }
    }

    /**
     * Adds the content from a zip file.
     * All file keep the same path inside the archive.
     *
     * @param zipFile the zip File.
     * @throws PackagerException      if an error occurred
     * @throws SealedPackageException if the APK is already sealed.
     * @throws DuplicateFileException if a file conflicts with another already added to the APK
     *                                at the same location inside the APK archive.
     */
    void addZipFile(File zipFile) throws PackagerException, SealedPackageException,
            DuplicateFileException {
        if (mIsSealed) {
            throw new SealedPackageException(ALREADY_SEALED);
        }

        FileInputStream fis = null;
        try {
            mLogger.verbose("%s:", zipFile);

            // reset the filter with this input.
            mNoDuplicateFilter.reset(zipFile);

            // ask the builder to add the content of the file.
            fis = new FileInputStream(zipFile);
            mBuilder.writeZip(fis, mNoDuplicateFilter, null /* ZipEntryExtractor */);
        } catch (DuplicateFileException e) {
            mBuilder.cleanUp();
            throw e;
        } catch (Exception e) {
            mBuilder.cleanUp();
            throw new PackagerException(e, FAILED_TO_ADD_S, zipFile);
        } finally {
            try {
                Closeables.close(fis, true /* swallowIOException */);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    /**
     * Adds all resources from a merged folder or jar file. There cannot be any duplicates and all
     * files present must be added unless it is a "binary" file like a .class or .dex (jack
     * produces the {@code classes.dex} in the same location as the obfuscated resources).
     *
     * @param jarFileOrDirectory a jar file or directory reference.
     * @throws PackagerException      could not add an entry to the package.
     * @throws DuplicateFileException if an entry with the same name was already present in the
     *                                package being built while adding the jarFileOrDirectory content.
     */
    public void addResources(@NonNull File jarFileOrDirectory)
            throws PackagerException, DuplicateFileException {

        mNoDuplicateFilter.reset(jarFileOrDirectory);
        InputStream fis = null;
        try {
            if (jarFileOrDirectory.isDirectory()) {
                addResourcesFromDirectory(jarFileOrDirectory, "");
            } else {
                fis = new BufferedInputStream(new FileInputStream(jarFileOrDirectory));
                mBuilder.writeZip(fis, mNoBinaryZipFilter, null /* ZipEntryExtractor */);
            }
        } catch (DuplicateFileException e) {
            mBuilder.cleanUp();
            throw e;
        } catch (Exception e) {
            mBuilder.cleanUp();
            throw new PackagerException(e, FAILED_TO_ADD_S, jarFileOrDirectory);
        } finally {
            try {
                if (fis != null) {
                    Closeables.close(fis, true /* swallowIOException */);
                }
            } catch (IOException e) {
                // ignore.
            }
        }
    }

    /**
     * Adds all resources from a directory.
     *
     * @param directory the directory to add.
     * @param path      the path of the directory inside the APK archive.
     * @throws IZipEntryFilter.ZipAbortException if the file should not be added.
     * @throws IOException                       if an error occurred
     */
    private void addResourcesFromDirectory(@NonNull File directory, String path)
            throws IOException, IZipEntryFilter.ZipAbortException {
        File[] directoryFiles = directory.listFiles();
        if (directoryFiles == null) {
            return;
        }
        for (File file : directoryFiles) {
            String entryName = path.isEmpty() ? file.getName() : path + "/" + file.getName();
            if (file.isDirectory()) {
                addResourcesFromDirectory(file, entryName);
            } else {
                doAddFile(file, entryName);
            }
        }
    }

    /**
     * Adds the native libraries from the top native folder.
     * The content of this folder must be the various ABI folders.
     * <p>
     * This may or may not copy gdbserver into the apk based on whether the debug mode is set.
     *
     * @param nativeFolder the root folder containing the abi folders which contain the .so
     * @param abiFilters   a list of abi filters to include. If null or empty, all abis are included.
     * @throws PackagerException      if an error occurred
     * @throws SealedPackageException if the APK is already sealed.
     * @see #setJniDebugMode(boolean)
     */
    public void addNativeLibraries(@NonNull File nativeFolder, @Nullable Set<String> abiFilters)
            throws PackagerException, SealedPackageException {
        if (mIsSealed) {
            throw new SealedPackageException(ALREADY_SEALED);
        }

        if (!nativeFolder.isDirectory()) {
            // not a directory? check if it's a file or doesn't exist
            throw new PackagerException(nativeFolder.exists() ?
                    "%s is not a folder" : "%s does not exist", nativeFolder);
        }

        File[] abiList = nativeFolder.listFiles();

        mLogger.verbose("Native folder: %s", nativeFolder);

        if (abiList != null) {
            for (File abi : abiList) {
                if (abiFilters != null && !abiFilters.isEmpty() && !abiFilters.contains(abi.getName())) {
                    continue;
                }

                if (abi.isDirectory()) { // ignore files
                    File[] libs = abi.listFiles();
                    if (libs != null) {
                        for (File lib : libs) {
                            // only consider files that are .so or, if in debug mode, that
                            // are gdbserver executables
                            String libName = lib.getName();
                            if (lib.isFile() &&
                                    (PATTERN_NATIVELIB_EXT.matcher(lib.getName()).matches() ||
                                            (mJniDebugMode &&
                                                    (SdkConstants.FN_GDBSERVER.equals(libName) ||
                                                            SdkConstants.FN_GDB_SETUP.equals(libName))))) {

                                String path = SdkConstants.FD_APK_NATIVE_LIBS + File.separatorChar +
                                        abi.getName() + File.separatorChar + libName;

                                try {
                                    if (mPackagingOptionsFilter == null
                                            || mPackagingOptionsFilter.checkEntry(path)) {
                                        doAddFile(lib, path);
                                    }
                                } catch (Exception e) {
                                    mBuilder.cleanUp();
                                    throw new PackagerException(e, FAILED_TO_ADD_S, lib);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Seals the APK, and signs it if necessary.
     *
     * @throws PackagerException      if an error occurred
     * @throws SealedPackageException if the APK is already sealed.
     */
    public void sealApk() throws PackagerException, SealedPackageException {
        if (mIsSealed) {
            throw new SealedPackageException(ALREADY_SEALED);
        }

        // Add all the merged files that are pending to be packaged.
        for (Map.Entry<String, File> entry : mMergeFiles.entrySet()) {
            File inputFile = entry.getValue();
            String archivePath = entry.getKey();
            try {
                if (inputFile.exists()) {
                    mBuilder.writeFile(inputFile, archivePath);
                }
            } catch (IOException e) {
                mBuilder.cleanUp();
                throw new PackagerException(e, "Failed to add merged file %s", inputFile);
            }
        }

        // close and sign the application package.
        try {
            mBuilder.close();
            mIsSealed = true;
        } catch (Exception e) {
            throw new PackagerException(e, "Failed to seal APK");
        } finally {
            mBuilder.cleanUp();
        }
    }

    /**
     * Adds a file to the APK at a given path
     *
     * @param file        the file to add
     * @param archivePath the path of the file inside the APK archive.
     * @throws IZipEntryFilter.ZipAbortException if the file should not be added.
     * @throws IOException                       if an error occurred
     */
    private void doAddFile(File file, String archivePath) throws IZipEntryFilter.ZipAbortException,
            IOException {

        // If a file has to be merged, write it to a file in the merging folder and add it later.
        if (mMergeFiles.keySet().contains(archivePath)) {
            File mergingFile = mMergeFiles.get(archivePath);
            Files.createParentDirs(mergingFile);
            try (FileOutputStream fos = new FileOutputStream(mMergeFiles.get(archivePath), true)) {
                fos.write(Files.toByteArray(file));
            }
        } else {
            if (!mNoBinaryZipFilter.checkEntry(archivePath)) {
                return;
            }
            mAddedFiles.put(archivePath, file);
            mBuilder.writeFile(file, archivePath);
        }
    }

    /**
     * Checks if the given path in the APK archive has not already been used and if it has been,
     * then returns a {@link File} object for the source of the duplicate
     *
     * @param archivePath the archive path to test.
     * @return A File object of either a file at the same location or an archive that contains a
     * file that was put at the same location.
     */
    private File checkFileForDuplicate(String archivePath) {
        return mAddedFiles.get(archivePath);
    }

    /**
     * A filter to filter out binary files like .class
     */
    private static final class NoBinaryZipFilter implements IZipEntryFilter {
        @NonNull
        private final IZipEntryFilter parentFilter;

        private NoBinaryZipFilter(@NonNull IZipEntryFilter parentFilter) {
            this.parentFilter = parentFilter;
        }


        @Override
        public boolean checkEntry(String archivePath) throws ZipAbortException {
            return parentFilter.checkEntry(archivePath) && !archivePath.endsWith(".class");
        }
    }

    /**
     * Filter to detect duplicate entries
     */
    private final class DuplicateZipFilter implements IZipEntryFilter {
        private File mInputFile;

        void reset(File inputFile) {
            mInputFile = inputFile;
        }

        @Override
        public boolean checkEntry(String archivePath) throws ZipAbortException {
            mLogger.verbose("=> %s", archivePath);

            File duplicate = checkFileForDuplicate(archivePath);
            if (duplicate != null) {
                // we have a duplicate, but it might be the same source file, in this case,
                // we just ignore the duplicate, and of course, we don't add it again.
                File potentialDuplicate = new File(mInputFile, archivePath);
                if (!duplicate.getAbsolutePath().equals(potentialDuplicate.getAbsolutePath())) {
                    throw new DuplicateFileException(archivePath, duplicate, mInputFile);
                }
                return false;
            } else {
                mAddedFiles.put(archivePath, mInputFile);
            }

            return true;
        }
    }

}
