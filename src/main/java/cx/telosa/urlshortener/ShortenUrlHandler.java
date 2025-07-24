

package cx.telosa.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;


public class ShortenUrlHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(ShortenUrlHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("req body  " + requestBody);
            Map<String, String> params = Utils.parseJson(requestBody);
            System.out.println("params " + params);
            String longUrl = params.get("url");
            System.out.println(" url handler " + longUrl);
            if (longUrl == null || longUrl.trim().isEmpty()) {
                sendResponse(exchange, 400, "{\"error\":\"URL is required\"}");
                return;
            }

            DatabaseService db = DatabaseService.getInstance();
            long id = db.saveUrl(longUrl, null, null);
            String shortCode = UrlShortener.encode(id);
            

            String sql = "UPDATE urls SET short_code = ? WHERE id = ?";
            try(var conn = db.getConnection(); var pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, shortCode);
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }

            String shortUrl = "http://" + exchange.getRequestHeaders().getFirst("Host") + "/r/" + shortCode;
            String responseJson = String.format("{\"shortUrl\":\"%s\"}", shortUrl);
            sendResponse(exchange, 200, responseJson);

        } catch (Exception e) {
            logger.error("Error handling shorten request", e);
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error\"}");
        }
    }
    
    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
}
