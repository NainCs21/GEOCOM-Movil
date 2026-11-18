import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
} from 'react-native';
import { colors } from '../theme/colors';

interface Props {
  correo: string;
  setCorreo: (v: string) => void;
  password: string;
  setPassword: (v: string) => void;
  onLogin: () => void;
  loading: boolean;
}

export default function LoginScreen({
  correo,
  setCorreo,
  password,
  setPassword,
  onLogin,
  loading,
}: Props) {
  const [verPass, setVerPass] = useState(false);
  const [localError, setLocalError] = useState<string | null>(null);

  const handlePress = () => {
    if (!correo.trim() || !password.trim()) {
      setLocalError('Completa correo y contraseña.');
      return;
    }
    setLocalError(null);
    onLogin();
  };

  return (
    <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
      {/* Encabezado de marca */}
      <View style={styles.brandHeader}>
        <Text style={styles.brandIcon}>⛽</Text>
        <View style={{ marginLeft: 12 }}>
          <Text style={styles.appName}>Órdenes de Combustible</Text>
          <Text style={styles.secureLabel}>ACCESO SEGURO</Text>
        </View>
      </View>

      {/* Tarjeta Hero (bg_tarjeta_hero.xml) */}
      <View style={styles.heroCard}>
        <Text style={styles.heroOverline}>ADMINISTRACIÓN MÓVIL</Text>
        <Text style={styles.heroTitle}>{'Control de combustible.\nDesde tu móvil.'}</Text>
        <Text style={styles.heroSubtitle}>Asigna, consulta y gestiona en un solo lugar.</Text>
      </View>

      <Text style={styles.loginTitle}>Inicia sesión</Text>

      <TextInput
        style={styles.input}
        placeholder="Correo electrónico"
        placeholderTextColor={colors.textoSecundario}
        keyboardType="email-address"
        autoCapitalize="none"
        value={correo}
        onChangeText={setCorreo}
      />

      <View style={styles.passWrapper}>
        <TextInput
          style={styles.passInput}
          placeholder="Contraseña"
          placeholderTextColor={colors.textoSecundario}
          secureTextEntry={!verPass}
          value={password}
          onChangeText={setPassword}
        />
        <TouchableOpacity onPress={() => setVerPass(!verPass)} style={styles.eyeBtn}>
          <Text style={styles.eyeText}>{verPass ? '🙈' : '👁'}</Text>
        </TouchableOpacity>
      </View>

      <Text style={styles.forgotText}>¿Olvidaste tu contraseña?</Text>

      <Text style={styles.exclusiveText}>Acceso exclusivo para personal autorizado.</Text>

      <TouchableOpacity
        style={[styles.btnIngresar, loading && { opacity: 0.7 }]}
        onPress={handlePress}
        disabled={loading}
      >
        <Text style={styles.btnIngresarText}>Ingresar</Text>
      </TouchableOpacity>

      {loading && <ActivityIndicator color={colors.marca} style={{ marginTop: 16 }} />}

      {localError && <Text style={styles.errorText}>{localError}</Text>}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scroll: { flexGrow: 1, backgroundColor: colors.fondo, padding: 20 },
  brandHeader: { flexDirection: 'row', alignItems: 'center', marginTop: 12 },
  brandIcon: { fontSize: 24 },
  appName: { fontSize: 21, fontWeight: '700', color: colors.textoPrincipal },
  secureLabel: { fontSize: 10, fontWeight: '700', color: colors.marca },
  heroCard: {
    backgroundColor: colors.marcaOscuro,
    borderRadius: 16,
    padding: 24,
    marginTop: 20,
  },
  heroOverline: { color: '#D8C9F4', fontWeight: '700', fontSize: 11 },
  heroTitle: { color: colors.blanco, fontSize: 26, fontWeight: '700', lineHeight: 32, marginTop: 10 },
  heroSubtitle: { color: '#E5DAF7', fontSize: 14, marginTop: 10 },
  loginTitle: { fontSize: 24, fontWeight: '700', color: colors.textoPrincipal, marginTop: 24 },
  input: {
    backgroundColor: colors.blanco,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    padding: 14,
    fontSize: 15,
    color: colors.textoPrincipal,
    marginTop: 16,
  },
  passWrapper: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.blanco,
    borderWidth: 1,
    borderColor: colors.borde,
    borderRadius: 12,
    marginTop: 12,
  },
  passInput: {
    flex: 1,
    padding: 14,
    fontSize: 15,
    color: colors.textoPrincipal,
  },
  eyeBtn: { paddingHorizontal: 14 },
  eyeText: { fontSize: 18 },
  forgotText: {
    alignSelf: 'flex-end',
    color: colors.marca,
    fontWeight: '700',
    fontSize: 14,
    marginTop: 12,
  },
  exclusiveText: {
    textAlign: 'center',
    color: colors.textoSecundario,
    fontSize: 12,
    marginTop: 16,
  },
  btnIngresar: {
    backgroundColor: colors.marca,
    height: 52,
    borderRadius: 28,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 20,
  },
  btnIngresarText: { color: colors.blanco, fontSize: 15, fontWeight: '700' },
  errorText: { color: colors.vencida, textAlign: 'center', marginTop: 12, fontSize: 13 },
});