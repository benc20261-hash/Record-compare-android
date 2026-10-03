import React, { useState, useRef } from "react";
import { Product, Specification, MediaAttachment } from "./types";
import { X, Plus, Upload, Trash2, Check, Loader2 } from "lucide-react";

export interface AddSpecModalProps {
  product?: Product | null;
  allProducts: Product[];
  onClose: () => void;
  onSubmit: (specData: Partial<Specification>) => Promise<Specification | void>;
}

const COMMON_SPEC_PRESETS = [
  { name: "Display", unit: "inch" },
  { name: "Refresh Rate", unit: "Hz" },
  { name: "Battery", unit: "mAh" },
  { name: "Weight", unit: "g" },
  { name: "RAM", unit: "GB" },
  { name: "Storage", unit: "GB" },
  { name: "Processor", unit: "" },
  { name: "Charging Speed", unit: "W" },
  { name: "Main Camera", unit: "MP" },
  { name: "Price", unit: "USD" },
];

export const AddSpecModal: React.FC<AddSpecModalProps> = ({
  product,
  allProducts,
  onClose,
  onSubmit,
}) => {
  const [selectedProductId, setSelectedProductId] = useState(
    product ? product.id : allProducts[0]?.id || ""
  );
  const [specName, setSpecName] = useState("");
  const [details, setDetails] = useState("");
  const [unit, setUnit] = useState("");
  const [pictures, setPictures] = useState<MediaAttachment[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handlePresetClick = (preset: { name: string; unit: string }) => {
    setSpecName(preset.name);
    if (preset.unit) setUnit(preset.unit);
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
    if (!specName.trim() || !details.trim() || isSubmitting) return;

    setIsSubmitting(true);
    try {
      const targetProduct = allProducts.find((p) => p.id === selectedProductId);
      await onSubmit({
        productId: selectedProductId,
        productTitle: targetProduct?.name || "",
        specName: specName.trim(),
        details: details.trim(),
        unit: unit.trim() || undefined,
        order: 99,
        pictures,
      });
      onClose();
    } catch (err) {
      console.error("Failed to add spec:", err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      id="add-spec-modal"
      className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Add Specification</h2>
            <p className="text-xs text-slate-500">Document a technical benchmark metric</p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 block">Product</label>
            <select
              value={selectedProductId}
              onChange={(e) => setSelectedProductId(e.target.value)}
              className="w-full px-3.5 py-2 text-sm bg-slate-50 text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
            >
              {allProducts.map((p) => (
                <option key={p.id} value={p.id}>
                  {p.name} ({p.brand})
                </option>
              ))}
            </select>
          </div>

          <div className="space-y-2">
            <label className="text-xs font-bold text-slate-700 block">Specification Presets</label>
            <div className="flex flex-wrap gap-1.5">
              {COMMON_SPEC_PRESETS.map((preset) => (
                <button
                  key={preset.name}
                  type="button"
                  onClick={() => handlePresetClick(preset)}
                  className={`px-2.5 py-1 text-xs rounded-lg transition-colors cursor-pointer ${
                    specName === preset.name
                      ? "bg-indigo-600 text-white font-semibold"
                      : "bg-slate-100 hover:bg-slate-200 text-slate-700"
                  }`}
                >
                  {preset.name}
                </button>
              ))}
            </div>
          </div>

          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2 space-y-1.5">
              <label className="text-xs font-bold text-slate-700 block">
                Spec Name <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                required
                value={specName}
                onChange={(e) => setSpecName(e.target.value)}
                placeholder="e.g. Battery Capacity"
                className="w-full px-3.5 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
              />
            </div>

            <div className="space-y-1.5">
              <label className="text-xs font-bold text-slate-700 block">Unit</label>
              <input
                type="text"
                value={unit}
                onChange={(e) => setUnit(e.target.value)}
                placeholder="e.g. mAh"
                className="w-full px-3.5 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
              />
            </div>
          </div>

          <div className="space-y-1.5">
            <label className="text-xs font-bold text-slate-700 block">
              Value / Details <span className="text-rose-500">*</span>
            </label>
            <input
              type="text"
              required
              value={details}
              onChange={(e) => setDetails(e.target.value)}
              placeholder="e.g. 5000"
              className="w-full px-3.5 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
            />
          </div>

          {/* Picture Attachments */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-700">Specification Pictures</label>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1 cursor-pointer"
              >
                <Upload className="w-3.5 h-3.5" />
                <span>Upload</span>
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

          <div className="pt-4 border-t border-slate-100 flex items-center justify-end gap-2">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl transition-colors cursor-pointer"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={isSubmitting || !specName.trim() || !details.trim()}
              className="flex items-center gap-1.5 px-5 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-indigo-200 cursor-pointer"
            >
              {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Check className="w-4 h-4" />}
              <span>Add Specification</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};

export default AddSpecModal;
