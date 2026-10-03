import React, { useState, useEffect, useRef } from "react";
import { Review, Product, MediaAttachment } from "./types";
import {
  X,
  Star,
  Plus,
  Trash2,
  Image as ImageIcon,
  Save,
  ThumbsUp,
  ThumbsDown,
  Loader2
} from "lucide-react";

export interface UnifiedReviewEditorProps {
  isOpen: boolean;
  onClose: () => void;
  products: Product[];
  initialReview?: Review | null;
  initialProductId?: string;
  onSave: (reviewData: Partial<Review>) => Promise<void>;
  onDelete?: (id: string) => Promise<void>;
}

export const UnifiedReviewEditor: React.FC<UnifiedReviewEditorProps> = ({
  isOpen,
  onClose,
  products,
  initialReview,
  initialProductId,
  onSave,
  onDelete,
}) => {
  const [title, setTitle] = useState("");
  const [selectedProductIds, setSelectedProductIds] = useState<string[]>([]);
  const [rating, setRating] = useState<number>(5);
  const [authorName, setAuthorName] = useState("");
  const [pros, setPros] = useState<string[]>([]);
  const [proInput, setProInput] = useState("");
  const [cons, setCons] = useState<string[]>([]);
  const [conInput, setConInput] = useState("");
  const [body, setBody] = useState("");
  const [pictures, setPictures] = useState<MediaAttachment[]>([]);
  const [isSaving, setIsSaving] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (initialReview) {
      setTitle(initialReview.title || "");
      setSelectedProductIds(initialReview.productIds || (initialReview.productId ? [initialReview.productId] : []));
      setRating(initialReview.rating || 5);
      setAuthorName(initialReview.authorName || "");
      setPros(initialReview.pros || []);
      setCons(initialReview.cons || []);
      setBody(initialReview.body || "");
      setPictures(initialReview.pictures || []);
    } else {
      setTitle("");
      setSelectedProductIds(initialProductId ? [initialProductId] : products[0]?.id ? [products[0].id] : []);
      setRating(5);
      setAuthorName("Admin User");
      setPros([]);
      setCons([]);
      setBody("");
      setPictures([]);
    }
  }, [initialReview, initialProductId, products, isOpen]);

  if (!isOpen) return null;

  const handleAddPro = () => {
    if (proInput.trim()) {
      setPros([...pros, proInput.trim()]);
      setProInput("");
    }
  };

  const handleAddCon = () => {
    if (conInput.trim()) {
      setCons([...cons, conInput.trim()]);
      setConInput("");
    }
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    Array.from(files).forEach((file) => {
      const url = URL.createObjectURL(file);
      const newPic: MediaAttachment = {
        id: `att_${Date.now()}_${Math.random().toString(36).substring(2, 7)}`,
        name: file.name,
        url,
        size: file.size,
        mimetype: file.type,
      };
      setPictures((prev) => [...prev, newPic]);
    });
  };

  const handleInsertPhotoInBody = (pic: MediaAttachment) => {
    const markdown = `\n![${pic.name}](${pic.url})\n`;
    setBody((prev) => prev + markdown);
  };

  const handleToggleProduct = (productId: string) => {
    if (selectedProductIds.includes(productId)) {
      setSelectedProductIds(selectedProductIds.filter((id) => id !== productId));
    } else {
      setSelectedProductIds([...selectedProductIds, productId]);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isSaving) return;

    setIsSaving(true);
    try {
      const selectedProducts = products.filter((p) => selectedProductIds.includes(p.id));
      await onSave({
        id: initialReview?.id,
        title: title.trim() || undefined,
        body,
        rating,
        authorName: authorName.trim() || "Verified Reviewer",
        pros: pros.length > 0 ? pros : undefined,
        cons: cons.length > 0 ? cons : undefined,
        productIds: selectedProductIds,
        productTitles: selectedProducts.map((p) => p.name),
        productId: selectedProductIds[0] || "",
        pictures,
      });
      onClose();
    } catch (err) {
      console.error("Failed to save review:", err);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div
      id="unified-review-editor-modal"
      className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-2xl max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div>
            <h2 className="text-lg font-bold text-slate-900">
              {initialReview ? "Edit Product Review" : "Write Product Review"}
            </h2>
            <p className="text-xs text-slate-500">
              Share benchmarks, hands-on notes, pros & cons, or UpNote records
            </p>
          </div>

          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Scrollable Form */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-5">
          {/* Linked Products */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 block">
              Reviewed Products <span className="text-rose-500">*</span>
            </label>
            <div className="flex flex-wrap gap-2 max-h-32 overflow-y-auto p-1 border border-slate-200 rounded-xl bg-slate-50">
              {products.map((p) => {
                const isSelected = selectedProductIds.includes(p.id);
                return (
                  <button
                    key={p.id}
                    type="button"
                    onClick={() => handleToggleProduct(p.id)}
                    className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
                      isSelected
                        ? "bg-indigo-600 text-white shadow-2xs"
                        : "bg-white text-slate-700 border border-slate-200 hover:border-slate-300"
                    }`}
                  >
                    {isSelected && "✓ "}
                    {p.name}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Title and Rating */}
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="sm:col-span-2 space-y-1.5">
              <label className="text-xs font-bold text-slate-700 block">Review Title</label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="e.g. Samsung Fold 8 Camera and Display Benchmark"
                className="w-full px-3.5 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700 block">Rating</label>
              <div className="flex items-center gap-1 py-1.5">
                {[1, 2, 3, 4, 5].map((star) => (
                  <button
                    key={star}
                    type="button"
                    onClick={() => setRating(star)}
                    className="p-1 hover:scale-110 transition-transform cursor-pointer"
                  >
                    <Star
                      className={`w-5 h-5 ${
                        star <= rating
                          ? "fill-amber-400 text-amber-400"
                          : "text-slate-300 hover:text-slate-400"
                      }`}
                    />
                  </button>
                ))}
              </div>
            </div>
          </div>

          {/* Author Name */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 block">Author Name</label>
            <input
              type="text"
              value={authorName}
              onChange={(e) => setAuthorName(e.target.value)}
              placeholder="e.g. TechReviewer or your name"
              className="w-full px-3.5 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
            />
          </div>

          {/* Pros & Cons */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Pros */}
            <div className="space-y-2">
              <label className="text-xs font-bold text-emerald-800 flex items-center gap-1">
                <ThumbsUp className="w-3.5 h-3.5" />
                <span>Pros</span>
              </label>
              <div className="flex gap-1.5">
                <input
                  type="text"
                  value={proInput}
                  onChange={(e) => setProInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), handleAddPro())}
                  placeholder="Add a pro..."
                  className="flex-1 px-3 py-1.5 text-xs bg-slate-50 text-slate-900 rounded-lg border border-slate-200 focus:border-emerald-500 outline-none"
                />
                <button
                  type="button"
                  onClick={handleAddPro}
                  className="px-2.5 py-1.5 bg-emerald-600 text-white rounded-lg text-xs font-bold hover:bg-emerald-700"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {pros.map((p, i) => (
                  <span
                    key={i}
                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-emerald-50 border border-emerald-200 text-emerald-900 text-xs"
                  >
                    <span>{p}</span>
                    <button
                      type="button"
                      onClick={() => setPros(pros.filter((_, idx) => idx !== i))}
                      className="text-emerald-700 hover:text-emerald-900"
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            </div>

            {/* Cons */}
            <div className="space-y-2">
              <label className="text-xs font-bold text-rose-800 flex items-center gap-1">
                <ThumbsDown className="w-3.5 h-3.5" />
                <span>Cons</span>
              </label>
              <div className="flex gap-1.5">
                <input
                  type="text"
                  value={conInput}
                  onChange={(e) => setConInput(e.target.value)}
                  onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), handleAddCon())}
                  placeholder="Add a con..."
                  className="flex-1 px-3 py-1.5 text-xs bg-slate-50 text-slate-900 rounded-lg border border-slate-200 focus:border-rose-500 outline-none"
                />
                <button
                  type="button"
                  onClick={handleAddCon}
                  className="px-2.5 py-1.5 bg-rose-600 text-white rounded-lg text-xs font-bold hover:bg-rose-700"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>
              <div className="flex flex-wrap gap-1.5">
                {cons.map((c, i) => (
                  <span
                    key={i}
                    className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md bg-rose-50 border border-rose-200 text-rose-900 text-xs"
                  >
                    <span>{c}</span>
                    <button
                      type="button"
                      onClick={() => setCons(cons.filter((_, idx) => idx !== i))}
                      className="text-rose-700 hover:text-rose-900"
                    >
                      ×
                    </button>
                  </span>
                ))}
              </div>
            </div>
          </div>

          {/* Body Content */}
          <div className="space-y-1.5">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-700">Review Notes / Markdown</label>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer"
              >
                <ImageIcon className="w-3.5 h-3.5" />
                <span>Insert Photo</span>
              </button>
            </div>
            <textarea
              rows={8}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Paste Markdown review, UpNote notes, comparison tables, or detailed benchmark observations..."
              className="w-full px-3.5 py-2.5 text-xs sm:text-sm font-mono bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none leading-relaxed"
            />
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              onChange={handleFileUpload}
              className="hidden"
            />
          </div>

          {/* Attached Pictures */}
          {pictures.length > 0 && (
            <div className="space-y-2">
              <span className="text-xs font-bold text-slate-700 block">Attached Pictures</span>
              <div className="grid grid-cols-3 sm:grid-cols-4 gap-2">
                {pictures.map((pic, idx) => (
                  <div
                    key={pic.id || idx}
                    className="relative group rounded-xl overflow-hidden border border-slate-200 bg-slate-100 aspect-video"
                  >
                    <img src={pic.url} alt={pic.name} className="w-full h-full object-cover" />
                    <div className="absolute inset-0 bg-black/50 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-1">
                      <button
                        type="button"
                        onClick={() => handleInsertPhotoInBody(pic)}
                        className="p-1 bg-white/20 hover:bg-white text-white hover:text-indigo-600 rounded-md text-[10px]"
                        title="Insert into text"
                      >
                        Insert
                      </button>
                      <button
                        type="button"
                        onClick={() => setPictures(pictures.filter((_, i) => i !== idx))}
                        className="p-1 bg-white/20 hover:bg-white text-white hover:text-rose-600 rounded-md"
                        title="Remove"
                      >
                        <Trash2 className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* Footer Controls */}
          <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
            {initialReview && onDelete ? (
              <button
                type="button"
                onClick={async () => {
                  if (confirm("Delete this review permanently?")) {
                    await onDelete(initialReview.id);
                    onClose();
                  }
                }}
                className="px-3.5 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer"
              >
                Delete Review
              </button>
            ) : (
              <div />
            )}

            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={onClose}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
              >
                Cancel
              </button>

              <button
                type="submit"
                disabled={isSaving || selectedProductIds.length === 0}
                className="flex items-center gap-1.5 px-5 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-indigo-200 cursor-pointer"
              >
                {isSaving ? (
                  <Loader2 className="w-4 h-4 animate-spin" />
                ) : (
                  <Save className="w-4 h-4" />
                )}
                <span>Save Review</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default UnifiedReviewEditor;
