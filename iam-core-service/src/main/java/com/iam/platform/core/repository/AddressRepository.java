package com.iam.platform.core.repository;

import com.iam.platform.core.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {

    List<Address> findByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);
}
