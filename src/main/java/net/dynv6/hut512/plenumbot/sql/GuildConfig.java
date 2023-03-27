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

@AllArgsConstructor
public class GuildConfig {
    private static final Table<GuildConfig> table = new Table<>(GuildConfig.class);
    private static final SelectStatement<GuildConfig> select = table.select(Table.PRIMARY);
    private static final Statement insert = table.insertAll();
    private static final Statement delete = table.delete(Table.PRIMARY);

    @Field(keys = {Table.PRIMARY})
    private final long guild;
    @Field(keys = {Table.PRIMARY})
    private final String config;
    @Field
    private final String value;

    public static String getConfig(long guild, String config) {
        GuildConfig value = select.select(guild, config);
        return value == null ? null : value.value;
    }

    public static void updateConfig(long guild, String config, String value) {
        if (value == null) {
            deleteConfig(guild, config);
            return;
        }
        insert.update(guild, config, value);
    }

    public static void deleteConfig(long guild, String config) {
        delete.update(guild, config);
    }
}
