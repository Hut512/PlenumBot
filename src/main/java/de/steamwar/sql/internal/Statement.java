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

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Statement implements AutoCloseable {
    //Start PlenumBot
    private static final Logger logger = Logger.getGlobal();
    //End PlenumBot

    private static final List<Statement> statements = new ArrayList<>();
    private static final Deque<Connection> connections = new ArrayDeque<>();
    private static final int MAX_CONNECTIONS;
    private static final Supplier<Connection> conProvider;
    static final Consumer<Table<?>> schemaCreator;
    static final String ON_DUPLICATE_KEY;
    static final UnaryOperator<String> upsertWrapper;
    public static final String NULL_SAFE_EQUALS;

    private static final boolean MYSQL_MODE;
    private static final boolean PRODUCTION_DATABASE;

    static {
        File file = new File(System.getProperty("user.home"), "mysql.properties");
        MYSQL_MODE = file.exists();

        if(MYSQL_MODE) {
            Properties properties = new Properties();
            try {
                properties.load(new FileReader(file));
            } catch (IOException e) {
                throw new SecurityException("Could not load SQL connection", e);
            }

            String url = "jdbc:mysql://" + properties.getProperty("host") + ":" + properties.getProperty("port") + "/" + properties.getProperty("database") + "?useServerPrepStmts=true";
            String user = properties.getProperty("user");
            String password = properties.getProperty("password");

            PRODUCTION_DATABASE = "core".equals(properties.getProperty("database"));
            //Start PlenumBot
            MAX_CONNECTIONS = 4;
            //End PlenumBot
            conProvider = () -> {
                try {
                    return DriverManager.getConnection(url, user, password);
                } catch (SQLException e) {
                    throw new SecurityException("Could not create MySQL connection", e);
                }
            };
            schemaCreator = table -> {};
            ON_DUPLICATE_KEY = " ON DUPLICATE KEY UPDATE ";
            upsertWrapper = f -> f + " = VALUES(" + f + ")";
            NULL_SAFE_EQUALS = " <=> ";
        } else {
            Connection connection;

            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.home") + "/standalone.db");
            } catch (SQLException | ClassNotFoundException e) {
                throw new SecurityException("Could not create sqlite connection", e);
            }

            PRODUCTION_DATABASE = false;
            MAX_CONNECTIONS = 1;
            conProvider = () -> connection;
            schemaCreator = Table::ensureExistanceInSqlite;
            ON_DUPLICATE_KEY = " ON CONFLICT DO UPDATE SET ";
            upsertWrapper = f -> f + " = " + f;
            NULL_SAFE_EQUALS = " IS ";
        }
    }

    private static volatile int connectionBudget = MAX_CONNECTIONS;

    public static void closeAll() {
        synchronized (connections) {
            while(connectionBudget < MAX_CONNECTIONS) {
                if(connections.isEmpty())
                    waitOnConnections();
                else
                    closeConnection(aquireConnection());
            }
        }
    }

    public static boolean mysqlMode() {
        return MYSQL_MODE;
    }

    public static boolean productionDatabase() {
        return PRODUCTION_DATABASE;
    }

    private final boolean returnGeneratedKeys;
    private final String sql;
    private final Map<Connection, PreparedStatement> cachedStatements = new HashMap<>();

    public Statement(String sql) {
        this(sql, false);
    }

    public Statement(String sql, boolean returnGeneratedKeys) {
        this.sql = sql;
        this.returnGeneratedKeys = returnGeneratedKeys;
        synchronized (statements) {
            statements.add(this);
        }
    }

    public <T> T select(ResultSetUser<T> user, Object... objects) {
        return withConnection(st -> {
            ResultSet rs = st.executeQuery();
            T result = user.use(rs);
            rs.close();
            return result;
        }, objects);
    }

    public void update(Object... objects) {
        withConnection(PreparedStatement::executeUpdate, objects);
    }

    public int insertGetKey(Object... objects) {
        return withConnection(st -> {
            st.executeUpdate();
            ResultSet rs = st.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);
        }, objects);
    }

    public String getSql() {
        return sql;
    }

    private <T> T withConnection(SQLRunnable<T> runnable, Object... objects) {
        Connection connection = aquireConnection();
        T result;

        try {
            result = tryWithConnection(connection, runnable, objects);
        } catch (Throwable e) {
            if(connectionInvalid(connection)) {
                closeConnection(connection);

                return withConnection(runnable, objects);
            } else {
                synchronized (connections) {
                    connections.push(connection);
                    connections.notify();
                }

                throw new SecurityException("Failing sql statement", e);
            }
        }

        synchronized (connections) {
            connections.push(connection);
            connections.notify();
        }

        return result;
    }

    private boolean connectionInvalid(Connection connection) {
        try {
            return connection.isClosed() || !connection.isValid(1);
        } catch (SQLException e) {
            logger.log(Level.INFO, "Could not check SQL connection status", e); // No database logging possible at this state
            return true;
        }
    }

    private <T> T tryWithConnection(Connection connection, SQLRunnable<T> runnable, Object... objects) throws SQLException {
        PreparedStatement st = cachedStatements.get(connection);
        if(st == null) {
            if(returnGeneratedKeys)
                st = connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS);
            else
                st = connection.prepareStatement(sql);
            cachedStatements.put(connection, st);
        }

        for (int i = 0; i < objects.length; i++) {
            Object o = objects[i];
            if(o != null)
                SqlTypeMapper.getMapper(o.getClass()).write(st, i+1, o);
            else
                st.setNull(i+1, Types.NULL);
        }

        return runnable.run(st);
    }

    @Override
    public void close() {
        cachedStatements.values().forEach(st -> closeStatement(st, false));
        cachedStatements.clear();
        synchronized (statements) {
            statements.remove(this);
        }
    }

    private void close(Connection connection) {
        PreparedStatement st = cachedStatements.remove(connection);
        if(st != null)
            closeStatement(st, true);
    }

    private static Connection aquireConnection() {
        synchronized (connections) {
            while(connections.isEmpty() && connectionBudget <= 0)
                waitOnConnections();

            if(!connections.isEmpty()) {
                return connections.pop();
            } else {
                Connection connection = conProvider.get();
                connectionBudget--;
                return connection;
            }
        }
    }

    private static void closeConnection(Connection connection) {
        synchronized (statements) {
            for (Statement statement : statements) {
                statement.close(connection);
            }
        }
        try {
            connection.close();
        } catch (SQLException e) {
            logger.log(Level.INFO, "Could not close connection", e);
        }

        synchronized (connections) {
            connectionBudget++;
            connections.notify();
        }
    }

    private static void waitOnConnections() {
        synchronized (connections) {
            try {
                connections.wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private static void closeStatement(PreparedStatement st, boolean silent) {
        try {
            st.close();
        } catch (SQLException e) {
            if(!silent)
                logger.log(Level.INFO, "Could not close statement", e);
        }
    }

    public interface ResultSetUser<T> {
        T use(ResultSet rs) throws SQLException;
    }

    private interface SQLRunnable<T> {
        T run(PreparedStatement st) throws SQLException;
    }
}
