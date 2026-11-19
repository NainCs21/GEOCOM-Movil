import AsyncStorage from '@react-native-async-storage/async-storage';
import { API_URL } from '../api/config';
import { formatoLegible, obtenerHace3DiasString, parseFechaIsoMillis } from './fechas';

export interface NotificacionApp {
  id: string;
  tipo: string;
  titulo: string;
  cuerpo: string;
  fechaMillis: number;
  destino: 'orden' | 'solicitudes';
  ordenId: number | null;
  leida: boolean;
}

const KEY_LISTA = (uid: number) => `geocom_notif_lista_${uid}`;
const KEY_VISTOS = (uid: number) => `geocom_notif_vistos_${uid}`;
const KEY_BASELINE = (uid: number) => `geocom_notif_baseline_${uid}`;

function derivarEventos(solicitudes: any[], ordenes: any[]): Omit<NotificacionApp, 'leida'>[] {
  const lista: Omit<NotificacionApp, 'leida'>[] = [];

  for (const sol of solicitudes) {
    const conductorNombre = sol.conductor?.usuario?.name || 'Conductor';
    if (sol.created_at) {
      lista.push({
        id: `solicitud_nueva_${sol.id}`,
        tipo: 'solicitud_nueva',
        titulo: 'Nueva solicitud',
        cuerpo: `${conductorNombre} solicitó combustible (${sol.tipo_destino})`,
        fechaMillis: parseFechaIsoMillis(sol.created_at),
        destino: 'solicitudes',
        ordenId: null,
      });
    }
    if (sol.estado === 'rechazada') {
      const fechaStr = sol.updated_at || sol.created_at;
      if (fechaStr) {
        lista.push({
          id: `solicitud_rechazada_${sol.id}`,
          tipo: 'solicitud_rechazada',
          titulo: 'Solicitud rechazada',
          cuerpo: `La solicitud de ${conductorNombre} fue rechazada`,
          fechaMillis: parseFechaIsoMillis(fechaStr),
          destino: 'solicitudes',
          ordenId: null,
        });
      }
    }
  }

  for (const ord of ordenes) {
    const conductorNombre = ord.conductor?.usuario?.name || 'Conductor';
    const gasolineraNombre = ord.gasolinera?.nombre || '';
    const total = (ord.detalles || []).reduce(
      (acc: number, d: any) => acc + (parseFloat(d.monto) || 0),
      0
    );
    const totalFmt = total.toFixed(2);

    if (ord.fecha_asignacion) {
      lista.push({
        id: `orden_asignada_${ord.id}`,
        tipo: 'orden_asignada',
        titulo: 'Solicitud aceptada',
        cuerpo: `Se asignó la orden ${ord.numero_orden} a ${conductorNombre} en ${gasolineraNombre}. Total: L ${totalFmt}`,
        fechaMillis: parseFechaIsoMillis(ord.fecha_asignacion),
        destino: 'orden',
        ordenId: ord.id,
      });
    }

    if (ord.fecha_apertura) {
      const horaVence = ord.vence_apertura ? formatoLegible(ord.vence_apertura) : '';
      const cuerpoTexto = horaVence
        ? `${conductorNombre} abrió la orden ${ord.numero_orden} en ${gasolineraNombre}. Tiene hasta las ${horaVence} para subir las evidencias`
        : `${conductorNombre} abrió la orden ${ord.numero_orden} en ${gasolineraNombre}`;
      lista.push({
        id: `orden_abierta_${ord.id}`,
        tipo: 'orden_abierta',
        titulo: 'Orden abierta',
        cuerpo: cuerpoTexto,
        fechaMillis: parseFechaIsoMillis(ord.fecha_apertura),
        destino: 'orden',
        ordenId: ord.id,
      });
    }

    (ord.detalles || []).forEach((detalle: any) => {
      (detalle.evidencias || []).forEach((evidencia: any) => {
        if (evidencia.fecha_subida) {
          lista.push({
            id: `evidencia_subida_${ord.id}_${evidencia.id}`,
            tipo: 'evidencia_subida',
            titulo: 'Evidencia subida',
            cuerpo: `${conductorNombre} subió la foto de ${evidencia.tipo} de la orden ${ord.numero_orden}`,
            fechaMillis: parseFechaIsoMillis(evidencia.fecha_subida),
            destino: 'orden',
            ordenId: ord.id,
          });
        }
      });
    });

    if (ord.fecha_completada) {
      lista.push({
        id: `orden_completada_${ord.id}`,
        tipo: 'orden_completada',
        titulo: 'Orden completada',
        cuerpo: `${conductorNombre} completó la orden ${ord.numero_orden} en ${gasolineraNombre}. Total: L ${totalFmt}`,
        fechaMillis: parseFechaIsoMillis(ord.fecha_completada),
        destino: 'orden',
        ordenId: ord.id,
      });
    }

    if (ord.estado === 'vencida') {
      const fechaVenceStr = ord.fecha_apertura ? ord.vence_apertura : ord.vence_asignacion;
      if (fechaVenceStr) {
        lista.push({
          id: `orden_vencida_${ord.id}`,
          tipo: 'orden_vencida',
          titulo: 'Orden vencida',
          cuerpo: `La orden ${ord.numero_orden} de ${conductorNombre} venció sin completarse`,
          fechaMillis: parseFechaIsoMillis(fechaVenceStr),
          destino: 'orden',
          ordenId: ord.id,
        });
      }
    }
  }

  return lista.sort((a, b) => b.fechaMillis - a.fechaMillis);
}

