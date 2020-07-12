package com.example.demo.repository.mysql;

import com.example.demo.domain.mysql.Job;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {
}
