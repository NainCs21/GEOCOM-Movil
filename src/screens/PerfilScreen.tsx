import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity } from 'react-native';
import { colors } from '../theme/colors';

interface Props {
  user: any;
  onLogout: () => void;
  onBack?: () => void;
  showBack?: boolean;
}

export default function PerfilScreen({ user, onLogout, onBack, showBack = true }: Props) {
  const esConductor = user?.rol === 'conductor';
  const etiquetaRol = esConductor
    ? 'CONDUCTOR'
    : user?.rol === 'admin'
    ? 'Administrador'
    : 'Supervisor';

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        {showBack && onBack && (
          <TouchableOpacity onPress={onBack} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.blanco }}>←</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.topHeaderTitle}>Datos de mi cuenta</Text>
      </View>

      <View style={styles.body}>
        <View style={styles.card}>
          <View style={styles.rolBadge}>
            <Text style={styles.rolText}>{etiquetaRol}</Text>
          </View>
          <Text style={styles.nombre}>{user?.nombre || ''}</Text>
          <Text style={styles.correo}>{user?.correo || ''}</Text>
        </View>

        {esConductor && (
          <View style={styles.infoCard}>
            <Text style={styles.infoTitle}>Tu información es personal</Text>
            <Text style={styles.infoSub}>
              Solo puedes consultar y gestionar tus propias solicitudes.
            </Text>
            <Text style={[styles.infoSub, { marginTop: 8 }]}>
              Si necesitas actualizar tus datos, contacta al administrador.
            </Text>
          </View>
        )}

        <View style={{ flex: 1 }} />

        {esConductor && (
          <Text style={styles.logoutNote}>
            Al salir, volverás a la pantalla de acceso. Tus solicitudes y evidencias guardadas se
            conservarán.
          </Text>
        )}

        <TouchableOpacity style={styles.btnLogout} onPress={onLogout}>
          <Text style={styles.btnLogoutText}>Cerrar sesión</Text>
        </TouchableOpacity>
      </View>
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
  backBtn: { marginRight: 8, padding: 4 },
  topHeaderTitle: { fontSize: 18, fontWeight: '700', color: colors.blanco },
  body: { flex: 1, padding: 20 },
  card: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 16,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  rolBadge: {
    alignSelf: 'flex-start',
    backgroundColor: colors.abiertaFondo,
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 10,
  },
  rolText: { color: colors.abierta, fontSize: 11, fontWeight: '700' },
  nombre: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal, marginTop: 12 },
  correo: { fontSize: 14, color: colors.textoSecundario, marginTop: 2 },
  infoCard: {
    backgroundColor: colors.marcaClaro,
    borderRadius: 16,
    padding: 16,
    marginTop: 16,
  },
  infoTitle: { fontSize: 14, fontWeight: '700', color: colors.marcaOscuro },
  infoSub: { fontSize: 13, color: colors.textoSecundario, marginTop: 4 },
  logoutNote: {
    textAlign: 'center',
    color: colors.textoSecundario,
    fontSize: 12,
    marginBottom: 12,
  },
  btnLogout: {
    backgroundColor: colors.vencida,
    height: 50,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnLogoutText: { color: colors.blanco, fontWeight: '700', fontSize: 15 },
});