package edu.bulsu.lms.persistence;

import java.sql.Connection;
import java.sql.SQLException;

/** A piece of database work that runs on one open {@link Connection} and may throw {@link SQLException}. */
@FunctionalInterface
public interface SqlFunction<T> {

    T apply(Connection connection) throws SQLException;
}
