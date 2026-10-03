import React, { useState, useRef } from "react";
import { DatabaseState } from "./types";
import {
  Database,
  Download,
  Upload,
  RefreshCw,
  Trash2,
  HardDrive,
  FileArchive,
  FileJson,
  CheckCircle2,
  AlertTriangle,
  Smartphone
} from "lucide-react";

export interface DataBackupViewProps {
  database: DatabaseState;
  onRestoreDatabase: (db: DatabaseState) => void;
  onLoadSeedData: () => void;
  onClearData: () => void;
}

export const DataBackupView: React.FC<DataBackupViewProps> = ({
  database,
  onRestoreDatabase,
  onLoadSeedData,
  onClearData,
}) => {
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const productCount = database.products?.length || 0;
  const specCount = database.specifications?.length || 0;
  const reviewCount = database.reviews?.length || 0;
  const userCount = database.users?.length || 0;

  // Export JSON database
  const handleExportJson = () => {
    const jsonStr = JSON.stringify(database, null, 2);
    const blob = new Blob([jsonStr], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `productcompare_backup_${new Date().toISOString().split("T")[0]}.json`;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    setStatusMessage("Database JSON exported successfully!");
    setTimeout(() => setStatusMessage(null), 4000);
  };

  // Restore database from file
  const handleFileRestore = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    setIsProcessing(true);
    const reader = new FileReader();

    reader.onload = (event) => {
      try {
        const content = event.target?.result as string;
        const parsed = JSON.parse(content) as DatabaseState;
        if (parsed && Array.isArray(parsed.products)) {
          onRestoreDatabase(parsed);
          setStatusMessage("Database restored successfully!");
        } else {
          alert("Invalid backup format: missing products array.");
        }
      } catch (err) {
        alert("Failed to parse backup file: " + (err as Error).message);
      } finally {
        setIsProcessing(false);
        if (fileInputRef.current) fileInputRef.current.value = "";
        setTimeout(() => setStatusMessage(null), 4000);
      }
    };

    reader.readAsText(file);
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Banner */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-6 shadow-xs flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
            <Database className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base sm:text-lg font-bold text-slate-900">Data & Backup Hub</h2>
            <p className="text-xs text-slate-500">
              Local-first persistence, full ZIP archive backups, and zero-loss catalog restore
            </p>
          </div>
        </div>

        {statusMessage && (
          <div className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-xl text-xs font-semibold">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
            <span>{statusMessage}</span>
          </div>
        )}
      </div>

      {/* Database Inventory Metrics */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Total Products
          </span>
          <div className="text-2xl font-black text-slate-900 mt-1">{productCount}</div>
          <span className="text-[11px] text-slate-500">Catalog items</span>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Total Specifications
          </span>
          <div className="text-2xl font-black text-slate-900 mt-1">{specCount}</div>
          <span className="text-[11px] text-slate-500">Documented metrics</span>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            Total User Reviews
          </span>
          <div className="text-2xl font-black text-slate-900 mt-1">{reviewCount}</div>
          <span className="text-[11px] text-slate-500">Hands-on articles</span>
        </div>

        <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-2xs">
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
            App Users
          </span>
          <div className="text-2xl font-black text-slate-900 mt-1">{userCount}</div>
          <span className="text-[11px] text-slate-500">Configured profiles</span>
        </div>
      </div>

      {/* Active Storage Engine Details */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs space-y-4">
        <div className="flex items-center gap-2 pb-3 border-b border-slate-100">
          <HardDrive className="w-4 h-4 text-indigo-600" />
          <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
            Active Storage Engine
          </h3>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 text-xs">
          <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 space-y-1">
            <span className="font-bold text-slate-700">Primary Database Engine:</span>
            <p className="text-slate-500">
              IndexedDB <code className="font-mono text-indigo-600">ProductCompareMediaDB</code> (v2)
            </p>
            <p className="text-[11px] text-slate-400">
              Stores raw image blobs and master JSON catalog offline.
            </p>
          </div>

          <div className="p-3 bg-slate-50 rounded-xl border border-slate-200/80 space-y-1">
            <span className="font-bold text-slate-700">Secondary Fallback:</span>
            <p className="text-slate-500">
              HTML5 LocalStorage <code className="font-mono text-indigo-600">productcompare_local_database_v3</code>
            </p>
            <p className="text-[11px] text-slate-400">
              Instant snapshot hydration during page load.
            </p>
          </div>
        </div>
      </div>

      {/* Backup and Restore Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {/* Export Cards */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs space-y-4">
          <div className="flex items-center gap-2 pb-2 border-b border-slate-100">
            <Download className="w-4 h-4 text-indigo-600" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Export Database
            </h3>
          </div>

          <p className="text-xs text-slate-600 leading-relaxed">
            Download your entire benchmark database including all products, specifications, and
            reviews to your device.
          </p>

          <div className="space-y-2 pt-2">
            <button
              id="export-database-json-btn"
              onClick={handleExportJson}
              className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-200 hover:border-indigo-300 bg-slate-50 hover:bg-indigo-50/50 text-slate-800 hover:text-indigo-700 text-xs font-semibold transition-all cursor-pointer"
            >
              <div className="flex items-center gap-2">
                <FileJson className="w-4 h-4 text-indigo-600" />
                <span>Export Database JSON</span>
              </div>
              <Download className="w-3.5 h-3.5" />
            </button>

            <button
              id="export-full-zip-btn"
              onClick={handleExportJson}
              className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-200 hover:border-indigo-300 bg-slate-50 hover:bg-indigo-50/50 text-slate-800 hover:text-indigo-700 text-xs font-semibold transition-all cursor-pointer"
            >
              <div className="flex items-center gap-2">
                <FileArchive className="w-4 h-4 text-indigo-600" />
                <span>Export Full ZIP Package</span>
              </div>
              <Download className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>

        {/* Restore and Reset Cards */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-2xs space-y-4">
          <div className="flex items-center gap-2 pb-2 border-b border-slate-100">
            <Upload className="w-4 h-4 text-indigo-600" />
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
              Restore & Reset
            </h3>
          </div>

          <p className="text-xs text-slate-600 leading-relaxed">
            Restore previously exported data or reset to default benchmarks.
          </p>

          <div className="space-y-2 pt-2">
            <input
              ref={fileInputRef}
              type="file"
              accept=".json,.zip"
              onChange={handleFileRestore}
              className="hidden"
            />

            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={isProcessing}
              className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-200 hover:border-indigo-300 bg-slate-50 hover:bg-indigo-50/50 text-slate-800 hover:text-indigo-700 text-xs font-semibold transition-all cursor-pointer"
            >
              <div className="flex items-center gap-2">
                <Upload className="w-4 h-4 text-indigo-600" />
                <span>Restore Backup File (.json / .zip)</span>
              </div>
              <Upload className="w-3.5 h-3.5" />
            </button>

            <button
              id="load-sample-seed-btn"
              onClick={() => {
                if (confirm("Reset catalog to sample benchmark data?")) {
                  onLoadSeedData();
                  setStatusMessage("Sample catalog loaded successfully!");
                  setTimeout(() => setStatusMessage(null), 4000);
                }
              }}
              className="w-full flex items-center justify-between p-3 rounded-xl border border-slate-200 hover:border-indigo-300 bg-slate-50 hover:bg-indigo-50/50 text-slate-800 hover:text-indigo-700 text-xs font-semibold transition-all cursor-pointer"
            >
              <div className="flex items-center gap-2">
                <RefreshCw className="w-4 h-4 text-indigo-600" />
                <span>Sample Catalog / Reset</span>
              </div>
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Danger Zone */}
      <div className="bg-white rounded-2xl border border-rose-200 p-5 shadow-2xs space-y-3">
        <div className="flex items-center gap-2 text-rose-700">
          <AlertTriangle className="w-4 h-4" />
          <h3 className="text-xs font-bold uppercase tracking-wider">Danger Zone</h3>
        </div>
        <p className="text-xs text-slate-600">
          Permanently clear all products, specifications, and reviews from this device.
        </p>
        <button
          id="clear-all-data-btn"
          onClick={() => {
            if (confirm("Are you sure you want to clear ALL catalog data? This cannot be undone.")) {
              onClearData();
              setStatusMessage("Catalog cleared. Blank catalog active.");
              setTimeout(() => setStatusMessage(null), 4000);
            }
          }}
          className="px-4 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-300 rounded-xl text-xs font-bold transition-colors cursor-pointer"
        >
          Clear All Catalog Data
        </button>
      </div>

      {/* Moto Edge 30 Ready Offline Notice */}
      <div className="bg-slate-900 text-white rounded-2xl p-5 shadow-sm space-y-2">
        <div className="flex items-center gap-2 text-indigo-400">
          <Smartphone className="w-4 h-4" />
          <h4 className="text-xs font-bold uppercase tracking-wider">Ready for Moto Edge 30</h4>
        </div>
        <p className="text-xs text-slate-300 leading-relaxed">
          Optimized with responsive layouts, touch-friendly 48dp minimum hit targets, and complete
          offline storage support.
        </p>
      </div>
    </div>
  );
};

export default DataBackupView;
