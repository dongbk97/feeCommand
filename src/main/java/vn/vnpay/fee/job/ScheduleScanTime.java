package vn.vnpay.fee.job;

import vn.vnpay.fee.service.impl.TransactionServiceImpl;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduleScanTime {
    public void cronJob(TransactionServiceImpl transactionService) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = transactionService::scanFee;
        scheduler.scheduleAtFixedRate(task, 0, 180, TimeUnit.SECONDS);
    }
}
