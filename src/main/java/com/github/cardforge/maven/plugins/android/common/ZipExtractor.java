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
    private final Log log;

    public ZipExtractor(Log log) {
        this.log = log;
    }

    public void extract(File zipFile, @NonNull File targetFolder, final String suffixToExclude) throws MojoExecutionException {
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
