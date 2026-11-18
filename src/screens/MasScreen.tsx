import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, ScrollView } from 'react-native';
import { colors } from '../theme/colors';

interface Props {
  setScreen: (screen: string) => void;
}

export default function MasScreen({ setScreen }: Props) {
  return (
    <View style={styles.flex}>
      <Text style={styles.title}>Más opciones</Text>
      
      <ScrollView contentContainerStyle={styles.container}>
        <TouchableOpacity style={styles.card} onPress={() => setScreen('notificaciones')}>
          <Text style={styles.cardTitle}>Notificaciones</Text>
          <Text style={styles.cardSub}>Avisos y alertas del sistema</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.card} onPress={() => setScreen('reportes')}>
          <Text style={styles.cardTitle}>Reportes</Text>
          <Text style={styles.cardSub}>Métricas de consumo e indicadores</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.card} onPress={() => setScreen('caja_chica')}>
          <Text style={styles.cardTitle}>Caja Chica</Text>
          <Text style={styles.cardSub}>Registro de viáticos y trabajos foráneos</Text>
        </TouchableOpacity>

        <TouchableOpacity style={styles.card} onPress={() => setScreen('perfil')}>
          <Text style={styles.cardTitle}>Mi Perfil</Text>
          <Text style={styles.cardSub}>Datos de cuenta y configuración</Text>
        </TouchableOpacity>
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  flex: { flex: 1, backgroundColor: colors.fondo },
  title: { fontSize: 24, fontWeight: '700', color: colors.textoPrincipal, padding: 20 },
  container: { paddingHorizontal: 20 },
  card: {
    backgroundColor: colors.blanco,
    borderRadius: 16,
    padding: 18,
    borderWidth: 1,
    borderColor: colors.borde,
    marginBottom: 12,
  },
  cardTitle: { fontSize: 16, fontWeight: '700', color: colors.textoPrincipal },
  cardSub: { fontSize: 13, color: colors.textoSecundario, marginTop: 4 },
});