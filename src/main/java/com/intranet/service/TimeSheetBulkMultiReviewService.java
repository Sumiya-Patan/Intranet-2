package com.intranet.service;

import com.intranet.dto.TimeSheetBulkReviewRequestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TimeSheetBulkMultiReviewService {

    private final TimeSheetReviewService reviewService;

    @Transactional
    public Map<String, Object> reviewMultipleUsers(Long managerId, List<TimeSheetBulkReviewRequestDTO> bulkReviews) {
        if (bulkReviews == null || bulkReviews.isEmpty()) {
            throw new IllegalArgumentException("At least one user review request must be provided.");
        }

        List<String> successMessages = new ArrayList<>();
        List<String> failedUsers = new ArrayList<>();

        for (TimeSheetBulkReviewRequestDTO dto : bulkReviews) {
            try {
                // ✅ Reuse existing logic for each user's review
                String result = reviewService.reviewMultipleTimesheets(managerId, dto);
                successMessages.add(
                        String.format("User ID %d: %s", dto.getUserId(), result)
                );
            } 
            catch (IllegalArgumentException e) {
                failedUsers.add(String.format(
                        "User ID %d failed due to: %s", dto.getUserId(), e.getMessage()
                ));
                System.err.printf("❌ Review failed for user %d: %s%n", dto.getUserId(), e.getMessage());
            }
            catch (Exception e) {
                failedUsers.add(String.format(
                        "User ID %d failed due to: %s", dto.getUserId(), e.getMessage()
                ));
                System.err.printf("❌ Review failed for user %d: %s%n", dto.getUserId(), e.getMessage());
            }
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("totalRequests", bulkReviews.size());
        response.put("successfulReviews", successMessages.size());
        response.put("failedReviews", failedUsers.size());
        response.put("successMessages", successMessages);
        response.put("failedMessages", failedUsers);

        return response;
    }
}
