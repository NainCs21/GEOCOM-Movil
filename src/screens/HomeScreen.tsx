import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  user: any;
  setScreen: (screen: string) => void;
  token: string;
  onOpenOrder: (id: number) => void;
  onOpenNuevaSolicitud: () => void;
}

export default function HomeScreen({
  user,
  setScreen,
  token,
  onOpenOrder,
  onOpenNuevaSolicitud,
}: Props) {
  const isConductor = user?.rol === 'conductor';
  const isAdmin = user?.rol === 'admin';
  const [ordenes, setOrdenes] = useState<any[]>([]);
  const [solicitudes, setSolicitudes] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const cargarDatos = () => {
    const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` };
    setLoading(true);
    Promise.all([
      fetch(`${API_URL}/ordenes`, { headers }).then((r) => r.json()).catch(() => []),
      !isConductor
        ? fetch(`${API_URL}/solicitudes`, { headers }).then((r) => r.json()).catch(() => [])
        : Promise.resolve([]),
    ])
      .then(([ordData, solData]) => {
        setOrdenes(Array.isArray(ordData) ? ordData : ordData.data || []);
        setSolicitudes(Array.isArray(solData) ? solData : solData.data || []);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    cargarDatos();
  }, [token]);

  const getBadgeStyle = (estado: string) => {
    switch (estado?.toLowerCase()) {
      case 'abierta':
        return { bg: colors.abiertaFondo, text: colors.abierta, label: 'ABIERTA' };
      case 'completada':
        return { bg: colors.completadaFondo, text: colors.completada, label: 'COMPLETADA' };
      case 'vencida':
        return { bg: colors.vencidaFondo, text: colors.vencida, label: 'VENCIDA' };
      case 'rehabilitada':
        return { bg: colors.abiertaFondo, text: colors.marca, label: 'REHABILITADA' };
      case 'tardio':
      case 'completado_tardio':
        return { bg: colors.vencidaFondo, text: colors.vencida, label: 'COMPLETADO TARDÍO' };
      default:
        return { bg: colors.pendienteFondo, text: colors.pendiente, label: 'PENDIENTE' };
    }
  };

  // ==================== VISTA CONDUCTOR (fragment_home_conductor.xml) ====================
  if (isConductor) {
    // Igual que HomeConductorFragment.kt: filtra las que no son "completada"
    const ordenesActivas = ordenes.filter((o) => o.estado?.toLowerCase() !== 'completada');

    return (
      <View style={styles.flex}>
        <View style={styles.conductorHeader}>
          <Text style={styles.conductorSaludo}>Hola, {user?.nombre || ''}</Text>
          <View style={styles.rowCenter}>
            <TouchableOpacity style={styles.iconBtn} onPress={() => setScreen('historial')}>
              <Text style={styles.iconText}>🕒</Text>
            </TouchableOpacity>
            <TouchableOpacity style={styles.iconBtn} onPress={() => setScreen('perfil')}>
              <Text style={styles.iconText}>👤</Text>
            </TouchableOpacity>
          </View>
        </View>

        <ScrollView
          contentContainerStyle={styles.listContainer}
          refreshControl={<RefreshControl refreshing={loading} onRefresh={cargarDatos} />}
        >
          {ordenesActivas.map((item) => {
            const badge = getBadgeStyle(item.estado);
            return (
              <TouchableOpacity key={item.id} style={styles.card} onPress={() => onOpenOrder(item.id)}>
                <View style={styles.cardHeader}>
                  <Text style={styles.cardTitle}>Orden #{item.numero_orden}</Text>
                  <View style={[styles.badge, { backgroundColor: badge.bg }]}>
                    <Text style={[styles.badgeText, { color: badge.text }]}>{badge.label}</Text>
                  </View>
                </View>
                <Text style={styles.cardSub}>
                  {item.gasolinera?.nombre || 'Gasolinera por definir'}
                </Text>
              </TouchableOpacity>
            );
          })}

          {!loading && ordenesActivas.length === 0 && (
            <Text style={styles.emptyText}>Todavía no tienes órdenes.</Text>
          )}
        </ScrollView>

        <View style={styles.bottomDock}>
          <TouchableOpacity style={styles.btnPrimaryPill} onPress={onOpenNuevaSolicitud}>
            <Text style={styles.btnPrimaryPillText}>＋ Nueva solicitud</Text>
          </TouchableOpacity>
        </View>
      </View>
    );
  }

  // ==================== VISTA ADMINISTRADOR / SUPERVISOR (fragment_home_admin.xml) ====================
  const pendientesCount = solicitudes.filter((s) => s.estado === 'pendiente').length;
  const abiertasCount = ordenes.filter((o) => o.estado === 'abierta').length;
  const completadasCount = ordenes.filter((o) => o.estado === 'completada').length;
  const vencidasCount = ordenes.filter((o) => o.estado === 'vencida').length;

  const proximaOrden = ordenes
    .filter((o) => o.estado === 'abierta' && o.vence_apertura)
    .sort(
      (a, b) =>
        new Date(a.vence_apertura.replace(' ', 'T')).getTime() -
        new Date(b.vence_apertura.replace(' ', 'T')).getTime()
    )[0];

  const minutosRestantesProxima = proximaOrden
    ? Math.max(
        0,
        Math.floor(
          (new Date(proximaOrden.vence_apertura.replace(' ', 'T')).getTime() - Date.now()) / 60000
        )
      )
    : 0;

  const fechaHoy = new Date()
    .toLocaleDateString('es-HN', { weekday: 'long', day: '2-digit', month: 'short', year: 'numeric' })
    .toUpperCase();

  return (
    <View style={styles.flex}>
      {/* Barra superior */}
      <View style={styles.topHeader}>
        <View style={styles.rowCenter}>
          <Text style={{ fontSize: 22, marginRight: 12 }}>⛽</Text>
          <View>
            <Text style={styles.topHeaderTitle}>Inicio</Text>
            <Text style={styles.topHeaderRole}>
              {isAdmin ? 'ADMINISTRADOR' : 'SUPERVISOR'}
            </Text>
          </View>
        </View>
        <TouchableOpacity style={styles.iconBtn} onPress={() => setScreen('notificaciones')}>
          <Text style={styles.iconText}>🔔</Text>
        </TouchableOpacity>
      </View>

      <ScrollView
        contentContainerStyle={styles.container}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={cargarDatos} />}
      >
        <Text style={styles.dateLabel}>{fechaHoy}</Text>
        <Text style={styles.welcomeTitle}>Buen día, {user?.nombre || ''}</Text>

        {/* Tarjeta Solicitudes por asignar */}
        <View style={styles.card}>
          <View style={styles.rowCenter}>
            <Text style={styles.bigNumber}>{pendientesCount}</Text>
            <View style={{ marginLeft: 16, flex: 1 }}>
              <Text style={styles.cardTitle}>Solicitudes pendientes</Text>
              <Text style={styles.cardSub}>Esperan asignación de monto</Text>
            </View>
          </View>
          <TouchableOpacity
            style={styles.btnLightPill}
            onPress={() => setScreen('solicitudes')}
          >
            <Text style={styles.btnLightPillText}>Asignar montos →</Text>
          </TouchableOpacity>
        </View>

        {/* 3 Indicadores del día */}
        <View style={styles.statsRow}>
          <View style={styles.statBox}>
            <Text style={[styles.statNumber, { color: colors.abierta }]}>{abiertasCount}</Text>
            <Text style={styles.statLabel}>ABIERTA</Text>
          </View>
          <View style={styles.statBox}>
            <Text style={[styles.statNumber, { color: colors.completada }]}>{completadasCount}</Text>
            <Text style={styles.statLabel}>COMPLETADA</Text>
          </View>
          <View style={styles.statBox}>
            <Text style={[styles.statNumber, { color: colors.vencida }]}>{vencidasCount}</Text>
            <Text style={styles.statLabel}>VENCIDA</Text>
          </View>
        </View>

        <Text style={styles.sectionSubLabel}>Órdenes del día · vigencia de 15 min</Text>

        {/* Orden próxima a vencer */}
        {proximaOrden && (
          <View style={styles.warningCard}>
            <View style={styles.cardHeader}>
              <Text style={styles.warningTitle}>{proximaOrden.numero_orden}</Text>
              <View style={[styles.badge, { backgroundColor: colors.abiertaFondo }]}>
                <Text style={[styles.badgeText, { color: colors.abierta }]}>ABIERTA</Text>
              </View>
            </View>
            <Text style={styles.warningDetail}>
              {proximaOrden.gasolinera?.nombre || 'Gasolinera'} · Vence a las{' '}
              {proximaOrden.vence_apertura?.substring(11, 16)}
            </Text>
            <View style={styles.warningFooter}>
              <Text style={styles.warningTimer}>Quedan {minutosRestantesProxima} min</Text>
              <TouchableOpacity onPress={() => onOpenOrder(proximaOrden.id)}>
                <Text style={styles.linkText}>Ver orden →</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}

        <Text style={styles.sectionTitle}>Accesos rápidos</Text>
        <View style={styles.actionRow}>
          <TouchableOpacity style={styles.outlineBtn} onPress={() => setScreen('caja_chica')}>
            <Text style={styles.outlineBtnText}>＋ Caja chica</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.outlineBtn} onPress={() => setScreen('catalogos')}>
            <Text style={styles.outlineBtnText}>Catálogos →</Text>
          </TouchableOpacity>
        </View>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  container: { padding: 20 },
  listContainer: { paddingHorizontal: 20, paddingBottom: 20 },
  conductorHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: 20,
  },
  conductorSaludo: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal, flex: 1 },
  iconBtn: { width: 40, height: 40, justifyContent: 'center', alignItems: 'center', marginLeft: 4 },
  iconText: { fontSize: 20 },
  topHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    backgroundColor: colors.blanco,
    padding: 20,
  },
  topHeaderTitle: { fontSize: 21, fontWeight: '700', color: colors.textoPrincipal },
  topHeaderRole: { fontSize: 11, fontWeight: '700', color: colors.marca },
  rowCenter: { flexDirection: 'row', alignItems: 'center' },
  dateLabel: { fontSize: 12, fontWeight: '700', color: colors.textoSecundario, letterSpacing: 0.8 },
  welcomeTitle: { fontSize: 24, fontWeight: '700', color: colors.textoPrincipal, marginTop: 4 },
  card: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: colors.borde,
    marginTop: 16,
  },
  bigNumber: { fontSize: 40, fontWeight: '700', color: colors.marca },
  cardTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  cardSub: { fontSize: 13, color: colors.textoSecundario, marginTop: 4 },
  btnLightPill: {
    backgroundColor: colors.marcaClaro,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 16,
  },
  btnLightPillText: { color: colors.marcaOscuro, fontWeight: '700', fontSize: 14 },
  statsRow: { flexDirection: 'row', gap: 12, marginTop: 16 },
  statBox: {
    flex: 1,
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  statNumber: { fontSize: 24, fontWeight: '700' },
  statLabel: { fontSize: 11, color: colors.textoSecundario, marginTop: 2, fontWeight: '600' },
  sectionSubLabel: { fontSize: 13, color: colors.textoSecundario, marginTop: 20 },
  warningCard: {
    backgroundColor: colors.advertenciaFondo,
    borderRadius: 20,
    padding: 17,
    marginTop: 8,
  },
  warningTitle: { fontSize: 15, fontWeight: '700', color: colors.textoPrincipal },
  warningDetail: { fontSize: 13, color: colors.advertenciaTextoSecundario, marginTop: 8 },
  warningFooter: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
  },
  warningTimer: { fontSize: 14, fontWeight: '700', color: colors.advertenciaTexto },
  linkText: { color: colors.marca, fontWeight: '700', fontSize: 14 },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal, marginTop: 20 },
  actionRow: { flexDirection: 'row', gap: 12, marginTop: 10, marginBottom: 8 },
  outlineBtn: {
    flex: 1,
    height: 44,
    borderRadius: 22,
    borderWidth: 1,
    borderColor: colors.borde,
    backgroundColor: colors.blanco,
    justifyContent: 'center',
    alignItems: 'center',
  },
  outlineBtnText: { color: colors.marca, fontWeight: '700', fontSize: 13 },
  bottomDock: { backgroundColor: colors.blanco, padding: 20 },
  btnPrimaryPill: {
    backgroundColor: colors.marca,
    height: 52,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnPrimaryPillText: { color: colors.blanco, fontSize: 15, fontWeight: '700' },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  badge: { paddingHorizontal: 12, paddingVertical: 5, borderRadius: 12 },
  badgeText: { fontSize: 11, fontWeight: '700' },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 60 },
});