package com.iam.platform.core.repository;

import com.iam.platform.core.entity.ContactChannel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContactChannelRepository extends JpaRepository<ContactChannel, UUID> {

    List<ContactChannel> findByOwnerTypeAndOwnerId(String ownerType, UUID ownerId);
}
