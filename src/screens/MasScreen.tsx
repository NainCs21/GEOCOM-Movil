import React from 'react';
import { View, Text, StyleSheet, TouchableOpacity, ScrollView } from 'react-native';
import { colors } from '../theme/colors';

interface Props {
  setScreen: (s: string) => void;
}

export default function MasScreen({ setScreen }: Props) {
  const opciones = [
    {
      id: 'notificaciones',
      icon: '🔔',
      titulo: 'Notificaciones',
      subtitulo: 'Avisos de órdenes asignadas, completadas y vencidas.',
    },
    {
      id: 'reportes',
      icon: '📊',
      titulo: 'Reportes',
      subtitulo: 'Consumo por conductor y por gasolinera.',
    },
    {
      id: 'caja_chica',
      icon: '💵',
      titulo: 'Caja chica',
      subtitulo: 'Registro de entregas para trabajos foráneos.',
    },
    {
      id: 'perfil',
      icon: '👤',
      titulo: 'Mi perfil',
      subtitulo: 'Tus datos y cierre de sesión.',
    },
  ];

  return (
    <View style={styles.flex}>
      <View style={styles.topHeader}>
        <Text style={styles.topHeaderTitle}>Más opciones</Text>
      </View>

      <ScrollView contentContainerStyle={{ padding: 20 }}>
        {opciones.map((op) => (
          <TouchableOpacity
            key={op.id}
            style={styles.card}
            onPress={() => setScreen(op.id)}
          >
            <View style={styles.iconBox}>
              <Text style={{ fontSize: 18 }}>{op.icon}</Text>
            </View>
            <View style={{ flex: 1, marginLeft: 12 }}>
              <Text style={styles.titulo}>{op.titulo}</Text>
              <Text style={styles.subtitulo}>{op.subtitulo}</Text>
            </View>
          </TouchableOpacity>
        ))}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  topHeader: {
    backgroundColor: colors.marca,
    paddingHorizontal: 20,
    paddingVertical: 16,
  },
  topHeaderTitle: { fontSize: 18, fontWeight: '700', color: colors.blanco },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 12,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 12,
  },
  iconBox: {
    width: 36,
    height: 36,
    borderRadius: 10,
    backgroundColor: colors.marcaClaro,
    justifyContent: 'center',
    alignItems: 'center',
  },
  titulo: { fontSize: 15, fontWeight: '700', color: colors.textoPrincipal },
  subtitulo: { fontSize: 13, color: colors.textoSecundario, marginTop: 2 },
});