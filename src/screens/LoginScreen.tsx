import React, { useState } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  StyleSheet,
  ScrollView,
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { colors } from '../theme/colors';

interface Props {
  correo: string;
  setCorreo: (v: string) => void;
  password: string;
  setPassword: (v: string) => void;
  onLogin: () => void;
  loading: boolean;
  sessionMessage?: string | null;
}

export default function LoginScreen({
  correo,
  setCorreo,
  password,
  setPassword,
  onLogin,
  loading,
  sessionMessage,
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
    <KeyboardAvoidingView
      style={{ flex: 1, backgroundColor: colors.fondo }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <ScrollView contentContainerStyle={styles.scroll} keyboardShouldPersistTaps="handled">
        <View style={styles.brandHeader}>
          <Text style={styles.brandIcon}>⛽</Text>
          <View style={{ marginLeft: 12 }}>
            <Text style={styles.appName}>Órdenes de Combustible</Text>
            <Text style={styles.secureLabel}>Acceso seguro</Text>
          </View>
        </View>

        <View style={styles.heroCard}>
          <Text style={styles.heroOverline}>ADMINISTRACIÓN MÓVIL</Text>
          <Text style={styles.heroTitle}>{'Control de combustible.\nDesde tu móvil.'}</Text>
          <Text style={styles.heroSubtitle}>Asigna, consulta y gestiona en un solo lugar.</Text>
        </View>

        {sessionMessage && (
          <View style={styles.sessionBanner}>
            <Text style={styles.sessionBannerText}>{sessionMessage}</Text>
          </View>
        )}

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

        <TouchableOpacity
          style={[styles.btnIngresar, loading && { opacity: 0.7 }]}
          onPress={handlePress}
          disabled={loading}
        >
          <Text style={styles.btnIngresarText}>Ingresar</Text>
        </TouchableOpacity>

        {loading && <ActivityIndicator color={colors.marca} style={{ marginTop: 16 }} />}

        {localError && <Text style={styles.errorText}>{localError}</Text>}

        <Text style={styles.exclusiveText}>Acceso exclusivo para personal autorizado.</Text>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  scroll: { flexGrow: 1, backgroundColor: colors.fondo, padding: 24 },
  brandHeader: { flexDirection: 'row', alignItems: 'center', marginTop: 16 },
  brandIcon: { fontSize: 26 },
  appName: { fontSize: 20, fontWeight: '700', color: colors.textoPrincipal },
  secureLabel: { fontSize: 11, fontWeight: '700', color: colors.marca },
  heroCard: {
    backgroundColor: colors.marcaOscuro,
    borderRadius: 16,
    padding: 20,
    marginTop: 24,
  },
  heroOverline: { color: colors.marcaClaro, fontWeight: '700', fontSize: 11 },
  heroTitle: { color: colors.blanco, fontSize: 22, fontWeight: '700', lineHeight: 28, marginTop: 8 },
  heroSubtitle: { color: colors.marcaClaro, fontSize: 13, marginTop: 8 },
  sessionBanner: {
    backgroundColor: colors.vencidaFondo,
    borderRadius: 12,
    padding: 12,
    marginTop: 16,
  },
  sessionBannerText: { color: colors.vencida, fontSize: 13, fontWeight: '600', textAlign: 'center' },
  loginTitle: { fontSize: 22, fontWeight: '700', color: colors.textoPrincipal, marginTop: 24 },
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
    fontSize: 13,
    marginTop: 12,
  },
  btnIngresar: {
    backgroundColor: colors.marca,
    height: 50,
    borderRadius: 16,
    justifyContent: 'center',
    alignItems: 'center',
    marginTop: 24,
  },
  btnIngresarText: { color: colors.blanco, fontSize: 15, fontWeight: '700' },
  errorText: { color: colors.vencida, textAlign: 'center', marginTop: 12, fontSize: 13 },
  exclusiveText: {
    textAlign: 'center',
    color: colors.textoSecundario,
    fontSize: 12,
    marginTop: 24,
  },
});