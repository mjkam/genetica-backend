package com.example.demo.repository.mysql;

import com.example.demo.domain.mysql.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    List<File> findByIdIn(List<Long> ids);
}
