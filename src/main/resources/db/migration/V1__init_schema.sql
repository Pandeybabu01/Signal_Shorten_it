-- ==========================================================================
-- URL Shortener Service - Initial schema
-- ==========================================================================

CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL,
    email           VARCHAR(255) NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    api_key         VARCHAR(64)  NOT NULL,
    is_enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_api_key UNIQUE (api_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE short_urls (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_code        VARCHAR(20)   NOT NULL,
    original_url      VARCHAR(2048) NOT NULL,
    custom_alias      BOOLEAN       NOT NULL DEFAULT FALSE,
    title             VARCHAR(255),
    password_hash     VARCHAR(255),
    owner_id          BIGINT,
    click_count       BIGINT        NOT NULL DEFAULT 0,
    max_clicks        BIGINT,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    expires_at        DATETIME,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_short_urls_short_code UNIQUE (short_code),
    CONSTRAINT fk_short_urls_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_short_urls_owner_id ON short_urls (owner_id);
CREATE INDEX idx_short_urls_created_at ON short_urls (created_at);
CREATE INDEX idx_short_urls_is_active ON short_urls (is_active);

CREATE TABLE click_events (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    short_url_id    BIGINT        NOT NULL,
    clicked_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address      VARCHAR(64),
    ip_hash         VARCHAR(64),
    user_agent      VARCHAR(512),
    referrer        VARCHAR(1024),
    country_code    VARCHAR(2),
    city             VARCHAR(120),
    device_type     VARCHAR(20),
    browser         VARCHAR(60),
    os              VARCHAR(60),
    CONSTRAINT fk_click_events_short_url FOREIGN KEY (short_url_id) REFERENCES short_urls (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_click_events_short_url_id ON click_events (short_url_id);
CREATE INDEX idx_click_events_clicked_at ON click_events (clicked_at);
CREATE INDEX idx_click_events_short_url_clicked_at ON click_events (short_url_id, clicked_at);
