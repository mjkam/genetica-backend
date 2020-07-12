package com.example.demo.repository.mongo;

import com.example.demo.domain.mongo.Pipeline;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PipelineRepository extends MongoRepository<Pipeline, String> {
}
