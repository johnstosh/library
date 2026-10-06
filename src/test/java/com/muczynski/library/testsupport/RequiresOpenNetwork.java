package com.muczynski.library.testsupport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test that calls a live external service and therefore needs an open network.
 *
 * <p>The test is skipped unless the {@code OPEN_NETWORK} environment variable is set to
 * {@code true} (case-insensitive), the same rule as
 * {@code @EnabledIfEnvironmentVariable(named = "OPEN_NETWORK", matches = "(?i)true")}.
 * CI sets that variable so the same tests run on the weekly deploy workflow. Hardened or
 * offline machines leave it unset and the tests are skipped rather than failed.
 *
 * <p>JUnit evaluates {@code @EnabledIfEnvironmentVariable} only when it is declared on the
 * test class or method, not when it is inherited from a composed annotation. Put both
 * annotations on each gated test.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresOpenNetwork {
}
