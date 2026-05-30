package congtuong.dev.mini_7eleven.service;

import congtuong.dev.mini_7eleven.dto.AddressRequest;
import congtuong.dev.mini_7eleven.dto.AddressResponse;
import congtuong.dev.mini_7eleven.pojo.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddressService {

    AddressResponse create(String email, AddressRequest request);

    AddressResponse update(String email, Long id, AddressRequest request);

    AddressResponse setDefault(String email, Long id);

    void delete(String email, Long id);

    AddressResponse getById(String email, Long id);

    Page<AddressResponse> getMyAddresses(String email, Pageable pageable);

    Address getOwnedAddress(String email, Long id);
}

