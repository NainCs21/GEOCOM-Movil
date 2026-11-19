import React, { useState, useEffect } from 'react';
import {
  Modal,
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  visible: boolean;
  onClose: () => void;
  token: string;
  onSuccess: () => void;
}

type Destino = 'vehiculo' | 'mgp' | 'ambos';

export default function NuevaSolicitudModal({ visible, onClose, token, onSuccess }: Props) {
  const [destino, setDestino] = useState<Destino>('vehiculo');
  const [vehiculos, setVehiculos] = useState<any[]>([]);
  const [vehiculoId, setVehiculoId] = useState<number | null>(null);
  const [descripcionMgp, setDescripcionMgp] = useState('');
  const [actividad, setActividad] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  useEffect(() => {
    if (visible) {
      setErrorMsg(null);
      setLoading(true);
      fetch(`${API_URL}/vehiculos`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      })
        .then((r) => r.json())
        .then((d) => {
          const list = Array.isArray(d) ? d : d.data || [];
          setVehiculos(list);
          if (list.length > 0 && !vehiculoId) setVehiculoId(list[0].id);
        })
        .finally(() => setLoading(false));
    }
  }, [visible, token]);

  const enviar = async () => {
    setErrorMsg(null);
    const necesitaVehiculo = destino === 'vehiculo' || destino === 'ambos';
    if (necesitaVehiculo && !vehiculoId) {
      setErrorMsg('Elige un destino válido: vehiculo, mgp o ambos.');
      return;
    }
    if (!actividad.trim()) {
      setErrorMsg('Describe la actividad que justifica el combustible.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch(`${API_URL}/solicitudes`, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          tipo_destino: destino,
          vehiculo_id: destino === 'mgp' ? null : vehiculoId,
          descripcion_mgp: destino === 'vehiculo' ? null : descripcionMgp.trim() || null,
          descripcion_actividad: actividad.trim(),
        }),
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.mensaje || data.message || 'Error al crear solicitud.');

      setActividad('');
      setDescripcionMgp('');
      onSuccess();
      onClose();
    } catch (e: any) {
      setErrorMsg(e.message);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal visible={visible} animationType="slide">
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        <View style={styles.topHeader}>
          <TouchableOpacity onPress={onClose} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.blanco }}>←</Text>
          </TouchableOpacity>
          <Text style={styles.topHeaderTitle}>Nueva solicitud</Text>
        </View>

        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          <Text style={styles.title}>¿Para qué necesitas combustible?</Text>

          <Text style={styles.label}>Destino (vehiculo, mgp o ambos)</Text>
          <View style={styles.destinosCol}>
            {(
              [
                { id: 'vehiculo', label: 'Vehículo' },
                { id: 'mgp', label: 'Planta generadora (MGP)' },
                { id: 'ambos', label: 'Ambos (Vehículo + MGP)' },
              ] as const
            ).map((opt) => (
              <TouchableOpacity
                key={opt.id}
                style={[styles.destinoOption, destino === opt.id && styles.destinoOptionActive]}
                onPress={() => setDestino(opt.id)}
              >
                <Text
                  style={[
                    styles.destinoOptionText,
                    destino === opt.id && styles.destinoOptionTextActive,
                  ]}
                >
                  {opt.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>

          {(destino === 'vehiculo' || destino === 'ambos') && (
            <>
              <Text style={styles.label}>Vehículo</Text>
              {loading ? (
                <ActivityIndicator color={colors.marca} style={{ marginVertical: 10 }} />
              ) : (
                <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginBottom: 4 }}>
                  {vehiculos.map((v) => {
                    const partes = [v.placa, v.marca, v.modelo].filter(Boolean);
                    const etiqueta = partes.length > 0 ? partes.join(' - ') : `Vehículo #${v.id}`;
                    const selected = vehiculoId === v.id;
                    return (
                      <TouchableOpacity
                        key={v.id}
                        style={[styles.vehiculoChip, selected && styles.vehiculoChipActive]}
                        onPress={() => setVehiculoId(v.id)}
                      >
                        <Text style={[styles.vehiculoText, selected && { color: colors.blanco }]}>
                          {etiqueta}
                        </Text>
                      </TouchableOpacity>
                    );
                  })}
                </ScrollView>
              )}
            </>
          )}

          {(destino === 'mgp' || destino === 'ambos') && (
            <>
              <Text style={styles.label}>Descripción de la planta generadora (MGP)</Text>
              <TextInput
                style={styles.input}
                placeholder="Descripción de la planta generadora (MGP)"
                placeholderTextColor={colors.textoSecundario}
                value={descripcionMgp}
                onChangeText={setDescripcionMgp}
              />
            </>
          )}

          <Text style={styles.label}>Descripción de la actividad</Text>
          <TextInput
            style={[styles.input, styles.textArea]}
            placeholder="Descripción de la actividad"
            placeholderTextColor={colors.textoSecundario}
            multiline
            numberOfLines={3}
            value={actividad}
            onChangeText={setActividad}
          />

          <TouchableOpacity
            style={[styles.btnSubmit, submitting && { opacity: 0.7 }]}
            onPress={enviar}
            disabled={submitting}
          >
            <Text style={styles.btnSubmitText}>Enviar solicitud</Text>
          </TouchableOpacity>

          {submitting && <ActivityIndicator color={colors.marca} style={{ marginTop: 16 }} />}
          {errorMsg && <Text style={styles.errorText}>{errorMsg}</Text>}
        </ScrollView>
      </KeyboardAvoidingView>
    </Modal>
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
  content: { padding: 20, paddingBottom: 40 },
  title: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal, marginBottom: 20 },
  label: { fontSize: 13, fontWeight: '700', color: colors.textoPrincipal, marginTop: 12, marginBottom: 6 },
  destinosCol: { gap: 8 },
  destinoOption: {
    backgroundColor: colors.blanco,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    padding: 13,
  },
  destinoOptionActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  destinoOptionText: { fontSize: 14, color: colors.textoPrincipal, fontWeight: '600' },
  destinoOptionTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  vehiculoChip: {
    backgroundColor: colors.blanco,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    paddingHorizontal: 14,
    paddingVertical: 12,
    marginRight: 8,
  },
  vehiculoChipActive: { backgroundColor: colors.marca, borderColor: colors.marca },
  vehiculoText: { fontSize: 13, fontWeight: '700', color: colors.textoPrincipal },
  input: {
    backgroundColor: colors.blanco,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    padding: 13,
    fontSize: 14,
    color: colors.textoPrincipal,
  },
  textArea: { minHeight: 72, textAlignVertical: 'top' },
  btnSubmit: {
    backgroundColor: colors.marca,
    height: 50,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 24,
  },
  btnSubmitText: { color: colors.blanco, fontSize: 15, fontWeight: '700' },
  errorText: { color: colors.vencida, textAlign: 'center', marginTop: 12, fontSize: 13 },
});