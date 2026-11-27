package com.clausify.user;

import com.clausify.analysis.AnalysisRepository;
import com.clausify.common.NotFoundException;
import com.clausify.contract.ContractRepository;
import com.clausify.contract.ContractStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-12: the signed-in user's account details, profile edits, and statistics. */
@Service
public class AccountService {

    private final UserRepository users;
    private final ContractRepository contracts;
    private final AnalysisRepository analyses;

    public AccountService(UserRepository users, ContractRepository contracts, AnalysisRepository analyses) {
        this.users = users;
        this.contracts = contracts;
        this.analyses = analyses;
    }

    @Transactional(readOnly = true)
    public AccountResponse get(Long userId) {
        return AccountResponse.from(find(userId));
    }

    @Transactional
    public AccountResponse update(Long userId, UpdateAccountRequest request) {
        User user = find(userId);
        if (request.name() != null) {
            user.setName(request.name().strip());
        }
        if (request.phoneNumber() != null) {
            String phone = request.phoneNumber().strip();
            Profile profile = user.getProfile();
            if (profile == null) {
                profile = Profile.builder().user(user).build();
                user.setProfile(profile);
            }
            profile.setPhoneNumber(phone.isEmpty() ? null : phone);
        }
        return AccountResponse.from(user);
    }

    @Transactional(readOnly = true)
    public AccountStatsResponse stats(Long userId) {
        Double average = analyses.averageOverallScoreByUserId(userId);
        return new AccountStatsResponse(contracts.countByUserId(userId),
                contracts.countByUserIdAndStatus(userId, ContractStatus.ANALYZED),
                average == null ? null : (int) Math.round(average));
    }

    private User find(Long userId) {
        // The token outlived its account (deleted); same 404 as GET /api/auth/me.
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));
    }
}
