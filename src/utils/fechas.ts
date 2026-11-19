import { API_URL } from '../api/config';

export function parseFechaIso(fecha?: string | null): Date {
  if (!fecha) return new Date();
  const limpia = fecha.trim().includes('T') ? fecha.trim() : fecha.trim().replace(' ', 'T') + 'Z';
  const normalizada = limpia.replace(/\.(\d{3})\d*Z?$/, '.$1Z');
  const parsed = new Date(normalizada);
  return isNaN(parsed.getTime()) ? new Date() : parsed;
}

export function parseFechaIsoMillis(fecha?: string | null): number {
  if (!fecha) return 0;
  const t = parseFechaIso(fecha).getTime();
  return isNaN(t) ? 0 : t;
}

export function formatoLegible(fecha?: string | null): string {
  if (!fecha) return '—';
  try {
    const d = parseFechaIso(fecha);
    return d.toLocaleString('es-HN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return fecha;
  }
}

export function obtenerFechaActualString(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function obtenerHace3DiasString(): string {
  const d = new Date();
  d.setDate(d.getDate() - 3);
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

export function obtenerInicioMesString(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}

export function obtenerMesAnteriorRango(): [string, string] {
  const d = new Date();
  const inicio = new Date(d.getFullYear(), d.getMonth() - 1, 1);
  const fin = new Date(d.getFullYear(), d.getMonth(), 0);
  const fmt = (x: Date) =>
    `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`;
  return [fmt(inicio), fmt(fin)];
}

export function obtenerUltimos7DiasRango(): [string, string] {
  const hasta = new Date();
  const desde = new Date();
  desde.setDate(hasta.getDate() - 6);
  const fmt = (x: Date) =>
    `${x.getFullYear()}-${String(x.getMonth() + 1).padStart(2, '0')}-${String(x.getDate()).padStart(2, '0')}`;
  return [fmt(desde), fmt(hasta)];
}

export function formatearFechaIsoShort(fechaStr?: string | null): string {
  if (!fechaStr) return '';
  const partes = fechaStr.split('-');
  if (partes.length === 3) {
    return `${partes[2]}/${partes[1]}/${partes[0]}`;
  }
  return fechaStr;
}

export function normalizarTexto(texto: string): string {
  return texto
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase();
}

export function normalizarUrlApi(url: string): string {
  if (!url) return url;
  if (url.toLowerCase().startsWith('http://') && url.includes('railway.app')) {
    return 'https://' + url.substring(7);
  }
  if (url.startsWith('/')) {
    const base = API_URL.replace(/\/api\/?$/, '');
    return `${base}${url}`;
  }
  return url;
}