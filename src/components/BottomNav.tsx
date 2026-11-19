import React from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { colors } from '../theme/colors';

interface Props {
  currentScreen: string;
  setScreen: (s: string) => void;
  rol?: string;
  unreadCount?: number;
}

export default function BottomNav({ currentScreen, setScreen, rol, unreadCount = 0 }: Props) {
  const tabsAdmin = [
    { id: 'home', icon: '🏠', label: 'Inicio' },
    { id: 'solicitudes', icon: '📋', label: 'Solicitudes' },
    { id: 'ordenes', icon: '⛽', label: 'Órdenes' },
    { id: 'catalogos', icon: '🗂️', label: 'Catálogos' },
    { id: 'mas', icon: '☰', label: 'Más', badge: true },
  ];

  const tabsSupervisor = [
    { id: 'home', icon: '🏠', label: 'Inicio' },
    { id: 'ordenes', icon: '⛽', label: 'Órdenes' },
    { id: 'notificaciones', icon: '🔔', label: 'Avisos', badge: true },
    { id: 'reportes', icon: '📊', label: 'Reportes' },
    { id: 'perfil', icon: '👤', label: 'Perfil' },
  ];

  const tabs = rol === 'admin' ? tabsAdmin : tabsSupervisor;

  return (
    <View style={styles.bar}>
      {tabs.map((t) => {
        const active = currentScreen === t.id;
        return (
          <TouchableOpacity
            key={t.id}
            style={styles.tab}
            onPress={() => setScreen(t.id)}
          >
            <View>
              <Text style={{ fontSize: 18, opacity: active ? 1 : 0.6 }}>{t.icon}</Text>
              {t.badge && unreadCount > 0 && (
                <View style={styles.badge}>
                  <Text style={styles.badgeText}>
                    {unreadCount > 99 ? '99+' : unreadCount}
                  </Text>
                </View>
              )}
            </View>
            <Text style={[styles.label, active && styles.labelActive]}>{t.label}</Text>
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
    paddingVertical: 8,
  },
  tab: { flex: 1, alignItems: 'center', justifyContent: 'center' },
  label: { fontSize: 11, color: colors.textoSecundario, marginTop: 2 },
  labelActive: { color: colors.marca, fontWeight: '700' },
  badge: {
    position: 'absolute',
    top: -4,
    right: -10,
    backgroundColor: colors.vencida,
    borderRadius: 10,
    minWidth: 18,
    height: 18,
    paddingHorizontal: 4,
    justifyContent: 'center',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.blanco,
  },
  badgeText: { color: colors.blanco, fontSize: 9, fontWeight: '700' },
});