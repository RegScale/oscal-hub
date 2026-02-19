-- V1.13: Add library item ratings and comments tables

-- Ratings table: stores user ratings for library items (1-5 stars)
CREATE TABLE IF NOT EXISTS library_item_ratings (
    id BIGSERIAL PRIMARY KEY,
    library_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rating_library_item FOREIGN KEY (library_item_id)
        REFERENCES library_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_rating_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uk_rating_user_item UNIQUE (library_item_id, user_id)
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_rating_library_item ON library_item_ratings(library_item_id);
CREATE INDEX IF NOT EXISTS idx_rating_user ON library_item_ratings(user_id);

-- Comments table: stores threaded comments for library items
CREATE TABLE IF NOT EXISTS library_item_comments (
    id BIGSERIAL PRIMARY KEY,
    comment_id VARCHAR(100) NOT NULL UNIQUE,
    library_item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    content TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_comment_library_item FOREIGN KEY (library_item_id)
        REFERENCES library_items(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_comment_id)
        REFERENCES library_item_comments(id) ON DELETE CASCADE
);

-- Indexes for efficient queries
CREATE INDEX IF NOT EXISTS idx_comment_library_item ON library_item_comments(library_item_id);
CREATE INDEX IF NOT EXISTS idx_comment_user ON library_item_comments(user_id);
CREATE INDEX IF NOT EXISTS idx_comment_parent ON library_item_comments(parent_comment_id);
CREATE INDEX IF NOT EXISTS idx_comment_created ON library_item_comments(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_comment_deleted ON library_item_comments(deleted);
