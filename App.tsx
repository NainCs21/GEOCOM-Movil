import React, { useState, useEffect, useCallback } from 'react';
import {
  SafeAreaView,
  StatusBar,
  StyleSheet,
  View,
  Alert,
  AppState,
} from 'react-native';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Notifications from 'expo-notifications';
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
import InstruccionEvidenciaModal from './src/screens/InstruccionEvidenciaModal';

import BottomNav from './src/components/BottomNav';
import {
  marcarNotificacionLeida,
  sincronizarNotificaciones,
} from './src/utils/notificaciones';
import {
  actualizarBadgeSistema,
  pedirPermisoNotificaciones,
} from './src/utils/notificador';
import {
  cancelarSincronizacionBackground,
  programarSincronizacionBackground,
} from './src/utils/backgroundNotifTask';

export default function App() {
  const [token, setToken] = useState<string | null>(null);
  const [user, setUser] = useState<any>(null);
  const [currentScreen, setCurrentScreen] = useState('home');
  const [loading, setLoading] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const [selectedOrdenId, setSelectedOrdenId] = useState<number | null>(null);
  const [selectedOrden, setSelectedOrden] = useState<any | null>(null);

  const [nuevaSolicitudVisible, setNuevaSolicitudVisible] = useState(false);
  const [solicitudParaAsignar, setSolicitudParaAsignar] = useState<any | null>(null);
  const [escanearQrVisible, setEscanearQrVisible] = useState(false);

  const [instruccionVisible, setInstruccionVisible] = useState(false);
  const [instruccionData, setInstruccionData] = useState<{ id: number; tipo: string } | null>(null);
  const [fotoCapturada, setFotoCapturada] = useState<any | null>(null);

  const [correo, setCorreo] = useState('cmendoza@empresa.com');
  const [password, setPassword] = useState('password');

  const [unreadCount, setUnreadCount] = useState(0);
  const [sessionMessage, setSessionMessage] = useState<string | null>(null);

  // Navegar al tocar una notificación nativa de iPhone
  const manejarToqueNotificacion = useCallback(
    async (data: any, currentToken: string | null, currentUser: any) => {
      if (!data || !currentToken || !currentUser || currentUser.rol === 'conductor') return;

      const notifId = data.notif_id as string | undefined;
      const destino = data.notif_destino as string | undefined;
      const ordenId = data.notif_orden_id as number | undefined;

      if (notifId) {
        const updated = await marcarNotificacionLeida(currentUser.id, notifId);
        setUnreadCount(updated.filter((n) => !n.leida).length);
      }

      if (destino === 'orden' && ordenId) {
        setSelectedOrden(null);
        setSelectedOrdenId(ordenId);
      } else if (destino === 'solicitudes') {
        setSelectedOrdenId(null);
        setCurrentScreen('solicitudes');
      } else if (destino === 'notificaciones') {
        setSelectedOrdenId(null);
        setCurrentScreen('notificaciones');
      }
    },
    []
  );

  // Persistencia de sesión en arranque en frío + validación GET /yo
  useEffect(() => {
    AsyncStorage.getItem('api_movil_sesion').then((val) => {
      if (val) {
        const parsed = JSON.parse(val);
        setToken(parsed.token);
        setUser(parsed.usuario);

        fetch(`${API_URL}/yo`, {
          headers: { Accept: 'application/json', Authorization: `Bearer ${parsed.token}` },
        })
          .then((r) => {
            if (r.status === 401) throw new Error('sesion_expirada');
            return r.json();
          })
          .then((data) => {
            if (data.id) {
              const updatedUser = { ...parsed.usuario, ...data };
              setUser(updatedUser);
              AsyncStorage.setItem(
                'api_movil_sesion',
                JSON.stringify({ token: parsed.token, usuario: updatedUser })
              );
            }
          })
          .catch((e) => {
            if (e.message === 'sesion_expirada') {
              handleLogoutLocal('Tu sesión expiró. Inicia sesión de nuevo.');
            }
          });
      }
    });
  }, []);

  // Permisos nativos, tarea background, AppState y polling cada 10s para Admin/Supervisor
  useEffect(() => {
    if (!token || !user || user.rol === 'conductor') {
      cancelarSincronizacionBackground();
      return;
    }

    pedirPermisoNotificaciones();
    programarSincronizacionBackground();

    const ejecutarSync = async () => {
      try {
        const result = await sincronizarNotificaciones(token, user.id);
        setUnreadCount(result.filter((n) => !n.leida).length);
      } catch {}
    };

    ejecutarSync();
    const interval = setInterval(ejecutarSync, 10000);

    // Sincronizar también cada vez que el usuario regresa la app al frente
    const appStateSub = AppState.addEventListener('change', (nextState) => {
      if (nextState === 'active') {
        ejecutarSync();
      }
    });

    // Listener cuando el usuario toca el banner nativo de iOS
    const responseSub = Notifications.addNotificationResponseReceivedListener((response) => {
      const data = response.notification.request.content.data;
      manejarToqueNotificacion(data, token, user);
    });

    // Revisar si la app se abrió desde una notificación en frío
    Notifications.getLastNotificationResponseAsync().then((response) => {
      if (response) {
        const data = response.notification.request.content.data;
        manejarToqueNotificacion(data, token, user);
      }
    });

    return () => {
      clearInterval(interval);
      appStateSub.remove();
      responseSub.remove();
    };
  }, [token, user, manejarToqueNotificacion]);

  const triggerRefresh = () => setRefreshKey((k) => k + 1);

  const handleLogin = async () => {
    setLoading(true);
    setSessionMessage(null);
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
      AsyncStorage.setItem(
        'api_movil_sesion',
        JSON.stringify({ token: data.token, usuario: data.usuario })
      );
      setCurrentScreen('home');
      setSelectedOrdenId(null);
      setSelectedOrden(null);
    } catch (err: any) {
      Alert.alert('Error', err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleLogoutLocal = (msg?: string) => {
    setToken(null);
    setUser(null);
    setCurrentScreen('home');
    setSelectedOrdenId(null);
    setSelectedOrden(null);
    setUnreadCount(0);
    actualizarBadgeSistema(0);
    cancelarSincronizacionBackground();
    AsyncStorage.removeItem('api_movil_sesion');
    if (msg) setSessionMessage(msg);
  };

  const handleLogout = async () => {
    if (token) {
      fetch(`${API_URL}/logout`, {
        method: 'POST',
        headers: { Accept: 'application/json', Authorization: `Bearer ${token}` },
      }).catch(() => {});
    }
    handleLogoutLocal();
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
            user={user}
            showBack={!isSupervisor}
            onBack={() => setCurrentScreen(user?.rol === 'admin' ? 'mas' : 'home')}
            onOpenOrder={(id) => {
              setSelectedOrden(null);
              setSelectedOrdenId(id);
            }}
            onOpenSolicitudes={() => setCurrentScreen('solicitudes')}
            onNotificacionesUpdate={(c) => setUnreadCount(c)}
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
    <SafeAreaView style={[styles.container, token ? { backgroundColor: colors.marca } : undefined]}>
      <StatusBar barStyle={token ? 'light-content' : 'dark-content'} />

      {!token ? (
        <LoginScreen
          correo={correo}
          setCorreo={setCorreo}
          password={password}
          setPassword={setPassword}
          onLogin={handleLogin}
          loading={loading}
          sessionMessage={sessionMessage}
        />
      ) : (
        <View style={[styles.flex, { backgroundColor: colors.fondo }]}>
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
                onAbrirInstruccion={(detalleId, tipo) => {
                  setInstruccionData({ id: detalleId, tipo });
                  setInstruccionVisible(true);
                }}
                fotoCapturadaPendiente={fotoCapturada}
                limpiarFotoCapturada={() => setFotoCapturada(null)}
              />
            ) : (
              renderVistaSecundaria() || (
                <>
                  {currentScreen === 'home' && (
                    <HomeScreen
                      key={`home-${refreshKey}`}
                      user={user}
                      unreadCount={unreadCount}
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
                      onBack={() => setCurrentScreen('home')}
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
                      onBack={() => setCurrentScreen('home')}
                    />
                  )}
                  {currentScreen === 'catalogos' && (
                    <CatalogosScreen
                      token={token}
                      user={user}
                      onBack={() => setCurrentScreen('home')}
                    />
                  )}
                  {currentScreen === 'mas' && (
                    <MasScreen unreadCount={unreadCount} setScreen={setCurrentScreen} />
                  )}
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

          {instruccionData && (
            <InstruccionEvidenciaModal
              visible={instruccionVisible}
              detalleId={instruccionData.id}
              tipo={instruccionData.tipo}
              onClose={() => {
                setInstruccionVisible(false);
                setInstruccionData(null);
              }}
              onContinue={(foto) => {
                setFotoCapturada(foto);
                setInstruccionVisible(false);
                setInstruccionData(null);
              }}
            />
          )}

          {!ocultarBottomNav && (
            <BottomNav
              currentScreen={currentScreen}
              setScreen={setCurrentScreen}
              rol={user?.rol}
              unreadCount={unreadCount}
            />
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