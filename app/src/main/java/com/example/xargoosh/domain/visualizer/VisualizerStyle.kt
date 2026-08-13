package com.example.xargoosh.domain.visualizer

import androidx.annotation.StringRes
import com.example.xargoosh.R

enum class RendererGroup(@StringRes val displayNameRes: Int) {
    MINIMAL(R.string.visualizer_group_minimal),
    SPECTRUM(R.string.visualizer_group_spectrum),
    AMBIENT(R.string.visualizer_group_ambient),
    PARTICLES(R.string.visualizer_group_particles),
    ORGANIC(R.string.visualizer_group_organic)
}

enum class VisualizerStyle(@StringRes val displayNameRes: Int, val group: RendererGroup) {

    MINIMAL_LINE(R.string.style_minimal_line, RendererGroup.MINIMAL),
    NIER_WAVE(R.string.style_nier_wave, RendererGroup.MINIMAL),
    DUAL_WAVE(R.string.style_dual_wave, RendererGroup.MINIMAL),

    CIRCULAR_SPECTRUM(R.string.style_circular_spectrum, RendererGroup.SPECTRUM),
    SPECTRUM_BARS(R.string.style_spectrum_bars, RendererGroup.SPECTRUM),
    PULSE_RING(R.string.style_pulse_ring, RendererGroup.SPECTRUM),

    NEBULA(R.string.style_nebula, RendererGroup.AMBIENT),
    AURORA(R.string.style_aurora, RendererGroup.AMBIENT),
    GALAXY(R.string.style_galaxy, RendererGroup.AMBIENT),

    PARTICLES(R.string.style_particles, RendererGroup.PARTICLES),
    CONSTELLATION(R.string.style_constellation, RendererGroup.PARTICLES),
    NEURAL_NETWORK(R.string.style_neural_network, RendererGroup.PARTICLES),

    RIBBON(R.string.style_ribbon, RendererGroup.ORGANIC),
    LIQUID(R.string.style_liquid, RendererGroup.ORGANIC),
    ORGANIC_FLOW(R.string.style_organic_flow, RendererGroup.ORGANIC),
    WATER_RIPPLE(R.string.style_water_ripple, RendererGroup.ORGANIC),
    ENERGY_FIELD(R.string.style_energy_field, RendererGroup.ORGANIC),
    HEX_GRID(R.string.style_hex_grid, RendererGroup.ORGANIC);

    companion object {
        fun byGroup(group: RendererGroup) = values().filter { it.group == group }
    }
}
