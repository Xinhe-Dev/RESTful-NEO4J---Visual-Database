package ca.yorku.eecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Config;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;

public class Utils {
	public static String uriDb = "bolt://localhost:7687";
	static Config config = Config.builder().withoutEncryption().build();

	public static Driver driver = GraphDatabase.driver(uriDb, AuthTokens.basic("neo4j", "12345678"), config);

	public static String convert(InputStream inputStream) throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
			return br.lines().collect(Collectors.joining(System.lineSeparator()));
		}
	}
}
