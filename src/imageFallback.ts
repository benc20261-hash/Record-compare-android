// SVG fallback generator for devices, specification images, and empty states

function escapeXml(unsafe: string): string {
  return (unsafe || "").replace(/[<>&'"]/g, (c) => {
    switch (c) {
      case "<": return "&lt;";
      case ">": return "&gt;";
      case "&": return "&amp;";
      case "'": return "&apos;";
      case '"': return "&quot;";
      default: return c;
    }
  });
}

export function detectDeviceCategory(title = "", brand = "", tags: string[] = []): string {
  const query = `${title} ${brand} ${tags.join(" ")}`.toLowerCase();
  if (query.includes("mic") || query.includes("boya") || query.includes("dji")) return "mic";
  if (query.includes("glasses") || query.includes("viture") || query.includes("ar") || query.includes("xr")) return "glasses";
  if (query.includes("laptop") || query.includes("macbook") || query.includes("asus") || query.includes("pc") || query.includes("notebook")) return "laptop";
  if (query.includes("watch") || query.includes("wearable")) return "watch";
  if (query.includes("headphone") || query.includes("headset") || query.includes("earphone") || query.includes("wh-")) return "headphones";
  if (query.includes("phone") || query.includes("iphone") || query.includes("samsung") || query.includes("fold") || query.includes("galaxy") || query.includes("mobile")) return "phone";
  return "general";
}

