import { ApiError, request } from './httpClient';
import type {
  BatchObjectsResponse,
  ImportOperationPageResponse,
} from '@/types/batch';
import type { ErrorResponse } from '@/types/common';

function resolveDownloadFileName(contentDisposition: string | null, fallback: string): string {
  if (contentDisposition) {
    const utf8Match = /filename\*=UTF-8''([^;]+)/i.exec(contentDisposition);
    if (utf8Match?.[1]) {
      try {
        return decodeURIComponent(utf8Match[1].trim());
      } catch {
        // Fallback to standard filename parameter below
      }
    }
    const asciiMatch = /filename="([^"]+)"/i.exec(contentDisposition);
    if (asciiMatch?.[1]) {
      return asciiMatch[1].trim();
    }
  }
  return fallback;
}

export const objectsApi = {

  importFile: (file: File | Blob, fileName?: string): Promise<BatchObjectsResponse> => {
    const formData = new FormData();
    const uploadName = fileName || (file instanceof File ? file.name : 'import.json');
    formData.append('file', file, uploadName);
    return request<BatchObjectsResponse>('/api/objects', {
      method: 'POST',
      body: formData,
    });
  },

  getHistory: (page: number = 0, size: number = 10): Promise<ImportOperationPageResponse> =>
    request<ImportOperationPageResponse>(`/api/objects/history?page=${page}&size=${size}`, {
      method: 'GET',
    }),

  downloadSnapshot: async (id: number): Promise<void> => {
    const response = await fetch(`/api/objects/history/${id}/file`, {
      method: 'GET',
      credentials: 'include',
    });

    if (!response.ok) {
      let message = `Ошибка скачивания файла (HTTP ${response.status})`;
      let details: string[] = [];
      const contentType = response.headers.get('content-type');
      if (contentType && contentType.includes('application/json')) {
        try {
          const errorData = (await response.json()) as ErrorResponse | null;
          if (errorData?.message) {
            message = errorData.message;
          }
          if (errorData?.details) {
            details = errorData.details;
          }
        } catch {
          // ignore JSON parse errors
        }
      }
      throw new ApiError(message, response.status, details);
    }

    const blob = await response.blob();
    const resolvedName = resolveDownloadFileName(
      response.headers.get('content-disposition'),
      `import_${id}.json`,
    );

    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = resolvedName;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  },
};
