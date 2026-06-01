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

  if (message.role === 'assistant') {
    return (
      <article className="message assistant">
        <div dangerouslySetInnerHTML={{ __html: html }} />
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
