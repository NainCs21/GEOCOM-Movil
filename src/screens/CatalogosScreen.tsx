import React, { useState, useEffect, useMemo } from 'react';
import {
  View,
  Text,
  StyleSheet,
  FlatList,
  TouchableOpacity,
  Modal,
  TextInput,
  ActivityIndicator,
  Alert,
  KeyboardAvoidingView,
  ScrollView,
  Platform,
  Image,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  token: string;
  user: any;
  onBack?: () => void;
}

type TabCatalogo = 'conductores' | 'vehiculos' | 'gasolineras';
type FiltroCombustible = 'todos' | 'gasolina' | 'diesel';

export default function CatalogosScreen({ token, user, onBack }: Props) {
  const [tab, setTab] = useState<TabCatalogo>('conductores');
  const [items, setItems] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  // Filtros de vehículos (igual que CatalogosFragment.kt)
  const [busquedaVehiculo, setBusquedaVehiculo] = useState('');
  const [filtroCombustible, setFiltroCombustible] = useState<FiltroCombustible>('todos');

  // Modal de creación / edición
  const [modalVisible, setModalVisible] = useState(false);
  const [editando, setEditando] = useState<any | null>(null);
  const [saving, setSaving] = useState(false);

  // Campos Conductor
  const [nombre, setNombre] = useState('');
  const [correo, setCorreo] = useState('');
  const [password, setPassword] = useState('');
  const [telefono, setTelefono] = useState('');
  const [cargo, setCargo] = useState('');

  // Campos Vehículo
  const [placa, setPlaca] = useState('');
  const [marca, setMarca] = useState('');
  const [modelo, setModelo] = useState('');
  const [anio, setAnio] = useState('');
  const [combustible, setCombustible] = useState<'Diesel' | 'Gasolina'>('Diesel');

  // Campos Gasolinera
  const [nombreGas, setNombreGas] = useState('');
  const [direccionGas, setDireccionGas] = useState('');
  const [codigoQr, setCodigoQr] = useState('');

  // Modal visor de QR
  const [qrModal, setQrModal] = useState<any | null>(null);

  const esAdmin = user?.rol === 'admin';

  const cargarDatos = async () => {
    setLoading(true);
    try {
      const endpoint = tab === 'vehiculos' ? `${API_URL}/vehiculos?todos=1` : `${API_URL}/${tab}`;
      const res = await fetch(endpoint, {
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      });
      const json = await res.json();
      setItems(Array.isArray(json) ? json : json.data || []);
    } catch {
      setItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    cargarDatos();
  }, [tab, token]);

  const itemsFiltrados = useMemo(() => {
    if (tab !== 'vehiculos') return items;
    const q = busquedaVehiculo.trim().toLowerCase();
    return items.filter((veh) => {
      const comb = (veh.tipo_combustible || '').toLowerCase();
      const matchComb =
        filtroCombustible === 'todos'
          ? true
          : filtroCombustible === 'gasolina'
          ? comb === 'gasolina'
          : comb.includes('diesel') || comb.includes('diésel');

      const texto = `${veh.placa} ${veh.marca} ${veh.modelo} ${veh.anio || ''} ${comb}`.toLowerCase();
      const matchTexto = !q || texto.includes(q);
      return matchComb && matchTexto;
    });
  }, [items, tab, busquedaVehiculo, filtroCombustible]);

  const abrirNuevo = () => {
    setEditando(null);
    setNombre('');
    setCorreo('');
    setPassword('');
    setTelefono('');
    setCargo('');
    setPlaca('');
    setMarca('');
    setModelo('');
    setAnio('');
    setCombustible('Diesel');
    setNombreGas('');
    setDireccionGas('');
    setCodigoQr('');
    setModalVisible(true);
  };

  const abrirEditar = (item: any) => {
    setEditando(item);
    if (tab === 'conductores') {
      setNombre(item.usuario?.name || '');
      setCorreo(item.usuario?.email || '');
      setPassword('');
      setTelefono(item.telefono || '');
      setCargo(item.cargo || '');
    } else if (tab === 'vehiculos') {
      setPlaca(item.placa || '');
      setMarca(item.marca || '');
      setModelo(item.modelo || '');
      setAnio(item.anio ? String(item.anio) : '');
      const comb = (item.tipo_combustible || '').toLowerCase();
      setCombustible(comb === 'gasolina' ? 'Gasolina' : 'Diesel');
    } else {
      setNombreGas(item.nombre || '');
      setDireccionGas(item.direccion || '');
      setCodigoQr(item.codigo_qr || '');
    }
    setModalVisible(true);
  };

  const confirmarDesactivar = (item: any) => {
    Alert.alert(
      'Desactivar',
      '¿Desactivar este registro? Dejará de estar disponible, pero no se borra y se puede reactivar después.',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Desactivar',
          style: 'destructive',
          onPress: async () => {
            try {
              const res = await fetch(`${API_URL}/${tab}/${item.id}`, {
                method: 'DELETE',
                headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
              });
              if (!res.ok) throw new Error('No se pudo desactivar el registro.');
              Alert.alert('Éxito', 'Registro exitoso');
              cargarDatos();
            } catch (e: any) {
              Alert.alert('Error', e.message);
            }
          },
        },
      ]
    );
  };

  const confirmarReactivar = (item: any) => {
    Alert.alert(
      'Reactivar',
      '¿Reactivar este registro? Volverá a estar disponible en el sistema.',
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Reactivar',
          onPress: async () => {
            try {
              const res = await fetch(`${API_URL}/${tab}/${item.id}`, {
                method: 'PUT',
                headers: {
                  Accept: 'application/json',
                  'Content-Type': 'application/json',
                  Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ activo: true }),
              });
              if (!res.ok) throw new Error('No se pudo reactivar el registro.');
              Alert.alert('Éxito', 'Registro reactivado');
              cargarDatos();
            } catch (e: any) {
              Alert.alert('Error', e.message);
            }
          },
        },
      ]
    );
  };

  const mostrarOpciones = (item: any) => {
    if (!esAdmin) return;
    const titulo =
      tab === 'conductores'
        ? item.usuario?.name || `Conductor #${item.id}`
        : tab === 'vehiculos'
        ? `${item.placa} · ${item.marca} ${item.modelo}`
        : item.nombre;

    const botones: any[] = [];
    if (tab === 'gasolineras') {
      botones.push({ text: 'Ver código QR', onPress: () => setQrModal(item) });
    }
    botones.push({ text: 'Editar', onPress: () => abrirEditar(item) });
    if (item.activo) {
      botones.push({
        text: 'Desactivar',
        style: 'destructive',
        onPress: () => confirmarDesactivar(item),
      });
    } else {
      botones.push({ text: 'Reactivar', onPress: () => confirmarReactivar(item) });
    }
    botones.push({ text: 'Cancelar', style: 'cancel' });

    Alert.alert(titulo, undefined, botones);
  };

  const guardar = async () => {
    let body: any = {};
    if (tab === 'conductores') {
      body = {
        nombre: nombre.trim(),
        correo: correo.trim(),
        telefono: telefono.trim(),
        cargo: cargo.trim(),
      };
      if (password.trim()) {
        body.contrasena = password.trim();
      }
    } else if (tab === 'vehiculos') {
      body = {
        placa: placa.trim(),
        marca: marca.trim(),
        modelo: modelo.trim(),
        anio: anio.trim() ? parseInt(anio.trim(), 10) : null,
        tipo_combustible: combustible === 'Gasolina' ? 'gasolina' : 'diesel',
      };
    } else {
      body = {
        nombre: nombreGas.trim(),
        direccion: direccionGas.trim(),
        codigo_qr: codigoQr.trim(),
      };
    }

    setSaving(true);
    try {
      const url = editando ? `${API_URL}/${tab}/${editando.id}` : `${API_URL}/${tab}`;
      const method = editando ? 'PUT' : 'POST';
      const res = await fetch(url, {
        method,
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(body),
      });
      const json = await res.json();
      if (!res.ok) throw new Error(json.mensaje || json.message || 'No se pudo guardar.');

      setModalVisible(false);
      Alert.alert('Éxito', 'Registro exitoso');
      cargarDatos();
    } catch (e: any) {
      Alert.alert('Error', e.message);
    } finally {
      setSaving(false);
    }
  };

  const handleChangeNombreGas = (texto: string) => {
    setNombreGas(texto);
    if (!editando) {
      const limpio = texto
        .trim()
        .toUpperCase()
        .replace(/[^A-Z0-9]+/g, '_')
        .replace(/^_+|_+$/g, '');
      setCodigoQr(`QR-${limpio}`);
    }
  };

  return (
    <View style={styles.flex}>
      {/* Toolbar morado idéntico a fragment_catalogos.xml */}
      <View style={styles.topHeader}>
        <View style={{ flexDirection: 'row', alignItems: 'center' }}>
          {onBack && (
            <TouchableOpacity onPress={onBack} style={styles.backBtn}>
              <Text style={{ fontSize: 22, color: colors.blanco }}>←</Text>
            </TouchableOpacity>
          )}
          <Text style={styles.topHeaderTitle}>Catálogos</Text>
        </View>
        {esAdmin && (
          <TouchableOpacity style={styles.btnNuevo} onPress={abrirNuevo}>
            <Text style={styles.btnNuevoText}>＋ Nuevo</Text>
          </TouchableOpacity>
        )}
      </View>

      {/* Selector de pestañas */}
      <View style={styles.tabsRow}>
        {(
          [
            { id: 'conductores', label: 'Conductores' },
            { id: 'vehiculos', label: 'Vehículos' },
            { id: 'gasolineras', label: 'Gasolineras' },
          ] as const
        ).map((t) => (
          <TouchableOpacity
            key={t.id}
            style={[styles.tabBtn, tab === t.id && styles.tabBtnActive]}
            onPress={() => setTab(t.id)}
          >
            <Text style={[styles.tabText, tab === t.id && styles.tabTextActive]}>{t.label}</Text>
          </TouchableOpacity>
        ))}
      </View>

      {/* Buscador y filtros exclusivos de Vehículos */}
      {tab === 'vehiculos' && (
        <View style={styles.filtrosVehiculosBox}>
          <TextInput
            style={styles.searchInput}
            placeholder="Buscar por placa, marca, modelo..."
            placeholderTextColor={colors.textoSecundario}
            value={busquedaVehiculo}
            onChangeText={setBusquedaVehiculo}
          />
          <View style={styles.combFilterRow}>
            {(
              [
                { id: 'todos', label: 'Todos' },
                { id: 'gasolina', label: 'Gasolina' },
                { id: 'diesel', label: 'Diesel' },
              ] as const
            ).map((f) => (
              <TouchableOpacity
                key={f.id}
                style={[styles.filterChip, filtroCombustible === f.id && styles.filterChipActive]}
                onPress={() => setFiltroCombustible(f.id)}
              >
                <Text
                  style={[
                    styles.filterChipText,
                    filtroCombustible === f.id && styles.filterChipTextActive,
                  ]}
                >
                  {f.label}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      )}

      <FlatList
        data={itemsFiltrados}
        keyExtractor={(item) => String(item.id)}
        contentContainerStyle={styles.list}
        renderItem={({ item }) => {
          const inactivoTag = !item.activo ? ' · inactivo' : '';
          let titulo = '';
          let subtitulo = '';

          if (tab === 'conductores') {
            titulo = item.usuario?.name || `Conductor #${item.id}`;
            subtitulo = `${item.telefono} · ${item.cargo}${inactivoTag}`;
          } else if (tab === 'vehiculos') {
            titulo = `${item.placa} · ${item.marca} ${item.modelo}`;
            subtitulo = `${item.tipo_combustible}${inactivoTag}`;
          } else {
            titulo = item.nombre;
            subtitulo = `${item.direccion || '—'} · QR: ${item.codigo_qr}`;
          }

          return (
            <TouchableOpacity
              style={styles.card}
              activeOpacity={0.8}
              onPress={() => mostrarOpciones(item)}
            >
              <View style={{ flex: 1 }}>
                <Text style={styles.cardTitle}>{titulo}</Text>
                <Text style={styles.cardSub}>{subtitulo}</Text>
              </View>
              {esAdmin && <Text style={{ color: colors.textoSecundario, fontSize: 18 }}>⋮</Text>}
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          loading ? (
            <ActivityIndicator color={colors.marca} style={{ marginTop: 30 }} />
          ) : (
            <Text style={styles.emptyText}>No hay registros en este catálogo.</Text>
          )
        }
      />

      {/* MODAL INFERIOR CON KEYBOARD AVOIDING VIEW PARA IOS */}
      <Modal
        visible={modalVisible}
        transparent
        animationType="slide"
        onRequestClose={() => setModalVisible(false)}
      >
        <KeyboardAvoidingView
          style={styles.modalOverlay}
          behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
        >
          <TouchableOpacity
            style={StyleSheet.absoluteFill}
            activeOpacity={1}
            onPress={() => setModalVisible(false)}
          />

          <View style={styles.bottomSheet}>
            <View style={styles.sheetHandle} />
            <ScrollView
              bounces={false}
              keyboardShouldPersistTaps="handled"
              showsVerticalScrollIndicator={false}
              contentContainerStyle={styles.sheetScroll}
            >
              <Text style={styles.modalTitle}>
                {editando
                  ? tab === 'conductores'
                    ? 'Editar conductor'
                    : tab === 'vehiculos'
                    ? 'Editar vehículo'
                    : 'Editar gasolinera'
                  : tab === 'conductores'
                  ? 'Nuevo conductor'
                  : tab === 'vehiculos'
                  ? 'Nuevo vehículo'
                  : 'Nueva gasolinera'}
              </Text>

              {tab === 'conductores' && (
                <>
                  <Text style={styles.inputLabel}>Nombre</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Nombre"
                    placeholderTextColor={colors.textoSecundario}
                    value={nombre}
                    onChangeText={setNombre}
                  />

                  <Text style={styles.inputLabel}>Correo electrónico</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Correo electrónico"
                    placeholderTextColor={colors.textoSecundario}
                    keyboardType="email-address"
                    autoCapitalize="none"
                    value={correo}
                    onChangeText={setCorreo}
                  />

                  <Text style={styles.inputLabel}>
                    {editando ? 'Contraseña (opcional)' : 'Contraseña'}
                  </Text>
                  <TextInput
                    style={styles.input}
                    placeholder={editando ? 'Contraseña (opcional)' : 'Contraseña'}
                    placeholderTextColor={colors.textoSecundario}
                    secureTextEntry
                    value={password}
                    onChangeText={setPassword}
                  />

                  <Text style={styles.inputLabel}>Teléfono</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Teléfono"
                    placeholderTextColor={colors.textoSecundario}
                    keyboardType="phone-pad"
                    value={telefono}
                    onChangeText={setTelefono}
                  />

                  <Text style={styles.inputLabel}>Cargo</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Cargo"
                    placeholderTextColor={colors.textoSecundario}
                    value={cargo}
                    onChangeText={setCargo}
                  />
                </>
              )}

              {tab === 'vehiculos' && (
                <>
                  <Text style={styles.inputLabel}>Placa</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Placa"
                    placeholderTextColor={colors.textoSecundario}
                    autoCapitalize="characters"
                    value={placa}
                    onChangeText={setPlaca}
                  />

                  <Text style={styles.inputLabel}>Marca</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Marca"
                    placeholderTextColor={colors.textoSecundario}
                    value={marca}
                    onChangeText={setMarca}
                  />

                  <Text style={styles.inputLabel}>Modelo</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Modelo"
                    placeholderTextColor={colors.textoSecundario}
                    value={modelo}
                    onChangeText={setModelo}
                  />

                  <Text style={styles.inputLabel}>Año</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Año"
                    placeholderTextColor={colors.textoSecundario}
                    keyboardType="number-pad"
                    value={anio}
                    onChangeText={setAnio}
                  />

                  <Text style={styles.inputLabel}>Tipo de combustible</Text>
                  <View style={styles.combRow}>
                    {(['Diesel', 'Gasolina'] as const).map((tipo) => (
                      <TouchableOpacity
                        key={tipo}
                        style={[styles.combChip, combustible === tipo && styles.combChipActive]}
                        onPress={() => setCombustible(tipo)}
                      >
                        <Text
                          style={[
                            styles.combChipText,
                            combustible === tipo && styles.combChipTextActive,
                          ]}
                        >
                          {tipo}
                        </Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                </>
              )}

              {tab === 'gasolineras' && (
                <>
                  <Text style={styles.inputLabel}>Nombre</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Nombre"
                    placeholderTextColor={colors.textoSecundario}
                    value={nombreGas}
                    onChangeText={handleChangeNombreGas}
                  />

                  <Text style={styles.inputLabel}>Dirección</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Dirección"
                    placeholderTextColor={colors.textoSecundario}
                    value={direccionGas}
                    onChangeText={setDireccionGas}
                  />

                  <Text style={styles.inputLabel}>Código QR</Text>
                  <TextInput
                    style={styles.input}
                    placeholder="Código QR"
                    placeholderTextColor={colors.textoSecundario}
                    value={codigoQr}
                    onChangeText={setCodigoQr}
                  />
                </>
              )}

              <View style={styles.modalActions}>
                <TouchableOpacity
                  style={styles.btnCancel}
                  onPress={() => setModalVisible(false)}
                >
                  <Text style={styles.btnCancelText}>Cancelar</Text>
                </TouchableOpacity>

                <TouchableOpacity
                  style={[styles.btnSave, saving && { opacity: 0.7 }]}
                  onPress={guardar}
                  disabled={saving}
                >
                  {saving ? (
                    <ActivityIndicator color={colors.blanco} />
                  ) : (
                    <Text style={styles.btnSaveText}>Guardar</Text>
                  )}
                </TouchableOpacity>
              </View>
            </ScrollView>
          </View>
        </KeyboardAvoidingView>
      </Modal>

      {/* Modal visor de Código QR */}
      <Modal visible={!!qrModal} transparent animationType="fade">
        <View style={styles.centerOverlay}>
          <View style={styles.qrCard}>
            <Text style={styles.modalTitle}>{qrModal?.nombre}</Text>
            {qrModal?.codigo_qr && (
              <Image
                source={{
                  uri: `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${encodeURIComponent(
                    qrModal.codigo_qr
                  )}`,
                }}
                style={{ width: 220, height: 220, marginVertical: 8 }}
              />
            )}
            <Text style={styles.qrCodeBig}>{qrModal?.codigo_qr}</Text>
            <TouchableOpacity style={styles.btnQrClose} onPress={() => setQrModal(null)}>
              <Text style={styles.btnSaveText}>Aceptar</Text>
            </TouchableOpacity>
          </View>
        </View>
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
  btnNuevo: {
    backgroundColor: colors.marcaClaro,
    paddingHorizontal: 14,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
  },
  btnNuevoText: { color: colors.marcaOscuro, fontWeight: '700', fontSize: 12 },
  tabsRow: {
    flexDirection: 'row',
    backgroundColor: colors.blanco,
    padding: 12,
    gap: 8,
  },
  tabBtn: {
    flex: 1,
    height: 40,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  tabBtnActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  tabText: { fontSize: 13, fontWeight: '600', color: colors.textoSecundario },
  tabTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  filtrosVehiculosBox: {
    backgroundColor: colors.blanco,
    paddingHorizontal: 12,
    paddingBottom: 12,
  },
  searchInput: {
    backgroundColor: colors.fondo,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 10,
    paddingHorizontal: 12,
    paddingVertical: 10,
    fontSize: 14,
    color: colors.textoPrincipal,
  },
  combFilterRow: { flexDirection: 'row', gap: 8, marginTop: 8 },
  filterChip: {
    flex: 1,
    height: 36,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  filterChipActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  filterChipText: { fontSize: 12, color: colors.textoSecundario, fontWeight: '600' },
  filterChipTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  list: { paddingHorizontal: 20, paddingTop: 16, paddingBottom: 88 },
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
  cardTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  cardSub: { fontSize: 13, color: colors.textoSecundario, marginTop: 2 },
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
    maxHeight: '85%',
    paddingHorizontal: 20,
    paddingTop: 12,
  },
  sheetHandle: {
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.borde,
    alignSelf: 'center',
    marginBottom: 12,
  },
  sheetScroll: { paddingBottom: 32 },
  modalTitle: { fontSize: 18, fontWeight: '700', color: colors.textoPrincipal, marginBottom: 8 },
  inputLabel: { fontSize: 12, fontWeight: '700', color: colors.textoPrincipal, marginTop: 10, marginBottom: 4 },
  input: {
    backgroundColor: colors.fondo,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    padding: 12,
    fontSize: 15,
    color: colors.textoPrincipal,
  },
  combRow: { flexDirection: 'row', gap: 8, marginTop: 4 },
  combChip: {
    flex: 1,
    height: 42,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: colors.fondo,
  },
  combChipActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  combChipText: { fontSize: 14, fontWeight: '600', color: colors.textoSecundario },
  combChipTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  modalActions: { flexDirection: 'row', gap: 10, marginTop: 20 },
  btnCancel: {
    flex: 1,
    height: 48,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnCancelText: { color: colors.textoSecundario, fontWeight: '700' },
  btnSave: {
    flex: 1,
    height: 48,
    borderRadius: 24,
    backgroundColor: colors.marca,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnSaveText: { color: colors.blanco, fontWeight: '700' },
  centerOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0,0,0,0.55)',
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
  },
  qrCard: {
    backgroundColor: colors.blanco,
    borderRadius: 20,
    padding: 24,
    width: '100%',
    alignItems: 'center',
    gap: 10,
  },
  qrCodeBig: {
    fontSize: 16,
    fontWeight: '700',
    color: colors.textoPrincipal,
  },
  btnQrClose: {
    marginTop: 10,
    backgroundColor: colors.marca,
    paddingHorizontal: 32,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
  },
});