import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, ActivityIndicator } from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  token: string;
  onBack: () => void;
  onOpenOrder: (id: number) => void;
}

export default function HistorialScreen({ token, onBack, onOpenOrder }: Props) {
  const [ordenes, setOrdenes] = useState<any[]>([]);
  const [filtro, setFiltro] = useState<'todas' | 'completadas'>('todas');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    fetch(`${API_URL}/ordenes`, {
      headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setOrdenes(Array.isArray(d) ? d : d.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [token]);

  const filtradas =
    filtro === 'completadas'
      ? ordenes.filter((o) => o.estado?.toLowerCase() === 'completada')
      : ordenes;

  const getBadgeStyle = (estado: string) => {
    switch (estado?.toLowerCase()) {
      case 'abierta':
        return { bg: colors.abiertaFondo, text: colors.abierta, label: 'ABIERTA' };
      case 'completada':
        return { bg: colors.completadaFondo, text: colors.completada, label: 'COMPLETADA' };
      case 'vencida':
        return { bg: colors.vencidaFondo, text: colors.vencida, label: 'VENCIDA' };
      default:
        return { bg: colors.pendienteFondo, text: colors.pendiente, label: 'PENDIENTE' };
    }
  };

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <TouchableOpacity onPress={onBack} style={styles.backBtn}>
          <Text style={{ fontSize: 22, color: colors.textoPrincipal }}>←</Text>
        </TouchableOpacity>
        <Text style={styles.topHeaderTitle}>Mi historial</Text>
      </View>

      <View style={styles.body}>
        <Text style={styles.subtitulo}>Consulta el estado final de tus órdenes.</Text>

        <View style={styles.toggleRow}>
          <TouchableOpacity
            style={[styles.toggleBtn, filtro === 'todas' && styles.toggleBtnActive]}
            onPress={() => setFiltro('todas')}
          >
            <Text style={[styles.toggleText, filtro === 'todas' && styles.toggleTextActive]}>
              Todas
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={[styles.toggleBtn, filtro === 'completadas' && styles.toggleBtnActive]}
            onPress={() => setFiltro('completadas')}
          >
            <Text style={[styles.toggleText, filtro === 'completadas' && styles.toggleTextActive]}>
              Completadas
            </Text>
          </TouchableOpacity>
        </View>

        <FlatList
          data={filtradas}
          keyExtractor={(item) => item.id.toString()}
          contentContainerStyle={{ paddingTop: 16, paddingBottom: 16 }}
          renderItem={({ item }) => {
            const badge = getBadgeStyle(item.estado);
            return (
              <TouchableOpacity style={styles.cardItem} onPress={() => onOpenOrder(item.id)}>
                <View style={styles.cardHeader}>
                  <Text style={styles.orderNumber}>Orden #{item.numero_orden}</Text>
                  <View style={[styles.badge, { backgroundColor: badge.bg }]}>
                    <Text style={[styles.badgeText, { color: badge.text }]}>{badge.label}</Text>
                  </View>
                </View>
                <Text style={styles.stationText}>
                  {item.gasolinera?.nombre || 'Gasolinera por definir'}
                </Text>
              </TouchableOpacity>
            );
          }}
          ListEmptyComponent={
            loading ? (
              <ActivityIndicator color={colors.marca} style={{ marginTop: 20 }} />
            ) : (
              <Text style={styles.emptyText}>Todavía no tienes órdenes.</Text>
            )
          }
        />

        <Text style={styles.privacidad}>Este historial muestra únicamente tus órdenes.</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  topHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.blanco,
    padding: 20,
  },
  backBtn: { marginRight: 10, padding: 4 },
  topHeaderTitle: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal },
  body: { flex: 1, padding: 20 },
  subtitulo: { fontSize: 14, color: colors.textoSecundario, marginBottom: 12 },
  toggleRow: { flexDirection: 'row', gap: 8 },
  toggleBtn: {
    paddingHorizontal: 18,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.borde,
    backgroundColor: colors.blanco,
    justifyContent: 'center',
    alignItems: 'center',
  },
  toggleBtnActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  toggleText: { fontSize: 13, fontWeight: '600', color: colors.textoSecundario },
  toggleTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  cardItem: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 17,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 12,
  },
  cardHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  orderNumber: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  badge: { paddingHorizontal: 12, paddingVertical: 5, borderRadius: 12 },
  badgeText: { fontSize: 11, fontWeight: '700' },
  stationText: { fontSize: 14, color: colors.textoSecundario, marginTop: 6 },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 40 },
  privacidad: { fontSize: 12, color: colors.textoSecundario, textAlign: 'center' },
});