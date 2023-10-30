package vn.vnpay.demo2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.demo2.config.database.DataSourceConfig;
import vn.vnpay.demo2.config.redis.RedisConfig;
import vn.vnpay.demo2.controller.FeeController;
import vn.vnpay.demo2.job.ScheduleScanTime;
import vn.vnpay.demo2.service.impl.TransactionServiceImpl;

import java.util.Arrays;
import java.util.List;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String[] args) {
        try {
            DataSourceConfig.initDatabaseConnectionPool();
            RedisConfig.initRedisConfig();
            TransactionServiceImpl transactionService = new TransactionServiceImpl();
            ScheduleScanTime scheduleScanTime = new ScheduleScanTime();
            scheduleScanTime.cronJob(transactionService);
            FeeController feeController = new FeeController();
            List<String> listPath = Arrays.asList("/init", "/update");
            feeController.start(listPath);
//            transactionService.updateStatus("bdf055ba-74a0-4d2e-81f7-6c20a3c0b2c9");
        } catch (Exception e) {
            logger.error("Failed to initialize",e);
            System.exit(3);
        }
    }
}