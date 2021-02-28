package com.rayx.modular.plugin;

import com.rayx.modular.plugin.schemas.YamlPlugin;
import com.rayx.modular.plugin.schemas.YamlSDF;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class PluginLoader {

    public PluginLoader() {

    }

    public Plugin loadPlugin(File file) throws FileNotFoundException {

        Yaml yaml = new Yaml(new Constructor(YamlPlugin.class));


        FileInputStream fs = new FileInputStream(file);
        YamlPlugin yamlPlugin = yaml.load(fs);

        System.out.println(file.getAbsoluteFile().toString());

        Plugin plugin = new Plugin(yamlPlugin);

        for (String include : yamlPlugin.getIncludes()) {
            File includeFile = new File(file.getParentFile(), include);

            Representer representer = new Representer();

            yaml = new Yaml(new Constructor(YamlSDF.class), representer);

            fs = new FileInputStream(includeFile);
            YamlSDF yamlSDF = yaml.load(fs);
            System.out.println(yamlSDF.getCode());
        }


        return null;
    }

   // public Plugin loadFromResources() {
        //new InputStream();
    //}

}
