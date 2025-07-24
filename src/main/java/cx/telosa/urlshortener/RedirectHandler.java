
/* File: src/main/java/com/example/urlshortener/RedirectHandler.java */
package cx.telosa.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


public class RedirectHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(RedirectHandler.class);
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String shortCode = path.substring(path.lastIndexOf('/') + 1);
        logger.info("Short code  {}", shortCode);
        DatabaseService db = DatabaseService.getInstance();

        Optional<String> longUrl = db.getLongUrl(shortCode);
        logger.info("Long  url  {}", longUrl);
        if (longUrl.isPresent()) {
            exchange.getResponseHeaders().set("Location", longUrl.get());
            exchange.sendResponseHeaders(301, -1); // 301 Moved Permanently
            logger.info("Redirecting {} to {}", shortCode, longUrl.get());
        } else {
            String response = "404 Not Found";
            exchange.sendResponseHeaders(404, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
            logger.warn("Short code not found: {}", shortCode);
        }
    }
}
