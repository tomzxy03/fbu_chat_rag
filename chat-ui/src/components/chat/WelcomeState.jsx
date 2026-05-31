import { Bot } from 'lucide-react';
import { SUGGESTIONS } from '../../constants/appConstants';

export function WelcomeState({ onSuggestion }) {
  return (
    <div className="welcome-state">
      <div className="welcome-icon">
        <Bot size={34} />
      </div>
      <h2>Xin chào! Tôi là trợ lý AI của FBU</h2>
      <p>Hãy hỏi tôi về quy chế, quy trình, học bổng hoặc thông tin của trường.</p>
      <div className="suggestions">
        {SUGGESTIONS.map((suggestion) => (
          <button key={suggestion} type="button" onClick={() => onSuggestion(suggestion)}>
            {suggestion}
          </button>
        ))}
      </div>
    </div>
  );
}
