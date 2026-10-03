export interface MediaAttachment {
  id: string;
  name: string;
  url: string;
  dataUrl?: string;
  size?: number;
  mimetype?: string;
  width?: number;
  height?: number;
}

export interface Product {
  id: string;
  name: string;
  brand: string;
  tags: string[];
  overview: string;
  pictures: MediaAttachment[];
  specificationIds?: string[];
  reviewIds?: string[];
  createdTime?: string;
}

export interface Specification {
  id: string;
  specName: string;
  details: string;
  order: number;
  productId: string;
  productTitle?: string;
  unit?: string;
  pictures: MediaAttachment[];
}

export interface Review {
  id: string;
  reviewNumber: number;
  title?: string;
  body: string;
  pros?: string[];
  cons?: string[];
  authorName?: string;
  authorAvatar?: string;
  rating?: number;
  productId?: string;
  productIds: string[];
  productTitles: string[];
  pictures: MediaAttachment[];
  createdTime?: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
  role: 'Admin' | 'User';
}

export interface DatabaseState {
  shareId?: string;
  baseId?: string;
  sourceUrl?: string;
  lastSynced: string;
  lastPushed?: string;
  products: Product[];
  specifications: Specification[];
  reviews: Review[];
  users: User[];
}

export type ViewType = 'grid' | 'table' | 'compare' | 'smart-add' | 'reviews' | 'database';
