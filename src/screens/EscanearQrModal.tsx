import React, { useState, useEffect, useRef } from 'react';
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  ScrollView,
} from 'react-native';
import { CameraView, useCameraPermissions } from 'expo-camera';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  visible: boolean;
  onClose: () => void;
  token: string;
  onScanned: (orden: any) => void;
}

export default function EscanearQrModal({ visible, onClose, token, onScanned }: Props) {
  const [permission, requestPermission] = useCameraPermissions();
  const [loading, setLoading] = useState(false);
  const [mensajeError, setMensajeError] = useState<string | null>(null);
  const isScanningRef = useRef(false);

  useEffect(() => {
    if (visible) {
      isScanningRef.current = false;
      setLoading(false);
      setMensajeError(null);
      if (permission && !permission.granted) {
        requestPermission();
      }
    }
  }, [visible]);

  const handleCerrar = () => {
    isScanningRef.current = false;
    setLoading(false);
    setMensajeError(null);
    onClose();
  };

  const handleBarCodeScanned = async ({ data }: { data: string }) => {
    if (isScanningRef.current || loading) return;
    isScanningRef.current = true;
    setLoading(true);
    setMensajeError(null);

    const qr = (data || '').trim();

    try {
      const res = await fetch(`${API_URL}/ordenes/escanear`, {
        method: 'POST',
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ codigo_qr: qr }),
      });

      const json = await res.json();

      if (res.ok) {
        onScanned(json);
        handleCerrar();
        return;
      }

      // Si la orden ya fue abierta por otro fotograma, abrirla sin error
      const ordRes = await fetch(`${API_URL}/ordenes`, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const ordData = await ordRes.json();
      const lista = Array.isArray(ordData) ? ordData : ordData.data || [];
      const ordenAbierta = lista.find(
        (o: any) => o.estado === 'abierta' || o.estado === 'rehabilitada'
      );

      if (ordenAbierta) {
        onScanned(ordenAbierta);
        handleCerrar();
        return;
      }

      throw new Error(
        json.mensaje || json.message || 'No tienes una orden pendiente para esta gasolinera.'
      );
    } catch (e: any) {
      setMensajeError(e.message);
      setLoading(false);
      // Reanudar cámara tras 1.5s para permitir reintentar sin bloquear la vista
      setTimeout(() => {
        isScanningRef.current = false;
      }, 1500);
    }
  };

  if (!visible) return null;

  return (
    <Modal visible={visible} animationType="slide">
      <View style={styles.flex}>
        {/* Barra superior blanca (fragment_escanear_qr.xml) */}
        <View style={styles.topHeader}>
          <TouchableOpacity onPress={handleCerrar} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.textoPrincipal }}>←</Text>
          </TouchableOpacity>
          <Text style={styles.topHeaderTitle}>Escanear QR</Text>
        </View>

        <ScrollView contentContainerStyle={styles.body}>
          <Text style={styles.title}>
            Escanea el código QR de la gasolinera para ver tu orden
          </Text>
          <Text style={styles.subtitle}>
            Apunta la cámara al código QR de la gasolinera
          </Text>

          {!permission?.granted ? (
            <View style={styles.permissionCard}>
              <Text style={styles.permissionText}>
                Se requiere permiso de cámara para escanear el código QR.
              </Text>
              <TouchableOpacity style={styles.btnPermiso} onPress={requestPermission}>
                <Text style={styles.btnPermisoText}>Abrir cámara</Text>
              </TouchableOpacity>
            </View>
          ) : (
            <View style={styles.scannerBox}>
              <CameraView
                style={StyleSheet.absoluteFill}
                facing="back"
                barcodeScannerSettings={{ barcodeTypes: ['qr'] }}
                onBarcodeScanned={isScanningRef.current ? undefined : handleBarCodeScanned}
              />
            </View>
          )}

          {loading && <ActivityIndicator color={colors.marca} style={{ marginTop: 16 }} />}

          {mensajeError && (
            <View style={styles.infoCard}>
              <Text style={{ fontSize: 20, marginRight: 12 }}>ℹ️</Text>
              <Text style={styles.infoCardText}>{mensajeError}</Text>
            </View>
          )}
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
  body: { padding: 24, alignItems: 'center' },
  title: {
    fontSize: 20,
    fontWeight: '700',
    color: colors.textoPrincipal,
    textAlign: 'center',
    marginTop: 8,
  },
  subtitle: {
    fontSize: 14,
    color: colors.textoSecundario,
    textAlign: 'center',
    marginTop: 6,
    marginBottom: 20,
  },
  scannerBox: {
    width: 260,
    height: 260,
    borderRadius: 16,
    borderWidth: 3,
    borderColor: colors.marca,
    overflow: 'hidden',
    backgroundColor: '#000',
  },
  permissionCard: {
    width: 260,
    height: 260,
    borderRadius: 16,
    borderWidth: 2,
    borderColor: colors.marca,
    backgroundColor: colors.blanco,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 20,
  },
  permissionText: {
    fontSize: 13,
    color: colors.textoSecundario,
    textAlign: 'center',
    marginBottom: 16,
  },
  btnPermiso: {
    backgroundColor: colors.marca,
    paddingHorizontal: 20,
    paddingVertical: 12,
    borderRadius: 24,
  },
  btnPermisoText: { color: colors.blanco, fontWeight: '700', fontSize: 14 },
  infoCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.marcaClaro,
    borderRadius: 16,
    padding: 16,
    marginTop: 24,
    width: '100%',
  },
  infoCardText: { flex: 1, color: colors.marcaOscuro, fontSize: 14, fontWeight: '700' },
});