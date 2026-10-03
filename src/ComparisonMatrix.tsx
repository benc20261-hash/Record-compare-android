import React, { useState, useMemo, useRef } from "react";
import { Product, Specification, Review, MediaAttachment } from "./types";
import { getProductPhotoFallback, getSpecPhotoFallback } from "./imageFallback";
import {
  Layers,
  Sparkles,
  Search,
  X,
  Plus,
  Edit2,
  Maximize2,
  MessageSquare,
  ChevronLeft,
  ChevronRight,
  SlidersHorizontal,
  ChevronDown,
  BarChart2
} from "lucide-react";

export interface ComparisonMatrixProps {
  products: Product[];
  specifications: Specification[];
  reviews: Review[];
  comparedProductIds: string[];
  onToggleCompare: (product: Product) => void;
  onEditProduct?: (product: Product) => void;
  onOpenAddReview?: (product: Product) => void;
  onOpenViewReview?: (review: Review) => void;
  onOpenFullscreenImage?: (images: MediaAttachment[], index: number) => void;
}

export const ComparisonMatrix: React.FC<ComparisonMatrixProps> = ({
  products,
  specifications,
  reviews,
  comparedProductIds,
  onToggleCompare,
  onEditProduct,
  onOpenAddReview,
  onOpenViewReview,
  onOpenFullscreenImage,
}) => {
  // Slots state - start with compared products or up to 3 slots
  const [slotProductIds, setSlotProductIds] = useState<(string | null)[]>(() => {
    if (comparedProductIds.length > 0) {
      return comparedProductIds.length === 1 ? [comparedProductIds[0], null] : [...comparedProductIds];
    }
    return [products[0]?.id || null, products[1]?.id || null];
  });

  const [activeSlotDropdown, setActiveSlotDropdown] = useState<number | null>(null);
  const [dropdownSearch, setDropdownSearch] = useState("");
  const [selectedTagFilter, setSelectedTagFilter] = useState("All");
  const [showOnlyDifferences, setShowOnlyDifferences] = useState(false);
  const [tagSearchQuery, setTagSearchQuery] = useState("");
  const [specModes, setSpecModes] = useState<Record<string, "higher" | "lower">>({});
  const tagScrollRef = useRef<HTMLDivElement>(null);

  // Available tags across catalog
  const availableTags = useMemo(() => {
    const set = new Set<string>();
    products.forEach((p) => {
      (p.tags || []).forEach((t) => {
        if (t && t.trim()) set.add(t.trim());
      });
      if (p.brand && p.brand.trim()) set.add(p.brand.trim());
    });
    return ["All", ...Array.from(set).sort()];
  }, [products]);

  const filteredTags = useMemo(() => {
    if (!tagSearchQuery.trim()) return availableTags;
    const q = tagSearchQuery.toLowerCase().trim();
    return availableTags.filter((t) => t === "All" || t.toLowerCase().includes(q));
  }, [availableTags, tagSearchQuery]);

  // Selected products for slots
  const selectedProducts = useMemo(() => {
    return slotProductIds.map((id) => (id ? products.find((p) => p.id === id) || null : null));
  }, [slotProductIds, products]);

  const activeProducts = useMemo(() => {
    return selectedProducts.filter((p): p is Product => p !== null);
  }, [selectedProducts]);

  // All unique spec names across active products
  const allSpecNames = useMemo(() => {
    const names = new Set<string>();
    activeProducts.forEach((p) => {
      specifications
        .filter((s) => s.productId === p.id || s.productTitle === p.name)
        .forEach((s) => {
          if (s.specName && s.specName.trim()) names.add(s.specName.trim());
        });
    });
    return Array.from(names);
  }, [activeProducts, specifications]);

  // Differences calculation
  const diffAnalysis = useMemo(() => {
    const diffs: Record<string, boolean> = {};
    let diffCount = 0;

    if (activeProducts.length < 2) {
      return { diffs, diffCount: 0 };
    }

    // Check Brand
    const brands = activeProducts.map((p) => (p.brand || "").toLowerCase().trim());
    if (new Set(brands).size > 1) {
      diffs["Brand"] = true;
      diffCount++;
    }

    // Check Tags
    const tagStrings = activeProducts.map((p) => (p.tags || []).slice().sort().join(","));
    if (new Set(tagStrings).size > 1) {
      diffs["Tags"] = true;
      diffCount++;
    }

    // Check Overview
    const overviews = activeProducts.map((p) => (p.overview || "").trim());
    if (new Set(overviews).size > 1) {
      diffs["Overview"] = true;
      diffCount++;
    }

    // Check Specs
    allSpecNames.forEach((specName) => {
      const values = activeProducts.map((p) => {
        const found = specifications.find(
          (s) => (s.productId === p.id || s.productTitle === p.name) && s.specName.toLowerCase() === specName.toLowerCase()
        );
        return (found?.details || "").toLowerCase().trim();
      });
      if (new Set(values).size > 1) {
        diffs[specName] = true;
        diffCount++;
      }
    });

    return { diffs, diffCount };
  }, [activeProducts, allSpecNames, specifications]);

  // Add slot
  const handleAddSlot = () => {
    if (slotProductIds.length < 5) {
      setSlotProductIds([...slotProductIds, null]);
    }
  };

  // Remove slot
  const handleRemoveSlot = (index: number) => {
    if (slotProductIds.length > 2) {
      setSlotProductIds(slotProductIds.filter((_, i) => i !== index));
    } else {
      const copy = [...slotProductIds];
      copy[index] = null;
      setSlotProductIds(copy);
    }
  };

  // Select product into slot
  const handleSelectProduct = (index: number, product: Product) => {
    const copy = [...slotProductIds];
    copy[index] = product.id;
    setSlotProductIds(copy);
    setActiveSlotDropdown(null);
  };

  const handleScrollTags = (direction: "left" | "right") => {
    if (tagScrollRef.current) {
      tagScrollRef.current.scrollBy({
        left: direction === "left" ? -200 : 200,
        behavior: "smooth",
      });
    }
  };

  return (
    <div className="space-y-6 animate-in fade-in duration-200">
      {/* Compare Workspace Header */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-6 shadow-xs space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-50 border border-indigo-100 flex items-center justify-center text-indigo-600">
              <Layers className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base sm:text-lg font-black text-slate-900 tracking-tight">
                  COMPARE WORKSPACE
                </h2>
                <span className="px-2 py-0.5 rounded-full text-[10px] font-bold bg-indigo-100 text-indigo-700">
                  {activeProducts.length} Selected
                </span>
              </div>
              <p className="text-xs text-slate-500">
                Side-by-side benchmark matrix with automated difference detection
              </p>
            </div>
          </div>

          {/* Action buttons */}
          <div className="flex items-center gap-2">
            <button
              onClick={() => setShowOnlyDifferences(!showOnlyDifferences)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                showOnlyDifferences
                  ? "bg-amber-100 text-amber-900 border border-amber-300 shadow-2xs"
                  : "bg-slate-100 hover:bg-slate-200 text-slate-700"
              }`}
            >
              <SlidersHorizontal className="w-3.5 h-3.5" />
              <span>Differences only</span>
              {diffAnalysis.diffCount > 0 && (
                <span className="px-1.5 py-0.2 bg-amber-500 text-white rounded-full text-[10px] font-bold">
                  {diffAnalysis.diffCount}
                </span>
              )}
            </button>

            <button
              id="add-record-slot-btn"
              onClick={handleAddSlot}
              disabled={slotProductIds.length >= 5}
              className="flex items-center gap-1.5 px-3.5 py-1.5 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 active:bg-indigo-800 rounded-xl transition-all shadow-xs cursor-pointer disabled:opacity-50"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add slot</span>
            </button>
          </div>
        </div>

        {/* Tag Filter Carousel */}
        <div className="pt-2 border-t border-slate-100 flex items-center justify-between gap-3">
          <div className="flex items-center gap-2 text-xs font-bold text-slate-600 shrink-0">
            <span>Filter by:</span>
          </div>

          <div className="relative flex items-center flex-1 min-w-0 group/scroll">
            <button
              type="button"
              onClick={() => handleScrollTags("left")}
              className="hidden sm:flex absolute left-0 z-10 w-6 h-6 -ml-1 rounded-full bg-white/95 text-slate-700 border border-slate-200 shadow-sm items-center justify-center cursor-pointer"
            >
              <ChevronLeft className="w-3.5 h-3.5" />
            </button>

            <div
              ref={tagScrollRef}
              className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-1 scroll-smooth w-full sm:px-6"
            >
              {filteredTags.map((tag) => (
                <button
                  key={tag}
                  type="button"
                  onClick={() => setSelectedTagFilter(tag)}
                  className={`px-2.5 py-1 text-xs rounded-lg transition-colors shrink-0 whitespace-nowrap cursor-pointer ${
                    selectedTagFilter === tag
                      ? "bg-indigo-600 text-white font-semibold shadow-2xs"
                      : "bg-slate-100 hover:bg-slate-200 text-slate-700"
                  }`}
                >
                  {tag}
                </button>
              ))}
            </div>

            <button
              type="button"
              onClick={() => handleScrollTags("right")}
              className="hidden sm:flex absolute right-0 z-10 w-6 h-6 -mr-1 rounded-full bg-white/95 text-slate-700 border border-slate-200 shadow-sm items-center justify-center cursor-pointer"
            >
              <ChevronRight className="w-3.5 h-3.5" />
            </button>
          </div>
        </div>
      </div>

      {/* Record Slot Selectors */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {slotProductIds.map((productId, idx) => {
          const product = productId ? products.find((p) => p.id === productId) || null : null;
          const isDropdownOpen = activeSlotDropdown === idx;

          return (
            <div
              key={idx}
              id={`record-slot-${idx + 1}`}
              className="relative bg-white rounded-2xl border border-slate-200 shadow-2xs p-3.5 sm:p-4 space-y-3 transition-all"
            >
              <div className="flex items-center justify-between pb-2 border-b border-slate-100">
                <span className="text-xs font-bold text-slate-400 tracking-wider uppercase">
                  RECORD {idx + 1}
                </span>
                <div className="flex items-center gap-1.5">
                  {product && onEditProduct && (
                    <button
                      onClick={() => onEditProduct(product)}
                      className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 px-2 py-0.5 rounded-md hover:bg-indigo-50"
                      title="Edit product"
                    >
                      <Edit2 className="w-3 h-3" />
                      <span>Edit</span>
                    </button>
                  )}
                  <button
                    onClick={() => handleRemoveSlot(idx)}
                    className="p-1 text-slate-400 hover:text-rose-600 rounded-md hover:bg-rose-50 transition-colors"
                    title={slotProductIds.length > 2 ? "Remove slot" : "Clear slot"}
                  >
                    <X className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>

              {product ? (
                <div
                  onClick={() => setActiveSlotDropdown(isDropdownOpen ? null : idx)}
                  className="flex items-center justify-between p-2 rounded-xl bg-slate-50 hover:bg-slate-100/80 border border-slate-200 cursor-pointer transition-colors group"
                >
                  <div className="flex items-center gap-3 min-w-0">
                    <img
                      src={
                        product.pictures && product.pictures[0]?.url
                          ? product.pictures[0].url
                          : getProductPhotoFallback(product.name, product.brand, product.tags)
                      }
                      alt={product.name}
                      onError={(e) => {
                        (e.target as HTMLImageElement).src = getProductPhotoFallback(
                          product.name,
                          product.brand,
                          product.tags
                        );
                      }}
                      className="w-12 h-12 rounded-lg object-cover bg-white border border-slate-200 shrink-0"
                    />
                    <div className="min-w-0">
                      <div className="font-bold text-slate-900 text-sm truncate group-hover:text-indigo-600 transition-colors">
                        {product.name}
                      </div>
                      <div className="text-xs text-slate-500 flex items-center gap-1.5 flex-wrap">
                        <span className="text-emerald-600 font-semibold">✓ Selected</span>
                        <span>/</span>
                        <span className="font-medium text-slate-700">{product.brand}</span>
                      </div>
                    </div>
                  </div>
                  <ChevronDown className="w-4 h-4 text-slate-400 shrink-0 ml-2" />
                </div>
              ) : (
                <button
                  id={`slot-select-btn-${idx + 1}`}
                  onClick={() => setActiveSlotDropdown(isDropdownOpen ? null : idx)}
                  className="w-full flex items-center justify-between p-3.5 rounded-xl border border-dashed border-slate-300 hover:border-indigo-500 bg-slate-50/60 hover:bg-indigo-50/30 transition-all text-left group cursor-pointer"
                >
                  <div className="flex items-center gap-3">
                    <div className="w-9 h-9 rounded-lg bg-slate-200 group-hover:bg-indigo-100 text-slate-500 group-hover:text-indigo-600 flex items-center justify-center transition-colors">
                      <Plus className="w-4 h-4" />
                    </div>
                    <div>
                      <div className="text-sm font-semibold text-slate-700 group-hover:text-indigo-600 transition-colors">
                        Search and select a record
                      </div>
                      <div className="text-xs text-slate-400">
                        Tap to filter and choose a product to compare
                      </div>
                    </div>
                  </div>
                  <ChevronDown className="w-4 h-4 text-slate-400 group-hover:text-indigo-600 shrink-0" />
                </button>
              )}

              {/* Slot Dropdown Menu */}
              {isDropdownOpen && (
                <div className="absolute left-0 right-0 top-full mt-2 z-40 bg-white rounded-2xl border border-slate-200 shadow-xl p-3 sm:p-4 space-y-3 animate-in fade-in zoom-in-95 duration-150">
                  <div className="relative">
                    <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
                    <input
                      type="text"
                      value={dropdownSearch}
                      onChange={(e) => setDropdownSearch(e.target.value)}
                      placeholder="Search any field..."
                      autoFocus
                      className="w-full pl-9 pr-8 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
                    />
                    {dropdownSearch && (
                      <button
                        onClick={() => setDropdownSearch("")}
                        className="absolute right-2.5 top-1/2 -translate-y-1/2 text-xs text-slate-400 hover:text-slate-600 p-1"
                      >
                        ✕
                      </button>
                    )}
                  </div>

                  <div className="max-h-56 overflow-y-auto space-y-1 pr-1">
                    {products
                      .filter((p) => {
                        if (selectedTagFilter !== "All" && !(p.tags || []).includes(selectedTagFilter) && p.brand !== selectedTagFilter) {
                          return false;
                        }
                        if (dropdownSearch.trim()) {
                          const q = dropdownSearch.toLowerCase().trim();
                          return (
                            (p.name || "").toLowerCase().includes(q) ||
                            (p.brand || "").toLowerCase().includes(q) ||
                            (p.overview || "").toLowerCase().includes(q)
                          );
                        }
                        return true;
                      })
                      .map((p) => (
                        <button
                          key={p.id}
                          type="button"
                          onClick={() => handleSelectProduct(idx, p)}
                          className="w-full flex items-center justify-between p-2 rounded-xl text-left hover:bg-slate-50 transition-colors cursor-pointer"
                        >
                          <div className="flex items-center gap-2.5 min-w-0">
                            <img
                              src={
                                p.pictures && p.pictures[0]?.url
                                  ? p.pictures[0].url
                                  : getProductPhotoFallback(p.name, p.brand, p.tags)
                              }
                              alt={p.name}
                              className="w-8 h-8 rounded-lg object-cover bg-slate-100 shrink-0"
                            />
                            <div className="min-w-0">
                              <div className="font-semibold text-xs text-slate-900 truncate">
                                {p.name}
                              </div>
                              <div className="text-[10px] text-slate-500 truncate">
                                {p.brand} {p.tags?.length ? `• ${p.tags.join(", ")}` : ""}
                              </div>
                            </div>
                          </div>
                          {p.id === productId && (
                            <span className="text-xs font-bold text-indigo-600">Selected</span>
                          )}
                        </button>
                      ))}
                  </div>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Comparison Matrix Cards */}
      {activeProducts.length > 0 && (
        <div className="space-y-4">
          {/* Brand Row */}
          {(!showOnlyDifferences || diffAnalysis.diffs["Brand"]) && (
            <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
              <div className="px-4 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <span className="font-bold text-sm text-slate-800">Brand</span>
                {diffAnalysis.diffs["Brand"] && (
                  <span className="px-2 py-0.5 rounded-md border border-amber-200 bg-amber-50 text-amber-700 text-xs font-medium">
                    Different
                  </span>
                )}
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-slate-100 p-4">
                {activeProducts.map((p) => (
                  <div key={p.id} className="p-2">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                      {p.name}
                    </div>
                    <div className="text-sm font-semibold text-slate-900">{p.brand}</div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Tags Row */}
          {(!showOnlyDifferences || diffAnalysis.diffs["Tags"]) && (
            <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
              <div className="px-4 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <span className="font-bold text-sm text-slate-800">Tags</span>
                {diffAnalysis.diffs["Tags"] && (
                  <span className="px-2 py-0.5 rounded-md border border-amber-200 bg-amber-50 text-amber-700 text-xs font-medium">
                    Different
                  </span>
                )}
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-slate-100 p-4">
                {activeProducts.map((p) => (
                  <div key={p.id} className="p-2">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1.5">
                      {p.name}
                    </div>
                    <div className="flex flex-wrap gap-1.5">
                      {p.tags && p.tags.length > 0 ? (
                        p.tags.map((t) => (
                          <span
                            key={t}
                            className="px-2 py-0.5 rounded-md bg-slate-100 text-slate-700 text-xs font-medium"
                          >
                            {t}
                          </span>
                        ))
                      ) : (
                        <span className="text-xs text-slate-400 italic">No tags</span>
                      )}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Overview Row */}
          {(!showOnlyDifferences || diffAnalysis.diffs["Overview"]) && (
            <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
              <div className="px-4 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
                <span className="font-bold text-sm text-slate-800">Overview</span>
                {diffAnalysis.diffs["Overview"] && (
                  <span className="px-2 py-0.5 rounded-md border border-amber-200 bg-amber-50 text-amber-700 text-xs font-medium">
                    Different
                  </span>
                )}
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-slate-100 p-4">
                {activeProducts.map((p) => (
                  <div key={p.id} className="p-2">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider mb-1">
                      {p.name}
                    </div>
                    <div className="text-xs text-slate-700 leading-relaxed whitespace-pre-line">
                      {p.overview || <span className="text-slate-400 italic">No overview provided.</span>}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Pictures Row */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
            <div className="px-4 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
              <span className="font-bold text-sm text-slate-800">Pictures</span>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-slate-100 p-4">
              {activeProducts.map((p) => {
                const pics = p.pictures || [];
                return (
                  <div key={p.id} className="p-2 space-y-2">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      {p.name}
                    </div>
                    {pics.length === 0 ? (
                      <div className="text-xs text-slate-400 italic">No pictures attached</div>
                    ) : (
                      <div className="grid grid-cols-2 gap-2">
                        {pics.map((pic, picIdx) => (
                          <div
                            key={pic.id || picIdx}
                            className="relative group rounded-xl overflow-hidden border border-slate-200 bg-slate-100 aspect-video"
                          >
                            <img
                              src={pic.url || getProductPhotoFallback(p.name, p.brand, p.tags)}
                              alt={pic.name || "Product picture"}
                              className="w-full h-full object-cover"
                              onError={(e) => {
                                (e.target as HTMLImageElement).src = getProductPhotoFallback(
                                  p.name,
                                  p.brand,
                                  p.tags
                                );
                              }}
                            />
                            {onOpenFullscreenImage && (
                              <button
                                onClick={() => onOpenFullscreenImage(pics, picIdx)}
                                className="absolute top-1.5 right-1.5 p-1 bg-black/70 hover:bg-black text-white rounded-lg opacity-0 group-hover:opacity-100 transition-opacity"
                                title="View full screen"
                              >
                                <Maximize2 className="w-3 h-3" />
                              </button>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* User Reviews Row */}
          <div className="bg-white rounded-2xl border border-slate-200 shadow-2xs overflow-hidden">
            <div className="px-4 py-3 bg-slate-50/80 border-b border-slate-100 flex items-center justify-between">
              <span className="font-bold text-sm text-slate-800">User Reviews</span>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 divide-y md:divide-y-0 md:divide-x divide-slate-100 p-4">
              {activeProducts.map((p) => {
                const linkedReviews = reviews.filter(
                  (r) =>
                    (r.productIds && r.productIds.includes(p.id)) ||
                    (r.productTitles && r.productTitles.includes(p.name))
                );

                return (
                  <div key={p.id} className="p-2 space-y-2">
                    <div className="text-[10px] font-bold text-slate-400 uppercase tracking-wider">
                      {p.name}
                    </div>
                    {linkedReviews.length === 0 ? (
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-slate-400 italic">No linked reviews</span>
                        {onOpenAddReview && (
                          <button
                            type="button"
                            onClick={() => onOpenAddReview(p)}
                            className="px-2.5 py-1 rounded-lg border border-dashed border-indigo-300 bg-indigo-50/50 hover:bg-indigo-100 text-indigo-700 text-xs font-semibold flex items-center gap-1 transition-all cursor-pointer"
                          >
                            <Plus className="w-3 h-3" />
                            <span>Write</span>
                          </button>
                        )}
                      </div>
                    ) : (
                      <div className="flex items-center justify-between">
                        <span className="text-xs text-slate-600 font-medium">
                          {linkedReviews.length} {linkedReviews.length === 1 ? "review" : "reviews"}
                        </span>
                        {onOpenViewReview && (
                          <button
                            type="button"
                            onClick={() => onOpenViewReview(linkedReviews[0])}
                            className="px-2.5 py-1 rounded-lg border border-slate-200 bg-white hover:bg-indigo-50 text-indigo-700 text-xs font-semibold flex items-center gap-1 transition-all cursor-pointer"
                          >
                            <MessageSquare className="w-3 h-3" />
                            <span>Read</span>
                          </button>
                        )}
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          </div>

          {/* Dynamic Specifications Rows with Higher/Lower toggle and Progress Bars */}
          {allSpecNames
            .filter((name) => !showOnlyDifferences || diffAnalysis.diffs[name])
            .map((specName) => {
              const isDiff = diffAnalysis.diffs[specName];

              // Parse numeric details for active products
              const items = activeProducts.map((p) => {
                const spec = specifications.find(
                  (s) =>
                    (s.productId === p.id || s.productTitle === p.name) &&
                    s.specName.toLowerCase() === specName.toLowerCase()
                );
                const details = (spec?.details || "").replace(/<[^>]*>/g, "").trim();
                let hasNumeric = false;
                let numVal = 0;
                let displayUnit = spec?.unit || "";

                // Currency pattern e.g. $599, Rm300
                const currMatch = details.match(/^([$€£¥₹]|Rm|RM)\s*([0-9,]+(?:\.[0-9]+)?)/i);
                if (currMatch) {
                  const n = parseFloat(currMatch[2].replace(/,/g, ""));
                  if (!isNaN(n)) {
                    hasNumeric = true;
                    numVal = n;
                    displayUnit = displayUnit || currMatch[1];
                  }
                } else {
                  const genMatch = details.match(/([0-9,]+(?:\.[0-9]+)?)\s*([a-zA-Z$€£%°/"″]+)?/);
                  if (genMatch) {
                    const n = parseFloat(genMatch[1].replace(/,/g, ""));
                    if (!isNaN(n)) {
                      hasNumeric = true;
                      numVal = n;
                      displayUnit = displayUnit || genMatch[2] || "";
                    }
                  }
                }

                return {
                  product: p,
                  spec,
                  hasNumeric,
                  val: numVal,
                  displayUnit,
                  rawText: details
                };
              });

              const hasAnyNumeric = items.some((it) => it.hasNumeric);
              const numericVals = items.filter((it) => it.hasNumeric).map((it) => it.val);
              const maxVal = numericVals.length > 0 ? Math.max(...numericVals) : 1;
              const currentMode = specModes[specName.toLowerCase()] || "higher";
              const bestVal =
                numericVals.length > 0
                  ? currentMode === "higher"
                    ? Math.max(...numericVals)
                    : Math.min(...numericVals)
                  : 0;

              return (
                <div
                  key={specName}
                  id={`compare-spec-card-${specName.replace(/[^a-zA-Z0-9]/g, "-").toLowerCase()}`}
                  className={`rounded-2xl border transition-all overflow-hidden ${
                    isDiff
                      ? "bg-yellow-50/90 border-yellow-300 ring-1 ring-yellow-300/40 shadow-xs"
                      : "bg-white border-slate-200 shadow-2xs"
                  }`}
                >
                  <div
                    className={`px-4 py-3 border-b flex flex-wrap items-center justify-between gap-2.5 ${
                      isDiff
                        ? "bg-yellow-100/80 border-yellow-200 text-yellow-950"
                        : "bg-slate-50/80 border-slate-100"
                    }`}
                  >
                    <div className="flex items-center gap-2">
                      {hasAnyNumeric && <BarChart2 className="w-4 h-4 text-indigo-600" />}
                      <span className="font-bold text-sm text-slate-900">{specName}</span>
                      {items.find((it) => it.displayUnit)?.displayUnit && (
                        <span className="px-2 py-0.5 rounded-md bg-slate-200/80 text-slate-700 font-mono text-[11px] font-semibold">
                          {items.find((it) => it.displayUnit)?.displayUnit}
                        </span>
                      )}
                    </div>

                    <div className="flex items-center gap-2">
                      {isDiff && (
                        <span className="px-2.5 py-1 rounded-md border border-yellow-500 bg-yellow-300 text-yellow-950 text-xs font-black uppercase tracking-wider flex items-center gap-1 shadow-2xs">
                          <span>Different</span>
                        </span>
                      )}

                      {hasAnyNumeric && (
                        <div className="flex items-center bg-slate-200/70 p-0.5 rounded-lg border border-slate-200">
                          <button
                            type="button"
                            id={`spec-mode-higher-${specName.replace(/[^a-zA-Z0-9]/g, "-").toLowerCase()}`}
                            onClick={() =>
                              setSpecModes((prev) => ({ ...prev, [specName.toLowerCase()]: "higher" }))
                            }
                            className={`px-2.5 py-0.5 text-xs font-semibold rounded-md transition-all cursor-pointer ${
                              currentMode === "higher"
                                ? "bg-white text-slate-900 shadow-2xs"
                                : "text-slate-500 hover:text-slate-900"
                            }`}
                          >
                            Higher
                          </button>
                          <button
                            type="button"
                            id={`spec-mode-lower-${specName.replace(/[^a-zA-Z0-9]/g, "-").toLowerCase()}`}
                            onClick={() =>
                              setSpecModes((prev) => ({ ...prev, [specName.toLowerCase()]: "lower" }))
                            }
                            className={`px-2.5 py-0.5 text-xs font-semibold rounded-md transition-all cursor-pointer ${
                              currentMode === "lower"
                                ? "bg-white text-slate-900 shadow-2xs"
                                : "text-slate-500 hover:text-slate-900"
                            }`}
                          >
                            Lower
                          </button>
                        </div>
                      )}
                    </div>
                  </div>

                  <div className="p-4 space-y-4 divide-y divide-slate-100">
                    {items.map(({ product: p, spec, hasNumeric, val, displayUnit, rawText }) => {
                      const isBest = hasNumeric && val === bestVal && numericVals.length > 1;
                      const pct = hasNumeric ? Math.max(8, Math.min(100, Math.round((val / (maxVal > 0 ? maxVal : 1)) * 100))) : 0;
                      const pics = spec?.pictures || [];

                      return (
                        <div key={p.id} className="pt-3 first:pt-0 space-y-1.5">
                          <div className="flex items-center justify-between text-xs sm:text-sm">
                            <span className="font-semibold text-slate-900 truncate max-w-[55%]">
                              {p.name}
                            </span>
                            <div className="flex items-center gap-1.5 shrink-0">
                              {isBest && (
                                <span className="text-emerald-700 bg-emerald-50 border border-emerald-200 px-1.5 py-0.5 rounded text-[11px] font-bold flex items-center gap-1">
                                  🏆 Best
                                </span>
                              )}
                              <span
                                className={`font-mono text-xs sm:text-sm ${
                                  isBest ? "font-bold text-slate-900" : "text-slate-700"
                                }`}
                              >
                                {hasNumeric
                                  ? `${val} ${displayUnit}`
                                  : rawText
                                  ? rawText
                                  : <span className="text-slate-400 font-normal italic">—</span>}
                              </span>
                            </div>
                          </div>

                          {/* Progress bar */}
                          {hasAnyNumeric && (
                            <div className="w-full bg-slate-100 rounded-full h-3 overflow-hidden p-0.5">
                              {hasNumeric ? (
                                <div
                                  className={`h-full rounded-full transition-all duration-300 ${
                                    isBest ? "bg-emerald-500 shadow-2xs" : "bg-slate-500"
                                  }`}
                                  style={{ width: `${pct}%` }}
                                />
                              ) : (
                                <div className="h-full w-0" />
                              )}
                            </div>
                          )}

                          {/* Spec Pictures thumbnails */}
                          {pics.length > 0 && (
                            <div className="pt-1.5 flex flex-wrap gap-2">
                              {pics.map((sp, spIdx) => (
                                <img
                                  key={sp.id || spIdx}
                                  src={sp.url || getSpecPhotoFallback(specName, spec?.details)}
                                  alt={sp.name || "Spec"}
                                  onClick={() =>
                                    onOpenFullscreenImage && onOpenFullscreenImage(pics, spIdx)
                                  }
                                  className="w-14 h-11 rounded-lg object-cover border border-slate-200 cursor-zoom-in hover:opacity-90"
                                />
                              ))}
                            </div>
                          )}
                        </div>
                      );
                    })}
                  </div>
                </div>
              );
            })}
        </div>
      )}
    </div>
  );
};

export default ComparisonMatrix;
