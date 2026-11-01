/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Runs {@link EssayColumnMigration} and {@link AlternateTitleMigration} before
 * Hibernate builds {@code entityManagerFactory}. That build is the schema
 * update. It casts an {@code oid} essay column to the object id unless the
 * large objects have already been copied, and it does not convert
 * {@code alternate_title} into {@code text[]}.
 */
@Configuration
public class EssayColumnMigrationConfig {

    @Bean
    public static BeanFactoryPostProcessor essayColumnMigrationBeforeSchemaUpdate() {
        return beanFactory -> {
            if (!beanFactory.containsBeanDefinition("entityManagerFactory")) {
                return;
            }
            BeanDefinition entityManagerFactory = beanFactory.getBeanDefinition("entityManagerFactory");
            List<String> dependsOn = new ArrayList<>();
            String[] existing = entityManagerFactory.getDependsOn();
            if (existing != null) {
                Collections.addAll(dependsOn, existing);
            }
            if (!dependsOn.contains("essayColumnMigration")) {
                dependsOn.add("essayColumnMigration");
            }
            if (!dependsOn.contains("alternateTitleMigration")) {
                dependsOn.add("alternateTitleMigration");
            }
            entityManagerFactory.setDependsOn(dependsOn.toArray(String[]::new));
        };
    }
}
