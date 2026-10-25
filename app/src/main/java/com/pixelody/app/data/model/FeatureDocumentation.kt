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
                    body = "The strip plots tracks in their queue order. Each point combines the available Camelot key with relative energy, so you can see a change coming without opening every track. Height represents energy on a 0–1 scale; color separates major and minor keys. These values describe the track information available to Pixelody, rather than proving how a transition will sound.",
                    keyPoints = listOf(
                        "Cyan / Sky: major keys.",
                        "Violet / Lavender: minor keys.",
                        "Height: relative track energy, from 0.0 to 1.0.",
                        "Left to right: playlist order."
                    )
                ),
                DocSection(
                    title = "Key and energy changes",
                    body = "Look for abrupt jumps if you want a smoother sequence. The cohesion figure counts neighboring key relationships, while the arc shows changes in energy. A high score can still join two recordings awkwardly, so listen to the transition when that matters.",
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
                    body = "Camelot gives musical keys a number and letter. A is minor and B is major: 8A is A minor, while 8B is its relative C major. You can look for the same key, move one number around the wheel, or switch A/B at the same number. Those are ways to narrow the library, not rules that every pair of recordings will follow.",
                    keyPoints = listOf(
                        "Exact Match: the same key, such as 8A to 8A.",
                        "Harmonic Step (±1): neighboring keys, such as 8A to 7A or 9A.",
                        "Mode Switch (A <-> B): relative major/minor, such as 8A to 8B."
                    )
                ),
                DocSection(
                    title = "Advanced Transition Modes",
                    body = "The modes change which nearby keys the lens includes. Harmonic stays within one step; Energy Boost and Sunset Drift explore two steps in opposite directions. Try the result rather than assuming the name predicts the mood.",
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
                    title = "What the recap groups together",
                    body = "The capsule summarizes the listening recorded for that day: time, tracks and available source information. Its named groups are descriptions of those patterns. They are not measurements of your equipment or a fixed profile of your taste.",
                    keyPoints = listOf(
                        "Hi-Res Audiophile: a high share of 24-bit or FLAC tracks.",
                        "Deep Flow Architect: longer sessions with compatible track keys.",
                        "Vinyl Purist: more full-album listening.",
                        "Electronic Explorer: higher-tempo sessions and more queue changes."
                    )
                ),
                DocSection(
                    title = "Lossless Ratio & Streaks",
                    body = "The lossless share comes from the source files played, and the streak follows recorded listening days. A FLAC or 24-bit badge tells you about the file; it doesn't verify Android's output path. Open the timeline for earlier days rather than treating today's capsule as the whole history.",
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
                    title = "Choosing an order",
                    body = "Flow uses the library and available track information to plan an order. Key, tempo and energy can help connect tracks, but missing or imperfect tags limit what those choices can establish. You can inspect the queue and try a different profile without turning the matching score into a promise about the music.",
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
                    title = "Choosing the curve",
                    body = "The curve describes how you want energy to change through the session. Peak Wave rises and settles, Slow Build keeps climbing, and Chill Descent winds down. Choose one close to what you have in mind or draw your own contour.",
                    keyPoints = listOf(
                        "Peak Wave: build to a peak, then wind down.",
                        "Slow Build: start quiet and gradually raise the energy.",
                        "Chill Descent: start upbeat, then settle down."
                    )
                ),
                DocSection(
                    title = "Applying the Curve",
                    body = "After choosing or drawing the curve, tap APPLY CONTOUR. Pixelody reorders the available tracks using energy and key compatibility while the current song keeps playing. Queue Undo restores the earlier order if the result is less useful than what you had.",
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
                    body = "A crate has nine slots for tracks or collections. Ordinary crates keep what you put there; Smart Crates use rules to gather matching music as the library changes. The preset names indicate the kind of collection being built, so inspect their rules if you want to know why a track is included.",
                    keyPoints = listOf(
                        "24-Bit FLAC Master: gathers matching high-resolution source files.",
                        "Harmonic Camelot Flow: gathers tracks by key relationships.",
                        "Vinyl Analog Lounge: gathers albums matching its rules.",
                        "Longest Tracks: sorts the matching tracks by duration."
                    )
                ),
                DocSection(
                    title = "Custom Rule Builder",
                    body = "Open '+ Smart Crate' to choose format, genre, tempo and key rules. Random Picks shuffles the matches. A rule can only use the metadata available for those tracks, so a missing tag can explain an unexpected gap. Use the crate menu to rename, reorder or delete it.",
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
                    body = "Pitch Locked changes speed from 0.5× to 2.0× while holding pitch steady. Tape Varispeed changes both: faster sounds higher and slower sounds lower. Pick the behavior you want before adjusting speed; the ±0.05× controls make smaller changes.",
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
                    title = "Normalization in Now Playing",
                    body = "Normalization applies the available track gain to reduce level jumps. The toggle is in Now Playing; this screen does not offer separate target presets. Turning it off removes that normalization gain, while other active processing and the device output path still apply.",
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
                    body = "Source metadata describes the recording file. Detected DAC information describes an output device or requested configuration. They are different parts of the route: a lossless file, matching rates or a USB connection alone doesn't establish that Android bypassed mixing or resampling. Check source and output separately, especially when processing is enabled.",
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
                    title = "Platter speed",
                    body = "Choose the LP-style 33⅓ RPM or single-style 45 RPM setting, then use the platter to scrub or change position. The turntable is a playback control with a physical-media presentation; the source is still the selected track.",
                    keyPoints = listOf(
                        "33⅓ RPM: the LP-style speed setting.",
                        "45 RPM: the single-style speed setting.",
                        "Scrubbing & Needle-Drop: drag the record to scrub or change position."
                    )
                ),
                DocSection(
                    title = "Needle-Drop Haptic Response",
                    body = "The tonearm and platter can use the phone's vibration feedback, including a small lead-in response at playback start. What you feel depends on the device and the haptic settings.",
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
                    title = "The four parts",
                    body = "The mixer presents Vocals, Drums, Bass and Instruments as adjustable parts of the signal. Their usefulness depends on the recording and the separation method. A center-focused vocal range can still contain instruments, and a frequency/transient split is not the same as having the original studio stems.",
                    keyPoints = listOf(
                        "Vocals: focus on the center-channel vocal range.",
                        "Drums: focus on rhythmic transients.",
                        "Bass: focus on frequencies below 250 Hz.",
                        "Instruments: focus on the remaining instrumental range."
                    )
                ),
                DocSection(
                    title = "Performance Mixing Controls",
                    body = "Solo a part to hear what the processing has actually isolated, then use mute, pan, three-band EQ or up to ±12 dB gain. The crossfader compares your mix with the original. Presets are starting settings, so check for unwanted spill or artifacts before relying on a vocal/instrumental result.",
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
                    body = "The five-band EQ covers 60 Hz through 14 kHz. Gain boosts or cuts a band by up to 12 dB, while Q changes how wide that adjustment reaches. Make one change at a time if you are trying to identify a particular sound; a narrow correction and a broad tonal change need different widths.",
                    keyPoints = listOf(
                        "Minimum-phase filters.",
                        "Boost or cut each band by up to 12 dB; adjust Q to change its width."
                    )
                ),
                DocSection(
                    title = "Drive, width and limiting",
                    body = "Drive adds harmonic saturation, Width changes the side-channel level, and Limiter reduces peaks above its ceiling. These controls can work alongside EQ, so a result may come from more than the band you just moved. Choose Global EQ for shared settings or Track-Specific Master when the adjustment belongs to one recording.",
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
                    body = "The tape types are effect presets for saturation, noise and frequency response. Type I has more warmth and a softer high end; Type II is clearer with less hiss; Type IV leaves more headroom and saturates less. They do not change the source file into a different recording format.",
                    keyPoints = listOf(
                        "Type I Ferric: more warmth and a softer high end.",
                        "Type II Chrome: a clearer high end and less hiss.",
                        "Type IV Metal: more headroom and less saturation."
                    )
                ),
                DocSection(
                    title = "Tape movement and saturation",
                    body = "Wow & flutter and drift add pitch movement, while azimuth and saturation change other parts of the tape effect. Increase them for an audible worn-tape result or keep them low for a smaller change. The 15 ips choice is a reel-style preset.",
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
                    body = "The virtual-room controls model size from 28 to 3,500 m³, decay and wall absorption. RT60 is the modeled time for reverberation to fall by 60 dB. More damping reduces reflected mid/high energy. These are effect settings, not measurements of the room you are sitting in.",
                    keyPoints = listOf(
                        "RT60 Decay: time for the reverb to fall by 60 dB.",
                        "Wall Damping: reduce reflected mid and high frequencies."
                    )
                ),
                DocSection(
                    title = "Virtual Speaker Positioning",
                    body = "Move the modeled speakers between 15° and 90° and adjust distance to change the virtual scene. HRTF crossfeed can bring the headphone channels toward a more shared soundstage. Compare with it off if you want to hear which part of the change comes from crossfeed.",
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
                    body = "The X-Y trace compares left and right samples. A narrow vertical trace suggests strong positive correlation; width shows differences between channels, and horizontal spread is a reason to check cancellation in mono. The picture helps inspect the signal, but a particular shape does not establish whether a mix sounds good.",
                    keyPoints = listOf(
                        "Vertical Line: positive correlation, approaching +1.0.",
                        "Circular Ring: a phase difference between channels.",
                        "Horizontal Spread: check for cancellation when listening in mono."
                    )
                ),
                DocSection(
                    title = "CRT Phosphor Types & Persistence",
                    body = "The phosphor choices change the trace appearance and persistence: Classic P1 Green, Vintage P3 Amber, Medical P4 Cyan, Studio P45 Monochrome, RGB Laser or Cyberpunk Ultraviolet. Choose whichever makes the trace easiest to read.",
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
                    body = "Auto-DJ uses BPM and bar counts to estimate where a transition can fall. A track with an unusual intro, changing tempo or inaccurate tags can miss that estimate. It is not detecting the actual musical phrase, so listen at the handover if timing matters.",
                    keyPoints = listOf(
                        "BPM Synchronization: adjust the incoming track's tempo.",
                        "Downbeat Alignment: start the next track at the beginning of a phrase."
                    )
                ),
                DocSection(
                    title = "Harmonic Crossfade Curves",
                    body = "Equal-Power changes the two levels through the overlap. Bass Swap also reduces the outgoing 40–200 Hz region as incoming bass arrives. Choose a curve for the actual pair of recordings rather than assuming a key match will prevent competing bass or an awkward fade.",
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
                    body = "Swipe through the collection by sleeve artwork. Track count and duration stay with the collection, and tapping a sleeve starts that track.",
                    keyPoints = listOf(
                        "Browse by sleeve artwork.",
                        "See track count and crate duration."
                    )
                ),
                DocSection(
                    title = "File details",
                    body = "Open the details for source bit depth, rate, codec, bitrate, ReplayGain and available album information. Missing values stay unavailable; a high-resolution source label does not establish the format reaching the output.",
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
                    title = "Bass-driven vibration",
                    body = "Bass mode follows the signal below 80 Hz through a 24 dB/oct low-pass filter. The phone turns that information into vibration, with timing and strength limited by its hardware.",
                    keyPoints = listOf(
                        "Vibration follows the rhythm.",
                        "Timing depends on your device."
                    )
                ),
                DocSection(
                    title = "Adjusting feedback",
                    body = "Strength controls how much vibration you feel; attack and decay affect how quickly it begins and fades. Start with lighter feedback and compare the rhythmic result. Sub-Bass Rumble is the heavier preset; Tactile Haptics also supports platter scrubbing.",
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
