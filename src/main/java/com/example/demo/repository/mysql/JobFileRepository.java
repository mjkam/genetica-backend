package com.example.demo.repository.mysql;

import com.example.demo.domain.mysql.JobFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobFileRepository extends JpaRepository<JobFile, Long> {
}
