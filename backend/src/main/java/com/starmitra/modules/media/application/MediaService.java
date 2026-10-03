package com.starmitra.modules.media.application;

import org.springframework.stereotype.Service;

/**
 * M04 media lifecycle orchestration (foundation — storage adapter wiring only;
 * full asset state machine lands with M04 feature implementation).
 * Enforces: metadata → pre-signed upload → complete → async process → delivery.
 */
@Service
public class MediaService {
    // foundation: contract surface only — implementation with the M04 feature slice
}
