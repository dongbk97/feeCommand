package vn.vnpay.demo2.service.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.demo2.bean.FeeCommand;
import vn.vnpay.demo2.bean.FeeTransaction;
import vn.vnpay.demo2.common.CommonUtil;
import vn.vnpay.demo2.common.CreateUser;
import vn.vnpay.demo2.common.FeeStatus;
import vn.vnpay.demo2.config.database.DataSourceConfig;
import vn.vnpay.demo2.service.TransactionService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static vn.vnpay.demo2.handle.RequestHandler.logIdThreadLocal;

public class TransactionServiceImpl implements TransactionService {
    private final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    public void initTransaction(FeeCommand feeCommand) {
        String logId = logIdThreadLocal.get();
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
        } catch (Exception ex) {
            logger.error("[{}] - Occur error while initializing transaction", logId, ex);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

    private void setFeeCommand(FeeCommand feeCommand) {
        feeCommand.setCommandCode(CommonUtil.getNextId());
        feeCommand.setCreatedUser(CreateUser.ADMIN);
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
    public void updateStatus(String commandCode) {
        String logId = logIdThreadLocal.get();
        logger.info("[{}] - Start update status for fee transaction with commandCode: {}", logId, commandCode);
        List<FeeTransaction> feeTransactions;
        DataSourceConfig connectionPool = DataSourceConfig.getInstance();
        SessionFactory sessionFactory = connectionPool.getSessionFactory();
        Transaction transaction = null;
        boolean hasRecords = false;
        try (Session session = sessionFactory.openSession()) {
            do {
                feeTransactions = this.getListFeeTransaction(session, commandCode);
                hasRecords = feeTransactions != null && !feeTransactions.isEmpty();
                if (hasRecords) {
                    transaction = session.beginTransaction();
                    logger.info("[{}] - Begin transaction to update fee transaction ", logId);
                    this.updateFeeTransaction(session, transaction, feeTransactions);
                }
            } while (hasRecords);
            logger.info("[{}] - Update fee transaction with commandCode: {} successfully", logId, commandCode);
        } catch (Exception ex) {
            logger.error("[{}] - Occur error while update fee transaction ", logId, ex);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
        }
    }


    public List<FeeTransaction> getListFeeTransaction(Session session, String commandCode) {
        String logId = logIdThreadLocal.get();
        String hql = "FROM FeeTransaction ft WHERE ft.status = :status and ft.totalScan = 0 and ft.commandCode = :commandCode ";
        Query query = session.createQuery(hql);
        query.setParameter("status", FeeStatus.CREATE);
        query.setParameter("commandCode", commandCode);
        query.setFirstResult(0);
        query.setMaxResults(20);
        List<FeeTransaction> feeTransactionList = query.list();
        logger.info("[{}] - Getting list fee transaction with commandCode: {} successfully with size: {}", logId,
                commandCode, feeTransactionList.size());
        return feeTransactionList;
    }

    private void updateFeeTransaction(Session session, Transaction transaction, List<FeeTransaction> feeTransactionList) {
        String logId = logIdThreadLocal.get();
        feeTransactionList.forEach(feeTransaction -> {
            feeTransaction.setTotalScan(1);
            feeTransaction.setModifiedDate(LocalDateTime.now());
            feeTransaction.setStatus(FeeStatus.FEE_CHARGING);
            session.save(feeTransaction);
        });
        transaction.commit();
        logger.info("[{}] - Commit transaction to update fee transaction successfully", logId);
    }

    public void scanFee() {
        DataSourceConfig connectionPool = DataSourceConfig.getInstance();
        SessionFactory sessionFactory = connectionPool.getSessionFactory();
        String hql = "FROM FeeTransaction ft WHERE ft.status = :status and ft.totalScan < 5";
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            Query query = session.createQuery(hql);
            query.setParameter("status", FeeStatus.FEE_CHARGING);
            List<FeeTransaction> feeTransactionList = query.list();
            if (feeTransactionList != null && feeTransactionList.size() > 0) {
                transaction = session.beginTransaction();
                feeTransactionList.forEach(feeTransaction -> {
                    if (feeTransaction.getTotalScan() == 4) {
                        feeTransaction.setStatus(FeeStatus.FEE_STOP);
                    }
                    feeTransaction.setTotalScan(feeTransaction.getTotalScan() + 1);
                    feeTransaction.setModifiedDate(LocalDateTime.now());
                    session.save(feeTransaction);

                });
                transaction.commit();
            }

        } catch (Exception ex) {
            logger.error("Occur error while update fee transaction ", ex);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

}
