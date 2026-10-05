import { Redirect } from 'expo-router';
import { ActivityIndicator, Image, StyleSheet, Text, View } from 'react-native';
import { useSession } from '../lib/session';
import { C, TAGLINE } from '../lib/theme';

/** Splash / auth gate — routes to login or the tab shell. */
export default function Index() {
  const { session, booted } = useSession();
  if (!booted) {
    return (
      <View style={styles.splash}>
        <Image source={require('../../assets/brand/StarMitra-Mark.png')}
          style={styles.mark} />
        <Text style={styles.name}>StarMitra</Text>
        <Text style={styles.tagline}>{TAGLINE}</Text>
        <ActivityIndicator color={C.gold} style={{ marginTop: 24 }} />
      </View>
    );
  }
  return <Redirect href={session ? '/(tabs)' : '/login'} />;
}

const styles = StyleSheet.create({
  splash: { flex: 1, backgroundColor: C.navy, alignItems: 'center',
    justifyContent: 'center' },
  mark: { width: 120, height: 120, borderRadius: 60 },
  name: { color: '#fff', fontSize: 30, fontWeight: '800', marginTop: 18 },
  tagline: { color: C.gold, fontSize: 14, letterSpacing: 1.5, marginTop: 6 },
});
