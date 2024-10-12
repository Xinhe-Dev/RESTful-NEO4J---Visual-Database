package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;

import static org.neo4j.driver.v1.Values.parameters;

public class getActor implements HttpHandler {
	public getActor() {
	}

	@Override
	public void handle(HttpExchange r) throws IOException {
		if ("GET".equals(r.getRequestMethod())) {
			handleGet(r);
		} else {
			r.sendResponseHeaders(405, -1);
			r.getResponseBody().close();
		}
	}

	/**
	 * extra parameter won't report error 400, but it should not appear as a
	 * property of a node
	 */
	private void handleGet(HttpExchange r) throws IOException {
		try {
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			System.out.println("Received GET request with body: " + deserialized);

			if (!deserialized.has("actorId")) {
				r.sendResponseHeaders(400, -1);
				r.getResponseBody().close();
			} else {
				String actorId = deserialized.getString("actorId");
				Map<String, Object> responseMap = getData(actorId);

				if (responseMap.isEmpty()) {
					r.sendResponseHeaders(404, -1);
					r.getResponseBody().close();
				} else {
					// manually constructing JSON with new lines and indentation
					String jsonResponse = "{\n" + "  \"actorId\": \"" + actorId + "\",\n" + "  \"name\": \""
							+ responseMap.get("name").toString().replace("\"", "\\\"") + "\",\n" + "  \"movies\": "
							+ new JSONArray((List) responseMap.get("movies")).toString().replace("[", "[\n    ")
									.replace("]", "\n  ]").replace(",", ",\n    ")
							+ "\n}";

					byte[] result = jsonResponse.getBytes(StandardCharsets.UTF_8);
					r.sendResponseHeaders(200, result.length);
					OutputStream os = r.getResponseBody();
					os.write(result);
					os.close();
				}
			}
		} catch (JSONException e) {
			r.sendResponseHeaders(400, -1);
			r.getResponseBody().close();
		} catch (Exception e) {
			r.sendResponseHeaders(500, -1);
			r.getResponseBody().close();
		}
	}

	// method use to handle the returned body
	private Map<String, Object> getData(String actorId) {
		try (Session session = Utils.driver.session()) {
			return session.writeTransaction(new TransactionWork<Map<String, Object>>() {
				@Override
				public Map<String, Object> execute(Transaction tx) {
					return queryData(tx, actorId);
				}
			});
		}
	}

	private Map<String, Object> queryData(Transaction tx, String actorId) {
		StatementResult result = tx.run("MATCH (a:actor {id: $actorId}) " + "OPTIONAL MATCH (a)-[:ACTED_IN]->(m:movie) "
				+ "RETURN a.Name as name, collect(m.id) as movies", parameters("actorId", actorId));

		if (result.hasNext()) {
			return result.single().asMap();
		} else {
			return new HashMap<>(); // return empty map if no actor found
		}
	}
}
