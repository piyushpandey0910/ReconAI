import React, { useState, useEffect, useRef } from 'react';
import { ChatMessage } from '../types';
import { chatApi } from '../api/client';
import { X, Send, Bot, User, Sparkles, AlertCircle, Loader2 } from 'lucide-react';

interface ChatDrawerProps {
  batchId: number | null;
  batchName: string;
  isOpen: boolean;
  onClose: () => void;
}

export const ChatDrawer: React.FC<ChatDrawerProps> = ({
  batchId,
  batchName,
  isOpen,
  onClose,
}) => {
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const quickPrompts = [
    "Summarize reconciliation results for this batch",
    "How much revenue is stuck in manual review?",
    "Show me all fee discrepancies",
    "Why wasn't transaction GW_TXN_155 matched?"
  ];

  useEffect(() => {
    if (isOpen && batchId) {
      loadHistory();
    }
  }, [isOpen, batchId]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, loading]);

  const loadHistory = async () => {
    if (!batchId) return;
    try {
      const history = await chatApi.getHistory(batchId);
      setMessages(history);
    } catch (err: any) {
      console.error('Failed to load chat history:', err);
    }
  };

  const handleSend = async (textToSend?: string) => {
    const query = textToSend || input;
    if (!query.trim() || !batchId || loading) return;

    setError(null);
    setInput('');

    // Optimistic user message
    const userMsg: ChatMessage = {
      id: Date.now(),
      batchId,
      username: 'You',
      sender: 'USER',
      message: query,
      timestamp: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);
    setLoading(true);

    try {
      const res = await chatApi.sendMessage(batchId, query);
      const assistantMsg: ChatMessage = {
        id: Date.now() + 1,
        batchId,
        username: res.modelUsed || 'Groq AI Controller',
        sender: 'ASSISTANT',
        message: res.answer,
        timestamp: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to receive answer from AI agent');
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-y-0 right-0 z-40 w-full sm:w-[480px] bg-slate-900 border-l border-slate-800 shadow-2xl flex flex-col animate-in slide-in-from-right duration-300">
      {/* Header */}
      <div className="px-5 py-4 border-b border-slate-800 flex items-center justify-between bg-slate-950/60">
        <div className="flex items-center space-x-3">
          <div className="p-2 rounded-xl bg-purple-500/10 text-purple-400 border border-purple-500/20">
            <Bot className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center space-x-2">
              <h3 className="text-sm font-bold text-white">AI Financial Controller</h3>
              <span className="text-[10px] font-mono font-semibold px-1.5 py-0.5 rounded bg-purple-500/20 text-purple-300 border border-purple-500/30">
                Grounded Q&A
              </span>
            </div>
            <p className="text-xs text-slate-400 truncate max-w-[260px]">
              Active Context: <span className="text-slate-300 font-medium">{batchName}</span>
            </p>
          </div>
        </div>
        <button
          onClick={onClose}
          className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {/* Messages List */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.length === 0 && (
          <div className="p-6 text-center space-y-3">
            <div className="h-12 w-12 mx-auto rounded-2xl bg-purple-500/10 border border-purple-500/20 flex items-center justify-center text-purple-400">
              <Sparkles className="h-6 w-6" />
            </div>
            <h4 className="text-sm font-semibold text-slate-200">Ask Natural Language Questions</h4>
            <p className="text-xs text-slate-400 leading-relaxed max-w-xs mx-auto">
              The AI Agent is grounded directly in this batch's transactions, match rates, and discrepancy logs.
            </p>
            <div className="pt-2 text-left space-y-2">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-slate-500 block">
                Suggested questions:
              </span>
              {quickPrompts.map((prompt, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSend(prompt)}
                  className="w-full text-left text-xs p-2.5 rounded-lg bg-slate-950 hover:bg-slate-800 text-slate-300 hover:text-white border border-slate-800 transition"
                >
                  "{prompt}"
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m) => (
          <div
            key={m.id}
            className={`flex items-start space-x-2.5 ${m.sender === 'USER' ? 'justify-end' : 'justify-start'}`}
          >
            {m.sender !== 'USER' && (
              <div className="p-1.5 rounded-lg bg-purple-500/10 text-purple-400 border border-purple-500/20 mt-1 shrink-0">
                <Bot className="h-4 w-4" />
              </div>
            )}
            <div
              className={`max-w-[85%] rounded-2xl px-4 py-3 text-xs leading-relaxed ${
                m.sender === 'USER'
                  ? 'bg-blue-600 text-white rounded-br-none'
                  : 'bg-slate-950 border border-slate-800 text-slate-200 rounded-bl-none shadow-sm'
              }`}
            >
              <div className="whitespace-pre-wrap font-sans">{m.message}</div>
              <div
                className={`mt-1.5 text-[10px] font-mono ${
                  m.sender === 'USER' ? 'text-blue-200' : 'text-slate-500'
                }`}
              >
                {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
              </div>
            </div>
            {m.sender === 'USER' && (
              <div className="p-1.5 rounded-lg bg-blue-600 text-white mt-1 shrink-0">
                <User className="h-4 w-4" />
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div className="flex items-center space-x-2 text-xs text-purple-400 p-2">
            <Loader2 className="h-4 w-4 animate-spin" />
            <span>Analyzing batch statistics & transactions with Groq LLM...</span>
          </div>
        )}

        {error && (
          <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-red-400 text-xs flex items-center space-x-2">
            <AlertCircle className="h-4 w-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input Form */}
      <div className="p-3 border-t border-slate-800 bg-slate-950/80">
        <form
          onSubmit={(e) => {
            e.preventDefault();
            handleSend();
          }}
          className="flex items-center space-x-2"
        >
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Ask about matches, fees, or specific transaction IDs..."
            disabled={loading}
            className="flex-1 bg-slate-900 border border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-slate-500 focus:outline-none focus:border-purple-500 disabled:opacity-50"
          />
          <button
            type="submit"
            disabled={!input.trim() || loading}
            className="p-2.5 rounded-xl bg-purple-600 hover:bg-purple-500 disabled:bg-slate-800 text-white transition active:scale-95 shadow-md shadow-purple-600/20"
          >
            <Send className="h-4 w-4" />
          </button>
        </form>
      </div>
    </div>
  );
};
