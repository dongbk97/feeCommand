package vn.vnpay.demo2.controller;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.demo2.handle.RequestHandler;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class FeeController {
    static Logger logger = LoggerFactory.getLogger(FeeController.class);

    public void start(List<String> pathList) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            RequestHandler requestHandler = new RequestHandler();
            pathList.forEach(t -> server.createContext(t, requestHandler));
            server.setExecutor(new ThreadPoolExecutor(4, 8, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100)));
            server.start();
            logger.info("Server is running on port 8080");
        } catch (Exception e) {
            logger.error("Server is started failed ", e);
        }
    }
}
