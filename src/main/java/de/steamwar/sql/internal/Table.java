/*
 *    This file is a part of the SteamWar software.
 *
 *    Copyright (C) 2022  SteamWar.de-Serverteam
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *    GNU Affero General Public License for more details.
 *
 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.steamwar.sql.internal;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Table<T> {
    public static final String PRIMARY = "primary";

    final String name;
    final TableField<?>[] fields;
    private final Map<String, TableField<?>> fieldsByIdentifier = new HashMap<>();
    final Constructor<T> constructor;

    private final Map<String, Table.TableField<?>[]> keys;


    public Table(Class<T> clazz) {
        this(clazz, clazz.getSimpleName());
    }

    public Table(Class<T> clazz, String name) {
        this.name = name;
        this.fields = Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Field.class)).map(TableField::new).toArray(TableField[]::new);
        try {
            this.constructor = clazz.getDeclaredConstructor(Arrays.stream(clazz.getDeclaredFields()).filter(field -> field.isAnnotationPresent(Field.class)).map(java.lang.reflect.Field::getType).toArray(Class[]::new));
        } catch (NoSuchMethodException e) {
            throw new SecurityException(e);
        }

        keys = Arrays.stream(fields).flatMap(field -> Arrays.stream(field.field.keys())).distinct().collect(Collectors.toMap(Function.identity(), key -> Arrays.stream(fields).filter(field -> Arrays.asList(field.field.keys()).contains(key)).toArray(TableField[]::new)));

        for (TableField<?> field : fields) {
            fieldsByIdentifier.put(field.identifier.toLowerCase(), field);
        }

        Statement.schemaCreator.accept(this);
    }

    public SelectStatement<T> select(String name) {
        return selectFields(keyFields(name));
    }

    public SelectStatement<T> selectFields(String... kfields) {
        return new SelectStatement<>(this, kfields);
    }

    public Statement update(String name, String... fields) {
        return updateFields(fields, keyFields(name));
    }

    public Statement updateField(String field, String... kfields) {
        return updateFields(new String[]{field}, kfields);
    }

    public Statement updateFields(String[] fields, String... kfields) {
        return new Statement("UPDATE " + name + " SET " + Arrays.stream(fields).map(f -> f + " = ?").collect(Collectors.joining(", ")) + " WHERE " + Arrays.stream(kfields).map(f -> f + " = ?").collect(Collectors.joining(" AND ")));
    }

    public Statement insert(String name) {
        return insertFields(keyFields(name));
    }

    public Statement insertAll() {
        return insertFields(false, Arrays.stream(fields).map(f -> f.identifier).toArray(String[]::new));
    }

    public Statement insertFields(String... fields) {
        return insertFields(false, fields);
    }

    public Statement insertFields(boolean returnGeneratedKeys, String... fields) {
        List<String> nonKeyFields = Arrays.stream(fields).filter(f -> fieldsByIdentifier.get(f.toLowerCase()).field.keys().length == 0).collect(Collectors.toList());
        return new Statement("INSERT INTO " + name + " (" + String.join(", ", fields) + ") VALUES (" + Arrays.stream(fields).map(f -> "?").collect(Collectors.joining(", ")) + ")" + (nonKeyFields.isEmpty() ? "" : Statement.ON_DUPLICATE_KEY + nonKeyFields.stream().map(Statement.upsertWrapper).collect(Collectors.joining(", "))), returnGeneratedKeys);
    }

    public Statement delete(String name) {
        return deleteFields(keyFields(name));
    }

    public Statement deleteFields(String... kfields) {
        return new Statement("DELETE FROM " + name + " WHERE " + Arrays.stream(kfields).map(f -> f + " = ?").collect(Collectors.joining(" AND ")));
    }

    void ensureExistanceInSqlite() {
        try (Statement statement = new Statement(
                "CREATE TABLE IF NOT EXISTS " + name + "(" +
                        Arrays.stream(fields).map(field -> field.identifier + " " + field.mapper.sqlType() + (field.field.nullable() ? " DEFAULT NULL" : " NOT NULL") + (field.field.nullable() || field.field.def().equals("") ? "" : " DEFAULT " + field.field.def())).collect(Collectors.joining(", ")) +
                        keys.entrySet().stream().map(key -> (key.getKey().equals(PRIMARY) ? ", PRIMARY KEY(" : ", UNIQUE (") + Arrays.stream(key.getValue()).map(field -> field.identifier).collect(Collectors.joining(", ")) + ")").collect(Collectors.joining(" ")) +
                        ")")) {
            statement.update();
        }
    }

    private String[] keyFields(String name) {
        return Arrays.stream(keys.get(name)).map(f -> f.identifier).toArray(String[]::new);
    }

    static class TableField<T> {

        final String identifier;

        final SqlTypeMapper<T> mapper;
        private final Field field;

        private TableField(java.lang.reflect.Field field) {
            this.identifier = field.getName();
            this.mapper = SqlTypeMapper.getMapper(field.getType());
            this.field = field.getAnnotation(Field.class);
        }

        T read(ResultSet rs) throws SQLException {
            return mapper.read(rs, identifier);
        }
    }
}
