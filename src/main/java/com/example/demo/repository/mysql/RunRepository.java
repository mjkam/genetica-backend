package com.example.demo.repository.mysql;

import com.example.demo.domain.mysql.Run;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface RunRepository extends JpaRepository<Run, Long> {
    @Query("SELECT r FROM Run r WHERE r.job.id = :jobId AND r.stepId = :stepId")
    Run findRun(Long jobId, String stepId);
}
