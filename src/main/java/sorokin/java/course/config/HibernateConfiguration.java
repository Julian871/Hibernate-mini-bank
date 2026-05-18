package sorokin.java.course.config;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import sorokin.java.course.entity.Account;
import sorokin.java.course.entity.User;

@Configuration
@PropertySource("classpath:application.properties")
public class HibernateConfiguration {

    @Value("${db.driver}")
    private String driverClass;

    @Value("${db.url}")
    private String url;

    @Value("${db.username}")
    private String username;

    @Value("${db.password}")
    private String password;

    @Value("${db.dialect}")
    private String dialect;

    @Value("${hibernate.hbm2ddl.auto}")
    private String hbm2ddlAuto;

    @Value("${hibernate.show_sql}")
    private String showSql;

    @Value("${hibernate.format_sql}")
    private String formatSql;

    @Value("${hibernate.current_session_context_class}")
    private String currentSessionContext;

    @Bean
    public SessionFactory sessionFactory() {
        org.hibernate.cfg.Configuration configuration = new org.hibernate.cfg.Configuration();

        configuration
                .addAnnotatedClass(User.class)
                .addAnnotatedClass(Account.class)
                .setProperty("hibernate.dialect", dialect)
                .setProperty("hibernate.connection.driver_class", driverClass)
                .setProperty("hibernate.connection.url", url)
                .setProperty("hibernate.connection.username", username)
                .setProperty("hibernate.connection.password", password)
                .setProperty("hibernate.show_sql", showSql)
                .setProperty("hibernate.format_sql", formatSql)
                .setProperty("hibernate.current_session_context_class", currentSessionContext)
                .setProperty("hibernate.hbm2ddl.auto", hbm2ddlAuto);

        ServiceRegistry serviceRegistry = new
                StandardServiceRegistryBuilder()
                .applySettings(configuration.getProperties())
                .build();


        return configuration.buildSessionFactory(serviceRegistry);
    }

}
