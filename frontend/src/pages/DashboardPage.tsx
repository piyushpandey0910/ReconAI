import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Batch, BatchMetrics, MatchResult } from '../types';
import { batchApi, reconciliationApi, metricsApi } from '../api/client';
import { Navbar } from '../components/Navbar';
import { SummaryCards } from '../components/SummaryCards';
import { ChartsView } from '../components/ChartsView';
import { RecordsTable } from '../components/RecordsTable';
import { AuditModal } from '../components/AuditModal';
import { UploadModal } from '../components/UploadModal';
import { ChatDrawer } from '../components/ChatDrawer';
import { Play, Cpu, CheckCircle2, AlertTriangle, Layers, FileSpreadsheet } from 'lucide-react';

export const DashboardPage: React.FC = () => {
  const { canReconcile } = useAuth();

  const [batches, setBatches] = useState<Batch[]>([]);
  const [selectedBatchId, setSelectedBatchId] = useState<number | null>(null);
  const [activeBatch, setActiveBatch] = useState<Batch | null>(null);
  const [metrics, setMetrics] = useState<BatchMetrics | null>(null);
  const [records, setRecords] = useState<MatchResult[]>([]);

  const [selectedStatus, setSelectedStatus] = useState<string>('ALL');
  const [searchQuery, setSearchQuery] = useState<string>('');

  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const [isUploadOpen, setIsUploadOpen] = useState<boolean>(false);
  const [isChatOpen, setIsChatOpen] = useState<boolean>(false);
  const [auditRecord, setAuditRecord] = useState<MatchResult | null>(null);

  useEffect(() => {
    loadBatches();
  }, []);

  useEffect(() => {
    if (selectedBatchId) {
      loadBatchData(selectedBatchId);
    }
  }, [selectedBatchId, selectedStatus, searchQuery]);

  const loadBatches = async () => {
    try {
      const list = await batchApi.list();
      setBatches(list);
      if (list.length > 0 && !selectedBatchId) {
        setSelectedBatchId(list[0].id);
      } else if (list.length === 0) {
        setLoading(false);
      }
    } catch (err: any) {
      console.error('Failed to load batches:', err);
      setLoading(false);
    }
  };

  const loadBatchData = async (batchId: number) => {
    setLoading(true);
    try {
      const [batchData, metricsData, recordsData] = await Promise.all([
        batchApi.get(batchId),
        metricsApi.getMetrics(batchId),
        reconciliationApi.getRecords(batchId, selectedStatus, searchQuery),
      ]);
      setActiveBatch(batchData);
      setMetrics(metricsData);
      setRecords(recordsData);
    } catch (err: any) {
      console.error('Failed to load batch details:', err);
    } finally {
      setLoading(false);
    }
  };

  const handleRunPass1 = async () => {
    if (!selectedBatchId) return;
    setActionLoading(true);
    setNotification(null);
    try {
      const res = await reconciliationApi.runRulesPass(selectedBatchId);
      setNotification({
        type: 'success',
        message: `Pass 1 Rules Completed: ${res.matchedCount} records matched, ${res.needsReviewCount} routed to Pass 2.`,
      });
      await loadBatchData(selectedBatchId);
      loadBatches();
    } catch (err: any) {
      setNotification({
        type: 'error',
        message: err.response?.data?.message || 'Pass 1 execution failed',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleRunPass2 = async () => {
    if (!selectedBatchId) return;
    setActionLoading(true);
    setNotification(null);
    try {
      const res = await reconciliationApi.runAiPass(selectedBatchId);
      setNotification({
        type: 'success',
        message: `Pass 2 AI Classification Completed: ${res.aiMatchedCount} resolved by Groq AI, ${res.manualReviewCount} flagged for manual review.`,
      });
      await loadBatchData(selectedBatchId);
      loadBatches();
    } catch (err: any) {
      setNotification({
        type: 'error',
        message: err.response?.data?.message || 'Pass 2 AI execution failed',
      });
    } finally {
      setActionLoading(false);
    }
  };

  const handleManualReviewAction = async (record: MatchResult, status: 'MATCHED' | 'DISCREPANCY', note: string) => {
    if (!selectedBatchId) return;
    try {
      await reconciliationApi.overrideManualReview(selectedBatchId, record.id, status, note);
      setNotification({
        type: 'success',
        message: `Record ${record.gatewayTxnId || record.id} marked as ${status}.`,
      });
      await loadBatchData(selectedBatchId);
    } catch (err: any) {
      setNotification({
        type: 'error',
        message: err.response?.data?.message || 'Failed to update manual review status',
      });
    }
  };

  const handleDownloadReport = () => {
    if (!selectedBatchId || !activeBatch) return;
    metricsApi.downloadReport(selectedBatchId, activeBatch.batchName);
  };

  return (
    <div className="min-h-screen bg-slate-950 flex flex-col text-slate-100">
      <Navbar
        onOpenUpload={() => setIsUploadOpen(true)}
        onToggleChat={() => setIsChatOpen((prev) => !prev)}
        isChatOpen={isChatOpen}
      />

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 lg:px-8 py-6 space-y-6">
        {/* Batch Selector & Action Toolbar */}
        <div className="bg-slate-900/80 border border-slate-800 rounded-2xl p-4 sm:p-5 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-sm">
          {/* Batch Selector */}
          <div className="flex items-center space-x-3">
            <div className="p-2 rounded-xl bg-slate-800 text-slate-300">
              <Layers className="h-5 w-5" />
            </div>
            <div>
              <label className="text-[11px] font-semibold text-slate-400 uppercase tracking-wider block">
                Active Reconciliation Batch
              </label>
              {batches.length > 0 ? (
                <select
                  value={selectedBatchId || ''}
                  onChange={(e) => setSelectedBatchId(Number(e.target.value))}
                  className="bg-slate-950 border border-slate-700 rounded-lg px-3 py-1.5 text-xs font-semibold text-white focus:outline-none focus:border-blue-500 mt-1"
                >
                  {batches.map((b) => (
                    <option key={b.id} value={b.id}>
                      {b.batchName} ({b.totalRecords} txns) — {b.status}
                    </option>
                  ))}
                </select>
              ) : (
                <span className="text-xs text-slate-400">No batches uploaded yet. Ingest a dataset to start.</span>
              )}
            </div>
          </div>

          {/* Engine Execution Actions */}
          {activeBatch && (
            <div className="flex flex-wrap items-center gap-2">
              {canReconcile && (
                <>
                  <button
                    onClick={handleRunPass1}
                    disabled={actionLoading}
                    className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:bg-slate-800 text-white text-xs font-semibold transition active:scale-95 shadow-md shadow-blue-600/20"
                  >
                    <Play className="h-3.5 w-3.5 fill-current" />
                    <span>Run Pass 1 (Rules)</span>
                  </button>

                  <button
                    onClick={handleRunPass2}
                    disabled={actionLoading || activeBatch.status === 'PENDING_FILES'}
                    className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl bg-purple-600 hover:bg-purple-500 disabled:bg-slate-800 text-white text-xs font-semibold transition active:scale-95 shadow-md shadow-purple-600/20"
                  >
                    <Cpu className="h-3.5 w-3.5" />
                    <span>Run Pass 2 (Groq AI)</span>
                  </button>
                </>
              )}

              <button
                onClick={handleDownloadReport}
                className="inline-flex items-center space-x-1.5 px-3.5 py-2 rounded-xl bg-emerald-700 hover:bg-emerald-600 text-white text-xs font-semibold transition active:scale-95 shadow-md shadow-emerald-700/20"
              >
                <FileSpreadsheet className="h-3.5 w-3.5" />
                <span>Download Report</span>
              </button>
            </div>
          )}
        </div>

        {/* Live Notification Feedback */}
        {notification && (
          <div
            className={`p-3.5 rounded-xl border text-xs flex items-center justify-between font-mono animate-in fade-in duration-200 ${
              notification.type === 'success'
                ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300'
                : 'bg-red-500/10 border-red-500/30 text-red-300'
            }`}
          >
            <div className="flex items-center space-x-2">
              {notification.type === 'success' ? (
                <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
              ) : (
                <AlertTriangle className="h-4 w-4 shrink-0 text-red-400" />
              )}
              <span>{notification.message}</span>
            </div>
            <button
              onClick={() => setNotification(null)}
              className="text-slate-400 hover:text-white text-xs ml-4"
            >
              Dismiss
            </button>
          </div>
        )}

        {/* Empty State / Prompt to Upload */}
        {batches.length === 0 && !loading && (
          <div className="rounded-2xl border border-dashed border-slate-800 bg-slate-900/40 p-12 text-center space-y-4">
            <div className="h-14 w-14 mx-auto rounded-2xl bg-blue-500/10 flex items-center justify-center text-blue-400">
              <Layers className="h-7 w-7" />
            </div>
            <h3 className="text-base font-bold text-white">No Reconciliation Batches Found</h3>
            <p className="text-xs text-slate-400 max-w-md mx-auto leading-relaxed">
              Upload your Gateway Settlements, Bank Statement, and Ledger CSVs to start the two-pass reconciliation pipeline.
            </p>
            {canReconcile && (
              <button
                onClick={() => setIsUploadOpen(true)}
                className="inline-flex items-center space-x-2 px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 text-white text-xs font-semibold transition"
              >
                <span>Upload Sample Datasets</span>
              </button>
            )}
          </div>
        )}

        {/* Core Dashboard: KPIs + Charts + Table */}
        {activeBatch && (
          <>
            {/* KPI Cards */}
            <SummaryCards metrics={metrics} loading={loading} />

            {/* Recharts Visualizations */}
            {metrics && metrics.totalRecords > 0 && <ChartsView metrics={metrics} />}

            {/* Records Table & Audit Drill-Down */}
            <RecordsTable
              records={records}
              selectedStatus={selectedStatus}
              onStatusChange={setSelectedStatus}
              searchQuery={searchQuery}
              onSearchChange={setSearchQuery}
              onSelectRecord={setAuditRecord}
              onManualReviewAction={handleManualReviewAction}
              onDownloadReport={handleDownloadReport}
              loading={loading}
            />
          </>
        )}
      </main>

      {/* Audit Modal */}
      <AuditModal record={auditRecord} onClose={() => setAuditRecord(null)} />

      {/* Upload Modal */}
      <UploadModal
        isOpen={isUploadOpen}
        onClose={() => setIsUploadOpen(false)}
        onBatchCreated={(batchId) => {
          setSelectedBatchId(batchId);
          loadBatches();
        }}
      />

      {/* Chat Agent Drawer */}
      <ChatDrawer
        batchId={selectedBatchId}
        batchName={activeBatch ? activeBatch.batchName : 'Reconciliation Batch'}
        isOpen={isChatOpen}
        onClose={() => setIsChatOpen(false)}
      />
    </div>
  );
};
