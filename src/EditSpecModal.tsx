import React, { useState, useRef } from "react";
import { Specification, MediaAttachment } from "./types";
import { X, Save, Trash2, Upload, Check, Loader2 } from "lucide-react";

export interface EditSpecModalProps {
  specification: Specification | null;
  onClose: () => void;
  onSubmit: (id: string, specData: Partial<Specification>) => Promise<void>;
  onDelete?: (id: string) => Promise<void>;
}

export const EditSpecModal: React.FC<EditSpecModalProps> = ({
  specification,
  onClose,
  onSubmit,
  onDelete,
}) => {
  if (!specification) return null;

  const [specName, setSpecName] = useState(specification.specName || "");
  const [details, setDetails] = useState(specification.details || "");
  const [unit, setUnit] = useState(specification.unit || "");
  const [pictures, setPictures] = useState<MediaAttachment[]>(specification.pictures || []);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

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
      await onSubmit(specification.id, {
        specName: specName.trim(),
        details: details.trim(),
        unit: unit.trim() || undefined,
        pictures,
      });
      onClose();
    } catch (err) {
      console.error("Failed to update spec:", err);
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div
      id="edit-spec-modal"
      className="fixed inset-0 z-50 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-3 sm:p-6 overflow-y-auto animate-in fade-in duration-150"
      onClick={onClose}
    >
      <div
        className="bg-white rounded-3xl shadow-2xl border border-slate-200 w-full max-w-lg max-h-[90vh] flex flex-col overflow-hidden animate-in zoom-in-95 duration-150"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="px-6 py-4 border-b border-slate-100 flex items-center justify-between bg-slate-50/50">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Edit Specification</h2>
            <p className="text-xs text-slate-500">{specification.productTitle || "Product Spec"}</p>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 text-slate-400 hover:text-slate-600 rounded-xl hover:bg-slate-100 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="flex-1 overflow-y-auto p-6 space-y-4">
          <div className="grid grid-cols-3 gap-3">
            <div className="col-span-2 space-y-1.5">
              <label className="text-xs font-bold text-slate-700 block">Spec Name</label>
              <input
                type="text"
                required
                value={specName}
                onChange={(e) => setSpecName(e.target.value)}
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
            <label className="text-xs font-bold text-slate-700 block">Details / Value</label>
            <input
              type="text"
              required
              value={details}
              onChange={(e) => setDetails(e.target.value)}
              className="w-full px-3.5 py-2 text-sm bg-slate-50 hover:bg-slate-100 focus:bg-white text-slate-900 rounded-xl border border-slate-200 focus:border-indigo-500 outline-none"
            />
          </div>

          {/* Pictures */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="text-xs font-bold text-slate-700">Specification Photos</label>
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

          {/* Footer */}
          <div className="pt-4 border-t border-slate-100 flex items-center justify-between">
            {onDelete ? (
              <button
                type="button"
                onClick={async () => {
                  if (confirm(`Delete specification "${specification.specName}"?`)) {
                    await onDelete(specification.id);
                    onClose();
                  }
                }}
                className="px-3 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50 rounded-xl transition-colors cursor-pointer"
              >
                Delete Spec
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
                disabled={isSubmitting || !specName.trim() || !details.trim()}
                className="flex items-center gap-1.5 px-5 py-2 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold transition-all shadow-md shadow-indigo-200 cursor-pointer"
              >
                {isSubmitting ? <Loader2 className="w-4 h-4 animate-spin" /> : <Save className="w-4 h-4" />}
                <span>Save</span>
              </button>
            </div>
          </div>
        </form>
      </div>
    </div>
  );
};

export default EditSpecModal;
