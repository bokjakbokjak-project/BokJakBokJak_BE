package kr.bokjakbokjak.matching;

import java.util.List;

/** Internal user data contract for recommendations, chat, and AI analysis. */
public interface UserQueryService {
    /** Returns eligible candidates. The caller filters users who already have a final match. */
    List<CandidateDto> findCandidates(Long viewerId);

    /** Returns existing users, including withdrawn users. Unknown IDs are omitted. */
    List<UserSummaryDto> getUserSummaries(List<Long> userIds);
}
