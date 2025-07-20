package com.xiaomi.repository;

import com.xiaomi.entity.Knowledge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeRepository extends JpaRepository<Knowledge, Long> {
}