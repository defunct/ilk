package com.goodworkalan.ilk.mix;

import com.goodworkalan.mix.ProjectModule;
import com.goodworkalan.mix.builder.Builder;
import com.goodworkalan.mix.cookbook.JavaProject;

/**
 * Builds the project definition for Ilk Inject.
 *
 * @author Alan Gutierrez
 */
public class IlkInjectProject implements ProjectModule {
    /**
     * Build the project definition Ilk Inject.
     *
     * @param builder
     *          The project builder.
     */
    public void build(Builder builder) {
        builder
            .cookbook(JavaProject.class)
                .produces("com.github.bigeasy.ilk/ilk-inject/0.1.0.3")
                .depends()
                    .production("com.github.bigeasy.ilk/ilk-reflect/0.+1")
                    .production("javax.inject/inject/+1")
                    .development("org.testng/testng-jdk15/5.10")
                    .end()
                .end()
            .end();
    }
}
