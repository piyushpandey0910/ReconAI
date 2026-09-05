import React from 'react';
import { MatchResult } from '../types';
import { X, ShieldCheck, Cpu, UserCheck, AlertCircle, FileText, CheckCircle2 } from 'lucide-react';

interface AuditModalProps {
  record: MatchResult | null;
  onClose: () => void;
}

export const AuditModal: React.FC<AuditModalProps> = ({ record, onClose }) => {
  if (!record) return null;

  const getStatusBadge = () => {
    switch (record.matchStatus) {
      case 'MATCHED':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-emerald-500/20 text-emerald-400 border border-emerald-500/30">
            <CheckCircle2 className="w-3 h-3 mr-1" />
            MATCHED
          </span>
        );
      case 'MANUAL_REVIEW':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-amber-500/20 text-amber-400 border border-amber-500/30">
            <AlertCircle className="w-3 h-3 mr-1" />
            MANUAL REVIEW
          </span>
        );
      case 'NEEDS_REVIEW':
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-blue-500/20 text-blue-400 border border-blue-500/30">
            NEEDS REVIEW (Pass 1 Leftover)
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-1 rounded-full text-xs font-semibold bg-red-500/20 text-red-400 border border-red-500/30">
            DISCREPANCY
          </span>
        );
    }
  };

  const getPassIcon = () => {
    if (record.matchPass === 'PASS1_RULES') {
      return (
        <span className="inline-flex items-center text-xs font-mono text-blue-400 bg-blue-500/10 px-2 py-0.5 rounded border border-blue-500/20">
          <ShieldCheck className="w-3 h-3 mr-1" />
          Pass 1: Deterministic Rules
        </span>
      );
    }
    if (record.matchPass === 'PASS2_AI') {
      return (
        <span className="inline-flex items-center text-xs font-mono text-purple-400 bg-purple-500/10 px-2 py-0.5 rounded border border-purple-500/20">
          <Cpu className="w-3 h-3 mr-1" />
          Pass 2: Groq AI Classification
        </span>
      );
    }
    return (
      <span className="inline-flex items-center text-xs font-mono text-emerald-400 bg-emerald-500/10 px-2 py-0.5 rounded border border-emerald-500/20">
        <UserCheck className="w-3 h-3 mr-1" />
        Manual Analyst Decision
      </span>
    );
  };

  const confidence = (record.confidence || 0) * 100;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="relative w-full max-w-3xl max-h-[90vh] bg-slate-900 border border-slate-700 rounded-2xl shadow-2xl flex flex-col overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-800 bg-slate-800/50">
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400">
              <FileText className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <h3 className="text-base font-bold text-white font-mono">
                  Audit Trail: {record.gatewayTxnId || `Record #${record.id}`}
                </h3>
                {getStatusBadge()}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">Full decision lineage and explanation snapshot</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="p-6 overflow-y-auto space-y-6 text-sm">
          {/* Decision Summary Banner */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 p-4 rounded-xl bg-slate-950/60 border border-slate-800">
            <div>
              <span className="text-xs text-slate-400 block">Resolution Engine</span>
              <div className="mt-1">{getPassIcon()}</div>
            </div>
            <div>
              <span className="text-xs text-slate-400 block">Rule / AI Model Fired</span>
              <span className="text-xs font-mono font-semibold text-slate-200 mt-1 block">
                {record.ruleFired || 'N/A'}
              </span>
            </div>
            <div>
              <span className="text-xs text-slate-400 block">Confidence Score</span>
              <div className="flex items-center space-x-2 mt-1">
                <span className={`text-xs font-mono font-bold ${confidence >= 70 ? 'text-emerald-400' : 'text-amber-400'}`}>
                  {confidence.toFixed(1)}%
                </span>
                <div className="w-20 bg-slate-800 h-2 rounded-full overflow-hidden">
                  <div
                    className={`h-full ${confidence >= 70 ? 'bg-emerald-500' : 'bg-amber-500'}`}
                    style={{ width: `${Math.min(confidence, 100)}%` }}
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Reasoning */}
          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Decision Rationale & Explanation
            </h4>
            <div className="p-3.5 rounded-xl bg-blue-950/20 border border-blue-900/40 text-blue-200 text-xs leading-relaxed font-sans">
              {record.reasoning || 'No specific reasoning recorded.'}
            </div>
          </div>

          {/* 3-Way Record Comparison */}
          <div>
            <h4 className="text-xs font-semibold uppercase tracking-wider text-slate-400 mb-2">
              Multi-Source Lineage Comparison
            </h4>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {/* Gateway */}
              <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800/80">
                <span className="text-xs font-semibold text-blue-400 uppercase tracking-wide block mb-2">
                  1. Payment Gateway
                </span>
                <div className="space-y-1 text-xs font-mono">
                  <div className="text-slate-400">Txn: <span className="text-slate-200">{record.gatewayTxnId || 'N/A'}</span></div>
                  <div className="text-slate-400">Order: <span className="text-slate-200">{record.gatewayOrderId || 'N/A'}</span></div>
                  <div className="text-slate-400">Amount: <span className="text-emerald-400 font-bold">₹{record.gatewayAmount || '0.00'}</span></div>
                  <div className="text-slate-400 truncate">Customer: <span className="text-slate-300">{record.gatewayCustomerEmail || 'N/A'}</span></div>
                </div>
              </div>

              {/* Bank */}
              <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800/80">
                <span className="text-xs font-semibold text-emerald-400 uppercase tracking-wide block mb-2">
                  2. Bank Statement
                </span>
                <div className="space-y-1 text-xs font-mono">
                  <div className="text-slate-400">Bank Ref: <span className="text-slate-200">{record.bankRefId || 'UNMATCHED'}</span></div>
                  <div className="text-slate-400">Amount: <span className="text-emerald-400 font-bold">{record.bankAmount ? `₹${record.bankAmount}` : 'N/A'}</span></div>
                  <div className="text-slate-400">Bank Fee: <span className="text-amber-400">₹{record.bankFee || '0.00'}</span></div>
                  <div className="text-slate-400 truncate">Narration: <span className="text-slate-300">{record.bankNarration || 'N/A'}</span></div>
                </div>
              </div>

              {/* Ledger */}
              <div className="p-3.5 rounded-xl bg-slate-950/80 border border-slate-800/80">
                <span className="text-xs font-semibold text-purple-400 uppercase tracking-wide block mb-2">
                  3. General Ledger
                </span>
                <div className="space-y-1 text-xs font-mono">
                  <div className="text-slate-400">Entry ID: <span className="text-slate-200">{record.ledgerEntryId || 'UNMATCHED'}</span></div>
                  <div className="text-slate-400">Internal Ref: <span className="text-slate-200">{record.ledgerInternalRef || 'N/A'}</span></div>
                  <div className="text-slate-400">Amount: <span className="text-emerald-400 font-bold">{record.ledgerAmount ? `₹${record.ledgerAmount}` : 'N/A'}</span></div>
                  <div className="text-slate-400 truncate">Desc: <span className="text-slate-300">{record.ledgerDescription || 'N/A'}</span></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Footer */}
        <div className="px-6 py-3 border-t border-slate-800 bg-slate-800/30 flex justify-end">
          <button
            onClick={onClose}
            className="px-4 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-200 text-xs font-medium transition"
          >
            Close Audit Trail
          </button>
        </div>
      </div>
    </div>
  );
};
