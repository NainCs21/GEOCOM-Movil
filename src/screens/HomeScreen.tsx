import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  FlatList,
  RefreshControl,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';
import { formatoLegible, parseFechaIso } from '../utils/fechas';

interface Props {
  user: any;
  token: string;
  setScreen: (s: string) => void;
  onOpenOrder: (id: number) => void;
  onOpenNuevaSolicitud: () => void;
}

export default function HomeScreen({
  user,
  token,
  setScreen,
  onOpenOrder,
  onOpenNuevaSolicitud,
}: Props) {
  const [ordenes, setOrdenes] = useState<any[]>([]);
  const [pendientesCount, setPendientesCount] = useState(0);
  const [refreshing, setRefreshing] = useState(false);

  const esConductor = user?.rol === 'conductor';
  const esAdmin = user?.rol === 'admin';

  const cargarDatos = useCallback(async () => {
    setRefreshing(true);
    const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` };
    try {
      if (esConductor) {
        const res = await fetch(`${API_URL}/ordenes`, { headers });
        const json = await res.json();
        const lista = Array.isArray(json) ? json : json.data || [];
        setOrdenes(lista.filter((o: any) => (o.estado || '').toLowerCase() !== 'completada'));
      } else {
        const [solRes, ordRes] = await Promise.all([
          fetch(`${API_URL}/solicitudes`, { headers }).then((r) => r.json()).catch(() => []),
          fetch(`${API_URL}/ordenes`, { headers }).then((r) => r.json()).catch(() => []),
        ]);
        const solList = Array.isArray(solRes) ? solRes : solRes.data || [];
        const ordList = Array.isArray(ordRes) ? ordRes : ordRes.data || [];
        setPendientesCount(solList.filter((s: any) => s.estado === 'pendiente').length);
        setOrdenes(ordList);
      }
    } catch {
      // Silencioso en recarga
    } finally {
      setRefreshing(false);
    }
  }, [esConductor, token]);

  useEffect(() => {
    cargarDatos();
  }, [cargarDatos]);

  const etiquetaEstado = (est: string) => {
    switch (est) {
      case 'pendiente':
        return 'PENDIENTE';
      case 'abierta':
        return 'ABIERTA';
      case 'completada':
        return 'COMPLETADA';
      case 'vencida':
        return 'VENCIDA';
      case 'rehabilitada':
        return 'REHABILITADA';
      case 'tardio':
      case 'completado_tardio':
        return 'COMPLETADO TARDÍO';
      default:
        return (est || '').toUpperCase();
    }
  };

  const colorBadge = (est: string) => {
    switch (est) {
      case 'pendiente':
        return { bg: colors.pendienteFondo, fg: colors.pendiente };
      case 'abierta':
        return { bg: colors.abiertaFondo, fg: colors.abierta };
      case 'completada':
        return { bg: colors.completadaFondo, fg: colors.completada };
      case 'vencida':
      case 'tardio':
      case 'completado_tardio':
        return { bg: colors.vencidaFondo, fg: colors.vencida };
      case 'rehabilitada':
        return { bg: colors.abiertaFondo, fg: colors.marca };
      default:
        return { bg: colors.pendienteFondo, fg: colors.pendiente };
    }
  };

  // ==================== VISTA CONDUCTOR ====================
  if (esConductor) {
    return (
      <View style={styles.flex}>
        <View style={styles.topHeader}>
          <Text style={[styles.topHeaderTitle, { flex: 1 }]}>
            Hola, {user?.nombre || ''}
          </Text>
          <TouchableOpacity onPress={() => setScreen('historial')} style={styles.iconBtn}>
            <Text style={{ fontSize: 19, color: colors.blanco }}>🕒</Text>
          </TouchableOpacity>
          <TouchableOpacity
            onPress={() => setScreen('perfil')}
            style={[styles.iconBtn, { marginLeft: 4 }]}
          >
            <Text style={{ fontSize: 19, color: colors.blanco }}>👤</Text>
          </TouchableOpacity>
        </View>

        <FlatList
          data={ordenes}
          keyExtractor={(item) => String(item.id)}
          contentContainerStyle={{ paddingHorizontal: 20, paddingTop: 16, paddingBottom: 88 }}
          refreshControl={<RefreshControl refreshing={refreshing} onRefresh={cargarDatos} />}
          renderItem={({ item }) => {
            const b = colorBadge(item.estado);
            return (
              <TouchableOpacity style={styles.orderCard} onPress={() => onOpenOrder(item.id)}>
                <View style={{ flex: 1 }}>
                  <Text style={styles.orderNum}>Orden #{item.numero_orden}</Text>
                  <Text style={styles.stationName}>
                    {item.gasolinera?.nombre || 'Gasolinera por definir'}
                  </Text>
                </View>
                <View style={[styles.badge, { backgroundColor: b.bg }]}>
                  <Text style={[styles.badgeText, { color: b.fg }]}>
                    {etiquetaEstado(item.estado)}
                  </Text>
                </View>
              </TouchableOpacity>
            );
          }}
          ListEmptyComponent={
            !refreshing ? (
              <Text style={styles.emptyText}>Todavía no tienes órdenes.</Text>
            ) : undefined
          }
        />

        {/* FAB Nueva Solicitud */}
        <TouchableOpacity style={styles.fab} onPress={onOpenNuevaSolicitud} activeOpacity={0.85}>
          <Text style={styles.fabIcon}>＋</Text>
        </TouchableOpacity>
      </View>
    );
  }

  // ==================== VISTA ADMIN / SUPERVISOR ====================
  const abiertas = ordenes.filter((o) => o.estado === 'abierta').length;
  const completadas = ordenes.filter((o) => o.estado === 'completada').length;
  const vencidas = ordenes.filter((o) => o.estado === 'vencida').length;

  const proxima = ordenes
    .filter((o) => o.estado === 'abierta' && o.vence_apertura)
    .sort(
      (a, b) =>
        parseFechaIso(a.vence_apertura).getTime() - parseFechaIso(b.vence_apertura).getTime()
    )[0];

  const minutosRestantes = proxima
    ? Math.max(
        0,
        Math.floor((parseFechaIso(proxima.vence_apertura).getTime() - Date.now()) / 60000)
      )
    : 0;

  const fechaHoy = new Date()
    .toLocaleDateString('es-HN', {
      weekday: 'long',
      day: '2-digit',
      month: 'short',
      year: 'numeric',
    })
    .toUpperCase();

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <View style={{ flex: 1 }}>
          <Text style={styles.topHeaderTitle}>Inicio</Text>
          <Text style={styles.rolSubtitle}>{esAdmin ? 'Administrador' : 'Supervisor'}</Text>
        </View>
        <TouchableOpacity onPress={() => setScreen('notificaciones')} style={styles.iconBtn}>
          <Text style={{ fontSize: 19, color: colors.blanco }}>🔔</Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        contentContainerStyle={{ padding: 20, paddingBottom: 32 }}
        refreshControl={<RefreshControl refreshing={refreshing} onRefresh={cargarDatos} />}
      >
        <Text style={styles.fechaText}>{fechaHoy}</Text>
        <Text style={styles.saludoText}>Buen día, {user?.nombre || ''}</Text>

        {/* Tarjeta Solicitudes pendientes */}
        <View style={styles.card}>
          <View style={{ flexDirection: 'row', alignItems: 'center' }}>
            <Text style={styles.bigCount}>{pendientesCount}</Text>
            <View style={{ flex: 1, marginLeft: 16 }}>
              <Text style={styles.cardTitle}>Solicitudes pendientes</Text>
              <Text style={styles.cardSub}>Esperan asignación de monto</Text>
            </View>
          </View>
          <TouchableOpacity
            style={styles.btnTonalFull}
            onPress={() => setScreen('solicitudes')}
          >
            <Text style={styles.btnTonalText}>
              {esAdmin ? 'Asignar montos  →' : 'Ver solicitudes'}
            </Text>
          </TouchableOpacity>
        </View>

        {/* 3 Indicadores de estado */}
        <View style={styles.kpiRow}>
          <View style={styles.kpiCard}>
            <Text style={[styles.kpiNum, { color: colors.abierta }]}>{abiertas}</Text>
            <Text style={styles.kpiLabel}>ABIERTA</Text>
          </View>
          <View style={styles.kpiCard}>
            <Text style={[styles.kpiNum, { color: colors.completada }]}>{completadas}</Text>
            <Text style={styles.kpiLabel}>COMPLETADA</Text>
          </View>
          <View style={styles.kpiCard}>
            <Text style={[styles.kpiNum, { color: colors.vencida }]}>{vencidas}</Text>
            <Text style={styles.kpiLabel}>VENCIDA</Text>
          </View>
        </View>

        <Text style={styles.sectionCaption}>Órdenes del día · vigencia de 15 min</Text>

        {proxima && (
          <View style={styles.warningCard}>
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
              <Text style={[styles.cardTitle, { flex: 1 }]}>{proxima.numero_orden}</Text>
              <View style={[styles.badge, { backgroundColor: colors.abiertaFondo }]}>
                <Text style={[styles.badgeText, { color: colors.abierta }]}>ABIERTA</Text>
              </View>
            </View>
            <Text style={styles.warningDetail}>
              {proxima.gasolinera?.nombre || ''} · Vence a las{' '}
              {formatoLegible(proxima.vence_apertura)}
            </Text>
            <View style={{ flexDirection: 'row', alignItems: 'center', marginTop: 12 }}>
              <Text style={styles.warningMin}>Quedan {minutosRestantes} min</Text>
              <TouchableOpacity onPress={() => onOpenOrder(proxima.id)}>
                <Text style={styles.verOrdenLink}>Ver orden →</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}

        <Text style={styles.sectionTitle}>Accesos rápidos</Text>

        <View style={styles.quickRow}>
          <TouchableOpacity
            style={styles.quickTonalBtn}
            onPress={() => setScreen('caja_chica')}
          >
            <Text style={styles.btnTonalText}>＋ Caja chica</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.quickTonalBtn}
            onPress={() => setScreen('catalogos')}
          >
            <Text style={styles.btnTonalText}>Catálogos</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  topHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.marca,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  topHeaderTitle: { fontSize: 18, fontWeight: '700', color: colors.blanco },
  rolSubtitle: { fontSize: 11, fontWeight: '700', color: colors.marcaClaro },
  iconBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center' },
  fechaText: {
    fontSize: 12,
    fontWeight: '700',
    color: colors.textoSecundario,
    letterSpacing: 0.5,
  },
  saludoText: { fontSize: 22, fontWeight: '700', color: colors.textoPrincipal, marginTop: 4 },
  card: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.borde,
    marginTop: 16,
  },
  bigCount: { fontSize: 36, fontWeight: '700', color: colors.marca },
  cardTitle: { fontSize: 15, fontWeight: '700', color: colors.textoPrincipal },
  cardSub: { fontSize: 12, color: colors.textoSecundario, marginTop: 2 },
  btnTonalFull: {
    backgroundColor: colors.marcaClaro,
    height: 44,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 12,
  },
  btnTonalText: { color: colors.marcaOscuro, fontSize: 14, fontWeight: '700' },
  kpiRow: { flexDirection: 'row', gap: 8, marginTop: 16 },
  kpiCard: {
    flex: 1,
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  kpiNum: { fontSize: 22, fontWeight: '700' },
  kpiLabel: { fontSize: 11, color: colors.textoSecundario, marginTop: 2 },
  sectionCaption: { fontSize: 12, color: colors.textoSecundario, marginTop: 20 },
  warningCard: {
    backgroundColor: colors.advertenciaFondo,
    borderRadius: 16,
    padding: 16,
    marginTop: 8,
  },
  warningDetail: { fontSize: 13, color: colors.advertenciaTextoSecundario, marginTop: 8 },
  warningMin: { flex: 1, fontSize: 14, fontWeight: '700', color: colors.advertenciaTexto },
  verOrdenLink: { fontSize: 14, fontWeight: '700', color: colors.marca },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal, marginTop: 20 },
  quickRow: { flexDirection: 'row', gap: 8, marginTop: 10 },
  quickTonalBtn: {
    flex: 1,
    height: 56,
    backgroundColor: colors.marcaClaro,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  orderCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 10,
  },
  orderNum: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  stationName: { fontSize: 13, color: colors.textoSecundario, marginTop: 2 },
  badge: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 12 },
  badgeText: { fontSize: 11, fontWeight: '700' },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 80, fontSize: 14 },
  fab: {
    position: 'absolute',
    right: 20,
    bottom: 20,
    width: 56,
    height: 56,
    borderRadius: 28,
    backgroundColor: colors.marca,
    justifyContent: 'center',
    alignItems: 'center',
    shadowColor: '#000',
    shadowOpacity: 0.25,
    shadowRadius: 6,
    shadowOffset: { width: 0, height: 3 },
    elevation: 6,
  },
  fabIcon: { color: colors.blanco, fontSize: 28, fontWeight: '600', lineHeight: 30 },
});