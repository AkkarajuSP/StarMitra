import { useState } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, StyleSheet, Text, View } from 'react-native';
import { router } from 'expo-router';
import { Btn, ErrorBox, Field, Lockup, OtpRow } from '../components/ui';
import { ApiError } from '../lib/api';
import { requestOtp as reqOtp, verifyOtp as verOtp } from '../lib/endpoints';
import { useSession } from '../lib/session';
import { C, TAGLINE } from '../lib/theme';

/** M01 OTP sign-in — backend authoritative; UAT OTP prints to backend console. */
export default function Login() {
  const { signIn } = useSession();
  const [identifier, setIdentifier] = useState('');
  const [otp, setOtp] = useState('');
  const [sent, setSent] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [resent, setResent] = useState(false);

  const submit = async () => {
    setBusy(true); setError(null);
    try {
      if (!sent) {
        await reqOtp(identifier.trim());
        setSent(true); setResent(false);
      } else {
        const s = await verOtp(identifier.trim(), otp);
        await signIn({
          userId: s.user.id, email: s.user.email,
          roles: s.user.systemRoles, accessToken: s.accessToken,
        });
        router.replace('/(tabs)');
      }
    } catch (e) {
      setError(e instanceof ApiError ? e
        : new ApiError(0, 'UNKNOWN', sent
          ? "That code didn't work — check the digits."
          : "We couldn't send a code. Check the email and try again."));
    } finally {
      setBusy(false);
    }
  };

  const resend = async () => {
    setBusy(true); setError(null);
    try { await reqOtp(identifier.trim()); setResent(true); setOtp(''); }
    catch (e) { setError(e); } finally { setBusy(false); }
  };

  return (
    <KeyboardAvoidingView style={{ flex: 1 }}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
      <ScrollView style={styles.page} contentContainerStyle={{ flexGrow: 1 }}
        keyboardShouldPersistTaps="handled">
        <View style={styles.hero}>
          <Lockup width={190} />
          <Text style={styles.sub}>Discover. Create. Perform. Connect.</Text>
        </View>
        <View style={styles.card}>
          <Text style={styles.h1}>Welcome to StarMitra</Text>
          <Text style={styles.p}>Enter your email — we'll send a one-time code.</Text>
          {error != null && <ErrorBox error={error} />}
          {sent && (
            <View style={styles.sent}>
              <Text style={styles.sentText}>
                Code sent to <Text style={{ fontWeight: '700' }}>{identifier}</Text>
                {resent ? ' (resent)' : ''}
              </Text>
            </View>
          )}
          <Field label="Email" value={identifier} autoCapitalize="none"
            autoComplete="email" keyboardType="email-address"
            onChangeText={setIdentifier} placeholder="you@starmitra.dev" />
          {sent && <OtpRow value={otp} onChange={setOtp} />}
          <Btn loading={busy}
            disabled={!identifier.includes('@') || (sent && otp.length < 6)}
            title={sent ? 'Verify & Sign In' : 'Send OTP'}
            onPress={submit} />
          {sent && (
            <View style={{ marginTop: 10 }}>
              <Btn kind="ghost" title="Resend code" onPress={resend} disabled={busy} />
            </View>
          )}
          <Text style={styles.note}>
            UAT: your code is printed to the backend console — look for the
            UAT OTP banner.
          </Text>
        </View>
      </ScrollView>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  page: { flex: 1, backgroundColor: C.navy },
  hero: { alignItems: 'center', paddingTop: 72, paddingBottom: 26,
    backgroundColor: C.navy },
  sub: { color: C.lavender, fontSize: 13, letterSpacing: 0.4, marginTop: 4 },
  card: { flex: 1, backgroundColor: '#fff', borderTopLeftRadius: 28,
    borderTopRightRadius: 28, padding: 24, paddingTop: 28 },
  h1: { fontSize: 22, fontWeight: '800', color: C.ink },
  p: { fontSize: 14, color: C.muted, marginTop: 4, marginBottom: 16 },
  sent: { backgroundColor: '#E7F6EE', borderRadius: 10, padding: 10,
    marginBottom: 12 },
  sentText: { color: C.ok, fontSize: 13 },
  note: { fontSize: 12, color: C.muted, marginTop: 18, textAlign: 'center' },
});
