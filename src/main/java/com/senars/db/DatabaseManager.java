package com.senars.db;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

/**
 * Manages the lifecycle of the MapDB embedded database.
 * This class provides a centralized point of access to the database instance
 * and ensures that it is properly opened on startup and closed on shutdown.
 */
public class DatabaseManager implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

    private final DB db;

    /**
     * Creates and initializes the database at the specified file path.
     *
     * @param dbFilePath The path to the database file.
     */
    public DatabaseManager(Path dbFilePath) {
        LOGGER.info("Initializing database at: {}", dbFilePath);
        this.db = DBMaker
                .fileDB(dbFilePath.toFile())
                .transactionEnable() // Enable transactions for data safety
                .closeOnJvmShutdown()  // Ensure the DB is closed gracefully
                .make();
    }

    /**
     * Gets or creates a persistent map with the given name.
     *
     * @param name The name of the map.
     * @param <K>  The key type.
     * @param <V>  The value type.
     * @return A thread-safe, persistent map.
     */
    public <K, V> ConcurrentMap<K, V> getPersistentMap(String name) {
        // Using the default Java serializer for simplicity.
        // For performance, custom serializers would be better.
        return db.hashMap(name, Serializer.JAVA, Serializer.JAVA).createOrOpen();
    }

    /**
     * Commits the current transaction, making all changes since the last commit durable.
     */
    public void commit() {
        db.commit();
        LOGGER.debug("Database transaction committed.");
    }

    /**
     * Rolls back the current transaction, discarding all changes since the last commit.
     */
    public void rollback() {
        db.rollback();
        LOGGER.warn("Database transaction rolled back.");
    }

    /**
     * Closes the database connection.
     */
    @Override
    public void close() {
        if (db != null && !db.isClosed()) {
            db.close();
            LOGGER.info("Database closed successfully.");
        }
    }
}
