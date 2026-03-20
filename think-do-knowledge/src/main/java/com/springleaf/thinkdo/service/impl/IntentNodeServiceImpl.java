package com.springleaf.thinkdo.service.impl;

import com.springleaf.thinkdo.domain.response.IntentNodeTreeResp;
import com.springleaf.thinkdo.service.IntentNodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class IntentNodeServiceImpl implements IntentNodeService {
    @Override
    public List<IntentNodeTreeResp> getFullTree() {
        return List.of();
    }
}
