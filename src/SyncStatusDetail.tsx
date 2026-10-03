import React from "react";
import { CheckCircle2, Clock, Cloud, RefreshCw, AlertCircle } from "lucide-react";

export interface SyncStatusDetailProps {
  lastSynced?: string;
  lastPushed?: string;
  isSyncing?: boolean;
  sourceUrl?: string;
  error?: string | null;
  onRefresh?: () => void;
}

export const SyncStatusDetail: React.FC<SyncStatusDetailProps> = ({
  lastSynced,
  lastPushed,
  isSyncing = false,
  sourceUrl,
  error,
  onRefresh,
}) => {
  const formatTime = (timeStr?: string) => {
    if (!timeStr) return "Never";
    try {
      const d = new Date(timeStr);
      return d.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit", second: "2-digit" });
    } catch {
      return "Unknown";
    }
  };

  const formatDate = (timeStr?: string) => {
    if (!timeStr) return "";
    try {
      const d = new Date(timeStr);
      return d.toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" });
    } catch {
      return "";
    }
  };

  return (
    <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-3">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="w-8 h-8 rounded-lg bg-indigo-100 text-indigo-600 flex items-center justify-center">
            <Cloud className="w-4 h-4" />
          </div>
          <div>
            <h4 className="text-xs font-bold text-slate-900">Sync Status</h4>
            <p className="text-[11px] text-slate-500">
              {sourceUrl ? `Connected to ${sourceUrl}` : "Local Storage Active"}
            </p>
          </div>
        </div>

        {onRefresh && (
          <button
            onClick={onRefresh}
            disabled={isSyncing}
            className="p-1.5 rounded-lg text-slate-500 hover:text-indigo-600 hover:bg-slate-200 transition-colors disabled:opacity-50"
            title="Refresh sync status"
          >
            <RefreshCw className={`w-4 h-4 ${isSyncing ? "animate-spin text-indigo-600" : ""}`} />
          </button>
        )}
      </div>

      {error && (
        <div className="flex items-center gap-1.5 text-xs text-rose-600 bg-rose-50 border border-rose-200 px-3 py-2 rounded-xl">
          <AlertCircle className="w-3.5 h-3.5 shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <div className="grid grid-cols-2 gap-3 pt-1 border-t border-slate-200/60">
        <div>
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-0.5">
            Last Pull
          </span>
          <div className="flex items-center gap-1 text-xs font-semibold text-slate-800">
            <Clock className="w-3.5 h-3.5 text-slate-400" />
            <span>{formatTime(lastSynced)}</span>
          </div>
          {lastSynced && <span className="text-[10px] text-slate-400 block">{formatDate(lastSynced)}</span>}
        </div>

        <div>
          <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-0.5">
            Last Push
          </span>
          <div className="flex items-center gap-1 text-xs font-semibold text-slate-800">
            <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500" />
            <span>{formatTime(lastPushed)}</span>
          </div>
          {lastPushed && <span className="text-[10px] text-slate-400 block">{formatDate(lastPushed)}</span>}
        </div>
      </div>
    </div>
  );
};

export default SyncStatusDetail;
