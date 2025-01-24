/*
 * Copyright (C) 2008 The Android Open Source Project
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
package com.android.sdklib.internal.build;

import com.android.annotations.NonNull;
import com.android.sdklib.internal.build.SignedJarBuilder.IZipEntryFilter.ZipAbortException;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.CMSSignedDataGenerator;
import org.bouncycastle.cms.CMSTypedData;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * A Jar file builder with signature support.
 *
 * @deprecated Use Android-Builder instead
 */
@Deprecated(since = "26.1.0")
public class SignedJarBuilder {
    private static final String DIGEST_ALGORITHM = "SHA1";
    private static final String DIGEST_ATTR = "SHA1-Digest";
    private static final String DIGEST_MANIFEST_ATTR = "SHA1-Digest-Manifest";
    private JarOutputStream mOutputJar;
    private final PrivateKey mKey;
    private final X509Certificate mCertificate;
    private Manifest mManifest;
    private MessageDigest mMessageDigest;
    private final byte[] mBuffer = new byte[4096];

    /**
     * Creates a {@link SignedJarBuilder} with a given output stream, and signing information.
     * <p>If either <code>key</code> or <code>certificate</code> is <code>null</code> then
     * the archive will not be signed.
     *
     * @param out         the {@link OutputStream} where to write the Jar archive.
     * @param key         the {@link PrivateKey} used to sign the archive, or <code>null</code>.
     * @param certificate the {@link X509Certificate} used to sign the archive, or
     *                    <code>null</code>.
     * @throws IOException              if an I/O error occurs.
     * @throws NoSuchAlgorithmException if the algorithm used for the signature is not available.
     */
    public SignedJarBuilder(OutputStream out, PrivateKey key, X509Certificate certificate)
            throws IOException, NoSuchAlgorithmException {
        mOutputJar = new JarOutputStream(new BufferedOutputStream(out));
        mOutputJar.setLevel(9);
        mKey = key;
        mCertificate = certificate;
        if (mKey != null && mCertificate != null) {
            mManifest = new Manifest();
            Attributes main = mManifest.getMainAttributes();
            main.putValue("Manifest-Version", "1.0");
            main.putValue("Created-By", "1.0 (Android)");
            mMessageDigest = MessageDigest.getInstance(DIGEST_ALGORITHM);
        }
    }

    /**
     * Writes a new {@link File} into the archive.
     *
     * @param inputFile the {@link File} to write.
     * @param jarPath   the filepath inside the archive.
     * @throws IOException if an I/O error occurs.
     */
    public void writeFile(@NonNull File inputFile, String jarPath) throws IOException {
        // Get an input stream on the file.
        try (FileInputStream fis = new FileInputStream(inputFile)) {
            // create the zip entry
            JarEntry entry = new JarEntry(jarPath);
            entry.setTime(inputFile.lastModified());
            writeEntry(fis, entry);
        }
        // Finally, close the file stream used to read the file
    }

