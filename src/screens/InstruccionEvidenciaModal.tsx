import React, { useState } from 'react';
import {
  Modal,
  View,
  Text,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Image,
  ScrollView,
  Alert,
} from 'react-native';
import * as ImagePicker from 'expo-image-picker';
import { colors } from '../theme/colors';

interface Props {
  visible: boolean;
  detalleId: number;
  tipo: string;
  onClose: () => void;
  onContinue: (foto: ImagePicker.ImagePickerAsset) => void;
}

export default function InstruccionEvidenciaModal({
  visible,
  tipo,
  onClose,
  onContinue,
}: Props) {
  const [loadingCamera, setLoadingCamera] = useState(false);

  const getInstrucciones = () => {
    switch (tipo) {
      case 'bomba':
        return {
          titulo: 'Fotografía de la Bomba',
          desc: 'Toma una fotografía clara de la bomba dispensadora de combustible en la gasolinera. Asegúrate de que el marcador que indica los litros/galones despachados y el precio por unidad sean legibles.',
          img: require('../../assets/ejemplo_bomba.jpeg'),
        };
      case 'tablero':
        return {
          titulo: 'Fotografía del Tablero',
          desc: 'Toma una fotografía del tablero (instrument cluster) del vehículo mostrando el kilometraje actual y el nivel de combustible.',
          img: require('../../assets/ejemplo_tablero.jpeg'),
        };
      case 'tanque':
        return {
          titulo: 'Fotografía del Tanque',
          desc: 'Toma una fotografía del tanque o tapón de llenado del vehículo mientras se suministra el combustible.',
          img: require('../../assets/ejemplo_tanque_gas.jpeg'),
        };
      case 'factura':
        return {
          titulo: 'Fotografía de la Factura / Recibo',
          desc: 'Toma una fotografía nítida del recibo, voucher o factura física emitida por la gasolinera. Verifica que el número de factura, fecha, gasolinera y monto total sean perfectamente legibles.',
          img: require('../../assets/ejemplo_factura.jpeg'),
        };
      default:
        return {
          titulo: 'Evidencia fotográfica',
          desc: 'Procura tomar la foto con buena iluminación y sin reflejos.',
          img: null,
        };
    }
  };

  const { titulo, desc, img } = getInstrucciones();

  const abrirCamara = async () => {
    setLoadingCamera(true);
    try {
      const permission = await ImagePicker.requestCameraPermissionsAsync();
      if (!permission.granted) {
        Alert.alert('Permiso requerido', 'Se requiere permiso de cámara para tomar la evidencia.');
        return;
      }
      const result = await ImagePicker.launchCameraAsync({
        mediaTypes: ImagePicker.MediaTypeOptions.Images,
        allowsEditing: false,
        quality: 0.8,
      });

      if (!result.canceled && result.assets && result.assets.length > 0) {
        onContinue(result.assets[0]);
      }
    } catch (e: any) {
      Alert.alert('Error', 'Error al abrir la cámara: ' + e.message);
    } finally {
      setLoadingCamera(false);
    }
  };

  return (
    <Modal visible={visible} animationType="slide">
      <View style={styles.flex}>
        <View style={styles.topHeader}>
          <TouchableOpacity onPress={onClose} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.blanco }}>←</Text>
          </TouchableOpacity>
          <Text style={styles.topHeaderTitle}>Instrucciones de evidencia</Text>
        </View>

        <ScrollView contentContainerStyle={styles.content}>
          <View style={styles.card}>
            <View style={{ flexDirection: 'row', alignItems: 'center' }}>
              <View style={styles.iconBox}>
                <Text style={{ fontSize: 20 }}>📷</Text>
              </View>
              <Text style={styles.title}>{titulo}</Text>
            </View>
            <Text style={styles.desc}>{desc}</Text>
          </View>

          <View style={styles.recommendCard}>
            <Text style={styles.recommendText}>
              Recomendación: Procura tomar la foto con buena iluminación, sin reflejos y
              asegurándote de que todos los datos sean totalmente legibles.
            </Text>
          </View>

          <View style={styles.exampleCard}>
            <Text style={styles.exampleTitle}>Fotografía de ejemplo</Text>
            {img && <Image source={img} style={styles.exampleImg} resizeMode="contain" />}
          </View>

          <TouchableOpacity
            style={[styles.btnCamera, loadingCamera && { opacity: 0.7 }]}
            onPress={abrirCamara}
            disabled={loadingCamera}
          >
            <Text style={styles.btnCameraText}>📷 Abrir cámara y tomar foto</Text>
          </TouchableOpacity>

          {loadingCamera && <ActivityIndicator color={colors.marca} style={{ marginTop: 16 }} />}
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
    backgroundColor: colors.marca,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  backBtn: { marginRight: 8, padding: 4 },
  topHeaderTitle: { fontSize: 18, fontWeight: '700', color: colors.blanco },
  content: { padding: 20, paddingBottom: 40 },
  card: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 20,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  iconBox: {
    backgroundColor: colors.marcaClaro,
    borderRadius: 10,
    padding: 8,
    marginRight: 14,
  },
  title: { flex: 1, fontSize: 18, fontWeight: '700', color: colors.textoPrincipal },
  desc: { fontSize: 15, color: colors.textoPrincipal, marginTop: 16, lineHeight: 22 },
  recommendCard: {
    backgroundColor: colors.marcaClaro,
    borderRadius: 16,
    padding: 16,
    marginTop: 16,
  },
  recommendText: { fontSize: 13, color: colors.marcaOscuro, lineHeight: 18 },
  exampleCard: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 14,
    borderWidth: 1,
    borderColor: colors.borde,
    marginTop: 16,
    alignItems: 'center',
  },
  exampleTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: colors.textoPrincipal,
    marginBottom: 8,
  },
  exampleImg: {
    width: '100%',
    height: 300,
    borderRadius: 8,
    backgroundColor: colors.fondo,
  },
  btnCamera: {
    backgroundColor: colors.marcaClaro,
    height: 52,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 24,
  },
  btnCameraText: { color: colors.marcaOscuro, fontWeight: '700', fontSize: 15 },
});