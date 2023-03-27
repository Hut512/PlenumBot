package net.dynv6.hut512.plenumbot;

import lombok.Getter;
import net.dynv6.hut512.plenumbot.sql.GuildConfig;

public class Main {
    private static final String BOT_CONFIG_FILE_NAME = "bot.properties";

    @Getter String test;
    public static void main(String[] args) {
        new PlenumBot(BOT_CONFIG_FILE_NAME);
        GuildConfig.getConfig(0, "23");
    }
}
