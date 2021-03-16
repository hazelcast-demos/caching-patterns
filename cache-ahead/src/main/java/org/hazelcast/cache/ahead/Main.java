package org.hazelcast.cache.ahead;

import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.function.FunctionEx;
import com.hazelcast.jet.Jet;
import com.hazelcast.jet.Util;
import com.hazelcast.jet.cdc.ChangeRecord;
import com.hazelcast.jet.cdc.RecordPart;
import com.hazelcast.jet.cdc.mysql.MySqlCdcSources;
import com.hazelcast.jet.pipeline.Pipeline;
import com.hazelcast.jet.pipeline.Sink;
import com.hazelcast.jet.pipeline.Sinks;
import com.hazelcast.jet.pipeline.StreamSource;
import org.json.JSONObject;

import java.util.Map;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        var jet = Jet.bootstrappedInstance();
        var main = new Main();
        main.waitForDependencies();
        jet.newJob(main.pipeline()).join();
    }

    private void waitForDependencies() throws InterruptedException {
        var env = System.getenv();
        var waitTime = Integer.parseInt(env.getOrDefault("WAIT_TIME", "0"));
        Thread.sleep(waitTime);
    }

    private Pipeline pipeline() {
        var pipeline = Pipeline.create();
        pipeline.readFrom(mysql())
                .withTimestamps(ChangeRecord::timestamp, 200)
                .map(((FunctionEx<? super ChangeRecord, RecordPart>) ChangeRecord::value)
                        .andThen(RecordPart::toMap)
                        .andThen(Main::toJson)
                        .andThen(json -> Util.entry(json.getInt("id"), json.toString(4)))
                ).peek()
                .writeTo(cache());
        return pipeline;
    }

    private static JSONObject toJson(Map<String, Object> map) {
        var json = new JSONObject();
        json.put("id", (int) map.get("id"));
        json.put("first_name", map.get("first_name"));
        json.put("last_name", map.get("last_name"));
        json.put("birthdate", map.get("birthdate"));
        return json;
    }

    private StreamSource<ChangeRecord> mysql() {
        var env = System.getenv();
        var schema = env.getOrDefault("DATABASE_SCHEMA", "patterns");
        return MySqlCdcSources
                .mysql("datastore")
                .setDatabaseAddress(env.getOrDefault("MYSQL_HOST", "localhost"))
                .setDatabasePort(Integer.parseInt(env.getOrDefault("MYSQL_PORT", "3306")))
                .setDatabaseUser(env.getOrDefault("MYSQL_USER", "root"))
                .setDatabasePassword(env.getOrDefault("MYSQL_PASSWORD", "root"))
                .setClusterName(env.getOrDefault("SERVER_NAME", "store"))
                .setDatabaseWhitelist(schema)
                .setTableWhitelist(schema + '.' + env.getOrDefault("NAMESPACED_TABLE", "Person"))
                .build();
    }

    private Sink<Map.Entry<Integer, String>> cache() {
        var clientConfig = new ClientConfig();
        var env = System.getenv();
        var cacheHost = env.get("CACHE_HOST");
        var cacheName = env.get("CACHE_NAME");
        clientConfig.getNetworkConfig().addAddress(cacheHost == null ? "localhost" : cacheHost);
        return Sinks.remoteMap(cacheName == null ? "persons" : cacheName, clientConfig);
    }
}
