import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  ScrollView,
  StyleSheet,
  ActivityIndicator,
  Alert,
  RefreshControl,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  token: string;
  user?: any;
  onBack: () => void;
}

export default function CajaChicaScreen({ token, user, onBack }: Props) {
  const isAdmin = user?.rol === 'admin';
  const [historial, setHistorial] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);

  const [conductorId, setConductorId] = useState('');
  const [monto, setMonto] = useState('');
  const [descripcion, setDescripcion] = useState('');

  const fetchCajaChica = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/caja-chica`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      setHistorial(Array.isArray(data) ? data : data.data || []);
    } catch (e) {}
    setLoading(false);
  };

  useEffect(() => {
    fetchCajaChica();
  }, [token]);

  const registrarMonto = async () => {
    if (!monto || !descripcion.trim()) {
      Alert.alert('Aviso', 'Completa el monto y la descripción.');
      return;
    }

    setSaving(true);
    try {
      const res = await fetch(`${API_URL}/caja-chica`, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          conductor_id: conductorId ? parseInt(conductorId) : null,
          monto: parseFloat(monto),
          descripcion: descripcion.trim(),
          fecha_entrega: new Date().toISOString().split('T')[0],
        }),
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.mensaje || data.message || 'Error al registrar.');

      setMonto('');
      setDescripcion('');
      setConductorId('');
      fetchCajaChica();
    } catch (err: any) {
      Alert.alert('Error', err.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <TouchableOpacity onPress={onBack} style={styles.backBtn}>
          <Text style={{ fontSize: 22, color: colors.textoPrincipal }}>←</Text>
        </TouchableOpacity>
        <Text style={styles.topHeaderTitle}>Caja chica</Text>
      </View>

      <ScrollView
        refreshControl={<RefreshControl refreshing={loading} onRefresh={fetchCajaChica} />}
      >
        {/* Solo visible para Administrador (igual que CajaChicaFragment.kt) */}
        {isAdmin && (
          <View style={styles.cardRegistro}>
            <Text style={styles.sectionTitle}>Registrar caja chica</Text>

            <TextInput
              style={styles.input}
              placeholder="Id del conductor (opcional)"
              placeholderTextColor="#9ca3af"
              value={conductorId}
              onChangeText={setConductorId}
              keyboardType="number-pad"
            />

            <TextInput
              style={styles.input}
              placeholder="Monto (L)"
              placeholderTextColor="#9ca3af"
              value={monto}
              onChangeText={setMonto}
              keyboardType="decimal-pad"
            />

            <TextInput
              style={styles.input}
              placeholder="Descripción"
              placeholderTextColor="#9ca3af"
              value={descripcion}
              onChangeText={setDescripcion}
            />

            <TouchableOpacity style={styles.btnPrimary} onPress={registrarMonto} disabled={saving}>
              {saving ? (
                <ActivityIndicator color="#fff" />
              ) : (
                <Text style={styles.btnPrimaryText}>Registrar</Text>
              )}
            </TouchableOpacity>
          </View>
        )}

        <View style={styles.listContainer}>
          {historial.map((item) => (
            <View key={item.id} style={styles.cardItem}>
              <Text style={styles.itemTitle}>
                {item.conductor?.usuario?.name || `Conductor #${item.conductor_id || 0}`}
              </Text>
              <Text style={styles.itemSubtitle}>
                L {Number(item.monto).toFixed(2)} · {item.descripcion}
              </Text>
            </View>
          ))}

          {!loading && historial.length === 0 && (
            <Text style={styles.emptyText}>No hay registros de caja chica.</Text>
          )}
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
    backgroundColor: colors.blanco,
    padding: 20,
  },
  backBtn: { marginRight: 10, padding: 4 },
  topHeaderTitle: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal },
  cardRegistro: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
    borderColor: colors.borde,
    margin: 20,
  },
  sectionTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal, marginBottom: 12 },
  input: {
    backgroundColor: colors.fondo,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 10,
    padding: 12,
    fontSize: 14,
    color: colors.textoPrincipal,
    marginBottom: 10,
  },
  btnPrimary: {
    backgroundColor: colors.marca,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 6,
  },
  btnPrimaryText: { color: colors.blanco, fontSize: 14, fontWeight: '700' },
  listContainer: { paddingHorizontal: 20, paddingBottom: 20, marginTop: 10 },
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
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 30 },
});