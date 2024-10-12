package ca.yorku.eecs;

import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.neo4j.driver.v1.TransactionWork;
import org.neo4j.driver.v1.StatementResult;

import static org.neo4j.driver.v1.Values.parameters;

public class addRelationship implements HttpHandler {

	public addRelationship() {
	}

	@Override
	public void handle(HttpExchange r) throws IOException {
		if ("PUT".equals(r.getRequestMethod())) {
			handlePut(r);
		} else {
			r.sendResponseHeaders(404, -1); // wrong request type
		}
	}

	/**
	 * extra parameter won't report error 400, but it should not appear as a
	 * property of a node
	 */
	private void handlePut(HttpExchange r) throws IOException {
		String body = Utils.convert(r.getRequestBody());
		JSONObject deserialized;
		String actorId;
		String movieId;

		try {
			deserialized = new JSONObject(body);
			actorId = deserialized.getString("actorId");
			movieId = deserialized.getString("movieId");
			System.out.println("Detected new relationship: ActorID [" + actorId + "] - MovieID [" + movieId + "]");
		} catch (JSONException e) {
			r.sendResponseHeaders(400, -1);
			return;
		}

		try (Session session = Utils.driver.session()) {
			StatementResult result = session.writeTransaction(new TransactionWork<StatementResult>() {
				@Override
				public StatementResult execute(Transaction tx) {
					return tx.run(
							"MATCH (a:actor {id: $actorId}), (m:movie {id: $movieId}) " + "MERGE (a)-[r:ACTED_IN]->(m) "
									+ "RETURN count(r) as relationshipCount",
							parameters("actorId", actorId, "movieId", movieId));
				}
			});

			int relationshipCount = result.single().get("relationshipCount").asInt();
			if (relationshipCount > 0) {
				r.sendResponseHeaders(200, -1);
			} else {
				// this implies the relationship was not created
				r.sendResponseHeaders(400, -1);
			}
		} catch (Exception e) {
			r.sendResponseHeaders(500, -1);
		}
	}
}