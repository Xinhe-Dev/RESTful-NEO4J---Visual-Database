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

public class getMovie implements HttpHandler {
	public getMovie() {
	}

	@Override
	public void handle(HttpExchange r) throws IOException {
		if ("GET".equals(r.getRequestMethod())) {
			handleGet(r);
		} else {
			r.sendResponseHeaders(405, -1);// same, not accept type
			r.getResponseBody().close();
		}
	}

	// same way to doing like getActor
	private void handleGet(HttpExchange r) throws IOException {
		try {
			String body = Utils.convert(r.getRequestBody());
			JSONObject deserialized = new JSONObject(body);
			System.out.println("Received GET request with body: " + deserialized);

			if (!deserialized.has("movieId")) {
				r.sendResponseHeaders(400, -1);
				r.getResponseBody().close();
			} else {
				String movieId = deserialized.getString("movieId");
				Map<String, Object> response = getData(movieId);

				if (response.isEmpty()) {
					r.sendResponseHeaders(404, -1);
				} else {
					JSONObject responseJSON = new JSONObject();
					responseJSON.put("movieId", movieId);
					responseJSON.put("name", response.get("name"));
					responseJSON.put("actors", response.get("actors"));

					String jsonResponse = "{\n" + "  \"movieId\": \"" + movieId + "\",\n" + "  \"name\": \""
							+ response.get("name").toString().replace("\"", "\\\"") + "\",\n" + "  \"actors\": "
							+ new JSONArray((List) response.get("actors")).toString().replace("[", "[\n    ")
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

	private Map<String, Object> getData(String movieId) {
		try (Session session = Utils.driver.session()) {
			return session.writeTransaction(new TransactionWork<Map<String, Object>>() {
				@Override
				public Map<String, Object> execute(Transaction tx) {
					return queryData(tx, movieId);
				}
			});
		}
	}

	private Map<String, Object> queryData(Transaction tx, String movieId) {
		StatementResult result = tx.run("MATCH (m:movie {id: $movieId}) " + "OPTIONAL MATCH (m)<-[:ACTED_IN]-(a:actor) "
				+ "RETURN m.Name as name, collect(a.id) as actors", parameters("movieId", movieId));

		if (result.hasNext()) {
			return result.single().asMap();
		} else {
			return new HashMap<>(); // Return empty map if no movie found
		}
	}
}
