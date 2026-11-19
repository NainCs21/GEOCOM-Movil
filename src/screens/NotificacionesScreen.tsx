import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  RefreshControl,
} from 'react-native';
import { colors } from '../theme/colors';
import { formatoLegible } from '../utils/fechas';
import {
  NotificacionApp,
  sincronizarNotificaciones,
  marcarNotificacionLeida,
  marcarTodasNotificacionesLeidas,
} from '../utils/notificaciones';
import { setNotificacionesScreenActive } from '../utils/notificador';

interface Props {
  token: string;
  user: any;
  onBack: () => void;
  showBack?: boolean;
  onOpenOrder: (id: number) => void;
  onOpenSolicitudes: () => void;
  onNotificacionesUpdate?: (count: number) => void;
}

export default function NotificacionesScreen({
  token,
  user,
  onBack,
  showBack = true,
  onOpenOrder,
  onOpenSolicitudes,
  onNotificacionesUpdate,
}: Props) {
  const [items, setItems] = useState<NotificacionApp[]>([]);
  const [loading, setLoading] = useState(false);

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      const listas = await sincronizarNotificaciones(token, user.id);
      setItems(listas);
      const noLeidas = listas.filter((n) => !n.leida).length;
      if (onNotificacionesUpdate) onNotificacionesUpdate(noLeidas);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  }, [token, user.id, onNotificacionesUpdate]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  // Evita mostrar banners nativos mientras esta pantalla está abierta en primer plano
  useEffect(() => {
    setNotificacionesScreenActive(true);
    return () => {
      setNotificacionesScreenActive(false);
    };
  }, []);

  const handleMarcarTodas = async () => {
    const act = await marcarTodasNotificacionesLeidas(user.id);
    setItems(act);
    if (onNotificacionesUpdate) onNotificacionesUpdate(0);
  };

  const handlePress = async (item: NotificacionApp) => {
    if (!item.leida) {
      const act = await marcarNotificacionLeida(user.id, item.id);
      setItems(act);
      const noLeidas = act.filter((n) => !n.leida).length;
      if (onNotificacionesUpdate) onNotificacionesUpdate(noLeidas);
    }

    if (item.destino === 'orden' && item.ordenId) {
      onOpenOrder(item.ordenId);
    } else if (item.destino === 'solicitudes') {
      onOpenSolicitudes();
    }
  };

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        {showBack && (
          <TouchableOpacity onPress={onBack} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.blanco }}>←</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.topHeaderTitle}>Notificaciones</Text>
        <TouchableOpacity style={styles.btnMarcar} onPress={handleMarcarTodas}>
          <Text style={styles.btnMarcarText}>Marcar leídas</Text>
        </TouchableOpacity>
      </View>

      <FlatList
        data={items}
        keyExtractor={(item) => item.id}
        contentContainerStyle={{ paddingHorizontal: 20, paddingTop: 16, paddingBottom: 88 }}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={cargar} />}
        renderItem={({ item }) => {
          const isUnread = !item.leida;
          return (
            <TouchableOpacity
              style={[styles.card, isUnread && styles.cardUnread]}
              onPress={() => handlePress(item)}
            >
              <View style={{ flexDirection: 'row', alignItems: 'center' }}>
                <Text style={[styles.titulo, isUnread && styles.tituloUnread, { flex: 1 }]}>
                  {item.titulo}
                </Text>
                <Text style={styles.hora}>
                  {formatoLegible(new Date(item.fechaMillis).toISOString())}
                </Text>
              </View>
              <Text style={styles.cuerpo}>{item.cuerpo}</Text>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          !loading ? (
            <Text style={styles.emptyText}>No hay notificaciones por ahora.</Text>
          ) : undefined
        }
      />

      <Text style={styles.footerNote}>
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
    backgroundColor: colors.marca,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  backBtn: { marginRight: 8, padding: 4 },
  topHeaderTitle: { flex: 1, fontSize: 18, fontWeight: '700', color: colors.blanco },
  btnMarcar: { padding: 8 },
  btnMarcarText: { color: colors.blanco, fontSize: 13, fontWeight: '700' },
  card: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 10,
  },
  cardUnread: {
    backgroundColor: colors.marcaClaro,
    borderColor: colors.marcaClaro,
  },
  titulo: { fontSize: 14, color: colors.textoPrincipal },
  tituloUnread: { fontWeight: '700' },
  hora: { fontSize: 11, color: colors.textoSecundario },
  cuerpo: { fontSize: 13, color: colors.textoSecundario, marginTop: 6 },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 60 },
  footerNote: {
    textAlign: 'center',
    color: colors.textoSecundario,
    fontSize: 12,
    padding: 16,
  },
});