    /**
     * Copies the content of a Jar/Zip archive into the receiver archive.
     * <p>An optional {@link IZipEntryFilter} allows to selectively choose which files
     * to copy over.
     *
     * @param input  the {@link InputStream} for the Jar/Zip to copy.
     * @param filter the filter or <code>null</code>
     * @throws IOException       if an I/O error occurs.
     * @throws ZipAbortException if the {@link IZipEntryFilter} filter indicated that the write
     *                           operation must be aborted.
     */
    public void writeZip(InputStream input, IZipEntryFilter filter)
            throws IOException, ZipAbortException {
        try (ZipInputStream zis = new ZipInputStream(input)) {
            // loop on the entries of the intermediary package and put them in the final package.
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                // do not take directories or anything inside a potential META-INF folder.
                if (entry.isDirectory() || name.startsWith("META-INF/")) {
                    continue;
                }
                // if we have a filter, we check the entry against it
                if (filter != null && !filter.checkEntry(name)) {
                    continue;
                }
                JarEntry newEntry;
                // Preserve the STORED method of the input entry.
                if (entry.getMethod() == ZipEntry.STORED) {
                    newEntry = new JarEntry(entry);
                } else {
                    // Create a new entry so that the compressed len is recomputed.
                    newEntry = new JarEntry(name);
                }
                writeEntry(zis, newEntry);
                zis.closeEntry();
            }
        }
    }

    /**
     * Closes the Jar archive by creating the manifest, and signing the archive.
     *
     * @throws IOException              if an I/O error occurs.
     * @throws GeneralSecurityException if a security error occurs.
     */
    public void close() throws IOException, GeneralSecurityException {
        if (mManifest != null) {
            // write the manifest to the jar file
            mOutputJar.putNextEntry(new JarEntry(JarFile.MANIFEST_NAME));
            mManifest.write(mOutputJar);
            // CERT.SF
            Signature signature = Signature.getInstance("SHA1with" + mKey.getAlgorithm());
            signature.initSign(mKey);
            mOutputJar.putNextEntry(new JarEntry("META-INF/CERT.SF"));
            SignatureOutputStream out = new SignatureOutputStream(mOutputJar, signature);
            writeSignatureFile(out);
            // CERT.*
            mOutputJar.putNextEntry(new JarEntry("META-INF/CERT." + mKey.getAlgorithm()));
            writeSignatureBlock(signature, mCertificate, mKey, out);
            // close out at the end because it can also close mOutputJar.
            // (there's some timing issue here I think, because it's worked before without
            // being closed after writing CERT.SF).
            out.close();
        }
        mOutputJar.close();
        mOutputJar = null;
    }

    /**
     * Clean up of the builder for interrupted workflow.
     * This does nothing if {@link #close()} was called successfully.
     */
    public void cleanUp() {
        if (mOutputJar != null) {
            try {
                mOutputJar.close();
            } catch (IOException e) {
                // pass
            }
        }
    }

    /**
     * Adds an entry to the output jar, and write its content from the {@link InputStream}
     *
     * @param input The input stream from where to write the entry content.
     * @param entry the entry to write in the jar.
     * @throws IOException if an I/O error occurs.
     */
    private void writeEntry(@NonNull InputStream input, JarEntry entry) throws IOException {
        // add the entry to the jar archive
        mOutputJar.putNextEntry(entry);
        // read the content of the entry from the input stream, and write it into the archive.
        int count;
        while ((count = input.read(mBuffer)) != -1) {
            mOutputJar.write(mBuffer, 0, count);
            // update the digest
            if (mMessageDigest != null) {
                mMessageDigest.update(mBuffer, 0, count);
            }
        }
        // close the entry for this file
        mOutputJar.closeEntry();
        if (mManifest != null) {
            // update the manifest for this entry.
            Attributes attr = mManifest.getAttributes(entry.getName());
            if (attr == null) {
                attr = new Attributes();
                mManifest.getEntries().put(entry.getName(), attr);
            }
            attr.putValue(DIGEST_ATTR, Base64.getEncoder().encodeToString(mMessageDigest.digest()));
        }
    }

    /**
     * Writes a .SF file with a digest to the manifest.
     */
    private void writeSignatureFile(SignatureOutputStream out)
            throws IOException, GeneralSecurityException {
        Manifest sf = new Manifest();
        Attributes main = sf.getMainAttributes();
        main.putValue("Signature-Version", "1.0");
        main.putValue("Created-By", "1.0 (Android)");
        MessageDigest md = MessageDigest.getInstance(DIGEST_ALGORITHM);
        PrintStream print = new PrintStream(
                new DigestOutputStream(new ByteArrayOutputStream(), md),
                true, StandardCharsets.UTF_8);
        // Digest of the entire manifest
        mManifest.write(print);
        print.flush();
        main.putValue(DIGEST_MANIFEST_ATTR, Base64.getEncoder().encodeToString(md.digest()));
        Map<String, Attributes> entries = mManifest.getEntries();
        for (Map.Entry<String, Attributes> entry : entries.entrySet()) {
            // Digest of the manifest stanza for this entry.
            com.android.builder.signing.SignedJarBuilder.digestManifest(print, entry);
            Attributes sfAttr = new Attributes();
            sfAttr.putValue(DIGEST_ATTR, Base64.getEncoder().encodeToString(md.digest()));
            sf.getEntries().put(entry.getKey(), sfAttr);
        }
        sf.write(out);
        // A bug in the java.util.jar implementation of Android platforms
        // up to version 1.6 will cause a spurious IOException to be thrown
        // if the length of the signature file is a multiple of 1024 bytes.
        // As a workaround, add an extra CRLF in this case.
        if ((out.size() % 1024) == 0) {
            out.write('\r');
            out.write('\n');
        }
    }


    /**
     * Writes the certificate file with a digital signature.
     *
     * @param outputStream the {@link OutputStream} to write the signature block to.
     * @param publicKey    the {@link X509Certificate} used to sign the archive.
     * @param privateKey   the {@link PrivateKey} used to sign the archive.
     * @throws IOException              if an I/O error occurs.
     * @throws GeneralSecurityException if a security error occurs.
     */
    private void writeSignatureBlock(Signature signature, X509Certificate publicKey,
                                     PrivateKey privateKey, OutputStream outputStream)
            throws IOException, GeneralSecurityException {

        try {
            // Step 1: Initialize the Signature with the private key
            String algorithm = "SHA256withRSA"; // Adjust based on your key and requirements
            signature.initSign(privateKey);
            byte[] dataToSign = new byte[0]; // Replace with actual data if required
            signature.update(dataToSign);

            // Step 2: Sign the data
            byte[] signedBytes = signature.sign();

            // Step 3: Create BouncyCastle structures
            ContentSigner contentSigner = new JcaContentSignerBuilder(algorithm).build(privateKey);
            CMSSignedDataGenerator generator = new CMSSignedDataGenerator();

            // Add signer information
            JcaSignerInfoGeneratorBuilder signerInfoGeneratorBuilder =
                    new JcaSignerInfoGeneratorBuilder(new JcaDigestCalculatorProviderBuilder().build());
            generator.addSignerInfoGenerator(
                    signerInfoGeneratorBuilder.build(contentSigner, publicKey));

            // Add the public certificate to the store
            JcaCertStore certStore = new JcaCertStore(Collections.singletonList(publicKey));
            generator.addCertificates(certStore);

            // Step 4: Generate PKCS7 structure
            CMSTypedData cmsData = new CMSProcessableByteArray(CMSObjectIdentifiers.data, dataToSign);
            CMSSignedData cmsSignedData = generator.generate(cmsData, true);

            // Step 5: Write the signed PKCS7 data
            outputStream.write(cmsSignedData.getEncoded());
            outputStream.flush();

        } catch (Exception e) {
            throw new GeneralSecurityException("Error writing signature block", e);
        }
    }

    /**
     * Classes which implement this interface provides a method to check whether a file should
     * be added to a Jar file.
     */
    public interface IZipEntryFilter {
        /**
         * Checks a file for inclusion in a Jar archive.
         *
         * @param archivePath the archive file path of the entry
         * @return <code>true</code> if the file should be included.
         * @throws ZipAbortException if writing the file should be aborted.
         */
        boolean checkEntry(String archivePath) throws ZipAbortException;

        /**
         * An exception thrown during packaging of a zip file into APK file.
         * This is typically thrown by implementations of
         * {@link IZipEntryFilter#checkEntry(String)}.
         */
        class ZipAbortException extends Exception {
            @Serial
            private static final long serialVersionUID = 1L;

            public ZipAbortException() {
                super();
            }

            public ZipAbortException(String format, Object... args) {
                super(String.format(format, args));
            }

            public ZipAbortException(Throwable cause, String format, Object... args) {
                super(String.format(format, args), cause);
            }

            public ZipAbortException(Throwable cause) {
                super(cause);
            }
        }
    }

    /**
     * Write to another stream and also feed it to the Signature object.
     */
    private static class SignatureOutputStream extends FilterOutputStream {
        private final Signature mSignature;
        private int mCount = 0;

        public SignatureOutputStream(OutputStream out, Signature sig) {
            super(out);
            mSignature = sig;
        }

        @Override
        public void write(int b) throws IOException {
            try {
                mSignature.update((byte) b);
            } catch (SignatureException e) {
                throw new IOException("SignatureException: " + e);
            }
            super.write(b);
            mCount++;
        }

        @Override
        public void write(@NonNull byte[] b, int off, int len) throws IOException {
            try {
                mSignature.update(b, off, len);
            } catch (SignatureException e) {
                throw new IOException("SignatureException: " + e);
            }
            super.write(b, off, len);
            mCount += len;
        }

        public int size() {
            return mCount;
        }
    }
}