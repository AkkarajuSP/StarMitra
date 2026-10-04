package com.starmitra.modules.connect.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlockEntity, UserBlockEntity.Pk> {

    /** Block exists in either direction between two users. */
    @Query("select count(b) > 0 from UserBlockEntity b " +
           "where (b.blockerId = :a and b.blockedId = :b) or (b.blockerId = :b and b.blockedId = :a)")
    boolean existsBetween(@Param("a") UUID a, @Param("b") UUID b);
}
