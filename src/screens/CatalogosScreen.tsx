import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Modal,
  TextInput,
  Alert,
  Image,
  ScrollView,
} from 'react-native';
import { API_URL } from '../api/config';
import { colors } from '../theme/colors';

interface Props {
  token: string;
  user?: any;
}

type Tab = 'conductores' | 'vehiculos' | 'gasolineras';

export default function CatalogosScreen({ token, user }: Props) {
  const isAdmin = user?.rol === 'admin';
  const [tab, setTab] = useState<Tab>('conductores');
  const [datos, setDatos] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);

  // Modal Crear / Editar
  const [formModalVisible, setFormModalVisible] = useState(false);
  const [editItem, setEditItem] = useState<any | null>(null);
  const [saving, setSaving] = useState(false);

  // Campos Conductor
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [telefono, setTelefono] = useState('');
  const [cargo, setCargo] = useState('');

  // Campos Vehículo
  const [placa, setPlaca] = useState('');
  const [marca, setMarca] = useState('');
  const [modelo, setModelo] = useState('');
  const [anio, setAnio] = useState('');
  const [tipoCombustible, setTipoCombustible] = useState('Regular');

  // Campos Gasolinera
  const [nombreGas, setNombreGas] = useState('');
  const [direccionGas, setDireccionGas] = useState('');
  const [codigoQr, setCodigoQr] = useState('');

  // Modal Ver QR
  const [qrModalGas, setQrModalGas] = useState<any | null>(null);

  const cargarCatalogo = () => {
    setLoading(true);
    fetch(`${API_URL}/${tab}`, {
      headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
    })
      .then((r) => r.json())
      .then((d) => setDatos(Array.isArray(d) ? d : d.data || []))
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    cargarCatalogo();
  }, [tab, token]);

  const abrirFormulario = (item: any | null) => {
    setEditItem(item);
    if (tab === 'conductores') {
      setName(item?.usuario?.name || '');
      setEmail(item?.usuario?.email || '');
      setPassword('');
      setTelefono(item?.telefono || '');
      setCargo(item?.cargo || '');
    } else if (tab === 'vehiculos') {
      setPlaca(item?.placa || '');
      setMarca(item?.marca || '');
      setModelo(item?.modelo || '');
      setAnio(item?.anio ? String(item.anio) : '');
      setTipoCombustible(item?.tipo_combustible || 'Regular');
    } else {
      setNombreGas(item?.nombre || '');
      setDireccionGas(item?.direccion || '');
      setCodigoQr(item?.codigo_qr || '');
    }
    setFormModalVisible(true);
  };

  const guardarFormulario = async () => {
    setSaving(true);
    try {
      let body: any = {};
      if (tab === 'conductores') {
        body = { name: name.trim(), email: email.trim(), telefono: telefono.trim(), cargo: cargo.trim() };
        if (password.trim()) body.password = password;
      } else if (tab === 'vehiculos') {
        body = {
          placa: placa.trim(),
          marca: marca.trim(),
          modelo: modelo.trim(),
          anio: anio ? parseInt(anio) : null,
          tipo_combustible: tipoCombustible,
        };
      } else {
        body = {
          nombre: nombreGas.trim(),
          direccion: direccionGas.trim(),
          codigo_qr: codigoQr.trim(),
        };
      }

      const url = editItem ? `${API_URL}/${tab}/${editItem.id}` : `${API_URL}/${tab}`;
      const method = editItem ? 'PUT' : 'POST';

      const res = await fetch(url, {
        method,
        headers: {
          Accept: 'application/json',
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(body),
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.mensaje || data.message || 'Error al guardar registro.');

      Alert.alert('Éxito', editItem ? 'Actualización exitosa' : 'Registro exitoso');
      setFormModalVisible(false);
      cargarCatalogo();
    } catch (e: any) {
      Alert.alert('Error', e.message);
    } finally {
      setSaving(false);
    }
  };

  const eliminarElemento = (id: number) => {
    Alert.alert('Eliminar', '¿Qué deseas hacer con este elemento?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Eliminar',
        style: 'destructive',
        onPress: async () => {
          try {
            const res = await fetch(`${API_URL}/${tab}/${id}`, {
              method: 'DELETE',
              headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
            });
            if (!res.ok) throw new Error('No se pudo eliminar el elemento.');
            Alert.alert('Éxito', 'Elemento eliminado con éxito');
            cargarCatalogo();
          } catch (e: any) {
            Alert.alert('Error', e.message);
          }
        },
      },
    ]);
  };

  const mostrarOpciones = (item: any, titulo: string) => {
    if (!isAdmin) return;
    if (tab === 'gasolineras') {
      Alert.alert(titulo, 'Selecciona una acción', [
        { text: 'Ver código QR', onPress: () => setQrModalGas(item) },
        { text: 'Editar', onPress: () => abrirFormulario(item) },
        { text: 'Eliminar', style: 'destructive', onPress: () => eliminarElemento(item.id) },
        { text: 'Cancelar', style: 'cancel' },
      ]);
    } else {
      Alert.alert(titulo, 'Selecciona una acción', [
        { text: 'Editar', onPress: () => abrirFormulario(item) },
        { text: 'Eliminar', style: 'destructive', onPress: () => eliminarElemento(item.id) },
        { text: 'Cancelar', style: 'cancel' },
      ]);
    }
  };

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <Text style={styles.topHeaderTitle}>Catálogos</Text>
        {isAdmin && (
          <TouchableOpacity style={styles.btnNuevo} onPress={() => abrirFormulario(null)}>
            <Text style={styles.btnNuevoText}>＋ Nuevo</Text>
          </TouchableOpacity>
        )}
      </View>

      <View style={styles.toggleRow}>
        {(['conductores', 'vehiculos', 'gasolineras'] as Tab[]).map((t) => (
          <TouchableOpacity
            key={t}
            style={[styles.toggleBtn, tab === t && styles.toggleBtnActive]}
            onPress={() => setTab(t)}
          >
            <Text style={[styles.toggleText, tab === t && styles.toggleTextActive]}>
              {t === 'conductores' ? 'Conductores' : t === 'vehiculos' ? 'Vehículos' : 'Gasolineras'}
            </Text>
          </TouchableOpacity>
        ))}
      </View>

      <FlatList
        data={datos}
        keyExtractor={(item) => item.id.toString()}
        contentContainerStyle={styles.list}
        onRefresh={cargarCatalogo}
        refreshing={loading}
        renderItem={({ item }) => {
          let titulo = '';
          let subtitulo = '';

          if (tab === 'conductores') {
            titulo = item.usuario?.name || `Conductor #${item.id}`;
            subtitulo = `${item.telefono || 'Sin tel.'} · ${item.cargo || 'Conductor'}${
              item.activo === false ? ' · inactivo' : ''
            }`;
          } else if (tab === 'vehiculos') {
            titulo = `${item.placa} · ${item.marca} ${item.modelo}`;
            subtitulo = `${item.tipo_combustible}${item.activo === false ? ' · inactivo' : ''}`;
          } else {
            titulo = item.nombre;
            subtitulo = `${item.direccion || '—'} · QR: ${item.codigo_qr}`;
          }

          return (
            <TouchableOpacity
              style={styles.cardItem}
              activeOpacity={isAdmin ? 0.7 : 1}
              onPress={() => mostrarOpciones(item, titulo)}
            >
              <Text style={styles.itemTitle}>{titulo}</Text>
              <Text style={styles.itemSubtitle}>{subtitulo}</Text>
            </TouchableOpacity>
          );
        }}
        ListEmptyComponent={
          loading ? (
            <ActivityIndicator color={colors.marca} style={{ marginTop: 20 }} />
          ) : (
            <Text style={styles.emptyText}>Sin registros disponibles.</Text>
          )
        }
      />

      {/* Modal Crear / Editar */}
      <Modal visible={formModalVisible} animationType="slide" transparent>
        <View style={styles.modalOverlay}>
          <View style={styles.modalSheet}>
            <ScrollView contentContainerStyle={{ padding: 20 }}>
              <Text style={styles.modalTitle}>
                {editItem
                  ? `Editar ${tab === 'conductores' ? 'conductor' : tab === 'vehiculos' ? 'vehículo' : 'gasolinera'}`
                  : tab === 'conductores'
                  ? 'Nuevo conductor'
                  : tab === 'vehiculos'
                  ? 'Nuevo vehículo'
                  : 'Nueva gasolinera'}
              </Text>

              {tab === 'conductores' && (
                <>
                  <TextInput style={styles.input} placeholder="Nombre" value={name} onChangeText={setName} />
                  <TextInput style={styles.input} placeholder="Correo electrónico" keyboardType="email-address" autoCapitalize="none" value={email} onChangeText={setEmail} />
                  <TextInput style={styles.input} placeholder={editItem ? 'Contraseña (opcional)' : 'Contraseña'} secureTextEntry value={password} onChangeText={setPassword} />
                  <TextInput style={styles.input} placeholder="Teléfono" keyboardType="phone-pad" value={telefono} onChangeText={setTelefono} />
                  <TextInput style={styles.input} placeholder="Cargo" value={cargo} onChangeText={setCargo} />
                </>
              )}

              {tab === 'vehiculos' && (
                <>
                  <TextInput style={styles.input} placeholder="Placa" autoCapitalize="characters" value={placa} onChangeText={setPlaca} />
                  <TextInput style={styles.input} placeholder="Marca" value={marca} onChangeText={setMarca} />
                  <TextInput style={styles.input} placeholder="Modelo" value={modelo} onChangeText={setModelo} />
                  <TextInput style={styles.input} placeholder="Año" keyboardType="number-pad" value={anio} onChangeText={setAnio} />
                  <View style={styles.combRow}>
                    {['Regular', 'Super', 'Diesel'].map((c) => (
                      <TouchableOpacity
                        key={c}
                        style={[styles.combChip, tipoCombustible === c && styles.combChipActive]}
                        onPress={() => setTipoCombustible(c)}
                      >
                        <Text style={[styles.combText, tipoCombustible === c && { color: colors.blanco }]}>{c}</Text>
                      </TouchableOpacity>
                    ))}
                  </View>
                </>
              )}

              {tab === 'gasolineras' && (
                <>
                  <TextInput
                    style={styles.input}
                    placeholder="Nombre"
                    value={nombreGas}
                    onChangeText={(txt) => {
                      setNombreGas(txt);
                      if (!editItem) {
                        const gen = 'QR-' + txt.trim().toUpperCase().replace(/[^A-Z0-9]+/g, '_').replace(/^_|_$/g, '');
                        setCodigoQr(gen);
                      }
                    }}
                  />
                  <TextInput style={styles.input} placeholder="Dirección" value={direccionGas} onChangeText={setDireccionGas} />
                  <TextInput style={styles.input} placeholder="Código QR" value={codigoQr} onChangeText={setCodigoQr} />
                </>
              )}

              <TouchableOpacity style={styles.btnSave} onPress={guardarFormulario} disabled={saving}>
                {saving ? <ActivityIndicator color={colors.blanco} /> : <Text style={styles.btnSaveText}>Guardar</Text>}
              </TouchableOpacity>

              <TouchableOpacity style={styles.btnCancel} onPress={() => setFormModalVisible(false)}>
                <Text style={styles.btnCancelText}>Cancelar</Text>
              </TouchableOpacity>
            </ScrollView>
          </View>
        </View>
      </Modal>

      {/* Modal Ver Código QR de Gasolinera */}
      <Modal visible={!!qrModalGas} transparent animationType="fade">
        <View style={styles.qrOverlay}>
          <View style={styles.qrCard}>
            <Text style={styles.modalTitle}>{qrModalGas?.nombre}</Text>
            {qrModalGas?.codigo_qr && (
              <Image
                source={{
                  uri: `https://api.qrserver.com/v1/create-qr-code/?size=250x250&data=${encodeURIComponent(
                    qrModalGas.codigo_qr
                  )}`,
                }}
                style={{ width: 220, height: 220, marginVertical: 16 }}
              />
            )}
            <Text style={{ fontWeight: '700', fontSize: 16, color: colors.textoPrincipal }}>
              {qrModalGas?.codigo_qr}
            </Text>
            <TouchableOpacity style={styles.btnSave} onPress={() => setQrModalGas(null)}>
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
    backgroundColor: colors.blanco,
    padding: 20,
  },
  topHeaderTitle: { fontSize: 21, fontWeight: '700', color: colors.textoPrincipal },
  btnNuevo: {
    backgroundColor: colors.marca,
    paddingHorizontal: 14,
    height: 36,
    borderRadius: 18,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnNuevoText: { color: colors.blanco, fontSize: 12, fontWeight: '700' },
  toggleRow: { flexDirection: 'row', backgroundColor: colors.blanco, padding: 16, gap: 8 },
  toggleBtn: {
    flex: 1,
    height: 40,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.borde,
    justifyContent: 'center',
    alignItems: 'center',
  },
  toggleBtnActive: { backgroundColor: colors.marcaClaro, borderColor: colors.marca },
  toggleText: { fontSize: 12, color: colors.textoSecundario, fontWeight: '600' },
  toggleTextActive: { color: colors.marcaOscuro, fontWeight: '700' },
  list: { padding: 20 },
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
  modalOverlay: { flex: 1, backgroundColor: 'rgba(0,0,0,0.45)', justifyContent: 'flex-end' },
  modalSheet: {
    backgroundColor: colors.blanco,
    borderTopLeftRadius: 24,
    borderTopRightRadius: 24,
    maxHeight: '88%',
  },
  modalTitle: { fontSize: 18, fontWeight: '700', color: colors.textoPrincipal, marginBottom: 14 },
  input: {
    backgroundColor: colors.fondo,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 10,
    padding: 12,
    fontSize: 14,
    color: colors.textoPrincipal,
    marginBottom: 12,
  },
  combRow: { flexDirection: 'row', gap: 8, marginBottom: 14 },
  combChip: {
    flex: 1,
    paddingVertical: 10,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: colors.borde,
    alignItems: 'center',
    backgroundColor: colors.fondo,
  },
  combChipActive: { backgroundColor: colors.marca, borderColor: colors.marca },
  combText: { fontSize: 13, fontWeight: '700', color: colors.textoPrincipal },
  btnSave: {
    backgroundColor: colors.marca,
    height: 48,
    borderRadius: 24,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 8,
    width: '100%',
  },
  btnSaveText: { color: colors.blanco, fontWeight: '700', fontSize: 15 },
  btnCancel: { alignItems: 'center', padding: 12, marginTop: 4 },
  btnCancelText: { color: colors.textoSecundario, fontWeight: '600' },
  qrOverlay: {
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
  },
});