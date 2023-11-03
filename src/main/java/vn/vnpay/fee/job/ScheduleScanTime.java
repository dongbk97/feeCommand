package vn.vnpay.fee.job;

import vn.vnpay.fee.service.TransactionService;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ScheduleScanTime {

    public void cronJob(TransactionService transactionService) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        Runnable task = transactionService::scanFee;
        scheduler.scheduleAtFixedRate(task, 180, 180, TimeUnit.SECONDS);
    }
}
