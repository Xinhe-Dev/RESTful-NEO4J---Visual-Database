package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
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

public class hasRelationship implements HttpHandler {
	public hasRelationship() {
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

	private void handleGet(HttpExchange r) throws IOException {
		try {
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);

			if (!deserialized.has("actorId") || !deserialized.has("movieId")) {
				r.sendResponseHeaders(400, -1);
				r.getResponseBody().close();
				return;
			}

			String actorId = deserialized.getString("actorId");
			String movieId = deserialized.getString("movieId");
			boolean hasRelationship = checkRelationship(actorId, movieId);

			// manually construct JSON response with formatting
			String jsonResponse = "{\n" + "  \"actorId\": \"" + actorId + "\",\n" + "  \"movieId\": \"" + movieId
					+ "\",\n" + "  \"hasRelationship\": " + hasRelationship + "\n" + "}";

			byte[] result = jsonResponse.getBytes(StandardCharsets.UTF_8);
			r.sendResponseHeaders(200, result.length);
			OutputStream os = r.getResponseBody();
			os.write(result);
			os.close();

		} catch (JSONException e) {
			r.sendResponseHeaders(400, -1);
			r.getResponseBody().close();
		} catch (Exception e) {
			r.sendResponseHeaders(500, -1);
			r.getResponseBody().close();
		}
	}

	private boolean checkRelationship(String actorId, String movieId) {
		try (Session session = Utils.driver.session()) {
			StatementResult result = session.run(
					"MATCH (a:actor {id: $actorId}), (m:movie {id: $movieId}) "
							+ "RETURN EXISTS((a)-[:ACTED_IN]->(m)) AS hasRel",
					parameters("actorId", actorId, "movieId", movieId));
			if (result.hasNext()) {
				return result.single().get("hasRel").asBoolean();
			} else {
				return false; // node or relationship not found
			}
		}
	}
}
