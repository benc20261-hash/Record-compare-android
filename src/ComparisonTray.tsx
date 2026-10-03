import React from "react";
import { Product } from "./types";
import { getProductPhotoFallback } from "./imageFallback";
import { X, Trash2, Sparkles, ArrowRight } from "lucide-react";

export interface ComparisonTrayProps {
  selectedProducts: Product[];
  onRemoveProduct: (id: string) => void;
  onClearAll: () => void;
  onCompareNow: () => void;
  onOpenSmartAdd?: () => void;
}

export const ComparisonTray: React.FC<ComparisonTrayProps> = ({
  selectedProducts,
  onRemoveProduct,
  onClearAll,
  onCompareNow,
  onOpenSmartAdd,
}) => {
  if (selectedProducts.length === 0) return null;

  return (
    <div
      id="comparison-tray"
      className="fixed bottom-4 sm:bottom-6 left-1/2 -translate-x-1/2 z-40 bg-slate-900/95 backdrop-blur-md text-white px-3 sm:px-4 py-2.5 rounded-2xl shadow-2xl border border-slate-700/80 flex items-center justify-between gap-3 max-w-[95vw] sm:max-w-2xl w-full animate-in slide-in-from-bottom-6 duration-200"
    >
      <div className="flex items-center gap-2 overflow-x-auto no-scrollbar py-0.5 min-w-0 flex-1">
        <span className="text-xs font-bold text-slate-400 uppercase tracking-wider shrink-0 mr-1 hidden sm:inline">
          Compared ({selectedProducts.length})
        </span>

        {selectedProducts.map((p) => {
          const imgSrc =
            p.pictures && p.pictures[0]?.url
              ? p.pictures[0].url
              : getProductPhotoFallback(p.name, p.brand, p.tags);

          return (
            <div
              key={p.id}
              className="flex items-center gap-2 bg-slate-800/90 hover:bg-slate-800 border border-slate-700/80 rounded-xl px-2 py-1 shrink-0 transition-colors"
            >
              <img
                src={imgSrc}
                alt={p.name}
                onError={(e) => {
                  (e.target as HTMLImageElement).src = getProductPhotoFallback(p.name, p.brand, p.tags);
                }}
                className="w-7 h-7 rounded-lg object-cover bg-slate-700"
              />
              <span className="text-xs font-medium text-slate-200 max-w-[90px] sm:max-w-[120px] truncate">
                {p.name}
              </span>
              <button
                id={`tray-remove-${p.id}`}
                onClick={() => onRemoveProduct(p.id)}
                className="text-slate-400 hover:text-white p-0.5 rounded-full hover:bg-slate-700 transition-colors cursor-pointer"
                title="Remove"
              >
                <X className="w-3 h-3" />
              </button>
            </div>
          );
        })}
      </div>

      <div className="flex items-center gap-2 shrink-0">
        <button
          id="tray-clear-all-btn"
          onClick={onClearAll}
          className="p-2 text-slate-400 hover:text-rose-400 rounded-lg hover:bg-slate-800 text-xs font-medium transition-colors cursor-pointer"
          title="Clear all selected"
        >
          <Trash2 className="w-4 h-4" />
        </button>

        {onOpenSmartAdd && (
          <button
            id="tray-smart-add-btn"
            onClick={onOpenSmartAdd}
            className="flex items-center gap-1.5 px-3 py-2 bg-slate-800 hover:bg-slate-700 text-amber-300 rounded-xl text-xs font-bold transition-all border border-amber-500/30 cursor-pointer shadow-sm"
            title="Open Smart Add Product with selected items"
          >
            <Sparkles className="w-3.5 h-3.5" />
            <span className="hidden sm:inline">Smart Add</span>
          </button>
        )}

        <button
          id="tray-compare-now-btn"
          onClick={onCompareNow}
          disabled={selectedProducts.length < 2}
          className="flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-500 disabled:opacity-50 disabled:hover:bg-indigo-600 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-indigo-900/40 cursor-pointer"
        >
          <span>Compare Now</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};

export default ComparisonTray;
