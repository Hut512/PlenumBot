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

package net.dynv6.hut512.plenumbot.sql;

import de.steamwar.sql.internal.Field;
import de.steamwar.sql.internal.SelectStatement;
import de.steamwar.sql.internal.Statement;
import de.steamwar.sql.internal.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

@AllArgsConstructor
public class GuildConfig {
    private static final Table<GuildConfig> TABLE = new Table(GuildConfig.class);
    private static final SelectStatement<GuildConfig> SELECT = TABLE.select(Table.PRIMARY);
    private static final Statement INSERT = TABLE.insertAll();
    private static final Statement DELETE = TABLE.delete(Table.PRIMARY);

    @Field(keys = Table.PRIMARY)
    private final long guild;
    @Field(keys = Table.PRIMARY)
    private final String config;
    @Field
    private final String value;

    public static <T> T getConfig(long guild, Config<T> config) {
        GuildConfig guildConfig = SELECT.select(guild, config.getName());
        String strValue = guildConfig == null ? config.getDefaultValue().toString() : guildConfig.value;
        return convertToObject(strValue, config.getType());
    }

    public static <T> void updateConfig(long guild, Config<T> config, T value) {
        if (value == null) {
            deleteConfig(guild, config);
            return;
        }
        INSERT.update(guild, config.getName(), value.toString());
    }

    public static void deleteConfig(long guild, Config<?> config) {
        DELETE.update(guild, config.getName());
    }

    public static <T> T convertToObject(String string, Class<T> targetType) {
        try {
            Constructor<T> constructor = targetType.getConstructor(String.class);
            return constructor.newInstance(string);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                 NoSuchMethodException e) {
            throw new RuntimeException("Could not convert " + string + " to " + targetType.getName(), e);
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Config<T> {
        public static final Config<String> LANGUAGE = new Config<>("language", Locale.ENGLISH.getLanguage(), String.class);
        public static final Config<String> MUSIC_INTERFACE_CHANNEL_ID = new Config<>("musicInterfaceChannelId", "0", String.class);
        public static final Config<Boolean> MUSIC_INTERFACE_SHOULD_CLEAR_CHANNEL = new Config<>("musicInterfaceShouldClearChannel", false, Boolean.class);
        public static final Config<String> SCHEDULE_INFO_RENDER_MANAGER = new Config<>("scheduleInfoRenderManager", "default", String.class);
        public static final Config<String> SCHEDULE_INFO_SOURCE_MANAGER = new Config<>("scheduleInfoSourceManager", "elternportal", String.class);
        public static final Config<String> SCHEDULE_INFO_CHANNEL_ID = new Config<>("scheduleInfoChannelId", "0", String.class);
        public static final Config<String> SCHEDULE_INFO_OPTIONS = new Config<>("scheduleInfoOptions", "", String.class);
        public static final Config<String> SCHEDULE_INFO_IMAGE_FORMAT = new Config<>("scheduleInfoImageFormat", "png", String.class);

        private final String name;
        private final T defaultValue;
        private final Class<T> type;
    }
}
