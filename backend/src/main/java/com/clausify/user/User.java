package com.clausify.user;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Generated;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * No Lombok equals/hashCode: entities compare by identity, which stays correct while the generated id changes.
 * The {@code @EqualsAndHashCode.Exclude} markers stay so the relationships are safe if equality is ever added.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    @ToString.Exclude // personal data; keep it out of logs and exception messages
    private String email;

    @Column(nullable = false)
    @ToString.Exclude
    private String password;

    // Set by the database default (V4); Hibernate reads it back after insert.
    @Generated
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Address> addresses = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Profile profile;

    /** Links an address to this user. Adding the same instance twice keeps one copy. */
    public void addAddress(Address address) {
        if (address == null) {
            throw new IllegalArgumentException("Address cannot be null.");
        }
        // By instance, not equals(): Address is @Data, so two different addresses with the same text are equal.
        if (addresses.stream().noneMatch(a -> a == address)) {
            addresses.add(address);
            address.setUser(this);
        }
    }

    /** Unlinks exactly this address instance; orphanRemoval deletes its row on flush. */
    public void removeAddress(Address address) {
        if (address != null && addresses.removeIf(a -> a == address)) {
            address.setUser(null);
        }
    }
}
