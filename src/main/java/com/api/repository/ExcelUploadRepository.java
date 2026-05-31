package com.api.repository;

import com.api.model.ExcelUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcelUploadRepository extends JpaRepository<ExcelUpload, Long> {
    List<ExcelUpload> findByUploadedByOrderByUploadedAtDesc(String username);
}
