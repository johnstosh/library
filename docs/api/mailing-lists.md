# Mailing lists

Controller: `MailingListController`.

Base path: `(none)`.

Dates and times in JSON are ISO-8601 strings. This file documents the API in the current source tree.

A subscription is one email, one list, and one frequency (`DAILY`, `WEEKLY`, or `MONTHLY`). An address already on a list keeps that frequency. Changing it means unsubscribing and signing up again. The unsubscribe token is a random secret stored on the subscription. It is not logged.

## POST /api/public/mailing-lists/subscriptions

Auth: Public. no `@PreAuthorize`; HTTP `/api/public/**` is permitAll. No login.

Parameters:

- Body `MailingListSignupRequest` fields: `email` (`String`, required), `lists` (`MailingListTopic` list, required, one or more), `frequency` (`MailingFrequency`, required).

`MailingListTopic` values: `LIVES_OF_SAINTS`, `CHILDRENS_FICTION`, `READ_ALOUD_BOOKS`, `NEW_CATALOG_ENTRIES`, `ADULT_FICTION`.

Response: `200 OK` body `MailingListSignupResponse` field `results`, a list of lines. Each line has `list`, `displayName`, `frequency`, `status` (`created`, `resubscribed`, or `already_subscribed`), `message`, and `sent`. `400 Bad Request` body `ErrorResponse` when the email, frequency, or list selection is missing or invalid.

Paging: None.

Ordering: Results follow the distinct lists in the request.

Notes:

Creates one subscription per selected list. A new or reactivated subscription is sent the previous-period issue immediately. An active subscription is left unchanged, including its frequency, and `message` says to unsubscribe first to change frequency. Email is disabled by default, so `sent` is false until outbound mail is configured.

## GET /mailing-lists/unsubscribe/{token}

Auth: Public. no `@PreAuthorize`; HTTP `/mailing-lists/unsubscribe/**` is permitAll. No login and no confirmation step.

Parameters:

- Path `token` (`String`, required). The subscription's unguessable unsubscribe secret.

Response: `200 OK` `text/html`. The page says the patron has been unsubscribed from that list, or is already unsubscribed. `404 Not Found` `text/html` when the token matches no subscription.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

Unsubscribes immediately. A second GET with the same token is idempotent and says the patron is already unsubscribed. The row and token stay so the link still resolves.

## POST /mailing-lists/unsubscribe/{token}

Auth: Public. no `@PreAuthorize`; HTTP `/mailing-lists/unsubscribe/**` is permitAll. No login and no confirmation step.

Parameters:

- Path `token` (`String`, required). The same secret as the GET link. A body is not required. Mail clients may POST `List-Unsubscribe=One-Click`.

Response: `200 OK` `text/plain` with the same sentence as the HTML page. `404 Not Found` `text/plain` when the token matches no subscription.

Paging: None.

Ordering: Not specified by this endpoint.

Notes:

One-click unsubscribe. A repeated POST stays unsubscribed. The token is not written to the log.
