/**
 * M22 Pricing & Entitlements — application layer.
 * Exposes EntitlementContract (limit/feature questions) and UserPlanContract
 * (default FREE assignment) to other modules; PaymentProviderContract is a
 * deferred seam — no payment implementation exists in MVP.
 */
package com.starmitra.modules.pricing.application;
