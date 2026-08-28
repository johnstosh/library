/*
 * (c) Copyright 2025 by Muczynski
 */
package com.muczynski.library.email;

/**
 * Snapshot of a newly pending library-card application. Passed to the email
 * service so async sending does not depend on a live JPA entity.
 */
public class PendingApplicationNotice {

    private final Long applicationId;
    private final String applicantName;
    private final String applicantEmail;

    public PendingApplicationNotice(Long applicationId, String applicantName, String applicantEmail) {
        this.applicationId = applicationId;
        this.applicantName = applicantName;
        this.applicantEmail = applicantEmail;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public String getApplicantName() {
        return applicantName;
    }

    public String getApplicantEmail() {
        return applicantEmail;
    }
}
