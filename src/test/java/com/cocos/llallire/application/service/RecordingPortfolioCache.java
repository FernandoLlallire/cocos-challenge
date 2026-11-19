package com.cocos.llallire.application.service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.cocos.llallire.application.dto.PortfolioInformation;
import com.cocos.llallire.application.port.out.PortfolioCachePort;

/**
 * Test double of {@link PortfolioCachePort}: never caches (always calls the loader) and records invalidations.
 */
class RecordingPortfolioCache implements PortfolioCachePort {
    final List<Long> invalidatedUsers = new ArrayList<>();
    int loaderCalls;

    @Override
    public PortfolioInformation getOrCompute(Long userId, Supplier<PortfolioInformation> loader) {
        loaderCalls++;
        return loader.get();
    }

    @Override
    public void invalidate(Long userId) {
        invalidatedUsers.add(userId);
    }
}
