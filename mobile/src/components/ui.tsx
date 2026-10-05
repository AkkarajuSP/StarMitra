import React from 'react';
import {
  ActivityIndicator, Image, Pressable, StyleSheet, Text, TextInput,
  View, type TextInputProps,
} from 'react-native';
import { C } from '../lib/theme';
import { ApiError } from '../lib/api';

/* ---------- logo (official assets; surface picks the correct variant) ---------- */
export function Mark({ size = 40, surface = 'light' }:
  { size?: number; surface?: 'light' | 'dark' }) {
  return (
    <Image
      source={surface === 'dark'
        ? require('../../assets/brand/mobileAppIconLight.png')
        : require('../../assets/brand/mobileAppIconDark.png')}
      style={{ width: size, height: size }} resizeMode="contain"
      accessibilityLabel="StarMitra logo" />
  );
}
export function Lockup({ width = 180, surface = 'light' }:
  { width?: number; surface?: 'light' | 'dark' }) {
  return (
    <Image
      source={surface === 'dark'
        ? require('../../assets/brand/sMLogoMainLight.png')
        : require('../../assets/brand/sMLogoMainDark.png')}
      style={{ width, height: width * 0.78 }} resizeMode="contain"
      accessibilityLabel="StarMitra — Passion to Perform" />
  );
}

/* ---------- button ---------- */
export function Btn({ title, onPress, kind = 'primary', disabled, loading }:
  { title: string; onPress: () => void;
    kind?: 'primary' | 'ghost' | 'danger' | 'gold';
    disabled?: boolean; loading?: boolean }) {
  return (
    <Pressable
      accessibilityRole="button"
      disabled={disabled || loading}
      onPress={onPress}
      style={({ pressed }) => [
        styles.btn,
        kind === 'primary' && styles.btnPrimary,
        kind === 'gold' && styles.btnGold,
        kind === 'ghost' && styles.btnGhost,
        kind === 'danger' && styles.btnDanger,
        pressed && { opacity: 0.85 },
        (disabled || loading) && { opacity: 0.55 },
      ]}>
      {loading
        ? <ActivityIndicator color={kind === 'ghost' ? C.purple : '#fff'} />
        : <Text style={[styles.btnText,
            kind === 'ghost' && { color: C.purple },
            kind === 'gold' && { color: C.navy }]}>{title}</Text>}
    </Pressable>
  );
}

/* ---------- input ---------- */
export function Field({ label, ...rest }: TextInputProps & { label?: string }) {
  return (
    <View style={{ marginBottom: 14 }}>
      {label ? <Text style={styles.fieldLabel}>{label}</Text> : null}
      <TextInput
        placeholderTextColor={C.muted}
        style={styles.input}
        {...rest} />
    </View>
  );
}

/** Segmented OTP entry. */
export function OtpRow({ value, onChange, length = 6 }:
  { value: string; onChange: (v: string) => void; length?: number }) {
  return (
    <View accessibilityLabel="One-time code" style={{ marginBottom: 14 }}>
      <Text style={styles.fieldLabel}>One-time code</Text>
      <TextInput
        value={value}
        onChangeText={(t) => onChange(t.replace(/\D/g, '').slice(0, length))}
        keyboardType="number-pad"
        autoComplete="sms-otp"
        textContentType="oneTimeCode"
        maxLength={length}
        placeholder="••••••"
        placeholderTextColor={C.muted}
        style={[styles.input, { letterSpacing: 10, textAlign: 'center',
          fontSize: 22, fontWeight: '700', color: C.navy }]}
      />
    </View>
  );
}

/* ---------- badge ---------- */
const BADGE_TONES: Record<string, { bg: string; fg: string }> = {
  open: { bg: '#E7F6EE', fg: C.ok },
  active: { bg: '#E7F6EE', fg: C.ok },
  submitted: { bg: C.lavender, fg: C.purple },
  published: { bg: C.lavender, fg: C.purple },
  advanced: { bg: '#E7F6EE', fg: C.ok },
  draft: { bg: '#F2F4F7', fg: C.muted },
  configured: { bg: '#F2F4F7', fg: C.muted },
  frozen: { bg: C.lavender, fg: C.violet },
  closed: { bg: '#FDEDED', fg: C.coral },
  complete: { bg: '#FDF3E0', fg: C.warn },
  finalized: { bg: '#FDF3E0', fg: C.warn },
  eliminated: { bg: '#FDEDED', fg: C.coral },
};
export function Badge({ v }: { v: string }) {
  const t = BADGE_TONES[v.toLowerCase()] ?? { bg: '#F2F4F7', fg: C.muted };
  return (
    <View style={[styles.badge, { backgroundColor: t.bg }]}>
      <Text style={[styles.badgeText, { color: t.fg }]}>{v.replace(/_/g, ' ')}</Text>
    </View>
  );
}

