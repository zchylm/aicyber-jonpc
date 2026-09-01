import { useState } from "react";
import type { FormEvent } from "react";
import { assistantPrompts, getAssistantReply, type AssistantReply } from "../data/aiKnowledge";

type Message = { id: number; role: "assistant" | "user"; content: AssistantReply | string };

const welcomeReply: AssistantReply = {
  title: "JON. AI is ready",
  body: "Ask about a component, a workload or your next build decision. I will keep the answer practical.",
};

const apiBaseUrl = (import.meta.env.VITE_API_BASE_URL ?? "").replace(/\/$/, "");

function AiAssistant() {
  const [isOpen, setIsOpen] = useState(false);
  const [question, setQuestion] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [connectionStatus, setConnectionStatus] = useState<"ready" | "live" | "fallback">("ready");
  const [messages, setMessages] = useState<Message[]>([{ id: 1, role: "assistant", content: welcomeReply }]);

  async function askQuestion(value: string) {
    const trimmed = value.trim();
    if (!trimmed || isLoading) return;
    setIsLoading(true);
    setMessages((current) => [
      ...current,
      { id: Date.now(), role: "user", content: trimmed },
    ]);
    setQuestion("");

    try {
      const response = await fetch(`${apiBaseUrl}/api/ai/chat`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ message: trimmed }),
      });
      if (!response.ok) throw new Error("Chat API request failed");
      const answer: AssistantReply = await response.json();
      setConnectionStatus("live");
      setMessages((current) => [...current, { id: Date.now(), role: "assistant", content: answer }]);
    } catch {
      const offlineReply = getAssistantReply(trimmed);
      setConnectionStatus("fallback");
      setMessages((current) => [...current, {
        id: Date.now(),
        role: "assistant",
        content: {
          ...offlineReply,
          title: "JON. AI live service unavailable",
          body: `The live Gemini connection is temporarily unavailable. Here is the local demo guidance instead.\n\n${offlineReply.body}`,
        },
      }]);
    } finally {
      setIsLoading(false);
    }
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    askQuestion(question);
  }

  function resetConversation() {
    setMessages([{ id: Date.now(), role: "assistant", content: welcomeReply }]);
    setQuestion("");
    setConnectionStatus("ready");
  }

  const statusLabel = connectionStatus === "live" ? "Live / Gemini" : connectionStatus === "fallback" ? "Local demo" : "Ready";

  return (
    <div className={isOpen ? "ai-assistant ai-assistant-open" : "ai-assistant"}>
      {isOpen && (
        <section className="ai-panel" aria-label="JON. AI assistant">
          <header className="ai-panel-header">
            <div>
              <span className="ai-panel-kicker"><i /> JON. AI / {statusLabel}</span>
              <h2>Ask about your build.</h2>
            </div>
            <div className="ai-panel-actions">
              <button className="ai-reset" type="button" onClick={resetConversation} aria-label="Start a new conversation" title="Start a new conversation">↺</button>
              <button className="ai-close" type="button" onClick={() => setIsOpen(false)} aria-label="Close JON. AI">×</button>
            </div>
          </header>

          <div className="ai-messages" aria-live="polite">
            {messages.map((message) => (
              <div className={message.role === "assistant" ? "ai-message ai-message-assistant" : "ai-message ai-message-user"} key={message.id}>
                {typeof message.content === "string" ? (
                  <p>{message.content}</p>
                ) : (
                  <>
                    <strong>{message.content.title}</strong>
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

          {messages.length === 1 && (
            <div className="ai-prompts" aria-label="Suggested questions">
              {assistantPrompts.slice(0, 3).map((prompt) => <button type="button" key={prompt} onClick={() => askQuestion(prompt)}>{prompt}</button>)}
            </div>
          )}

          <form className="ai-input" onSubmit={handleSubmit}>
            <label className="sr-only" htmlFor="ai-question">Ask JON. AI a question</label>
            <input id="ai-question" value={question} onChange={(event) => setQuestion(event.target.value)} placeholder="Ask about GPU, RAM or your use case" />
            <button type="submit" aria-label="Send question" disabled={isLoading}>↗</button>
          </form>
          <p className="ai-disclaimer">AI guidance for exploration. Pricing and availability are confirmed by JON. PC.</p>
        </section>
      )}

      <button className="ai-launcher" type="button" onClick={() => setIsOpen((current) => !current)} aria-expanded={isOpen} aria-label={isOpen ? "Close JON. AI" : "Open JON. AI assistant"}>
        <span className="ai-launcher-mark" aria-hidden="true"><i /><i /><i /></span>
        <span>JON. AI</span>
        <b aria-hidden="true">↗</b>
      </button>
    </div>
  );
}

export default AiAssistant;
