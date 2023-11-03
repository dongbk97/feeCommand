package vn.vnpay.fee.config.database;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.fee.bean.FeeCommand;
import vn.vnpay.fee.bean.FeeTransaction;

public class DataSourceConfig {

    private static volatile DataSourceConfig instance;
    private final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);
    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    public static void initDatabaseConnectionPool() {
        if (instance == null) {
            synchronized (DataSourceConfig.class) {
                if (instance == null) {
                    DataSourceConfig connectionPoolTemp = new DataSourceConfig();
                    connectionPoolTemp.registerSession();
                    instance = connectionPoolTemp;
                }
            }
        }
    }

    private static StandardServiceRegistryBuilder getStandardServiceRegistryBuilder() {
        StandardServiceRegistryBuilder registryBuilder = new StandardServiceRegistryBuilder();
        registryBuilder.configure();
        return registryBuilder;
    }

    public static DataSourceConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseConnectionPool not initialized. Call init() before getInstance()");
        }
        return instance;
    }

    private void registerSession() {
        if (sessionFactory == null) {
            try {
                StandardServiceRegistryBuilder registryBuilder = getStandardServiceRegistryBuilder();
                registry = registryBuilder.build();
                logger.info("Hibernate Registry builder created.");

                MetadataSources sources = new MetadataSources(registry);
                sources.addAnnotatedClass(FeeCommand.class);
                sources.addAnnotatedClass(FeeTransaction.class);
                Metadata metadata = sources.getMetadataBuilder().build();
                sessionFactory = metadata.getSessionFactoryBuilder().build();

            } catch (Exception ex) {
                logger.error("SessionFactory creation failed", ex);
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            }
        }
    }

    public SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}
