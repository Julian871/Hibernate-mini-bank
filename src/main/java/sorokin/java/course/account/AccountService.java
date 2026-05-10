package sorokin.java.course.account;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.stereotype.Component;
import sorokin.java.course.entity.Account;
import sorokin.java.course.entity.User;

import java.util.Objects;
import java.util.function.Supplier;

@Component
public class AccountService {

    private final AccountProperties accountProperties;
    private final SessionFactory sessionFactory;

    public AccountService(AccountProperties accountProperties, SessionFactory sessionFactory) {
        this.accountProperties = accountProperties;
        this.sessionFactory = sessionFactory;
    }

    public Account createAccount(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user must not be null");
        }

        return executeInTransactionOrJoin(() -> {
            Account account = new Account(accountProperties.getDefaultAmount(), user);
            sessionFactory.getCurrentSession().persist(account);
            user.addAccount(account);
            sessionFactory.getCurrentSession().merge(user);
            return account;
        });
    }

    public void withdraw(Long fromAccountId, Integer amount) {
        validatePositiveId(fromAccountId, "account id");
        validatePositiveAmount(amount);

        executeInTransactionOrJoin(() -> {
            Account account = sessionFactory.getCurrentSession().get(Account.class, fromAccountId);
            if(account == null) throw new IllegalArgumentException("No such account: id=%s".formatted(fromAccountId));

            if (amount > account.getMoneyAmount()) {
                throw new IllegalArgumentException(
                        "insufficient funds on account id=%s, moneyAmount=%s, attempted withdraw=%s"
                                .formatted(account.getId(), account.getMoneyAmount(), amount)
                );
            }

            account.setMoneyAmount(account.getMoneyAmount() - amount);
            sessionFactory.getCurrentSession().merge(account);
            return null;
        });
    }

    public void deposit(Long toAccountId, Integer amount) {
        validatePositiveId(toAccountId, "account id");
        validatePositiveAmount(amount);

        executeInTransactionOrJoin(() -> {
            Account account = sessionFactory.getCurrentSession().get(Account.class, toAccountId);
            if(account == null) throw new IllegalArgumentException("No such account: id=%s".formatted(toAccountId));

            account.setMoneyAmount(account.getMoneyAmount() + amount);
            sessionFactory.getCurrentSession().merge(account);
            return null;
        });
    }

    public void closeAccount(Long accountId) {
        validatePositiveId(accountId, "account id");

        executeInTransactionOrJoin(() -> {
            String hql = "SELECT a FROM Account a LEFT JOIN FETCH a.user WHERE a.id = :id";

            Account account = sessionFactory.getCurrentSession()
                    .createQuery(hql, Account.class)
                    .setParameter("id", accountId)
                    .getSingleResult();

            if (account == null) throw new IllegalArgumentException("No such account: id=%s".formatted(accountId));

            if (account.getUser().getAccounts().size() == 1) {
                throw new IllegalStateException("Can't close the only one account");
            }

            Account accountToTransferMoney = account.getUser().getAccounts().stream()
                    .filter(a -> !a.getId().equals(accountId))
                    .findFirst()
                    .orElseThrow();

            accountToTransferMoney.setMoneyAmount(accountToTransferMoney.getMoneyAmount() + account.getMoneyAmount());

            sessionFactory.getCurrentSession().merge(accountToTransferMoney);
            sessionFactory.getCurrentSession().remove(account);

            account.getUser().getAccounts().remove(account);
            sessionFactory.getCurrentSession().merge(account.getUser());
            return null;
        });
    }

    public void transfer(Long fromAccountId, Long toAccountId, int amount) {
        validatePositiveId(fromAccountId, "source account id");
        validatePositiveId(toAccountId, "target account id");
        if (fromAccountId.equals(toAccountId)) {
            throw new IllegalArgumentException("source and target account id must be different");
        }

        executeInTransactionOrJoin(() -> {
            String hql = "SELECT a FROM Account a LEFT JOIN FETCH a.user WHERE a.id = :id";
            Account accountFrom = sessionFactory.getCurrentSession()
                    .createQuery(hql, Account.class)
                    .setParameter("id", fromAccountId)
                    .getSingleResult();

            if(accountFrom == null) throw new IllegalArgumentException("No such account: id=%s".formatted(fromAccountId));
            if (amount > accountFrom.getMoneyAmount()) {
                throw new IllegalArgumentException(
                        "insufficient funds on account id=%s, moneyAmount=%s, attempted transfer=%s"
                                .formatted(accountFrom.getId(), accountFrom.getMoneyAmount(), amount)
                );
            }

            Account accountTo = sessionFactory.getCurrentSession()
                    .createQuery(hql, Account.class)
                    .setParameter("id", toAccountId)
                    .getSingleResult();
            if(accountTo == null) throw new IllegalArgumentException("No such account: id=%s".formatted(toAccountId));

            accountFrom.setMoneyAmount(accountFrom.getMoneyAmount() - amount);
            sessionFactory.getCurrentSession().merge(accountFrom);

            int amountToTransfer = Objects.equals(accountTo.getUser().getId(), accountFrom.getUser().getId())
                    ? amount
                    : (int) Math.round(amount * (1 - accountProperties.getTransferCommission()));
            accountTo.setMoneyAmount(accountTo.getMoneyAmount() + amountToTransfer);
            sessionFactory.getCurrentSession().merge(accountTo);
            return null;
        });
    }

    private void validatePositiveId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(fieldName + " must be > 0");
        }
    }

    private void validatePositiveAmount(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
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
