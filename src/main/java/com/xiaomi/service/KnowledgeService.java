package com.xiaomi.service;

import com.xiaomi.entity.Knowledge;
import com.xiaomi.repository.KnowledgeRepository;
import org.apache.commons.text.similarity.CosineSimilarity; // 引入余弦相似度工具
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KnowledgeService {

    @Autowired
    private KnowledgeRepository knowledgeRepository;

    // 定义相似度阈值，只有得分高于此值的才被认为是有效匹配
    private static final double SIMILARITY_THRESHOLD = 0.5;

    /**
     * 根据用户问题，在知识库中智能查找最相似的问题并返回其答案。
     *
     * @param userQuestion 用户的问题
     * @return 如果找到相似度足够高的答案，则返回 Optional<String>；否则返回空。
     */
    public Optional<String> findAnswerInKnowledgeBase(String userQuestion) {
        // 1. 从数据库加载所有知识库条目
        List<Knowledge> allKnowledge = knowledgeRepository.findAll();
        if (allKnowledge.isEmpty()) {
            return Optional.empty();
        }

        // 2. 将用户的问题和知识库中的所有问题都转换为词频向量
        Map<CharSequence, Integer> userVector = toTermFrequencyVector(userQuestion);

        Knowledge bestMatch = null;
        double maxSimilarity = 0.0;

        CosineSimilarity cosineSimilarity = new CosineSimilarity();

        // 3. 遍历所有知识库条目，计算相似度并找出最佳匹配
        for (Knowledge knowledge : allKnowledge) {
            Map<CharSequence, Integer> knowledgeVector = toTermFrequencyVector(knowledge.getQuestion());

            // 计算两个向量的余弦相似度得分
            double similarity = cosineSimilarity.cosineSimilarity(userVector, knowledgeVector);

            // 更新最高分和最佳匹配项
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                bestMatch = knowledge;
            }
        }

        // 4. 如果最高分超过了我们设定的阈值，就返回答案
        System.out.printf("智能知识库查询: 用户问题='%s', 最佳匹配='%s', 相似度=%.2f%n",
                userQuestion, (bestMatch != null ? bestMatch.getQuestion() : "无"), maxSimilarity);

        if (bestMatch != null && maxSimilarity >= SIMILARITY_THRESHOLD) {
            return Optional.of(bestMatch.getAnswer());
        }

        return Optional.empty(); // 没有找到足够相似的问题
    }

    /**
     * 辅助方法：将一个句子转换为词频向量 (Map<词, 出现次数>)
     * 这是一个简化的分词和向量化实现。
     */
    private Map<CharSequence, Integer> toTermFrequencyVector(String text) {
        Map<CharSequence, Integer> vector = new HashMap<>();
        // 使用正则表达式进行一个简单的分词，这里会按非字母数字字符分割
        String[] terms = text.toLowerCase().split("[^\\u4e00-\\u9fa5a-zA-Z0-9]+");

        for (String term : terms) {
            if (term.isEmpty()) continue;
            vector.put(term, vector.getOrDefault(term, 0) + 1);
        }
        return vector;
    }
}