package com.example.xargoosh.core.visualizer.effects

import com.example.xargoosh.domain.visualizer.VisualizerRenderer
import com.example.xargoosh.domain.visualizer.VisualizerStyle
import com.example.xargoosh.core.visualizer.effects.renderers.*

object RendererFactory {
    fun create(style: VisualizerStyle): VisualizerRenderer = when (style) {
        VisualizerStyle.MINIMAL_LINE -> MinimalLineRenderer()
        VisualizerStyle.NIER_WAVE -> NierWaveRenderer()
        VisualizerStyle.DUAL_WAVE -> DualWaveRenderer()
        VisualizerStyle.CIRCULAR_SPECTRUM -> CircularSpectrumRenderer()
        VisualizerStyle.SPECTRUM_BARS -> SpectrumBarsRenderer()
        VisualizerStyle.PULSE_RING -> PulseRingRenderer()
        VisualizerStyle.NEBULA -> NebulaRenderer()
        VisualizerStyle.AURORA -> AuroraRenderer()
        VisualizerStyle.GALAXY -> GalaxyRenderer()
        VisualizerStyle.PARTICLES -> ParticlesRenderer()
        VisualizerStyle.CONSTELLATION -> ConstellationRenderer()
        VisualizerStyle.NEURAL_NETWORK -> NeuralNetworkRenderer()
        VisualizerStyle.RIBBON -> RibbonRenderer()
        VisualizerStyle.LIQUID -> LiquidRenderer()
        VisualizerStyle.ORGANIC_FLOW -> OrganicFlowRenderer()
        VisualizerStyle.WATER_RIPPLE -> WaterRippleRenderer()
        VisualizerStyle.ENERGY_FIELD -> EnergyFieldRenderer()
        VisualizerStyle.HEX_GRID -> HexGridRenderer()
    }
}

class RendererRegistry {
    private val cache = mutableMapOf<VisualizerStyle, VisualizerRenderer>()
    private var activeStyle: VisualizerStyle? = null

    fun get(style: VisualizerStyle): VisualizerRenderer {
        if (activeStyle != null && activeStyle != style) {
            cache[activeStyle!!]?.onDeactivated()
        }
        val renderer = cache.getOrPut(style) { RendererFactory.create(style) }
        if (activeStyle != style) {
            renderer.onActivated()
            activeStyle = style
        }
        return renderer
    }

    fun clear() {
        cache.values.forEach { it.onDeactivated() }
        cache.clear()
        activeStyle = null
    }
}
