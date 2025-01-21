package com.github.cardforge.maven.plugins.android.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ConfigPojo is an annotation identifying a pojo as a configuration holder for ConfigHandler.
 *
 * @author <a href="https://github.com/grundid/">Adrian Stabiszewski</a>
 * @author Manfred Moser - manfred@simpligility.com
 * @see ConfigHandler
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ConfigPojo {

    String prefix() default "parsed";
}
