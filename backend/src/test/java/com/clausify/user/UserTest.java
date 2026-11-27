package com.clausify.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Entity behavior that needs no database: identity equality and the address helpers. */
class UserTest {

    private final User user = User.builder().name("Maria").email("maria@example.com").password("hash").build();

    @Test
    void addAddressLinksBothSides() {
        Address home = address();

        user.addAddress(home);

        assertThat(user.getAddresses()).containsExactly(home);
        assertThat(home.getUser()).isSameAs(user);
    }

    @Test
    void addingTheSameInstanceTwiceKeepsOneCopy() {
        Address home = address();

        user.addAddress(home);
        user.addAddress(home);

        assertThat(user.getAddresses()).hasSize(1);
    }

    @Test
    void twoAddressesWithTheSameTextAreBothKept() {
        user.addAddress(address());
        user.addAddress(address());

        assertThat(user.getAddresses()).hasSize(2);
    }

    @Test
    void addAddressRejectsNull() {
        assertThatThrownBy(() -> user.addAddress(null)).isInstanceOf(IllegalArgumentException.class);
        assertThat(user.getAddresses()).isEmpty();
    }

    @Test
    void removeAddressUnlinksExactlyThatInstance() {
        Address kept = address();
        Address removed = address();
        user.addAddress(kept);
        user.addAddress(removed);

        user.removeAddress(removed);

        assertThat(user.getAddresses()).singleElement().isSameAs(kept);
        assertThat(kept.getUser()).isSameAs(user);
        assertThat(removed.getUser()).isNull();
    }

    @Test
    void removingAnAddressThatWasNeverAddedChangesNothing() {
        Address home = address();
        user.addAddress(home);

        user.removeAddress(address());
        user.removeAddress(null);

        assertThat(user.getAddresses()).singleElement().isSameAs(home);
        assertThat(home.getUser()).isSameAs(user);
    }

    @Test
    void usersCompareByIdentityEvenWithTheSameId() {
        User a = User.builder().id(1L).name("Maria").email("maria@example.com").password("hash").build();
        User b = User.builder().id(1L).name("Maria").email("maria@example.com").password("hash").build();

        assertThat(a).isNotEqualTo(b).isEqualTo(a);
        assertThat(new User()).isNotEqualTo(new User());
    }

    @Test
    void toStringLeavesOutPersonalDataAndSecrets() {
        assertThat(user.toString()).doesNotContain("maria@example.com", "hash");
    }

    private static Address address() {
        return Address.builder().street("1 Main St").city("Cincinnati").state("OH").zip("45221").build();
    }
}
