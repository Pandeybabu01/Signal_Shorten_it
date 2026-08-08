# Privacy Policy

**Last updated:** 2026-08-08

This Privacy Policy explains what data the URL Shortener Service ("the
Service", "we", "us") collects when you create short links or click on
them, why we collect it, how long we keep it, and the choices available to
you. It is written to be read by both end users and the engineers
operating the Service, and it should be kept in sync with `application.yml`
and the `ClickTrackingService` implementation - if the code changes, this
document must change with it.

## 1. Data We Collect

### 1.1 Account data (registered users only)
- Username, email address, and a bcrypt hash of your password (we never
  store or log the plaintext password).
- An API key used for programmatic access.
- Account timestamps (created/updated).

### 1.2 Short link data
- The destination URL, short code, optional custom alias, optional title,
  optional expiry date, optional click limit, and (if you set one) a
  bcrypt hash of a link-access password.
- The owning account, if the link was created while logged in.

### 1.3 Click / analytics data
Each time someone opens a short link, we record:
- **Timestamp** of the click.
- **A salted SHA-256 hash of the visitor's IP address** (`ipHash`), *not*
  the IP address itself. The hash lets us approximate unique visitors
  without retaining anyone's real IP. The raw IP is used only in-memory,
  for the single request, to (a) compute this hash and (b) enforce rate
  limiting; it is discarded immediately afterward and never written to the
  database or to persistent logs.
- **User-Agent string**, parsed into a coarse device type (Desktop /
  Mobile / Tablet), browser family, and OS family. The raw User-Agent is
  stored for that parsing to remain reproducible, but it is not linked to
  an individual identity.
- **Referrer header**, if the visitor's browser sent one.
- **Country code**, only if your deployment sits behind a provider (such
  as Cloudflare) that supplies a coarse `CF-IPCountry` header. We do not
  perform IP-based geolocation lookups ourselves in the default
  configuration.

We deliberately do **not** collect: names, precise geolocation
(GPS-level), device identifiers, cookies for cross-site tracking, or
third-party advertising identifiers.

## 2. Why We Collect It

- To operate the core function of the Service: redirecting a short code to
  its destination URL.
- To provide the analytics dashboard (click counts, trends over time,
  device/browser/referrer breakdowns) that link creators use to understand
  traffic to their links.
- To protect the Service and its users from abuse, via rate limiting and
  malicious-domain blocking.
- To secure accounts (password hashing, JWT-based session tokens).

## 3. Legal Basis

Where applicable data-protection law requires a legal basis (e.g. GDPR),
we rely on: performance of a contract (providing the shortening service
you requested), and legitimate interests (abuse prevention, service
security, and aggregate analytics for link owners) balanced against user
privacy - which is why IP addresses are hashed rather than stored in the
clear.

## 4. Data Retention

- **Click events**: retained for as long as the associated short link
  exists. Deleting a short link cascades and deletes its click events
  (`ON DELETE CASCADE` in the schema).
- **Account data**: retained until the account is deleted upon request.
- **Expired/inactive links**: automatically deactivated by a scheduled job
  (`LinkCleanupScheduler`) but not auto-deleted, so owners can still see
  historical analytics; owners may delete them manually at any time via
  `DELETE /api/urls/{shortCode}`.

Operators deploying this Service should set a retention policy appropriate
to their jurisdiction and document any changes here.

## 5. Data Sharing

We do not sell personal data. We do not share click analytics with third
parties. If you deploy this Service behind a CDN or reverse proxy (e.g.
Cloudflare, AWS CloudFront), that provider will process traffic metadata
(such as IP addresses) as part of delivering requests to this Service,
under its own privacy policy - review and disclose that separately if you
operate a public instance.

## 6. Your Rights and Choices

Subject to applicable law, you may:
- **Access** the short links and analytics associated with your account
  via the dashboard or `GET /api/urls`.
- **Delete** a short link (and its click history) at any time.
- **Delete your account**, which removes your account record; short links
  you created remain functional but are disassociated from any owner
  unless you request their deletion too (contact the operator).
- **Export** your link list via the `GET /api/urls` API.

## 7. Security Measures

- Passwords are hashed with bcrypt (work factor 12).
- Link-access passwords (optional, per-link) are also bcrypt-hashed.
- Sessions use short-lived JWT access tokens plus longer-lived refresh
  tokens, transmitted over HTTPS in production.
- IP addresses are hashed with a server-side secret (pepper) before
  storage; the pepper is never exposed via any API.
- All administrative and analytics endpoints require authentication;
  ownership checks prevent one user from viewing another user's link
  analytics.

## 8. Children's Privacy

The Service is not directed at children and does not knowingly collect
personal data from children under the age of applicable consent in their
jurisdiction.

## 9. Changes to This Policy

We will update the "Last updated" date above whenever this policy
changes. Operators of a deployed instance should notify users of material
changes through appropriate means (email, in-app notice, etc.).

## 10. Contact

For privacy questions or data-subject requests, contact the operator of
this instance at the support address configured for this deployment
(default placeholder: `privacy@example.com` - update before going to
production).
