package org.hazelcast.cache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.MapLoader;
import com.hazelcast.map.MapLoaderLifecycleSupport;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class SqlMapLoader implements MapLoader<Integer, String>, MapLoaderLifecycleSupport {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlMapLoader.class);
    private HikariDataSource dataSource;

    public String load(Integer key) {
        try (var connection = dataSource.getConnection();
             var statement =
                     connection.prepareStatement("SELECT id, first_name, last_name, birthdate FROM Person WHERE id = ?")) {
            statement.setInt(1, key);
            var resultSet = statement.executeQuery();
            if (resultSet.isAfterLast()) {
                return null;
            }
            resultSet.next();
            LOGGER.info("Person with pk {} has been loaded from the data store", key);
            return toString(resultSet);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<Integer, String> loadAll(Collection<Integer> keys) {
        try (var connection = dataSource.getConnection();
             var statement =
                     connection.prepareStatement("SELECT id, first_name, last_name, birthdate FROM Person WHERE id IN (SELECT ?)")) {
            var selectIn = keys.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));
            statement.setString(1, selectIn);
            var resultSet = statement.executeQuery();
            var map = new HashMap<Integer, String>();
            while (resultSet.next()) {
                var key = resultSet.getInt("id");
                var value = toString(resultSet);
                map.put(key, value);
            }
            LOGGER.info("Persons with keys {} have been loaded from the data store", selectIn);
            return map;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Iterable<Integer> loadAllKeys() {
        try (var connection = dataSource.getConnection();
             var statement =
                     connection.prepareStatement("SELECT id FROM Person")) {
            var resultSet = statement.executeQuery();
            var ids = new ArrayList<Integer>();
            while (resultSet.next()) {
                ids.add(resultSet.getInt("id"));
            }
            LOGGER.info("All keys have been loaded from the data store");
            return ids;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void init(HazelcastInstance instance, Properties props, String mapName) {
        var url = props.getProperty("jdbc.url");
        var config = new HikariConfig();
        config.setJdbcUrl(url);
        dataSource = new HikariDataSource(config);
    }

    public void destroy() {
        dataSource.close();
    }

    private String toString(ResultSet resultSet) throws SQLException {
        var id = resultSet.getInt("id");
        var firstName = resultSet.getString("first_name");
        var lastName = resultSet.getString("last_name");
        var birthdate = resultSet.getString("birthdate");
        var json = new JSONObject();
        json.put("id", id);
        json.put("first_name", firstName);
        json.put("last_name", lastName);
        if (birthdate != null) {
            json.put("birthdate", birthdate);
        }
        return json.toString(4);
    }
}
