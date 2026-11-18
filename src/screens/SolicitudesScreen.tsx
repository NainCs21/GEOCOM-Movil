import React, { useState, useEffect } from 'react';
import { View, Text, FlatList, StyleSheet, TouchableOpacity, Alert } from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  token: string;
  user: any;
  onAsignar: (solicitud: any) => void;
}

export default function SolicitudesScreen({ token, user, onAsignar }: Props) {
  const [solicitudes, setSolicitudes] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const isAdmin = user?.rol === 'admin';

  const fetchSolicitudes = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/solicitudes`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      const lista = Array.isArray(data) ? data : data.data || [];
      setSolicitudes(lista.filter((s: any) => s.estado === 'pendiente'));
    } catch (e) {}
    setLoading(false);
  };

  useEffect(() => {
    fetchSolicitudes();
  }, [token]);

  const confirmarRechazo = (solicitud: any) => {
    Alert.alert('Rechazar', '¿Estás seguro de que deseas rechazar esta solicitud?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Rechazar',
        style: 'destructive',
        onPress: async () => {
          try {
            const res = await fetch(`${API_URL}/solicitudes/${solicitud.id}/rechazar`, {
              method: 'POST',
              headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
            });
            if (!res.ok) throw new Error('No se pudo rechazar la solicitud.');
            Alert.alert('Éxito', 'Solicitud rechazada con éxito');
            fetchSolicitudes();
          } catch (e: any) {
            Alert.alert('Error', e.message);
          }
        },
      },
    ]);
  };

  const etiquetaDestino = (tipo: string) => {
    if (tipo === 'vehiculo') return 'VEHÍCULO';
    if (tipo === 'mgp') return 'MGP';
    if (tipo === 'ambos') return 'VEHÍCULO + MGP';
    return (tipo || 'VEHÍCULO').toUpperCase();
  };

  const calcularAntiguedad = (createdAt?: string) => {
    if (!createdAt) return '';
    const diffMs = Date.now() - new Date(createdAt.replace(' ', 'T')).getTime();
    const min = Math.max(0, Math.floor(diffMs / 60000));
    if (min < 60) return `Hace ${min} min`;
    return `Hace ${Math.floor(min / 60)} h`;
  };

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <Text style={styles.topHeaderTitle}>Solicitudes pendientes</Text>
        {!isAdmin && (
          <View style={styles.infoBanner}>
            <Text style={styles.infoBannerText}>
              Tu rol es de solo consulta: puedes ver las solicitudes, pero no asignar montos.
            </Text>
          </View>
        )}
      </View>

      <FlatList
        data={solicitudes}
        keyExtractor={(item) => item.id.toString()}
        onRefresh={fetchSolicitudes}
        refreshing={loading}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => (
          <TouchableOpacity
            style={styles.cardItem}
            activeOpacity={isAdmin ? 0.7 : 1}
            onPress={() => isAdmin && onAsignar(item)}
          >
            <View style={styles.row}>
              <Text style={styles.codeText}>SOL-{String(item.id).padStart(4, '0')}</Text>
              <Text style={styles.dateText}>{calcularAntiguedad(item.created_at)}</Text>
            </View>

            <Text style={styles.conductorName}>
              {item.conductor?.usuario?.name || `Conductor #${item.conductor_id}`}
            </Text>

            <View style={styles.destinationBadge}>
              <Text style={styles.destinationText}>{etiquetaDestino(item.tipo_destino)}</Text>
            </View>

            <Text style={styles.activityText}>{item.descripcion_actividad}</Text>

            {isAdmin && (
              <View style={styles.actionsRow}>
                <TouchableOpacity onPress={() => confirmarRechazo(item)}>
                  <Text style={styles.rejectLink}>Rechazar</Text>
                </TouchableOpacity>
                <TouchableOpacity onPress={() => onAsignar(item)}>
                  <Text style={styles.assignLink}>Asignar monto →</Text>
                </TouchableOpacity>
              </View>
            )}
          </TouchableOpacity>
        )}
        ListEmptyComponent={
          !loading ? <Text style={styles.emptyText}>No hay solicitudes pendientes.</Text> : undefined
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  topHeader: { backgroundColor: colors.blanco, padding: 20 },
  topHeaderTitle: { fontSize: 21, fontWeight: '700', color: colors.textoPrincipal },
  infoBanner: {
    marginTop: 8,
    backgroundColor: colors.marcaClaro,
    padding: 12,
    borderRadius: 12,
  },
  infoBannerText: { color: colors.marcaOscuro, fontSize: 13 },
  list: { padding: 20 },
  cardItem: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 17,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 12,
  },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  codeText: { fontSize: 15, fontWeight: '700', color: colors.textoPrincipal },
  dateText: { fontSize: 12, color: colors.textoSecundario },
  conductorName: { fontSize: 18, fontWeight: '700', color: colors.textoPrincipal, marginTop: 6 },
  destinationBadge: {
    backgroundColor: colors.marcaClaro,
    alignSelf: 'flex-start',
    paddingHorizontal: 10,
    paddingVertical: 5,
    borderRadius: 12,
    marginTop: 8,
  },
  destinationText: { color: colors.marcaOscuro, fontWeight: '700', fontSize: 12 },
  activityText: { fontSize: 14, color: colors.textoSecundario, marginTop: 10 },
  actionsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 12,
  },
  rejectLink: { color: colors.vencida, fontWeight: '700', fontSize: 14, padding: 4 },
  assignLink: { color: colors.marca, fontWeight: '700', fontSize: 14, padding: 4 },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 40 },
});