export function getDeviceGraphicSvg(category: string): string {
  switch (category) {
    case "phone":
      return `
        <!-- Smartphone Graphic -->
        <g transform="translate(255, 65)">
          <rect x="0" y="0" width="90" height="145" rx="16" fill="#1E1B4B" stroke="#4338CA" stroke-width="3"/>
          <rect x="5" y="6" width="80" height="133" rx="12" fill="#312E81"/>
          <!-- Dynamic Island / Camera Pill -->
          <rect x="33" y="10" width="24" height="6" rx="3" fill="#0F172A"/>
          <!-- Screen Gradient Art -->
          <rect x="10" y="24" width="70" height="98" rx="8" fill="url(#phoneScreenGrad)"/>
          <line x1="32" y1="132" x2="58" y2="132" stroke="#6366F1" stroke-width="2.5" stroke-linecap="round"/>
        </g>`;
    case "laptop":
      return `
        <!-- Laptop Graphic -->
        <g transform="translate(230, 75)">
          <!-- Screen Frame -->
          <rect x="15" y="0" width="110" height="75" rx="6" fill="#1E293B" stroke="#475569" stroke-width="3"/>
          <rect x="20" y="5" width="100" height="65" rx="3" fill="url(#laptopScreenGrad)"/>
          <!-- WebCam -->
          <circle cx="70" cy="3" r="1.5" fill="#94A3B8"/>
          <!-- Keyboard Base -->
          <path d="M0 75 L140 75 L130 92 L10 92 Z" fill="#334155" stroke="#475569" stroke-width="2"/>
          <!-- Trackpad -->
          <rect x="54" y="80" width="32" height="9" rx="2" fill="#1E293B"/>
        </g>`;
    case "mic":
      return `
        <!-- Wireless Mic Graphic -->
        <g transform="translate(260, 68)">
          <!-- Transmitter Body -->
          <rect x="0" y="0" width="80" height="110" rx="12" fill="#0F172A" stroke="#334155" stroke-width="3"/>
          <!-- Mic Grille Top -->
          <rect x="20" y="-12" width="40" height="16" rx="6" fill="#1E293B" stroke="#475569" stroke-width="2"/>
          <line x1="28" y1="-4" x2="52" y2="-4" stroke="#94A3B8" stroke-width="2"/>
          <!-- OLED Display -->
          <rect x="12" y="18" width="56" height="38" rx="4" fill="#020617"/>
          <line x1="18" y1="30" x2="46" y2="30" stroke="#10B981" stroke-width="3" stroke-linecap="round"/>
          <line x1="18" y1="42" x2="36" y2="42" stroke="#38BDF8" stroke-width="2.5" stroke-linecap="round"/>
          <circle cx="58" cy="37" r="3" fill="#EF4444"/>
          <!-- Brand Badge -->
          <rect x="25" y="72" width="30" height="14" rx="4" fill="#1E293B"/>
          <text x="40" y="82" font-family="sans-serif" font-size="8" font-weight="700" fill="#E2E8F0" text-anchor="middle">MIC</text>
        </g>`;
    case "glasses":
      return `
        <!-- Smart Glasses Graphic -->
        <g transform="translate(210, 85)">
          <rect x="15" y="10" width="65" height="48" rx="10" fill="#1E293B" stroke="#6366F1" stroke-width="3"/>
          <rect x="20" y="15" width="55" height="38" rx="6" fill="url(#glassesLensGrad)"/>
          <path d="M80 24 Q90 18 100 24" fill="none" stroke="#6366F1" stroke-width="4" stroke-linecap="round"/>
          <rect x="100" y="10" width="65" height="48" rx="10" fill="#1E293B" stroke="#6366F1" stroke-width="3"/>
          <rect x="105" y="15" width="55" height="38" rx="6" fill="url(#glassesLensGrad)"/>
          <rect x="35" y="26" width="25" height="16" rx="2" fill="#06B6D4" fill-opacity="0.3" stroke="#22D3EE" stroke-width="1.5"/>
          <rect x="120" y="26" width="25" height="16" rx="2" fill="#06B6D4" fill-opacity="0.3" stroke="#22D3EE" stroke-width="1.5"/>
          <line x1="15" y1="18" x2="-8" y2="8" stroke="#4338CA" stroke-width="4" stroke-linecap="round"/>
          <line x1="165" y1="18" x2="188" y2="8" stroke="#4338CA" stroke-width="4" stroke-linecap="round"/>
        </g>`;
    case "watch":
      return `
        <!-- Smartwatch Graphic -->
        <g transform="translate(265, 55)">
          <rect x="18" y="0" width="34" height="28" rx="4" fill="#334155"/>
          <rect x="18" y="125" width="34" height="28" rx="4" fill="#334155"/>
          <rect x="0" y="24" width="70" height="85" rx="18" fill="#0F172A" stroke="#475569" stroke-width="3.5"/>
          <rect x="6" y="30" width="58" height="73" rx="13" fill="url(#phoneScreenGrad)"/>
          <rect x="69" y="42" width="5" height="16" rx="2" fill="#F59E0B"/>
          <circle cx="35" cy="66" r="20" fill="none" stroke="#6366F1" stroke-width="2" stroke-dasharray="2 3"/>
          <line x1="35" y1="66" x2="35" y2="54" stroke="#FFFFFF" stroke-width="2" stroke-linecap="round"/>
          <line x1="35" y1="66" x2="45" y2="66" stroke="#F43F5E" stroke-width="1.5" stroke-linecap="round"/>
        </g>`;
    case "headphones":
      return `
        <!-- Headphones Graphic -->
        <g transform="translate(245, 65)">
          <path d="M15 70 C15 15, 95 15, 95 70" fill="none" stroke="#334155" stroke-width="7" stroke-linecap="round"/>
          <path d="M22 65 C22 25, 88 25, 88 65" fill="none" stroke="#6366F1" stroke-width="3.5" stroke-linecap="round"/>
          <rect x="0" y="60" width="22" height="42" rx="11" fill="#1E293B" stroke="#475569" stroke-width="2.5"/>
          <rect x="88" y="60" width="22" height="42" rx="11" fill="#1E293B" stroke="#475569" stroke-width="2.5"/>
          <circle cx="11" cy="81" r="5" fill="#6366F1"/>
          <circle cx="99" cy="81" r="5" fill="#6366F1"/>
        </g>`;
    default:
      return `
        <!-- Generic Device Graphic -->
        <g transform="translate(255, 75)" stroke="#6366F1" stroke-width="3" fill="none" stroke-linecap="round" stroke-linejoin="round">
          <rect x="5" y="5" width="80" height="95" rx="14" fill="#EEF2FF"/>
          <line x1="28" y1="20" x2="62" y2="20" stroke="#818CF8" stroke-width="2.5"/>
          <rect x="18" y="32" width="54" height="42" rx="6" fill="#FFFFFF" stroke="#C7D2FE" stroke-width="2"/>
          <circle cx="45" cy="88" r="4.5" fill="#818CF8"/>
        </g>`;
  }
}

