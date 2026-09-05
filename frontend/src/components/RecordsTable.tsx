import React, { useState } from 'react';
import { MatchResult, MatchStatus } from '../types';
import { useAuth } from '../context/AuthContext';
import { Search, Eye, CheckCircle2, AlertCircle, HelpCircle, FileSpreadsheet, UserCheck } from 'lucide-react';

interface RecordsTableProps {
  records: MatchResult[];
  selectedStatus: string;
  onStatusChange: (status: string) => void;
  searchQuery: string;
  onSearchChange: (q: string) => void;
  onSelectRecord: (record: MatchResult) => void;
  onManualReviewAction: (record: MatchResult, status: 'MATCHED' | 'DISCREPANCY', note: string) => void;
  onDownloadReport: () => void;
  loading: boolean;
}

export const RecordsTable: React.FC<RecordsTableProps> = ({
  records,
  selectedStatus,
  onStatusChange,
  searchQuery,
  onSearchChange,
  onSelectRecord,
  onManualReviewAction,
  onDownloadReport,
  loading,
}) => {
  const { canReconcile } = useAuth();
  const [overrideModalRecord, setOverrideModalRecord] = useState<MatchResult | null>(null);
  const [overrideNote, setOverrideNote] = useState('');

  const tabs = [
    { id: 'ALL', label: 'All Records' },
    { id: 'MATCHED', label: 'Matched' },
    { id: 'NEEDS_REVIEW', label: 'Needs Review (Pass 1)' },
    { id: 'MANUAL_REVIEW', label: 'Manual Review (Queue)' },
  ];

  const getStatusBadge = (status: MatchStatus) => {
    switch (status) {
      case 'MATCHED':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
            <CheckCircle2 className="w-3 h-3 mr-1" />
            MATCHED
          </span>
        );
      case 'MANUAL_REVIEW':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-amber-500/10 text-amber-400 border border-amber-500/20">
            <AlertCircle className="w-3 h-3 mr-1" />
            MANUAL REVIEW
          </span>
        );
      case 'NEEDS_REVIEW':
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-blue-500/10 text-blue-400 border border-blue-500/20">
            <HelpCircle className="w-3 h-3 mr-1" />
            NEEDS REVIEW
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium bg-red-500/10 text-red-400 border border-red-500/20">
            DISCREPANCY
          </span>
        );
    }
  };

  return (
    <div className="rounded-xl bg-slate-900/80 border border-slate-800 shadow-sm overflow-hidden">
      {/* Header controls: Tabs, Search, Export */}
      <div className="p-4 border-b border-slate-800 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
        {/* Filter Tabs */}
        <div className="flex items-center space-x-1 overflow-x-auto pb-1 sm:pb-0">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              onClick={() => onStatusChange(tab.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition ${
                selectedStatus === tab.id
                  ? 'bg-blue-600 text-white shadow'
                  : 'bg-slate-800/60 hover:bg-slate-800 text-slate-300'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* Search & Export button */}
        <div className="flex items-center space-x-2">
          <div className="relative">
            <Search className="h-4 w-4 absolute left-3 top-2.5 text-slate-400" />
            <input
              type="text"
              placeholder="Search Txn, Order, Email..."
              value={searchQuery}
              onChange={(e) => onSearchChange(e.target.value)}
              className="pl-9 pr-3 py-1.5 bg-slate-950 border border-slate-700 rounded-lg text-xs text-white placeholder-slate-500 focus:outline-none focus:border-blue-500 w-48 sm:w-60"
            />
          </div>

          <button
            onClick={onDownloadReport}
            className="inline-flex items-center space-x-1.5 px-3 py-1.5 rounded-lg bg-emerald-700 hover:bg-emerald-600 text-white text-xs font-medium transition shadow"
            title="Download Excel Report"
          >
            <FileSpreadsheet className="h-4 w-4" />
            <span className="hidden md:inline">Export Excel</span>
          </button>
        </div>
      </div>

      {/* Table */}
      <div className="overflow-x-auto">
        <table className="w-full text-left text-xs">
          <thead className="bg-slate-950/60 border-b border-slate-800 text-slate-400 uppercase tracking-wider font-mono">
            <tr>
              <th className="py-3 px-4">Status</th>
              <th className="py-3 px-4">Gateway Txn ID</th>
              <th className="py-3 px-4">Order ID</th>
              <th className="py-3 px-4 text-right">Amount</th>
              <th className="py-3 px-4">Bank Ref / Narration</th>
              <th className="py-3 px-4">Rule / Model Fired</th>
              <th className="py-3 px-4 text-center">Confidence</th>
              <th className="py-3 px-4 text-right">Actions</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800/60">
            {loading ? (
              <tr>
                <td colSpan={8} className="py-12 text-center text-slate-500">
                  Loading reconciliation records...
                </td>
              </tr>
            ) : records.length === 0 ? (
              <tr>
                <td colSpan={8} className="py-12 text-center text-slate-500">
                  No reconciliation records found for this filter.
                </td>
              </tr>
            ) : (
              records.map((r) => {
                const conf = (r.confidence || 0) * 100;
                return (
                  <tr key={r.id} className="hover:bg-slate-800/40 transition">
                    <td className="py-3 px-4">{getStatusBadge(r.matchStatus)}</td>
                    <td className="py-3 px-4 font-mono font-medium text-slate-200">
                      {r.gatewayTxnId || '—'}
                    </td>
                    <td className="py-3 px-4 font-mono text-slate-400">
                      {r.gatewayOrderId || '—'}
                    </td>
                    <td className="py-3 px-4 font-mono font-bold text-white text-right">
                      ${r.gatewayAmount ? r.gatewayAmount.toFixed(2) : '0.00'}
                    </td>
                    <td className="py-3 px-4 max-w-xs truncate">
                      {r.bankRefId ? (
                        <div>
                          <span className="font-mono text-slate-300 text-[11px] block">{r.bankRefId}</span>
                          <span className="text-slate-500 text-[11px] truncate block">{r.bankNarration}</span>
                        </div>
                      ) : (
                        <span className="text-slate-500 italic">No bank match</span>
                      )}
                    </td>
                    <td className="py-3 px-4 font-mono text-[11px] text-slate-300">
                      <span className="px-2 py-0.5 rounded bg-slate-800 border border-slate-700">
                        {r.ruleFired || 'UNMATCHED'}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-center">
                      <span
                        className={`font-mono text-xs font-bold ${
                          conf >= 80
                            ? 'text-emerald-400'
                            : conf >= 70
                            ? 'text-blue-400'
                            : conf > 0
                            ? 'text-amber-400'
                            : 'text-slate-500'
                        }`}
                      >
                        {conf > 0 ? `${conf.toFixed(0)}%` : '0%'}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-right space-x-1.5 whitespace-nowrap">
                      <button
                        onClick={() => onSelectRecord(r)}
                        className="p-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white transition"
                        title="View Audit Trail"
                      >
                        <Eye className="h-3.5 w-3.5" />
                      </button>
                      {canReconcile && (r.matchStatus === 'MANUAL_REVIEW' || r.matchStatus === 'NEEDS_REVIEW') && (
                        <button
                          onClick={() => setOverrideModalRecord(r)}
                          className="px-2 py-1 rounded-lg bg-blue-600/80 hover:bg-blue-600 text-white text-[11px] font-medium transition"
                          title="Manual Decision"
                        >
                          Resolve
                        </button>
                      )}
                    </td>
                  </tr>
                );
              })
            )}
          </tbody>
        </table>
      </div>

      {/* Manual Review Modal */}
      {overrideModalRecord && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm">
          <div className="bg-slate-900 border border-slate-700 rounded-xl p-6 max-w-md w-full shadow-2xl space-y-4">
            <h3 className="text-sm font-bold text-white flex items-center space-x-2">
              <UserCheck className="h-4 w-4 text-blue-400" />
              <span>Manual Review Override</span>
            </h3>
            <p className="text-xs text-slate-400 font-mono">
              Transaction: {overrideModalRecord.gatewayTxnId} (${overrideModalRecord.gatewayAmount})
            </p>
            <div>
              <label className="text-xs text-slate-300 block mb-1">Analyst Justification / Audit Note</label>
              <textarea
                value={overrideNote}
                onChange={(e) => setOverrideNote(e.target.value)}
                placeholder="Explain the manual matching decision for audit compliance..."
                className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2.5 text-xs text-white focus:outline-none focus:border-blue-500 h-24"
              />
            </div>
            <div className="flex justify-end space-x-2 pt-2">
              <button
                onClick={() => {
                  setOverrideModalRecord(null);
                  setOverrideNote('');
                }}
                className="px-3 py-1.5 rounded-lg bg-slate-800 text-slate-300 text-xs hover:bg-slate-700 transition"
              >
                Cancel
              </button>
              <button
                onClick={() => {
                  onManualReviewAction(overrideModalRecord, 'DISCREPANCY', overrideNote);
                  setOverrideModalRecord(null);
                  setOverrideNote('');
                }}
                className="px-3 py-1.5 rounded-lg bg-red-600 hover:bg-red-500 text-white text-xs font-medium transition"
              >
                Flag Discrepancy
              </button>
              <button
                onClick={() => {
                  onManualReviewAction(overrideModalRecord, 'MATCHED', overrideNote);
                  setOverrideModalRecord(null);
                  setOverrideNote('');
                }}
                className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium transition"
              >
                Approve as Matched
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
