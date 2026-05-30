package congtuong.dev.mini_7eleven.controller;

import congtuong.dev.mini_7eleven.dto.AddressRequest;
import congtuong.dev.mini_7eleven.dto.AddressResponse;
import congtuong.dev.mini_7eleven.service.AddressService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class AddressController {

    private final AddressService addressService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public AddressResponse create(@AuthenticationPrincipal UserDetails principal,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.create(principal.getUsername(), request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public AddressResponse update(@AuthenticationPrincipal UserDetails principal,
                                  @PathVariable Long id,
                                  @Valid @RequestBody AddressRequest request) {
        return addressService.update(principal.getUsername(), id, request);
    }

    @PatchMapping("/{id}/default")
    @PreAuthorize("hasRole('USER')")
    public AddressResponse setDefault(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return addressService.setDefault(principal.getUsername(), id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public void delete(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        addressService.delete(principal.getUsername(), id);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER')")
    public AddressResponse getById(@AuthenticationPrincipal UserDetails principal, @PathVariable Long id) {
        return addressService.getById(principal.getUsername(), id);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public Page<AddressResponse> getMyAddresses(@AuthenticationPrincipal UserDetails principal,
                                                @PageableDefault(size = 20) Pageable pageable) {
        return addressService.getMyAddresses(principal.getUsername(), pageable);
    }
}

