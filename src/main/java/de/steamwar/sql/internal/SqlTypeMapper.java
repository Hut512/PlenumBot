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

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Map;

public final class SqlTypeMapper<T> {
    private static final Map<Class<?>, SqlTypeMapper<?>> mappers = new IdentityHashMap<>();

    public static <T> SqlTypeMapper<T> getMapper(Class<?> clazz) {
        return (SqlTypeMapper<T>) mappers.get(clazz);
    }

    public static <T extends Enum<T>> void ordinalEnumMapper(Class<T> type) {
        T[] enumConstants = type.getEnumConstants();
        new SqlTypeMapper<>(
                type,
                "INTEGER(" + (int)Math.ceil(enumConstants.length/256.0) + ")",
                (rs, identifier) -> enumConstants[rs.getInt(identifier)],
                (st, index, value) -> st.setInt(index, value.ordinal())
        );
    }

    public static <T extends Enum<T>> void nameEnumMapper(Class<T> type) {
        new SqlTypeMapper<>(
                type,
                "VARCHAR(" + Arrays.stream(type.getEnumConstants()).map(e -> e.name().length()).max(Integer::compareTo).orElse(0) + ")",
                (rs, identifier) -> Enum.valueOf(type, rs.getString(identifier)),
                (st, index, value) -> st.setString(index, value.name())
        );
    }

    static {
        primitiveMapper(boolean.class, Boolean.class, "BOOLEAN", ResultSet::getBoolean, PreparedStatement::setBoolean);
        primitiveMapper(byte.class, Byte.class, "INTEGER(1)", ResultSet::getByte, PreparedStatement::setByte);
        primitiveMapper(short.class, Short.class, "INTEGER(2)", ResultSet::getShort, PreparedStatement::setShort);
        primitiveMapper(int.class, Integer.class, "INTEGER", ResultSet::getInt, PreparedStatement::setInt);
        primitiveMapper(long.class, Long.class, "INTEGER(8)", ResultSet::getLong, PreparedStatement::setLong);
        primitiveMapper(float.class, Float.class, "REAL", ResultSet::getFloat, PreparedStatement::setFloat);
        primitiveMapper(double.class, Double.class, "REAL", ResultSet::getDouble, PreparedStatement::setDouble);
        new SqlTypeMapper<>(String.class, "TEXT", ResultSet::getString, PreparedStatement::setString);
        new SqlTypeMapper<>(Timestamp.class, "TIMESTAMP", ResultSet::getTimestamp, PreparedStatement::setTimestamp);
        new SqlTypeMapper<>(InputStream.class, "BLOB", ResultSet::getBinaryStream, PreparedStatement::setBinaryStream);
    }

    private static <T> void primitiveMapper(Class<T> primitive, Class<T> wrapped, String sqlType, SQLReader<T> reader, SQLWriter<T> writer) {
        new SqlTypeMapper<>(primitive, sqlType, reader, writer);
        new SqlTypeMapper<>(wrapped, sqlType, (rs, identifier) -> {
            T value = reader.read(rs, identifier);
            return rs.wasNull() ? null : value;
        }, writer);
    }

    private final String sqlType;
    private final SQLReader<T> reader;
    private final SQLWriter<T> writer;

    public SqlTypeMapper(Class<T> clazz, String sqlType, SQLReader<T> reader, SQLWriter<T> writer) {
        this.sqlType = sqlType;
        this.reader = reader;
        this.writer = writer;
        mappers.put(clazz, this);
    }

    public T read(ResultSet rs, String identifier) throws SQLException {
        return reader.read(rs, identifier);
    }

    public void write(PreparedStatement st, int index, Object value) throws SQLException {
        writer.write(st, index, (T) value);
    }

    public String sqlType() {
        return sqlType;
    }

    @FunctionalInterface
    public interface SQLReader<T> {
        T read(ResultSet rs, String identifier) throws SQLException;
    }

    @FunctionalInterface
    public interface SQLWriter<T> {
        void write(PreparedStatement st, int index, T value) throws SQLException;
    }
}
