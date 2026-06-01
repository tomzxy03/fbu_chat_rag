import { useState } from 'react';
import { MessageCircle, Send } from 'lucide-react';

export function FeedbackPage() {
  const [form, setForm] = useState({ topic: '', message: '', contact: '' });
  const [status, setStatus] = useState(null);

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
    setStatus(null);
  };

  const handleSubmit = (event) => {
    event.preventDefault();
    setStatus({ type: 'success', text: 'Đã ghi nhận góp ý của bạn.' });
    setForm({ topic: '', message: '', contact: '' });
  };

  return (
    <main className="admin-main">
      <header className="topbar">
        <div>
          <p>Góp ý</p>
          <h2>Phản hồi trải nghiệm</h2>
        </div>
      </header>

      <section className="feedback-wrap">
        <form className="admin-panel feedback-form" onSubmit={handleSubmit}>
          <div className="panel-heading">
            <MessageCircle size={22} />
            <div>
              <h3>Nội dung góp ý</h3>
              <p>Ý kiến của bạn giúp cải thiện trợ lý FBU Chat.</p>
            </div>
          </div>

          <label>
            Chủ đề
            <input
              onChange={(event) => updateField('topic', event.target.value)}
              placeholder="Ví dụ: Câu trả lời, giao diện, tài liệu"
              required
              value={form.topic}
            />
          </label>

          <label>
            Góp ý
            <textarea
              onChange={(event) => updateField('message', event.target.value)}
              placeholder="Nhập nội dung góp ý..."
              required
              rows={7}
              value={form.message}
            />
          </label>

          <label>
            Liên hệ
            <input
              onChange={(event) => updateField('contact', event.target.value)}
              placeholder="Email hoặc số điện thoại"
              value={form.contact}
            />
          </label>

          <button className="primary-button feedback-submit" type="submit">
            <Send size={17} />
            Gửi góp ý
          </button>

          {status && <p className={`status-text ${status.type}`}>{status.text}</p>}
        </form>
      </section>
    </main>
  );
}
