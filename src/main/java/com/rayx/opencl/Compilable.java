package com.rayx.opencl;

import java.util.Arrays;

public class Compilable {
    private final String name;
    private final String content;
    private final Compilable[] dependencies;
    private boolean valid;
    private final int rank;

    public Compilable(String name, String content, Compilable[] dependencies) {
        this.name = name;
        this.content = content;
        this.dependencies = dependencies;
        this.valid = false;
        this.rank = calcRank();
    }

    public Compilable(String sourceFile, Compilable[] dependencies) {
        this(sourceFile, CLManager.readFromFile("/" + sourceFile), dependencies);
        assert !sourceFile.startsWith("/") : "Files have to start without slash";
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public Compilable[] getDependencies() {
        return dependencies;
    }

    public String[] getDependenciesId() {
        return Arrays.stream(dependencies).map(Compilable::getName).toArray(String[]::new);
    }

    public boolean isValid() {
        return valid;
    }

    public void invalidate() {
        this.valid = false;
    }

    public int getRank() {
        return rank;
    }

    private int calcRank() {
        int l = 0;
        for(Compilable d: dependencies) {
            l = Math.max(l, d.getRank());
        }
        return l + 1;
    }
}
