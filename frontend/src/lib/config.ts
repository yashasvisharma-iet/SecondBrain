const trimTrailingSlash = (value: string) => value.replace(/\/$/, '');

const rawApiBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim() ?? 'http://localhost:8080';
const rawNotionClientId = import.meta.env.VITE_NOTION_CLIENT_ID?.trim() ?? '';
const rawNotionRedirectUri = import.meta.env.VITE_NOTION_REDIRECT_URI?.trim() ?? 'http://localhost:5173/auth/notion/callback';

export const API_BASE_URL = trimTrailingSlash(rawApiBaseUrl);
export const NOTION_CLIENT_ID = rawNotionClientId;
export const NOTION_REDIRECT_URI = rawNotionRedirectUri;

export const apiUrl = (path: string) => `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}`;
