# GlobalSettings

Controller: `GlobalSettingsController`.

Base path: `/api/global-settings`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

## GET /api/global-settings

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

None.

Response: `200 OK` body `GlobalSettingsDto` fields: `googleClientSecret`, `googleClientSecretPartial`, `googleClientSecretUpdatedAt`, `googleClientId`, `redirectUri`, `googleSsoClientId`, `googleSsoClientSecret`, `googleSsoClientSecretPartial`, `googleSsoCredentialsUpdatedAt`, `googleSsoClientSecretConfigured`, `googleSsoClientIdConfigured`, `lastUpdated`, `googleClientSecretConfigured`, `googleClientSecretValidation`, `googleSsoClientSecretValidation`, `emailMethod`, `emailFromAddress`, `emailFromName`, `emailNotifyLibrariansOnPending`, `emailNotifyApplicantOnPending`, `emailLibrarianRecipients`, `emailIncludeLibrarianUserEmails`, `emailNotifyLibrariansOnLoanChange`, `emailNotifyBorrowerOnLoanChange`, `smtpHost`, `smtpPort`, `smtpUsername`, `smtpUsernameSource`, `smtpPassword`, `smtpPasswordPartial`, `smtpPasswordConfigured`, `smtpPasswordSource`, `smtpStartTls`, `smtpSsl`, `sendGridApiKey`, `sendGridApiKeyPartial`, `sendGridApiKeyConfigured`, `webhookUrl`, `webhookBearerToken`, `webhookBearerTokenPartial`, `webhookBearerTokenConfigured`, `emailMethodConfigured`, `emailMethodStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Get global settings Only librarians can view global settings

## PUT /api/global-settings

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `GlobalSettingsDto` (required).

Response: `200 OK` body `GlobalSettingsDto` fields: `googleClientSecret`, `googleClientSecretPartial`, `googleClientSecretUpdatedAt`, `googleClientId`, `redirectUri`, `googleSsoClientId`, `googleSsoClientSecret`, `googleSsoClientSecretPartial`, `googleSsoCredentialsUpdatedAt`, `googleSsoClientSecretConfigured`, `googleSsoClientIdConfigured`, `lastUpdated`, `googleClientSecretConfigured`, `googleClientSecretValidation`, `googleSsoClientSecretValidation`, `emailMethod`, `emailFromAddress`, `emailFromName`, `emailNotifyLibrariansOnPending`, `emailNotifyApplicantOnPending`, `emailLibrarianRecipients`, `emailIncludeLibrarianUserEmails`, `emailNotifyLibrariansOnLoanChange`, `emailNotifyBorrowerOnLoanChange`, `smtpHost`, `smtpPort`, `smtpUsername`, `smtpUsernameSource`, `smtpPassword`, `smtpPasswordPartial`, `smtpPasswordConfigured`, `smtpPasswordSource`, `smtpStartTls`, `smtpSsl`, `sendGridApiKey`, `sendGridApiKeyPartial`, `sendGridApiKeyConfigured`, `webhookUrl`, `webhookBearerToken`, `webhookBearerTokenPartial`, `webhookBearerTokenConfigured`, `emailMethodConfigured`, `emailMethodStatus`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Update global settings Only librarians can update

## POST /api/global-settings/test-email

Auth: Librarian. method `@PreAuthorize("hasAuthority('LIBRARIAN')")`; HTTP `anyRequest` requires a logged-in user.

Parameters:

- Body `TestEmailRequest` (optional).

Response: `200 OK` body `TestEmailResultDto` fields: `sent`, `method`, `message`, `recipients`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Send a test message using the currently saved email method. Save settings before calling this; unsaved form values are not used.

## GET /api/global-settings/sso-status

Auth: Public. no `@PreAuthorize`; HTTP `/api/global-settings/sso-status` is permitAll.

Parameters:

None.

Response: `200 OK` body `SsoStatusDto`.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Check if SSO is configured Public endpoint - no authentication required
