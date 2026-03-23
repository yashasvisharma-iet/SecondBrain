const trimTrailingSlash = (value: string) => value.replace(/\/$/, '');

const browserOrigin = typeof window !== 'undefined' ? window.location.origin : '';

const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? browserOrigin;
const rawNotionClientId = import.meta.env.VITE_NOTION_CLIENT_ID?.trim() ?? '';
const rawNotionRedirectUri = import.meta.env.VITE_NOTION_REDIRECT_URI?.trim() ?? `${browserOrigin}/auth/notion/callback`;

export const API_BASE_URL = trimTrailingSlash(rawApiBaseUrl);
export const NOTION_CLIENT_ID = rawNotionClientId;
export const NOTION_REDIRECT_URI = rawNotionRedirectUri;
console.log("API BASE URL:", API_BASE_URL);
export const apiUrl = (path: string) => `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
