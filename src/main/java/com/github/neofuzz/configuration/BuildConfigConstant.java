package com.github.neofuzz.configuration;

import org.apache.maven.plugins.annotations.Parameter;

/**
 * Configuration for a custom BuildConfig constant.
 *
 * @author jonasfa@gmail.com
 */
public class BuildConfigConstant {
    /**
     * Name of the constant.
     * E.g.: SERVER_URL, etc
     */
    @Parameter(property = "android.buildConfigConstants[].name", required = true)
    private String name;

    /**
     * Type of the value.
     * Eg.: String, int, com.mypackage.MyType, etc
     */
    @Parameter(property = "android.buildConfigConstants[].type", required = true)
    private String type;

    /**
     * Value of the constant.
     * Eg.: MyString, 123, new com.mypackage.MyType(), etc
     */
    @Parameter(property = "android.buildConfigConstants[].value", required = true)
    private String value;

    /**
     * @return the BuildConfigConstant type.
     */
    public String getType() {
        return type;
    }

    /**
     * @return the BuildConfigConstant value.
     */
    public String getValue() {
        return value;
    }

    /**
     * @return the BuildConfigConstant name.
     */
    public String getName() {
        return name;
    }
}
