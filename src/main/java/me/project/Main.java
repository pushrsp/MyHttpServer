package me.project;

import me.project.config.Configuration;
import me.project.config.ConfigurationManager;

public class Main {
    public static void main(String[] args) {
        ConfigurationManager.getInstance().loadConfigurationFile("src/main/resources/config.json");
        Configuration currentConfiguration = ConfigurationManager.getInstance().getCurrentConfiguration();

        System.out.println("port: " + currentConfiguration.getPort());
        System.out.println("webroot: " + currentConfiguration.getWebroot());
    }
}
