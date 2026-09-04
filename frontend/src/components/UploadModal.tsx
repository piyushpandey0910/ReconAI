import React, { useState } from 'react';
import { batchApi } from '../api/client';
import { X, Upload, AlertCircle, FileText, Loader2 } from 'lucide-react';

interface UploadModalProps {
  isOpen: boolean;
  onClose: () => void;
  onBatchCreated: (batchId: number) => void;
}

export const UploadModal: React.FC<UploadModalProps> = ({ isOpen, onClose, onBatchCreated }) => {
  const [batchName, setBatchName] = useState('');
  const [gatewayFile, setGatewayFile] = useState<File | null>(null);
  const [bankFile, setBankFile] = useState<File | null>(null);
  const [ledgerFile, setLedgerFile] = useState<File | null>(null);

  const [uploading, setUploading] = useState(false);
  const [stepStatus, setStepStatus] = useState<string>('');
  const [error, setError] = useState<string | null>(null);

  if (!isOpen) return null;

  const handleUpload = async () => {
    if (!gatewayFile || !bankFile || !ledgerFile) {
      setError('Please select all 3 CSV files (Gateway, Bank, and Ledger).');
      return;
    }

    setUploading(true);
    setError(null);

    try {
      setStepStatus('Creating reconciliation batch...');
      const batch = await batchApi.create(batchName || undefined);

      setStepStatus('Ingesting Payment Gateway Settlements CSV...');
      await batchApi.uploadCsv(batch.id, 'gateway', gatewayFile);

      setStepStatus('Ingesting Bank Statement CSV...');
      await batchApi.uploadCsv(batch.id, 'bank', bankFile);

      setStepStatus('Ingesting Internal Ledger CSV...');
      await batchApi.uploadCsv(batch.id, 'ledger', ledgerFile);

      setStepStatus('Completed!');
      onBatchCreated(batch.id);
      onClose();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Upload failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/70 backdrop-blur-sm animate-in fade-in duration-200">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl p-6 max-w-xl w-full shadow-2xl space-y-5">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center space-x-2">
            <div className="p-2 rounded-lg bg-blue-500/10 text-blue-400">
              <Upload className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-white">Ingest Reconciliation Datasets</h3>
              <p className="text-xs text-slate-400">Upload 3 multi-source CSV files for 3-way matching</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-white hover:bg-slate-800 transition"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Batch Name Input */}
        <div>
          <label className="text-xs font-semibold uppercase tracking-wider text-slate-400 block mb-1">
            Batch Name / Period (Optional)
          </label>
          <input
            type="text"
            value={batchName}
            onChange={(e) => setBatchName(e.target.value)}
            placeholder="e.g. August 2026 Monthly Reconciliation"
            className="w-full bg-slate-950 border border-slate-700 rounded-xl px-3.5 py-2 text-xs text-white focus:outline-none focus:border-blue-500"
          />
        </div>

        {/* 3 CSV Upload Dropzones */}
        <div className="space-y-3">
          {/* 1. Gateway */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <FileText className="w-5 h-5 text-blue-400" />
              <div>
                <div className="text-xs font-semibold text-slate-200">1. Payment Gateway Settlements</div>
                <div className="text-[11px] text-slate-500">
                  {gatewayFile ? gatewayFile.name : 'CSV (transactionId, amount, orderId, email)'}
                </div>
              </div>
            </div>
            <label className="cursor-pointer px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 transition">
              {gatewayFile ? 'Change' : 'Choose CSV'}
              <input
                type="file"
                accept=".csv"
                className="hidden"
                onChange={(e) => setGatewayFile(e.target.files?.[0] || null)}
              />
            </label>
          </div>

          {/* 2. Bank */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <FileText className="w-5 h-5 text-emerald-400" />
              <div>
                <div className="text-xs font-semibold text-slate-200">2. Bank Statement Feed</div>
                <div className="text-[11px] text-slate-500">
                  {bankFile ? bankFile.name : 'CSV (bankRefId, amount, type, narration, fee)'}
                </div>
              </div>
            </div>
            <label className="cursor-pointer px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 transition">
              {bankFile ? 'Change' : 'Choose CSV'}
              <input
                type="file"
                accept=".csv"
                className="hidden"
                onChange={(e) => setBankFile(e.target.files?.[0] || null)}
              />
            </label>
          </div>

          {/* 3. Ledger */}
          <div className="p-3.5 rounded-xl bg-slate-950 border border-slate-800 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <FileText className="w-5 h-5 text-purple-400" />
              <div>
                <div className="text-xs font-semibold text-slate-200">3. Internal General Ledger</div>
                <div className="text-[11px] text-slate-500">
                  {ledgerFile ? ledgerFile.name : 'CSV (ledgerEntryId, internalRef, amount, date)'}
                </div>
              </div>
            </div>
            <label className="cursor-pointer px-3 py-1.5 rounded-lg bg-slate-800 hover:bg-slate-700 text-xs text-slate-200 transition">
              {ledgerFile ? 'Change' : 'Choose CSV'}
              <input
                type="file"
                accept=".csv"
                className="hidden"
                onChange={(e) => setLedgerFile(e.target.files?.[0] || null)}
              />
            </label>
          </div>
        </div>

        {uploading && (
          <div className="p-3 rounded-lg bg-blue-500/10 border border-blue-500/20 text-xs text-blue-400 flex items-center space-x-2 font-mono">
            <Loader2 className="w-4 h-4 animate-spin" />
            <span>{stepStatus}</span>
          </div>
        )}

        {error && (
          <div className="p-3 rounded-lg bg-red-500/10 border border-red-500/20 text-xs text-red-400 flex items-center space-x-2">
            <AlertCircle className="w-4 h-4 shrink-0" />
            <span>{error}</span>
          </div>
        )}

        {/* Buttons */}
        <div className="flex items-center justify-between pt-2">
          <div className="text-[11px] text-slate-500">
            CSV files located in <code className="text-slate-400">data-generator/sample_data</code>
          </div>

          <div className="flex space-x-2">
            <button
              onClick={onClose}
              disabled={uploading}
              className="px-4 py-2 rounded-xl bg-slate-800 text-slate-300 text-xs hover:bg-slate-700 transition"
            >
              Cancel
            </button>
            <button
              onClick={handleUpload}
              disabled={uploading || !gatewayFile || !bankFile || !ledgerFile}
              className="px-4 py-2 rounded-xl bg-blue-600 hover:bg-blue-500 disabled:bg-slate-800 disabled:text-slate-600 text-white text-xs font-semibold transition active:scale-95 shadow-md shadow-blue-600/20"
            >
              {uploading ? 'Processing...' : 'Upload & Validate'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
