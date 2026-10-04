package com.starmitra.modules.connect.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageIdempotencyRepository extends JpaRepository<MessageIdempotencyEntity, String> {
}
