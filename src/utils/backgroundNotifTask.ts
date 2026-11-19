import AsyncStorage from '@react-native-async-storage/async-storage';
import * as BackgroundTask from 'expo-background-task';
import * as TaskManager from 'expo-task-manager';
import { sincronizarNotificaciones } from './notificaciones';

export const BACKGROUND_NOTIF_TASK = 'GEOCOM_SYNC_NOTIF';

// Debe ejecutarse a nivel de módulo (fuera de componentes React)
TaskManager.defineTask(BACKGROUND_NOTIF_TASK, async () => {
  try {
    const raw = await AsyncStorage.getItem('api_movil_sesion');
    if (!raw) return BackgroundTask.BackgroundTaskResult.Success;

    const parsed = JSON.parse(raw);
    const token = parsed?.token;
    const usuario = parsed?.usuario;

    if (!token || !usuario?.id || usuario?.rol === 'conductor') {
      return BackgroundTask.BackgroundTaskResult.Success;
    }

    await sincronizarNotificaciones(token, usuario.id);
    return BackgroundTask.BackgroundTaskResult.Success;
  } catch {
    return BackgroundTask.BackgroundTaskResult.Failed;
  }
});

export async function programarSincronizacionBackground() {
  try {
    const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_NOTIF_TASK);
    if (!isRegistered) {
      await BackgroundTask.registerTaskAsync(BACKGROUND_NOTIF_TASK, {
        minimumInterval: 15, // 15 minutos mínimo (mismo estándar que WorkManager)
      });
    }
  } catch {
    // En Expo Go iOS puede reportar Restricted; en build nativo funciona sin problemas
  }
}

export async function cancelarSincronizacionBackground() {
  try {
    const isRegistered = await TaskManager.isTaskRegisteredAsync(BACKGROUND_NOTIF_TASK);
    if (isRegistered) {
      await BackgroundTask.unregisterTaskAsync(BACKGROUND_NOTIF_TASK);
    }
  } catch {
    // Ignorar si no estaba registrada
  }
}