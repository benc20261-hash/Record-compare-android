import React from "react";

export interface MarkdownContentProps {
  content: string;
  className?: string;
  onImageClick?: (imageUrl: string) => void;
}

export const MarkdownContent: React.FC<MarkdownContentProps> = ({
  content,
  className = "",
  onImageClick,
}) => {
  if (!content) return null;

  // Simple clean markdown parser for standard elements
  const renderFormattedText = (raw: string) => {
    // Process paragraphs and line breaks
    const lines = raw.split("\n");
    return lines.map((line, idx) => {
      // Heading 1
      if (line.startsWith("# ")) {
        return (
          <h1 key={idx} className="text-xl sm:text-2xl font-black text-slate-900 mt-5 mb-2 pb-1 border-b border-slate-200">
            {line.replace(/^#\s+/, "")}
          </h1>
        );
      }
      // Heading 2
      if (line.startsWith("## ")) {
        return (
          <h2 key={idx} className="text-lg sm:text-xl font-bold text-slate-900 mt-4 mb-2">
            {line.replace(/^##\s+/, "")}
          </h2>
        );
      }
      // Heading 3
      if (line.startsWith("### ")) {
        return (
          <h3 key={idx} className="text-base sm:text-lg font-bold text-slate-900 mt-3 mb-1.5">
            {line.replace(/^###\s+/, "")}
          </h3>
        );
      }
      // Blockquote
      if (line.startsWith("> ")) {
        return (
          <blockquote key={idx} className="border-l-4 border-indigo-500 pl-3 py-1 my-2 bg-indigo-50/50 rounded-r-lg text-slate-700 italic text-sm">
            {line.replace(/^>\s+/, "")}
          </blockquote>
        );
      }
      // Markdown image: ![alt](url)
      const imgMatch = line.match(/!\[(.*?)\]\((.*?)\)/);
      if (imgMatch) {
        const alt = imgMatch[1];
        const src = imgMatch[2];
        return (
          <div key={idx} className="my-3">
            <img
              src={src}
              alt={alt || "Review image"}
              onClick={() => onImageClick && onImageClick(src)}
              className="max-w-full rounded-xl border border-slate-200 shadow-2xs cursor-zoom-in object-cover"
              loading="lazy"
            />
          </div>
        );
      }
      // List items
      if (line.match(/^[-*+]\s+/)) {
        return (
          <li key={idx} className="ml-4 list-disc text-sm text-slate-700 leading-relaxed">
            {line.replace(/^[-*+]\s+/, "")}
          </li>
        );
      }
      // Blank lines
      if (!line.trim()) {
        return <div key={idx} className="h-2" />;
      }
      // Paragraph
      return (
        <p key={idx} className="text-sm text-slate-700 leading-relaxed mb-2">
          {line}
        </p>
      );
    });
  };

  return (
    <div className={`prose prose-slate max-w-none text-slate-800 ${className}`}>
      {renderFormattedText(content)}
    </div>
  );
};

export default MarkdownContent;
