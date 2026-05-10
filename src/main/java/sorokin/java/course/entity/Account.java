package sorokin.java.course.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "money_amount", nullable = false)
    private int moneyAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    public Account() {
    }

    public Account(int moneyAmount, User user) {
        this.moneyAmount = moneyAmount;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public int getMoneyAmount() {
        return moneyAmount;
    }

    public User getUser() {
        return user;
    }

    public void setMoneyAmount(int moneyAmount) {
        if(moneyAmount < 0) throw new IllegalArgumentException("Attempted to set moneyAmount less than 0");
        this.moneyAmount = moneyAmount;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
