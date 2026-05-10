package sorokin.java.course.user;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.stereotype.Component;
import sorokin.java.course.account.AccountProperties;
import sorokin.java.course.entity.Account;
import sorokin.java.course.entity.User;

import java.util.*;
import java.util.function.Supplier;

@Component
public class UserService {

    private final SessionFactory sessionFactory;
    private final AccountProperties accountProperties;

    public UserService(SessionFactory sessionFactory, AccountProperties accountProperties) {
        this.sessionFactory = sessionFactory;
        this.accountProperties = accountProperties;
    }

    public User createUser(String login) {
        return executeInTransactionOrJoin(() -> {
            String normalizedLogin = validateLogin(login);

            String hql = "SELECT count(u) FROM User u WHERE u.login = :login";
            Long count = sessionFactory.getCurrentSession()
                    .createQuery(hql, Long.class)
                    .setParameter("login", normalizedLogin)
                    .getSingleResult();

            if (count > 0) {
                throw new IllegalArgumentException("User already exists with login=%s".formatted(normalizedLogin));
            }

            User user = new User(normalizedLogin);
            sessionFactory.getCurrentSession().persist(user);

            Account account = new Account(accountProperties.getDefaultAmount(), user);
            sessionFactory.getCurrentSession().persist(account);

            user.addAccount(account);

            return user;
        });
    }

    public User findUserById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("user id must be > 0");
        }

        User user = executeInTransactionOrJoin(() -> sessionFactory.getCurrentSession()
                .get(User.class, id));

        if (user == null) {
            throw new IllegalArgumentException("No such user with id=%s".formatted(id));
        }
        return user;
    }

    public List<User> findAll() {
        return executeInTransactionOrJoin(() -> sessionFactory.getCurrentSession()
                .createQuery("SELECT u FROM User u", User.class)
                .list());
    }

    private String validateLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new IllegalArgumentException("login must not be blank");
        }
        return login.trim();
    }

    private <T> T executeInTransactionOrJoin(Supplier<T> action) {
        Session session = sessionFactory.getCurrentSession();
        Transaction tx = session.getTransaction();
        boolean owner = tx.getStatus() == TransactionStatus.NOT_ACTIVE;
        if (owner) {
            tx = session.beginTransaction();
        }
        try {
            T result = action.get();
            if (owner) {
                tx.commit();
            }
            return result;
        } catch (RuntimeException e) {
            if (owner) {
                tx.rollback();
            }
            throw e;
        } finally {
            if (owner) {
                session.close();
            }
        }
    }

}