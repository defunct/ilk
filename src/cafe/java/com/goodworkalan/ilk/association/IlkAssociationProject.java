package com.goodworkalan.ilk.association;

import com.goodworkalan.cafe.ProjectModule;
import com.goodworkalan.cafe.builder.Builder;
import com.goodworkalan.cafe.outline.JavaProject;

/**
 * Buils the project  definition for Ilk Association.
 *
 * @author Alan Gutierrez
 */
public class IlkAssociationProject implements ProjectModule {
    /**
     * Build the project definition Ilk Association.
     *
     * @param builder
     *          The project builder.
     */
    public void build(Builder builder) {
        builder
            .cookbook(JavaProject.class)
                .produces("com.github.bigeasy.ilk/ilk-association/0.1.0.1")
                .depends()
                    .production("com.github.bigeasy.ilk/ilk/0.+1")
                    .development("org.testng/testng-jdk15/5.10")
                    .end()
                .end()
            .end();
    }
}
