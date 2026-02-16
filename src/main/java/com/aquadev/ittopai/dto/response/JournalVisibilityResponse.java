package com.aquadev.ittopai.dto.response;


public record JournalVisibilityResponse(
        Boolean isDesign,
        Boolean isVideoCourses,
        Boolean isVacancy,
        Boolean isSignal,
        Boolean isPromo,
        Boolean isTest,
        Boolean isEmailVerified,
        Boolean isQuizzesExpired,
        Boolean isDebtor,
        Boolean isPhoneVerified,
        Boolean isOnlyProfile,
        Boolean isReferralProgram,
        Boolean isDzGroupIssue,
        Boolean isBirthday,
        Boolean isSchool,
        Boolean isNewsPopup,
        Boolean isSchoolBranch,
        Boolean isCollegeBranch,
        Boolean isHigherEducationBranch,
        Boolean isRussianBranch,
        Boolean isAcademyBranch
) {
}
