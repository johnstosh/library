/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Vite content-hashed assets (JS, CSS) — filenames change on every rebuild,
        // so caching with immutable is safe and eliminates unnecessary revalidation.
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic().immutable());

        // Images — no content hash in filenames, so skip immutable but still cache 7 days.
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/images/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());

        // Favicon and other root-level static files — cache 7 days.
        registry.addResourceHandler("/favicon.ico", "/vite.svg")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());

        // index.html must never be cached — it is the SPA entry point that embeds
        // content-hashed asset filenames. If cached, users won't pick up new deployments.
        registry.addResourceHandler("/index.html")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache().mustRevalidate());

        // Fonts (Century Schoolbook L) — long-lived cache for performance.
        // Use immutable + 1 year TTL since font bytes rarely change.
        // NOTE: These files are NOT content-fingerprinted by Vite (they live in
        // frontend/public/fonts and are served from /fonts/* paths referenced by
        // fixed URLs in index.css @font-face rules).
        // Cache-busting strategy when fonts change:
        //   - Rename the .ttf files (e.g. CenturySchL-Roma-v2.ttf)
        //   - Update the src urls in frontend/src/index.css (and the copy under src/main/resources/public/fonts if needed)
        //   - Rebuild frontend and redeploy.
        // This keeps HTML/API responses fresh while allowing aggressive font caching.
        registry.addResourceHandler("/fonts/**")
                .addResourceLocations("classpath:/public/fonts/", "classpath:/static/fonts/")
                .setCacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());
    }
}
