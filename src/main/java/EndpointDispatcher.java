package ca.yorku.eecs;

import java.io.IOException;
import java.net.InetSocketAddress;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.util.HashMap;
import java.util.Map;

public class EndpointDispatcher implements HttpHandler {
	private Map<String, HttpHandler> handlers;

	public EndpointDispatcher() {
		handlers = new HashMap<>();
		handlers.put("/api/v1/addActor", new addActor());
		handlers.put("/api/v1/addMovie", new addMovie());
		handlers.put("/api/v1/addRelationship", new addRelationship());
		handlers.put("/api/v1/getActor", new getActor());
		handlers.put("/api/v1/getMovie", new getMovie());
		handlers.put("/api/v1/hasRelationship", new hasRelationship());
		handlers.put("/api/v1/computeBaconNumber", new computeBaconNumber());
		handlers.put("/api/v1/computeBaconPath", new computeBaconPath());
		handlers.put("/api/v1/getCommonMovies", new getCommonMovies());
		handlers.put("/api/v1/highestNumber", new highestNumber());
		handlers.put("/api/v1/findActorWithSameFirstLetter", new findActorWithSameFirstLetter());
	}

	@Override
	public void handle(HttpExchange exchange) throws IOException {
		String path = exchange.getRequestURI().getPath();
		HttpHandler handler = handlers.get(path);

		if (handler != null) {
			handler.handle(exchange);
		} else {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
		}
	}
}