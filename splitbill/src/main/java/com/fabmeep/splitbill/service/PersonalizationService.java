package com.fabmeep.splitbill.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PersonalizationService {

    private final String githubUsername;
    private final int serviceChargePct;

    public PersonalizationService(@Value("${app.personalization.github-username:fabmeep}") String githubUsername) {
        if (githubUsername == null || githubUsername.trim().isEmpty()) {
            throw new IllegalArgumentException("GitHub username cannot be blank");
        }
        this.githubUsername = githubUsername.trim().toLowerCase();
        this.serviceChargePct = computeServiceChargePct(this.githubUsername);
    }

    private int computeServiceChargePct(String username) {
        int unicodeSum = username.chars().sum();
        return unicodeSum % 10;
    }

    public String getGithubUsername() {
        return githubUsername;
    }

    public int getServiceChargePct() {
        return serviceChargePct;
    }

    public BigDecimal calculateServiceChargeAmount(BigDecimal totalExpenses) {
        if (totalExpenses == null || totalExpenses.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return totalExpenses
            .multiply(BigDecimal.valueOf(serviceChargePct))
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
