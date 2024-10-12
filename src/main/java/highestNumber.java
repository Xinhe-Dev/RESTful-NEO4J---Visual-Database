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

public class highestNumber implements HttpHandler {

	/**
	 * this designed api discover the actor that most frequently collaborate with
	 * Kevin Bacon
	 */
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

			if (!deserialized.has("actorId")) {
				r.sendResponseHeaders(400, -1);
				r.getResponseBody().close();
				return;
			}

			String actorId = deserialized.getString("actorId");

			int highestNumber = findHighestBaconNumber(actorId);
			if (highestNumber == -1) {
				r.sendResponseHeaders(404, -1);
				r.getResponseBody().close();
			} else {
				JSONObject response = new JSONObject();
				response.put("highestNumber", highestNumber);
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

	private int findHighestBaconNumber(String actorId) {
		try (Session session = Utils.driver.session()) {
			StatementResult result = session.run("MATCH (kevin:actor {id: 'nm0000102'}), (actor:actor) "
					+ "WHERE actor.id <> 'nm0000102' " + "WITH kevin, actor " + "MATCH path = (kevin)-[*]-(actor) "
					+ "WITH actor, path " + "WHERE length(path) > 0 "
					+ "RETURN actor.id AS actorId, length(path) AS distance " + "ORDER BY distance DESC LIMIT 1");
			if (result.hasNext()) {
				return result.single().get("distance").asInt() / 2;// same as computerBaconNumber
			}
		}
		return -1;
	}

	private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
		byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(statusCode, responseBytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(responseBytes);
		os.close();
	}

}