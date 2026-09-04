import React from 'react';
import { BatchMetrics } from '../types';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Cell,
  PieChart,
  Pie,
  Legend,
} from 'recharts';

interface ChartsViewProps {
  metrics: BatchMetrics;
}

const COLORS = ['#3b82f6', '#8b5cf6', '#f59e0b', '#ef4444', '#10b981', '#06b6d4'];

export const ChartsView: React.FC<ChartsViewProps> = ({ metrics }) => {
  // Pass comparison data
  const passData = [
    { name: 'Pass 1 (Deterministic Rules)', count: metrics.pass1MatchedCount, color: '#3b82f6' },
    { name: 'Pass 2 (Groq AI)', count: metrics.pass2MatchedCount, color: '#8b5cf6' },
    { name: 'Manual Review Queue', count: metrics.manualReviewCount, color: '#f59e0b' },
  ];

  // Exception breakdown data
  const exceptionData = Object.entries(metrics.exceptionBreakdown || {}).map(([key, val]) => ({
    name: key.replace(/_/g, ' '),
    value: val,
  }));

  const CustomTooltip = ({ active, payload }: any) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-slate-900 border border-slate-700 p-2.5 rounded-lg shadow-xl text-xs font-mono">
          <p className="font-semibold text-slate-200">{payload[0].name}</p>
          <p className="text-blue-400">Count: {payload[0].value}</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
      {/* Chart 1: Two-Pass Reconciliation Distribution */}
      <div className="rounded-xl bg-slate-900/80 border border-slate-800 p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-sm font-semibold text-white">Reconciliation Breakdown by Engine Pass</h3>
            <p className="text-xs text-slate-400">Pass 1 deterministic rules vs Pass 2 Groq AI resolution</p>
          </div>
        </div>
        <div className="h-64 w-full">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={passData} layout="vertical" margin={{ top: 10, right: 30, left: 40, bottom: 5 }}>
              <XAxis type="number" stroke="#64748b" tick={{ fill: '#94a3b8', fontSize: 11 }} />
              <YAxis
                type="category"
                dataKey="name"
                stroke="#64748b"
                tick={{ fill: '#94a3b8', fontSize: 11 }}
                width={130}
              />
              <Tooltip content={<CustomTooltip />} />
              <Bar dataKey="count" radius={[0, 6, 6, 0]}>
                {passData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Chart 2: Exception Breakdown */}
      <div className="rounded-xl bg-slate-900/80 border border-slate-800 p-5 shadow-sm">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-sm font-semibold text-white">Exception & Discrepancy Taxonomy</h3>
            <p className="text-xs text-slate-400">Classified fee discrepancies, name variations, and settlement delays</p>
          </div>
        </div>
        <div className="h-64 w-full flex items-center justify-center">
          {exceptionData.length > 0 ? (
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={exceptionData}
                  cx="50%"
                  cy="50%"
                  innerRadius={55}
                  outerRadius={85}
                  paddingAngle={4}
                  dataKey="value"
                >
                  {exceptionData.map((_, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip content={<CustomTooltip />} />
                <Legend
                  verticalAlign="bottom"
                  height={36}
                  formatter={(value) => <span className="text-xs text-slate-300 font-sans">{value}</span>}
                />
              </PieChart>
            </ResponsiveContainer>
          ) : (
            <div className="text-center text-xs text-slate-500 font-mono">
              No unresolved exceptions in this batch.
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
