import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  token: string;
  onOpenOrder: (id: number) => void;
}

const ESTADOS = ['Todos los estados', 'pendiente', 'abierta', 'completada', 'vencida', 'rehabilitada'];

export default function OrdenesScreen({ token, onOpenOrder }: Props) {
  const [ordenes, setOrdenes] = useState<any[]>([]);
  const [conductores, setConductores] = useState<any[]>([]);
  const [estadoFiltro, setEstadoFiltro] = useState<string>('Todos los estados');
  const [conductorFiltro, setConductorFiltro] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    fetch(`${API_URL}/conductores`, {
      headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setConductores(Array.isArray(d) ? d : d.data || []))
      .catch(() => {});
  }, [token]);

  const fetchOrdenes = async () => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (estadoFiltro !== 'Todos los estados') params.append('estado', estadoFiltro);
      if (conductorFiltro) params.append('conductor_id', String(conductorFiltro));

      const query = params.toString() ? `?${params.toString()}` : '';
      const res = await fetch(`${API_URL}/ordenes${query}`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      setOrdenes(Array.isArray(data) ? data : data.data || []);
    } catch (e) {}
    setLoading(false);
  };

  useEffect(() => {
    fetchOrdenes();
  }, [token, estadoFiltro, conductorFiltro]);

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

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <Text style={styles.topHeaderTitle}>Órdenes</Text>
      </View>

      {/* Filtros idénticos a OrdenesAdminFragment.kt */}
      <View style={styles.filtersBox}>
        <Text style={styles.filterLabel}>Filtrar por estado</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipsRow}>
          {ESTADOS.map((est) => {
            const active = estadoFiltro === est;
            return (
              <TouchableOpacity
                key={est}
                style={[styles.chip, active && styles.chipActive]}
                onPress={() => setEstadoFiltro(est)}
              >
                <Text style={[styles.chipText, active && styles.chipTextActive]}>{est}</Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>

        <Text style={[styles.filterLabel, { marginTop: 10 }]}>Filtrar por conductor</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipsRow}>
          <TouchableOpacity
            style={[styles.chip, conductorFiltro === null && styles.chipActive]}
            onPress={() => setConductorFiltro(null)}
          >
            <Text style={[styles.chipText, conductorFiltro === null && styles.chipTextActive]}>
              Todos los conductores
            </Text>
          </TouchableOpacity>
          {conductores.map((c) => {
            const active = conductorFiltro === c.id;
            return (
              <TouchableOpacity
                key={c.id}
                style={[styles.chip, active && styles.chipActive]}
                onPress={() => setConductorFiltro(c.id)}
              >
                <Text style={[styles.chipText, active && styles.chipTextActive]}>
                  {c.usuario?.name || `Conductor #${c.id}`}
                </Text>
              </TouchableOpacity>
            );
          })}
        </ScrollView>
      </View>

      <FlatList
        data={ordenes}
        keyExtractor={(item) => item.id.toString()}
        onRefresh={fetchOrdenes}
        refreshing={loading}
        contentContainerStyle={styles.list}
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
          !loading ? <Text style={styles.emptyText}>Todavía no tienes órdenes.</Text> : undefined
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  topHeader: { backgroundColor: colors.blanco, padding: 20 },
  topHeaderTitle: { fontSize: 21, fontWeight: '700', color: colors.textoPrincipal },
  filtersBox: {
    backgroundColor: colors.blanco,
    paddingHorizontal: 16,
    paddingBottom: 16,
    borderBottomWidth: 1,
    borderBottomColor: colors.borde,
  },
  filterLabel: { fontSize: 12, fontWeight: '700', color: colors.textoSecundario, marginBottom: 6 },
  chipsRow: { flexDirection: 'row' },
  chip: {
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.borde,
    backgroundColor: colors.fondo,
    marginRight: 8,
  },
  chipActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  chipText: { fontSize: 12, fontWeight: '600', color: colors.textoSecundario },
  chipTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  list: { padding: 20 },
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
});