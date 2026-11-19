import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Modal,
  TextInput,
  ScrollView,
  KeyboardAvoidingView,
  Platform,
  RefreshControl,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';
import { formatearFechaIsoShort, normalizarTexto } from '../utils/fechas';

interface Props {
  token: string;
  onOpenOrder: (id: number) => void;
}

const ESTADOS = ['Todos los estados', 'pendiente', 'abierta', 'completada', 'vencida', 'rehabilitada'];

export default function OrdenesScreen({ token, onOpenOrder }: Props) {
  const [ordenes, setOrdenes] = useState<any[]>([]);
  const [conductores, setConductores] = useState<any[]>([]);
  const [gasolineras, setGasolineras] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  const [estadoSel, setEstadoSel] = useState<string | null>(null);
  const [conductorIdSel, setConductorIdSel] = useState<number | null>(null);
  const [gasolineraIdSel, setGasolineraIdSel] = useState<number | null>(null);
  const [desdeSel, setDesdeSel] = useState<string | null>(null);
  const [hastaSel, setHastaSel] = useState<string | null>(null);

  const [sheetVisible, setSheetVisible] = useState(false);
  const [tempEstado, setTempEstado] = useState<string | null>(null);
  const [tempConductorId, setTempConductorId] = useState<number | null>(null);
  const [tempGasolineraId, setTempGasolineraId] = useState<number | null>(null);
  const [tempDesde, setTempDesde] = useState('');
  const [tempHasta, setTempHasta] = useState('');
  const [busquedaConductor, setBusquedaConductor] = useState('');
  const [busquedaGasolinera, setBusquedaGasolinera] = useState('');

  useEffect(() => {
    const headers = { Accept: 'application/json', Authorization: `Bearer ${token}` };
    Promise.all([
      fetch(`${API_URL}/conductores`, { headers }).then((r) => r.json()).catch(() => []),
      fetch(`${API_URL}/gasolineras`, { headers }).then((r) => r.json()).catch(() => []),
    ]).then(([cData, gData]) => {
      setConductores(Array.isArray(cData) ? cData : cData.data || []);
      const gList = Array.isArray(gData) ? gData : gData.data || [];
      setGasolineras(gList.filter((g: any) => g.activo));
    });
  }, [token]);

  const cargarOrdenes = () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (estadoSel) params.append('estado', estadoSel);
    if (conductorIdSel) params.append('conductor_id', String(conductorIdSel));
    if (gasolineraIdSel) params.append('gasolinera_id', String(gasolineraIdSel));
    if (desdeSel) params.append('desde', desdeSel);
    if (hastaSel) params.append('hasta', hastaSel);

    const qs = params.toString() ? `?${params.toString()}` : '';
    fetch(`${API_URL}/ordenes${qs}`, {
      headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setOrdenes(Array.isArray(d) ? d : d.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    cargarOrdenes();
  }, [estadoSel, conductorIdSel, gasolineraIdSel, desdeSel, hastaSel, token]);

  const abrirSheet = () => {
    setTempEstado(estadoSel);
    setTempConductorId(conductorIdSel);
    setTempGasolineraId(gasolineraIdSel);
    setTempDesde(desdeSel || '');
    setTempHasta(hastaSel || '');
    setBusquedaConductor('');
    setBusquedaGasolinera('');
    setSheetVisible(true);
  };

  const etiquetaEstado = (est: string) => {
    switch (est) {
      case 'pendiente':
        return 'PENDIENTE';
      case 'abierta':
        return 'ABIERTA';
      case 'completada':
        return 'COMPLETADA';
      case 'vencida':
        return 'VENCIDA';
      case 'rehabilitada':
        return 'REHABILITADA';
      case 'tardio':
      case 'completado_tardio':
        return 'COMPLETADO TARDÍO';
      default:
        return est.toUpperCase();
    }
  };

  const colorBadge = (est: string) => {
    switch (est) {
      case 'pendiente':
        return { bg: colors.pendienteFondo, fg: colors.pendiente };
      case 'abierta':
        return { bg: colors.abiertaFondo, fg: colors.abierta };
      case 'completada':
        return { bg: colors.completadaFondo, fg: colors.completada };
      case 'vencida':
      case 'tardio':
      case 'completado_tardio':
        return { bg: colors.vencidaFondo, fg: colors.vencida };
      case 'rehabilitada':
        return { bg: colors.abiertaFondo, fg: colors.marca };
      default:
        return { bg: colors.pendienteFondo, fg: colors.pendiente };
    }
  };

  const hayChips = !!(estadoSel || conductorIdSel || gasolineraIdSel || (desdeSel && hastaSel));

  const conductoresFiltrados = conductores.filter((c) => {
    const nombre = c.usuario?.name || `Conductor #${c.id}`;
    return normalizarTexto(nombre).includes(normalizarTexto(busquedaConductor));
  });

  const gasolinerasFiltradas = gasolineras.filter((g) => {
    const nombre = g.direccion ? `${g.nombre} (${g.direccion})` : g.nombre;
    return normalizarTexto(nombre).includes(normalizarTexto(busquedaGasolinera));
  });

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <Text style={styles.topHeaderTitle}>Órdenes</Text>
      </View>

      <View style={styles.filterBar}>
        <TouchableOpacity style={styles.btnFiltros} onPress={abrirSheet}>
          <Text style={styles.btnFiltrosText}>☰ Filtros</Text>
        </TouchableOpacity>

        {hayChips && (
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginLeft: 8 }}>
            {estadoSel && (
              <TouchableOpacity style={styles.chip} onPress={() => setEstadoSel(null)}>
                <Text style={styles.chipText}>{etiquetaEstado(estadoSel)} ✕</Text>
              </TouchableOpacity>
            )}
            {conductorIdSel && (
              <TouchableOpacity style={styles.chip} onPress={() => setConductorIdSel(null)}>
                <Text style={styles.chipText}>
                  {conductores.find((c) => c.id === conductorIdSel)?.usuario?.name ||
                    `Conductor #${conductorIdSel}`}{' '}
                  ✕
                </Text>
              </TouchableOpacity>
            )}
            {gasolineraIdSel && (
              <TouchableOpacity style={styles.chip} onPress={() => setGasolineraIdSel(null)}>
                <Text style={styles.chipText}>
                  {gasolineras.find((g) => g.id === gasolineraIdSel)?.nombre ||
                    `Gasolinera #${gasolineraIdSel}`}{' '}
                  ✕
                </Text>
              </TouchableOpacity>
            )}
            {desdeSel && hastaSel && (
              <TouchableOpacity
                style={styles.chip}
                onPress={() => {
                  setDesdeSel(null);
                  setHastaSel(null);
                }}
              >
                <Text style={styles.chipText}>
                  {formatearFechaIsoShort(desdeSel).slice(0, 5)} -{' '}
                  {formatearFechaIsoShort(hastaSel).slice(0, 5)} ✕
                </Text>
              </TouchableOpacity>
            )}
          </ScrollView>
        )}
      </View>

      <FlatList
        data={ordenes}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        refreshControl={<RefreshControl refreshing={loading} onRefresh={cargarOrdenes} />}
        renderItem={({ item }) => {
          const b = colorBadge(item.estado);
          return (
            <TouchableOpacity style={styles.card} onPress={() => onOpenOrder(item.id)}>
              <View style={{ flex: 1 }}>
                <Text style={styles.orderNum}>Orden #{item.numero_orden}</Text>
                <Text style={styles.stationName}>
                  {item.gasolinera?.nombre || 'Gasolinera por definir'}
                </Text>
              </View>
              <View style={[styles.badge, { backgroundColor: b.bg }]}>
                <Text style={[styles.badgeText, { color: b.fg }]}>{etiquetaEstado(item.estado)}</Text>
              </View>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          !loading ? (
            <Text style={styles.emptyText}>Todavía no tienes órdenes.</Text>
          ) : undefined
        }
      />

      <Modal
        visible={sheetVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setSheetVisible(false)}
      >
        <KeyboardAvoidingView
          style={styles.modalOverlay}
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        >
          <TouchableOpacity
            style={StyleSheet.absoluteFill}
            activeOpacity={1}
            onPress={() => setSheetVisible(false)}
          />

          <View style={styles.bottomSheet}>
            <ScrollView
              bounces={false}
              keyboardShouldPersistTaps="handled"
              showsVerticalScrollIndicator={false}
              contentContainerStyle={{ paddingBottom: 28 }}
            >
              <Text style={styles.sheetTitle}>Filtros de órdenes</Text>

              <Text style={styles.label}>Filtrar por estado</Text>
              <View style={styles.pillsWrap}>
                {ESTADOS.map((est) => {
                  const active = (est === 'Todos los estados' && !tempEstado) || tempEstado === est;
                  return (
                    <TouchableOpacity
                      key={est}
                      style={[styles.selectPill, active && styles.selectPillActive]}
                      onPress={() => setTempEstado(est === 'Todos los estados' ? null : est)}
                    >
                      <Text style={[styles.selectPillText, active && styles.selectPillTextActive]}>
                        {est === 'Todos los estados' ? est : etiquetaEstado(est)}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </View>

              <Text style={styles.label}>Filtrar por conductor</Text>
              <TextInput
                style={styles.input}
                placeholder="Buscar conductor..."
                placeholderTextColor={colors.textoSecundario}
                value={busquedaConductor}
                onChangeText={setBusquedaConductor}
              />
              <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginTop: 8 }}>
                <TouchableOpacity
                  style={[styles.selectPill, !tempConductorId && styles.selectPillActive]}
                  onPress={() => setTempConductorId(null)}
                >
                  <Text
                    style={[
                      styles.selectPillText,
                      !tempConductorId && styles.selectPillTextActive,
                    ]}
                  >
                    Todos los conductores
                  </Text>
                </TouchableOpacity>
                {conductoresFiltrados.map((c) => {
                  const active = tempConductorId === c.id;
                  return (
                    <TouchableOpacity
                      key={c.id}
                      style={[styles.selectPill, active && styles.selectPillActive]}
                      onPress={() => setTempConductorId(c.id)}
                    >
                      <Text style={[styles.selectPillText, active && styles.selectPillTextActive]}>
                        {c.usuario?.name || `Conductor #${c.id}`}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </ScrollView>

              <Text style={styles.label}>Filtrar por gasolinera</Text>
              <TextInput
                style={styles.input}
                placeholder="Buscar gasolinera..."
                placeholderTextColor={colors.textoSecundario}
                value={busquedaGasolinera}
                onChangeText={setBusquedaGasolinera}
              />
              <ScrollView horizontal showsHorizontalScrollIndicator={false} style={{ marginTop: 8 }}>
                <TouchableOpacity
                  style={[styles.selectPill, !tempGasolineraId && styles.selectPillActive]}
                  onPress={() => setTempGasolineraId(null)}
                >
                  <Text
                    style={[
                      styles.selectPillText,
                      !tempGasolineraId && styles.selectPillTextActive,
                    ]}
                  >
                    Todas las gasolineras
                  </Text>
                </TouchableOpacity>
                {gasolinerasFiltradas.map((g) => {
                  const active = tempGasolineraId === g.id;
                  return (
                    <TouchableOpacity
                      key={g.id}
                      style={[styles.selectPill, active && styles.selectPillActive]}
                      onPress={() => setTempGasolineraId(g.id)}
                    >
                      <Text style={[styles.selectPillText, active && styles.selectPillTextActive]}>
                        {g.nombre}
                      </Text>
                    </TouchableOpacity>
                  );
                })}
              </ScrollView>

              <Text style={styles.label}>Rango de fechas (AAAA-MM-DD)</Text>
              <View style={{ flexDirection: 'row', gap: 8 }}>
                <TextInput
                  style={[styles.input, { flex: 1 }]}
                  placeholder="Desde (2026-11-01)"
                  placeholderTextColor={colors.textoSecundario}
                  value={tempDesde}
                  onChangeText={setTempDesde}
                />
                <TextInput
                  style={[styles.input, { flex: 1 }]}
                  placeholder="Hasta (2026-11-30)"
                  placeholderTextColor={colors.textoSecundario}
                  value={tempHasta}
                  onChangeText={setTempHasta}
                />
              </View>

              <View style={styles.sheetActions}>
                <TouchableOpacity
                  style={styles.btnLimpiar}
                  onPress={() => {
                    setTempEstado(null);
                    setTempConductorId(null);
                    setTempGasolineraId(null);
                    setTempDesde('');
                    setTempHasta('');
                    setBusquedaConductor('');
                    setBusquedaGasolinera('');
                  }}
                >
                  <Text style={styles.btnLimpiarText}>Limpiar</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={styles.btnAplicar}
                  onPress={() => {
                    setEstadoSel(tempEstado);
                    setConductorIdSel(tempConductorId);
                    setGasolineraIdSel(tempGasolineraId);
                    if (tempDesde.trim() && tempHasta.trim()) {
                      setDesdeSel(tempDesde.trim());
                      setHastaSel(tempHasta.trim());
                    } else {
                      setDesdeSel(null);
                      setHastaSel(null);
                    }
                    setSheetVisible(false);
                  }}
                >
                  <Text style={styles.btnAplicarText}>Aplicar</Text>
                </TouchableOpacity>
              </View>
            </ScrollView>
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
    backgroundColor: colors.marca,
    paddingHorizontal: 16,
    paddingVertical: 14,
  },
  topHeaderTitle: { fontSize: 18, fontWeight: '700', color: colors.blanco },
  filterBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: 16,
    paddingTop: 12,
    paddingBottom: 8,
  },
  btnFiltros: {
    backgroundColor: colors.blanco,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 8,
  },
  btnFiltrosText: { fontSize: 13, fontWeight: '600', color: colors.textoPrincipal },
  chip: {
    backgroundColor: colors.marcaClaro,
    borderRadius: 16,
    paddingHorizontal: 12,
    paddingVertical: 7,
    marginRight: 6,
  },
  chipText: { fontSize: 12, fontWeight: '700', color: colors.marcaOscuro },
  list: { paddingHorizontal: 20, paddingTop: 8, paddingBottom: 88 },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 10,
  },
  orderNum: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  stationName: { fontSize: 13, color: colors.textoSecundario, marginTop: 2 },
  badge: { paddingHorizontal: 10, paddingVertical: 5, borderRadius: 12 },
  badgeText: { fontSize: 11, fontWeight: '700' },
  emptyText: { textAlign: 'center', color: colors.textoSecundario, marginTop: 40 },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.5)',
    justifyContent: 'flex-end',
  },
  bottomSheet: {
    backgroundColor: colors.blanco,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    maxHeight: '88%',
    padding: 20,
  },
  sheetTitle: { fontSize: 18, fontWeight: '700', color: colors.textoPrincipal, marginBottom: 12 },
  label: { fontSize: 12, fontWeight: '700', color: colors.textoPrincipal, marginTop: 12, marginBottom: 6 },
  pillsWrap: { flexDirection: 'row', flexWrap: 'wrap', gap: 6 },
  selectPill: {
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 16,
    paddingHorizontal: 12,
    paddingVertical: 7,
    marginRight: 6,
    backgroundColor: colors.fondo,
  },
  selectPillActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  selectPillText: { fontSize: 12, color: colors.textoSecundario, fontWeight: '600' },
  selectPillTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  input: {
    backgroundColor: colors.fondo,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    padding: 11,
    fontSize: 14,
    color: colors.textoPrincipal,
  },
  sheetActions: { flexDirection: 'row', justifyContent: 'flex-end', gap: 12, marginTop: 20 },
  btnLimpiar: {
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 20,
    paddingHorizontal: 20,
    paddingVertical: 10,
  },
  btnLimpiarText: { color: colors.textoPrincipal, fontSize: 13, fontWeight: '600' },
  btnAplicar: {
    backgroundColor: colors.marca,
    borderRadius: 20,
    paddingHorizontal: 22,
    paddingVertical: 10,
  },
  btnAplicarText: { color: colors.blanco, fontSize: 13, fontWeight: '700' },
});