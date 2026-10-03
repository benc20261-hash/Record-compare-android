import React, { useMemo } from "react";

export interface FormattedReviewContentProps {
  content: string;
  className?: string;
  onImageClick?: (imageUrl: string) => void;
}

export function sanitizeAndFormatHtml(htmlContent: string): string {
  if (!htmlContent) return "";
  let text = htmlContent;

  // Unescape encoded HTML if present
  if (
    (text.includes("&lt;h") || text.includes("&lt;p") || text.includes("&lt;div") || text.includes("&lt;table")) &&
    !text.includes("<h") &&
    !text.includes("<p")
  ) {
    if (typeof document !== "undefined") {
      const textarea = document.createElement("textarea");
      textarea.innerHTML = text;
      text = textarea.value;
    }
  }

  if (typeof DOMParser === "undefined") return text;

  try {
    const doc = new DOMParser().parseFromString(text, "text/html");

    // Remove potentially dangerous tags
    doc.querySelectorAll("script, iframe, object, embed, applet, base, form, input, button").forEach((el) => el.remove());

    // Remove inline event handlers
    doc.querySelectorAll("*").forEach((el) => {
      const attrs = Array.from(el.attributes);
      for (const attr of attrs) {
        if (attr.name.toLowerCase().startsWith("on") || attr.value.toLowerCase().includes("javascript:")) {
          el.removeAttribute(attr.name);
        }
      }
    });

    // Style tables responsively
    doc.querySelectorAll("table").forEach((table) => {
      if (!table.parentElement?.classList.contains("overflow-x-auto")) {
        const wrapper = doc.createElement("div");
        wrapper.className = "w-full overflow-x-auto my-4 rounded-xl border border-slate-200 shadow-2xs bg-white";
        table.parentNode?.insertBefore(wrapper, table);
        wrapper.appendChild(table);
      }
      table.classList.add("w-full", "min-w-[480px]", "border-collapse", "text-sm");
      table.querySelectorAll("th").forEach((th) => {
        th.classList.add("border-b", "border-r", "last:border-r-0", "border-slate-200", "px-3", "py-2.5", "bg-slate-50", "font-bold", "text-slate-800", "text-left", "whitespace-nowrap");
      });
      table.querySelectorAll("td").forEach((td) => {
        td.classList.add("border-b", "border-r", "last:border-r-0", "border-slate-200", "px-3", "py-2", "text-slate-700", "align-top");
      });
    });

    // Style typography
    doc.querySelectorAll("h1").forEach((h) => h.classList.add("text-2xl", "font-black", "text-slate-900", "mt-6", "mb-3", "pb-2", "border-b", "border-slate-200"));
    doc.querySelectorAll("h2").forEach((h) => h.classList.add("text-xl", "font-bold", "text-slate-900", "mt-5", "mb-2.5"));
    doc.querySelectorAll("h3").forEach((h) => h.classList.add("text-lg", "font-bold", "text-slate-900", "mt-4", "mb-2"));
    doc.querySelectorAll("p").forEach((p) => p.classList.add("mb-3", "leading-relaxed", "text-slate-700", "text-sm"));
    doc.querySelectorAll("ul").forEach((ul) => ul.classList.add("list-disc", "pl-5", "my-3", "space-y-1", "text-slate-700"));
    doc.querySelectorAll("ol").forEach((ol) => ol.classList.add("list-decimal", "pl-5", "my-3", "space-y-1", "text-slate-700"));
    doc.querySelectorAll("blockquote").forEach((b) => b.classList.add("border-l-4", "border-indigo-500", "pl-4", "py-1.5", "my-3", "bg-indigo-50/50", "rounded-r-lg", "text-slate-700", "italic"));
    doc.querySelectorAll("pre").forEach((pre) => pre.classList.add("overflow-x-auto", "p-3", "my-3", "bg-slate-900", "text-slate-100", "rounded-xl", "text-xs", "font-mono"));

    // Style images
    doc.querySelectorAll("img").forEach((img) => {
      img.classList.add("w-full", "rounded-2xl", "border", "border-slate-200", "shadow-2xs", "object-cover", "my-4", "max-w-2xl", "cursor-zoom-in");
      img.setAttribute("loading", "lazy");
      img.setAttribute("referrerpolicy", "no-referrer");
    });

    return doc.body.innerHTML;
  } catch (err) {
    console.warn("DOMParser error:", err);
    return text;
  }
}

export const FormattedReviewContent: React.FC<FormattedReviewContentProps> = ({
  content,
  className = "",
  onImageClick,
}) => {
  const formattedHtml = useMemo(() => sanitizeAndFormatHtml(content), [content]);

  const handleClick = (e: React.MouseEvent<HTMLDivElement>) => {
    const target = e.target as HTMLElement;
    if (target.tagName === "IMG" && onImageClick) {
      const src = (target as HTMLImageElement).src;
      if (src) onImageClick(src);
    }
  };

  return (
    <div
      className={`review-formatted-body text-slate-800 leading-relaxed text-sm ${className}`}
      dangerouslySetInnerHTML={{ __html: formattedHtml }}
      onClick={handleClick}
    />
  );
};

export default FormattedReviewContent;
