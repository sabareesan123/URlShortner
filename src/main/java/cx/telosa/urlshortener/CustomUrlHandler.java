

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


public class CustomUrlHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomUrlHandler.class);
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = Utils.parseJson(requestBody);
            
            String longUrl = params.get("url");
            String customCode = params.get("customCode");
            String username = params.get("username");

            if (longUrl == null || customCode == null || username == null) {
                sendResponse(exchange, 400, "{\"error\":\"URL, custom code, and username are required\"}");
                return;
            }

            DatabaseService db = DatabaseService.getInstance();
            if (db.isShortCodeTaken(customCode)) {
                sendResponse(exchange, 409, "{\"error\":\"Custom name is already taken\"}");
                return;
            }

            var userOpt = db.findUserByUsername(username);
            if (userOpt.isEmpty()) {
                sendResponse(exchange, 401, "{\"error\":\"User not found\"}");
                return;
            }

            db.saveUrl(longUrl, customCode, userOpt.get().getId());
            String shortUrl = "http://" + exchange.getRequestHeaders().getFirst("Host") + "/r/" + customCode;
            String responseJson = String.format("{\"shortUrl\":\"%s\"}", shortUrl);
            sendResponse(exchange, 200, responseJson);

        } catch (Exception e) {
            logger.error("Error creating custom URL", e);
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

