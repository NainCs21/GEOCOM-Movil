import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  ActivityIndicator,
  Alert,
  Modal,
  TextInput,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import * as Print from 'expo-print';
import * as Sharing from 'expo-sharing';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';
import {
  formatearFechaIsoShort,
  obtenerFechaActualString,
  obtenerInicioMesString,
  obtenerMesAnteriorRango,
  obtenerUltimos7DiasRango,
} from '../utils/fechas';

interface Props {
  token: string;
  onBack?: () => void;
  showBack?: boolean;
}

export default function ReportesScreen({ token, onBack, showBack = true }: Props) {
  const [tab, setTab] = useState<'conductor' | 'gasolinera'>('conductor');
  const [datos, setDatos] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const [etiquetaPeriodo, setEtiquetaPeriodo] = useState('Este mes');
  const [desde, setDesde] = useState<string | null>(obtenerInicioMesString());
  const [hasta, setHasta] = useState<string | null>(obtenerFechaActualString());

  const [customModal, setCustomModal] = useState(false);
  const [customDesde, setCustomDesde] = useState('');
  const [customHasta, setCustomHasta] = useState('');

  useEffect(() => {
    setLoading(true);
    const params = new URLSearchParams();
    if (desde) params.append('desde', desde);
    if (hasta) params.append('hasta', hasta);
    const qs = params.toString() ? `?${params.toString()}` : '';

    fetch(`${API_URL}/reportes/por-${tab}${qs}`, {
      headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setDatos(Array.isArray(d) ? d : d.data || []))
      .catch(() => setDatos([]))
      .finally(() => setLoading(false));
  }, [tab, desde, hasta, token]);

  const abrirSelectorPeriodo = () => {
    Alert.alert('Seleccionar periodo', undefined, [
      {
        text: 'Todo el periodo',
        onPress: () => {
          setEtiquetaPeriodo('Todo el periodo');
          setDesde(null);
          setHasta(null);
        },
      },
      {
        text: 'Este mes',
        onPress: () => {
          setEtiquetaPeriodo('Este mes');
          setDesde(obtenerInicioMesString());
          setHasta(obtenerFechaActualString());
        },
      },
      {
        text: 'Mes anterior',
        onPress: () => {
          const [d, h] = obtenerMesAnteriorRango();
          setEtiquetaPeriodo(`${formatearFechaIsoShort(d)} - ${formatearFechaIsoShort(h)}`);
          setDesde(d);
          setHasta(h);
        },
      },
      {
        text: 'Últimos 7 días',
        onPress: () => {
          const [d, h] = obtenerUltimos7DiasRango();
          setEtiquetaPeriodo(`${formatearFechaIsoShort(d)} - ${formatearFechaIsoShort(h)}`);
          setDesde(d);
          setHasta(h);
        },
      },
      {
        text: 'Personalizado…',
        onPress: () => {
          setCustomDesde(desde || obtenerInicioMesString());
          setCustomHasta(hasta || obtenerFechaActualString());
          setCustomModal(true);
        },
      },
      { text: 'Cancelar', style: 'cancel' },
    ]);
  };

  const formatearMonto = (num: number) =>
    Number(num || 0).toLocaleString('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });

  const totalOrdenes = datos.reduce((acc, item) => acc + (Number(item.cantidad_ordenes) || 0), 0);
  const totalMonto = datos.reduce((acc, item) => acc + (Number(item.monto_total) || 0), 0);

  const exportarPdf = async () => {
    if (datos.length === 0) return;
    const esConductor = tab === 'conductor';
    const tituloReporte = esConductor
      ? 'Reporte de consumo por conductor'
      : 'Reporte de consumo por gasolinera';
    const columnaEntidad = esConductor ? 'Conductor' : 'Gasolinera';
    const periodoStr =
      !desde || !hasta
        ? 'Órdenes completadas, todo el periodo'
        : `Órdenes completadas del ${formatearFechaIsoShort(desde)} al ${formatearFechaIsoShort(hasta)}`;

    const fechaGen = new Date().toLocaleString('es-HN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });

    const filasHtml = datos
      .map((row, idx) => {
        const nombre = esConductor ? row.nombre_conductor : row.nombre_gasolinera;
        return `
          <tr>
            <td>${idx + 1}</td>
            <td>${nombre}</td>
            <td>${row.cantidad_ordenes}</td>
            <td style="text-align:right;">${formatearMonto(row.monto_total)}</td>
          </tr>
        `;
      })
      .join('');

    const html = `
      <html>
        <head>
          <meta charset="utf-8" />
          <style>
            body { font-family: Helvetica, Arial, sans-serif; padding: 32px; color: #24212D; }
            .brand { font-size: 11px; color: #24212D; margin-bottom: 6px; }
            h1 { font-size: 18px; margin: 0 0 6px 0; }
            .sub { font-size: 12px; color: #666270; margin: 2px 0; }
            table { width: 100%; border-collapse: collapse; margin-top: 18px; font-size: 12px; }
            th { background-color: #6042A6; color: #FFFFFF; text-align: left; padding: 8px; }
            td { border-bottom: 1px solid #E5E0EB; padding: 8px; }
            .total-row td { font-weight: bold; border-top: 2px solid #6042A6; border-bottom: none; }
          </style>
        </head>
        <body>
          <div class="brand">GEOCOM · Sistema de Órdenes de Combustible</div>
          <h1>${tituloReporte}</h1>
          <div class="sub">${periodoStr}</div>
          <div class="sub">Generado el ${fechaGen} por Administrador</div>

          <table>
            <thead>
              <tr>
                <th style="width:40px;">#</th>
                <th>${columnaEntidad}</th>
                <th style="width:80px;">Órdenes</th>
                <th style="width:100px; text-align:right;">Monto (L)</th>
              </tr>
            </thead>
            <tbody>
              ${filasHtml}
              <tr class="total-row">
                <td colspan="2">Total general</td>
                <td>${totalOrdenes}</td>
                <td style="text-align:right;">${formatearMonto(totalMonto)}</td>
              </tr>
            </tbody>
          </table>
        </body>
      </html>
    `;

    try {
      const { uri } = await Print.printToFileAsync({ html });
      await Sharing.shareAsync(uri, { mimeType: 'application/pdf', dialogTitle: 'Compartir reporte' });
    } catch {
      Alert.alert('Error', 'No se pudo generar el PDF');
    }
  };

  const mostrarDetalle = (item: any) => {
    const montoFmt = formatearMonto(item.monto_total);
    if (tab === 'conductor') {
      Alert.alert(
        item.nombre_conductor,
        `ID Conductor: ${item.conductor_id}\nÓrdenes totales: ${item.cantidad_ordenes}\nMonto total: L ${montoFmt}`
      );
    } else {
      Alert.alert(
        item.nombre_gasolinera,
        `ID Gasolinera: ${item.gasolinera_id}\nÓrdenes totales: ${item.cantidad_ordenes}\nMonto total: L ${montoFmt}`
      );
    }
  };

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          {showBack && onBack && (
            <TouchableOpacity onPress={onBack} style={styles.backBtn}>
              <Text style={{ fontSize: 22, color: colors.blanco }}>←</Text>
            </TouchableOpacity>
          )}
          <Text style={styles.topHeaderTitle}>Reportes</Text>
        </View>
        <TouchableOpacity
          onPress={exportarPdf}
          disabled={loading || datos.length === 0}
          style={[styles.btnPdf, (loading || datos.length === 0) && { opacity: 0.4 }]}
        >
          <Text style={styles.btnPdfText}>📥 PDF</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.toggleRow}>
        <TouchableOpacity
          style={[styles.toggleBtn, tab === 'conductor' && styles.toggleBtnActive]}
          onPress={() => setTab('conductor')}
        >
          <Text style={[styles.toggleText, tab === 'conductor' && styles.toggleTextActive]}>
            Por conductor
          </Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[styles.toggleBtn, tab === 'gasolinera' && styles.toggleBtnActive]}
          onPress={() => setTab('gasolinera')}
        >
          <Text style={[styles.toggleText, tab === 'gasolinera' && styles.toggleTextActive]}>
            Por gasolinera
          </Text>
        </TouchableOpacity>
      </View>

      <View style={styles.periodoBox}>
        <TouchableOpacity style={styles.btnPeriodo} onPress={abrirSelectorPeriodo}>
          <Text style={styles.btnPeriodoText}>📅  {etiquetaPeriodo}</Text>
        </TouchableOpacity>
      </View>

      <View style={styles.body}>
        <View style={styles.cardResumen}>
          <Text style={styles.resumenTitulo}>
            {tab === 'conductor'
              ? 'Reporte de consumo por conductor'
              : 'Reporte de consumo por gasolinera'}
          </Text>
          <Text style={styles.resumenSub}>
            {!desde || !hasta
              ? 'Órdenes completadas, todo el periodo'
              : `Órdenes completadas del ${formatearFechaIsoShort(desde)} al ${formatearFechaIsoShort(hasta)}`}
          </Text>
          <Text style={styles.resumenTotales}>
            {totalOrdenes} órdenes · Total: L {formatearMonto(totalMonto)}
          </Text>
        </View>

        <FlatList
          data={datos}
          keyExtractor={(_, index) => index.toString()}
          contentContainerStyle={{ paddingBottom: 88 }}
          renderItem={({ item }) => (
            <TouchableOpacity style={styles.cardItem} onPress={() => mostrarDetalle(item)}>
              <Text style={styles.itemTitle}>
                {item.nombre_conductor || item.nombre_gasolinera}
              </Text>
              <Text style={styles.itemSubtitle}>
                {item.cantidad_ordenes} órdenes · L {formatearMonto(item.monto_total)}
              </Text>
            </TouchableOpacity>
          )}
          ListEmptyComponent={
            loading ? (
              <ActivityIndicator color={colors.marca} style={{ marginTop: 24 }} />
            ) : (
              <Text style={styles.emptyText}>No hay órdenes completadas en este periodo</Text>
            )
          }
        />
      </View>

      <Modal visible={customModal} transparent animationType="fade">
        <KeyboardAvoidingView
          style={styles.modalOverlay}
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        >
          <View style={styles.modalCard}>
            <Text style={styles.resumenTitulo}>Seleccionar rango de fechas</Text>
            <Text style={styles.label}>Desde (AAAA-MM-DD)</Text>
            <TextInput
              style={styles.input}
              value={customDesde}
              onChangeText={setCustomDesde}
              placeholder="2026-11-01"
            />
            <Text style={styles.label}>Hasta (AAAA-MM-DD)</Text>
            <TextInput
              style={styles.input}
              value={customHasta}
              onChangeText={setCustomHasta}
              placeholder="2026-11-30"
            />
            <View style={{ flexDirection: 'row', gap: 10, marginTop: 18 }}>
              <TouchableOpacity
                style={styles.btnModalCancel}
                onPress={() => setCustomModal(false)}
              >
                <Text style={{ color: colors.textoSecundario, fontWeight: '700' }}>Cancelar</Text>
              </TouchableOpacity>
              <TouchableOpacity
                style={styles.btnModalOk}
                onPress={() => {
                  if (customDesde.trim() && customHasta.trim()) {
                    const d = customDesde.trim();
                    const h = customHasta.trim();
                    setDesde(d);
                    setHasta(h);
                    setEtiquetaPeriodo(`${formatearFechaIsoShort(d)} - ${formatearFechaIsoShort(h)}`);
                  }
                  setCustomModal(false);
                }}
              >
                <Text style={{ color: colors.blanco, fontWeight: '700' }}>Aplicar</Text>
              </TouchableOpacity>
            </View>
          </View>
        </KeyboardAvoidingView>
      </Modal>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  topHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    backgroundColor: colors.marca,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  backBtn: { marginRight: 8, padding: 4 },
  topHeaderTitle: { fontSize: 18, fontWeight: '700', color: colors.blanco },
  btnPdf: {
    backgroundColor: 'rgba(255,255,255,0.18)',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 14,
  },
  btnPdfText: { color: colors.blanco, fontWeight: '700', fontSize: 12 },
  toggleRow: { flexDirection: 'row', backgroundColor: colors.blanco, padding: 12, gap: 8 },
  toggleBtn: {
    flex: 1,
    height: 40,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  toggleBtnActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  toggleText: { fontSize: 13, color: colors.textoSecundario, fontWeight: '600' },
  toggleTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  periodoBox: {
    backgroundColor: colors.blanco,
    paddingHorizontal: 16,
    paddingBottom: 12,
  },
  btnPeriodo: {
    height: 44,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnPeriodoText: { fontSize: 13, fontWeight: '600', color: colors.textoPrincipal },
  body: { flex: 1, paddingHorizontal: 16, paddingTop: 12 },
  cardResumen: {
    backgroundColor: colors.marcaClaro,
    borderRadius: 16,
    padding: 16,
    marginBottom: 12,
  },
  resumenTitulo: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  resumenSub: { fontSize: 13, color: colors.textoSecundario, marginTop: 4 },
  resumenTotales: { fontSize: 14, fontWeight: '700', color: colors.marcaOscuro, marginTop: 8 },
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
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 40 },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.55)',
    justifyContent: 'center',
    padding: 20,
  },
  modalCard: { backgroundColor: colors.blanco, borderRadius: 16, padding: 20 },
  label: { fontSize: 12, fontWeight: '700', color: colors.textoPrincipal, marginTop: 12, marginBottom: 4 },
  input: {
    backgroundColor: colors.fondo,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 10,
    padding: 11,
    fontSize: 14,
  },
  btnModalCancel: {
    flex: 1,
    height: 44,
    borderRadius: 22,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnModalOk: {
    flex: 1,
    height: 44,
    borderRadius: 22,
    backgroundColor: colors.marca,
    justifyContent: 'center',
    alignItems: 'center',
  },
});