-- Store document-level metadata on child chunks for filtering and source display.
ALTER TABLE document_chunks
    ADD COLUMN IF NOT EXISTS title VARCHAR(500),
    ADD COLUMN IF NOT EXISTS section VARCHAR(500);

