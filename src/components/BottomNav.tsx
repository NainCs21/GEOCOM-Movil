import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet, Platform } from 'react-native';
import { colors } from '../theme/colors';

interface BottomNavProps {
  currentScreen: string;
  setScreen: (screen: string) => void;
  rol?: string;
}

export default function BottomNav({ currentScreen, setScreen, rol }: BottomNavProps) {
  // En Android el conductor navega desde el header superior y no usa barra inferior
  if (rol === 'conductor') return null;

  const adminItems = [
    { id: 'home', icon: '🏠', label: 'Inicio' },
    { id: 'solicitudes', icon: '📩', label: 'Solicitudes' },
    { id: 'ordenes', icon: '📋', label: 'Órdenes' },
    { id: 'catalogos', icon: '📑', label: 'Catálogos' },
    { id: 'mas', icon: '☰', label: 'Más' },
  ];

  const supervisorItems = [
    { id: 'home', icon: '🏠', label: 'Inicio' },
    { id: 'ordenes', icon: '📋', label: 'Órdenes' },
    { id: 'notificaciones', icon: '🔔', label: 'Avisos' },
    { id: 'reportes', icon: '📊', label: 'Reportes' },
    { id: 'perfil', icon: '👤', label: 'Perfil' },
  ];

  const items = rol === 'admin' ? adminItems : supervisorItems;

  return (
    <View style={styles.bar}>
      {items.map((item) => {
        const isActive = currentScreen === item.id;
        return (
          <TouchableOpacity key={item.id} style={styles.item} onPress={() => setScreen(item.id)}>
            <Text style={[styles.icon, isActive && styles.activeIcon]}>{item.icon}</Text>
            <Text style={[styles.label, isActive && styles.activeLabel]}>{item.label}</Text>
          </TouchableOpacity>
        );
      })}
    </View>
  );
}

const styles = StyleSheet.create({
  bar: {
    flexDirection: 'row',
    backgroundColor: colors.blanco,
    borderTopWidth: 1,
    borderTopColor: colors.borde,
    paddingTop: 10,
    paddingBottom: Platform.OS === 'ios' ? 24 : 10,
  },
  item: { flex: 1, alignItems: 'center' },
  icon: { fontSize: 20, color: colors.textoSecundario },
  activeIcon: { transform: [{ scale: 1.12 }] },
  label: { fontSize: 11, color: colors.textoSecundario, marginTop: 4, fontWeight: '600' },
  activeLabel: { color: colors.marca, fontWeight: '700' },
});