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

public class addMovie implements HttpHandler {
	public addMovie() {
	}

	public void handle(HttpExchange r) throws IOException {
		if ("PUT".equals(r.getRequestMethod())) {
			handlePut(r);
		} else {
			r.sendResponseHeaders(404, -1);
		}
	}

	/**
	 * Extra parameter won't report error 400, but it should not appear as a
	 * property of a node
	 */
	private void handlePut(HttpExchange r) throws IOException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject deserialized;
		final int[] statusCode = { 200 };
		String name;
		String movieId;

		try {
			deserialized = new JSONObject(body);
			name = deserialized.getString("name");
			movieId = deserialized.getString("movieId");
			System.out.println("Detected new Movie: [" + name + "] - MovieID: " + movieId);
		} catch (JSONException e) {
			r.sendResponseHeaders(400, -1);
			return;
		}

		try (Session session = Utils.driver.session()) {
			session.writeTransaction(tx -> {
				StatementResult result = tx.run("MATCH (m:movie {id: $movieId}) RETURN m",
						parameters("movieId", movieId));
				if (result.hasNext()) {
					// movie already exists
					statusCode[0] = 400;
				} else {
					// create new movie
					tx.run("CREATE (m:movie {id: $movieId, Name: $name})",
							parameters("movieId", movieId, "name", name));
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
