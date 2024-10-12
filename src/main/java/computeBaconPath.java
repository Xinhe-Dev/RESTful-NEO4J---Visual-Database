package ca.yorku.eecs;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.Value;

import static org.neo4j.driver.v1.Values.parameters;

public class computeBaconPath implements HttpHandler {

	public computeBaconPath() {
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

	// use the same way as computeBaconNumber, it seems pretty same
	private void handleGet(HttpExchange r) throws IOException {
		try {
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);

			if (!deserialized.has("actorId")) {
				r.sendResponseHeaders(400, -1);
				r.getResponseBody().close();
				return;
			}

			String actorId = deserialized.getString("actorId");
			String baconId = "nm0000102"; // Kevin Bacon's actor ID
			if (actorId.equals(baconId)) {
				JSONArray baconPath = new JSONArray();
				baconPath.put(baconId);
				sendResponse(r, 200, new JSONObject().put("baconPath", baconPath).toString(4));
				return;
			}

			List<String> baconPath = computePath(actorId, baconId);
			if (baconPath == null || baconPath.isEmpty()) {
				r.sendResponseHeaders(404, -1);
				r.getResponseBody().close();
			} else {
				JSONArray pathArray = new JSONArray(baconPath);
				JSONObject responseJSON = new JSONObject().put("baconPath", pathArray);
				sendResponse(r, 200, responseJSON.toString(4));
			}

		} catch (JSONException e) {
			r.sendResponseHeaders(400, -1);
			r.getResponseBody().close();
		} catch (Exception e) {
			r.sendResponseHeaders(500, -1);
			r.getResponseBody().close();
		}
	}

	private List<String> computePath(String actorId, String baconId) {
		try (Session session = Utils.driver.session()) {
			StatementResult result = session.run(
					"MATCH path = shortestPath((bacon:actor {id: $baconId})-[*]-(actor:actor {id: $actorId})) "
							+ "RETURN [node in nodes(path) WHERE 'actor' in labels(node) AND EXISTS(node.id) | node.id] as ids",
					parameters("actorId", actorId, "baconId", baconId));
			if (result.hasNext()) {
				return result.single().get("ids").asList(Value::asString);
			}
		}
		return null;
	}

	private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
		byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(statusCode, responseBytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(responseBytes);
		os.close();
	}
}
