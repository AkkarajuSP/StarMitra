package com.starmitra.modules.room.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface CreativeRoomRepository extends JpaRepository<CreativeRoomEntity, UUID> {

    /** Rooms visible to caller: PUBLIC, or rooms where they're a member/owner. */
    @Query("select distinct r from CreativeRoomEntity r " +
           "where r.status = 'OPEN' and (r.visibility = 'PUBLIC' or r.ownerId = :caller or " +
           "r.id in (select m.roomId from ProjectMemberEntity m " +
           "         where m.userId = :caller and m.status = 'ACTIVE')) " +
           "and (:skillId is null or r.id in (select s.roomId from RequiredSkillEntity s " +
           "       where s.skillId = :skillId)) " +
           "order by r.createdAt desc, r.id asc")
    List<CreativeRoomEntity> findVisible(@Param("caller") UUID caller,
                                         @Param("skillId") UUID skillId,
                                         Pageable page);
}
