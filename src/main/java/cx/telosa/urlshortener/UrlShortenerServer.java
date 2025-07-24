package cx.telosa.urlshortener;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class UrlShortenerServer {

    private static final Logger logger = LoggerFactory.getLogger(UrlShortenerServer.class);

    public static void main(String[] args) throws IOException {
        int port = 8080;
        DatabaseService.getInstance().initializeDatabase();

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        

        server.createContext("/api/shorten", new ShortenUrlHandler());

        server.createContext("/api/login", new LoginHandler());

        server.createContext("/api/custom", new CustomUrlHandler());

        server.createContext("/", new StaticFileHandler());

        server.createContext("/r/", new RedirectHandler());


        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        logger.info("Server started on port {}", port);
    }
}
