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
import static org.neo4j.driver.v1.Values.parameters;

public class computeBaconNumber implements HttpHandler {
	public computeBaconNumber() {
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
	 * What is method is just count the number from the given actorId going path to
	 * Kevin Bacon node, count the steps
	 */
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
				JSONObject response = new JSONObject();
				response.put("baconNumber", 0);
				sendResponse(r, 200, response.toString());
				return;
			}

			int baconNumber = computeBaconNumber(actorId, baconId);
			if (baconNumber == -1) {
				r.sendResponseHeaders(404, -1);
				r.getResponseBody().close();
			} else {
				JSONObject response = new JSONObject();
				response.put("baconNumber", baconNumber);
				sendResponse(r, 200, response.toString());
			}

		} catch (JSONException e) {
			r.sendResponseHeaders(400, -1);
			r.getResponseBody().close();
		} catch (Exception e) {
			r.sendResponseHeaders(500, -1);
			r.getResponseBody().close();
		}
	}

	private int computeBaconNumber(String actorId, String baconId) {
		try (Session session = Utils.driver.session()) {
			StatementResult result = session
					.run("MATCH p=shortestPath((bacon:actor {id: $baconId})-[*]-(actor:actor {id: $actorId})) "
							+ "RETURN length(p) as distance", parameters("actorId", actorId, "baconId", baconId));
			if (result.hasNext()) {
				return result.single().get("distance").asInt() / 2; // divide by 2 because each relationship in the path represents two nodes
			}
		}
		return -1; // no path found
	}

	private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
		byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(statusCode, responseBytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(responseBytes);
		os.close();
	}
}
