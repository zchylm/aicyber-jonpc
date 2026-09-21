import { useEffect, useRef, useState } from "react";
import type { FormEvent } from "react";
import BrandLockup from "./BrandLockup";
import officialLogo from "../assets/jonpc-official-logo.png";

type AssistantReply = {
  title: string;
  body: string;
  bullets?: string[];
  source?: string;
};

type Message = { id: number; role: "assistant" | "user"; content: AssistantReply | string; context: boolean };

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

function AiAssistant() {
  const [isOpen, setIsOpen] = useState(false);
  const [question, setQuestion] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<"ready" | "live" | "unavailable">("ready");
  const [messages, setMessages] = useState<Message[]>([]);
  const messagesRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const openAssistant = () => setIsOpen(true);
    window.addEventListener("jonpc:open-ai", openAssistant);
    return () => window.removeEventListener("jonpc:open-ai", openAssistant);
  }, []);

  useEffect(() => {
    if (messagesRef.current) messagesRef.current.scrollTop = messagesRef.current.scrollHeight;
  }, [messages, isLoading]);

  async function askQuestion(value: string) {
    const trimmed = value.trim();
    if (!trimmed || isLoading) return;
    const userMessageId = Date.now();
    const history = messages
      .filter((message) => message.context)
      .map((message) => ({
        role: message.role,
        content: typeof message.content === "string"
          ? message.content
          : [message.content.body, ...(message.content.bullets ?? [])].join("\n"),
      }));
    setIsLoading(true);
    setMessages((current) => [
      ...current,
      { id: userMessageId, role: "user", content: trimmed, context: true },
    ]);
    setQuestion("");

    try {
      const response = await fetch(`${apiBaseUrl}/api/ai/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: trimmed, history }),
      });
      if (!response.ok) throw new Error("Chat API request failed");
      const answer: AssistantReply = await response.json();
      setConnectionStatus("live");
      setMessages((current) => [...current, { id: Date.now(), role: "assistant", content: answer, context: answer.source === "claude" }]);
    } catch {
      setConnectionStatus("unavailable");
      setMessages((current) => [
        ...current.map((message) => message.id === userMessageId ? { ...message, context: false } : message),
        {
          id: Date.now(),
          role: "assistant",
          context: false,
          content: {
            title: "Connection interrupted.",
            body: "JON. AI could not answer that safely. Please check your connection and try again.",
          },
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    askQuestion(question);
  }

  function resetConversation() {
    setMessages([]);
    setQuestion("");
    setConnectionStatus("ready");
  }

  const statusLabel = connectionStatus === "live" ? "Live" : connectionStatus === "unavailable" ? "Unavailable" : "Ready";

  return (
    <div className={isOpen ? "ai-assistant ai-assistant-open" : "ai-assistant"}>
      {isOpen && (
        <section className="ai-panel" aria-label="JON. AI assistant">
          <header className="ai-panel-header">
            <div className="ai-panel-brand">
              <BrandLockup compact />
            </div>
            <div className="ai-panel-actions">
              <button className="ai-reset" type="button" onClick={resetConversation} aria-label="Start a new conversation" title="Start a new conversation">↺</button>
              <button className="ai-close" type="button" onClick={() => setIsOpen(false)} aria-label="Close JON. AI">×</button>
            </div>
            <div className={`ai-panel-status ai-panel-status-${connectionStatus}`}><i /> {statusLabel}</div>
            <div className="ai-panel-heading">
              <h2>Ask JON. AI.</h2>
              <p>Tell me what you need. Start anywhere.</p>
            </div>
          </header>

          {(messages.length > 0 || isLoading) && (
            <div className="ai-messages" aria-live="polite" ref={messagesRef}>
              {messages.map((message) => (
                <div className={message.role === "assistant" ? "ai-message ai-message-assistant" : "ai-message ai-message-user"} key={message.id}>
                  <span className="ai-message-label">{message.role === "assistant" ? "JON. AI" : "You"}</span>
                  {typeof message.content === "string" ? (
                    <p>{message.content}</p>
                  ) : (
                    <>
                      {message.content.title !== "JON. AI" && <strong>{message.content.title}</strong>}
                      <p>{message.content.body}</p>
                      {message.content.bullets && message.content.bullets.length > 0 && (
                        <ul>{message.content.bullets.map((bullet) => <li key={bullet}>{bullet}</li>)}</ul>
                      )}
                    </>
                  )}
                </div>
              ))}
              {isLoading && <div className="ai-message ai-message-assistant ai-message-loading"><span /><span /><span /> JON. AI is thinking</div>}
            </div>
          )}

          <form className="ai-input" onSubmit={handleSubmit}>
            <label className="sr-only" htmlFor="ai-question">Ask JON. AI a question</label>
            <input id="ai-question" value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Describe your goal or ask anything" autoComplete="off" maxLength={2000} />
            <button type="submit" aria-label="Send question" disabled={isLoading}>↗</button>
          </form>
        </section>
      )}

      <button className="ai-launcher" type="button" onClick={() => setIsOpen((current) => !current)} aria-expanded={isOpen} aria-label={isOpen ? "Close JON. AI" : "Open JON. AI assistant"}>
        <span className="ai-launcher-label" aria-hidden="true">Ask JON. AI</span>
        <span className="ai-launcher-portrait" aria-hidden="true"><img src={officialLogo} alt="" /></span>
        <i className="ai-launcher-status" aria-hidden="true" />
      </button>
    </div>
  );
}

export default AiAssistant;
