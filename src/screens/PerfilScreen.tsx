import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { colors } from '../theme/colors';

interface Props {
  user: any;
  onLogout: () => void;
  onBack?: () => void;
  showBack?: boolean;
}

export default function PerfilScreen({ user, onLogout, onBack, showBack = false }: Props) {
  const isConductor = user?.rol === 'conductor';
  const rolTexto =
    user?.rol === 'admin'
      ? 'ADMINISTRADOR'
      : user?.rol === 'contabilidad'
      ? 'SUPERVISOR'
      : 'CONDUCTOR';

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        {showBack && onBack && (
          <TouchableOpacity onPress={onBack} style={styles.backBtn}>
            <Text style={{ fontSize: 22, color: colors.textoPrincipal }}>←</Text>
          </TouchableOpacity>
        )}
        <Text style={styles.topHeaderTitle}>Datos de mi cuenta</Text>
      </View>

      <View style={styles.body}>
        <View style={styles.card}>
          <View style={styles.rolBadge}>
            <Text style={styles.rolBadgeText}>{rolTexto}</Text>
          </View>
          <Text style={styles.nombre}>{user?.nombre || ''}</Text>
          <Text style={styles.correo}>{user?.correo || ''}</Text>
        </View>

        {isConductor && (
          <View style={styles.infoCard}>
            <Text style={styles.infoTitle}>Tu información es personal</Text>
            <Text style={styles.infoBody}>
              Solo puedes consultar y gestionar tus propias solicitudes.
            </Text>
            <Text style={[styles.infoBody, { marginTop: 8 }]}>
              Si necesitas actualizar tus datos, contacta al administrador.
            </Text>
          </View>
        )}

        <View style={{ flex: 1 }} />

        {isConductor && (
          <Text style={styles.logoutHint}>
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
    backgroundColor: colors.blanco,
    padding: 20,
  },
  backBtn: { marginRight: 10, padding: 4 },
  topHeaderTitle: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal },
  body: { flex: 1, padding: 20 },
  card: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
    borderColor: colors.borde,
  },
  rolBadge: {
    backgroundColor: colors.abiertaFondo,
    alignSelf: 'flex-start',
    paddingHorizontal: 12,
    paddingVertical: 6,
    borderRadius: 12,
  },
  rolBadgeText: { color: colors.abierta, fontWeight: '700', fontSize: 11 },
  nombre: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal, marginTop: 12 },
  correo: { fontSize: 14, color: colors.textoSecundario, marginTop: 2 },
  infoCard: {
    backgroundColor: colors.marcaClaro,
    borderRadius: 16,
    padding: 16,
    marginTop: 16,
  },
  infoTitle: { fontSize: 14, fontWeight: '700', color: colors.marcaOscuro },
  infoBody: { fontSize: 13, color: colors.textoSecundario, marginTop: 4 },
  logoutHint: {
    fontSize: 12,
    color: colors.textoSecundario,
    textAlign: 'center',
    marginBottom: 12,
  },
  btnLogout: {
    backgroundColor: colors.vencida,
    height: 52,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
  },
  btnLogoutText: { color: colors.blanco, fontSize: 15, fontWeight: '700' },
});