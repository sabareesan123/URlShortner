
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
import java.util.Optional;


public class LoginHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);

    @Override
    public void handle(HttpExchange exchange) throws IOException {
         if (!"POST".equals(exchange.getRequestMethod())) {
            sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            return;
        }

        try (InputStream is = exchange.getRequestBody()) {
            String requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = Utils.parseJson(requestBody);
            
            String username = params.get("username");
            String password = params.get("password");


            DatabaseService db = DatabaseService.getInstance();
            Optional<User> userOpt = db.findUserByUsername(username);

            if (userOpt.isPresent()) {

                if (userOpt.get().getPassword().equals(password)) {
                     sendResponse(exchange, 200, "{\"message\":\"Login successful\"}");
                } else {
                     sendResponse(exchange, 401, "{\"error\":\"Invalid credentials\"}");
                }
            } else {

                String insertUserSql = "INSERT INTO users (username, password) VALUES (?, ?)";
                try (var conn = db.getConnection(); var pstmt = conn.prepareStatement(insertUserSql)) {
                    pstmt.setString(1, username);
                    pstmt.setString(2, password); // NEVER store plain text passwords in production!
                    pstmt.executeUpdate();
                    logger.info("Created new user: {}", username);
                    sendResponse(exchange, 200, "{\"message\":\"User created and logged in\"}");
                } catch (Exception e) {
                    logger.error("Error creating new user", e);
                    sendResponse(exchange, 500, "{\"error\":\"Could not create user\"}");
                }
            }
        } catch (Exception e) {
            logger.error("Error during login", e);
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
