package vn.vnpay.fee.service;

import vn.vnpay.fee.bean.FeeCommand;
import vn.vnpay.fee.service.impl.TransactionServiceImpl;

public interface TransactionService {

    void initFee(FeeCommand feeCommand);

    void updateFee(String commandCode);

    void scanFee();

    static TransactionService getInstance() {
        return TransactionServiceImpl.getInstance();
    }
}
