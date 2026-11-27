import type { NotificationEvent } from '@/types/common';
import type { ImportOperationResponse } from '@/types/batch';
import { queryClient } from './queryClient';
import { useCreaturesStore } from '@/stores/creaturesStore';
import { useCitiesStore } from '@/stores/citiesStore';
import { useRingsStore } from '@/stores/ringsStore';
import { useImportStore } from '@/stores/importStore';
import { useNotificationStore } from '@/stores/notificationStore';

const SYNC_CONFIG = {
  BOOK_CREATURE: {
    label: 'Существо',
    plural: 'Все существа',
    queryKey: ['creatures'],
    refetch: () => useCreaturesStore().fetchCreatures(true),
  },
  MAGIC_CITY: {
    label: 'Город',
    plural: 'Все города',
    queryKey: ['cities'],
    refetch: () => useCitiesStore().fetchCities(true),
  },
  RING: {
    label: 'Кольцо',
    plural: 'Все кольца',
    queryKey: ['rings'],
    refetch: () => useRingsStore().fetchRings(true),
  },
} as const;

function handleSseMessage(event: NotificationEvent): void {
  const config = SYNC_CONFIG[event.entityType];
  if (!config) return;

  queryClient.invalidateQueries({ queryKey: config.queryKey });
  config.refetch().catch(() => {});

  const subject = event.entityId === 0 ? config.plural : `${config.label} #${event.entityId}`;
  useNotificationStore().addToast({
    type: 'info',
    title: 'Синхронизация данных',
    message: `${subject} обновлены на сервере (${event.action})`,
    duration: 3000,
  });
}

function handleImportOperation(op: ImportOperationResponse): void {
  queryClient.invalidateQueries({ queryKey: ['import-history'] });
  useImportStore()
    .fetchHistory()
    .catch(() => {});

  const notificationStore = useNotificationStore();
  switch (op.status) {
    case 'SUCCESS':
      notificationStore.addToast({
        type: 'success',
        title: 'Импорт завершен',
        message: `Операция #${op.id} от ${op.username} выполнена успешно (добавлено: ${op.addedObjectsCount ?? 0})`,
        duration: 4000,
      });
      break;
    case 'INVALID_FILE':
      notificationStore.addToast({
        type: 'error',
        title: 'Неверный файл импорта',
        message: `Операция #${op.id} от ${op.username}: неверный файл`,
        duration: 4000,
      });
      break;
    case 'FAILED':
    default:
      notificationStore.addToast({
        type: 'error',
        title: 'Импорт не удался',
        message: `Операция #${op.id} от ${op.username} завершилась ошибкой`,
        duration: 4000,
      });
      break;
  }
}

class SseClient {
  private eventSource: EventSource | null = null;
  private url = '/api/notifications/events';
  private reconnectTimer: number | null = null;
  private reconnectDelay = 2000;
  private maxReconnectDelay = 30000;
  private isExplicitlyClosed = false;

  public connect(): void {
    if (this.eventSource) return;
    this.isExplicitlyClosed = false;

    try {
      this.eventSource = new EventSource(this.url);

      this.eventSource.onopen = () => {
        this.reconnectDelay = 2000;
      };

      this.eventSource.addEventListener('entity-change', (e: MessageEvent) => {
        try {
          const parsed: NotificationEvent = JSON.parse(e.data);
          handleSseMessage(parsed);
        } catch (err) {
          console.error('Failed to parse SSE entity-change data:', err);
        }
      });

      this.eventSource.addEventListener('import-operation', (e: MessageEvent) => {
        try {
          const parsed: ImportOperationResponse = JSON.parse(e.data);
          handleImportOperation(parsed);
        } catch (err) {
          console.error('Failed to parse SSE import-operation data:', err);
        }
      });

      this.eventSource.onerror = (err) => {
        console.warn('SSE connection error:', err);
        this.cleanup();
        this.scheduleReconnect();
      };
    } catch (err) {
      console.error('Error instantiating EventSource:', err);
      this.scheduleReconnect();
    }
  }

  public disconnect(): void {
    this.isExplicitlyClosed = true;
    if (this.reconnectTimer !== null) {
      window.clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }
    this.cleanup();
  }

  private cleanup(): void {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
    }
  }

  private scheduleReconnect(): void {
    if (this.isExplicitlyClosed || this.reconnectTimer !== null) return;

    this.reconnectTimer = window.setTimeout(() => {
      this.reconnectTimer = null;
      this.reconnectDelay = Math.min(this.reconnectDelay * 1.5, this.maxReconnectDelay);
      this.connect();
    }, this.reconnectDelay);
  }
}

export const sseClient = new SseClient();
