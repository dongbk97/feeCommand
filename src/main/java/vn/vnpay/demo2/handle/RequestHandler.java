package vn.vnpay.demo2.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.demo2.bean.FeeCommand;
import vn.vnpay.demo2.common.CommonUtil;
import vn.vnpay.demo2.common.HttpStatus;
import vn.vnpay.demo2.common.RequestMethod;
import vn.vnpay.demo2.service.impl.TransactionServiceImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


public class RequestHandler implements HttpHandler {
    public static final ThreadLocal<String> logIdThreadLocal = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger(RequestHandler.class);


    @Override
    public void handle(HttpExchange exchange) {
        String requestMethod = exchange.getRequestMethod();
        String logId = CommonUtil.generateLogId();
        logIdThreadLocal.set(logId);
        logger.info("[{}] - Received {} request with endpoint: {}", logId, requestMethod, exchange.getRequestURI());
        try {
            if (requestMethod.equalsIgnoreCase(RequestMethod.GET.name())) {
                this.handleGet(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.POST.name())) {
                this.handlePost(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.PUT.name())) {
                this.handlePut(exchange);
            } else if (requestMethod.equalsIgnoreCase(RequestMethod.DELETE.name())) {
                this.handleDelete(exchange);
            } else {
                sendResponse(exchange, "Method not supported", 405);
            }
        } catch (IOException e) {
            logger.error("Handle request fail ", e);
        }
    }

    private void handleGet(HttpExchange httpExchange) throws IOException {
        Map<String, List<String>> params = CommonUtil.splitQuery(httpExchange.getRequestURI().getRawQuery());
        String response = "GET request received. Params: " + params;
        this.sendResponse(httpExchange, response, HttpStatus.SUCCESS.getCode());
    }

    private void handlePost(HttpExchange httpExchange) {
        long start = System.currentTimeMillis();
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start handle request POST in RequestHandler", logId);

        try {
            this.logClientIP(httpExchange);
            String endpoint = httpExchange.getRequestURI().getPath();
            TransactionServiceImpl transactionService = new TransactionServiceImpl();
            FeeCommandRequest feeCommandRequest = getPaymentRequestBody(httpExchange);
            if ("/init".equalsIgnoreCase(endpoint)) {
                FeeCommand feeCommand = new FeeCommand();
                feeCommand.setTotalFee(feeCommandRequest.getTotalFee());
                feeCommand.setTotalRecord(feeCommandRequest.getTotalRecord());
                transactionService.initTransaction(feeCommand);
                this.sendResponse(httpExchange, HttpStatus.SUCCESS.getMessage(), HttpStatus.SUCCESS.getCode());
            } else if ("/update".equalsIgnoreCase(endpoint)) {
                transactionService.updateStatus(feeCommandRequest.getCommandCode());
            }
            long end = System.currentTimeMillis();
            logger.info("[{}] - Process request in RequestHandler take {} millisecond ", logId, (end - start));
        } catch (Exception e) {
            logger.error("[{}] - Error processing payment request", logId, e);
            this.sendResponse(httpExchange, HttpStatus.INTERNAL_SERVER_ERROR.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.getCode());
        } finally {
            httpExchange.close();
        }
    }

    private void logClientIP(HttpExchange httpExchange) {
        String logId = logIdThreadLocal.get();
        try {
            String clientIP = httpExchange.getRemoteAddress().getAddress().getHostAddress();
            logger.info("[{}] - Handle request with clientIp {} ", logId, clientIP);
        } catch (Exception e) {
            logger.error("[{}] - Error getting client IP", logId, e);
        }
    }

    private FeeCommandRequest getPaymentRequestBody(HttpExchange httpExchange) throws IOException {
        String logId = logIdThreadLocal.get();
        InputStream is = httpExchange.getRequestBody();
        byte[] bytes = CommonUtil.getBytesFromInputStream(is);
        FeeCommandRequest feeCommandRequest = CommonUtil.bytesToObject(bytes, FeeCommandRequest.class);
        String responseLog = CommonUtil.objectToJson(feeCommandRequest);
        logger.info("[{}] - Handle request with requestBody: {} ", logId, responseLog);
        return feeCommandRequest;
    }


    private void handlePut(HttpExchange httpExchange) throws IOException {
        InputStream is = httpExchange.getRequestBody();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String requestBody = reader.readLine();
        String response = "PUT request received. Data: " + requestBody;
        this.sendResponse(httpExchange, response, HttpStatus.SUCCESS.getCode());
    }

    private void handleDelete(HttpExchange httpExchange) {
        String response = "DELETE request received";
        this.sendResponse(httpExchange, response, HttpStatus.SUCCESS.getCode());
    }

    private void sendResponse(HttpExchange httpExchange, Object response, int statusCode) {
        String logId = logIdThreadLocal.get();
        try {
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            String responseString = CommonUtil.objectToJson(response);
            httpExchange.sendResponseHeaders(statusCode, responseString.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(responseString.getBytes());
            os.close();
        } catch (IOException e) {
            logger.error("[{}] - Occur error when write response", logId, e);
        }
    }

}
