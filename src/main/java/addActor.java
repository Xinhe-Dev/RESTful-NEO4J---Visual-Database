package ca.yorku.eecs;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.StatementResult;
import static org.neo4j.driver.v1.Values.parameters;

public class addActor implements HttpHandler {
	public addActor() {
		System.out.println("Constructor Initialized");
	}

	public void handle(HttpExchange r) {
		try {
			if (r.getRequestMethod().equals("PUT")) {
				System.out.println("Handle request accepted");
				handlePut(r);
			} else {
				System.out.println("Handle request denied");
				r.sendResponseHeaders(404, -1);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Extra parameter won't report error 400, but it should not appear as a
	 * property of a node
	 */
	public void handlePut(HttpExchange r) throws IOException, JSONException {
		System.out.println("Request body initialized");
		String body = Utils.convert(r.getRequestBody());
		JSONObject deserialized = new JSONObject(body);
		final int[] statusCode = { 200 };
		String name;
		String actorId;

		try {
			name = deserialized.getString("name");
			actorId = deserialized.getString("actorId");
			// debug code to check the request body send successful
			System.out.println("Detected new Actor: [" + name + "] - ActorID: " + actorId);
		} catch (JSONException e) {
			r.sendResponseHeaders(400, -1);
			return;
		}

		try (Session session = Utils.driver.session()) {
			session.writeTransaction(tx -> {
				StatementResult result = tx.run("MATCH (a:actor {id: $actorId}) RETURN a",
						parameters("actorId", actorId));
				if (result.hasNext()) {
					// actor already exists
					statusCode[0] = 400;
				} else {
					// create new actor
					tx.run("CREATE (a:actor {id: $actorId, Name: $name})",
							parameters("actorId", actorId, "name", name));
				}
				return null;
			});
		} catch (Exception e) {
			System.err.println("Caught Exception: " + e.getMessage());
			statusCode[0] = 500;
		}

		r.sendResponseHeaders(statusCode[0], -1);
	}
}
