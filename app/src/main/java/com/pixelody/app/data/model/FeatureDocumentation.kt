package com.pixelody.app.data.model

/**
 * Data structures for Pixelody's contextual documentation and feature guide system.
 * Provides concise, audiophile-grade explanations for specialized audio concepts
 * and non-standard music player mechanics.
 */
data class DocSection(
    val title: String,
    val body: String,
    val keyPoints: List<String> = emptyList()
)

data class FeatureDocTopic(
    val id: String,
    val title: String,
    val category: String,
    val summary: String,
    val sections: List<DocSection>,
    val proTip: String? = null,
    val relatedTopicIds: List<String> = emptyList()
)

object FeatureDocumentationRepository {

    private val topics = listOf(
        FeatureDocTopic(
            id = "harmonigrains",
            title = "Harmonigrains & Trajectory Arc",
            category = "Harmonic Flow",
            summary = "A continuous vector sparkline visualizing key modulation, energy contour, and acoustic cohesion across any playlist or playback queue.",
            sections = listOf(
                DocSection(
                    title = "What Are Harmonigrains?",
                    body = "Harmonigrains decomposes your song order into harmonic nodes (grains). Instead of treating a queue as a flat list of tracks, each grain illustrates the musical key (using the 12-hour Camelot wheel) and relative energy level of that track in sequence.",
                    keyPoints = listOf(
                        "Cyan / Sky nodes represent Major keys (bright, uplifting tonality).",
                        "Violet / Lavender nodes represent Minor keys (deep, introspective tonality).",
                        "Vertical position represents track acoustic energy (0.0 to 1.0 dynamic range).",
                        "Horizontal axis represents playlist progression from beginning to end."
                    )
                ),
                DocSection(
                    title = "Flow Cohesion & Compatibility",
                    body = "Smooth curves indicate consonant harmonic transitions between adjacent songs. Sharp vertical spikes or color clashes signify dramatic genre pivots or key jumps that may sound jarring during DJ mixing.",
                    keyPoints = listOf(
                        "High Cohesion (>85%): Songs blend smoothly via adjacent Camelot keys (±1 step).",
                        "Energy Arc: Tracks build up and wind down naturally without erratic volume or tempo leaps."
                    )
                )
            ),
            proTip = "Hold down the Harmonigrains strip or tap the '?' button anytime to inspect harmonic telemetry. Use the Trajectory Sculptor in the Queue to reshape energy curves.",
            relatedTopicIds = listOf("harmonic_lens", "trajectory_sculptor", "smart_flow")
        ),
        FeatureDocTopic(
            id = "harmonic_lens",
            title = "Harmonic Camelot Digging Lens",
            category = "Digging & Curation",
            summary = "A circular circle-of-fifths digging filter allowing instant discovery of tracks that are harmonically compatible with whatever is currently playing.",
            sections = listOf(
                DocSection(
                    title = "The Camelot System (1A - 12B)",
                    body = "Professional DJs use Camelot notation to simplify music theory. Numbers (1-12) denote clock face positions on the Circle of Fifths. 'A' represents Minor keys (e.g., 8A is A Minor), and 'B' represents Major keys (e.g., 8B is C Major).",
                    keyPoints = listOf(
                        "Exact Match (Same Key): Zero key dissonance (e.g., 8A to 8A).",
                        "Harmonic Step (±1 Hour): Natural modulation (e.g., 8A moves cleanly to 7A or 9A).",
                        "Mode Switch (A <-> B): Relative Major/Minor shift (e.g., 8A A-Minor to 8B C-Major)."
                    )
                ),
                DocSection(
                    title = "Advanced Transition Modes",
                    body = "Select different digging modes on the lens to discover songs suited for specific dancefloor and listening dynamics:",
                    keyPoints = listOf(
                        "Harmonic: Safe, consonant pairing within ±1 step.",
                        "Energy Boost (+2 Hours): Lifts crowd energy with an intentional upward pitch modulation.",
                        "Sunset Drift (-2 Hours): Softens tension, lowering musical energy for late-night transitions."
                    )
                )
            ),
            proTip = "Tap any hour on the Camelot wheel to filter your entire library by that key instantly. Tap RESET to return to standard library view.",
            relatedTopicIds = listOf("harmonigrains", "smart_flow")
        ),
        FeatureDocTopic(
            id = "daily_capsule",
            title = "Daily Sonic Capsule",
            category = "Acoustic Intelligence",
            summary = "A daily generative recap analyzing your listening habits, acoustic DNA, lossless fidelity ratios, and listening streaks.",
            sections = listOf(
                DocSection(
                    title = "Sonic Archetypes & Audio DNA",
                    body = "Pixelody analyzes your daily listening telemetry—including dynamic range, audio formats, listening durations, and time-of-day moods—to classify your sonic archetype.",
                    keyPoints = listOf(
                        "Hi-Res Audiophile: High ratio of 24-bit / FLAC playback with bit-perfect signal routing.",
                        "Deep Flow Architect: Extended continuous playback sessions with seamless harmonic transitions.",
                        "Vinyl Purist: Heavy rotation of full album plays, analog warm masters, and linear tracking.",
                        "Electronic Explorer: High-energy BPM sessions, club transition curves, and active queue sculpting."
                    )
                ),
                DocSection(
                    title = "Lossless Ratio & Streaks",
                    body = "The capsule highlights the percentage of your session delivered in bit-perfect lossless quality and tracks daily listening streaks to encourage deep active listening.",
                    keyPoints = listOf(
                        "Tap the capsule to launch the daily sound check curated from your top highlights.",
                        "Tap 'Explore Sonic Timeline' to inspect historical capsules and weekly trends."
                    )
                )
            ),
            proTip = "Long-press the capsule card or click the corner '?' button to review this guide anytime.",
            relatedTopicIds = listOf("hires_lossless", "smart_crates")
        ),
        FeatureDocTopic(
            id = "smart_flow",
            title = "Smart Flow & Harmonic Pairing",
            category = "Playback Engine",
            summary = "Intelligent playback automation that curates consecutive tracks using circle-of-fifths key compatibility, tempo proximity, and auto-DJ crossfades.",
            sections = listOf(
                DocSection(
                    title = "Beyond Random Shuffle",
                    body = "Traditional shuffle plays random songs regardless of key clashes or jarring mood shifts. Smart Flow analyzes acoustic timbre, Camelot keys, and energy levels to weave your library into an uninterrupted DJ mix.",
                    keyPoints = listOf(
                        "Prevents jarring musical key clashes during track changes.",
                        "Maintains smooth tempo acceleration and deceleration curves.",
                        "Applies intelligent DJ crossfades tailored to song intro/outro phrase structures."
                    )
                )
            ),
            proTip = "Activate Smart Flow from the Quick Start tiles or the Flow Shuffle icon in the Now Playing player to let the engine steer your listening session.",
            relatedTopicIds = listOf("harmonigrains", "harmonic_lens", "trajectory_sculptor")
        ),
        FeatureDocTopic(
            id = "trajectory_sculptor",
            title = "Queue Trajectory Sculptor",
            category = "DJ & Queue Control",
            summary = "An interactive contour canvas in the Queue allowing you to draw or apply energy envelopes (Peak Wave, Slow Build, Chill Descent) to mathematically reorder tracks.",
            sections = listOf(
                DocSection(
                    title = "Sculpting Your Listening Session",
                    body = "Instead of manually dragging songs one-by-one to create a mix, the Trajectory Sculptor applies mathematical envelopes to your queue:",
                    keyPoints = listOf(
                        "Peak Wave: Builds energy from the start, reaches a high-octane peak in the middle, and winds down.",
                        "Slow Build: Gradual crescendo from quiet ambient tracks into peak energetic anthems.",
                        "Chill Descent: Starts with upbeat tracks and gently transitions into calming acoustic tones."
                    )
                ),
                DocSection(
                    title = "Applying the Curve",
                    body = "Select a preset curve or sketch your own envelope, then tap [APPLY CONTOUR]. The engine re-orders your queue tracks to match the target energy slope while honoring key compatibility.",
                    keyPoints = listOf(
                        "Preserves currently playing song without interrupting audio.",
                        "Instant undo available via the Queue Undo pill if you wish to revert."
                    )
                )
            ),
            proTip = "Combine the Trajectory Sculptor with Flow Profiles in the Queue for ultimate set-building control.",
            relatedTopicIds = listOf("harmonigrains", "smart_flow")
        ),
        FeatureDocTopic(
            id = "smart_crates",
            title = "Smart Crates & Rule Engine",
            category = "Library & Physicality",
            summary = "Tactile 9-slot vinyl crates with dynamic metadata rules that automatically organize your music collection.",
            sections = listOf(
                DocSection(
                    title = "Physicality Meets Smart Automation",
                    body = "Inspired by vintage vinyl record digging, each crate holds a curated set of tracks or collections. Smart Crates update automatically as new tracks are added to your library.",
                    keyPoints = listOf(
                        "24-Bit FLAC Master: Auto-populates all bit-perfect high-resolution tracks.",
                        "Harmonic Camelot Flow: Auto-populates songs fitting a continuous harmonic progression.",
                        "Vinyl Analog Lounge: Auto-populates albums with vintage acoustic characteristics.",
                        "Heavy Rotation Digs: Gathers your most frequently played tracks across sessions."
                    )
                ),
                DocSection(
                    title = "Custom Rule Builder",
                    body = "Tap '+ Smart Crate' to create custom crates with precise filter predicates including format, sample rate, year, genre, artist, and play count.",
                    keyPoints = listOf(
                        "Crates can be renamed, reordered, or deleted via the crate action menu.",
                        "Long-press any crate to access options or view documentation."
                    )
                )
            ),
            proTip = "Tap any crate to open its 9-slot grid. Tap any slot to play immediately from that position.",
            relatedTopicIds = listOf("daily_capsule", "hires_lossless")
        ),
        FeatureDocTopic(
            id = "varispeed",
            title = "Analog Tape Varispeed & Pitch Lock",
            category = "Playback & DSP",
            summary = "Dual-mode tempo manipulation providing authentic analog tape pitch shift or pristine time-stretch pitch lock.",
            sections = listOf(
                DocSection(
                    title = "Tape Varispeed vs Pitch Lock",
                    body = "Traditional digital players speed up music with mathematical time-stretching, keeping pitch artificial. Pixelody offers true analog tape varispeed behavior as well:",
                    keyPoints = listOf(
                        "Pitch Locked (Time-Stretch): Playback speed adjusts from 0.5x to 2.0x while key remains constant. Perfect for study, podcasting, or transcribing solos.",
                        "Tape Varispeed (Analog): Speed and pitch shift together, exactly like turning the pitch slider on a Technics 1200 turntable or reel-to-reel tape machine. Speeding up makes audio brighter and higher pitched; slowing down adds deep warmth."
                    )
                )
            ),
            proTip = "Access Varispeed from the Now Playing menu (Speed & Pitch). Use the fine-tune buttons (±0.05x) for delicate adjustments.",
            relatedTopicIds = listOf("loudness_normalization")
        ),
        FeatureDocTopic(
            id = "loudness_normalization",
            title = "EBU R128 Loudness Normalization",
            category = "Playback & DSP",
            summary = "Automatic dynamic gain targeting preventing jarring volume jumps between tracks while preserving bit-perfect audiophile dynamics.",
            sections = listOf(
                DocSection(
                    title = "Understanding LUFS & Headroom",
                    body = "Different recording eras and mastering engineers master songs at drastically different volume levels. Loudness normalization measures perceived loudness across entire tracks:",
                    keyPoints = listOf(
                        "Streaming Standard (-14 LUFS): Equalizes all tracks to modern streaming targets for uniform everyday listening.",
                        "Audiophile Dynamic (-18 LUFS): Leaves extra headroom (+4 dB) for maximum dynamic range, ideal for classical, jazz, and uncompressed acoustic masters.",
                        "Off (Bit-Perfect): Completely bypasses digital attenuation for purist DAC output."
                    )
                )
            ),
            proTip = "Normalizer gain is computed per-track and applied smoothly without clipping or pumping compression.",
            relatedTopicIds = listOf("hires_lossless", "varispeed")
        ),
        FeatureDocTopic(
            id = "hires_lossless",
            title = "Hi-Res Lossless & Pure Signal Chain",
            category = "Audiophile Engine",
            summary = "Direct bit-perfect audio delivery preserving 24-bit / 192kHz sample rates and original studio mastering dynamics.",
            sections = listOf(
                DocSection(
                    title = "Bit-Perfect Pipeline",
                    body = "Pixelody identifies uncompressed FLAC, WAV, ALAC, and DSD containers. When connected to compatible USB DACs or headphone amplifiers, audio streams directly without Android OS downsampling.",
                    keyPoints = listOf(
                        "Zero resampler distortion or frequency roll-off.",
                        "Full dynamic range expression with zero peak limiting.",
                        "Visual indicators badge tracks as LOSSLESS or 24-BIT."
                    )
                )
            ),
            proTip = "Check the Horizon status card on Home to see your active DAC connection and current resolution.",
            relatedTopicIds = listOf("loudness_normalization", "daily_capsule")
        ),
        FeatureDocTopic(
            id = "turntable",
            title = "Analog Vinyl Turntable",
            category = "Acoustic Console",
            summary = "Tactile direct-drive vinyl platter simulation with rotational inertia, pitch-locked 33⅓ and 45 RPM speeds, and physical needle-drop groove response.",
            sections = listOf(
                DocSection(
                    title = "Rotational Mechanics & Speeds",
                    body = "The virtual platter models real direct-drive turntable inertia. Toggle between 33⅓ RPM LP albums and 45 RPM maxi-singles with immediate pitch scaling and tactile acoustic inertia.",
                    keyPoints = listOf(
                        "33⅓ RPM: Standard full-length vinyl playback speed with warm analog harmonic response.",
                        "45 RPM: High-velocity maxi-single speed with enhanced transient definition.",
                        "Scrubbing & Needle-Drop: Touch and drag the vinyl record directly to scratch or jump position."
                    )
                ),
                DocSection(
                    title = "Needle-Drop Haptic Response",
                    body = "Dropping or scrubbing the tonearm stimulates the linear haptic motor to mimic physical diamond stylus vibrations across micro-grooves.",
                    keyPoints = listOf(
                        "Subtle groove lead-in click on playback start.",
                        "Linear tactile response matching rotational velocity."
                    )
                )
            ),
            proTip = "Toggle Analog Tape Saturation inside the turntable deck for authentic tube pre-amp harmonic warmth.",
            relatedTopicIds = listOf("cassette_deck", "audio_haptics", "vinyl_vault")
        ),
        FeatureDocTopic(
            id = "stems_isolator",
            title = "4-Stem Audio Isolator & Mixer",
            category = "Mixing & DSP",
            summary = "Real-time acoustic stem separation isolating Vocals, Drums, Bass, and Melody with individual solo, mute, gain, stereo panning, and filter sweeps.",
            sections = listOf(
                DocSection(
                    title = "4-Channel Acoustic Separation",
                    body = "Decomposes any stereo master into 4 distinct frequency/spectral bands: Lead & Backing Vocals, Percussion & Drums, Sub & Mid Bass, and Melodic Instruments.",
                    keyPoints = listOf(
                        "Vocals: Isolates center-channel vocal harmonics; ideal for instant acapella extraction.",
                        "Drums: Captures transient kick, snare, and hi-hat elements.",
                        "Bass: Isolates fundamental sub-bass and basslines below 250Hz.",
                        "Instruments: Extracts guitars, synths, keys, and atmospheric background elements."
                    )
                ),
                DocSection(
                    title = "Performance Mixing Controls",
                    body = "Each stem features an independent mute, solo switch, ±12dB gain trim, stereo pan slider, and 3-band surgical equalization with low/high-pass resonance.",
                    keyPoints = listOf(
                        "Quick Presets: Instant setups for Acapella Focus, Dub Instrumental, Drum & Bass, and Vocal Cut.",
                        "Crossfader: Smoothly blend between custom stem mixes and full master audio."
                    )
                )
            ),
            proTip = "Soloing the Vocal stem while engaging the Phosphor Oscilloscope creates an incredible visual phase analysis of lead vocal harmonics.",
            relatedTopicIds = listOf("mastering_rack", "oscilloscope", "auto_dj")
        ),
        FeatureDocTopic(
            id = "mastering_rack",
            title = "Studio Mastering & Parametric EQ",
            category = "Acoustic Console",
            summary = "Precision 10-band mastering DSP rack featuring surgical parametric equalization, analog tape drive, stereo field expansion, and brickwall peak limiting.",
            sections = listOf(
                DocSection(
                    title = "10-Band Parametric Equalization",
                    body = "Ten center frequencies calibrated for acoustic clarity: Sub-Bass (31Hz), Bass (62Hz), Punch (125Hz), Warmth (250Hz), Body (500Hz), Presence (1kHz), Definition (2kHz), Attack (4kHz), Sheen (8kHz), and Air (16kHz).",
                    keyPoints = listOf(
                        "Zero-latency minimum phase filter topology.",
                        "Linear ±12dB boost and cut per band with smooth Q-factor curve interpolation."
                    )
                ),
                DocSection(
                    title = "Mastering Dynamics & Enhancers",
                    body = "Features analog tape saturation drive to add subtle odd/even harmonic density, a stereo width imaging expander, and a lookahead brickwall ceiling limiter to prevent inter-sample clipping.",
                    keyPoints = listOf(
                        "Drive: Adds rich analog harmonic overtone warmth.",
                        "Stereo Width: Expands side-channel imaging without introducing mono phase cancellation.",
                        "Limiter: Protects DAC output from digital overload and distortion."
                    )
                )
            ),
            proTip = "Switch between Global EQ and Track-Specific Master profiles to tailor acoustic curves for different genres or output headphones.",
            relatedTopicIds = listOf("loudness_normalization", "stems_isolator", "spatial_chamber")
        ),
        FeatureDocTopic(
            id = "cassette_deck",
            title = "Magnetic Cassette Tape Deck",
            category = "Analog Studio",
            summary = "Authentic magnetic tape simulation modeling Type I Ferric, Type II Chrome, and Type IV Metal formulations with analog hysteresis, wow & flutter, and head saturation.",
            sections = listOf(
                DocSection(
                    title = "Magnetic Tape Formulations",
                    body = "Choose between vintage Type I Normal Bias (warm lo-fi saturation), Type II High Bias Chrome (crisp transients with extended high end), and Type IV Metal (wide dynamic range and transparent tape compression).",
                    keyPoints = listOf(
                        "Type I Ferric: Classic vintage cassette warmth with gentle high-frequency roll-off.",
                        "Type II Chrome: Studio-grade dynamic response with enhanced presence.",
                        "Type IV Metal: Audiophile headroom with exceptional transient transparency."
                    )
                ),
                DocSection(
                    title = "Transport Physics & Mechanical Wow",
                    body = "Simulates mechanical capstan wobble, motor drift, tape head azimuth alignment, and high-frequency soft tape clipping.",
                    keyPoints = listOf(
                        "Adjustable Wow & Flutter rate from pristine studio deck to vintage boombox.",
                        "Analog head saturation compression that gently glues rhythmic elements together."
                    )
                )
            ),
            proTip = "Engage 15 ips (inches per second) mode for high-fidelity studio reel-to-reel response with minimal tape noise.",
            relatedTopicIds = listOf("turntable", "mastering_rack", "varispeed")
        ),
        FeatureDocTopic(
            id = "spatial_chamber",
            title = "Spatial Acoustic Chamber",
            category = "Spatial Audio",
            summary = "Binaural room acoustic simulator modeling virtual chamber dimensions, reverberant decay time (RT60), wall absorption coefficients, and speaker positioning.",
            sections = listOf(
                DocSection(
                    title = "Room Geometry & Wall Damping",
                    body = "Simulates physical room dimensions from small intimate studio booths (40 m³) to cavernous cathedral halls (1,200 m³), with material absorption damping from concrete to velvet.",
                    keyPoints = listOf(
                        "RT60 Decay: Controls how long reverberant sound energy takes to decay by 60dB.",
                        "Wall Damping: Simulates acoustic acoustic absorption across high and mid frequencies."
                    )
                ),
                DocSection(
                    title = "Virtual Speaker Positioning",
                    body = "Position virtual left and right studio monitors across azimuth angles (15° to 90°) and listener distance to sculpt your binaural soundstage.",
                    keyPoints = listOf(
                        "Head-Related Transfer Function (HRTF) crossfeed for natural headphone listening.",
                        "Eliminates severe headphone stereo separation fatigue."
                    )
                )
            ),
            proTip = "Select the 'Audiophile Studio' preset for transparent early reflections that improve headphone imaging without muddying the mix.",
            relatedTopicIds = listOf("mastering_rack", "oscilloscope", "hires_lossless")
        ),
        FeatureDocTopic(
            id = "oscilloscope",
            title = "Phosphor CRT Oscilloscope Lab",
            category = "Audio Analysis",
            summary = "Hardware vector oscilloscope displaying real-time stereo phase correlation, Lissajous X-Y figures, cathode-ray tube phosphor decay, and 3D beam rotation.",
            sections = listOf(
                DocSection(
                    title = "Stereo Phase Correlation & Lissajous",
                    body = "Maps Left and Right audio channels to orthogonal deflection plates. A vertical line represents mono audio; circular or elliptical figures reveal wide stereo imaging; out-of-phase audio flattens horizontally.",
                    keyPoints = listOf(
                        "Vertical Line: Perfect mono correlation (+1.0 phase).",
                        "Circular Ring: Balanced stereo soundstage with 90° phase difference.",
                        "Horizontal Spread: Negative phase correlation indicating potential phase cancellation."
                    )
                ),
                DocSection(
                    title = "CRT Phosphor Types & Persistence",
                    body = "Select between classic P31 Green (fast transient response), P7 Long-Persistence Amber (slow radar-style phosphor trail), or modern Blue Phosphor.",
                    keyPoints = listOf(
                        "P31 Green: High-speed lab standard for transient waveform inspection.",
                        "P7 Amber: Long-afterglow phosphor creating artistic visual motion trails."
                    )
                )
            ),
            proTip = "Drag the 3D perspective to view the oscilloscope beam in isometric 3D space while music plays.",
            relatedTopicIds = listOf("stems_isolator", "spatial_chamber")
        ),
        FeatureDocTopic(
            id = "auto_dj",
            title = "Auto-DJ & Transition Engine",
            category = "Harmonic Flow",
            summary = "Intelligent automatic transition system analyzing beat grids, phrase boundaries, and Camelot keys to blend tracks seamlessly in real time.",
            sections = listOf(
                DocSection(
                    title = "Phrase Boundary Matching",
                    body = "Instead of abruptly cutting or fading mid-bar, Auto-DJ detects musical phrases (16-bar or 32-bar sections) and triggers transitions on downbeats.",
                    keyPoints = listOf(
                        "BPM Synchronization: Gently warps incoming tempo to match outgoing track.",
                        "Downbeat Alignment: Launches incoming track on the 1-beat of a new musical phrase."
                    )
                ),
                DocSection(
                    title = "Harmonic Crossfade Curves",
                    body = "Applies Equal-Power (3dB dip compensation) or Slow Exponential crossfade curves, automatically ducking the bass of the outgoing track to prevent low-end mud.",
                    keyPoints = listOf(
                        "Equal-Power: Maintains consistent perceived volume throughout the transition.",
                        "Bass Swap: Automatically ducks outgoing 40-200Hz frequencies when the incoming bass drops."
                    )
                )
            ),
            proTip = "Enable 'Harmonic Key Lock' so the engine automatically pitches incoming tracks to adjacent Camelot steps for seamless harmonic mixes.",
            relatedTopicIds = listOf("smart_flow", "harmonigrains", "trajectory_sculptor")
        ),
        FeatureDocTopic(
            id = "vinyl_vault",
            title = "Vinyl Crate & Extended Metadata",
            category = "Digging & Curation",
            summary = "A tactile digital record crate for flipping through high-resolution vinyl artwork, technical recording provenance, and deep tag metadata.",
            sections = listOf(
                DocSection(
                    title = "Tactile Sleeve Browsing",
                    body = "Swipe horizontally through your current collection or playlist with realistic album sleeve tilt and tactile feedback.",
                    keyPoints = listOf(
                        "Direct visual record browsing matching the physical digging experience.",
                        "Displays total tracks and crate duration at a glance."
                    )
                ),
                DocSection(
                    title = "Technical Metadata Inspection",
                    body = "Inspect audio stream bit depth, sample rate, codec profile, container bitrate, embedded replaygain tags, and release year.",
                    keyPoints = listOf(
                        "Reveals true encoding parameters (e.g. FLAC 96kHz/24-bit).",
                        "Displays integrated LUFS loudness profiles."
                    )
                )
            ),
            proTip = "Tap any sleeve in the vault to jump playback directly to that track without leaving the full player.",
            relatedTopicIds = listOf("turntable", "smart_crates", "hires_lossless")
        ),
        FeatureDocTopic(
            id = "audio_haptics",
            title = "Tactile Audio Haptics Lab",
            category = "Hardware Transducer",
            summary = "Advanced sub-bass extraction and tactile resonance synthesis that translates audio rhythms directly into your device's linear vibration actuator.",
            sections = listOf(
                DocSection(
                    title = "Sub-Bass Transient Extraction",
                    body = "A specialized 24dB/oct low-pass filter isolates kicks, bass drops, and rhythmic impacts below 80Hz.",
                    keyPoints = listOf(
                        "Physical tactile rhythm synced perfectly with playback.",
                        "Zero delay audio-to-haptic synthesis."
                    )
                ),
                DocSection(
                    title = "Resonance & Haptic Modulation",
                    body = "Adjust transducer intensity, haptic envelope attack/decay, and frequency tuning to feel the physical groove in your hand.",
                    keyPoints = listOf(
                        "Subtle, battery-efficient vibration pulses.",
                        "Simulates the physical feeling of standing near club subwoofers."
                    )
                )
            ),
            proTip = "Turn on Tactile Haptics during turntable scrubbing to feel the physical resistance of the vinyl grooves.",
            relatedTopicIds = listOf("turntable", "cassette_deck", "stems_isolator")
        )
    )

    fun getTopic(topicId: String): FeatureDocTopic? {
        return topics.firstOrNull { it.id.equals(topicId, ignoreCase = true) }
            ?: topics.firstOrNull { it.id.contains(topicId, ignoreCase = true) }
    }

    fun getAllTopics(): List<FeatureDocTopic> = topics
}
