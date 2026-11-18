import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  token: string;
  onBack?: () => void;
  showBack?: boolean;
}

export default function ReportesScreen({ token, onBack, showBack = true }: Props) {
  const [tab, setTab] = useState<'conductor' | 'gasolinera'>('conductor');
  const [datos, setDatos] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    setLoading(true);
    fetch(`${API_URL}/reportes/por-${tab}`, {
      headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setDatos(Array.isArray(d) ? d : d.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [tab, token]);

  const mostrarDetalle = (item: any) => {
    if (tab === 'conductor') {
      Alert.alert(
        item.nombre_conductor,
        `ID Conductor: ${item.conductor_id}\nÓrdenes totales: ${item.cantidad_ordenes}\nMonto total: L ${Number(
          item.monto_total
        ).toFixed(2)}`
      );
    } else {
      Alert.alert(
        item.nombre_gasolinera,
        `ID Gasolinera: ${item.gasolinera_id}\nÓrdenes totales: ${item.cantidad_ordenes}\nMonto total: L ${Number(
          item.monto_total
        ).toFixed(2)}`
      );
    }
  };

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        {showBack && onBack && (
          <TouchableOpacity onPress={onBack} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.textoPrincipal }}>←</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.topHeaderTitle}>Reportes</Text>
      </View>

      <View style={styles.toggleRow}>
        <TouchableOpacity
          style={[styles.toggleBtn, tab === 'conductor' && styles.toggleBtnActive]}
          onPress={() => setTab('conductor')}
        >
          <Text style={[styles.toggleText, tab === 'conductor' && styles.toggleTextActive]}>
            Por conductor
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.toggleBtn, tab === 'gasolinera' && styles.toggleBtnActive]}
          onPress={() => setTab('gasolinera')}
        >
          <Text style={[styles.toggleText, tab === 'gasolinera' && styles.toggleTextActive]}>
            Por gasolinera
          </Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={datos}
        keyExtractor={(_, index) => index.toString()}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <TouchableOpacity style={styles.cardItem} onPress={() => mostrarDetalle(item)}>
            <Text style={styles.itemTitle}>
              {item.nombre_conductor || item.nombre_gasolinera}
            </Text>
            <Text style={styles.itemSubtitle}>
              {item.cantidad_ordenes} órdenes · L {Number(item.monto_total).toFixed(2)}
            </Text>
          </TouchableOpacity>
        )}
        ListEmptyComponent={
          loading ? (
            <ActivityIndicator color={colors.marca} style={{ marginTop: 20 }} />
          ) : (
            <Text style={styles.emptyText}>No hay datos para mostrar.</Text>
          )
        }
      />
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
  toggleRow: { flexDirection: 'row', backgroundColor: colors.blanco, padding: 16, gap: 8 },
  toggleBtn: {
    flex: 1,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  toggleBtnActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  toggleText: { fontSize: 13, color: colors.textoSecundario, fontWeight: '600' },
  toggleTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  list: { padding: 20 },
  cardItem: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 10,
  },
  itemTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  itemSubtitle: { fontSize: 13, color: colors.textoSecundario, marginTop: 2 },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 40 },
});