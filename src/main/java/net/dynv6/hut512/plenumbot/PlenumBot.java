/*
 * This file is a part of the PlenumBot software.
 *
 * Copyright (c) 2023  Hut512
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package net.dynv6.hut512.plenumbot;

import lombok.Getter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dynv6.hut512.plenumbot.command.BasicCommand;
import net.dynv6.hut512.plenumbot.command.PingCommand;
import net.dynv6.hut512.plenumbot.listener.CommandListener;
import net.dynv6.hut512.plenumbot.listener.MusicInterfaceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class PlenumBot {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlenumBot.class);

    public static final String VERSION = "PlenumBot v0.1";
    public static final String URL = "https://hut512.dynv6.net/plenumbot";
    @Getter
    private static PlenumBot instance;

    @Getter
    private final JDA jda;
    private final Map<String, BasicCommand> commandMap;
    @Getter
    private final Properties config;

    public PlenumBot(String configFileName) {
        PlenumBot.instance = this;
        commandMap = new HashMap<>();
        config = loadConfig(configFileName);
        JDABuilder builder = JDABuilder.createDefault(config.getProperty("TOKEN"));
        builder.enableIntents(GatewayIntent.MESSAGE_CONTENT);
        builder.addEventListeners(new CommandListener(), new MusicInterfaceListener());
        jda = builder.build();
        try {
            jda.awaitReady();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        new ScheduleInfoUpdater();
        addCommands(new PingCommand());
        LOGGER.info("Bot Ready!");
    }

    private void addCommands(BasicCommand... commands) {
        for (BasicCommand command : commands) {
            commandMap.put(command.getName(), command);
        }
        jda.updateCommands().addCommands(commands).queue();
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

    public BasicCommand getCommand(String name) {
        return commandMap.get(name);
    }
}
