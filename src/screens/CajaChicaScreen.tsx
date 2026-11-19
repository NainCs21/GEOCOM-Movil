import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';
import { obtenerFechaActualString } from '../utils/fechas';

interface Props {
  token: string;
  user: any;
  onBack: () => void;
}

export default function CajaChicaScreen({ token, user, onBack }: Props) {
  const [registros, setRegistros] = useState<any[]>([]);
  const [conductorId, setConductorId] = useState('');
  const [monto, setMonto] = useState('');
  const [descripcion, setDescripcion] = useState('');
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const esAdmin = user?.rol === 'admin';

  const cargar = useCallback(async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/caja-chica`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const json = await res.json();
      setRegistros(Array.isArray(json) ? json : json.data || []);
    } catch {
      setRegistros([]);
    } finally {
      setLoading(false);
    }
  }, [token]);

  useEffect(() => {
    cargar();
  }, [cargar]);

  const registrar = async () => {
    setErrorMsg(null);
    const m = parseFloat(monto);
    if (isNaN(m) || !descripcion.trim()) {
      setErrorMsg('Completa el monto y la descripción.');
      return;
    }

    setSaving(true);
    try {
      const cid = conductorId.trim() ? parseInt(conductorId.trim(), 10) : null;
      const res = await fetch(`${API_URL}/caja-chica`, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          conductor_id: isNaN(cid as number) ? null : cid,
          monto: m,
          descripcion: descripcion.trim(),
          fecha_entrega: obtenerFechaActualString(),
        }),
      });
      const json = await res.json();
      if (!res.ok) throw new Error(json.mensaje || json.message || 'No se pudo registrar.');

      setConductorId('');
      setMonto('');
      setDescripcion('');
      cargar();
    } catch (e: any) {
      setErrorMsg(e.message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <KeyboardAvoidingView
      style={styles.flex}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.topHeader}>
        <TouchableOpacity onPress={onBack} style={styles.backBtn}>
          <Text style={{ fontSize: 22, color: colors.blanco }}>←</Text>
        </TouchableOpacity>
        <Text style={styles.topHeaderTitle}>Caja chica</Text>
      </View>

      <ScrollView contentContainerStyle={{ paddingBottom: 40 }} keyboardShouldPersistTaps="handled">
        {esAdmin && (
          <View style={styles.formCard}>
            <Text style={styles.cardTitle}>Registrar caja chica</Text>

            <TextInput
              style={styles.input}
              placeholder="Id del conductor (opcional)"
              placeholderTextColor={colors.textoSecundario}
              keyboardType="number-pad"
              value={conductorId}
              onChangeText={setConductorId}
            />

            <TextInput
              style={styles.input}
              placeholder="Monto (L)"
              placeholderTextColor={colors.textoSecundario}
              keyboardType="decimal-pad"
              value={monto}
              onChangeText={setMonto}
            />

            <TextInput
              style={styles.input}
              placeholder="Descripción"
              placeholderTextColor={colors.textoSecundario}
              value={descripcion}
              onChangeText={setDescripcion}
            />

            <TouchableOpacity
              style={[styles.btnRegistrar, saving && { opacity: 0.7 }]}
              onPress={registrar}
              disabled={saving}
            >
              <Text style={styles.btnRegistrarText}>Registrar</Text>
            </TouchableOpacity>

            {saving && <ActivityIndicator color={colors.marca} style={{ marginTop: 10 }} />}
            {errorMsg && <Text style={styles.errorText}>{errorMsg}</Text>}
          </View>
        )}

        {loading && <ActivityIndicator color={colors.marca} style={{ marginVertical: 16 }} />}

        <View style={{ paddingHorizontal: 16 }}>
          {registros.map((item) => (
            <View key={item.id} style={styles.rowCard}>
              <Text style={styles.rowTitle}>
                {item.conductor?.usuario?.name || `Conductor #${item.conductor_id || 0}`}
              </Text>
              <Text style={styles.rowSub}>
                L {Number(item.monto).toFixed(2)} · {item.descripcion}
              </Text>
            </View>
          ))}
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
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
  formCard: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    margin: 16,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  cardTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal, marginBottom: 4 },
  input: {
    backgroundColor: colors.blanco,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    padding: 12,
    fontSize: 14,
    color: colors.textoPrincipal,
    marginTop: 10,
  },
  btnRegistrar: {
    backgroundColor: colors.marca,
    height: 48,
    borderRadius: 12,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 16,
  },
  btnRegistrarText: { color: colors.blanco, fontWeight: '700', fontSize: 15 },
  errorText: { color: colors.vencida, textAlign: 'center', marginTop: 8, fontSize: 13 },
  rowCard: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 10,
  },
  rowTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  rowSub: { fontSize: 13, color: colors.textoSecundario, marginTop: 2 },
});