import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, TouchableOpacity, StyleSheet } from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  token: string;
  onBack?: () => void;
  showBack?: boolean;
  onOpenOrder: (id: number) => void;
}

export default function NotificacionesScreen({ token, onBack, showBack = true, onOpenOrder }: Props) {
  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const cargarNotificaciones = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/ordenes`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      const ordenes = Array.isArray(data) ? data : data.data || [];

      const relevantes = ordenes
        .filter((o: any) => o.estado === 'completada' || o.estado === 'vencida')
        .sort((a: any, b: any) => {
          const fa = a.fecha_completada || a.vence_apertura || a.fecha_asignacion || '';
          const fb = b.fecha_completada || b.vence_apertura || b.fecha_asignacion || '';
          return fb.localeCompare(fa);
        })
        .slice(0, 30);

      setItems(relevantes);
    } catch (e) {}
    setLoading(false);
  };

  useEffect(() => {
    cargarNotificaciones();
  }, [token]);

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        {showBack && onBack && (
          <TouchableOpacity onPress={onBack} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.textoPrincipal }}>←</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.topHeaderTitle}>Notificaciones</Text>
      </View>

      <FlatList
        data={items}
        keyExtractor={(item) => item.id.toString()}
        onRefresh={cargarNotificaciones}
        refreshing={loading}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const esCompletada = item.estado === 'completada';
          const conductorNombre = item.conductor?.usuario?.name || `Conductor #${item.conductor_id}`;
          const titulo = esCompletada
            ? `COMPLETADA · ${item.numero_orden}`
            : `VENCIDA · ${item.numero_orden}`;
          const cuerpo = esCompletada
            ? `${conductorNombre} completó su carga en ${item.gasolinera?.nombre || 'la gasolinera'}.`
            : `${conductorNombre} no completó la orden ${item.numero_orden} dentro del tiempo de vigencia.`;
          const hora = (esCompletada ? item.fecha_completada : item.vence_apertura)?.substring(0, 16) || '';

          return (
            <TouchableOpacity style={styles.cardItem} onPress={() => onOpenOrder(item.id)}>
              <View style={styles.rowBetween}>
                <Text style={styles.title}>{titulo}</Text>
                <Text style={styles.hora}>{hora}</Text>
              </View>
              <Text style={styles.cuerpo}>{cuerpo}</Text>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          !loading ? <Text style={styles.emptyText}>No hay notificaciones por ahora.</Text> : undefined
        }
      />

      <Text style={styles.footerNota}>
        Se generan a partir de tus órdenes completadas y vencidas recientes.
      </Text>
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
  list: { padding: 20 },
  cardItem: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 12,
  },
  rowBetween: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  title: { fontSize: 15, fontWeight: '700', color: colors.textoPrincipal, flex: 1 },
  hora: { fontSize: 12, color: colors.textoSecundario },
  cuerpo: { fontSize: 13, color: colors.textoSecundario, marginTop: 4 },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 40 },
  footerNota: {
    fontSize: 12,
    color: colors.textoSecundario,
    textAlign: 'center',
    padding: 16,
  },
});