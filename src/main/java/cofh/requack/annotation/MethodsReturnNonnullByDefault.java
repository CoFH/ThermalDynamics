/*
 * This file is part of Quack and is Licensed under the MIT License.
 */
package cofh.requack.annotation;

import javax.annotation.Nonnull;
import javax.annotation.meta.TypeQualifierDefault;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Nonnull
@Retention (RetentionPolicy.RUNTIME)
@TypeQualifierDefault (ElementType.METHOD)
public @interface MethodsReturnNonnullByDefault {}
