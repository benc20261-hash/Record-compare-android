import React, { useState, useMemo } from "react";
import { Product } from "./types";
import { getProductPhotoFallback } from "./imageFallback";
import { Search, X, Check } from "lucide-react";

export interface SearchableProductSelectProps {
  products: Product[];
  selectedProductId?: string;
  onSelectProduct: (product: Product) => void;
  onClose?: () => void;
  placeholder?: string;
}

export const SearchableProductSelect: React.FC<SearchableProductSelectProps> = ({
  products,
  selectedProductId,
  onSelectProduct,
  onClose,
  placeholder = "Search products by name, brand, or tag...",
}) => {
  const [search, setSearch] = useState("");
  const [selectedTag, setSelectedTag] = useState("All");

  const tags = useMemo(() => {
    const set = new Set<string>();
    products.forEach((p) => {
      (p.tags || []).forEach((t) => {
        if (t && t.trim()) set.add(t.trim());
      });
    });
    return ["All", ...Array.from(set).sort()];
  }, [products]);

  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      if (selectedTag !== "All" && !(p.tags || []).includes(selectedTag)) {
        return false;
      }
      if (search.trim()) {
        const q = search.toLowerCase().trim();
        const matchesName = (p.name || "").toLowerCase().includes(q);
        const matchesBrand = (p.brand || "").toLowerCase().includes(q);
        const matchesTags = (p.tags || []).some((t) => t.toLowerCase().includes(q));
        if (!matchesName && !matchesBrand && !matchesTags) return false;
      }
      return true;
    });
  }, [products, selectedTag, search]);

  return (
    <div className="bg-white rounded-2xl border border-slate-200 shadow-xl p-3 sm:p-4 space-y-3 animate-in fade-in zoom-in-95 duration-150">
      {/* Search Input */}
      <div className="relative">
        <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder={placeholder}
          autoFocus
          className="w-full pl-9 pr-8 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 outline-none transition-all"
        />
        {search && (
          <button
            onClick={() => setSearch("")}
            className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 p-1"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        )}
      </div>

      {/* Tag Filters */}
      {tags.length > 1 && (
        <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-1">
          {tags.map((tag) => (
            <button
              key={tag}
              type="button"
              onClick={() => setSelectedTag(tag)}
              className={`px-2.5 py-1 text-xs rounded-lg transition-colors shrink-0 ${
                selectedTag === tag
                  ? "bg-indigo-600 text-white font-semibold"
                  : "bg-slate-100 hover:bg-slate-200 text-slate-600"
              }`}
            >
              {tag}
            </button>
          ))}
        </div>
      )}

      {/* Results List */}
      <div className="max-h-64 overflow-y-auto space-y-1.5 divide-y divide-slate-100 pr-1">
        {filteredProducts.length === 0 ? (
          <div className="py-6 text-center text-xs text-slate-400">
            No products found matching &ldquo;{search}&rdquo;
          </div>
        ) : (
          filteredProducts.map((p) => {
            const isSelected = p.id === selectedProductId;
            const imgSrc =
              p.pictures && p.pictures[0]?.url
                ? p.pictures[0].url
                : getProductPhotoFallback(p.name, p.brand, p.tags);

            return (
              <button
                key={p.id}
                type="button"
                onClick={() => {
                  onSelectProduct(p);
                  if (onClose) onClose();
                }}
                className={`w-full flex items-center justify-between p-2 rounded-xl text-left transition-all cursor-pointer ${
                  isSelected ? "bg-indigo-50 border border-indigo-200" : "hover:bg-slate-50"
                }`}
              >
                <div className="flex items-center gap-2.5 min-w-0">
                  <img
                    src={imgSrc}
                    alt={p.name}
                    onError={(e) => {
                      (e.target as HTMLImageElement).src = getProductPhotoFallback(p.name, p.brand, p.tags);
                    }}
                    className="w-10 h-10 rounded-lg object-cover bg-slate-100 border border-slate-200 shrink-0"
                  />
                  <div className="min-w-0">
                    <div className="font-semibold text-xs sm:text-sm text-slate-900 truncate">
                      {p.name}
                    </div>
                    <div className="text-[11px] text-slate-500 truncate">
                      {p.brand} {p.tags && p.tags.length > 0 && `• ${p.tags.join(", ")}`}
                    </div>
                  </div>
                </div>

                {isSelected && <Check className="w-4 h-4 text-indigo-600 shrink-0 ml-2" />}
              </button>
            );
          })
        )}
      </div>
    </div>
  );
};

export default SearchableProductSelect;
