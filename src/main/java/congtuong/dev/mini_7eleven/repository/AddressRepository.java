package congtuong.dev.mini_7eleven.repository;

import congtuong.dev.mini_7eleven.pojo.Address;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    Page<Address> findByAccountId(Long accountId, Pageable pageable);

    Optional<Address> findByIdAndAccountId(Long id, Long accountId);

    boolean existsByAccountIdAndIsDefaultTrue(Long accountId);

    @Modifying
    @Query("""
            update Address a
               set a.isDefault = false
             where a.account.id = :accountId
               and a.id <> :addressId
            """)
    int clearDefaultForAccount(@Param("accountId") Long accountId, @Param("addressId") Long addressId);
}

