import React, { useState, useRef } from "react";
import { Product, Specification, MediaAttachment } from "./types";
import { X, Save, Trash2, Upload, Plus, Check, Loader2 } from "lucide-react";

export interface EditProductModalProps {
  product: Product;
  specifications?: Specification[];
  onClose: () => void;
  onNavigateToCompare?: () => void;
  onNavigateToManage?: () => void;
  allExistingTags?: string[];
  onAddNewTag?: (tag: string) => void;
  onUpdateProduct: (productData: Partial<Product>) => Promise<void>;
  onDeleteProduct?: (id: string) => Promise<void>;
}

export const EditProductModal: React.FC<EditProductModalProps> = ({
  product,
  onClose,
  allExistingTags = ["Smartphone", "IT product", "Shoe", "TV", "Audio", "Wearable", "viture"],
  onAddNewTag,
  onUpdateProduct,
  onDeleteProduct,
}) => {
  const [name, setName] = useState(product.name || "");
  const [brand, setBrand] = useState(product.brand || "");
  const [selectedTags, setSelectedTags] = useState<string[]>(product.tags || []);
  const [newTagInput, setNewTagInput] = useState("");
  const [overview, setOverview] = useState(product.overview || "");
  const [pictures, setPictures] = useState<MediaAttachment[]>(product.pictures || []);
  const [isSaving, setIsSaving] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleToggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      setSelectedTags(selectedTags.filter((t) => t !== tag));
    } else {
      setSelectedTags([...selectedTags, tag]);
    }
  };

  const handleAddCustomTag = () => {
    const trimmed = newTagInput.trim();
    if (trimmed) {
      if (!selectedTags.includes(trimmed)) {
        setSelectedTags([...selectedTags, trimmed]);
      }
      if (onAddNewTag) onAddNewTag(trimmed);
      setNewTagInput("");
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || isSaving) return;

    setIsSaving(true);
    try {
      await onUpdateProduct({
        id: product.id,
        name: name.trim(),
        brand: brand.trim(),
        tags: selectedTags,
        overview: overview.trim(),
        pictures,
      });
      onClose();
    } catch (err) {
      console.error("Failed to update product:", err);
    } finally {
      setIsSaving(false);
    }
  };

  return (
    <div
      id="edit-product-modal"
      className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-xl max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Edit Product</h2>
            <p className="text-xs text-slate-500">Update specifications and details for {product.name}</p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 block">Product Title</label>
            <input
              type="text"
              required
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3.5 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
            />
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 block">Brand</label>
            <input
              type="text"
              value={brand}
              onChange={(e) => setBrand(e.target.value)}
              className="w-full px-3.5 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
            />
          </div>

          {/* Tags Selection */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-slate-700 block">Tags</label>
            <div className="flex flex-wrap gap-1.5">
              {allExistingTags.map((tag) => {
                const isSelected = selectedTags.includes(tag);
                return (
                  <button
                    key={tag}
                    type="button"
                    onClick={() => handleToggleTag(tag)}
                    className={`px-2.5 py-1 text-xs rounded-lg transition-all cursor-pointer ${
                      isSelected
                        ? "bg-indigo-600 text-white font-semibold shadow-2xs"
                        : "bg-slate-100 text-slate-700 hover:bg-slate-200"
                    }`}
                  >
                    {isSelected && "✓ "}
                    {tag}
                  </button>
                );
              })}
            </div>

            <div className="flex items-center gap-1.5 pt-1">
              <input
                type="text"
                value={newTagInput}
                onChange={(e) => setNewTagInput(e.target.value)}
                onKeyDown={(e) => e.key === "Enter" && (e.preventDefault(), handleAddCustomTag())}
                placeholder="New tag..."
                className="px-3 py-1 text-xs bg-slate-50 text-slate-900 rounded-lg border border-slate-200 focus:border-indigo-500 outline-none w-36"
              />
              <button
                type="button"
                onClick={handleAddCustomTag}
                className="px-2.5 py-1 bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-lg text-xs font-semibold cursor-pointer"
              >
                Add Tag
              </button>
            </div>
          </div>

          {/* Overview */}
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 block">Overview</label>
            <textarea
              rows={3}
              value={overview}
              onChange={(e) => setOverview(e.target.value)}
              className="w-full px-3.5 py-2 text-xs sm:text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none leading-relaxed"
            />
          </div>

          {/* Pictures */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-700">Pictures ({pictures.length})</label>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer"
              >
                <Upload className="w-3.5 h-3.5" />
                <span>Upload Photos</span>
              </button>
            </div>

            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              multiple
              onChange={handleFileUpload}
              className="hidden"
            />

            {pictures.length > 0 && (
              <div className="grid grid-cols-3 gap-2 pt-1">
                {pictures.map((pic, idx) => (
                  <div
                    key={pic.id || idx}
                    className="relative group rounded-xl overflow-hidden border border-slate-200 bg-slate-100 aspect-video"
                  >
                    <img src={pic.url} alt={pic.name} className="w-full h-full object-cover" />
                    <button
                      type="button"
                      onClick={() => setPictures(pictures.filter((_, i) => i !== idx))}
                      className="absolute top-1 right-1 p-1 bg-rose-600 text-white rounded-md opacity-0 group-hover:opacity-100 transition-opacity"
                    >
                      <Trash2 className="w-3 h-3" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Footer Controls */}
          <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
            {onDeleteProduct ? (
              <button
                type="button"
                onClick={async () => {
                  if (confirm(`Permanently delete "${product.name}"?`)) {
                    await onDeleteProduct(product.id);
                    onClose();
                  }
                }}
                className="px-3.5 py-2 text-xs font-semibold text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer"
              >
                Delete Product
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
                disabled={isSaving || !name.trim()}
                className="flex items-center gap-1.5 px-5 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-indigo-200 cursor-pointer"
              >
                {isSaving ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                <span>Save Changes</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditProductModal;
