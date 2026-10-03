import React, { useState, useRef, useEffect, useMemo } from "react";
import { ViewType, User } from "./types";
import {
  Layers,
  Sparkles,
  MessageSquare,
  Database,
  Grid,
  Menu,
  ChevronDown,
  Check,
  Plus
} from "lucide-react";

export interface NavbarProps {
  currentView: ViewType;
  setCurrentView: (view: ViewType) => void;
  searchQuery?: string;
  setSearchQuery?: (q: string) => void;
  comparedCount: number;
  lastSynced?: string;
  lastPushed?: string;
  isSyncing?: boolean;
  currentUser?: User | null;
  allUsers?: User[];
  onSelectUser?: (user: User) => void;
  onOpenSyncModal?: () => void;
  onOpenAddProduct?: () => void;
  onOpenFullscreenStudio?: () => void;
}

export const Navbar: React.FC<NavbarProps> = ({
  currentView,
  setCurrentView,
  comparedCount,
  onOpenAddProduct,
}) => {
  const [isMenuOpen, setIsMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handleOutsideClick = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setIsMenuOpen(false);
      }
    };
    document.addEventListener("mousedown", handleOutsideClick);
    return () => document.removeEventListener("mousedown", handleOutsideClick);
  }, []);

  return (
    <header id="main-header" className="sticky top-0 z-50 bg-white/95 backdrop-blur-md border-b border-slate-200 w-full max-w-full">
      <div className="w-full max-w-7xl mx-auto px-3 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-14 sm:h-16 gap-2 sm:gap-4">
          {/* Logo & Subtitle */}
          <div className="flex items-center gap-2 sm:gap-3 shrink-0">
            <button
              id="brand-logo-btn"
              onClick={() => setCurrentView("grid")}
              className="flex items-center gap-2 text-left group cursor-pointer focus:outline-none"
            >
              <div className="w-8 h-8 sm:w-10 sm:h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-blue-500 flex items-center justify-center text-white shadow-sm shadow-indigo-200 shrink-0">
                <Layers className="w-4 h-4 sm:w-5 sm:h-5 transition-transform group-hover:scale-110" />
              </div>
              <div className="min-w-0">
                <div className="flex items-center gap-1.5 sm:gap-2">
                  <span className="font-bold text-slate-900 tracking-tight text-sm sm:text-base md:text-lg truncate">
                    ProductCompare
                  </span>
                  <span className="inline-flex text-[10px] sm:text-[11px] font-semibold bg-indigo-50 text-indigo-700 px-1.5 sm:px-2 py-0.5 rounded-full border border-indigo-100 shrink-0">
                    Live
                  </span>
                </div>
                <p className="text-[11px] text-slate-500 hidden md:block truncate">
                  Side-by-side consumer tech benchmark
                </p>
              </div>
            </button>
          </div>

          {/* Desktop Navigation */}
          <div className="flex items-center gap-1.5 sm:gap-2 shrink-0">
            <div className="hidden lg:flex items-center p-1 bg-slate-100 rounded-lg text-xs font-medium text-slate-600">
              <button
                id="nav-catalog-tab"
                onClick={() => setCurrentView("grid")}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md transition-colors cursor-pointer ${
                  currentView === "grid" || currentView === "table"
                    ? "bg-white text-slate-900 shadow-xs font-semibold"
                    : "hover:text-slate-900"
                }`}
              >
                <Grid className="w-3.5 h-3.5" />
                <span>Catalog</span>
              </button>

              <button
                id="nav-compare-tab"
                onClick={() => setCurrentView("compare")}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md transition-colors relative cursor-pointer ${
                  currentView === "compare"
                    ? "bg-white text-slate-900 shadow-xs font-semibold"
                    : "hover:text-slate-900"
                }`}
              >
                <Layers className="w-3.5 h-3.5" />
                <span>Compare</span>
                {comparedCount > 0 && (
                  <span className="ml-0.5 px-1.5 py-0.2 bg-indigo-600 text-white rounded-full text-[10px] font-bold">
                    {comparedCount}
                  </span>
                )}
              </button>

              <button
                id="nav-smart-add-tab"
                onClick={() => setCurrentView("smart-add")}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md transition-colors cursor-pointer ${
                  currentView === "smart-add"
                    ? "bg-slate-900 text-white shadow-xs font-bold"
                    : "bg-amber-50 hover:bg-amber-100 text-amber-900 font-semibold"
                }`}
                title="Smart Add Product"
              >
                <Sparkles className="w-3.5 h-3.5 text-amber-500" />
                <span>Smart Add Product</span>
              </button>

              <button
                id="nav-reviews-tab"
                onClick={() => setCurrentView("reviews")}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md transition-colors cursor-pointer ${
                  currentView === "reviews"
                    ? "bg-white text-slate-900 shadow-xs font-semibold"
                    : "hover:text-slate-900"
                }`}
              >
                <MessageSquare className="w-3.5 h-3.5" />
                <span>Reviews</span>
              </button>

              <button
                id="nav-database-tab"
                onClick={() => setCurrentView("database")}
                className={`flex items-center gap-1.5 px-3 py-1.5 rounded-md transition-colors cursor-pointer ${
                  currentView === "database"
                    ? "bg-white text-slate-900 shadow-xs font-semibold"
                    : "hover:text-slate-900"
                }`}
              >
                <Database className="w-3.5 h-3.5 text-indigo-600" />
                <span>Data & Backup</span>
              </button>
            </div>

            {onOpenAddProduct && (
              <button
                onClick={onOpenAddProduct}
                className="hidden sm:flex items-center gap-1 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold shadow-xs transition-all cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Add Product</span>
              </button>
            )}
          </div>
        </div>

        {/* Mobile Navigation Bar */}
        <div className="flex lg:hidden items-center justify-between gap-1.5 py-1.5 border-t border-slate-100 w-full max-w-full relative">
          <div
            id="mobile-nav-scroll-bar"
            className="flex items-center gap-1.5 overflow-x-auto no-scrollbar scroll-smooth flex-1 min-w-0 py-0.5"
            style={{ WebkitOverflowScrolling: "touch" }}
          >
            <button
              id="mobile-nav-catalog-tab"
              onClick={() => {
                setCurrentView("grid");
                setIsMenuOpen(false);
              }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap shrink-0 transition-colors ${
                currentView === "grid" || currentView === "table"
                  ? "bg-indigo-600 text-white shadow-xs"
                  : "bg-slate-100 text-slate-700 hover:bg-slate-200 active:bg-slate-300"
              }`}
            >
              <Grid className="w-3.5 h-3.5 shrink-0" />
              <span>Catalog</span>
            </button>

            <button
              id="mobile-nav-compare-tab"
              onClick={() => {
                setCurrentView("compare");
                setIsMenuOpen(false);
              }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap shrink-0 transition-colors relative ${
                currentView === "compare"
                  ? "bg-indigo-600 text-white shadow-xs"
                  : "bg-slate-100 text-slate-700 hover:bg-slate-200 active:bg-slate-300"
              }`}
            >
              <Layers className="w-3.5 h-3.5 shrink-0" />
              <span>Compare</span>
              {comparedCount > 0 && (
                <span
                  className={`px-1.5 py-0.2 rounded-full text-[10px] font-bold shrink-0 ${
                    currentView === "compare" ? "bg-white text-indigo-700" : "bg-indigo-600 text-white"
                  }`}
                >
                  {comparedCount}
                </span>
              )}
            </button>

            <button
              id="mobile-nav-smart-add-tab"
              onClick={() => {
                setCurrentView("smart-add");
                setIsMenuOpen(false);
              }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold whitespace-nowrap shrink-0 transition-colors ${
                currentView === "smart-add"
                  ? "bg-slate-900 text-white shadow-xs"
                  : "bg-amber-50 text-amber-900 border border-amber-300 hover:bg-amber-100 active:bg-amber-200"
              }`}
            >
              <Sparkles className="w-3.5 h-3.5 text-amber-500 shrink-0" />
              <span>Smart Add Product</span>
            </button>

            <button
              id="mobile-nav-reviews-tab"
              onClick={() => {
                setCurrentView("reviews");
                setIsMenuOpen(false);
              }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap shrink-0 transition-colors ${
                currentView === "reviews"
                  ? "bg-indigo-600 text-white shadow-xs"
                  : "bg-slate-100 text-slate-700 hover:bg-slate-200 active:bg-slate-300"
              }`}
            >
              <MessageSquare className="w-3.5 h-3.5 shrink-0" />
              <span>Reviews</span>
            </button>

            <button
              id="mobile-nav-database-tab"
              onClick={() => {
                setCurrentView("database");
                setIsMenuOpen(false);
              }}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap shrink-0 transition-colors ${
                currentView === "database"
                  ? "bg-indigo-600 text-white shadow-xs"
                  : "bg-slate-100 text-slate-700 hover:bg-slate-200 active:bg-slate-300"
              }`}
            >
              <Database className="w-3.5 h-3.5 shrink-0" />
              <span>Data & Backup</span>
            </button>
          </div>

          {/* Droplist button for mobile */}
          <div className="relative shrink-0" ref={menuRef}>
            <button
              id="mobile-view-droplist-btn"
              onClick={() => setIsMenuOpen(!isMenuOpen)}
              className={`flex items-center gap-1 px-2.5 py-1.5 rounded-xl text-xs font-semibold border transition-all cursor-pointer ${
                isMenuOpen
                  ? "bg-indigo-600 text-white border-indigo-600 shadow-xs"
                  : "bg-slate-50 hover:bg-slate-100 active:bg-slate-200 text-slate-700 border-slate-200"
              }`}
              title="Open view drop list"
              aria-label="Open view drop list"
            >
              <Menu className="w-3.5 h-3.5" />
              <ChevronDown className={`w-3 h-3 transition-transform ${isMenuOpen ? "rotate-180" : ""}`} />
            </button>

            {isMenuOpen && (
              <div
                id="mobile-view-droplist-menu"
                className="absolute right-0 top-full mt-2 w-56 bg-white rounded-2xl shadow-2xl border border-slate-200 py-1.5 z-[100] animate-in fade-in zoom-in-95 duration-150"
              >
                <div className="px-3 py-1.5 text-[10px] font-bold uppercase tracking-wider text-slate-400 border-b border-slate-100 mb-1">
                  Switch Section / Tool
                </div>
                <button
                  id="droplist-item-catalog"
                  onClick={() => {
                    setCurrentView("grid");
                    setIsMenuOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2 text-xs text-left transition-colors ${
                    currentView === "grid" || currentView === "table"
                      ? "bg-indigo-50 text-indigo-700 font-bold"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <Grid className="w-4 h-4 text-indigo-600" />
                    <span>Catalog</span>
                  </div>
                  {(currentView === "grid" || currentView === "table") && (
                    <Check className="w-3.5 h-3.5 text-indigo-600" />
                  )}
                </button>

                <button
                  id="droplist-item-compare"
                  onClick={() => {
                    setCurrentView("compare");
                    setIsMenuOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2 text-xs text-left transition-colors ${
                    currentView === "compare"
                      ? "bg-indigo-50 text-indigo-700 font-bold"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <Layers className="w-4 h-4 text-indigo-600" />
                    <span>Compare Products</span>
                  </div>
                  {comparedCount > 0 ? (
                    <span className="px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-indigo-600 text-white">
                      {comparedCount}
                    </span>
                  ) : currentView === "compare" ? (
                    <Check className="w-3.5 h-3.5 text-indigo-600" />
                  ) : null}
                </button>

                <button
                  id="droplist-item-smart-add"
                  onClick={() => {
                    setCurrentView("smart-add");
                    setIsMenuOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2 text-xs text-left transition-colors ${
                    currentView === "smart-add"
                      ? "bg-indigo-50 text-indigo-700 font-bold"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <Sparkles className="w-4 h-4 text-amber-500" />
                    <span>Smart Add Product</span>
                  </div>
                  {currentView === "smart-add" && <Check className="w-3.5 h-3.5 text-indigo-600" />}
                </button>

                <button
                  id="droplist-item-reviews"
                  onClick={() => {
                    setCurrentView("reviews");
                    setIsMenuOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2 text-xs text-left transition-colors ${
                    currentView === "reviews"
                      ? "bg-indigo-50 text-indigo-700 font-bold"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <MessageSquare className="w-4 h-4 text-indigo-600" />
                    <span>Product Reviews</span>
                  </div>
                  {currentView === "reviews" && <Check className="w-3.5 h-3.5 text-indigo-600" />}
                </button>

                <button
                  id="droplist-item-database"
                  onClick={() => {
                    setCurrentView("database");
                    setIsMenuOpen(false);
                  }}
                  className={`w-full flex items-center justify-between px-3 py-2 text-xs text-left transition-colors ${
                    currentView === "database"
                      ? "bg-indigo-50 text-indigo-700 font-bold"
                      : "text-slate-700 hover:bg-slate-50"
                  }`}
                >
                  <div className="flex items-center gap-2">
                    <Database className="w-4 h-4 text-indigo-600" />
                    <span>Data & Backup</span>
                  </div>
                  {currentView === "database" && <Check className="w-3.5 h-3.5 text-indigo-600" />}
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};

export default Navbar;
