package com.goodworkalan.ilk.io.mix;

import com.goodworkalan.go.go.Artifact;
import com.goodworkalan.mix.ProjectModule;
import com.goodworkalan.mix.builder.Builder;
import com.goodworkalan.mix.builder.JavaProject;

public class IlkStreamProject extends ProjectModule {
    @Override
    public void build(Builder builder) {
        builder
            .cookbook(JavaProject.class)
                .produces(new Artifact("com.goodworkalan/ilk-stream/0.1"))
                .main()
                    .depends()
                        .artifact(new Artifact("com.goodworkalan/ilk/0.1"))
                        .end()
                    .end()
                .test()
                    .depends()
                        .artifact(new Artifact("org.testng/testng/5.10/jdk"))
                        .end()
                    .end()
                .end()
            .end();
    }
}
