import React, { useState, useRef } from "react";
import { Product, Specification, MediaAttachment } from "./types";
import {
  Sparkles,
  Plus,
  Trash2,
  Upload,
  ArrowRight,
  Layers,
  Grid,
  Check,
  Loader2
} from "lucide-react";

export interface SmartAddProductProps {
  allProducts: Product[];
  specifications: Specification[];
  onSaveProduct: (productData: Partial<Product>) => Promise<Product>;
  onUpdateProduct?: (id: string, productData: Partial<Product>) => Promise<void>;
  onAddSpecification: (specData: Partial<Specification>) => Promise<Specification>;
  onUpdateSpecification?: (id: string, specData: Partial<Specification>) => Promise<void>;
  onDeleteSpecification?: (id: string) => Promise<void>;
  onReorderSpecifications?: (newSpecs: Specification[]) => Promise<void>;
  onDeleteProduct?: (id: string) => Promise<void>;
  onNavigateToCompare: () => void;
  onNavigateToCatalog: () => void;
  onSyncCloud?: () => void;
  preselectedProductIds?: string[];
  onOpenProductWorkspace?: (product: Product) => void;
  onOpenAddSpec?: (product: Product) => void;
  onOpenEditSpec?: (spec: Specification) => void;
}

interface SpecRow {
  name: string;
  value: string;
  unit: string;
}

const COMMON_TAGS = [
  "Smartphone",
  "IT product",
  "Shoe",
  "TV",
  "Audio",
  "Wearable",
  "viture",
  "Home appliance",
];

const QUICK_UNITS = ["mAh", "Hz", "GB", "W", "inch", "g", "MP", "USD", "mm"];

const DEFAULT_SPEC_SUGGESTIONS = [
  { name: "Display", unit: "inch" },
  { name: "Refresh Rate", unit: "Hz" },
  { name: "Battery", unit: "mAh" },
  { name: "Weight", unit: "g" },
  { name: "RAM", unit: "GB" },
  { name: "Storage", unit: "GB" },
  { name: "Processor", unit: "" },
];

