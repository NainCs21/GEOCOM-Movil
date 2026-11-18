import React, { useState } from 'react';
import {
  SafeAreaView,
  StatusBar,
  StyleSheet,
  View,
  Alert,
} from 'react-native';
import { API_URL } from './src/api/config';
import { colors } from './src/theme/colors';

import LoginScreen from './src/screens/LoginScreen';
import HomeScreen from './src/screens/HomeScreen';
import OrdenesScreen from './src/screens/OrdenesScreen';
import SolicitudesScreen from './src/screens/SolicitudesScreen';
import CatalogosScreen from './src/screens/CatalogosScreen';
import CajaChicaScreen from './src/screens/CajaChicaScreen';
import PerfilScreen from './src/screens/PerfilScreen';
import OrdenDetalleScreen from './src/screens/OrdenDetalleScreen';
import MasScreen from './src/screens/MasScreen';
import ReportesScreen from './src/screens/ReportesScreen';
import HistorialScreen from './src/screens/HistorialScreen';
import NotificacionesScreen from './src/screens/NotificacionesScreen';

import NuevaSolicitudModal from './src/screens/NuevaSolicitudModal';
import AsignarMontoModal from './src/screens/AsignarMontoModal';
import EscanearQrModal from './src/screens/EscanearQrModal';

import BottomNav from './src/components/BottomNav';

export default function App() {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<any>(null);
  const [currentScreen, setCurrentScreen] = useState('home');
  const [loading, setLoading] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  // Detalle de orden
  const [selectedOrdenId, setSelectedOrdenId] = useState<number | null>(null);
  const [selectedOrden, setSelectedOrden] = useState<any | null>(null);

  // Modales
  const [nuevaSolicitudVisible, setNuevaSolicitudVisible] = useState(false);
  const [solicitudParaAsignar, setSolicitudParaAsignar] = useState<any | null>(null);
  const [escanearQrVisible, setEscanearQrVisible] = useState(false);

  const [correo, setCorreo] = useState('cmendoza@empresa.com');
  const [password, setPassword] = useState('password');

  const triggerRefresh = () => setRefreshKey((k) => k + 1);

  const handleLogin = async () => {
    setLoading(true);
    try {
      const res = await fetch(`${API_URL}/login`, {
        method: 'POST',
        headers: { Accept: 'application/json', 'Content-Type': 'application/json' },
        body: JSON.stringify({
          correo: correo.trim(),
          contrasena: password,
          nombre_dispositivo: 'iPhone-iOS',
        }),
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.mensaje || data.message || 'Correo o contraseña incorrectos.');

      setToken(data.token);
      setUser(data.usuario);
      setCurrentScreen('home');
      setSelectedOrdenId(null);
      setSelectedOrden(null);
    } catch (err: any) {
      Alert.alert('Error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogout = () => {
    setToken(null);
    setUser(null);
    setCurrentScreen('home');
    setSelectedOrdenId(null);
    setSelectedOrden(null);
  };

  const isSupervisor = user?.rol === 'contabilidad';
  const isConductor = user?.rol === 'conductor';

  const renderVistaSecundaria = () => {
    switch (currentScreen) {
      case 'caja_chica':
        return (
          <CajaChicaScreen
            token={token!}
            user={user}
            onBack={() => setCurrentScreen(user?.rol === 'admin' ? 'mas' : 'home')}
          />
        );
      case 'reportes':
        return (
          <ReportesScreen
            token={token!}
            showBack={!isSupervisor}
            onBack={() => setCurrentScreen(user?.rol === 'admin' ? 'mas' : 'home')}
          />
        );
      case 'notificaciones':
        return (
          <NotificacionesScreen
            token={token!}
            showBack={!isSupervisor}
            onBack={() => setCurrentScreen(user?.rol === 'admin' ? 'mas' : 'home')}
            onOpenOrder={(id) => {
              setSelectedOrden(null);
              setSelectedOrdenId(id);
            }}
          />
        );
      case 'historial':
        return (
          <HistorialScreen
            token={token!}
            onBack={() => setCurrentScreen('home')}
            onOpenOrder={(id) => {
              setSelectedOrden(null);
              setSelectedOrdenId(id);
            }}
          />
        );
      default:
        return null;
    }
  };

  const ocultarBottomNav =
    !!selectedOrdenId ||
    isConductor ||
    currentScreen === 'caja_chica' ||
    currentScreen === 'historial' ||
    (currentScreen === 'reportes' && !isSupervisor) ||
    (currentScreen === 'notificaciones' && !isSupervisor);

  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" />

      {!token ? (
        <LoginScreen
          correo={correo}
          setCorreo={setCorreo}
          password={password}
          setPassword={setPassword}
          onLogin={handleLogin}
          loading={loading}
        />
      ) : (
        <View style={styles.flex}>
          <View style={styles.flex}>
            {selectedOrdenId ? (
              <OrdenDetalleScreen
                ordenId={selectedOrdenId}
                ordenData={selectedOrden}
                token={token}
                user={user}
                onBack={() => {
                  setSelectedOrdenId(null);
                  setSelectedOrden(null);
                  triggerRefresh();
                }}
                onOpenQr={() => setEscanearQrVisible(true)}
              />
            ) : (
              renderVistaSecundaria() || (
                <>
                  {currentScreen === 'home' && (
                    <HomeScreen
                      key={`home-${refreshKey}`}
                      user={user}
                      setScreen={setCurrentScreen}
                      token={token}
                      onOpenOrder={(id) => {
                        setSelectedOrden(null);
                        setSelectedOrdenId(id);
                      }}
                      onOpenNuevaSolicitud={() => setNuevaSolicitudVisible(true)}
                    />
                  )}
                  {currentScreen === 'solicitudes' && (
                    <SolicitudesScreen
                      key={`sol-${refreshKey}`}
                      token={token}
                      user={user}
                      onAsignar={(sol) => setSolicitudParaAsignar(sol)}
                    />
                  )}
                  {currentScreen === 'ordenes' && (
                    <OrdenesScreen
                      key={`ord-${refreshKey}`}
                      token={token}
                      onOpenOrder={(id) => {
                        setSelectedOrden(null);
                        setSelectedOrdenId(id);
                      }}
                    />
                  )}
                  {currentScreen === 'catalogos' && (
                    <CatalogosScreen token={token} user={user} />
                  )}
                  {currentScreen === 'mas' && <MasScreen setScreen={setCurrentScreen} />}
                  {currentScreen === 'perfil' && (
                    <PerfilScreen
                      user={user}
                      onLogout={handleLogout}
                      showBack={!isSupervisor}
                      onBack={() => setCurrentScreen(isConductor ? 'home' : 'mas')}
                    />
                  )}
                </>
              )
            )}
          </View>

          <NuevaSolicitudModal
            visible={nuevaSolicitudVisible}
            onClose={() => setNuevaSolicitudVisible(false)}
            token={token}
            onSuccess={triggerRefresh}
          />

          <AsignarMontoModal
            solicitud={solicitudParaAsignar}
            onClose={() => setSolicitudParaAsignar(null)}
            token={token}
            onSuccess={triggerRefresh}
          />

          <EscanearQrModal
            visible={escanearQrVisible}
            onClose={() => setEscanearQrVisible(false)}
            token={token}
            onScanned={(orden) => {
              setSelectedOrden(orden);
              setSelectedOrdenId(orden.id);
              triggerRefresh();
            }}
          />

          {!ocultarBottomNav && (
            <BottomNav currentScreen={currentScreen} setScreen={setCurrentScreen} rol={user?.rol} />
          )}
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.fondo },
  flex: { flex: 1 },
});