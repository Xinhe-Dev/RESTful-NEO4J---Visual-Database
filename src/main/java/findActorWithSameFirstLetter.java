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
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import static org.neo4j.driver.v1.Values.parameters;

public class findActorWithSameFirstLetter implements HttpHandler {

	@Override
	public void handle(HttpExchange r) throws IOException {
		if ("GET".equals(r.getRequestMethod())) {
			handleGetActorsByFirstLetter(r);
		} else {
			r.sendResponseHeaders(405, -1);
			r.getResponseBody().close();
		}
	}

	/**
	 * this method receive request body for a actor, and match all other actor has
	 * same first letter
	 **/
	private void handleGetActorsByFirstLetter(HttpExchange r) throws IOException {
		try {
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);

			if (!deserialized.has("actorName")) {
				r.sendResponseHeaders(400, -1);
				r.getResponseBody().close();
				return;
			}

			String actorName = deserialized.getString("actorName");
			char firstLetter = actorName.charAt(0);

			List<String> actors = getActorsByFirstLetter(firstLetter);
			if (actors.isEmpty()) {
				r.sendResponseHeaders(404, -1);
				r.getResponseBody().close();
			} else {
				JSONArray responseArray = new JSONArray(actors);
				JSONObject response = new JSONObject();
				response.put("actors", responseArray);
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

	private List<String> getActorsByFirstLetter(char firstLetter) {
		try (Session session = Utils.driver.session()) {
			StatementResult result = session.run(
					"MATCH (actor:actor) " + "WHERE actor.name STARTS WITH $firstLetter " + "RETURN actor.name AS name",
					parameters("firstLetter", String.valueOf(firstLetter)));

			return result.list(record -> record.get("name").asString());
		}
	}

	private void sendResponse(HttpExchange exchange, int statusCode, String responseText) throws IOException {
		byte[] responseBytes = responseText.getBytes(StandardCharsets.UTF_8);
		exchange.sendResponseHeaders(statusCode, responseBytes.length);
		OutputStream os = exchange.getResponseBody();
		os.write(responseBytes);
		os.close();
	}
}
