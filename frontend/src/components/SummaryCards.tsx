import React from 'react';
import { BatchMetrics } from '../types';
import { CheckCircle2, AlertTriangle, HelpCircle, DollarSign, Layers } from 'lucide-react';

interface SummaryCardsProps {
  metrics: BatchMetrics | null;
  loading: boolean;
}

export const SummaryCards: React.FC<SummaryCardsProps> = ({ metrics, loading }) => {
  if (loading || !metrics) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        {[1, 2, 3, 4, 5].map((i) => (
          <div key={i} className="h-28 rounded-xl bg-slate-900/60 border border-slate-800 animate-pulse" />
        ))}
      </div>
    );
  }

  const formatCurrency = (val?: number) => {
    return new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR' }).format(val || 0);
  };

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
      {/* Total Processed */}
      <div className="rounded-xl bg-slate-900/80 border border-slate-800 p-4 relative overflow-hidden group hover:border-slate-700 transition shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Total Transactions</span>
          <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400">
            <Layers className="h-4 w-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline space-x-2">
          <span className="text-2xl font-bold font-mono text-white">{metrics.totalRecords}</span>
          <span className="text-xs text-slate-500">records</span>
        </div>
        <div className="mt-2 text-xs text-slate-400 flex items-center space-x-1">
          <span className="text-blue-400 font-semibold">{metrics.matchedCount}</span>
          <span>reconciled</span>
        </div>
      </div>

      {/* Match Rate */}
      <div className="rounded-xl bg-slate-900/80 border border-slate-800 p-4 relative overflow-hidden group hover:border-emerald-500/40 transition shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Match Rate</span>
          <div className="p-2 rounded-lg bg-emerald-500/10 text-emerald-400">
            <CheckCircle2 className="h-4 w-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline space-x-2">
          <span className="text-2xl font-bold font-mono text-emerald-400">
            {metrics.matchRatePercentage.toFixed(1)}%
          </span>
        </div>
        <div className="mt-2 text-xs text-slate-400">
          Pass 1: <span className="text-slate-300 font-mono font-medium">{metrics.pass1MatchedCount}</span> | Pass 2: <span className="text-purple-400 font-mono font-medium">{metrics.pass2MatchedCount}</span>
        </div>
      </div>

      {/* Reconciled Volume */}
      <div className="rounded-xl bg-slate-900/80 border border-slate-800 p-4 relative overflow-hidden group hover:border-blue-500/40 transition shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Reconciled Amount</span>
          <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400">
            <DollarSign className="h-4 w-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline space-x-2">
          <span className="text-xl font-bold font-mono text-white">
            {formatCurrency(metrics.totalReconciledAmount)}
          </span>
        </div>
        <div className="mt-2 text-xs text-emerald-400 flex items-center space-x-1">
          <span>Settled & matched</span>
        </div>
      </div>

      {/* Manual Review Queue */}
      <div className="rounded-xl bg-slate-900/80 border border-slate-800 p-4 relative overflow-hidden group hover:border-amber-500/40 transition shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Manual Review Queue</span>
          <div className="p-2 rounded-lg bg-amber-500/10 text-amber-400">
            <HelpCircle className="h-4 w-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline space-x-2">
          <span className="text-2xl font-bold font-mono text-amber-400">
            {metrics.manualReviewCount}
          </span>
          <span className="text-xs text-slate-500">pending</span>
        </div>
        <div className="mt-2 text-xs text-slate-400">
          Confidence &lt; 0.70 guardrail
        </div>
      </div>

      {/* Discrepancy Amount */}
      <div className="rounded-xl bg-slate-900/80 border border-slate-800 p-4 relative overflow-hidden group hover:border-red-500/40 transition shadow-sm">
        <div className="flex items-center justify-between">
          <span className="text-xs font-medium text-slate-400">Unresolved Discrepancies</span>
          <div className="p-2 rounded-lg bg-red-500/10 text-red-400">
            <AlertTriangle className="h-4 w-4" />
          </div>
        </div>
        <div className="mt-2 flex items-baseline space-x-2">
          <span className="text-xl font-bold font-mono text-red-400">
            {formatCurrency(metrics.totalDiscrepancyAmount)}
          </span>
        </div>
        <div className="mt-2 text-xs text-slate-400">
          Fees / anomalies variance
        </div>
      </div>
    </div>
  );
};
