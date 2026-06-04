import { Mail, Facebook, MessageCircle, ExternalLink } from 'lucide-react';

export function FeedbackPage() {
  const supportChannels = [
    {
      name: 'Email hỗ trợ',
      value: 'trinhdat24102003@gmail.com',
      link: 'mailto:trinhdat24102003@gmail.com',
      icon: <Mail size={24} />,
      desc: 'Phản hồi chi tiết về nội dung câu trả lời'
    },
    {
      name: 'Fanpage FBU',
      value: 'Trịnh Đạt',
      link: 'https://web.facebook.com/trinh.at.293350',
      icon: <Facebook size={24} />,
      desc: 'Nhắn tin trực tiếp qua Messenger'
    },
    {
      name: 'Zalo Group',
      value: 'Tham gia nhóm Chatbot',
      link: '#', // Thay link Zalo của bác vào đây
      icon: <MessageCircle size={24} />,
      desc: 'Cộng đồng người dùng FBU Chat'
    }
  ];

  return (
    <main className="admin-main">
      <header className="topbar">
        <div>
          <p>Hỗ trợ</p>
          <h2>Kênh phản hồi & Liên hệ</h2>
        </div>
      </header>

      <section className="feedback-wrap">
        <div className="admin-panel feedback-info">
          <div className="panel-heading">
            <MessageCircle size={22} />
            <div>
              <h3>Chúng tôi luôn lắng nghe</h3>
              <p>Hãy liên hệ với chúng tôi qua các kênh sau để đóng góp ý kiến hoặc báo lỗi.</p>
            </div>
          </div>

          <div className="channels-grid">
            {supportChannels.map((channel, idx) => (
              <a key={idx} href={channel.link} className="channel-card" target="_blank" rel="noreferrer">
                <div className="icon-box">{channel.icon}</div>
                <div className="text-box">
                  <h4>{channel.name}</h4>
                  <p>{channel.value}</p>
                  <span>{channel.desc}</span>
                </div>
  
  <ExternalLink size={16} className="external-icon" />
</a>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}