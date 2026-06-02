Thiết kế cụ thể
Bước 1 — Schema bảng document_images:
sqlCREATE TABLE document_images (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    minio_url     VARCHAR(500) NOT NULL,    -- URL đầy đủ từ MinIO
    caption       VARCHAR(300),             -- "Thư viện tầng 3 cơ sở Mê Linh"
    tags          TEXT NOT NULL,            -- "thư viện, cơ sở vật chất, Mê Linh"
    tag_embedding vector(384),             -- embed của tags + caption
    category      VARCHAR(100),            -- "co_so_vat_chat", "giang_duong", "ky_tuc_xa"
    uploaded_at   TIMESTAMPTZ DEFAULT now()
);

CREATE INDEX idx_images_embedding
    ON document_images USING hnsw (tag_embedding vector_cosine_ops);
Bước 2 — Khi admin upload ảnh, Spring Boot làm 2 việc:
java// ImageService.java
public void uploadImage(MultipartFile file, String caption, String tags, String category) {

    // 1. Upload file lên MinIO
    String minioUrl = minioClient.upload(file, "images/" + UUID.randomUUID() + ".jpg");

    // 2. Embed caption + tags để search sau này
    String textToEmbed = caption + ". " + tags;  // "Thư viện tầng 3. thư viện, đọc sách, cơ sở"
    float[] embedding = embeddingService.embed(textToEmbed);

    // 3. Lưu DB
    DocumentImage image = DocumentImage.builder()
        .minioUrl(minioUrl)
        .caption(caption)
        .tags(tags)
        .tagEmbedding(embedding)
        .category(category)
        .build();
    imageRepo.save(image);
}
Bước 3 — Thêm image search vào RagService, chạy song song text search:
java// Trong chat() — sau khi có vectorStr
// Chạy song song text search và image search
List<ChunkResult> textChunks = searchTextChunks(vectorStr, request);
List<ImageResult> images     = searchImages(vectorStr, 3);  // top-3

// Image search
private List<ImageResult> searchImages(String vectorStr, int topN) {
    return imageRepo.findSimilarImages(vectorStr, topN, 0.70); // threshold 0.70 cao hơn text
}
Repository query:
java@Query(value = """
    SELECT minio_url AS minioUrl,
           caption,
           tags,
           1 - (tag_embedding <=> CAST(:queryVector AS vector)) AS score
    FROM document_images
    WHERE 1 - (tag_embedding <=> CAST(:queryVector AS vector)) > :threshold
    ORDER BY tag_embedding <=> CAST(:queryVector AS vector)
    LIMIT :topN
    """, nativeQuery = true)
List<ImageResult> findSimilarImages(
    @Param("queryVector") String queryVector,
    @Param("topN") int topN,
    @Param("threshold") double threshold
);
Bước 4 — Update ChatResponse thêm images:
java// ChatResponse.java — thêm field
@Builder
public class ChatResponse {
    private UUID conversationId;
    private String answer;
    private List<SourceInfo> sources;
    private List<ImageInfo> images;      // ← thêm

    @Builder
    public static class ImageInfo {
        private String url;
        private String caption;
        private double score;
    }
}