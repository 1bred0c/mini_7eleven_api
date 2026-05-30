package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.AddressRequest;
import congtuong.dev.mini_7eleven.dto.AddressResponse;
import congtuong.dev.mini_7eleven.exception.NotFoundException;
import congtuong.dev.mini_7eleven.pojo.Account;
import congtuong.dev.mini_7eleven.pojo.Address;
import congtuong.dev.mini_7eleven.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addressRepository;
    private final AccountService accountService;

    @Override
    @Transactional
    public AddressResponse create(String email, AddressRequest request) {
        Account account = accountService.getByEmail(email);
        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault());

        Address address = Address.builder()
                .account(account)
                .receiverName(request.getReceiverName().trim())
                .phoneNumber(request.getPhoneNumber().trim())
                .addressLine(request.getAddressLine().trim())
                .ward(normalize(request.getWard()))
                .district(normalize(request.getDistrict()))
                .city(normalize(request.getCity()))
                .isDefault(makeDefault)
                .build();

        Address saved = addressRepository.save(address);
        if (makeDefault) {
            addressRepository.clearDefaultForAccount(account.getId(), saved.getId());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse update(String email, Long id, AddressRequest request) {
        Address address = getOwnedAddress(email, id);

        address.setReceiverName(request.getReceiverName().trim());
        address.setPhoneNumber(request.getPhoneNumber().trim());
        address.setAddressLine(request.getAddressLine().trim());
        address.setWard(normalize(request.getWard()));
        address.setDistrict(normalize(request.getDistrict()));
        address.setCity(normalize(request.getCity()));

        boolean makeDefault = Boolean.TRUE.equals(request.getIsDefault());
        address.setIsDefault(makeDefault);

        Address saved = addressRepository.save(address);
        if (makeDefault) {
            addressRepository.clearDefaultForAccount(address.getAccount().getId(), saved.getId());
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public AddressResponse setDefault(String email, Long id) {
        Address address = getOwnedAddress(email, id);
        address.setIsDefault(true);
        Address saved = addressRepository.save(address);
        addressRepository.clearDefaultForAccount(address.getAccount().getId(), saved.getId());
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String email, Long id) {
        Address address = getOwnedAddress(email, id);
        addressRepository.delete(address);
    }

    @Override
    @Transactional(readOnly = true)
    public AddressResponse getById(String email, Long id) {
        return toResponse(getOwnedAddress(email, id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AddressResponse> getMyAddresses(String email, Pageable pageable) {
        Account account = accountService.getByEmail(email);
        return addressRepository.findByAccountId(account.getId(), pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Address getOwnedAddress(String email, Long id) {
        Account account = accountService.getByEmail(email);
        return addressRepository.findByIdAndAccountId(id, account.getId())
                .orElseThrow(() -> new NotFoundException("ADDRESS_NOT_FOUND", "Address not found"));
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .accountId(address.getAccount().getId())
                .receiverName(address.getReceiverName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine(address.getAddressLine())
                .ward(address.getWard())
                .district(address.getDistrict())
                .city(address.getCity())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .updatedAt(address.getUpdatedAt())
                .build();
    }
}