export async function obtenerNotificacionesGuardadas(usuarioId: number): Promise<NotificacionApp[]> {
  try {
    const raw = await AsyncStorage.getItem(KEY_LISTA(usuarioId));
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export async function marcarNotificacionLeida(
  usuarioId: number,
  notifId: string
): Promise<NotificacionApp[]> {
  const actual = await obtenerNotificacionesGuardadas(usuarioId);
  const updated = actual.map((n) => (n.id === notifId ? { ...n, leida: true } : n));
  await AsyncStorage.setItem(KEY_LISTA(usuarioId), JSON.stringify(updated));
  return updated;
}

export async function marcarTodasNotificacionesLeidas(
  usuarioId: number
): Promise<NotificacionApp[]> {
  const actual = await obtenerNotificacionesGuardadas(usuarioId);
  const updated = actual.map((n) => ({ ...n, leida: true }));
  await AsyncStorage.setItem(KEY_LISTA(usuarioId), JSON.stringify(updated));
  return updated;
}

export async function sincronizarNotificaciones(
  token: string,
  usuarioId: number
): Promise<NotificacionApp[]> {
  const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` };
  const limite3DiasMs = Date.now() - 3 * 24 * 3600 * 1000;
  const limite90DiasMs = Date.now() - 90 * 24 * 3600 * 1000;

  try {
    const solicitudes: any[] = [];
    let page = 1;
    let lastPage = 1;
    do {
      const r = await fetch(`${API_URL}/solicitudes?page=${page}`, { headers });
      if (!r.ok) return await obtenerNotificacionesGuardadas(usuarioId);
      const json = await r.json();
      const data = Array.isArray(json) ? json : json.data || [];
      solicitudes.push(...data);
      lastPage = json.last_page || 1;
      const todosViejos =
        data.length > 0 && data.every((s: any) => parseFechaIsoMillis(s.created_at) < limite3DiasMs);
      if (todosViejos) break;
      page++;
    } while (page <= lastPage);

    const ordenes: any[] = [];
    const desde3d = obtenerHace3DiasString();
    page = 1;
    lastPage = 1;
    do {
      const r = await fetch(`${API_URL}/ordenes?desde=${desde3d}&page=${page}`, { headers });
      if (!r.ok) return await obtenerNotificacionesGuardadas(usuarioId);
      const json = await r.json();
      const data = Array.isArray(json) ? json : json.data || [];
      ordenes.push(...data);
      lastPage = json.last_page || 1;
      page++;
    } while (page <= lastPage);

    const eventos = derivarEventos(solicitudes, ordenes);
    const ahora = Date.now();
    const baseline = (await AsyncStorage.getItem(KEY_BASELINE(usuarioId))) === 'true';

    const vistosRaw = await AsyncStorage.getItem(KEY_VISTOS(usuarioId));
    const vistosMap: Record<string, number> = vistosRaw ? JSON.parse(vistosRaw) : {};

    for (const k of Object.keys(vistosMap)) {
      if (vistosMap[k] < limite90DiasMs) delete vistosMap[k];
    }

    const existentes = await obtenerNotificacionesGuardadas(usuarioId);
    const existentesIds = new Set(existentes.map((e) => e.id));

    if (!baseline) {
      for (const ev of eventos) vistosMap[ev.id] = ev.fechaMillis;
      const limite24h = ahora - 24 * 3600 * 1000;
      const iniciales: NotificacionApp[] = eventos
        .filter((ev) => ev.fechaMillis >= limite24h && !existentesIds.has(ev.id))
        .map((ev) => ({ ...ev, leida: true }));

      const merged = [...iniciales, ...existentes]
        .sort((a, b) => b.fechaMillis - a.fechaMillis)
        .slice(0, 200);

      await AsyncStorage.setItem(KEY_VISTOS(usuarioId), JSON.stringify(vistosMap));
      await AsyncStorage.setItem(KEY_LISTA(usuarioId), JSON.stringify(merged));
      await AsyncStorage.setItem(KEY_BASELINE(usuarioId), 'true');
      return merged;
    } else {
      const nuevosEventos = eventos.filter(
        (ev) => !(ev.id in vistosMap) && ev.fechaMillis >= limite90DiasMs
      );
      if (nuevosEventos.length > 0) {
        for (const ev of nuevosEventos) vistosMap[ev.id] = ev.fechaMillis;
        const nuevasNotifs: NotificacionApp[] = nuevosEventos
          .filter((ev) => !existentesIds.has(ev.id))
          .map((ev) => ({ ...ev, leida: false }));

        const merged = [...nuevasNotifs, ...existentes]
          .sort((a, b) => b.fechaMillis - a.fechaMillis)
          .slice(0, 200);

        await AsyncStorage.setItem(KEY_VISTOS(usuarioId), JSON.stringify(vistosMap));
        await AsyncStorage.setItem(KEY_LISTA(usuarioId), JSON.stringify(merged));
        return merged;
      }
      return existentes;
    }
  } catch {
    return await obtenerNotificacionesGuardadas(usuarioId);
  }
}