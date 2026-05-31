import { useMemo } from 'react';
import DOMPurify from 'dompurify';
import { marked } from 'marked';

export function MessageBubble({ message }) {
  const html = useMemo(() => {
    if (message.role !== 'assistant') return null;
    return DOMPurify.sanitize(marked.parse(message.content || ''));
  }, [message]);

  if (message.role === 'assistant') {
    return <article className="message assistant" dangerouslySetInnerHTML={{ __html: html }} />;
  }

  return <article className="message user">{message.content}</article>;
}
