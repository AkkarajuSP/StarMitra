package com.starmitra.modules.judge.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.UUID;

public interface JudgeExpertiseRepository extends JpaRepository<JudgeExpertiseEntity, UUID> {

    @Modifying
    @Query("delete from JudgeExpertiseEntity e where e.judgeId = :judgeId")
    void deleteByJudgeId(UUID judgeId);
}
