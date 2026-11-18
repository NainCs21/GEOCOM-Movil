export function parseFechaIso(fecha?: string | null): Date {
  if (!fecha) return new Date();
  const limpia = fecha.includes('T') ? fecha : fecha.replace(' ', 'T') + 'Z';
  const normalizada = limpia.replace(/\.(\d{3})\d*Z$/, '.$1Z');
  const parsed = new Date(normalizada);
  return isNaN(parsed.getTime()) ? new Date() : parsed;
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

export function formatoHoraCorta(fecha?: string | null): string {
  if (!fecha) return '';
  try {
    const d = parseFechaIso(fecha);
    return d.toLocaleTimeString('es-HN', {
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    });
  } catch {
    return fecha.substring(11, 16);
  }
}