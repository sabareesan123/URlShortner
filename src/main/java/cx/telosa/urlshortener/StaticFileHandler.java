

package cx.telosa.urlshortener;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;


public class StaticFileHandler implements HttpHandler {
    private static final Logger logger = LoggerFactory.getLogger(StaticFileHandler.class);
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String path = uri.getPath().equals("/") ? "/index.html" : uri.getPath();
        String filePath = "src/main/resources/static" + path;
        
        File file = new File(filePath).getCanonicalFile();
        
        if (!file.isFile()) {
            sendResponse(exchange, 404, "404 Not Found");
            return;
        }
        
        exchange.getResponseHeaders().set("Content-Type", getContentType(path));
        exchange.sendResponseHeaders(200, file.length());
        
        try (OutputStream os = exchange.getResponseBody(); FileInputStream fs = new FileInputStream(file)) {
            final byte[] buffer = new byte[0x10000];
            int count;
            while ((count = fs.read(buffer)) >= 0) {
                os.write(buffer, 0, count);
            }
        }
    }

    private String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}
