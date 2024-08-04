package com.BM.MoneyTransfer.entity;

import com.BM.MoneyTransfer.dto.enums.Gender;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user")
@Data
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "email")
    @Email(message = "Email is not valid")
    @NotNull(message = "Email cannot be null")
    private String email;

    @Size(min = 6, max = 255, message = "Username must be between 6 and 255 characters")
    @NotNull(message = "Username cannot be null")
    @Column(name = "username")
    private String userName;

    @NotNull(message = "Password cannot be null")
    @Size(min = 6, max = 60, message = "Password must be at least 6 characters and at most 60 characters long")
    @Column(name = "password")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{6,}$", message = "Password must contain at least one letter, one number, and one special character")
    private String password;

    @Column(name = "gender")
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @NotNull(message = "Date of birth cannot be null")
    @Past(message = "Date of birth must be in the past")
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @NotNull(message = "Country cannot be null")
    @Size(min = 1, max = 50, message = "Country must be between 1 and 50 characters")
    @Column(name = "country")
    private String country;

    @NotNull(message = "Active status cannot be null")
    @Min(value = 0, message = "Active status must be 0 or 1")
    @Max(value = 1, message = "Active status must be 0 or 1")
    @Column(name = "is_active")
    private Integer isActive = 1;

    @ToString.Exclude
    @ManyToMany(cascade = {CascadeType.DETACH, CascadeType.MERGE, CascadeType.PERSIST, CascadeType.REFRESH})
    @JoinTable(
            name = "favourite_recipients",
            joinColumns = @JoinColumn(name = "user_email1"),
            inverseJoinColumns = @JoinColumn(name = "user_email2")
    )
    @JsonIgnore
    List<User> favouriteRecipients = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "user")
    @JsonIgnore
    List<Card> cards = new ArrayList<>();

    @ToString.Exclude
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_email")
    @JsonIgnore
    List<Authority> authorities = new ArrayList<>();

    public User(String email, String userName, String password, Gender gender, LocalDate dateOfBirth, String country) {
        this.email = email;
        this.userName = userName;
        this.password = password;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.country = country;
    }

    public void addFavouriteRecipient(User user) {
        favouriteRecipients.add(user);
    }

    public void addCard(Card card) {
        cards.add(card);
        card.setUser(this);
    }

    public void setCards(List<Card> cards) {
        for (Card card : cards) {
            card.setUser(this);
        }
        this.cards = cards;
    }

    public void addAuthority(Authority authority) {
        this.authorities.add(authority);
    }

}
