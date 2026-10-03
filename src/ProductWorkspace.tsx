import React, { useState, useMemo } from "react";
import { Product, Specification, Review, ViewType } from "./types";
import { getProductPhotoFallback } from "./imageFallback";
import {
  Search,
  Grid,
  List,
  Plus,
  Sparkles,
  Layers,
  Star,
  X,
  MessageSquare,
  ChevronRight,
  ExternalLink
} from "lucide-react";

export interface ProductWorkspaceProps {
  products: Product[];
  specifications: Specification[];
  reviews: Review[];
  comparedProductIds?: string[];
  onToggleCompare: (product: Product) => void;
  onOpenProductDetail: (product: Product) => void;
  onOpenEditProduct?: (product: Product) => void;
  onOpenAddProduct?: () => void;
  onOpenSmartAdd?: () => void;
  onOpenAddSpec?: (product?: Product) => void;
  onOpenEditSpec?: (spec: Specification) => void;
  onOpenViewReview?: (review: Review) => void;
  onOpenAddReview?: (product?: Product | null) => void;
}

export const ProductWorkspace: React.FC<ProductWorkspaceProps> = ({
  products,
  specifications,
  reviews,
  comparedProductIds = [],
  onToggleCompare,
  onOpenProductDetail,
  onOpenAddProduct,
  onOpenSmartAdd,
}) => {
  const [viewMode, setViewMode] = useState<"grid" | "table">("grid");
  const [searchQuery, setSearchQuery] = useState("");
  const [selectedTag, setSelectedTag] = useState("All");
  const [selectedBrand, setSelectedBrand] = useState("All");

  // Tag list
  const tags = useMemo(() => {
    const set = new Set<string>();
    products.forEach((p) => {
      (p.tags || []).forEach((t) => {
        if (t && t.trim()) set.add(t.trim());
      });
    });
    return ["All", ...Array.from(set).sort()];
  }, [products]);

  // Brand list
  const brands = useMemo(() => {
    const set = new Set<string>();
    products.forEach((p) => {
      if (p.brand && p.brand.trim()) set.add(p.brand.trim());
    });
    return ["All", ...Array.from(set).sort()];
  }, [products]);

  // Filtered products
  const filteredProducts = useMemo(() => {
    return products.filter((p) => {
      if (selectedTag !== "All" && !(p.tags || []).includes(selectedTag)) {
        return false;
      }
      if (selectedBrand !== "All" && p.brand !== selectedBrand) {
        return false;
      }
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchesName = (p.name || "").toLowerCase().includes(q);
        const matchesBrand = (p.brand || "").toLowerCase().includes(q);
        const matchesOverview = (p.overview || "").toLowerCase().includes(q);
        const matchesTags = (p.tags || []).some((t) => t.toLowerCase().includes(q));
        const matchesSpecs = specifications
          .filter((s) => s.productId === p.id || s.productTitle === p.name)
          .some((s) => s.specName.toLowerCase().includes(q) || s.details.toLowerCase().includes(q));

        if (!matchesName && !matchesBrand && !matchesOverview && !matchesTags && !matchesSpecs) {
          return false;
        }
      }
      return true;
    });
  }, [products, specifications, selectedTag, selectedBrand, searchQuery]);

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Top Filter and Controls Bar */}
      <div className="bg-white rounded-2xl border border-slate-200 p-3.5 sm:p-4 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-2.5 sm:gap-3">
          {/* Search Box */}
          <div className="relative flex-1 min-w-0">
            <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
            <input
              id="catalog-search-input"
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search products, brands, or specifications..."
              className="w-full pl-10 pr-9 py-2 text-xs sm:text-sm bg-slate-50 hover:bg-slate-100/80 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-100 transition-all outline-none"
            />
            {searchQuery && (
              <button
                id="catalog-clear-search-btn"
                type="button"
                onClick={() => setSearchQuery("")}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 p-1 cursor-pointer"
                title="Clear search"
              >
                <X className="w-3.5 h-3.5" />
              </button>
            )}
          </div>

          {/* View and Action Buttons */}
          <div className="flex items-center justify-between sm:justify-end gap-2 shrink-0">
            <div className="flex items-center bg-slate-100 p-1 rounded-lg">
              <button
                id="view-grid-btn"
                onClick={() => setViewMode("grid")}
                className={`p-1.5 rounded-md transition-colors cursor-pointer ${
                  viewMode === "grid"
                    ? "bg-white text-slate-900 shadow-2xs font-semibold"
                    : "text-slate-500 hover:text-slate-900"
                }`}
                title="Grid View"
              >
                <Grid className="w-3.5 h-3.5" />
              </button>
              <button
                id="view-table-btn"
                onClick={() => setViewMode("table")}
                className={`p-1.5 rounded-md transition-colors cursor-pointer ${
                  viewMode === "table"
                    ? "bg-white text-slate-900 shadow-2xs font-semibold"
                    : "text-slate-500 hover:text-slate-900"
                }`}
                title="Table View"
              >
                <List className="w-3.5 h-3.5" />
              </button>
            </div>

            {onOpenSmartAdd && (
              <button
                id="catalog-smart-add-btn"
                onClick={onOpenSmartAdd}
                className="flex items-center gap-1.5 px-3 py-2 bg-slate-900 hover:bg-slate-800 active:bg-slate-950 text-white rounded-xl text-xs font-bold transition-all shadow-xs cursor-pointer"
                title="Smart Add Product"
              >
                <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                <span>Smart Add</span>
              </button>
            )}

            {onOpenAddProduct && (
              <button
                id="add-product-btn"
                onClick={onOpenAddProduct}
                className="flex items-center gap-1.5 px-3 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold transition-all shadow-xs cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Product</span>
              </button>
            )}
          </div>
        </div>

        {/* Filter chips */}
        <div className="flex items-center gap-2 overflow-x-auto no-scrollbar pt-1">
          <span className="text-[11px] font-bold text-slate-400 shrink-0">Tags:</span>
          {tags.map((tag) => (
            <button
              key={tag}
              type="button"
              onClick={() => setSelectedTag(tag)}
              className={`px-2.5 py-1 text-xs rounded-lg transition-colors shrink-0 ${
                selectedTag === tag
                  ? "bg-indigo-600 text-white font-semibold"
                  : "bg-slate-100 hover:bg-slate-200 text-slate-700"
              }`}
            >
              {tag}
            </button>
          ))}
        </div>
      </div>

      {/* Grid View */}
      {viewMode === "grid" ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-5">
          {filteredProducts.map((p) => {
            const isCompared = comparedProductIds.includes(p.id);
            const productSpecs = specifications.filter(
              (s) => s.productId === p.id || s.productTitle === p.name
            );
            const productReviews = reviews.filter(
              (r) =>
                (r.productIds && r.productIds.includes(p.id)) ||
                (r.productTitles && r.productTitles.includes(p.name))
            );

            const imgSrc =
              p.pictures && p.pictures[0]?.url
                ? p.pictures[0].url
                : getProductPhotoFallback(p.name, p.brand, p.tags);

            return (
              <div
                key={p.id}
                className="bg-white rounded-2xl border border-slate-200 shadow-2xs hover:shadow-md hover:border-indigo-200 transition-all overflow-hidden flex flex-col justify-between group"
              >
                <div>
                  {/* Image container */}
                  <div
                    className="relative aspect-video bg-slate-100 cursor-pointer overflow-hidden"
                    onClick={() => onOpenProductDetail(p)}
                  >
                    <img
                      src={imgSrc}
                      alt={p.name}
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = getProductPhotoFallback(
                          p.name,
                          p.brand,
                          p.tags
                        );
                      }}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                    />

                    {/* Brand Pill */}
                    <div className="absolute top-2.5 left-2.5 px-2 py-0.5 rounded-lg bg-black/60 backdrop-blur-xs text-white text-[10px] font-bold uppercase tracking-wider">
                      {p.brand}
                    </div>

                    {/* Compare Checkbox */}
                    <button
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        onToggleCompare(p);
                      }}
                      className={`absolute top-2.5 right-2.5 p-1.5 rounded-lg shadow-sm backdrop-blur-xs transition-all cursor-pointer ${
                        isCompared
                          ? "bg-indigo-600 text-white ring-2 ring-white/50"
                          : "bg-white/80 hover:bg-white text-slate-700"
                      }`}
                      title={isCompared ? "Remove from comparison" : "Add to compare"}
                    >
                      <Layers className="w-3.5 h-3.5" />
                    </button>
                  </div>

                  {/* Card Content */}
                  <div className="p-4 space-y-2.5">
                    <h3
                      className="font-bold text-sm sm:text-base text-slate-900 group-hover:text-indigo-600 transition-colors cursor-pointer line-clamp-1"
                      onClick={() => onOpenProductDetail(p)}
                    >
                      {p.name}
                    </h3>

                    {/* Tags */}
                    {p.tags && p.tags.length > 0 && (
                      <div className="flex flex-wrap gap-1">
                        {p.tags.slice(0, 3).map((tag) => (
                          <span
                            key={tag}
                            className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-600 text-[10px] font-medium"
                          >
                            {tag}
                          </span>
                        ))}
                      </div>
                    )}

                    {/* Highlighted Specs */}
                    {productSpecs.length > 0 && (
                      <div className="space-y-1 pt-1 border-t border-slate-100 text-xs">
                        {productSpecs.slice(0, 2).map((s) => (
                          <div key={s.id} className="flex items-center justify-between text-slate-600">
                            <span className="text-slate-400">{s.specName}:</span>
                            <span className="font-semibold text-slate-800 truncate max-w-[120px]">
                              {s.details} {s.unit || ""}
                            </span>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {/* Card Footer */}
                <div className="px-4 py-3 bg-slate-50/70 border-t border-slate-100 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-1 text-slate-500">
                    <MessageSquare className="w-3.5 h-3.5 text-slate-400" />
                    <span>{productReviews.length} reviews</span>
                  </div>

                  <button
                    onClick={() => onOpenProductDetail(p)}
                    className="flex items-center gap-1 font-semibold text-indigo-600 hover:text-indigo-800 transition-colors"
                  >
                    <span>Details</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      ) : (
        /* Table View */
        <div className="bg-white rounded-2xl border border-slate-200 overflow-hidden shadow-2xs">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-500 font-bold uppercase tracking-wider border-b border-slate-200">
                <tr>
                  <th className="p-3 w-10">Compare</th>
                  <th className="p-3">Product</th>
                  <th className="p-3">Brand</th>
                  <th className="p-3">Tags</th>
                  <th className="p-3">Key Specs</th>
                  <th className="p-3 text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {filteredProducts.map((p) => {
                  const isCompared = comparedProductIds.includes(p.id);
                  const productSpecs = specifications.filter(
                    (s) => s.productId === p.id || s.productTitle === p.name
                  );

                  return (
                    <tr key={p.id} className="hover:bg-slate-50/80 transition-colors">
                      <td className="p-3">
                        <button
                          type="button"
                          onClick={() => onToggleCompare(p)}
                          className={`p-1.5 rounded-lg ${
                            isCompared ? "bg-indigo-600 text-white" : "text-slate-400 hover:bg-slate-200"
                          }`}
                        >
                          <Layers className="w-3.5 h-3.5" />
                        </button>
                      </td>
                      <td className="p-3 font-semibold text-slate-900">
                        <button
                          onClick={() => onOpenProductDetail(p)}
                          className="hover:text-indigo-600 text-left font-bold"
                        >
                          {p.name}
                        </button>
                      </td>
                      <td className="p-3 text-slate-600 font-medium">{p.brand}</td>
                      <td className="p-3">
                        <div className="flex flex-wrap gap-1">
                          {p.tags?.map((t) => (
                            <span
                              key={t}
                              className="px-1.5 py-0.5 rounded bg-slate-100 text-slate-600 text-[10px]"
                            >
                              {t}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="p-3 text-slate-500">
                        {productSpecs
                          .slice(0, 2)
                          .map((s) => `${s.specName}: ${s.details}`)
                          .join(" • ") || "-"}
                      </td>
                      <td className="p-3 text-right">
                        <button
                          onClick={() => onOpenProductDetail(p)}
                          className="px-2.5 py-1 text-xs font-semibold text-indigo-600 hover:bg-indigo-50 rounded-lg"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default ProductWorkspace;
