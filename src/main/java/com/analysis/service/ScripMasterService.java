package com.analysis.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.analysis.documents.ScripMaster;
import com.analysis.repository.ScripMasterRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ScripMasterService {

    private final ScripMasterRepository scripMasterRepository;

    public List<ScripMaster> getAllScripMasters() {
        return scripMasterRepository.findAll();
    }
}