export function getProductPhotoFallback(title = "Product", brand = "", tags: string[] = []): string {
  const safeTitle = title && typeof title === "string" ? title : "Product";
  const safeBrand = brand && typeof brand === "string" ? brand : "";
  const displayTitle = escapeXml(safeTitle.length > 28 ? safeTitle.substring(0, 26) + "…" : safeTitle);
  const displayBrand = escapeXml(safeBrand.toUpperCase());
  const category = detectDeviceCategory(safeTitle, safeBrand, tags);

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 600 450" width="100%" height="100%">
    <defs>
      <linearGradient id="bgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#F8FAFC"/>
        <stop offset="100%" stop-color="#F1F5F9"/>
      </linearGradient>
      <linearGradient id="phoneScreenGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#4F46E5"/>
        <stop offset="50%" stop-color="#7C3AED"/>
        <stop offset="100%" stop-color="#06B6D4"/>
      </linearGradient>
      <linearGradient id="laptopScreenGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#0F172A"/>
        <stop offset="100%" stop-color="#1E293B"/>
      </linearGradient>
      <linearGradient id="glassesLensGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#047857"/>
        <stop offset="100%" stop-color="#065F46"/>
      </linearGradient>
    </defs>
    <rect width="600" height="450" fill="url(#bgGrad)"/>
    <rect x="24" y="24" width="552" height="402" rx="20" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="2"/>
    ${getDeviceGraphicSvg(category)}
    ${displayBrand ? `<rect x="220" y="270" width="160" height="24" rx="6" fill="#F1F5F9" stroke="#E2E8F0" stroke-width="1"/><text x="300" y="286" font-family="system-ui, -apple-system, sans-serif" font-size="11" font-weight="700" fill="#475569" text-anchor="middle" letter-spacing="1.5">${displayBrand}</text>` : ""}
    <text x="300" y="${displayBrand ? "324" : "304"}" font-family="system-ui, -apple-system, sans-serif" font-size="18" font-weight="700" fill="#0F172A" text-anchor="middle">${displayTitle}</text>
    <text x="300" y="${displayBrand ? "350" : "330"}" font-family="system-ui, -apple-system, sans-serif" font-size="12" font-weight="500" fill="#64748B" text-anchor="middle">Ready for photo upload</text>
  </svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

export function getSpecPhotoFallback(specName = "Specification Photo", details = ""): string {
  const safeName = specName && typeof specName === "string" ? specName : "Specification Photo";
  const safeDetails = details && typeof details === "string" ? details : "";
  const displayName = escapeXml(safeName.length > 24 ? safeName.substring(0, 22) + "…" : safeName);
  const displayDetails = escapeXml(safeDetails.length > 30 ? safeDetails.substring(0, 28) + "…" : safeDetails);

  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 300" width="100%" height="100%">
    <defs>
      <linearGradient id="specBgGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#F8FAFC"/>
        <stop offset="100%" stop-color="#F1F5F9"/>
      </linearGradient>
      <linearGradient id="specGridGrad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" stop-color="#6366F1" stop-opacity="0.08"/>
        <stop offset="100%" stop-color="#4F46E5" stop-opacity="0.02"/>
      </linearGradient>
    </defs>
    <rect width="400" height="300" fill="url(#specBgGrad)"/>
    <rect x="14" y="14" width="372" height="272" rx="14" fill="#FFFFFF" stroke="#E2E8F0" stroke-width="1.5"/>
    <rect x="22" y="22" width="356" height="256" rx="10" fill="url(#specGridGrad)"/>
    <line x1="22" y1="85" x2="378" y2="85" stroke="#E2E8F0" stroke-width="1" stroke-dasharray="3 3"/>
    <line x1="22" y1="150" x2="378" y2="150" stroke="#E2E8F0" stroke-width="1" stroke-dasharray="3 3"/>
    <line x1="130" y1="22" x2="130" y2="278" stroke="#E2E8F0" stroke-width="1" stroke-dasharray="3 3"/>
    <line x1="270" y1="22" x2="270" y2="278" stroke="#E2E8F0" stroke-width="1" stroke-dasharray="3 3"/>
    <g transform="translate(160, 58)">
      <rect x="0" y="0" width="80" height="58" rx="12" fill="#EEF2FF" stroke="#6366F1" stroke-width="2"/>
      <circle cx="40" cy="29" r="16" fill="#FFFFFF" stroke="#4F46E5" stroke-width="2"/>
      <circle cx="40" cy="29" r="7" fill="#6366F1"/>
      <rect x="12" y="-6" width="22" height="6" rx="2" fill="#4F46E5"/>
      <circle cx="62" cy="14" r="3" fill="#F59E0B"/>
    </g>
    <rect x="135" y="138" width="130" height="22" rx="6" fill="#F1F5F9" stroke="#E2E8F0" stroke-width="1"/>
    <text x="200" y="153" font-family="system-ui, -apple-system, sans-serif" font-size="9" font-weight="700" fill="#475569" text-anchor="middle" letter-spacing="1.2">SPEC PHOTO</text>
    <text x="200" y="188" font-family="system-ui, -apple-system, sans-serif" font-size="13" font-weight="700" fill="#0F172A" text-anchor="middle">${displayName}</text>
    <text x="200" y="212" font-family="system-ui, -apple-system, sans-serif" font-size="11" font-weight="500" fill="#64748B" text-anchor="middle">${displayDetails || "Click to view full screen"}</text>
  </svg>`;
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

export function isInvalidMediaUrl(url?: string): boolean {
  if (!url || typeof url !== "string") return true;
  const trimmed = url.trim();
  return !trimmed || trimmed.includes("storage-private.teable.io") || trimmed.includes("inline-attachment.invalid");
}
