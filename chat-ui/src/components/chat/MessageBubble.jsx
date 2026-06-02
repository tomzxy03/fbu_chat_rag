import { useMemo } from 'react';
import DOMPurify from 'dompurify';
import { marked } from 'marked';

export function MessageBubble({ message }) {
  const html = useMemo(() => {
    if (message.role !== 'assistant') return null;
    return DOMPurify.sanitize(marked.parse(message.content || ''));
  }, [message]);
  const sources = Array.isArray(message.sources)
    ? message.sources.filter((source) => source?.file)
    : [];
  const images = Array.isArray(message.images)
    ? message.images.filter((image) => image?.url)
    : [];

  if (message.role === 'assistant') {
    return (
      <article className="message assistant">
        <div dangerouslySetInnerHTML={{ __html: html }} />
        {images.length > 0 && (
          <div className="message-image-gallery" aria-label="Hình ảnh liên quan">
            {images.map((image, index) => (
              <a
                className="message-image-card"
                href={image.url}
                key={`${image.url}-${index}`}
                rel="noreferrer"
                target="_blank"
              >
                <img src={image.url} alt={image.caption || 'Hình ảnh liên quan'} loading="lazy" />
                {(image.caption || image.category || typeof image.score === 'number') && (
                  <span className="message-image-meta">
                    {image.caption && <strong>{image.caption}</strong>}
                    {(image.category || typeof image.score === 'number') && (
                      <small>
                        {[image.category, typeof image.score === 'number' ? `${Math.round(image.score * 100)}%` : null]
                          .filter(Boolean)
                          .join(' - ')}
                      </small>
                    )}
                  </span>
                )}
              </a>
            ))}
          </div>
        )}
        {sources.length > 0 && (
          <div className="message-sources" aria-label="Nguồn trích dẫn">
            {sources.map((source, index) => (
              <span className="source-badge" key={`${source.file}-${source.year || 'unknown'}-${index}`}>
                {source.file}
              </span>
            ))}
          </div>
        )}
      </article>
    );
  }

  return <article className="message user">{message.content}</article>;
}
