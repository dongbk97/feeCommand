package vn.vnpay.demo2.service.impl;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.demo2.bean.FeeCommand;
import vn.vnpay.demo2.bean.FeeTransaction;
import vn.vnpay.demo2.common.CreateUser;
import vn.vnpay.demo2.common.FeeStatus;
import vn.vnpay.demo2.config.database.DataSourceConfig;
import vn.vnpay.demo2.service.TransactionService;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TransactionServiceImpl implements TransactionService {
    private final Logger logger = LoggerFactory.getLogger(TransactionServiceImpl.class);

    public void initTransaction(FeeCommand feeCommand) {
        DataSourceConfig connectionPool = DataSourceConfig.getInstance();
        SessionFactory sessionFactory = connectionPool.getSessionFactory();
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            this.setFeeCommand(feeCommand);
            List<FeeTransaction> feeTransactionList = this.createFeeTransactions(feeCommand);
            transaction = session.beginTransaction();
            session.save(feeCommand);
            feeTransactionList.forEach(session::save);
            transaction.commit();

        } catch (Exception ex) {
            logger.error("Occur error while initializing transaction", ex);
            if (transaction != null && transaction.isActive()) {
                transaction.rollback();
            }
        }
    }

    private void setFeeCommand(FeeCommand feeCommand) {
        feeCommand.setCommandCode(UUID.randomUUID().toString().replace("-", ""));
        feeCommand.setCreatedUser(CreateUser.ADMIN);
        feeCommand.setCreatedDate(LocalDateTime.now());
    }

    private List<FeeTransaction> createFeeTransactions(FeeCommand feeCommand) {
        int totalRecord = feeCommand.getTotalRecord();
        List<FeeTransaction> feeTransactionList = new ArrayList<>(totalRecord);
        for (int i = 0; i < totalRecord; i++) {
            FeeTransaction feeTransaction = new FeeTransaction();
            feeTransaction.setCommandCode(feeCommand.getCommandCode());
            feeTransaction.setTransactionCode(UUID.randomUUID().toString());
            feeTransaction.setStatus(FeeStatus.CREATE);
            feeTransaction.setCreatedDate(LocalDateTime.now());
            feeTransactionList.add(feeTransaction);
        }
        return feeTransactionList;
    }

    public void fetchAllFeeTransactions(String commandCode) {
        List<FeeTransaction> feeTransactions;
        DataSourceConfig connectionPool = DataSourceConfig.getInstance();
        SessionFactory sessionFactory = connectionPool.getSessionFactory();
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            do {
                feeTransactions = fetchFeeTransactionsByPage(session, 0, 5,commandCode);
                if (feeTransactions != null && !feeTransactions.isEmpty()) {
                    transaction = session.beginTransaction();
                    feeTransactions.forEach(feeTransaction -> {
                        feeTransaction.setTotalScan(1);
                        feeTransaction.setModifiedDate(LocalDateTime.now());
                        feeTransaction.setStatus(FeeStatus.FEE_CHARGING);
                        session.save(feeTransaction);
                    });
                    transaction.commit();
                }

            } while (feeTransactions != null && !feeTransactions.isEmpty());
        }
    }



    public List<FeeTransaction> fetchFeeTransactionsByPage(Session session, int pageNumber, int pageSize,String commandCode) {
        try  {
            CriteriaBuilder builder = session.getCriteriaBuilder();
            CriteriaQuery<FeeTransaction> criteria = builder.createQuery(FeeTransaction.class);
            Root<FeeTransaction> root = criteria.from(FeeTransaction.class);
            criteria.select(root);

            Predicate commandCodePredicate = builder.equal(root.get("commandCode"), commandCode);
            Predicate statusPredicate = builder.equal(root.get("status"), FeeStatus.CREATE);
            Predicate andPredicate = builder.and(commandCodePredicate, statusPredicate);
            criteria.where(andPredicate);

            Query<FeeTransaction> query = session.createQuery(criteria);
            query.setFirstResult((pageNumber - 1) * pageSize);
            query.setMaxResults(pageSize);

            return query.getResultList();
        } catch (Exception ex) {
            logger.error("Occur error while fetching fee transactions ", ex);
            return null;
        }
    }


    public void updateStatus(String commandCode) {
        DataSourceConfig connectionPool = DataSourceConfig.getInstance();
        SessionFactory sessionFactory = connectionPool.getSessionFactory();
        String hql = "FROM FeeTransaction ft WHERE ft.status = :status and ft.totalScan = 0 and ft.commandCode = :commandCode ";
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            Query query = session.createQuery(hql);
            query.setParameter("status", FeeStatus.CREATE);
            query.setParameter("commandCode", commandCode);

            List<FeeTransaction> feeTransactionList = fetchFeeTransactionsByPage(query, 0, 5);

//            List<FeeTransaction> feeTransactionList = query.list();
            if (feeTransactionList != null && !feeTransactionList.isEmpty()) {
                transaction = session.beginTransaction();
                feeTransactionList.forEach(feeTransaction -> {
                    feeTransaction.setTotalScan(1);
                    feeTransaction.setModifiedDate(LocalDateTime.now());
                    feeTransaction.setStatus(FeeStatus.FEE_CHARGING);
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


    public List<FeeTransaction> fetchFeeTransactionsByPage(Query query, int beginResult, int pageSize) {
        query.setFirstResult(beginResult);
        query.setMaxResults(pageSize);
        return query.list();
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
