package vn.vnpay.fee.service.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.fee.bean.FeeCommand;
import vn.vnpay.fee.bean.FeeTransaction;
import vn.vnpay.fee.common.CommonUtil;
import vn.vnpay.fee.common.FeeStatus;
import vn.vnpay.fee.common.Performer;
import vn.vnpay.fee.config.database.DataSourceConfig;
import vn.vnpay.fee.service.TransactionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionServiceImpl implements TransactionService {

    private final ThreadLocal<String> logIdThreadLocal = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);
    private static volatile TransactionService instance;

    public static TransactionService getInstance() {
        if (instance == null) {
            synchronized (TransactionServiceImpl.class) {
                if (instance == null) {
                    instance = new TransactionServiceImpl();
                }
            }
        }
        return instance;
    }

    public boolean initFeeCommand(FeeCommand feeCommand, String logId) {
        logIdThreadLocal.set(logId);
        DataSourceConfig connectionPool = DataSourceConfig.getInstance();
        SessionFactory sessionFactory = connectionPool.getSessionFactory();
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            this.setFeeCommand(feeCommand);
            List<FeeTransaction> feeTransactionList = this.createFeeTransactions(feeCommand);
            transaction = session.beginTransaction();
            logger.info("[{}] - Begin transaction to save fee command and fee transaction ", logId);
            session.save(feeCommand);
            logger.info("[{}] - Save fee command successfully with id: {}", logId, feeCommand.getId());
            feeTransactionList.forEach(session::save);
            logger.info("[{}] - Save list fee transaction successfully with size : {}", logId, feeTransactionList.size());
            transaction.commit();
            logger.info("[{}] - Commit transaction to save fee command and fee transaction successfully", logId);
            return true;
        } catch (Exception ex) {
            logger.error("[{}] - Occur error while initializing transaction", logId, ex);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            return false;
        } finally {
            logIdThreadLocal.remove();
        }
    }

    private void setFeeCommand(FeeCommand feeCommand) {
        feeCommand.setCommandCode(CommonUtil.getNextId());
        feeCommand.setCreatedUser(Performer.ADMIN);
        feeCommand.setCreatedDate(LocalDateTime.now());
    }

    private List<FeeTransaction> createFeeTransactions(FeeCommand feeCommand) {
        String logId = logIdThreadLocal.get();
        int totalRecord = feeCommand.getTotalRecord();
        logger.info("[{}] - Begin create list fee transaction with total record : {}", logId, totalRecord);
        List<FeeTransaction> feeTransactionList = new ArrayList<>(totalRecord);
        for (int i = 0; i < totalRecord; i++) {
            FeeTransaction feeTransaction = new FeeTransaction();
            feeTransaction.setCommandCode(feeCommand.getCommandCode());
            feeTransaction.setTransactionCode(CommonUtil.getNextId());
            feeTransaction.setStatus(FeeStatus.CREATE);
            feeTransaction.setCreatedDate(LocalDateTime.now());
            feeTransactionList.add(feeTransaction);
        }
        logger.info("[{}] - Create list fee transaction successfully with size : {}", logId, feeTransactionList.size());
        return feeTransactionList;
    }

    public boolean updateFee(String commandCode, String logId) {
        logIdThreadLocal.set(logId);
        logger.info("[{}] - Start update status for fee transaction with commandCode: {}", logId, commandCode);
        List<FeeTransaction> feeTransactions;
        DataSourceConfig connectionPool = DataSourceConfig.getInstance();
        SessionFactory sessionFactory = connectionPool.getSessionFactory();
        logger.info("[{}] -Getting sessionFactory from pool successfully", logId);
        Transaction transaction = null;
        boolean hasRecords = false;
        try (Session session = sessionFactory.openSession()) {
            do {
                feeTransactions = this.getListFeeTransactionForUpdate(session, commandCode);
                hasRecords = !feeTransactions.isEmpty();
                if (hasRecords) {
                    transaction = session.beginTransaction();
                    logger.info("[{}] - Begin transaction to update fee transaction ", logId);
                    this.updateFeeTransaction(session, feeTransactions);
                    transaction.commit();
                    logger.info("[{}] - Commit transaction to update fee transaction successfully", logId);
                }
            } while (hasRecords);
            logger.info("[{}] - Update fee transaction with commandCode: {} successfully", logId, commandCode);
            return true;
        } catch (Exception ex) {
            logger.error("[{}] - Occur error while update fee transaction ", logId, ex);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
            return false;
        } finally {
            logIdThreadLocal.remove();
        }
    }


    private List<FeeTransaction> getListFeeTransactionForUpdate(Session session, String commandCode) {
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start getting list fee transaction for update with commandCode: {} ", logId,
                commandCode);
        String hql = "FROM FeeTransaction ft WHERE ft.status = :status and ft.totalScan = 0 and ft.commandCode = :commandCode ";
        Query query = session.createQuery(hql);
        query.setParameter("status", FeeStatus.CREATE);
        query.setParameter("commandCode", commandCode);
        query.setFirstResult(0);
        query.setMaxResults(500);
        List<FeeTransaction> feeTransactionList = query.list();
        logger.info("[{}] - Getting list fee transaction for update with commandCode: {} successfully with size: {}", logId,
                commandCode, feeTransactionList.size());
        return feeTransactionList;
    }

    private void updateFeeTransaction(Session session, List<FeeTransaction> feeTransactionList) {
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start update list fee transaction ", logId);
        feeTransactionList.forEach(feeTransaction -> {
            feeTransaction.setTotalScan(1);
            feeTransaction.setModifiedDate(LocalDateTime.now());
            feeTransaction.setStatus(FeeStatus.FEE_CHARGING);
            session.save(feeTransaction);
        });
        logger.info("[{}] - End of update list fee transaction", logId);
    }


    public void scanFee() {
        String logId = CommonUtil.generateLogId();
        logIdThreadLocal.set(logId);
        logger.info("[{}] - Start scan fee transaction ", logId);
        DataSourceConfig connectionPool = DataSourceConfig.getInstance();
        SessionFactory sessionFactory = connectionPool.getSessionFactory();
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            List<FeeTransaction> listFeeTransactionForScan = getListFeeTransactionForScan(session);
            if (!listFeeTransactionForScan.isEmpty()) {
                transaction = session.beginTransaction();
                logger.info("[{}] - Begin transaction to update fee transaction when scan", logId);
                this.processScanFee(session, listFeeTransactionForScan);
                transaction.commit();
                logger.info("[{}] - Commit transaction to update fee transaction when scan successfully", logId);
            }
            logger.info("[{}] - Scan fee transaction successfully ", logId);
        } catch (Exception ex) {
            logger.error("[{}] - Occur error while scan fee transaction ", logId, ex);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

    private List<FeeTransaction> getListFeeTransactionForScan(Session session) {
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start getting list fee transaction for scan fee ", logId);
        String hql = "FROM FeeTransaction ft WHERE ft.status = :status and ft.totalScan < 5";
        Query query = session.createQuery(hql);
        query.setParameter("status", FeeStatus.FEE_CHARGING);
        query.setFirstResult(0);
        query.setMaxResults(500);
        List<FeeTransaction> feeTransactionList = query.list();
        logger.info("[{}] - Getting list fee transaction for scan fee successfully with size: {}", logId,
                feeTransactionList.size());
        return feeTransactionList;
    }

    private void processScanFee(Session session, List<FeeTransaction> feeTransactionList) {
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start update list fee transaction when scan fee", logId);
        feeTransactionList.forEach(feeTransaction -> {
            if (feeTransaction.getTotalScan() == 4) {
                feeTransaction.setStatus(FeeStatus.FEE_STOP);
            }
            feeTransaction.setTotalScan(feeTransaction.getTotalScan() + 1);
            feeTransaction.setModifiedDate(LocalDateTime.now());
            session.save(feeTransaction);
        });
        logger.info("[{}] - End of update list fee transaction when scan fee", logId);
    }

}
