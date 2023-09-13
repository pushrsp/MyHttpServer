package me.project.config;

import com.fasterxml.jackson.databind.JsonNode;
import me.project.exception.HttpConfigurationException;
import me.project.util.Json;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class ConfigurationManager {

    private static ConfigurationManager configurationManager;
    private static Configuration configuration;

    public static ConfigurationManager getInstance() {
        if(configurationManager == null)
            configurationManager = new ConfigurationManager();

        return configurationManager;
    }

    /**
     * Used to load a configuration file by path provided
     */
    public void loadConfigurationFile(String filePath) {
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(filePath);
        } catch (FileNotFoundException e) {
            throw new HttpConfigurationException(e);
        }

        StringBuffer sb = new StringBuffer();

        int i;
        try {
            while ((i = fileReader.read()) != -1)
                sb.append((char) i);
        } catch (IOException e) {
            throw new HttpConfigurationException(e);
        }

        JsonNode conf = null;
        try {
            conf = Json.parse(sb.toString());
        } catch (IOException e) {
            throw new HttpConfigurationException("Error parsing configuration file", e);
        }

        try {
            configuration = Json.fromJson(conf, Configuration.class);
        } catch (IOException e) {
            throw new HttpConfigurationException("Error parsing configuration file(internal)", e);
        }
    }

    /**
     * Returns the current loaded configuration
     */
    public Configuration getCurrentConfiguration() {
        if(configuration == null) {
            throw new HttpConfigurationException("No Configuration");
        }

        return configuration;
    }

    private ConfigurationManager() { }
}
