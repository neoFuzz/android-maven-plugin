package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;

/**
 * FileNameHelper can make a valid filename.
 *
 * @author alexv
 */
public class FileNameHelper {
    //    { '/', '\n', '\r', '\t', '\0', '\f', '`', '?', '*', '\\', '<', '>', '|', '\"', ':' };
    private static final String ILLEGAL_CHARACTERS_REGEX = "[/\\n\\r\\t\\\0\\f`\\?\\*\\\\<>\\|\":]";
    private static final String SEPARATOR = "_";

    private FileNameHelper() {
        // no instances
    }

    @NonNull
    public static String fixFileName(@NonNull String fileName) {
        return fileName.replaceAll(ILLEGAL_CHARACTERS_REGEX, SEPARATOR);
    }

}
