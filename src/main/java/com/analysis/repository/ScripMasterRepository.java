package com.analysis.repository;

import java.util.List;

import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import com.analysis.constants.Constants;
import com.analysis.documents.ScripMaster;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ScripMasterRepository {

    private final MongoTemplate mongoTemplate;

    public List<ScripMaster> findAll() {
        return mongoTemplate.findAll(ScripMaster.class, Constants.SCRIP_SYMBOL_EQ_COLLECTION);
    }
}
