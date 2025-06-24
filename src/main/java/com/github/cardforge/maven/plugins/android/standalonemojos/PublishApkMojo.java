package com.github.cardforge.maven.plugins.android.standalonemojos;

import com.android.annotations.NonNull;
import com.github.cardforge.maven.plugins.android.AbstractPublisherMojo;
import com.github.cardforge.maven.plugins.android.common.AndroidPublisherHelper;
import com.google.api.client.http.FileContent;
import com.google.api.services.androidpublisher.AndroidPublisher;
import com.google.api.services.androidpublisher.model.Apk;
import com.google.api.services.androidpublisher.model.ApkListing;
import com.google.api.services.androidpublisher.model.Track;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.github.cardforge.maven.plugins.android.common.AndroidExtension.APK;

/**
 * @author Joris de Groot
 * @author Benoit Billington
 */
@SuppressWarnings("unused") // used in Maven goal
@Mojo(name = "publish-apk", requiresProject = false)
public class PublishApkMojo extends AbstractPublisherMojo {

    private static final int MAX_CHARS_WHATSNEW = 500;

    @Parameter(property = "android.publisher.track", defaultValue = "alpha")
    private String track;

    @Parameter(property = "android.publisher.apkpath")
    private File apkFile;

    @Parameter(property = "android.publisher.filename.whatsnew", defaultValue = "whatsnew.txt")
    private String fileNameWhatsnew;

    /**
     * {@inheritDoc}
     */
    @Inject
    protected PublishApkMojo(ArtifactResolver artifactResolver, ArtifactHandler artHandler,
                             MavenProjectHelper projectHelper, DependencyGraphBuilder dependencyGraphBuilder) {
        super(artifactResolver, artHandler, projectHelper, dependencyGraphBuilder);
    }

    /**
     * Executes this mojo.
     *
     * @throws org.apache.maven.plugin.MojoExecutionException if an error occurs
     * @throws org.apache.maven.plugin.MojoFailureException   if an error occurs
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (apkFile == null) {
            apkFile = new File(targetDirectory, finalName + "-aligned." + APK);
        }

        String packageName = extractPackageNameFromApk(apkFile);
        getLog().debug("Package name: " + packageName);

        initializePublisher(packageName);
        publishApk(packageName);
    }

    private void publishApk(@NonNull String packageName) throws MojoExecutionException {
        try {
            getLog().info("Starting upload of apk " + apkFile.getAbsolutePath());
            FileContent newApkFile = new FileContent(AndroidPublisherHelper.MIME_TYPE_APK, apkFile);
            Apk apk = edits.apks().upload(packageName, editId, newApkFile).execute();

            List<Integer> versionCodes = new ArrayList<>();
            versionCodes.add(apk.getVersionCode());
            Track newTrack = new Track().setVersionCodes(versionCodes);
            edits.tracks().update(packageName, editId, track, newTrack).execute();

            publishWhatsNew(packageName, edits, editId, apk);

            edits.commit(packageName, editId).execute();
        } catch (Exception e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    private void publishWhatsNew(String packageName, AndroidPublisher.Edits edits, String editId, Apk apk)
            throws IOException {
        warnPlatformDefaultEncoding();

        File[] localeDirs = getLocaleDirs();
        if (localeDirs == null) {
            return;
        }

        for (File localeDir : localeDirs) {
            String recentChanges = readFileWithChecks(localeDir, fileNameWhatsnew,
                    MAX_CHARS_WHATSNEW, "What's new texts are missing.");
            if (recentChanges == null) {
                continue;
            }
            ApkListing newApkListing = new ApkListing().setRecentChanges(recentChanges);
            edits.apklistings()
                    .update(packageName, editId, apk.getVersionCode(), localeDir.getName(), newApkListing)
                    .execute();
        }
    }
}