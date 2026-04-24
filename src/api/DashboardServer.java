package api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class DashboardServer {
    public static void start(int port) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/api/network", new NetworkHandler());
            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("--- API Server started on port " + port + " ---");
        } catch (IOException e) {
            System.err.println("Failed to start API Server: " + e.getMessage());
        }
    }

    static class NetworkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String response = StateStore.getInstance().toJson();
            
            // Set Headers
            t.getResponseHeaders().set("Content-Type", "application/json");
            t.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            t.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            
            if (t.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                t.sendResponseHeaders(204, -1);
                return;
            }

            byte[] bytes = response.getBytes();
            t.sendResponseHeaders(200, bytes.length);
            OutputStream os = t.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }
}
