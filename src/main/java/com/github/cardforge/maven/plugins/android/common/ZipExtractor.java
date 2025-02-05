package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;
import org.codehaus.plexus.components.io.fileselectors.FileSelector;

import java.io.File;


/**
 * Extract an archive to a given location.
 */
public final class ZipExtractor {
    /**
     * Log instance to use for logging
     */
    private final Log log;

    /**
     * @param log Log instance to use for logging
     */
    public ZipExtractor(Log log) {
        this.log = log;
    }

    /**
     * @param zipFile         the archive to extract
     * @param targetFolder    the target directory to extract to
     * @param suffixToExclude the suffix to exclude from the archive
     * @throws MojoExecutionException if there is a problem extracting the archive
     */
    public void extract(File zipFile, @NonNull File targetFolder, final String suffixToExclude)
            throws MojoExecutionException {
        final UnArchiver unArchiver = new ZipUnArchiver(zipFile);

        targetFolder.mkdirs();

        final FileSelector exclusionFilter = fileInfo -> !fileInfo.getName().endsWith(suffixToExclude);

        unArchiver.setDestDirectory(targetFolder);
        unArchiver.setFileSelectors(new FileSelector[]{exclusionFilter});

        log.debug("Extracting archive to " + targetFolder);
        try {
            unArchiver.extract();
        } catch (ArchiverException e) {
            throw new MojoExecutionException("ArchiverException while extracting " + zipFile.getAbsolutePath()
                    + ". Message: " + e.getLocalizedMessage(), e);
        }
    }
}
