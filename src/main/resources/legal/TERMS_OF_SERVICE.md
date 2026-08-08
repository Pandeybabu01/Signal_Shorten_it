# Terms of Service

**Last updated:** 2026-08-08

These Terms of Service ("Terms") govern your use of the URL Shortener
Service ("the Service"). By creating short links or using the API, you
agree to these Terms.

## 1. Acceptable Use

You agree not to use the Service to create links that point to content
that is illegal, that facilitates phishing or malware distribution, that
infringes intellectual property rights, or that otherwise violates
applicable law. The Service maintains a configurable domain blocklist
(`app.url-validation.blocked-domains`) and reserves the right to
deactivate any link that violates these Terms, with or without notice.

## 2. Rate Limits

To keep the Service available for everyone, link creation and redirect
traffic are rate-limited per account or IP address (see
`app.rate-limit.*` configuration). Exceeding these limits will result in
HTTP 429 responses until the limit window resets.

## 3. Accounts

You are responsible for maintaining the confidentiality of your account
credentials and API key. Notify the operator promptly if you suspect
unauthorized use of your account.

## 4. Link Ownership and Expiry

Links created while authenticated belong to your account and appear in
your dashboard. Links may expire on a schedule you configure, or be
capped to a maximum number of clicks; once expired, deactivated, or over
their click limit, the Service returns an HTTP 410 (Gone) response instead
of redirecting.

## 5. Availability

The Service is provided "as is" without warranty of any kind. The
operator does not guarantee uninterrupted availability and may perform
maintenance, rate-limit abusive traffic, or suspend accounts that violate
these Terms.

## 6. Analytics Data

By using the Service, you acknowledge that anonymized/pseudonymized click
analytics (see the Privacy Policy) are collected for links you create, and
are visible to you (or your account) via the analytics dashboard and API.

## 7. Termination

The operator may suspend or terminate accounts that violate these Terms,
including repeated creation of links to prohibited content or abusive API
usage that circumvents rate limiting.

## 8. Changes

These Terms may be updated from time to time; continued use of the
Service after changes take effect constitutes acceptance of the revised
Terms.

## 9. Contact

Questions about these Terms should be directed to the operator of this
deployment.
