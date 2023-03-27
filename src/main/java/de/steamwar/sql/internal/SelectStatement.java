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

import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SelectStatement<T> extends Statement {
    private final Table<T> table;

    SelectStatement(Table<T> table, String... kfields) {
        this(table, "SELECT " + Arrays.stream(table.fields).map(f -> f.identifier).collect(Collectors.joining(", ")) + " FROM " + table.name + " WHERE " + Arrays.stream(kfields).map(f -> f + " = ?").collect(Collectors.joining(" AND ")));
    }

    public SelectStatement(Table<T> table, String sql) {
        super(sql);
        this.table = table;
    }

    public T select(Object... values) {
        return select(rs -> {
            if (rs.next())
                return read(rs);
            return null;
        }, values);
    }

    public List<T> listSelect(Object... values) {
        return select(rs -> {
            List<T> result = new ArrayList<>();
            while (rs.next())
                result.add(read(rs));

            return result;
        }, values);
    }

    private T read(ResultSet rs) throws SQLException {
        Object[] params = new Object[table.fields.length];
        for (int i = 0; i < params.length; i++) {
            params[i] = table.fields[i].read(rs);
        }

        try {
            return table.constructor.newInstance(params);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new SecurityException(e);
        }
    }
}
