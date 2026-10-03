// Utility for client-side image compression, dimension extraction, and thumbnail generation

export interface ImageCompressionOptions {
  maxWidth?: number;
  maxHeight?: number;
  quality?: number;
  mimeType?: string;
}

export function getImageDimensions(source: File | Blob | string): Promise<{ width: number; height: number }> {
  return new Promise((resolve) => {
    const img = new Image();
    let objectUrl: string | null = null;

    img.onload = () => {
      resolve({
        width: img.naturalWidth || img.width,
        height: img.naturalHeight || img.height,
      });
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };

    img.onerror = () => {
      resolve({ width: 0, height: 0 });
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };

    if (typeof source === "string") {
      img.src = source;
    } else {
      objectUrl = URL.createObjectURL(source);
      img.src = objectUrl;
    }
  });
}

export async function compressImage(
  file: File | Blob,
  options: ImageCompressionOptions = {}
): Promise<{ blob: Blob; dataUrl: string; width: number; height: number }> {
  const { maxWidth = 1920, maxHeight = 1080, quality = 0.85, mimeType = "image/jpeg" } = options;

  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = (e) => {
      const img = new Image();
      img.onload = () => {
        let width = img.naturalWidth || img.width;
        let height = img.naturalHeight || img.height;

        if (width > maxWidth || height > maxHeight) {
          const ratio = Math.min(maxWidth / width, maxHeight / height);
          width = Math.round(width * ratio);
          height = Math.round(height * ratio);
        }

        const canvas = document.createElement("canvas");
        canvas.width = width;
        canvas.height = height;

        const ctx = canvas.getContext("2d");
        if (!ctx) {
          reject(new Error("Unable to create canvas context"));
          return;
        }

        // Draw image with smooth scaling
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = "high";
        ctx.drawImage(img, 0, 0, width, height);

        const dataUrl = canvas.toDataURL(mimeType, quality);
        canvas.toBlob(
          (blob) => {
            if (blob) {
              resolve({ blob, dataUrl, width, height });
            } else {
              reject(new Error("Image compression failed"));
            }
          },
          mimeType,
          quality
        );
      };

      img.onerror = () => reject(new Error("Failed to load image for compression"));
      img.src = e.target?.result as string;
    };

    reader.onerror = () => reject(new Error("Failed to read file"));
    reader.readAsDataURL(file);
  });
}

export async function createThumbnail(
  file: File | Blob,
  size = 320
): Promise<{ blob: Blob; dataUrl: string }> {
  return compressImage(file, {
    maxWidth: size,
    maxHeight: size,
    quality: 0.8,
    mimeType: "image/jpeg",
  });
}
