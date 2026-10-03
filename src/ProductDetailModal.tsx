import React, { useState } from "react";
import { Product, Specification, Review } from "./types";
import { getProductPhotoFallback, getSpecPhotoFallback } from "./imageFallback";
import {
  X,
  Edit2,
  Trash2,
  Plus,
  Layers,
  MessageSquare,
  Maximize2,
  Star,
  Check,
  ChevronRight
} from "lucide-react";

export interface ProductDetailModalProps {
  product: Product;
  specifications: Specification[];
  reviews: Review[];
  isCompared?: boolean;
  onToggleCompare?: (product: Product) => void;
  onClose: () => void;
  onOpenAddReview?: (product: Product) => void;
  onOpenViewReview?: (review: Review) => void;
  onOpenEditReview?: (review: Review) => void;
  onOpenAddSpec?: (product: Product) => void;
  onOpenEditSpec?: (spec: Specification) => void;
  onOpenEditProduct?: (product: Product) => void;
  onDeleteProduct?: (id: string) => Promise<void>;
  onDeleteSpec?: (id: string) => Promise<void>;
}

export const ProductDetailModal: React.FC<ProductDetailModalProps> = ({
  product,
  specifications,
  reviews,
  isCompared = false,
  onToggleCompare,
  onClose,
  onOpenAddReview,
  onOpenViewReview,
  onOpenAddSpec,
  onOpenEditSpec,
  onOpenEditProduct,
  onDeleteProduct,
  onDeleteSpec,
}) => {
  const [fullscreenImage, setFullscreenImage] = useState<string | null>(null);

  const productSpecs = specifications.filter(
    (s) => s.productId === product.id || s.productTitle === product.name
  );

  const productReviews = reviews.filter(
    (r) =>
      (r.productIds && r.productIds.includes(product.id)) ||
      (r.productTitles && r.productTitles.includes(product.name))
  );

  const heroImage =
    product.pictures && product.pictures[0]?.url
      ? product.pictures[0].url
      : getProductPhotoFallback(product.name, product.brand, product.tags);

  return (
    <div
      id="product-detail-modal"
      className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-3xl max-h-[92vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div className="flex items-center gap-2">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">
              {product.brand}
            </span>
          </div>

          <div className="flex items-center gap-1.5">
            {onToggleCompare && (
              <button
                onClick={() => onToggleCompare(product)}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                  isCompared
                    ? "bg-indigo-600 text-white shadow-2xs"
                    : "bg-slate-100 hover:bg-slate-200 text-slate-700"
                }`}
              >
                <Layers className="w-3.5 h-3.5" />
                <span>{isCompared ? "Compared" : "Compare"}</span>
              </button>
            )}

            {onOpenEditProduct && (
              <button
                onClick={() => onOpenEditProduct(product)}
                className="p-2 text-slate-500 hover:text-indigo-600 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
                title="Edit product"
              >
                <Edit2 className="w-4 h-4" />
              </button>
            )}

            {onDeleteProduct && (
              <button
                onClick={async () => {
                  if (confirm(`Delete "${product.name}"?`)) {
                    await onDeleteProduct(product.id);
                    onClose();
                  }
                }}
                className="p-2 text-slate-500 hover:text-rose-600 rounded-xl hover:bg-rose-50 transition-colors cursor-pointer"
                title="Delete product"
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
          {/* Hero Section */}
          <div className="flex flex-col sm:flex-row gap-5 items-start">
            <div
              className="relative w-full sm:w-48 aspect-square rounded-2xl overflow-hidden border border-slate-200 bg-slate-50 shrink-0 cursor-pointer group"
              onClick={() => setFullscreenImage(heroImage)}
            >
              <img
                src={heroImage}
                alt={product.name}
                onError={(e) => {
                  (e.target as HTMLImageElement).src = getProductPhotoFallback(
                    product.name,
                    product.brand,
                    product.tags
                  );
                }}
                className="w-full h-full object-cover group-hover:scale-105 transition-transform"
              />
              <div className="absolute inset-0 bg-black/25 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center text-white">
                <Maximize2 className="w-5 h-5" />
              </div>
            </div>

            <div className="space-y-2 flex-1 min-w-0">
              <h1 className="text-xl sm:text-2xl font-black text-slate-900 tracking-tight">
                {product.name}
              </h1>

              {product.tags && product.tags.length > 0 && (
                <div className="flex flex-wrap gap-1.5 pt-1">
                  {product.tags.map((tag) => (
                    <span
                      key={tag}
                      className="px-2.5 py-0.5 rounded-lg bg-indigo-50 text-indigo-700 font-semibold text-xs border border-indigo-100/60"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              )}

              {product.overview && (
                <p className="text-xs sm:text-sm text-slate-600 leading-relaxed whitespace-pre-line pt-2">
                  {product.overview}
                </p>
              )}
            </div>
          </div>

          {/* Specifications Table */}
          <div className="space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                Specifications ({productSpecs.length})
              </h3>
              {onOpenAddSpec && (
                <button
                  type="button"
                  onClick={() => onOpenAddSpec(product)}
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Add spec</span>
                </button>
              )}
            </div>

            {productSpecs.length === 0 ? (
              <div className="py-6 text-center text-xs text-slate-400 bg-slate-50 rounded-2xl border border-slate-200">
                No specifications recorded yet.
              </div>
            ) : (
              <div className="divide-y divide-slate-100 border border-slate-200 rounded-2xl bg-white overflow-hidden shadow-2xs">
                {productSpecs.map((spec) => (
                  <div
                    key={spec.id}
                    className="p-3.5 flex items-center justify-between gap-4 hover:bg-slate-50/70 transition-colors"
                  >
                    <div className="min-w-0 flex-1">
                      <div className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                        {spec.specName}
                      </div>
                      <div className="text-sm font-semibold text-slate-900 mt-0.5">
                        {spec.details} {spec.unit ? <span className="text-xs font-normal text-slate-500">{spec.unit}</span> : ""}
                      </div>
                    </div>

                    <div className="flex items-center gap-1">
                      {spec.pictures && spec.pictures.length > 0 && (
                        <div className="flex items-center gap-1 mr-2">
                          {spec.pictures.map((sp, idx) => (
                            <img
                              key={sp.id || idx}
                              src={sp.url || getSpecPhotoFallback(spec.specName, spec.details)}
                              alt="Spec photo"
                              onClick={() => setFullscreenImage(sp.url)}
                              className="w-8 h-8 rounded-lg object-cover border border-slate-200 cursor-pointer"
                            />
                          ))}
                        </div>
                      )}

                      {onOpenEditSpec && (
                        <button
                          onClick={() => onOpenEditSpec(spec)}
                          className="p-1.5 text-slate-400 hover:text-indigo-600 rounded-lg hover:bg-slate-100"
                          title="Edit spec"
                        >
                          <Edit2 className="w-3.5 h-3.5" />
                        </button>
                      )}

                      {onDeleteSpec && (
                        <button
                          onClick={async () => {
                            if (confirm(`Delete spec "${spec.specName}"?`)) {
                              await onDeleteSpec(spec.id);
                            }
                          }}
                          className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50"
                          title="Delete spec"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* User Reviews */}
          <div className="space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                Linked Reviews ({productReviews.length})
              </h3>
              {onOpenAddReview && (
                <button
                  type="button"
                  onClick={() => onOpenAddReview(product)}
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Write review</span>
                </button>
              )}
            </div>

            {productReviews.length === 0 ? (
              <div className="py-6 text-center text-xs text-slate-400 bg-slate-50 rounded-2xl border border-slate-200">
                No user reviews linked to this product yet.
              </div>
            ) : (
              <div className="space-y-2">
                {productReviews.map((r) => (
                  <div
                    key={r.id}
                    className="p-3.5 bg-slate-50 hover:bg-indigo-50/40 rounded-2xl border border-slate-200/80 transition-colors flex items-center justify-between gap-4 cursor-pointer"
                    onClick={() => onOpenViewReview && onOpenViewReview(r)}
                  >
                    <div className="min-w-0">
                      <div className="flex items-center gap-2">
                        <span className="text-xs font-bold text-slate-900">
                          {r.title || `Review #${r.reviewNumber || 1}`}
                        </span>
                        {r.rating && (
                          <div className="flex items-center gap-0.5 text-amber-500 text-xs">
                            <Star className="w-3 h-3 fill-amber-400" />
                            <span>{r.rating}</span>
                          </div>
                        )}
                      </div>
                      <span className="text-[11px] text-slate-500 block truncate mt-0.5">
                        By {r.authorName || "Verified Reviewer"}
                      </span>
                    </div>

                    <ChevronRight className="w-4 h-4 text-slate-400 shrink-0" />
                  </div>
                ))}
              </div>
            )}
          </div>
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

export default ProductDetailModal;
