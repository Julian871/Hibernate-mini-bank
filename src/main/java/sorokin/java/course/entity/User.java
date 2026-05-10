package sorokin.java.course.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login", nullable = false, unique = true)
    private String login;

    @OneToMany(
            mappedBy = "user",
            fetch = FetchType.EAGER
    )
    private List<Account> accounts = new ArrayList<>();

    public User() {
    }

    public User(String login) {
        this.login = login;
    }

    public Long getId() {
        return id;
    }

    public List<Account> getAccounts() {
        return accounts;
    }

    public void addAccount(Account account) {
        accounts.add(account);
    }
}
