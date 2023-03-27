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
import net.dynv6.hut512.plenumbot.sql.GuildConfig;

public class Main {
    private static final String BOT_CONFIG_FILE_NAME = "bot.properties";

    @Getter
    String test;

    public static void main(String[] args) {
        new PlenumBot(BOT_CONFIG_FILE_NAME);
        GuildConfig.getConfig(0, "23");
    }
}
