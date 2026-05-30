package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.AddressRequest;
import congtuong.dev.mini_7eleven.dto.AddressResponse;
import congtuong.dev.mini_7eleven.exception.NotFoundException;
import congtuong.dev.mini_7eleven.pojo.Account;
import congtuong.dev.mini_7eleven.pojo.Address;
import congtuong.dev.mini_7eleven.repository.AddressRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressServiceImplTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private AccountService accountService;

    @InjectMocks
    private AddressServiceImpl addressService;

    @Test
    void shouldCreateAddressSuccessfully() {
        Account account = buildAccount(1L, "user@example.com");
        when(accountService.getByEmail("user@example.com")).thenReturn(account);

        Address saved = buildAddress(10L, account, true);
        saved.setWard("Ward 1");
        when(addressRepository.save(any(Address.class))).thenReturn(saved);

        AddressRequest request = AddressRequest.builder()
                .receiverName("  John Doe ")
                .phoneNumber(" 0123456789 ")
                .addressLine(" 123 Main St ")
                .ward(" Ward 1 ")
                .district(" District 1 ")
                .city(" City ")
                .isDefault(true)
                .build();

        AddressResponse response = addressService.create("user@example.com", request);

        assertEquals(10L, response.getId());
        assertEquals(1L, response.getAccountId());
        assertTrue(response.getIsDefault());
        assertEquals("Ward 1", response.getWard());

        verify(addressRepository).clearDefaultForAccount(1L, 10L);

        ArgumentCaptor<Address> captor = ArgumentCaptor.forClass(Address.class);
        verify(addressRepository).save(captor.capture());
        assertEquals("John Doe", captor.getValue().getReceiverName());
    }

    @Test
    void shouldUpdateAddressSuccessfully() {
        Account account = buildAccount(2L, "user@example.com");
        Address existing = buildAddress(11L, account, false);

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(addressRepository.findByIdAndAccountId(11L, 2L)).thenReturn(Optional.of(existing));
        when(addressRepository.save(existing)).thenReturn(existing);

        AddressResponse response = addressService.update("user@example.com", 11L, AddressRequest.builder()
                .receiverName("Jane")
                .phoneNumber("999")
                .addressLine("New line")
                .ward(null)
                .district("  ")
                .city("City")
                .isDefault(true)
                .build());

        assertEquals(11L, response.getId());
        assertTrue(response.getIsDefault());
        assertNull(response.getDistrict());
        verify(addressRepository).clearDefaultForAccount(2L, 11L);
    }

    @Test
    void shouldSetDefaultAddressSuccessfully() {
        Account account = buildAccount(3L, "user@example.com");
        Address existing = buildAddress(12L, account, false);

        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(addressRepository.findByIdAndAccountId(12L, 3L)).thenReturn(Optional.of(existing));
        when(addressRepository.save(existing)).thenReturn(existing);

        AddressResponse response = addressService.setDefault("user@example.com", 12L);

        assertTrue(response.getIsDefault());
        verify(addressRepository).clearDefaultForAccount(3L, 12L);
    }

    @Test
    void shouldGetCurrentUserAddresses() {
        Account account = buildAccount(4L, "user@example.com");
        when(accountService.getByEmail("user@example.com")).thenReturn(account);

        Address address1 = buildAddress(21L, account, false);
        Address address2 = buildAddress(22L, account, true);

        Page<Address> page = new PageImpl<>(List.of(address1, address2));
        when(addressRepository.findByAccountId(eq(4L), any(PageRequest.class))).thenReturn(page);

        Page<AddressResponse> response = addressService.getMyAddresses("user@example.com", PageRequest.of(0, 10));

        assertEquals(2, response.getTotalElements());
        assertEquals(22L, response.getContent().get(1).getId());
    }

    @Test
    void shouldRejectAccessToAnotherUsersAddress() {
        Account account = buildAccount(5L, "user@example.com");
        when(accountService.getByEmail("user@example.com")).thenReturn(account);
        when(addressRepository.findByIdAndAccountId(99L, 5L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> addressService.getById("user@example.com", 99L));
    }

    private Account buildAccount(Long id, String email) {
        Account account = Account.builder()
                .fullName("User")
                .email(email)
                .passwordHash("hash")
                .role(congtuong.dev.mini_7eleven.enums.Role.USER)
                .build();
        account.setId(id);
        return account;
    }

    private Address buildAddress(Long id, Account account, boolean isDefault) {
        Address address = Address.builder()
                .account(account)
                .receiverName("Receiver")
                .phoneNumber("0123")
                .addressLine("Line")
                .ward("Ward")
                .district("District")
                .city("City")
                .isDefault(isDefault)
                .build();
        address.setId(id);
        return address;
    }
}


