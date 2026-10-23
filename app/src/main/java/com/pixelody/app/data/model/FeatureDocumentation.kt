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
            summary = "See how key and energy change through your playlist or queue.",
            sections = listOf(
                DocSection(
                    title = "Reading Harmonigrains",
                    body = "Each point represents a track's Camelot key and relative energy, in queue order.",
                    keyPoints = listOf(
                        "Cyan / Sky: major keys.",
                        "Violet / Lavender: minor keys.",
                        "Height: relative track energy, from 0.0 to 1.0.",
                        "Left to right: playlist order."
                    )
                ),
                DocSection(
                    title = "Flow Cohesion & Compatibility",
                    body = "Look for changes in key and energy between songs. A sudden jump may be worth listening to before using it in a mix.",
                    keyPoints = listOf(
                        "High Cohesion (>85%): many neighboring tracks have adjacent Camelot keys (±1 step).",
                        "Energy Arc: the rise and fall of track energy through the queue."
                    )
                )
            ),
            proTip = "Hold the strip or tap '?' for details. Use Trajectory Sculptor in Queue to change the energy curve.",
            relatedTopicIds = listOf("harmonic_lens", "trajectory_sculptor", "smart_flow")
        ),
        FeatureDocTopic(
            id = "harmonic_lens",
            title = "Harmonic Camelot Digging Lens",
            category = "Digging & Curation",
            summary = "Find tracks by key, or look for a match to the current song.",
            sections = listOf(
                DocSection(
                    title = "The Camelot System (1A - 12B)",
                    body = "Camelot labels keys from 1 to 12. A means minor and B means major: 8A is A minor; 8B is C major.",
                    keyPoints = listOf(
                        "Exact Match: the same key, such as 8A to 8A.",
                        "Harmonic Step (±1): neighboring keys, such as 8A to 7A or 9A.",
                        "Mode Switch (A <-> B): relative major/minor, such as 8A to 8B."
                    )
                ),
                DocSection(
                    title = "Advanced Transition Modes",
                    body = "Choose a mode to explore different key relationships:",
                    keyPoints = listOf(
                        "Harmonic: keys within ±1 step.",
                        "Energy Boost: look two steps ahead on the wheel.",
                        "Sunset Drift: look two steps back on the wheel."
                    )
                )
            ),
            proTip = "Tap a key to filter the library. Tap RESET to show everything again.",
            relatedTopicIds = listOf("harmonigrains", "smart_flow")
        ),
        FeatureDocTopic(
            id = "daily_capsule",
            title = "Daily Sonic Capsule",
            category = "Acoustic Intelligence",
            summary = "A daily recap of what you've played, listening time, and source formats.",
            sections = listOf(
                DocSection(
                    title = "Sonic Archetypes & Audio DNA",
                    body = "The recap groups your day using listening time, track formats, and other available listening data.",
                    keyPoints = listOf(
                        "Hi-Res Audiophile: a high share of 24-bit or FLAC tracks.",
                        "Deep Flow Architect: longer sessions with compatible track keys.",
                        "Vinyl Purist: more full-album listening.",
                        "Electronic Explorer: higher-tempo sessions and more queue changes."
                    )
                ),
                DocSection(
                    title = "Lossless Ratio & Streaks",
                    body = "See the share of lossless source files and your listening streak. Source format doesn't verify the output path.",
                    keyPoints = listOf(
                        "Tap the capsule for your daily sound check.",
                        "Tap 'Explore Sonic Timeline' for earlier days and weekly trends."
                    )
                )
            ),
            proTip = "Hold the capsule card or tap '?' to open this guide.",
            relatedTopicIds = listOf("hires_lossless", "smart_crates")
        ),
        FeatureDocTopic(
            id = "smart_flow",
            title = "Flow & Harmonic Pairing",
            category = "Playback Engine",
            summary = "Plan the next tracks using key, tempo, and transition settings.",
            sections = listOf(
                DocSection(
                    title = "Beyond Random Shuffle",
                    body = "Flow uses available key and energy information to choose an order from your library.",
                    keyPoints = listOf(
                        "Favors compatible keys.",
                        "Uses tempo when planning changes.",
                        "Uses the available DJ transition settings."
                    )
                )
            ),
            proTip = "Start Flow from Quick Start or the Flow Shuffle control in Now Playing.",
            relatedTopicIds = listOf("harmonigrains", "harmonic_lens", "trajectory_sculptor")
        ),
        FeatureDocTopic(
            id = "trajectory_sculptor",
            title = "Queue Trajectory Sculptor",
            category = "DJ & Queue Control",
            summary = "Reorder the queue around an energy curve you choose or draw.",
            sections = listOf(
                DocSection(
                    title = "Sculpting Your Listening Session",
                    body = "Choose a shape for the session:",
                    keyPoints = listOf(
                        "Peak Wave: build to a peak, then wind down.",
                        "Slow Build: start quiet and gradually raise the energy.",
                        "Chill Descent: start upbeat, then settle down."
                    )
                ),
                DocSection(
                    title = "Applying the Curve",
                    body = "Choose or draw a curve, then tap APPLY CONTOUR. Tracks are reordered using energy and key compatibility.",
                    keyPoints = listOf(
                        "The current track keeps playing.",
                        "Use Queue Undo to restore the order."
                    )
                )
            ),
            proTip = "Try Flow Profiles alongside the curve when building a set.",
            relatedTopicIds = listOf("harmonigrains", "smart_flow")
        ),
        FeatureDocTopic(
            id = "smart_crates",
            title = "Smart Crates & Rule Engine",
            category = "Library & Physicality",
            summary = "Keep music in nine-slot crates, with rules for automatic collections.",
            sections = listOf(
                DocSection(
                    title = "How crates work",
                    body = "Crates hold tracks or collections. Smart Crates update when new music matches their rules.",
                    keyPoints = listOf(
                        "24-Bit FLAC Master: gathers matching high-resolution source files.",
                        "Harmonic Camelot Flow: gathers tracks by key relationships.",
                        "Vinyl Analog Lounge: gathers albums matching its rules.",
                        "Longest Tracks: sorts the matching tracks by duration."
                    )
                ),
                DocSection(
                    title = "Custom Rule Builder",
                    body = "Tap '+ Smart Crate' to see the available format, genre, tempo, and key filters. Random Picks shuffles the matching tracks.",
                    keyPoints = listOf(
                        "Rename, reorder, or delete crates from their action menu.",
                        "Hold a crate for options and help."
                    )
                )
            ),
            proTip = "Open a crate, then tap a slot to play from there.",
            relatedTopicIds = listOf("daily_capsule", "hires_lossless")
        ),
        FeatureDocTopic(
            id = "varispeed",
            title = "Analog Tape Varispeed & Pitch Lock",
            category = "Playback & DSP",
            summary = "Change playback speed with pitch held steady or changing alongside it.",
            sections = listOf(
                DocSection(
                    title = "Tape Varispeed vs Pitch Lock",
                    body = "Choose how pitch responds when you change speed:",
                    keyPoints = listOf(
                        "Pitch Locked: change speed from 0.5x to 2.0x while keeping pitch steady.",
                        "Tape Varispeed: speed and pitch change together. Faster raises pitch; slower lowers it."
                    )
                )
            ),
            proTip = "Open Speed & Pitch in Now Playing. Use ±0.05x for smaller changes.",
            relatedTopicIds = listOf("loudness_normalization")
        ),
        FeatureDocTopic(
            id = "loudness_normalization",
            title = "EBU R128 Loudness Normalization",
            category = "Playback & DSP",
            summary = "Adjust track gain to reduce volume jumps between songs.",
            sections = listOf(
                DocSection(
                    title = "Understanding LUFS & Headroom",
                    body = "Use the loudness-normalization toggle in Now Playing.",
                    keyPoints = listOf(
                        "Turn normalization on to apply the available track gain.",
                        "Separate loudness-target presets are not available in this screen.",
                        "Off: no loudness-normalization gain."
                    )
                )
            ),
            proTip = "Normalization changes each track's gain. It doesn't prove bit-perfect output.",
            relatedTopicIds = listOf("hires_lossless", "varispeed")
        ),
        FeatureDocTopic(
            id = "hires_lossless",
            title = "Hi-Res Lossless & Pure Signal Chain",
            category = "Audiophile Engine",
            summary = "Inspect source format and the available output information.",
            sections = listOf(
                DocSection(
                    title = "Source and output",
                    body = "Source metadata and detected DAC capabilities describe different parts of the path. A lossless file or USB connection alone doesn't confirm that Android bypassed resampling.",
                    keyPoints = listOf(
                        "Check source and output sample rates separately.",
                        "Processing and device settings can affect the output.",
                        "LOSSLESS and 24-BIT badges describe the source file."
                    )
                )
            ),
            proTip = "Check the Horizon card on Home for DAC and format information.",
            relatedTopicIds = listOf("loudness_normalization", "daily_capsule")
        ),
        FeatureDocTopic(
            id = "turntable",
            title = "Analog Vinyl Turntable",
            category = "Acoustic Console",
            summary = "A virtual turntable with platter movement, speed settings, and touch feedback.",
            sections = listOf(
                DocSection(
                    title = "Rotational Mechanics & Speeds",
                    body = "Choose 33⅓ or 45 RPM and try the platter controls.",
                    keyPoints = listOf(
                        "33⅓ RPM: the LP-style speed setting.",
                        "45 RPM: the single-style speed setting.",
                        "Scrubbing & Needle-Drop: drag the record to scrub or change position."
                    )
                ),
                DocSection(
                    title = "Needle-Drop Haptic Response",
                    body = "Tonearm movement uses the phone's available haptic feedback.",
                    keyPoints = listOf(
                        "Subtle groove lead-in click on playback start.",
                        "Feedback follows platter movement."
                    )
                )
            ),
            proTip = "Try Analog Tape Saturation in the deck for a different sound.",
            relatedTopicIds = listOf("cassette_deck", "audio_haptics", "vinyl_vault")
        ),
        FeatureDocTopic(
            id = "stems_isolator",
            title = "4-Stem Audio Isolator & Mixer",
            category = "Mixing & DSP",
            summary = "Explore four parts of the mix with solo, mute, gain, pan, and filter controls.",
            sections = listOf(
                DocSection(
                    title = "4-Channel Acoustic Separation",
                    body = "The mixer offers Vocals, Drums, Bass, and Instruments controls. Results depend on the source and separation method.",
                    keyPoints = listOf(
                        "Vocals: focus on the center-channel vocal range.",
                        "Drums: focus on rhythmic transients.",
                        "Bass: focus on frequencies below 250 Hz.",
                        "Instruments: focus on the remaining instrumental range."
                    )
                ),
                DocSection(
                    title = "Performance Mixing Controls",
                    body = "Each part has mute, solo, ±12 dB gain, pan, and three-band EQ controls.",
                    keyPoints = listOf(
                        "Quick Presets: try Acapella Focus, Dub Instrumental, Drum & Bass, or Vocal Cut.",
                        "Crossfader: blend your mix with the original audio."
                    )
                )
            ),
            proTip = "Solo a part and open the Phosphor Oscilloscope to inspect its signal.",
            relatedTopicIds = listOf("mastering_rack", "oscilloscope", "auto_dj")
        ),
        FeatureDocTopic(
            id = "mastering_rack",
            title = "Studio Mastering & Parametric EQ",
            category = "Acoustic Console",
            summary = "Adjust five EQ bands, tape drive, stereo width, and limiting.",
            sections = listOf(
                DocSection(
                    title = "5-Band Equalization",
                    body = "The five bands cover 60 Hz through 14 kHz.",
                    keyPoints = listOf(
                        "Minimum-phase filters.",
                        "Boost or cut each band by up to 12 dB; adjust Q to change its width."
                    )
                ),
                DocSection(
                    title = "Mastering Dynamics & Enhancers",
                    body = "Drive adds saturation, Width changes the stereo image, and Limiter controls peaks.",
                    keyPoints = listOf(
                        "Drive: add harmonic saturation.",
                        "Stereo Width: adjust side-channel level.",
                        "Limiter: reduce peaks above the ceiling."
                    )
                )
            ),
            proTip = "Use Global EQ for shared settings, or Track-Specific Master for one song.",
            relatedTopicIds = listOf("loudness_normalization", "stems_isolator", "spatial_chamber")
        ),
        FeatureDocTopic(
            id = "cassette_deck",
            title = "Magnetic Cassette Tape Deck",
            category = "Analog Studio",
            summary = "Try tape-style saturation, hiss, and wow & flutter.",
            sections = listOf(
                DocSection(
                    title = "Magnetic Tape Formulations",
                    body = "Choose Type I, Type II, or Type IV for different saturation, noise, and frequency settings.",
                    keyPoints = listOf(
                        "Type I Ferric: more warmth and a softer high end.",
                        "Type II Chrome: a clearer high end and less hiss.",
                        "Type IV Metal: more headroom and less saturation."
                    )
                ),
                DocSection(
                    title = "Transport Physics & Mechanical Wow",
                    body = "Adjust wow & flutter, drift, azimuth, and saturation for the tape effect.",
                    keyPoints = listOf(
                        "Wow & Flutter: adjust pitch movement.",
                        "Saturation: add compression and harmonics."
                    )
                )
            ),
            proTip = "Try 15 ips for the reel-to-reel-style preset.",
            relatedTopicIds = listOf("turntable", "mastering_rack", "varispeed")
        ),
        FeatureDocTopic(
            id = "spatial_chamber",
            title = "Spatial Acoustic Chamber",
            category = "Spatial Audio",
            summary = "Adjust a virtual room's size, decay, damping, and speaker positions.",
            sections = listOf(
                DocSection(
                    title = "Room Geometry & Wall Damping",
                    body = "Choose room sizes from 28 to 3,500 m³ and adjust how much sound the walls absorb.",
                    keyPoints = listOf(
                        "RT60 Decay: time for the reverb to fall by 60 dB.",
                        "Wall Damping: reduce reflected mid and high frequencies."
                    )
                ),
                DocSection(
                    title = "Virtual Speaker Positioning",
                    body = "Move the virtual speakers between 15° and 90°, then adjust their distance.",
                    keyPoints = listOf(
                        "HRTF crossfeed adjusts the headphone soundstage.",
                        "Try crossfeed if you prefer less separation between channels."
                    )
                )
            ),
            proTip = "Try 'Abbey Control Room' as a starting point for headphone listening.",
            relatedTopicIds = listOf("mastering_rack", "oscilloscope", "hires_lossless")
        ),
        FeatureDocTopic(
            id = "oscilloscope",
            title = "Phosphor CRT Oscilloscope Lab",
            category = "Audio Analysis",
            summary = "View stereo phase, X-Y figures, and phosphor-style trails.",
            sections = listOf(
                DocSection(
                    title = "Stereo Phase Correlation & Lissajous",
                    body = "The X-Y view compares the two channels. A vertical line suggests mono; a wider shape shows channel differences. Horizontal spread can indicate negative correlation.",
                    keyPoints = listOf(
                        "Vertical Line: positive correlation, approaching +1.0.",
                        "Circular Ring: a phase difference between channels.",
                        "Horizontal Spread: check for cancellation when listening in mono."
                    )
                ),
                DocSection(
                    title = "CRT Phosphor Types & Persistence",
                    body = "Choose Classic P1 Green, Vintage P3 Amber, Medical P4 Cyan, Studio P45 Monochrome, RGB Laser, or Cyberpunk Ultraviolet.",
                    keyPoints = listOf(
                        "Classic P1 Green: green traces.",
                        "Vintage P3 Amber: amber traces."
                    )
                )
            ),
            proTip = "Drag the 3D view to change the trace's angle.",
            relatedTopicIds = listOf("stems_isolator", "spatial_chamber")
        ),
        FeatureDocTopic(
            id = "auto_dj",
            title = "Auto-DJ & Transition Engine",
            category = "Harmonic Flow",
            summary = "Plan transitions using available beat, phrase, and key information.",
            sections = listOf(
                DocSection(
                    title = "Phrase Boundary Matching",
                    body = "Auto-DJ estimates phrase boundaries from BPM and bar counts. These are timing estimates, not detected musical phrases.",
                    keyPoints = listOf(
                        "BPM Synchronization: adjust the incoming track's tempo.",
                        "Downbeat Alignment: start the next track at the beginning of a phrase."
                    )
                ),
                DocSection(
                    title = "Harmonic Crossfade Curves",
                    body = "Choose from the transition curves shown in Auto-DJ. Bass Swap reduces outgoing bass during the transition.",
                    keyPoints = listOf(
                        "Equal-Power: balance the two tracks' levels through the fade.",
                        "Bass Swap: reduce outgoing 40–200 Hz bass as the incoming bass starts."
                    )
                )
            ),
            proTip = "Try Harmonic Key Lock to pitch the incoming track toward a compatible key.",
            relatedTopicIds = listOf("smart_flow", "harmonigrains", "trajectory_sculptor")
        ),
        FeatureDocTopic(
            id = "vinyl_vault",
            title = "Vinyl Crate & Extended Metadata",
            category = "Digging & Curation",
            summary = "Browse album sleeves and inspect their file details.",
            sections = listOf(
                DocSection(
                    title = "Tactile Sleeve Browsing",
                    body = "Swipe through the current collection or playlist.",
                    keyPoints = listOf(
                        "Browse by sleeve artwork.",
                        "See track count and crate duration."
                    )
                ),
                DocSection(
                    title = "Technical Metadata Inspection",
                    body = "Check bit depth, sample rate, codec, bitrate, ReplayGain, and available album information.",
                    keyPoints = listOf(
                        "Shows available source metadata, such as FLAC 96 kHz / 24-bit.",
                        "Shows available LUFS information."
                    )
                )
            ),
            proTip = "Tap a sleeve to play that track.",
            relatedTopicIds = listOf("turntable", "smart_crates", "hires_lossless")
        ),
        FeatureDocTopic(
            id = "audio_haptics",
            title = "Tactile Audio Haptics Lab",
            category = "Hardware Transducer",
            summary = "Use vibration feedback for bass and rhythmic changes.",
            sections = listOf(
                DocSection(
                    title = "Sub-Bass Transient Extraction",
                    body = "The bass mode uses a 24 dB/oct low-pass filter below 80 Hz.",
                    keyPoints = listOf(
                        "Vibration follows the rhythm.",
                        "Timing depends on your device."
                    )
                ),
                DocSection(
                    title = "Resonance & Haptic Modulation",
                    body = "Adjust vibration strength, attack, decay, and frequency.",
                    keyPoints = listOf(
                        "Use lower intensity for lighter feedback.",
                        "Try Sub-Bass Rumble for heavier pulses."
                    )
                )
            ),
            proTip = "Turn on Tactile Haptics for feedback while scrubbing the turntable.",
            relatedTopicIds = listOf("turntable", "cassette_deck", "stems_isolator")
        )
    )

    fun getTopic(topicId: String): FeatureDocTopic? {
        return topics.firstOrNull { it.id.equals(topicId, ignoreCase = true) }
            ?: topics.firstOrNull { it.id.contains(topicId, ignoreCase = true) }
    }

    fun getAllTopics(): List<FeatureDocTopic> = topics
}
