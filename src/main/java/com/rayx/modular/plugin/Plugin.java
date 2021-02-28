package com.rayx.modular.plugin;

import com.rayx.modular.plugin.schemas.YamlPlugin;
import com.rayx.scene.shape.Shape;

import java.util.ArrayList;
import java.util.List;

public class Plugin {

    private String name;
    private String description;
    private String author;
    private String version;

    private List<Shape> sdfs;

    public Plugin(YamlPlugin plugin) {
        this(plugin.getName(), plugin.getDescription(), plugin.getAuthor(), plugin.getVersion());
    }

    public Plugin(String name, String description, String author, String version) {
        this.name = name;
        this.description = description;
        this.author = author;
        this.version = version;

        sdfs = new ArrayList<>();
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<Shape> getSDFs() {
        return sdfs;
    }

    public void setSDFs(List<Shape> sdfs) {
        this.sdfs = sdfs;
    }
}