export const SmartAddProduct: React.FC<SmartAddProductProps> = ({
  onSaveProduct,
  onAddSpecification,
  onNavigateToCompare,
  onNavigateToCatalog,
}) => {
  const [name, setName] = useState("");
  const [brand, setBrand] = useState("");
  const [selectedTags, setSelectedTags] = useState<string[]>(["Smartphone"]);
  const [newTagInput, setNewTagInput] = useState("");
  const [overview, setOverview] = useState("");
  const [specs, setSpecs] = useState<SpecRow[]>([
    { name: "Display", value: "", unit: "inch" },
    { name: "Refresh Rate", value: "", unit: "Hz" },
    { name: "Battery", value: "", unit: "mAh" },
  ]);
  const [pictures, setPictures] = useState<MediaAttachment[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleToggleTag = (tag: string) => {
    if (selectedTags.includes(tag)) {
      setSelectedTags(selectedTags.filter((t) => t !== tag));
    } else {
      setSelectedTags([...selectedTags, tag]);
    }
  };

  const handleAddCustomTag = () => {
    if (newTagInput.trim() && !selectedTags.includes(newTagInput.trim())) {
      setSelectedTags([...selectedTags, newTagInput.trim()]);
      setNewTagInput("");
    }
  };

  const handleAddSpecRow = (preset?: { name: string; unit: string }) => {
    setSpecs([
      ...specs,
      {
        name: preset?.name || "",
        value: "",
        unit: preset?.unit || "",
      },
    ]);
  };

  const handleUpdateSpecRow = (index: number, field: keyof SpecRow, value: string) => {
    const updated = [...specs];
    updated[index][field] = value;
    setSpecs(updated);
  };

  const handleRemoveSpecRow = (index: number) => {
    setSpecs(specs.filter((_, i) => i !== index));
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

  const handleSubmit = async (andCompare = false) => {
    if (!name.trim() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      // 1. Create product
      const createdProduct = await onSaveProduct({
        name: name.trim(),
        brand: brand.trim(),
        tags: selectedTags,
        overview: overview.trim(),
        pictures,
      });

      // 2. Add non-empty specs
      for (let i = 0; i < specs.length; i++) {
        const s = specs[i];
        if (s.name.trim() && s.value.trim()) {
          await onAddSpecification({
            productId: createdProduct.id,
            productTitle: createdProduct.name,
            specName: s.name.trim(),
            details: s.value.trim(),
            unit: s.unit.trim() || undefined,
            order: i + 1,
            pictures: [],
          });
        }
      }

      setSuccessBanner(`Product "${createdProduct.name}" added with specifications!`);

      if (andCompare) {
        onNavigateToCompare();
      } else {
        // Reset form for "Save & Add Another"
        setName("");
        setBrand("");
        setOverview("");
        setPictures([]);
        setSpecs([
          { name: "Display", value: "", unit: "inch" },
          { name: "Refresh Rate", value: "", unit: "Hz" },
          { name: "Battery", value: "", unit: "mAh" },
        ]);
        setTimeout(() => setSuccessBanner(null), 4000);
      }
    } catch (err) {
      console.error("Failed to smart add product:", err);
      alert("Failed to save product: " + (err as Error).message);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="space-y-6 max-w-4xl mx-auto animate-in fade-in duration-200">
      {/* Banner */}
      <div className="bg-gradient-to-r from-slate-900 to-indigo-950 text-white rounded-3xl p-6 sm:p-8 shadow-xl relative overflow-hidden">
        <div className="relative z-10 flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2">
              <div className="w-8 h-8 rounded-lg bg-amber-400 text-slate-950 flex items-center justify-center font-black">
                <Sparkles className="w-4 h-4" />
              </div>
              <h1 className="text-xl sm:text-2xl font-black tracking-tight">Smart Add Product</h1>
            </div>
            <p className="text-xs sm:text-sm text-slate-300">
              One-click product intake: define identity, category tags, and benchmark matrix together
            </p>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="button"
              onClick={onNavigateToCatalog}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-semibold backdrop-blur-xs transition-colors cursor-pointer"
            >
              <Grid className="w-3.5 h-3.5" />
              <span>Catalog</span>
            </button>
            <button
              type="button"
              onClick={onNavigateToCompare}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/10 hover:bg-white/20 text-white text-xs font-semibold backdrop-blur-xs transition-colors cursor-pointer"
            >
              <Layers className="w-3.5 h-3.5" />
              <span>Compare</span>
            </button>
          </div>
        </div>

        {successBanner && (
          <div className="mt-4 px-4 py-2.5 bg-emerald-500/20 border border-emerald-400/40 text-emerald-200 rounded-2xl text-xs font-semibold flex items-center gap-2 animate-in fade-in">
            <Check className="w-4 h-4 text-emerald-400" />
            <span>{successBanner}</span>
          </div>
        )}
      </div>

      {/* Main Form */}
      <div className="bg-white rounded-3xl border border-slate-200 shadow-xs p-6 space-y-6">
        {/* Basic Details */}
        <div className="space-y-4">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-900 pb-2 border-b border-slate-100">
            1. Product Identification
          </h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <div className="sm:col-span-2 space-y-1.5">
              <label className="text-xs font-bold text-slate-700 block">
                Product Title <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Motorola Edge 30 Ultra"
                className="w-full px-3.5 py-2.5 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700 block">Brand</label>
              <input
                type="text"
                value={brand}
                onChange={(e) => setBrand(e.target.value)}
                placeholder="e.g. Motorola"
                className="w-full px-3.5 py-2.5 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
              />
            </div>
          </div>

          {/* Tags Selection */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-slate-700 block">Category Tags</label>
            <div className="flex flex-wrap gap-1.5">
              {COMMON_TAGS.map((tag) => {
                const isSelected = selectedTags.includes(tag);
                return (
                  <button
                    key={tag}
                    type="button"
                    onClick={() => handleToggleTag(tag)}
                    className={`px-3 py-1 rounded-xl text-xs font-semibold transition-all cursor-pointer ${
                      isSelected
                        ? "bg-indigo-600 text-white shadow-2xs"
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
                placeholder="Add custom tag..."
                className="px-3 py-1.5 text-xs bg-slate-50 text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none w-44"
              />
              <button
                type="button"
                onClick={handleAddCustomTag}
                className="px-3 py-1.5 bg-slate-200 hover:bg-slate-300 text-slate-800 rounded-xl text-xs font-semibold cursor-pointer"
              >
                Add Tag
              </button>
            </div>
          </div>
        </div>

        {/* Specifications Matrix Builder */}
        <div className="space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-slate-100">
            <div>
              <h2 className="text-sm font-bold uppercase tracking-wider text-slate-900">
                2. Technical Specifications Matrix
              </h2>
              <p className="text-xs text-slate-500">
                Quick-add metrics that populate the side-by-side comparison tables
              </p>
            </div>

            <button
              type="button"
              onClick={() => handleAddSpecRow()}
              className="flex items-center gap-1 px-3 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 rounded-xl text-xs font-semibold transition-colors cursor-pointer"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Add row</span>
            </button>
          </div>

          {/* Quick presets */}
          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar py-1">
            <span className="text-[11px] font-bold text-slate-400 shrink-0">Quick presets:</span>
            {DEFAULT_SPEC_SUGGESTIONS.map((preset) => (
              <button
                key={preset.name}
                type="button"
                onClick={() => handleAddSpecRow(preset)}
                className="px-2.5 py-1 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-lg text-xs font-medium shrink-0 cursor-pointer"
              >
                + {preset.name}
              </button>
            ))}
          </div>

          {/* Specs Rows */}
          <div className="space-y-2.5">
            {specs.map((row, idx) => (
              <div
                key={idx}
                className="grid grid-cols-1 sm:grid-cols-12 gap-2 p-2.5 bg-slate-50 rounded-2xl border border-slate-200/80 items-center"
              >
                <div className="sm:col-span-5">
                  <input
                    type="text"
                    value={row.name}
                    onChange={(e) => handleUpdateSpecRow(idx, "name", e.target.value)}
                    placeholder="Spec Name (e.g. Battery)"
                    className="w-full px-3 py-1.5 text-xs bg-white text-slate-900 rounded-lg border border-slate-200 focus:border-indigo-500 outline-none"
                  />
                </div>

                <div className="sm:col-span-4">
                  <input
                    type="text"
                    value={row.value}
                    onChange={(e) => handleUpdateSpecRow(idx, "value", e.target.value)}
                    placeholder="Value (e.g. 5000)"
                    className="w-full px-3 py-1.5 text-xs bg-white text-slate-900 rounded-lg border border-slate-200 focus:border-indigo-500 outline-none"
                  />
                </div>

                <div className="sm:col-span-2">
                  <input
                    type="text"
                    value={row.unit}
                    onChange={(e) => handleUpdateSpecRow(idx, "unit", e.target.value)}
                    placeholder="Unit (mAh)"
                    className="w-full px-3 py-1.5 text-xs bg-white text-slate-900 rounded-lg border border-slate-200 focus:border-indigo-500 outline-none"
                  />
                </div>

                <div className="sm:col-span-1 flex justify-end">
                  <button
                    type="button"
                    onClick={() => handleRemoveSpecRow(idx)}
                    className="p-1.5 text-slate-400 hover:text-rose-600 rounded-lg hover:bg-rose-50 transition-colors"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Quick Units Chips */}
          <div className="flex items-center gap-1.5 flex-wrap pt-1 text-xs text-slate-500">
            <span className="font-semibold text-slate-400">Quick unit:</span>
            {QUICK_UNITS.map((u) => (
              <span key={u} className="px-1.5 py-0.5 bg-slate-100 text-slate-600 rounded text-[11px] font-mono">
                {u}
              </span>
            ))}
          </div>
        </div>

        {/* Overview & Pictures */}
        <div className="space-y-4">
          <h2 className="text-sm font-bold uppercase tracking-wider text-slate-900 pb-2 border-b border-slate-100">
            3. Overview & Media
          </h2>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 block">Overview Markdown</label>
            <textarea
              rows={3}
              value={overview}
              onChange={(e) => setOverview(e.target.value)}
              placeholder="Highlight standout hardware, chipsets, or camera benchmarks..."
              className="w-full px-3.5 py-2.5 text-xs sm:text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none leading-relaxed"
            />
          </div>

          {/* Picture Attachments */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-700">Photos ({pictures.length})</label>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer"
              >
                <Upload className="w-3.5 h-3.5" />
                <span>Choose pictures</span>
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
              <div className="grid grid-cols-4 sm:grid-cols-6 gap-2">
                {pictures.map((pic, idx) => (
                  <div
                    key={pic.id || idx}
                    className="relative group rounded-xl overflow-hidden border border-slate-200 bg-slate-100 aspect-square"
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
        </div>

        {/* Submit Actions */}
        <div className="pt-6 border-t border-slate-100 flex flex-col sm:flex-row items-stretch sm:items-center justify-end gap-3">
          <button
            type="button"
            disabled={isSubmitting || !name.trim()}
            onClick={() => handleSubmit(false)}
            className="flex items-center justify-center gap-2 px-5 py-2.5 rounded-xl border border-slate-300 hover:border-indigo-300 bg-white hover:bg-slate-50 text-slate-700 text-xs font-bold transition-all cursor-pointer disabled:opacity-50"
          >
            {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Plus className="w-4 h-4" />}
            <span>Save & Add Another</span>
          </button>

          <button
            type="button"
            disabled={isSubmitting || !name.trim()}
            onClick={() => handleSubmit(true)}
            className="flex items-center justify-center gap-2 px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-indigo-200 cursor-pointer"
          >
            {isSubmitting ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <ArrowRight className="w-4 h-4" />
            )}
            <span>Save & Compare</span>
          </button>
        </div>
      </div>
    </div>
  );
};

export default SmartAddProduct;
