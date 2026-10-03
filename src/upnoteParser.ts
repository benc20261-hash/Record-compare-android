// Utility for parsing notes, markdown, and HTML content from UpNote or external sources

export interface ParsedUpnoteContent {
  title: string;
  sanitizedHtml: string;
  plainText: string;
  imageUrls: string[];
  upnoteShareUrl?: string;
}

export function extractUpnoteShareUrl(content: string): string | undefined {
  if (!content) return undefined;
  const match = content.match(/https:\/\/getupnote\.com\/share\/notes\/[a-zA-Z0-9_\-]+(?:\/[a-zA-Z0-9_\-]+)?/i);
  return match ? match[0] : undefined;
}

export function stripMarkdownFormatting(markdown: string, maxLength?: number): string {
  if (!markdown) return "";
  let text = markdown
    .replace(/^#+\s+(.*)$/gm, "$1")
    .replace(/\*\*([^*]+)\*\*/g, "$1")
    .replace(/\*([^*]+)\*/g, "$1")
    .replace(/__([^_]+)__/g, "$1")
    .replace(/_([^_]+)_/g, "$1")
    .replace(/~~([^~]+)~~/g, "$1")
    .replace(/==([^=]+)==/g, "$1")
    .replace(/!\[.*?\]\(.*?\)/g, "")
    .replace(/\[([^\]]+)\]\(.*?\)/g, "$1")
    .replace(/^\s*[-*+]\s+/gm, "")
    .replace(/^\s*\d+\.\s+/gm, "")
    .replace(/\|/g, " ")
    .replace(/[-:]{3,}/g, " ")
    .replace(/\s+/g, " ")
    .trim();

  if (maxLength && text.length > maxLength) {
    return text.slice(0, maxLength).trim() + "...";
  }
  return text;
}

export function parseUpnoteContent(rawText: string): ParsedUpnoteContent {
  if (!rawText) {
    return {
      title: "",
      sanitizedHtml: "",
      plainText: "",
      imageUrls: [],
    };
  }

  let text = rawText;
  // If HTML encoded
  if ((text.includes("&lt;h") || text.includes("&lt;p") || text.includes("&lt;div") || text.includes("&lt;table")) && !text.includes("<h") && !text.includes("<p")) {
    if (typeof document !== "undefined") {
      const textarea = document.createElement("textarea");
      textarea.innerHTML = text;
      text = textarea.value;
    }
  }

  const imageUrls: string[] = [];
  // Extract markdown images: ![alt](url)
  const mdImgMatches = text.matchAll(/!\[.*?\]\((.*?)\)/g);
  for (const m of mdImgMatches) {
    if (m[1]) imageUrls.push(m[1]);
  }

  // Extract HTML img tags: <img src="url" .../>
  const htmlImgMatches = text.matchAll(/<img[^>]+src=["']([^"']+)["']/gi);
  for (const m of htmlImgMatches) {
    if (m[1]) imageUrls.push(m[1]);
  }

  // Extract title from first heading or first line
  let title = "";
  const headingMatch = text.match(/^#\s+(.+)$/m) || text.match(/<h[1-3][^>]*>(.*?)<\/h[1-3]>/i);
  if (headingMatch && headingMatch[1]) {
    title = headingMatch[1].replace(/<[^>]+>/g, "").trim();
  } else {
    const firstLine = text.trim().split("\n")[0];
    if (firstLine && firstLine.length <= 80) {
      title = firstLine.replace(/^[#\-*\s]+/, "").trim();
    }
  }

  const plainText = stripMarkdownFormatting(text);
  const upnoteShareUrl = extractUpnoteShareUrl(text);

  return {
    title,
    sanitizedHtml: text,
    plainText,
    imageUrls,
    upnoteShareUrl,
  };
}
