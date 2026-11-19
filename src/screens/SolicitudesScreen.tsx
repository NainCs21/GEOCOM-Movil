import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Alert,
  RefreshControl,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';
import { parseFechaIso } from '../utils/fechas';

interface Props {
  token: string;
  user: any;
  onAsignar: (solicitud: any) => void;
  onBack?: () => void;
}

export default function SolicitudesScreen({ token, user, onAsignar, onBack }: Props) {
  const [solicitudes, setSolicitudes] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const esAdmin = user?.rol === 'admin';

  const cargarSolicitudes = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/solicitudes`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const json = await res.json();
      const lista = Array.isArray(json) ? json : json.data || [];
      setSolicitudes(lista.filter((s: any) => s.estado === 'pendiente'));
    } catch {
      setSolicitudes([]);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    cargarSolicitudes();
  }, [cargarSolicitudes]);

  const confirmarRechazar = (sol: any) => {
    if (!esAdmin) return;
    Alert.alert('Rechazar', '¿Estás seguro de que deseas rechazar esta solicitud?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Aceptar',
        style: 'destructive',
        onPress: async () => {
          try {
            const res = await fetch(`${API_URL}/solicitudes/${sol.id}/rechazar`, {
              method: 'POST',
              headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
            });
            if (!res.ok) throw new Error('No se pudo rechazar la solicitud.');
            Alert.alert('Éxito', 'Solicitud rechazada con éxito');
            cargarSolicitudes();
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
    return (tipo || '').toUpperCase();
  };

  const antiguedad = (createdAt?: string) => {
    if (!createdAt) return '';
    const mins = Math.max(0, Math.floor((Date.now() - parseFechaIso(createdAt).getTime()) / 60000));
    return mins < 60 ? `Hace ${mins} min` : `Hace ${Math.floor(mins / 60)} h`;
  };

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        {onBack && (
          <TouchableOpacity onPress={onBack} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.blanco }}>←</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.topHeaderTitle}>Solicitudes pendientes</Text>
      </View>

      {!esAdmin && (
        <View style={styles.helpBanner}>
          <Text style={styles.helpText}>
            Tu rol es de solo consulta: puedes ver las solicitudes, pero no asignar montos.
          </Text>
        </View>
      )}

      <FlatList
        data={solicitudes}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={{ paddingHorizontal: 20, paddingTop: 16, paddingBottom: 88 }}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={cargarSolicitudes} />}
        renderItem={({ item }) => {
          const codigo = `SOL-${String(item.id).padStart(4, '0')}`;
          return (
            <TouchableOpacity
              style={styles.card}
              activeOpacity={esAdmin ? 0.85 : 1}
              onPress={() => {
                if (esAdmin) onAsignar(item);
              }}
            >
              <View style={styles.iconBox}>
                <Text style={{ fontSize: 16 }}>📋</Text>
              </View>

              <View style={{ flex: 1, marginLeft: 12 }}>
                <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                  <Text style={styles.codigoText}>{codigo}</Text>
                  <Text style={styles.tiempoText}>{antiguedad(item.created_at)}</Text>
                </View>

                <Text style={styles.conductorText}>
                  {item.conductor?.usuario?.name || `Conductor #${item.conductor_id}`}
                </Text>

                <View style={styles.destinoPill}>
                  <Text style={styles.destinoText}>{etiquetaDestino(item.tipo_destino)}</Text>
                </View>

                <Text style={styles.actividadText}>{item.descripcion_actividad}</Text>

                {esAdmin && (
                  <View style={styles.actionsRow}>
                    <TouchableOpacity onPress={() => confirmarRechazar(item)}>
                      <Text style={styles.rechazarText}>Rechazar</Text>
                    </TouchableOpacity>
                    <TouchableOpacity onPress={() => onAsignar(item)}>
                      <Text style={styles.asignarText}>Asignar monto  →</Text>
                    </TouchableOpacity>
                  </View>
                )}
              </View>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          !loading ? (
            <Text style={styles.emptyText}>No hay solicitudes pendientes.</Text>
          ) : undefined
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
    backgroundColor: colors.marca,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  backBtn: { marginRight: 8, padding: 4 },
  topHeaderTitle: { fontSize: 18, fontWeight: '700', color: colors.blanco },
  helpBanner: {
    backgroundColor: colors.marcaClaro,
    margin: 16,
    padding: 12,
    borderRadius: 12,
  },
  helpText: { color: colors.marcaOscuro, fontSize: 13 },
  card: {
    flexDirection: 'row',
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 12,
  },
  iconBox: {
    width: 36,
    height: 36,
    borderRadius: 10,
    backgroundColor: colors.marcaClaro,
    justifyContent: 'center',
    alignItems: 'center',
  },
  codigoText: { flex: 1, fontSize: 14, fontWeight: '700', color: colors.textoPrincipal },
  tiempoText: { fontSize: 12, color: colors.textoSecundario },
  conductorText: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal, marginTop: 4 },
  destinoPill: {
    alignSelf: 'flex-start',
    backgroundColor: colors.marcaClaro,
    paddingHorizontal: 8,
    paddingVertical: 4,
    borderRadius: 8,
    marginTop: 6,
  },
  destinoText: { fontSize: 12, fontWeight: '700', color: colors.marcaOscuro },
  actividadText: { fontSize: 13, color: colors.textoSecundario, marginTop: 6 },
  actionsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 10,
  },
  rechazarText: { color: colors.vencida, fontWeight: '700', fontSize: 13 },
  asignarText: { color: colors.marca, fontWeight: '700', fontSize: 13 },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 60 },
});