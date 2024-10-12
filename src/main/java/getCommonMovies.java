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

/*
 * This API was designed by out group
 * GET request body (raw JSON) should have format:
 * 
 * {
 * 		"actor1Id": "...",
 * 		"actor2Id": "..."
 * }
 * 
 */

public class getCommonMovies implements HttpHandler {

	public getCommonMovies() {
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
			System.out.println("Received GET request with body: " + deserialized);

			if (!deserialized.has("actor1Id") || !deserialized.has("actor2Id")) {
				r.sendResponseHeaders(400, -1);
				r.getResponseBody().close();
			} else {
				String actor1Id = deserialized.getString("actor1Id");
				String actor2Id = deserialized.getString("actor2Id");

				List<String> commonMovies = getCommonMovies(actor1Id, actor2Id);

				if (commonMovies.isEmpty()) {
					r.sendResponseHeaders(404, -1);
					r.getResponseBody().close();
				} else {
					JSONObject jsonResponse = new JSONObject();
					jsonResponse.put("actor1Id", actor1Id);
					jsonResponse.put("actor2Id", actor2Id);
					jsonResponse.put("commonMovies", new JSONArray(commonMovies));

					byte[] result = jsonResponse.toString(4).getBytes(StandardCharsets.UTF_8);
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

	private List<String> getCommonMovies(String actor1Id, String actor2Id) {
		try (Session session = Utils.driver.session()) {
			return session.writeTransaction(new TransactionWork<List<String>>() {
				@Override
				public List<String> execute(Transaction tx) {
					return queryCommonMovies(tx, actor1Id, actor2Id);
				}
			});
		}
	}

	private List<String> queryCommonMovies(Transaction tx, String actor1Id, String actor2Id) {
		StatementResult result = tx.run(
				"MATCH (actor1:actor {id: $actor1Id})-[:ACTED_IN]->(movie:movie)<-[:ACTED_IN]-(actor2:actor {id: $actor2Id}) "
						+ "RETURN movie.Name AS movieTitle",
				parameters("actor1Id", actor1Id, "actor2Id", actor2Id));

		return result.list(record -> record.get("movieTitle").asString());
	}
}
