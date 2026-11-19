import { Platform } from 'react-native';
import * as Notifications from 'expo-notifications';
import type { NotificacionApp } from './notificaciones';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowBanner: true,
    shouldShowList: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

let notificacionesScreenActive = false;

export function setNotificacionesScreenActive(active: boolean) {
  notificacionesScreenActive = active;
}

export async function pedirPermisoNotificaciones(): Promise<boolean> {
  try {
    const { status: existingStatus } = await Notifications.getPermissionsAsync();
    let finalStatus = existingStatus;
    if (existingStatus !== 'granted') {
      const { status } = await Notifications.requestPermissionsAsync();
      finalStatus = status;
    }

    if (Platform.OS === 'android') {
      await Notifications.setNotificationChannelAsync('geocom_eventos', {
        name: 'Solicitudes y órdenes',
        importance: Notifications.AndroidImportance.HIGH,
      });
    }

    return finalStatus === 'granted';
  } catch {
    return false;
  }
}

export async function actualizarBadgeSistema(cantidadNoLeidas: number) {
  try {
    await Notifications.setBadgeCountAsync(Math.max(0, cantidadNoLeidas));
  } catch {
  }
}

export async function mostrarNotificacionNativa(notif: NotificacionApp) {
  if (notificacionesScreenActive) return;

  try {
    await Notifications.scheduleNotificationAsync({
      content: {
        title: notif.titulo,
        body: notif.cuerpo,
        sound: 'default',
        data: {
          notif_id: notif.id,
          notif_destino: notif.destino,
          notif_orden_id: notif.ordenId,
        },
      },
      trigger: null,
    });
  } catch {
  }
}

export async function mostrarResumenNativo(cantidad: number) {
  if (notificacionesScreenActive) return;

  try {
    await Notifications.scheduleNotificationAsync({
      content: {
        title: 'Notificaciones nuevas',
        body: `Tienes ${cantidad} notificaciones nuevas`,
        sound: 'default',
        data: {
          notif_destino: 'notificaciones',
        },
      },
      trigger: null,
    });
  } catch {
  }
}