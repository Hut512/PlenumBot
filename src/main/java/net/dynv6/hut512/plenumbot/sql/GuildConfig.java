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
