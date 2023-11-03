package vn.vnpay.fee.service;

import vn.vnpay.fee.bean.FeeCommand;
import vn.vnpay.fee.service.impl.TransactionServiceImpl;

public interface TransactionService {

    boolean initFeeCommand(FeeCommand feeCommand, String logId);

    boolean updateFee(String commandCode, String logId);

    void scanFee();

    static TransactionService getInstance() {
        return TransactionServiceImpl.getInstance();
    }
}
