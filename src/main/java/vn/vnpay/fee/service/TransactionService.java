package vn.vnpay.fee.service;

import vn.vnpay.fee.bean.FeeCommand;

public interface TransactionService {
    void initFee(FeeCommand feeCommand);

    void updateFee(String commandCode);

    void scanFee();
}
