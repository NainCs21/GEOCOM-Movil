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
  Alert,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  solicitud: any | null;
  onClose: () => void;
  token: string;
  onSuccess: () => void;
}

export default function AsignarMontoModal({ solicitud, onClose, token, onSuccess }: Props) {
  const [gasolineras, setGasolineras] = useState<any[]>([]);
  const [vehiculos, setVehiculos] = useState<any[]>([]);
  const [gasolineraId, setGasolineraId] = useState<number | null>(null);
  const [horasVigencia, setHorasVigencia] = useState('2');
  const [montoVehiculo, setMontoVehiculo] = useState('');
  const [montoMgp, setMontoMgp] = useState('');
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const tipo = (solicitud?.tipo_destino || 'vehiculo').toLowerCase();
  const esVehiculo = tipo === 'vehiculo' || tipo === 'ambos' || tipo.includes('vehiculo') || tipo.includes('+');
  const esMgp = tipo === 'mgp' || tipo === 'ambos' || tipo.includes('mgp') || tipo.includes('+');

  useEffect(() => {
    if (solicitud) {
      setErrorMsg(null);
      setMontoVehiculo('');
      setMontoMgp('');
      setHorasVigencia('2');
      setLoading(true);
      const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` };
      Promise.all([
        fetch(`${API_URL}/gasolineras`, { headers }).then((r) => r.json()).catch(() => []),
        fetch(`${API_URL}/vehiculos`, { headers }).then((r) => r.json()).catch(() => []),
      ])
        .then(([gasData, vehData]) => {
          const gList = (Array.isArray(gasData) ? gasData : gasData.data || []).filter((g: any) => g.activo);
          const vList = Array.isArray(vehData) ? vehData : vehData.data || [];
          setGasolineras(gList);
          setVehiculos(vList);
          if (gList.length > 0) setGasolineraId(gList[0].id);
        })
        .finally(() => setLoading(false));
    }
  }, [solicitud, token]);

  const totalCalculado =
    (esVehiculo ? parseFloat(montoVehiculo) || 0 : 0) +
    (esMgp ? parseFloat(montoMgp) || 0 : 0);

  const asignarOrden = async () => {
    setErrorMsg(null);
    const horas = parseInt(horasVigencia);
    if (!gasolineraId || isNaN(horas) || horas < 1 || horas > 3) {
      setErrorMsg('Elige la gasolinera e indica las horas de vigencia (1 a 3).');
      return;
    }

    const detalles: any[] = [];
    if (esVehiculo) {
      const m = parseFloat(montoVehiculo);
      if (!isNaN(m) && m > 0) {
        const targetVehiculoId = solicitud.vehiculo_id || vehiculos[0]?.id || 1;
        detalles.push({ tipo_destino: 'vehiculo', vehiculo_id: targetVehiculoId, monto: m });
      }
    }
    if (esMgp) {
      const m = parseFloat(montoMgp);
      if (!isNaN(m) && m > 0) {
        detalles.push({ tipo_destino: 'mgp', descripcion_mgp: solicitud.descripcion_mgp, monto: m });
      }
    }

    if (detalles.length === 0) {
      setErrorMsg('Ingresa al menos un monto.');
      return;
    }

    setSubmitting(true);
    try {
      const res = await fetch(`${API_URL}/ordenes`, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({
          solicitud_id: solicitud.id,
          gasolinera_id: gasolineraId,
          horas_vigencia: horas,
          detalles,
        }),
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.mensaje || data.message || 'Error al generar orden.');

      onSuccess();
      onClose();
    } catch (e: any) {
      setErrorMsg(e.message);
    } finally {
      setSubmitting(false);
    }
  };

  if (!solicitud) return null;

  const etiquetaDestino =
    tipo === 'vehiculo'
      ? 'Vehículo'
      : tipo === 'mgp'
      ? 'Planta generadora'
      : 'Vehículo + Planta generadora';

  return (
    <Modal visible={!!solicitud} animationType="slide">
      <View style={styles.flex}>
        <View style={styles.topHeader}>
          <TouchableOpacity onPress={onClose} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.textoPrincipal }}>←</Text>
          </TouchableOpacity>
          <Text style={styles.topHeaderTitle}>Asignar monto</Text>
        </View>

        <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
          {/* Resumen de la solicitud */}
          <View style={styles.card}>
            <Text style={styles.resumenConductor}>
              {solicitud.conductor?.usuario?.name || `Conductor #${solicitud.conductor_id}`}
            </Text>
            <Text style={styles.resumenDestino}>{etiquetaDestino}</Text>
            <Text style={styles.resumenActividad}>{solicitud.descripcion_actividad}</Text>
          </View>

          <Text style={styles.label}>Gasolinera</Text>
          {loading ? (
            <ActivityIndicator color={colors.marca} />
          ) : (
            <View style={{ gap: 8 }}>
              {gasolineras.map((g) => {
                const selected = gasolineraId === g.id;
                const nombreCompleto = g.direccion ? `${g.nombre} (${g.direccion})` : g.nombre;
                return (
                  <TouchableOpacity
                    key={g.id}
                    style={[styles.gasolineraItem, selected && styles.gasolineraItemActive]}
                    onPress={() => setGasolineraId(g.id)}
                  >
                    <Text style={[styles.gasolineraText, selected && styles.gasolineraTextActive]}>
                      {nombreCompleto}
                    </Text>
                  </TouchableOpacity>
                );
              })}
            </View>
          )}

          <Text style={styles.label}>Horas de vigencia (1 a 3)</Text>
          <TextInput
            style={styles.input}
            keyboardType="number-pad"
            value={horasVigencia}
            onChangeText={setHorasVigencia}
            placeholder="Horas de vigencia (1 a 3)"
          />

          {/* Tarjeta 01 · Vehículo */}
          {esVehiculo && (
            <View style={[styles.card, { marginTop: 16 }]}>
              <Text style={styles.lineHeader}>01 · Vehículo</Text>
              <TextInput
                style={[styles.input, { marginTop: 10 }]}
                placeholder="Monto para el vehículo (L)"
                placeholderTextColor={colors.textoSecundario}
                keyboardType="decimal-pad"
                value={montoVehiculo}
                onChangeText={setMontoVehiculo}
              />
              <View style={styles.quickRow}>
                {['500', '1000', '1500'].map((val) => (
                  <TouchableOpacity
                    key={val}
                    style={styles.quickChip}
                    onPress={() => setMontoVehiculo(val)}
                  >
                    <Text style={styles.quickChipText}>{val}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          )}

          {/* Tarjeta 02 · MGP */}
          {esMgp && (
            <View style={[styles.card, { marginTop: 16 }]}>
              <Text style={styles.lineHeader}>02 · MGP</Text>
              <TextInput
                style={[styles.input, { marginTop: 10 }]}
                placeholder="Monto para la planta MGP (L)"
                placeholderTextColor={colors.textoSecundario}
                keyboardType="decimal-pad"
                value={montoMgp}
                onChangeText={setMontoMgp}
              />
              <View style={styles.quickRow}>
                {['500', '1000', '1500'].map((val) => (
                  <TouchableOpacity
                    key={val}
                    style={styles.quickChip}
                    onPress={() => setMontoMgp(val)}
                  >
                    <Text style={styles.quickChipText}>{val}</Text>
                  </TouchableOpacity>
                ))}
              </View>
            </View>
          )}

          {/* Total de la solicitud */}
          <View style={styles.totalInfoBox}>
            <Text style={styles.totalText}>
              Total de la solicitud · L {totalCalculado.toFixed(2)}
            </Text>
          </View>

          <TouchableOpacity
            style={[styles.btnAssign, submitting && { opacity: 0.7 }]}
            onPress={asignarOrden}
            disabled={submitting}
          >
            <Text style={styles.btnAssignText}>Asignar monto y generar orden</Text>
          </TouchableOpacity>

          {submitting && <ActivityIndicator color={colors.marca} style={{ marginTop: 16 }} />}
          {errorMsg && <Text style={styles.errorText}>{errorMsg}</Text>}
        </ScrollView>
      </View>
    </Modal>
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
  content: { padding: 20 },
  card: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  resumenConductor: { fontSize: 17, fontWeight: '700', color: colors.textoPrincipal },
  resumenDestino: { fontSize: 13, fontWeight: '700', color: colors.marca, marginTop: 6 },
  resumenActividad: { fontSize: 14, color: colors.textoSecundario, marginTop: 6 },
  label: { fontSize: 13, fontWeight: '700', color: colors.textoPrincipal, marginTop: 16, marginBottom: 6 },
  gasolineraItem: {
    backgroundColor: colors.blanco,
    borderRadius: 12,
    padding: 13,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  gasolineraItemActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  gasolineraText: { fontSize: 14, color: colors.textoPrincipal, fontWeight: '600' },
  gasolineraTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  input: {
    backgroundColor: colors.blanco,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    padding: 13,
    fontSize: 15,
    color: colors.textoPrincipal,
  },
  lineHeader: { fontSize: 15, fontWeight: '700', color: colors.textoPrincipal },
  quickRow: { flexDirection: 'row', gap: 8, marginTop: 10 },
  quickChip: {
    flex: 1,
    height: 36,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.blanco,
  },
  quickChipText: { fontSize: 12, fontWeight: '700', color: colors.marca },
  totalInfoBox: {
    backgroundColor: colors.marcaClaro,
    borderRadius: 16,
    padding: 18,
    marginTop: 16,
  },
  totalText: { fontSize: 20, fontWeight: '700', color: colors.marcaOscuro },
  btnAssign: {
    backgroundColor: colors.marca,
    height: 52,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 20,
  },
  btnAssignText: { color: colors.blanco, fontWeight: '700', fontSize: 15 },
  errorText: { color: colors.vencida, textAlign: 'center', marginTop: 12, fontSize: 13 },
});