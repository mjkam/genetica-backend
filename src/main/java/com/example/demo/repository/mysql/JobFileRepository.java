package com.example.demo.repository.mysql;

import com.example.demo.domain.mysql.JobFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JobFileRepository extends JpaRepository<JobFile, Long> {
    @Query("SELECT jf FROM JobFile jf JOIN FETCH jf.file f WHERE jf.job.id = :jobId")
    List<JobFile> findByJobId(Long jobId);
}
