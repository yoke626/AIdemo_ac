package com.xiaomi.entity;

import lombok.Data;
import javax.persistence.*;

@Data
@Entity
@Table(name = "knowledge_base")
public class Knowledge {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String question; // 问题或关键词

    @Column(columnDefinition = "TEXT", nullable = false)
    private String answer; // 对应的答案

    // 【新增字段】用于存储图片的访问路径
    private String imageUrl;
}