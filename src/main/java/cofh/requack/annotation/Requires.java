/*
 * This file is part of Quack and is Licensed under the MIT License.
 */
package cofh.requack.annotation;

import java.lang.annotation.*;

/**
 * Specifies what Maven Dependencies something requires,
 * Used as Human readable markers for denoting what Things
 * in Quack require what maven dependencies.
 * <p>
 * Created by covers1624 on 13/1/21.
 */
@Target (ElementType.TYPE)
@Retention (RetentionPolicy.CLASS)
@Repeatable (Requires.RequiresList.class)
public @interface Requires {

    /**
     * The Maven group and module separated by a colon. E.g. 'com.google.guava:guava'
     *
     * @return The coords.
     */
    String value();

    /**
     * Denotes the minimum version bound. In some cases this might be inaccurate.
     *
     * @return The minimum version.
     */
    String minVersion() default "";

    /**
     * Denotes the maximum version bound. In some cases this might be inaccurate.
     *
     * @return The maximum version.
     */
    String maxVersion() default "";

    @Target (ElementType.TYPE)
    @Retention (RetentionPolicy.CLASS)
    @interface RequiresList {

        Requires[] value();

    }

}
