package com.github.cardforge.maven.plugins.android.common;

import com.android.annotations.NonNull;

/**
 * FileNameHelper can make a valid filename.
 *
 * @author alexv
 */
public class FileNameHelper {
    /**
     * Regex for illegal characters in a filename. This is a copy of the regex from
     * {@link java.io.File#createTempFile(String, String)}.
     * <p>
     * The following characters are not allowed:
     * <ul>
     * <li>{@code /}
     * <li>{@code \n}
     * <li>{@code \r}
     * <li>{@code \t}
     * <li>{@code \0}
     * <li>{@code \f}
     * <li>{@code `}
     * <li>{@code ?}
     * <li>{@code *}
     * <li>{@code \}
     * <li>{@code <}
     * <li>{@code >}
     * <li>{@code |}
     * <li>{@code "}
     * <li>{@code :}
     * </ul>
     */
    private static final String ILLEGAL_CHARACTERS_REGEX = "[/\\n\\r\\t\\\0\\f`\\?\\*\\\\<>\\|\":]";
    /**
     * Separator for the filename.
     */
    private static final String SEPARATOR = "_";

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FileNameHelper() {
        // no instances
    }

    /**
     * Make a valid filename from the given filename.
     *
     * @param fileName the filename to make valid
     * @return a valid filename
     */
    @NonNull
    public static String fixFileName(@NonNull String fileName) {
        return fileName.replaceAll(ILLEGAL_CHARACTERS_REGEX, SEPARATOR);
    }

}
