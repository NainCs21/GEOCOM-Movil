import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  ScrollView,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
  Image,
  Modal,
  TextInput,
  Share,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
// @ts-ignore
import * as FileSystem from 'expo-file-system/legacy';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';
import { formatoLegible, parseFechaIso } from '../utils/fechas';

interface Props {
  ordenId: number;
  ordenData?: any;
  token: string;
  user: any;
  onBack: () => void;
  onOpenQr: () => void;
}

export default function OrdenDetalleScreen({
  ordenId,
  ordenData,
  token,
  user,
  onBack,
  onOpenQr,
}: Props) {
  const [orden, setOrden] = useState<any | null>(ordenData || null);
  const [loading, setLoading] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [minutosRestantes, setMinutosRestantes] = useState<string>('--:--');
  const [fotosLocales, setFotosLocales] = useState<Record<string, string>>({});

  // Visor de foto con descarga
  const [fotoModalVisible, setFotoModalVisible] = useState(false);
  const [fotoTargetSeleccionada, setFotoTargetSeleccionada] = useState<string | null>(null);
  const [descargando, setDescargando] = useState(false);

  // Justificación entrega tardía (estado === 'rehabilitada')
  const [justifModalVisible, setJustifModalVisible] = useState(false);
  const [justifTexto, setJustifTexto] = useState('');
  const [pendienteCaptura, setPendienteCaptura] = useState<{
    detalleId: number;
    tipo: string;
  } | null>(null);

  const fetchOrden = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/ordenes/${ordenId}`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const data = await res.json();
      setOrden(data);
    } catch (e: any) {
      Alert.alert('Error', 'No se pudo cargar la orden: ' + e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (ordenData && ordenData.id === ordenId && ordenData.detalles) {
      setOrden(ordenData);
    } else {
      fetchOrden();
    }
  }, [ordenId, ordenData]);

  useEffect(() => {
    if (orden?.vence_apertura && (orden.estado === 'abierta' || orden.estado === 'rehabilitada')) {
      const interval = setInterval(() => {
        const vence = parseFechaIso(orden.vence_apertura).getTime();
        const diff = vence - Date.now();
        if (diff <= 0) {
          setMinutosRestantes('Tiempo agotado');
          clearInterval(interval);
        } else {
          const m = Math.floor(diff / 60000);
          const s = Math.floor((diff % 60000) / 1000);
          setMinutosRestantes(`${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')} restantes`);
        }
      }, 1000);
      return () => clearInterval(interval);
    }
  }, [orden]);

  const iniciarCaptura = (detalleId: number, tipoFoto: string) => {
    if (orden?.estado === 'rehabilitada') {
      setPendienteCaptura({ detalleId, tipo: tipoFoto });
      setJustifTexto('');
      setJustifModalVisible(true);
    } else {
      tomarYSubirFoto(detalleId, tipoFoto, null);
    }
  };

  const tomarYSubirFoto = async (
    detalleId: number,
    tipoFoto: string,
    justificacion: string | null
  ) => {
    try {
      const permission = await ImagePicker.requestCameraPermissionsAsync();
      if (!permission.granted) {
        Alert.alert('Permiso Denegado', 'Se requiere acceso a la cámara para capturar evidencia.');
        return;
      }

      const result = await ImagePicker.launchCameraAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: false,
        quality: 0.7,
      });

      if (result.canceled || !result.assets || result.assets.length === 0) return;

      const fotoLocal = result.assets[0];
      setActionLoading(true);

      const parameters: Record<string, string> = { tipo: tipoFoto };
      if (justificacion && justificacion.trim()) {
        parameters.justificacion = justificacion.trim();
      }

      const uploadResponse = await FileSystem.uploadAsync(
        `${API_URL}/detalle-ordenes/${detalleId}/evidencia`,
        fotoLocal.uri,
        {
          httpMethod: 'POST',
          uploadType: ((FileSystem as any).FileSystemUploadType?.MULTIPART ?? 1) as any,
          fieldName: 'foto',
          parameters,
          headers: {
            Accept: 'application/json',
            Authorization: `Bearer ${token}`,
          },
        }
      );

      const json = JSON.parse(uploadResponse.body);

      // Igual que ConductorViewModel.kt: si el backend dice que ese tipo ya fue subido, refresca orden
      if (uploadResponse.status === 422) {
        const msg = json.mensaje || json.message || '';
        if (msg.toLowerCase().includes('ya fue subido')) {
          await fetchOrden();
          return;
        }
      }

      if (uploadResponse.status < 200 || uploadResponse.status >= 300) {
        throw new Error(
          json.mensaje || json.message || `Error del servidor (${uploadResponse.status})`
        );
      }

      setFotosLocales((prev) => ({
        ...prev,
        [`${detalleId}_${tipoFoto}`]: fotoLocal.uri,
      }));

      if (json.id) {
        setOrden(json);
      } else {
        await fetchOrden();
      }
    } catch (err: any) {
      Alert.alert('Error al subir evidencia', err.message);
    } finally {
      setActionLoading(false);
    }
  };

  const descargarFoto = async (urlEvidencia: string) => {
    setDescargando(true);
    try {
      const destino = `${FileSystem.cacheDirectory}evidencia_${Date.now()}.jpg`;
      const res = await FileSystem.downloadAsync(urlEvidencia, destino, {
        headers: { Authorization: `Bearer ${token}` },
      });
      await Share.share({ url: res.uri });
    } catch (e: any) {
      Alert.alert('Error', 'No se pudo exportar la fotografía: ' + e.message);
    } finally {
      setDescargando(false);
    }
  };

  const rehabilitarOrden = async () => {
    setActionLoading(true);
    try {
      const res = await fetch(`${API_URL}/ordenes/${orden.id}/rehabilitar`, {
        method: 'POST',
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      if (!res.ok) throw new Error('No se pudo rehabilitar la orden.');
      Alert.alert('Éxito', 'Orden rehabilitada con éxito');
      fetchOrden();
    } catch (e: any) {
      Alert.alert('Error', e.message);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading || !orden) {
    return (
      <View style={styles.center}>
        <ActivityIndicator size="large" color={colors.marca} />
      </View>
    );
  }

  const isConductor = user?.rol === 'conductor';
  const isAdmin = user?.rol === 'admin';
  const isPendiente = orden.estado === 'pendiente';
  const isVencida = orden.estado === 'vencida';
  const isCompletada =
    orden.estado === 'completada' ||
    orden.estado === 'tardio' ||
    orden.estado === 'completado_tardio';

  const lineas = orden.detalles || [];
  const montoTotal = lineas.reduce((acc: number, l: any) => acc + (parseFloat(l.monto) || 0), 0);

  // Verifica si las 4 evidencias de todas las líneas están completas (igual que OrdenDetalleFragment.kt)
  const todasLineasCompletas =
    lineas.length > 0 &&
    lineas.every((l: any) => {
      const mgp = l.tipo_destino === 'mgp' || !l.vehiculo_id;
      const tiposSubidos = new Set((l.evidencias || []).map((e: any) => e.tipo));
      return (
        tiposSubidos.has('bomba') &&
        (tiposSubidos.has('tablero') || mgp) &&
        tiposSubidos.has('tanque') &&
        tiposSubidos.has('factura')
      );
    });

  const isOpen =
    (orden.estado === 'abierta' || orden.estado === 'rehabilitada') && !todasLineasCompletas;

  const badgeStyle = () => {
    switch (orden.estado) {
      case 'pendiente':
        return { bg: colors.pendienteFondo, text: colors.pendiente, label: 'PENDIENTE' };
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
        return { bg: colors.pendienteFondo, text: colors.pendiente, label: (orden.estado || '').toUpperCase() };
    }
  };

  const badge = badgeStyle();

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <TouchableOpacity onPress={onBack} style={styles.backBtn}>
          <Text style={{ fontSize: 22, color: colors.textoPrincipal }}>←</Text>
        </TouchableOpacity>
        <Text style={styles.topHeaderTitle}>Detalle de la orden</Text>
      </View>

      <ScrollView contentContainerStyle={styles.container}>
        <Text style={styles.orderNumber}>Orden #{orden.numero_orden}</Text>
        <View style={[styles.badge, { backgroundColor: badge.bg }]}>
          <Text style={[styles.badgeText, { color: badge.text }]}>{badge.label}</Text>
        </View>

        {/* ORDEN PROTEGIDA (Conductor + Pendiente) */}
        {isPendiente && isConductor && (
          <View style={styles.protectedCard}>
            <Text style={{ fontSize: 48, marginBottom: 10 }}>🔒</Text>
            <Text style={styles.protectedTitle}>Orden protegida</Text>
            <Text style={styles.protectedSubtitle}>
              El monto y la gasolinera se mostrarán únicamente después de validar el QR.
            </Text>
            <Text style={styles.protectedPlazo}>
              Retira antes de las {formatoLegible(orden.vence_asignacion)}
            </Text>
            <TouchableOpacity style={styles.btnPrimaryPill} onPress={onOpenQr}>
              <Text style={styles.btnPrimaryPillText}>📷 Escanear QR de gasolinera</Text>
            </TouchableOpacity>
          </View>
        )}

        {/* DETALLE REVELADO */}
        {(!isPendiente || !isConductor) && (
          <>
            {isOpen && isConductor && (
              <View style={styles.warningCard}>
                <Text style={{ fontSize: 26, marginRight: 14 }}>⏰</Text>
                <View style={{ flex: 1 }}>
                  <Text style={styles.warningTimer}>{minutosRestantes}</Text>
                  <Text style={styles.warningSub}>Tiempo restante · máximo 15 min</Text>
                  <Text style={styles.warningSub}>Al llegar a 00:00, la orden vence.</Text>
                </View>
              </View>
            )}

            <View style={styles.infoRow}>
              <Text style={{ fontSize: 24, marginRight: 10 }}>📍</Text>
              <View style={{ flex: 1 }}>
                <Text style={styles.stationTitle}>{orden.gasolinera?.nombre || '—'}</Text>
                <Text style={styles.stationDir}>{orden.gasolinera?.direccion || ''}</Text>
              </View>
            </View>

            <View style={styles.totalBox}>
              <Text style={styles.totalLabel}>MONTO TOTAL AUTORIZADO</Text>
              <Text style={styles.totalNumber}>L {montoTotal.toFixed(2)}</Text>
              <Text style={styles.totalSub}>
                Distribuido en {lineas.length} línea(s) de combustible
              </Text>
            </View>

            <View style={styles.linesCard}>
              {lineas.map((linea: any, index: number) => {
                const esMgp = linea.tipo_destino === 'mgp' || !linea.vehiculo_id;
                const etiquetaDestino = !esMgp
                  ? linea.vehiculo?.placa || 'Vehículo'
                  : linea.descripcion_mgp || 'Planta generadora';

                // Mapeo exacto de linea.evidencias[]
                const evidenciasMap: Record<string, any> = {};
                (linea.evidencias || []).forEach((ev: any) => {
                  evidenciasMap[ev.tipo] = ev;
                });

                const getTarget = (tipo: string): string | null =>
                  evidenciasMap[tipo]?.url || fotosLocales[`${linea.id}_${tipo}`] || null;

                const filasEvidencia = [
                  { key: 'bomba', label: '1. Bomba (cantidad llenada)', esTableroMgp: false },
                  {
                    key: 'tablero',
                    label: esMgp ? '2. Tablero del carro (Opcional - MGP)' : '2. Tablero del carro',
                    esTableroMgp: esMgp,
                  },
                  { key: 'tanque', label: '3. Tanque (evidencia de llenado)', esTableroMgp: false },
                  { key: 'factura', label: '4. Factura emitida', esTableroMgp: false },
                ];

                return (
                  <View
                    key={linea.id}
                    style={[
                      index > 0 && {
                        borderTopWidth: 1,
                        borderTopColor: colors.borde,
                        paddingTop: 14,
                        marginTop: 14,
                      },
                    ]}
                  >
                    <Text style={styles.lineaTitle}>
                      {etiquetaDestino} ({linea.tipo_combustible})
                    </Text>
                    <Text style={styles.lineaMonto}>
                      Monto autorizado: L {Number(linea.monto).toFixed(2)}
                    </Text>

                    {filasEvidencia.map((ev) => {
                      const targetUrl = getTarget(ev.key);
                      const subido = !!targetUrl;

                      return (
                        <View key={ev.key} style={styles.evidenciaRow}>
                          <Text style={styles.evidenciaLabel}>{ev.label}</Text>

                          {ev.esTableroMgp ? (
                            <Text style={styles.noAplicaText}>
                              {isConductor ? 'No aplica (MGP)' : 'No aplica'}
                            </Text>
                          ) : isConductor && isOpen && !subido ? (
                            <TouchableOpacity
                              style={styles.btnSubirFoto}
                              onPress={() => iniciarCaptura(linea.id, ev.key)}
                              disabled={actionLoading}
                            >
                              <Text style={styles.btnSubirFotoText}>Subir foto</Text>
                            </TouchableOpacity>
                          ) : (
                            <TouchableOpacity
                              disabled={!subido}
                              onPress={() => {
                                if (targetUrl) {
                                  setFotoTargetSeleccionada(targetUrl);
                                  setFotoModalVisible(true);
                                }
                              }}
                            >
                              <Text
                                style={[
                                  styles.fotoSubidaText,
                                  !subido && { color: colors.textoSecundario },
                                ]}
                              >
                                {subido ? 'Ver evidencia' : 'Sin evidencia'}
                              </Text>
                            </TouchableOpacity>
                          )}
                        </View>
                      );
                    })}
                  </View>
                );
              })}
            </View>
          </>
        )}

        {(isCompletada || todasLineasCompletas) && (
          <Text style={styles.mensajeFinal}>Orden completada. ¡Buen viaje!</Text>
        )}
        {isVencida && (
          <Text style={styles.mensajeFinal}>Esta orden venció. Genera una nueva solicitud.</Text>
        )}

        {isVencida && isAdmin && (
          <TouchableOpacity
            style={styles.btnRehabilitar}
            onPress={rehabilitarOrden}
            disabled={actionLoading}
          >
            {actionLoading ? (
              <ActivityIndicator color={colors.blanco} />
            ) : (
              <Text style={styles.btnPrimaryPillText}>Rehabilitar orden</Text>
            )}
          </TouchableOpacity>
        )}
      </ScrollView>

      {/* Modal Justificación de Entrega Tardía */}
      <Modal visible={justifModalVisible} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Justificación de entrega tardía</Text>
            <TextInput
              style={styles.justifInput}
              placeholder="Ingresa el motivo por el cual se entrega tarde este pedido:"
              value={justifTexto}
              onChangeText={setJustifTexto}
              multiline
            />
            <View style={{ flexDirection: 'row', gap: 10, marginTop: 14 }}>
              <TouchableOpacity
                style={[styles.modalCloseBtn, { backgroundColor: colors.textoSecundario }]}
                onPress={() => setJustifModalVisible(false)}
              >
                <Text style={styles.modalCloseText}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.modalCloseBtn}
                onPress={() => {
                  if (!justifTexto.trim()) {
                    Alert.alert('Aviso', 'Debes ingresar una justificación.');
                    return;
                  }
                  setJustifModalVisible(false);
                  if (pendienteCaptura) {
                    tomarYSubirFoto(pendienteCaptura.detalleId, pendienteCaptura.tipo, justifTexto);
                  }
                }}
              >
                <Text style={styles.modalCloseText}>Continuar</Text>
              </TouchableOpacity>
            </View>
          </View>
        </View>
      </Modal>

      {/* Modal Visor y Descarga de Evidencia */}
      <Modal visible={fotoModalVisible} transparent animationType="fade">
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <Text style={styles.modalTitle}>Evidencia fotográfica</Text>
            {fotoTargetSeleccionada && (
              <Image
                source={{
                  uri: fotoTargetSeleccionada,
                  headers: fotoTargetSeleccionada.startsWith('http')
                    ? { Authorization: `Bearer ${token}` }
                    : undefined,
                }}
                style={styles.imagenEvidencia}
                resizeMode="contain"
              />
            )}

            {fotoTargetSeleccionada?.startsWith('http') && (
              <TouchableOpacity
                style={styles.btnDescargar}
                onPress={() => descargarFoto(fotoTargetSeleccionada)}
                disabled={descargando}
              >
                {descargando ? (
                  <ActivityIndicator color={colors.marcaOscuro} />
                ) : (
                  <Text style={styles.btnDescargarText}>📥 Descargar / Exportar foto</Text>
                )}
              </TouchableOpacity>
            )}

            <TouchableOpacity
              style={styles.modalCloseBtn}
              onPress={() => setFotoModalVisible(false)}
            >
              <Text style={styles.modalCloseText}>Aceptar</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  center: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  container: { padding: 20 },
  topHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.blanco,
    padding: 20,
  },
  backBtn: { marginRight: 10, padding: 4 },
  topHeaderTitle: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal },
  orderNumber: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal },
  badge: {
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
    alignSelf: 'flex-start',
    marginTop: 8,
  },
  badgeText: { fontSize: 12, fontWeight: '700' },
  protectedCard: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 24,
    alignItems: 'center',
    marginTop: 16,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  protectedTitle: { fontSize: 18, fontWeight: '700', color: colors.textoPrincipal },
  protectedSubtitle: {
    fontSize: 13,
    color: colors.textoSecundario,
    textAlign: 'center',
    marginTop: 6,
  },
  protectedPlazo: { fontSize: 14, fontWeight: '700', color: colors.marca, marginTop: 16 },
  warningCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.advertenciaFondo,
    borderRadius: 20,
    padding: 18,
    marginTop: 16,
  },
  warningTimer: { fontSize: 28, fontWeight: '700', color: colors.advertenciaTexto },
  warningSub: { fontSize: 12, color: colors.advertenciaTextoSecundario },
  infoRow: { flexDirection: 'row', alignItems: 'center', marginTop: 16 },
  stationTitle: { fontSize: 18, fontWeight: '700', color: colors.textoPrincipal },
  stationDir: { fontSize: 13, color: colors.textoSecundario },
  totalBox: { backgroundColor: colors.marcaClaro, padding: 20, borderRadius: 16, marginTop: 16 },
  totalLabel: { fontSize: 11, fontWeight: '700', color: colors.marcaOscuro },
  totalNumber: { fontSize: 32, fontWeight: '700', color: colors.textoPrincipal, marginTop: 2 },
  totalSub: { fontSize: 13, color: colors.textoSecundario },
  linesCard: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
    borderColor: colors.borde,
    marginTop: 16,
  },
  lineaTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  lineaMonto: { fontSize: 15, color: colors.textoSecundario, marginTop: 4, marginBottom: 12 },
  evidenciaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
  },
  evidenciaLabel: { fontSize: 13, color: colors.textoPrincipal, flex: 1, paddingRight: 8 },
  btnSubirFoto: {
    backgroundColor: colors.marca,
    paddingHorizontal: 14,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnSubirFotoText: { color: colors.blanco, fontSize: 12, fontWeight: '700' },
  fotoSubidaText: { color: colors.completada, fontSize: 12, fontWeight: '700' },
  noAplicaText: { color: colors.textoSecundario, fontSize: 12, fontWeight: '600' },
  mensajeFinal: {
    fontSize: 16,
    color: colors.textoSecundario,
    textAlign: 'center',
    marginTop: 16,
  },
  btnPrimaryPill: {
    backgroundColor: colors.marca,
    height: 52,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    width: '100%',
    marginTop: 16,
  },
  btnPrimaryPillText: { color: colors.blanco, fontWeight: '700', fontSize: 15 },
  btnRehabilitar: {
    backgroundColor: colors.marca,
    height: 52,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 16,
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.6)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  modalContent: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 20,
    width: '100%',
    alignItems: 'center',
  },
  modalTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal, marginBottom: 12 },
  justifInput: {
    width: '100%',
    backgroundColor: colors.fondo,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 10,
    padding: 12,
    minHeight: 70,
    textAlignVertical: 'top',
  },
  imagenEvidencia: { width: '100%', height: 280, borderRadius: 10, backgroundColor: '#eee' },
  btnDescargar: {
    marginTop: 14,
    backgroundColor: colors.marcaClaro,
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 20,
  },
  btnDescargarText: { color: colors.marcaOscuro, fontWeight: '700', fontSize: 13 },
  modalCloseBtn: {
    marginTop: 12,
    backgroundColor: colors.marca,
    paddingHorizontal: 24,
    paddingVertical: 10,
    borderRadius: 20,
  },
  modalCloseText: { color: colors.blanco, fontWeight: '700' },
});