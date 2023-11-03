package vn.vnpay.fee.handle;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;
import vn.vnpay.fee.bean.FeeCommand;
import vn.vnpay.fee.common.CommonUtil;
import vn.vnpay.fee.common.GeneralResponse;
import vn.vnpay.fee.common.HttpStatus;
import vn.vnpay.fee.common.RequestMethod;
import vn.vnpay.fee.config.redis.RedisConfig;
import vn.vnpay.fee.service.TransactionService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;


public class RequestHandler implements HttpHandler {
    private final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
    private final ThreadLocal<String> logIdThreadLocal = new ThreadLocal<>();

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
                sendResponse(exchange, "Method not supported", null, 405,
                        HttpStatus.INTERNAL_SERVER_ERROR.getMessageStatus());
            }
        } catch (IOException e) {
            logger.error("[{}] - Handle request fail ", logId, e);
        }
    }

    private void handleGet(HttpExchange httpExchange) throws IOException {
        Map<String, List<String>> params = CommonUtil.splitQuery(httpExchange.getRequestURI().getRawQuery());
        String response = "GET request received. Params: " + params;
        this.sendResponse(httpExchange, response, null, HttpStatus.SUCCESS.getCode(),
                HttpStatus.SUCCESS.getMessageStatus());
    }

    private void handlePost(HttpExchange httpExchange) {
        long start = System.currentTimeMillis();
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start handle request POST in RequestHandler", logId);
        try {
            this.logClientIP(httpExchange);
            FeeCommandRequest feeCommandRequest = this.getPaymentRequestBody(httpExchange);

            boolean isExpired = this.checkIfExpiredRequest(feeCommandRequest, httpExchange);
            boolean isExistRequestId = this.checkIfExistsRequestId(feeCommandRequest.getRequestId(), httpExchange, isExpired);

            if (!isExpired && !isExistRequestId) {
                logger.info("[{}] - Start process fee command ", logId);
                this.processInitFee(httpExchange, feeCommandRequest);
            }
        } catch (Exception e) {
            logger.error("[{}] - Error processing payment request", logId, e);
            this.sendResponse(httpExchange, HttpStatus.INTERNAL_SERVER_ERROR.getMessage(), null,
                    HttpStatus.INTERNAL_SERVER_ERROR.getCode(), HttpStatus.INTERNAL_SERVER_ERROR.getMessageStatus());
        } finally {
            logIdThreadLocal.remove();
            httpExchange.close();
            long end = System.currentTimeMillis();
            logger.info("[{}] - Process request in RequestHandler take {} millisecond ", logId, (end - start));
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
        logger.info("[{}] - Handle {} request with requestBody: {} ", logId, httpExchange.getRequestMethod(), responseLog);
        return feeCommandRequest;
    }

    private void processInitFee(HttpExchange httpExchange, FeeCommandRequest feeCommandRequest) {
        String logId = logIdThreadLocal.get();
        this.pushKeyToRedis(feeCommandRequest);
        String endpoint = httpExchange.getRequestURI().getPath();
        logger.info("[{}] - Process request with endpoint: {} ", logId, endpoint);
        TransactionService transactionService = TransactionService.getInstance();
        this.initFeeCommand(transactionService, feeCommandRequest, httpExchange);
    }


    private void initFeeCommand(TransactionService transactionService, FeeCommandRequest feeCommandRequest,
                                HttpExchange httpExchange) {
        String logId = logIdThreadLocal.get();
        FeeCommand feeCommand = new FeeCommand();
        feeCommand.setTotalFee(feeCommandRequest.getTotalFee());
        feeCommand.setTotalRecord(feeCommandRequest.getTotalRecord());
        boolean resultInitFee = transactionService.initFeeCommand(feeCommand, logId);
        if (resultInitFee) {
            logger.info("[{}] - Init fee command successfully", logId);
            this.sendResponse(httpExchange, HttpStatus.SUCCESS.getMessage(), feeCommand.getCommandCode(),
                    HttpStatus.SUCCESS.getCode(), HttpStatus.SUCCESS.getMessageStatus());
        } else {
            logger.info("[{}] - Init fee command false", logId);
            this.sendResponse(httpExchange, HttpStatus.FAILURE.getMessage(), null,
                    HttpStatus.FAILURE.getCode(), HttpStatus.FAILURE.getMessageStatus());
        }

    }

    private void handlePut(HttpExchange httpExchange) throws IOException {
        long start = System.currentTimeMillis();
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start handle request PUT in RequestHandler", logId);
        try {
            this.logClientIP(httpExchange);
            FeeCommandRequest feeCommandRequest = this.getPaymentRequestBody(httpExchange);

            boolean isExpired = this.checkIfExpiredRequest(feeCommandRequest, httpExchange);
            boolean isExistRequestId = this.checkIfExistsRequestId(feeCommandRequest.getRequestId(), httpExchange, isExpired);

            if (!isExpired && !isExistRequestId) {
                boolean resultUpdateFee = this.processUpdateFee(httpExchange, feeCommandRequest);
                if (resultUpdateFee) {
                    this.sendResponse(httpExchange, HttpStatus.SUCCESS.getMessage(), null,
                            HttpStatus.SUCCESS.getCode(), HttpStatus.SUCCESS.getMessageStatus());
                } else {
                    this.sendResponse(httpExchange, HttpStatus.FAILURE.getMessage(), null,
                            HttpStatus.FAILURE.getCode(), HttpStatus.FAILURE.getMessageStatus());
                }
            }

        } catch (Exception e) {
            logger.error("[{}] - Error processing update fee", logId, e);
            this.sendResponse(httpExchange, HttpStatus.INTERNAL_SERVER_ERROR.getMessage(), null,
                    HttpStatus.INTERNAL_SERVER_ERROR.getCode(), HttpStatus.INTERNAL_SERVER_ERROR.getMessageStatus());
        } finally {
            logIdThreadLocal.remove();
            httpExchange.close();
            long end = System.currentTimeMillis();
            logger.info("[{}] - Process PUT request to update fee in RequestHandler take {} millisecond ", logId, (end - start));
        }
    }

    private boolean processUpdateFee(HttpExchange httpExchange, FeeCommandRequest feeCommandRequest) {
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start process update fee transaction ", logId);
        this.pushKeyToRedis(feeCommandRequest);
        String endpoint = httpExchange.getRequestURI().getPath();
        logger.info("[{}] - Process request update fee with endpoint: {} ", logId, endpoint);
        TransactionService transactionService = TransactionService.getInstance();
        return transactionService.updateFee(feeCommandRequest.getCommandCode(), logId);
    }
    private void handleDelete(HttpExchange httpExchange) {
        String response = "DELETE request received";
        this.sendResponse(httpExchange, response, null, HttpStatus.SUCCESS.getCode(), HttpStatus.SUCCESS.getMessageStatus());
    }


    private boolean checkIfExpiredRequest(FeeCommandRequest feeCommandRequest, HttpExchange httpExchange) {
        String logId = logIdThreadLocal.get();
        boolean isExpired;
        isExpired = CommonUtil.isExpired(feeCommandRequest.getRequestTime());
        if (isExpired) {
            logger.info("[{}] - Request with requestId: {} is expired", logId, feeCommandRequest.getRequestId());
            this.sendResponse(httpExchange, HttpStatus.EXPIRED_REQUEST.getMessage(), null,
                    HttpStatus.EXPIRED_REQUEST.getCode(), HttpStatus.EXPIRED_REQUEST.getMessageStatus());
        }
        return isExpired;
    }

    private boolean checkIfExistsRequestId(String requestId, HttpExchange httpExchange, boolean isExpired) {
        String logId = logIdThreadLocal.get();
        boolean isExistsRequestId;
        isExistsRequestId = this.isExistKey(requestId);
        if (isExistsRequestId && !isExpired) {
            logger.info("[{}] -Request with requestId: {} is duplicated", logId, requestId);
            this.sendResponse(httpExchange, HttpStatus.DUPLICATE_REQUEST_ID.getMessage(), null,
                    HttpStatus.DUPLICATE_REQUEST_ID.getCode(), HttpStatus.DUPLICATE_REQUEST_ID.getMessageStatus());
        }
        return isExistsRequestId;
    }


    private boolean isExistKey(String requestId) {
        String logId = logIdThreadLocal.get();
        RedisConfig redisConfig = RedisConfig.getInstance();
        Jedis jedis = null;
        try {
            jedis = redisConfig.getJedisPool().getResource();
            return jedis.exists(requestId);
        } catch (JedisConnectionException e) {
            logger.error("[{}] - Error connecting to Redis", logId, e);
            return false;
        } catch (Exception e) {
            logger.error("[{}] - An occur error when check exist requestId on redis", logId, e);
            return false;
        } finally {
            if (jedis != null) {
                redisConfig.returnConnection(jedis);
            }
        }
    }

    private void pushKeyToRedis(FeeCommandRequest feeCommand) {
        String logId = logIdThreadLocal.get();
        boolean resultPushKeyToRedis = this.pushRedis(feeCommand.getRequestId());
        if (!resultPushKeyToRedis) {
            logger.info("[{}] - Push requestId to redis fail ", logId);
        }
    }

    private boolean pushRedis(String requestId) {
        String logId = logIdThreadLocal.get();
        RedisConfig redisConfig = RedisConfig.getInstance();
        Jedis jedis = null;
        try {
            jedis = redisConfig.getJedisPool().getResource();
            String resultPushMessage = "";
            if (jedis != null) {
                long timeToLife = CommonUtil.processTimeToLife();
                resultPushMessage = jedis.setex(requestId, timeToLife, "");
            }
            boolean isPushMessageSuccessfully = "OK".equalsIgnoreCase(resultPushMessage);
            if (isPushMessageSuccessfully) {
                logger.info("[{}] - Push message with requestId : {} to Redis successfully !", logId,
                        requestId);
            }
            return isPushMessageSuccessfully;
        } catch (JedisConnectionException e) {
            logger.error("[{}] - Error connecting to Redis", logId, e);
            return false;
        } catch (Exception e) {
            logger.error("[{}] - Error push message to redis", logId, e);
            return false;
        } finally {
            if (jedis != null) {
                redisConfig.returnConnection(jedis);
            }
        }
    }

    private void sendResponse(HttpExchange httpExchange, String responseMessage, String data, int statusCode, String statusMessage) {
        String logId = logIdThreadLocal.get();
        try {
            logger.info("[{}] - Start send response to client ", logId);
            GeneralResponse<String> generalResponse = new GeneralResponse<>();
            httpExchange.getResponseHeaders().set("Content-Type", "application/json");
            logger.info("[{}] - Send response to client with Content-Type : application/json", logId);
            generalResponse.setData(data);
            generalResponse.setMessage(responseMessage);
            generalResponse.setCode(statusMessage);
            String responseString = CommonUtil.objectToJson(generalResponse);
            httpExchange.sendResponseHeaders(statusCode, responseString.length());
            OutputStream os = httpExchange.getResponseBody();
            os.write(responseString.getBytes());
            os.close();
            logger.info("[{}] - Send response to client successfully with response: {} ", logId, responseString);
        } catch (IOException e) {
            logger.error("[{}] - Occur error when send response to client", logId, e);
        }
    }
}
