import React, { useState, useMemo } from "react";
import { Review, Product } from "./types";
import { stripMarkdownFormatting } from "./upnoteParser";
import {
  MessageSquare,
  Search,
  Plus,
  Star,
  Trash2,
  Edit2,
  Calendar,
  Layers,
  ChevronRight
} from "lucide-react";

export interface ReviewsSectionProps {
  reviews: Review[];
  products: Product[];
  onOpenAddReview: (product?: Product | null) => void;
  onOpenViewReview: (review: Review) => void;
  onOpenEditReview: (review: Review) => void;
  onDeleteReview: (id: string) => void;
  onViewProduct?: (product: Product) => void;
}

export const ReviewsSection: React.FC<ReviewsSectionProps> = ({
  reviews,
  products,
  onOpenAddReview,
  onOpenViewReview,
  onOpenEditReview,
  onDeleteReview,
  onViewProduct,
}) => {
  const [selectedProductFilter, setSelectedProductFilter] = useState("all");
  const [searchQuery, setSearchQuery] = useState("");

  const safeReviews = Array.isArray(reviews) ? reviews : [];
  const safeProducts = Array.isArray(products) ? products : [];

  const filteredReviews = useMemo(() => {
    return safeReviews.filter((r) => {
      // Filter by product
      if (selectedProductFilter !== "all") {
        const matchesId = r.productIds && r.productIds.includes(selectedProductFilter);
        const product = safeProducts.find((p) => p.id === selectedProductFilter);
        const matchesTitle =
          product &&
          r.productTitles &&
          r.productTitles.some((t) => t && t.toLowerCase() === product.name.toLowerCase());
        if (!matchesId && !matchesTitle) return false;
      }

      // Filter by search
      if (searchQuery.trim()) {
        const q = searchQuery.toLowerCase().trim();
        const matchesTitle = (r.title || "").toLowerCase().includes(q);
        const matchesBody = (r.body || "").toLowerCase().includes(q);
        const matchesAuthor = (r.authorName || "").toLowerCase().includes(q);
        const matchesProduct = (r.productTitles || []).some((t) => t.toLowerCase().includes(q));
        if (!matchesTitle && !matchesBody && !matchesAuthor && !matchesProduct) return false;
      }

      return true;
    });
  }, [safeReviews, selectedProductFilter, safeProducts, searchQuery]);

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Top Banner */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-6 shadow-xs flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
            <MessageSquare className="w-5 h-5" />
          </div>
          <div>
            <h2 className="text-base sm:text-lg font-bold text-slate-900">Product Reviews</h2>
            <p className="text-xs text-slate-500">
              Community benchmark reports, hands-on notes, and device comparisons
            </p>
          </div>
        </div>

        <button
          onClick={() => onOpenAddReview(null)}
          className="flex items-center justify-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold shadow-xs transition-all cursor-pointer"
        >
          <Plus className="w-3.5 h-3.5" />
          <span>Write review</span>
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="bg-white rounded-2xl border border-slate-200 p-3.5 sm:p-4 shadow-xs flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        <div className="relative flex-1 min-w-0">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search reviews by title, notes, author, or product..."
            className="w-full pl-10 pr-4 py-2 text-xs sm:text-sm bg-slate-50 hover:bg-slate-100/80 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none transition-all"
          />
        </div>

        <div className="flex items-center gap-2 shrink-0">
          <select
            value={selectedProductFilter}
            onChange={(e) => setSelectedProductFilter(e.target.value)}
            className="px-3 py-2 text-xs bg-slate-50 hover:bg-slate-100 text-slate-700 font-semibold rounded-xl border border-slate-200 outline-none cursor-pointer"
          >
            <option value="all">All Reviewed Products ({safeProducts.length})</option>
            {safeProducts.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Reviews Grid */}
      {filteredReviews.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center space-y-3">
          <div className="w-12 h-12 rounded-2xl bg-indigo-50 text-indigo-600 flex items-center justify-center mx-auto">
            <MessageSquare className="w-6 h-6" />
          </div>
          <h3 className="text-sm font-bold text-slate-800">No reviews found</h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            {searchQuery
              ? `No reviews match your query "${searchQuery}"`
              : "No reviews have been published for this product yet."}
          </p>
          <button
            onClick={() => onOpenAddReview(null)}
            className="inline-flex items-center gap-1.5 px-4 py-2 bg-indigo-600 text-white rounded-xl text-xs font-semibold shadow-xs hover:bg-indigo-700 transition-colors"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Write the first review</span>
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredReviews.map((r) => {
            const displayTitle =
              r.title?.trim() ||
              r.body?.match(/^#\s+(.+)$/m)?.[1] ||
              `Review #${r.reviewNumber || 1}`;

            const previewText = stripMarkdownFormatting(r.body, 160);

            return (
              <div
                key={r.id}
                className="bg-white rounded-2xl border border-slate-200 shadow-2xs hover:shadow-md hover:border-indigo-200 transition-all p-4 flex flex-col justify-between group"
              >
                <div className="space-y-3">
                  {/* Card Header */}
                  <div className="flex items-center justify-between">
                    <span className="text-[10px] font-bold text-indigo-700 bg-indigo-50 border border-indigo-100 px-2 py-0.5 rounded-full">
                      #{r.reviewNumber || 1}
                    </span>

                    {r.rating && (
                      <div className="flex items-center gap-1 text-amber-500 text-xs font-bold">
                        <Star className="w-3.5 h-3.5 fill-amber-400 text-amber-400" />
                        <span>{r.rating}/5</span>
                      </div>
                    )}
                  </div>

                  {/* Title & Preview */}
                  <div className="cursor-pointer" onClick={() => onOpenViewReview(r)}>
                    <h3 className="font-bold text-sm sm:text-base text-slate-900 group-hover:text-indigo-600 transition-colors line-clamp-2">
                      {displayTitle}
                    </h3>
                    <p className="text-xs text-slate-600 line-clamp-3 mt-1.5 leading-relaxed">
                      {previewText || "No text preview available."}
                    </p>
                  </div>

                  {/* Linked Product Badges */}
                  {r.productTitles && r.productTitles.length > 0 && (
                    <div className="flex flex-wrap gap-1 pt-1">
                      {r.productTitles.map((t, idx) => (
                        <span
                          key={idx}
                          className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-[10px] font-medium"
                        >
                          <Layers className="w-2.5 h-2.5 text-slate-400" />
                          <span>{t}</span>
                        </span>
                      ))}
                    </div>
                  )}
                </div>

                {/* Footer */}
                <div className="pt-4 mt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-400">
                  <div className="flex items-center gap-1 truncate max-w-[50%]">
                    <span>{r.authorName || "Reviewer"}</span>
                  </div>

                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => onOpenEditReview(r)}
                      className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-slate-100 transition-colors"
                      title="Edit review"
                    >
                      <Edit2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => {
                        if (confirm("Delete this review?")) onDeleteReview(r.id);
                      }}
                      className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition-colors"
                      title="Delete review"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                    <button
                      onClick={() => onOpenViewReview(r)}
                      className="flex items-center gap-1 px-2.5 py-1 bg-slate-100 hover:bg-indigo-50 text-slate-700 hover:text-indigo-600 rounded-lg font-semibold transition-colors"
                    >
                      <span>Read</span>
                      <ChevronRight className="w-3 h-3" />
                    </button>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};

export default ReviewsSection;
