import React, { useState } from "react";
import { Review, Product, MediaAttachment } from "./types";
import { FormattedReviewContent } from "./FormattedReviewContent";
import { getProductPhotoFallback } from "./imageFallback";
import {
  X,
  Edit2,
  Trash2,
  Star,
  Layers,
  ThumbsUp,
  ThumbsDown,
  Calendar,
  ExternalLink,
  Maximize2
} from "lucide-react";

export interface UnifiedReviewViewerProps {
  review: Review | null;
  products: Product[];
  onClose: () => void;
  onEditReview?: (review: Review) => void;
  onDeleteReview?: (id: string) => void;
  onViewProduct?: (product: Product) => void;
  onToggleCompare?: (product: Product) => void;
  comparedProductIds?: string[];
}

export const UnifiedReviewViewer: React.FC<UnifiedReviewViewerProps> = ({
  review,
  products,
  onClose,
  onEditReview,
  onDeleteReview,
  onViewProduct,
  onToggleCompare,
  comparedProductIds = [],
}) => {
  const [fullscreenImage, setFullscreenImage] = useState<string | null>(null);

  if (!review) return null;

  const linkedProducts = products.filter(
    (p) =>
      (review.productIds && review.productIds.includes(p.id)) ||
      (review.productTitles && review.productTitles.includes(p.name))
  );

  const displayTitle =
    review.title?.trim() ||
    (review.body?.match(/^#\s+(.+)$/m)?.[1]) ||
    (review.body?.match(/<h[1-3][^>]*>(.*?)<\/h[1-3]>/i)?.[1]?.replace(/<[^>]+>/g, "").trim()) ||
    `Review #${review.reviewNumber || 1}`;

  return (
    <div
      id="unified-review-viewer-modal"
      className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-3xl max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold text-indigo-700 bg-indigo-50 border border-indigo-100 px-2.5 py-1 rounded-full">
              Review #{review.reviewNumber || 1}
            </span>
            {review.rating && (
              <div className="flex items-center gap-1 text-amber-500 bg-amber-50 border border-amber-200 px-2 py-0.5 rounded-full text-xs font-bold">
                <Star className="w-3.5 h-3.5 fill-amber-500" />
                <span>{review.rating}/5</span>
              </div>
            )}
          </div>

          <div className="flex items-center gap-1">
            {onEditReview && (
              <button
                onClick={() => onEditReview(review)}
                className="p-2 text-slate-500 hover:text-indigo-600 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
                title="Edit review"
              >
                <Edit2 className="w-4 h-4" />
              </button>
            )}

            {onDeleteReview && (
              <button
                onClick={() => {
                  if (confirm("Are you sure you want to delete this review?")) {
                    onDeleteReview(review.id);
                    onClose();
                  }
                }}
                className="p-2 text-slate-500 hover:text-rose-600 rounded-xl hover:bg-rose-50 transition-colors cursor-pointer"
                title="Delete review"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            )}

            <button
              onClick={onClose}
              className="p-2 text-slate-400 hover:text-slate-600 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Scrollable Content */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {/* Title and Author Meta */}
          <div>
            <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight leading-snug">
              {displayTitle}
            </h1>
            <div className="flex items-center gap-2 mt-2 text-xs text-slate-500">
              <span className="font-semibold text-slate-700">
                {review.authorName || "Verified Reviewer"}
              </span>
              <span>•</span>
              <div className="flex items-center gap-1">
                <Calendar className="w-3 h-3 text-slate-400" />
                <span>{review.createdTime ? new Date(review.createdTime).toLocaleDateString() : "Consumer Tech"}</span>
              </div>
            </div>
          </div>

          {/* Linked Products Carousel / Cards */}
          {linkedProducts.length > 0 && (
            <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200/80 space-y-2">
              <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                Reviewed Products ({linkedProducts.length})
              </span>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                {linkedProducts.map((p) => {
                  const isCompared = comparedProductIds.includes(p.id);
                  const imgSrc =
                    p.pictures && p.pictures[0]?.url
                      ? p.pictures[0].url
                      : getProductPhotoFallback(p.name, p.brand, p.tags);

                  return (
                    <div
                      key={p.id}
                      className="flex items-center justify-between p-2.5 bg-white rounded-xl border border-slate-200 shadow-2xs hover:border-indigo-200 transition-all"
                    >
                      <div className="flex items-center gap-2.5 min-w-0">
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
                          className="w-10 h-10 rounded-lg object-cover bg-slate-100 shrink-0"
                        />
                        <div className="min-w-0">
                          <h4 className="font-bold text-xs text-slate-900 truncate">{p.name}</h4>
                          <span className="text-[11px] text-slate-500 truncate block">{p.brand}</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-1">
                        {onViewProduct && (
                          <button
                            onClick={() => onViewProduct(p)}
                            className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-slate-50 transition-colors"
                            title="View product"
                          >
                            <ExternalLink className="w-3.5 h-3.5" />
                          </button>
                        )}
                        {onToggleCompare && (
                          <button
                            onClick={() => onToggleCompare(p)}
                            className={`p-1.5 rounded-lg text-xs font-semibold transition-colors ${
                              isCompared
                                ? "bg-indigo-600 text-white"
                                : "text-slate-400 hover:text-slate-700 hover:bg-slate-100"
                            }`}
                            title={isCompared ? "Remove from compare" : "Add to compare"}
                          >
                            <Layers className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Pros & Cons */}
          {((review.pros && review.pros.length > 0) || (review.cons && review.cons.length > 0)) && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              {review.pros && review.pros.length > 0 && (
                <div className="bg-emerald-50/70 border border-emerald-200 rounded-2xl p-3.5 space-y-2">
                  <div className="flex items-center gap-1.5 text-emerald-800 text-xs font-bold">
                    <ThumbsUp className="w-3.5 h-3.5" />
                    <span>Pros</span>
                  </div>
                  <ul className="space-y-1">
                    {review.pros.map((pro, i) => (
                      <li key={i} className="text-xs text-emerald-950 flex items-start gap-1.5">
                        <span className="text-emerald-500 font-bold">•</span>
                        <span>{pro}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}

              {review.cons && review.cons.length > 0 && (
                <div className="bg-rose-50/70 border border-rose-200 rounded-2xl p-3.5 space-y-2">
                  <div className="flex items-center gap-1.5 text-rose-800 text-xs font-bold">
                    <ThumbsDown className="w-3.5 h-3.5" />
                    <span>Cons</span>
                  </div>
                  <ul className="space-y-1">
                    {review.cons.map((con, i) => (
                      <li key={i} className="text-xs text-rose-950 flex items-start gap-1.5">
                        <span className="text-rose-500 font-bold">•</span>
                        <span>{con}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Body Content */}
          <FormattedReviewContent
            content={review.body}
            onImageClick={(url) => setFullscreenImage(url)}
          />

          {/* Extra Attached Pictures Gallery */}
          {review.pictures && review.pictures.length > 0 && (
            <div className="space-y-2 pt-4 border-t border-slate-100">
              <span className="text-xs font-bold text-slate-700">Attached Pictures</span>
              <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                {review.pictures.map((pic, idx) => (
                  <div
                    key={pic.id || idx}
                    className="relative group rounded-xl overflow-hidden border border-slate-200 bg-slate-100 aspect-video cursor-pointer"
                    onClick={() => setFullscreenImage(pic.url)}
                  >
                    <img
                      src={pic.url}
                      alt={pic.name || "Review attachment"}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform"
                    />
                    <div className="absolute inset-0 bg-black/30 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white">
                      <Maximize2 className="w-5 h-5" />
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Fullscreen Image Overlay */}
      {fullscreenImage && (
        <div
          className="fixed inset-0 z-[60] bg-black/90 flex items-center justify-center p-4 animate-in fade-in"
          onClick={() => setFullscreenImage(null)}
        >
          <img
            src={fullscreenImage}
            alt="Fullscreen preview"
            className="max-w-full max-h-[90vh] object-contain rounded-xl"
          />
          <button
            onClick={() => setFullscreenImage(null)}
            className="absolute top-4 right-4 p-2 bg-white/20 text-white rounded-full hover:bg-white/30"
          >
            <X className="w-6 h-6" />
          </button>
        </div>
      )}
    </div>
  );
};

export default UnifiedReviewViewer;