/* ---------- card ---------- */
export function Card({ children, style }: { children: React.ReactNode; style?: object }) {
  return <View style={[styles.card, style]}>{children}</View>;
}

/* ---------- states ---------- */
export function Loading() {
  return (
    <View style={styles.stateWrap} accessibilityLiveRegion="polite">
      <ActivityIndicator size="large" color={C.purple} />
      <Text style={styles.stateText}>Loading…</Text>
    </View>
  );
}
export function ErrorBox({ error }: { error: unknown }) {
  const msg = error instanceof ApiError
    ? (error.status === 401 || error.status === 403
        ? 'Not authorized for this.'
        : error.message)
    : 'Something went wrong.';
  return (
    <View style={[styles.card, styles.errorCard]} accessibilityLiveRegion="assertive">
      <Text style={styles.errorText}>{msg}</Text>
    </View>
  );
}
export function Empty({ title = 'Nothing to show', detail }: { title?: string; detail?: string }) {
  return (
    <View style={styles.stateWrap}>
      <Mark size={44} />
      <Text style={styles.stateTitle}>{title}</Text>
      {detail ? <Text style={styles.stateText}>{detail}</Text> : null}
    </View>
  );
}

/* ---------- avatar ---------- */
export function Avatar({ name, size = 40 }: { name?: string; size?: number }) {
  const initials = (name ?? '?').split(/\s+/).map((p) => p[0]).slice(0, 2)
    .join('').toUpperCase();
  return (
    <View style={[styles.avatar, { width: size, height: size,
      borderRadius: size / 2 }]}>
      <Text style={[styles.avatarText, { fontSize: size * 0.36 }]}>{initials}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  btn: { paddingVertical: 13, paddingHorizontal: 18, borderRadius: 12,
    alignItems: 'center', justifyContent: 'center', minHeight: 48 },
  btnPrimary: { backgroundColor: C.purple },
  btnGold: { backgroundColor: C.gold },
  btnGhost: { backgroundColor: 'transparent', borderWidth: 1.5, borderColor: C.purple },
  btnDanger: { backgroundColor: C.coral },
  btnText: { color: '#fff', fontWeight: '700', fontSize: 15 },
  fieldLabel: { fontSize: 13, fontWeight: '600', color: C.ink, marginBottom: 6 },
  input: { borderWidth: 1.5, borderColor: C.line, borderRadius: 12,
    backgroundColor: '#fff', paddingHorizontal: 14, paddingVertical: 12,
    fontSize: 15, color: C.ink, minHeight: 48 },
  badge: { paddingHorizontal: 10, paddingVertical: 4, borderRadius: 999 },
  badgeText: { fontSize: 11.5, fontWeight: '700' },
  card: { backgroundColor: '#fff', borderRadius: 16, padding: 16,
    borderWidth: 1, borderColor: C.line,
    shadowColor: C.navy, shadowOpacity: 0.06, shadowRadius: 10,
    shadowOffset: { width: 0, height: 3 }, elevation: 2 },
  stateWrap: { alignItems: 'center', justifyContent: 'center',
    paddingVertical: 40, gap: 10 },
  stateTitle: { fontSize: 16, fontWeight: '700', color: C.ink },
  stateText: { fontSize: 13, color: C.muted, textAlign: 'center' },
  errorCard: { borderColor: C.coral, backgroundColor: '#FDEDED' },
  errorText: { color: C.coral, fontWeight: '600' },
  avatar: { backgroundColor: C.lavender, alignItems: 'center',
    justifyContent: 'center' },
  avatarText: { color: C.violet, fontWeight: '800' },
});
