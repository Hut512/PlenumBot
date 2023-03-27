package net.dynv6.hut512.plenumbot;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dynv6.hut512.plenumbot.elternportal.SubstitutionPlanMessageUpdater;
import net.dynv6.hut512.plenumbot.listener.MusicListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Properties;

public class PlenumBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlenumBot.class);
    @Getter private static PlenumBot instance;

    @Getter private final JDA jda;
    @Getter private final Properties config;
    public PlenumBot(String configFileName) {
        PlenumBot.instance = this;
        config = loadConfig(configFileName);
        JDABuilder builder = JDABuilder.createDefault(config.getProperty("TOKEN"));
        builder.addEventListeners(new MusicListener());
        jda = builder.build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        new SubstitutionPlanMessageUpdater();
        LOGGER.info("Bot Ready!");
    }

    private Properties loadConfig(String fileName) {
        try {
            InputStream defaultConfigInputStream = PlenumBot.class.getResourceAsStream("/" + fileName);

            Objects.requireNonNull(defaultConfigInputStream);

            Properties config = new Properties();
            config.load(defaultConfigInputStream);

            File configFile = new File(new File(PlenumBot.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParent(), fileName);
            if (!configFile.exists()) {
                Files.copy(defaultConfigInputStream, configFile.toPath());
            }
            config.load(Files.newInputStream(configFile.toPath()));
            return config;
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
