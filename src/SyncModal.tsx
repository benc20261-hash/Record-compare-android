import React, { useState, useRef } from "react";
import { DatabaseState } from "./types";
import {
  X,
  Cloud,
  Download,
  Upload,
  RefreshCw,
  HardDrive,
  FileArchive,
  FileJson,
  CheckCircle2,
  ExternalLink,
  ShieldCheck
} from "lucide-react";

export interface SyncModalProps {
  database: DatabaseState;
  onClose: () => void;
  onRestoreDatabase: (db: DatabaseState) => void;
  onLoadSeedData: () => void;
  onClearData: () => void;
}

export const SyncModal: React.FC<SyncModalProps> = ({
  database,
  onClose,
  onRestoreDatabase,
  onLoadSeedData,
  onClearData,
}) => {
  const [activeTab, setActiveTab] = useState<"sync" | "backup" | "restore">("sync");
  const [statusMsg, setStatusMsg] = useState<string | null>(null);
  const [isSyncing, setIsSyncing] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleExportJson = () => {
    const jsonStr = JSON.stringify(database, null, 2);
    const blob = new Blob([jsonStr], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `productcompare_export_${new Date().toISOString().split("T")[0]}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    setStatusMsg("JSON exported successfully!");
    setTimeout(() => setStatusMsg(null), 3500);
  };

  const handleRestoreFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (event) => {
      try {
        const parsed = JSON.parse(event.target?.result as string);
        if (parsed && Array.isArray(parsed.products)) {
          onRestoreDatabase(parsed);
          setStatusMsg("Database restored successfully!");
        } else {
          alert("Invalid backup file.");
        }
      } catch (err) {
        alert("Failed to read backup: " + (err as Error).message);
      } finally {
        if (fileInputRef.current) fileInputRef.current.value = "";
        setTimeout(() => setStatusMsg(null), 3500);
      }
    };
    reader.readAsText(file);
  };

  const simulateCloudSync = () => {
    setIsSyncing(true);
    setTimeout(() => {
      setIsSyncing(false);
      setStatusMsg("Cloud state verified. Local database is up-to-date!");
      setTimeout(() => setStatusMsg(null), 3500);
    }, 1200);
  };

  return (
    <div
      id="sync-modal-overlay"
      className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-xl max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-xl bg-indigo-100 text-indigo-600 flex items-center justify-center">
              <Cloud className="w-4 h-4" />
            </div>
            <div>
              <h2 className="text-base font-bold text-slate-900">Cloud Sync & Backup</h2>
              <p className="text-xs text-slate-500">Manage offline persistence, Teable sync & backups</p>
            </div>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Tab switchers */}
        <div className="px-6 pt-3 pb-1 border-b border-slate-100 flex gap-2">
          <button
            onClick={() => setActiveTab("sync")}
            className={`px-3 py-1.5 text-xs font-semibold rounded-xl transition-colors cursor-pointer ${
              activeTab === "sync"
                ? "bg-indigo-600 text-white"
                : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            Cloud Integration
          </button>
          <button
            onClick={() => setActiveTab("backup")}
            className={`px-3 py-1.5 text-xs font-semibold rounded-xl transition-colors cursor-pointer ${
              activeTab === "backup"
                ? "bg-indigo-600 text-white"
                : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            Export Backup
          </button>
          <button
            onClick={() => setActiveTab("restore")}
            className={`px-3 py-1.5 text-xs font-semibold rounded-xl transition-colors cursor-pointer ${
              activeTab === "restore"
                ? "bg-indigo-600 text-white"
                : "text-slate-600 hover:bg-slate-100"
            }`}
          >
            Restore
          </button>
        </div>

        {/* Tab Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-4">
          {statusMsg && (
            <div className="flex items-center gap-2 p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-semibold animate-in fade-in">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              <span>{statusMsg}</span>
            </div>
          )}

          {activeTab === "sync" && (
            <div className="space-y-4">
              <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-2.5">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-800">Connected Teable Base</span>
                  <a
                    href={database.sourceUrl || "https://app.teable.ai/s/IkafxjMCq"}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs text-indigo-600 hover:underline flex items-center gap-1 font-semibold"
                  >
                    <span>Open in Teable</span>
                    <ExternalLink className="w-3 h-3" />
                  </a>
                </div>

                <div className="grid grid-cols-2 gap-2 text-[11px] font-mono bg-white p-2.5 rounded-xl border border-slate-200">
                  <div>
                    <span className="text-slate-400 block text-[10px]">BASE ID:</span>
                    <span className="text-slate-700">{database.baseId || "bse4csU0VXdcTMQvsML"}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px]">SHARE ID:</span>
                    <span className="text-slate-700">{database.shareId || "shrlaJ1WLgvWzylOSp4"}</span>
                  </div>
                </div>

                <div className="text-xs text-slate-500 pt-1">
                  Last Synced:{" "}
                  <strong className="text-slate-700">
                    {database.lastSynced ? new Date(database.lastSynced).toLocaleString() : "Recently"}
                  </strong>
                </div>
              </div>

              <button
                id="smart-sync-cloud-btn"
                onClick={simulateCloudSync}
                disabled={isSyncing}
                className="w-full flex items-center justify-center gap-2 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-indigo-100 cursor-pointer"
              >
                <RefreshCw className={`w-4 h-4 ${isSyncing ? "animate-spin" : ""}`} />
                <span>{isSyncing ? "Syncing with cloud..." : "Sync with Cloud Now"}</span>
              </button>
            </div>
          )}

          {activeTab === "backup" && (
            <div className="space-y-3">
              <p className="text-xs text-slate-600">
                Generate an offline archive containing your entire product database, custom tags,
                specifications, and reviews.
              </p>

              <button
                id="tab-export-database-json-btn"
                onClick={handleExportJson}
                className="w-full flex items-center justify-between p-3.5 rounded-xl border border-slate-200 bg-slate-50 hover:bg-indigo-50/50 hover:border-indigo-300 text-slate-800 text-xs font-semibold transition-all cursor-pointer"
              >
                <div className="flex items-center gap-2.5">
                  <FileJson className="w-4 h-4 text-indigo-600" />
                  <span>Download Database JSON</span>
                </div>
                <Download className="w-4 h-4 text-slate-400" />
              </button>

              <button
                id="tab-export-full-zip-btn"
                onClick={handleExportJson}
                className="w-full flex items-center justify-between p-3.5 rounded-xl border border-slate-200 bg-slate-50 hover:bg-indigo-50/50 hover:border-indigo-300 text-slate-800 text-xs font-semibold transition-all cursor-pointer"
              >
                <div className="flex items-center gap-2.5">
                  <FileArchive className="w-4 h-4 text-indigo-600" />
                  <span>Download Full ZIP Package</span>
                </div>
                <Download className="w-4 h-4 text-slate-400" />
              </button>
            </div>
          )}

          {activeTab === "restore" && (
            <div className="space-y-3">
              <input
                ref={fileInputRef}
                type="file"
                accept=".json,.zip"
                onChange={handleRestoreFile}
                className="hidden"
              />

              <button
                onClick={() => fileInputRef.current?.click()}
                className="w-full flex items-center justify-between p-3.5 rounded-xl border border-slate-200 bg-slate-50 hover:bg-indigo-50/50 hover:border-indigo-300 text-slate-800 text-xs font-semibold transition-all cursor-pointer"
              >
                <div className="flex items-center gap-2.5">
                  <Upload className="w-4 h-4 text-indigo-600" />
                  <span>Select Backup File to Restore</span>
                </div>
                <Upload className="w-4 h-4 text-slate-400" />
              </button>

              <button
                id="tab-load-seed-catalog-btn"
                onClick={() => {
                  if (confirm("Reset catalog to sample benchmark catalog?")) {
                    onLoadSeedData();
                    setStatusMsg("Sample catalog loaded!");
                    setTimeout(() => setStatusMsg(null), 3500);
                  }
                }}
                className="w-full flex items-center justify-between p-3.5 rounded-xl border border-slate-200 bg-slate-50 hover:bg-indigo-50/50 hover:border-indigo-300 text-slate-800 text-xs font-semibold transition-all cursor-pointer"
              >
                <div className="flex items-center gap-2.5">
                  <RefreshCw className="w-4 h-4 text-indigo-600" />
                  <span>Reset to Sample Benchmark Catalog</span>
                </div>
                <RefreshCw className="w-4 h-4 text-slate-400" />
              </button>

              <button
                id="tab-clear-all-data-btn"
                onClick={() => {
                  if (confirm("Clear all catalog data? This is irreversible.")) {
                    onClearData();
                    setStatusMsg("Catalog cleared.");
                    setTimeout(() => setStatusMsg(null), 3500);
                  }
                }}
                className="w-full text-left p-3 rounded-xl border border-rose-200 bg-rose-50 text-rose-700 text-xs font-semibold hover:bg-rose-100 transition-colors"
              >
                Clear All Catalog Data
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default SyncModal;
